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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import vn.nhom2.lokap.data.fake.FakeAuthRepository
import vn.nhom2.lokap.data.fake.FakeAuthStore
import vn.nhom2.lokap.data.fake.FakeSocialRepository
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.data.fake.SampleSocialData
import vn.nhom2.lokap.domain.model.FriendCode
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.model.User
import vn.nhom2.lokap.domain.repository.SocialRepository
import vn.nhom2.lokap.feature.addfriend.AddFriendAction
import vn.nhom2.lokap.feature.addfriend.AddFriendEffect
import vn.nhom2.lokap.feature.addfriend.AddFriendViewModel
import vn.nhom2.lokap.feature.addfriend.FriendLookup

@OptIn(ExperimentalCoroutinesApi::class)
class AddFriendLogicTest {

    private lateinit var dispatcher: TestDispatcher

    @Before
    fun setUp() {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        SampleSocialData.resetAccountForDemo()
        FakeAuthStore.currentUser.value = SampleData.currentUser
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        username: String? = null,
        social: SocialRepository = FakeSocialRepository()
    ) = AddFriendViewModel(
        SavedStateHandle(mapOf(AddFriendViewModel.ARG_USERNAME to username)),
        FakeAuthRepository(),
        social
    )

    private fun strangerWithStatus(status: FriendStatus): User = SampleSocialData.directoryUsers.first {
        it.id != SampleData.currentUser.id && it.username.matches(Regex("[a-z0-9._]{3,20}")) &&
            SampleSocialData.getFriendStatus(it.id) == status
    }

    // ---------- Mã QR / link mời ----------

    @Test
    fun `doc username tu link moi`() {
        assertEquals("vy.matcha", FriendCode.parse("lokap://add/vy.matcha"))
        assertEquals("vy.matcha", FriendCode.parse("  LOKAP://add/Vy.Matcha/ "))
        assertEquals("vy.matcha", FriendCode.parse("lokap://add/vy.matcha?ref=qr"))
        assertEquals("hieu.film", FriendCode.parse("@hieu.film"))
    }

    @Test
    fun `ma khong phai cua Lokap thi bo qua`() {
        assertNull(FriendCode.parse("https://example.com/vy.matcha"))
        assertNull(FriendCode.parse("vy.matcha"))
        assertNull(FriendCode.parse("lokap://add/ab"))
        assertNull(FriendCode.parse("lokap://add/ten co dau cach"))
        assertNull(FriendCode.parse("lokap://add/"))
        assertNull(FriendCode.parse("lokap://profile/vy.matcha"))
    }

    @Test
    fun `link tao ra doc lai dung username`() {
        val link = FriendCode.linkFor("@Minh.Khoi_99")
        assertEquals("lokap://add/minh.khoi_99", link)
        assertEquals("minh.khoi_99", FriendCode.parse(link))
        assertTrue(FriendCode.inviteText("minh.khoi_99").contains(link))
    }

    // ---------- Màn Thêm bạn ----------

    @Test
    fun `mo tu link moi thi hien nguoi do`() = runTest(dispatcher) {
        val stranger = strangerWithStatus(FriendStatus.NONE)
        val vm = viewModel(username = FriendCode.linkFor(stranger.username).substringAfterLast('/'))
        advanceUntilIdle()
        val found = vm.uiState.value.lookup as FriendLookup.Found
        assertEquals(stranger.id, found.user.id)
        assertEquals(FriendStatus.NONE, found.status)
        assertFalse(found.isSelf)
    }

    @Test
    fun `quet ma roi ket ban thi nut doi sang da gui`() = runTest(dispatcher) {
        val stranger = strangerWithStatus(FriendStatus.NONE)
        val vm = viewModel()
        vm.onAction(AddFriendAction.LookUp(stranger.username))
        advanceUntilIdle()
        vm.onAction(AddFriendAction.SendRequest)
        advanceUntilIdle()
        val found = vm.uiState.value.lookup as FriendLookup.Found
        assertEquals(FriendStatus.REQUEST_SENT, found.status)
        assertFalse(found.isWorking)
        assertEquals(FriendStatus.REQUEST_SENT, SampleSocialData.getFriendStatus(stranger.id))
    }

    @Test
    fun `nguoi da moi minh thi chap nhan thanh ban`() = runTest(dispatcher) {
        val requester = strangerWithStatus(FriendStatus.REQUEST_RECEIVED)
        val vm = viewModel()
        vm.onAction(AddFriendAction.LookUp(requester.username))
        advanceUntilIdle()
        assertEquals(FriendStatus.REQUEST_RECEIVED, (vm.uiState.value.lookup as FriendLookup.Found).status)
        vm.onAction(AddFriendAction.AcceptRequest)
        advanceUntilIdle()
        assertEquals(FriendStatus.FRIENDS, (vm.uiState.value.lookup as FriendLookup.Found).status)
    }

    @Test
    fun `username khong ton tai thi bao khong tim thay`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onAction(AddFriendAction.LookUp("khong.co.ai"))
        advanceUntilIdle()
        assertEquals(FriendLookup.NotFound("khong.co.ai"), vm.uiState.value.lookup)
    }

    @Test
    fun `quet ma cua chinh minh thi khong co nut ket ban`() = runTest(dispatcher) {
        val me = SampleData.currentUser
        val social = object : SocialRepository by FakeSocialRepository() {
            override suspend fun findByUsername(username: String) = Result.success<User?>(me)
        }
        val vm = viewModel(social = social)
        vm.onAction(AddFriendAction.LookUp(me.username))
        advanceUntilIdle()
        val found = vm.uiState.value.lookup as FriendLookup.Found
        assertTrue(found.isSelf)
        vm.onAction(AddFriendAction.SendRequest)
        advanceUntilIdle()
        assertEquals(FriendStatus.NONE, SampleSocialData.getFriendStatus(me.id))
    }

    @Test
    fun `chia se gui link moi cua minh`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onAction(AddFriendAction.ShareInvite)
        val effect = withTimeoutOrNull(1_000) { vm.effects.first() } as AddFriendEffect.ShareInvite
        assertTrue(effect.text, effect.text.contains(FriendCode.linkFor(SampleData.currentUser.username)))
    }

    @Test
    fun `dong sheet thi xoa ket qua`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onAction(AddFriendAction.LookUp(strangerWithStatus(FriendStatus.NONE).username))
        advanceUntilIdle()
        vm.onAction(AddFriendAction.DismissLookup)
        assertNull(vm.uiState.value.lookup)
    }
}
