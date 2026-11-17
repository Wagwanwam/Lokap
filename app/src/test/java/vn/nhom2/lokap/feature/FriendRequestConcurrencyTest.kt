package vn.nhom2.lokap.feature

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import vn.nhom2.lokap.data.fake.FakeAuthRepository
import vn.nhom2.lokap.data.fake.FakeAuthStore
import vn.nhom2.lokap.data.fake.FakePostRepository
import vn.nhom2.lokap.data.fake.FakeSocialRepository
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.data.fake.SampleSocialData
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.repository.SocialGraph
import vn.nhom2.lokap.domain.repository.SocialRepository
import vn.nhom2.lokap.feature.addfriend.AddFriendAction
import vn.nhom2.lokap.feature.addfriend.AddFriendViewModel
import vn.nhom2.lokap.feature.addfriend.FriendLookup
import vn.nhom2.lokap.feature.friends.FriendsAction
import vn.nhom2.lokap.feature.friends.FriendsViewModel
import vn.nhom2.lokap.feature.profile.ProfileAction
import vn.nhom2.lokap.feature.profile.ProfileViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class FriendRequestConcurrencyTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
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

    private class PendingSocial : SocialRepository by FakeSocialRepository() {
        val users = SampleSocialData.directoryUsers.filter { it.id != SampleData.currentUser.id }
        override val graph = MutableStateFlow(SocialGraph(users = users.associateBy { it.id }))
        val sends = mutableMapOf<String, Int>()
        val completions = mutableMapOf<String, CompletableDeferred<Result<FriendStatus>>>()
        var cancels = 0

        override suspend fun refresh() = Result.success(Unit)
        override suspend fun findByUsername(username: String) = Result.success(users.firstOrNull { it.username == username })
        override suspend fun getUser(userId: String) = Result.success(users.firstOrNull { it.id == userId })
        override suspend fun getFriendCount(userId: String) = Result.success(0)
        override suspend fun sendFriendRequest(userId: String): Result<FriendStatus> {
            sends[userId] = (sends[userId] ?: 0) + 1
            val result = completions.getOrPut(userId) { CompletableDeferred() }.await()
            result.onSuccess { status -> graph.update { it.copy(statuses = it.statuses + (userId to status)) } }
            return result
        }
        override suspend fun cancelFriendRequest(userId: String): Result<Unit> {
            cancels++
            graph.update { it.copy(statuses = it.statuses - userId) }
            return Result.success(Unit)
        }
        fun finish(userId: String, result: Result<FriendStatus> = Result.success(FriendStatus.REQUEST_SENT)) {
            completions.getValue(userId).complete(result)
        }
    }

    @Test
    fun `friends ignores repeated taps and stale send while allowing cancel then resend`() = runTest(dispatcher) {
        val social = PendingSocial()
        val target = social.users.first().id
        val vm = FriendsViewModel(social)
        vm.onAction(FriendsAction.SendRequest(target))
        vm.onAction(FriendsAction.SendRequest(target))
        vm.onAction(FriendsAction.CancelSentRequest(target))
        assertEquals(1, social.sends[target])
        assertEquals(0, social.cancels)
        assertTrue(target in vm.uiState.value.workingUserIds)

        social.finish(target)
        vm.onAction(FriendsAction.SendRequest(target))
        assertEquals(1, social.sends[target])
        assertFalse(target in vm.uiState.value.workingUserIds)
        vm.onAction(FriendsAction.CancelSentRequest(target))
        assertEquals(1, social.cancels)
        social.completions[target] = CompletableDeferred()
        vm.onAction(FriendsAction.SendRequest(target))
        assertEquals(2, social.sends[target])
        social.finish(target)
    }

    @Test
    fun `friends releases pending state after failure and permits retry`() = runTest(dispatcher) {
        val social = PendingSocial()
        val target = social.users.first().id
        val vm = FriendsViewModel(social)
        vm.onAction(FriendsAction.SendRequest(target))
        social.finish(target, Result.failure(IllegalStateException("offline")))
        assertFalse(target in vm.uiState.value.workingUserIds)
        social.completions[target] = CompletableDeferred()
        vm.onAction(FriendsAction.SendRequest(target))
        assertEquals(2, social.sends[target])
        social.finish(target)
    }

    @Test
    fun `friends can send to different people independently`() = runTest(dispatcher) {
        val social = PendingSocial()
        val targets = social.users.take(2).map { it.id }
        val vm = FriendsViewModel(social)
        targets.forEach { vm.onAction(FriendsAction.SendRequest(it)) }
        assertEquals(targets.toSet(), vm.uiState.value.workingUserIds)
        targets.forEach { assertEquals(1, social.sends[it]); social.finish(it) }
        assertTrue(vm.uiState.value.workingUserIds.isEmpty())
    }

    @Test
    fun `profile locks relationship actions before a second tap`() = runTest(dispatcher) {
        val social = PendingSocial()
        val target = social.users.first().id
        val vm = ProfileViewModel(SavedStateHandle(mapOf("userId" to target)), FakeAuthRepository(),
            ContentTestRepository(), social, FakePostRepository())
        advanceUntilIdle()
        vm.onAction(ProfileAction.SendFriendRequest)
        vm.onAction(ProfileAction.SendFriendRequest)
        vm.onAction(ProfileAction.CancelFriendRequest)
        assertEquals(1, social.sends[target])
        assertEquals(0, social.cancels)
        assertTrue(vm.uiState.value.isUpdatingFriendship)
        social.finish(target)
        assertFalse(vm.uiState.value.isUpdatingFriendship)
        vm.onAction(ProfileAction.SendFriendRequest)
        assertEquals(1, social.sends[target])
    }

    @Test
    fun `reopening add friend sheet retains pending guard for that person`() = runTest(dispatcher) {
        val social = PendingSocial()
        val target = social.users.first()
        val vm = AddFriendViewModel(SavedStateHandle(), FakeAuthRepository(), social)
        vm.onAction(AddFriendAction.LookUp(target.username))
        vm.onAction(AddFriendAction.SendRequest)
        vm.onAction(AddFriendAction.DismissLookup)
        vm.onAction(AddFriendAction.LookUp(target.username))
        assertTrue((vm.uiState.value.lookup as FriendLookup.Found).isWorking)
        vm.onAction(AddFriendAction.SendRequest)
        assertEquals(1, social.sends[target.id])
        social.finish(target.id)
        assertFalse((vm.uiState.value.lookup as FriendLookup.Found).isWorking)
    }

    @Test
    fun `completion of old add friend sheet does not unlock another person`() = runTest(dispatcher) {
        val social = PendingSocial()
        val (first, second) = social.users.take(2)
        val vm = AddFriendViewModel(SavedStateHandle(), FakeAuthRepository(), social)
        vm.onAction(AddFriendAction.LookUp(first.username))
        vm.onAction(AddFriendAction.SendRequest)
        vm.onAction(AddFriendAction.LookUp(second.username))
        vm.onAction(AddFriendAction.SendRequest)
        social.finish(first.id)
        val found = vm.uiState.value.lookup as FriendLookup.Found
        assertEquals(second.id, found.user.id)
        assertTrue(found.isWorking)
        vm.onAction(AddFriendAction.SendRequest)
        assertEquals(1, social.sends[second.id])
        social.finish(second.id)
    }
}
