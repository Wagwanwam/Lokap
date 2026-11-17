package vn.nhom2.lokap.feature

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import vn.nhom2.lokap.data.fake.FakeAuthRepository
import vn.nhom2.lokap.data.fake.FakeAuthStore
import vn.nhom2.lokap.data.fake.FakePostRepository
import vn.nhom2.lokap.data.fake.FakePostStore
import vn.nhom2.lokap.data.fake.FakeSettingsRepository
import vn.nhom2.lokap.data.fake.FakeSocialRepository
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.data.fake.SampleSocialData
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.feature.friends.FriendsAction
import vn.nhom2.lokap.feature.friends.FriendsEffect
import vn.nhom2.lokap.feature.friends.FriendsViewModel
import vn.nhom2.lokap.feature.profile.ProfileAction
import vn.nhom2.lokap.feature.profile.ProfileViewModel
import vn.nhom2.lokap.domain.repository.SocialGraph
import vn.nhom2.lokap.feature.search.SearchAction
import vn.nhom2.lokap.feature.search.SearchEffect
import vn.nhom2.lokap.data.fake.FakeLocationRepository
import vn.nhom2.lokap.feature.search.SearchViewModel
import vn.nhom2.lokap.feature.settings.SettingsAction
import vn.nhom2.lokap.feature.settings.SettingsEffect
import vn.nhom2.lokap.feature.settings.SettingsUiState
import vn.nhom2.lokap.feature.settings.SettingsViewModel
import java.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class SocialLogicTest {

    private lateinit var dispatcher: TestDispatcher

    @Before
    fun setUp() {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        SampleSocialData.resetAccountForDemo()
        FakeAuthStore.currentUser.value = SampleData.currentUser
    }

    @After
    fun tearDown() {
        SampleSocialData.resetAccountForDemo()
        FakeAuthStore.currentUser.value = null
        Dispatchers.resetMain()
    }

    // ViewModel nhận Repository; bản giả bọc đúng SampleSocialData / FakePostStore nên luật cũ không đổi
    private fun profile(userId: String?) = ProfileViewModel(
        SavedStateHandle(if (userId == null) emptyMap() else mapOf("userId" to userId)),
        FakeAuthRepository(),
        ContentTestRepository(),
        FakeSocialRepository(),
        FakePostRepository()
    )

    private fun friends() = FriendsViewModel(FakeSocialRepository())

    private fun settings() = SettingsViewModel(
        FakeAuthRepository(), FakeSettingsRepository(), FakeSocialRepository(),
        TestDeviceSettingsRepository(), TestAppCacheRepository(),
        vn.nhom2.lokap.data.local.PersistentAccountContactRepository(TestContactPreferences())
    )

    private fun search() = SearchViewModel(FakeSocialRepository(), FakeLocationRepository())

    private fun strangerWithStatus(status: FriendStatus) = SampleSocialData.directoryUsers.first {
        it.id != SampleData.currentUser.id && SampleSocialData.getFriendStatus(it.id) == status
    }

    // ---------- "Đã đến X/34" ----------

    @Test
    fun `da den dem so tinh khac nhau trong bai cua minh`() = runTest(dispatcher) {
        val vm = profile(null)
        advanceUntilIdle()
        val expected = FakePostStore.posts.value
            .filter { it.author.id == SampleData.currentUser.id }
            .mapNotNull { it.location.provinceCode }
            .toSet().size
        assertEquals(expected, vm.uiState.value.visitedProvincesCount)
        assertEquals(34, vm.uiState.value.totalProvincesCount)
    }

    // ---------- Luật xem bài trên hồ sơ người khác ----------

    private val base = SampleData.posts.first { it.author.id != SampleData.currentUser.id }
    private val friendsPost = base.copy(audience = Audience.FRIENDS, publicVisibleAt = null)
    private val scheduledPublic = base.copy(audience = Audience.PUBLIC, publicVisibleAt = SampleData.NOW.plus(Duration.ofDays(3)))
    private val shownPublic = base.copy(audience = Audience.PUBLIC, publicVisibleAt = SampleData.NOW.minus(Duration.ofHours(1)))
    private val directPost = base.copy(audience = Audience.DIRECT, publicVisibleAt = null)

    private fun visible(post: vn.nhom2.lokap.domain.model.Post, status: FriendStatus) =
        SampleSocialData.isPostVisibleToViewer(post, isMe = false, friendStatus = status, now = SampleData.NOW)

    @Test
    fun `ban be thay bai FRIENDS va PUBLIC ke ca dang hen gio`() {
        assertTrue(visible(friendsPost, FriendStatus.FRIENDS))
        assertTrue(visible(scheduledPublic, FriendStatus.FRIENDS))
        assertTrue(visible(shownPublic, FriendStatus.FRIENDS))
        assertFalse(visible(directPost, FriendStatus.FRIENDS))
    }

    @Test
    fun `nguoi la chi thay PUBLIC da toi gio`() {
        for (status in listOf(FriendStatus.NONE, FriendStatus.REQUEST_SENT, FriendStatus.REQUEST_RECEIVED)) {
            assertFalse(visible(friendsPost, status))
            assertFalse(visible(scheduledPublic, status))
            assertTrue(visible(shownPublic, status))
        }
    }

    @Test
    fun `bi chan thi khong thay gi`() {
        listOf(friendsPost, scheduledPublic, shownPublic).forEach { assertFalse(visible(it, FriendStatus.BLOCKED)) }
    }

    @Test
    fun `5 nguoi trong SampleData la ban, khop voi Camera va Feed`() {
        SampleData.users.filter { it.id != SampleData.currentUser.id }
            .forEach { assertEquals(it.displayName, FriendStatus.FRIENDS, SampleSocialData.getFriendStatus(it.id)) }
    }

    @Test
    fun `ho so nguoi khac chi hien bai duoc phep xem`() = runTest(dispatcher) {
        val friend = SampleData.users[1]
        val vm = profile(friend.id)
        advanceUntilIdle()
        val state = vm.uiState.value
        assertFalse(state.isMe)
        assertEquals(friend.id, state.user?.id)
        assertEquals(FriendStatus.FRIENDS, state.friendStatus)
        assertTrue(state.visiblePosts.isNotEmpty())
        assertTrue(state.visiblePosts.all { it.author.id == friend.id && it.audience != Audience.DIRECT })
    }

    @Test
    fun `ho so nguoi khong ton tai thi bao khong tim thay`() = runTest(dispatcher) {
        val vm = profile("u_khong_co")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isUserNotFound)
    }

    // ---------- Giới hạn 20 bạn ----------

    @Test
    fun `du 20 ban thi khoa Ket ban va Chap nhan`() = runTest(dispatcher) {
        val stranger = SampleSocialData.directoryUsers.last { it.id != SampleData.currentUser.id }
        val twenty = (1..20).associate { "fake_$it" to FriendStatus.FRIENDS }
        SampleSocialData.setFriendStatusesForTest(twenty + (stranger.id to FriendStatus.NONE))

        assertFalse(SampleSocialData.sendFriendRequest(stranger.id))
        assertFalse(SampleSocialData.acceptFriendRequest(stranger.id))
        assertEquals(FriendStatus.NONE, SampleSocialData.getFriendStatus(stranger.id))

        val vm = profile(stranger.id)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isFriendLimitReached)
    }

    @Test
    fun `du 20 ban thi man Ban be bao gioi han khi gui loi moi`() = runTest(dispatcher) {
        val stranger = SampleSocialData.directoryUsers.last { it.id != SampleData.currentUser.id }
        val twenty = (1..20).associate { "fake_$it" to FriendStatus.FRIENDS }
        SampleSocialData.setFriendStatusesForTest(twenty + (stranger.id to FriendStatus.NONE))

        val vm = friends()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isFriendLimitReached)
        vm.onAction(FriendsAction.SendRequest(stranger.id))
        val effect = withTimeoutOrNull(1000) { vm.effects.first() } as FriendsEffect.ShowMessage
        assertTrue(effect.message, effect.message.contains("20"))
        assertEquals(FriendStatus.NONE, SampleSocialData.getFriendStatus(stranger.id))
    }

    @Test
    fun `duoi 20 ban thi gui loi moi duoc`() {
        val stranger = strangerWithStatus(FriendStatus.NONE)
        assertTrue(SampleSocialData.sendFriendRequest(stranger.id))
        assertEquals(FriendStatus.REQUEST_SENT, SampleSocialData.getFriendStatus(stranger.id))
    }

    @Test
    fun `gui loi moi tu trang ho so cap nhat trang thai ngay`() = runTest(dispatcher) {
        val stranger = strangerWithStatus(FriendStatus.NONE)
        val vm = profile(stranger.id)
        advanceUntilIdle()
        assertEquals(FriendStatus.NONE, vm.uiState.value.friendStatus)
        vm.onAction(ProfileAction.SendFriendRequest)
        advanceUntilIdle()
        assertEquals(FriendStatus.REQUEST_SENT, vm.uiState.value.friendStatus)
        assertEquals(FriendStatus.REQUEST_SENT, SampleSocialData.getFriendStatus(stranger.id))
    }

    // ---------- Tìm bạn ----------

    @Test
    fun `tim ban khong hien nguoi da chan`() = runTest(dispatcher) {
        val blocked = strangerWithStatus(FriendStatus.BLOCKED)
        val vm = friends()
        vm.onAction(FriendsAction.ChangeSearchQuery(blocked.username))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.searchResults.none { it.user.id == blocked.id })
    }

    @Test
    fun `man Ban be chia dung loi moi den, da gui va ban be tu Repository`() = runTest(dispatcher) {
        val vm = friends()
        advanceUntilIdle()
        val state = vm.uiState.value
        assertEquals(strangerWithStatus(FriendStatus.REQUEST_SENT).id, state.sentRequests.single().id)
        assertTrue(state.incomingRequests.any { it.id == strangerWithStatus(FriendStatus.REQUEST_RECEIVED).id })
        assertEquals(SampleData.users.size - 1, state.friends.size)
        assertEquals(20, state.maxFriends)
        assertFalse(state.isLoading)
    }

    @Test
    fun `chap nhan loi moi o man Ban be thi thanh ban`() = runTest(dispatcher) {
        val requester = strangerWithStatus(FriendStatus.REQUEST_RECEIVED)
        val vm = friends()
        advanceUntilIdle()
        vm.onAction(FriendsAction.AcceptRequest(requester.id))
        advanceUntilIdle()
        assertEquals(FriendStatus.FRIENDS, SampleSocialData.getFriendStatus(requester.id))
        assertTrue(vm.uiState.value.friends.any { it.id == requester.id })
    }

    @Test
    fun `mo man Ban be thi lam moi tu server, mat mang thi bao offline nhung van hien ban da co`() = runTest(dispatcher) {
        val vm = FriendsViewModel(OfflineSocialRepository())
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.isOffline)
        assertTrue(vm.uiState.value.friends.isNotEmpty())
        assertNull("Có dữ liệu trong máy thì không che bằng màn lỗi", vm.uiState.value.errorMessage)
    }

    // ---------- Tìm kiếm ----------

    @Test
    fun `tim kiem that bai thi bao loi, khong im lang`() = runTest(dispatcher) {
        val vm = SearchViewModel(OfflineSocialRepository(), FakeLocationRepository())
        advanceUntilIdle()
        vm.onAction(SearchAction.QueryChanged("khanh"))
        advanceUntilIdle()
        val effect = withTimeoutOrNull(1000) { vm.effect.first() }
        assertTrue("Phải báo lỗi, nhận: $effect", effect is SearchEffect.ShowMessage)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `tim kiem ket ban that bai thi bao loi`() = runTest(dispatcher) {
        val stranger = strangerWithStatus(FriendStatus.NONE)
        val vm = SearchViewModel(OfflineSocialRepository(), FakeLocationRepository())
        advanceUntilIdle()
        vm.onAction(SearchAction.ToggleFriendRequest(stranger.id))
        advanceUntilIdle()
        val effect = withTimeoutOrNull(1000) { vm.effect.first() }
        assertTrue("Phải báo lỗi, nhận: $effect", effect is SearchEffect.ShowMessage)
        assertEquals(FriendStatus.NONE, SampleSocialData.getFriendStatus(stranger.id))
    }

    @Test
    fun `goi y tim kiem cap nhat khi danh sach ban tu server ve muon`() = runTest(dispatcher) {
        val repo = LateGraphSocialRepository()
        val vm = SearchViewModel(repo, FakeLocationRepository())
        advanceUntilIdle()
        assertTrue(vm.uiState.value.userResults.isEmpty())
        val friend = SampleData.users[1]
        repo.graphState.value = SocialGraph(statuses = mapOf(friend.id to FriendStatus.FRIENDS), users = mapOf(friend.id to friend))
        advanceUntilIdle()
        assertEquals(listOf(friend.id), vm.uiState.value.userResults.map { it.id })
    }

    @Test
    fun `sua ho so tieu su trong thi o nhap de trong, khong dien cau mau, toi da 60 ky tu`() = runTest(dispatcher) {
        // SampleData.currentUser không có tiểu sử
        val vm = profile(null)
        advanceUntilIdle()
        vm.onAction(ProfileAction.OpenEditProfileSheet)
        assertEquals("", vm.uiState.value.editBioInput)
        vm.onAction(ProfileAction.ChangeEditBio("a".repeat(70)))
        assertEquals(60, vm.uiState.value.editBioInput.length)

        val s = settings()
        advanceUntilIdle()
        s.onAction(SettingsAction.OpenEditProfileSheet)
        assertEquals("", s.uiState.value.editBioInput)
        s.onAction(SettingsAction.ChangeEditBio("b".repeat(70)))
        assertEquals(60, s.uiState.value.editBioInput.length)
    }

    @Test
    fun `tim kiem chua go thi goi y, go thi loc theo username hoac ten`() = runTest(dispatcher) {
        val vm = search()
        advanceUntilIdle()
        assertEquals(SampleData.users.size - 1, vm.uiState.value.userResults.size)

        vm.onAction(SearchAction.QueryChanged("khanh"))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.userResults.any { it.username == "khanhlinh" })
        assertTrue(vm.uiState.value.userResults.none { it.username == "hoangnam" })
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `tim kiem bam Ket ban thi gui loi moi that`() = runTest(dispatcher) {
        val stranger = strangerWithStatus(FriendStatus.NONE)
        val vm = search()
        advanceUntilIdle()
        vm.onAction(SearchAction.ToggleFriendRequest(stranger.id))
        advanceUntilIdle()
        assertEquals(FriendStatus.REQUEST_SENT, SampleSocialData.getFriendStatus(stranger.id))
        assertEquals(FriendStatus.REQUEST_SENT, vm.uiState.value.friendStatuses[stranger.id])
    }

    // ---------- Cài đặt ----------

    @Test
    fun `cai dat doc nguoi dang dang nhap va cong tac thong bao tu Repository`() = runTest(dispatcher) {
        val vm = settings()
        advanceUntilIdle()
        assertEquals(SampleData.currentUser.id, vm.uiState.value.currentUser?.id)
        vm.onAction(SettingsAction.ToggleNotifyMessages(false))
        advanceUntilIdle()
        assertFalse(vm.uiState.value.notifyMessages)
        assertFalse(SampleSocialData.settings.value.notifyMessages)
    }

    @Test
    fun `bo chan o Cai dat thi het chan`() = runTest(dispatcher) {
        val blocked = strangerWithStatus(FriendStatus.BLOCKED)
        val vm = settings()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.blockedUsers.any { it.id == blocked.id })
        vm.onAction(SettingsAction.UnblockUser(blocked.id))
        advanceUntilIdle()
        assertEquals(FriendStatus.NONE, SampleSocialData.getFriendStatus(blocked.id))
        assertTrue(vm.uiState.value.blockedUsers.none { it.id == blocked.id })
    }

    @Test
    fun `dang xuat thi xoa phien va ve man Dang nhap`() = runTest(dispatcher) {
        val vm = settings()
        vm.onAction(SettingsAction.ConfirmLogout)
        advanceUntilIdle()
        assertEquals(SettingsEffect.NavigateToLogin, withTimeoutOrNull(1000) { vm.effects.first() })
        assertNull(FakeAuthStore.currentUser.value)
    }

    @Test
    fun `sua ho so o Cai dat cap nhat nguoi dang dang nhap`() = runTest(dispatcher) {
        val vm = settings()
        advanceUntilIdle()
        vm.onAction(SettingsAction.OpenEditProfileSheet)
        vm.onAction(SettingsAction.ChangeEditDisplayName("Khôi Mới"))
        vm.onAction(SettingsAction.ChangeEditUsername("@khoi.moi"))
        vm.onAction(SettingsAction.SaveEditProfile)
        advanceUntilIdle()
        assertEquals("Khôi Mới", vm.uiState.value.currentUser?.displayName)
        assertEquals("khoi.moi", vm.uiState.value.currentUser?.username)
        assertFalse(vm.uiState.value.isEditProfileSheetOpen)
    }

    // ---------- Xoá tài khoản ----------

    @Test
    fun `xoa tai khoan chi bat khi go dung XOA`() {
        fun enabled(text: String) = SettingsUiState(deleteConfirmTextInput = text).isDeleteConfirmEnabled
        assertTrue(enabled("XOÁ"))
        assertTrue(enabled("XÓA"))            // kiểu đặt dấu mới
        assertTrue(enabled(" XOÁ "))
        assertTrue(enabled("XO" + "Á"))  // A + dấu sắc tổ hợp -> Á sau NFC
        assertFalse(enabled("xoá"))
        assertFalse(enabled("XOA"))
        assertFalse(enabled(""))
    }

    // ---------- Sửa tên 2–30 ký tự ----------

    @Test
    fun `sua ten chi nhan 2 den 30 ky tu`() = runTest(dispatcher) {
        val vm = profile(null)
        advanceUntilIdle()
        fun errorFor(name: String): String? {
            vm.onAction(ProfileAction.ChangeEditDisplayName(name))
            return vm.uiState.value.editDisplayNameError
        }
        assertNotNull(errorFor("A"))
        assertNotNull(errorFor("   A   "))
        assertNull(errorFor("An"))
        assertNull(errorFor("a".repeat(30)))
        assertNotNull(errorFor("a".repeat(31)))
    }

    @Test
    fun `luu ho so o trang ca nhan cap nhat ten va username`() = runTest(dispatcher) {
        val vm = profile(null)
        advanceUntilIdle()
        vm.onAction(ProfileAction.OpenEditProfileSheet)
        vm.onAction(ProfileAction.ChangeEditDisplayName("Khôi Mới"))
        vm.onAction(ProfileAction.ChangeEditUsername("@khoi.moi"))
        vm.onAction(ProfileAction.SaveEditProfile)
        advanceUntilIdle()
        assertEquals("Khôi Mới", vm.uiState.value.user?.displayName)
        assertEquals("khoi.moi", vm.uiState.value.user?.username)
        assertFalse(vm.uiState.value.isEditSheetOpen)
    }
}
