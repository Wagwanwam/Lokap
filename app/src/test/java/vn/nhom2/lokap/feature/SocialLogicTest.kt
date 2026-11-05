package vn.nhom2.lokap.feature

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import vn.nhom2.lokap.data.fake.FakePostStore
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.data.fake.SampleSocialData
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.feature.friends.FriendsAction
import vn.nhom2.lokap.feature.friends.FriendsViewModel
import vn.nhom2.lokap.feature.profile.ProfileAction
import vn.nhom2.lokap.feature.profile.ProfileViewModel
import vn.nhom2.lokap.feature.settings.SettingsUiState
import java.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class SocialLogicTest {

    private lateinit var dispatcher: TestDispatcher

    @Before
    fun setUp() {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        SampleSocialData.resetAccountForDemo()
    }

    @After
    fun tearDown() {
        SampleSocialData.resetAccountForDemo()
        Dispatchers.resetMain()
    }

    private fun profile(userId: String?) =
        ProfileViewModel(SavedStateHandle(if (userId == null) emptyMap() else mapOf("userId" to userId)))

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
    fun `duoi 20 ban thi gui loi moi duoc`() {
        val stranger = SampleSocialData.directoryUsers.first {
            it.id != SampleData.currentUser.id && SampleSocialData.getFriendStatus(it.id) == FriendStatus.NONE
        }
        assertTrue(SampleSocialData.sendFriendRequest(stranger.id))
        assertEquals(FriendStatus.REQUEST_SENT, SampleSocialData.getFriendStatus(stranger.id))
    }

    // ---------- Tìm bạn ----------

    @Test
    fun `tim ban khong hien nguoi da chan`() = runTest(dispatcher) {
        val blocked = SampleSocialData.directoryUsers.first { SampleSocialData.getFriendStatus(it.id) == FriendStatus.BLOCKED }
        val vm = FriendsViewModel()
        vm.onAction(FriendsAction.ChangeSearchQuery(blocked.username))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.searchResults.none { it.user.id == blocked.id })
    }

    // ---------- Xoá tài khoản ----------

    @Test
    fun `xoa tai khoan chi bat khi go dung XOA`() {
        fun enabled(text: String) = SettingsUiState(deleteConfirmTextInput = text).isDeleteConfirmEnabled
        assertTrue(enabled("XOÁ"))
        assertTrue(enabled("XÓA"))            // kiểu đặt dấu mới
        assertTrue(enabled(" XOÁ "))
        assertTrue(enabled("XO" + "Á"))  // A + dấu sắc tổ hợp -> Á sau NFC
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
}
