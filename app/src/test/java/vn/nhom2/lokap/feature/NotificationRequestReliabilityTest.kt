package vn.nhom2.lokap.feature

import androidx.lifecycle.viewModelScope
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import vn.nhom2.lokap.data.fake.FakeSocialRepository
import vn.nhom2.lokap.data.fake.FakeAuthRepository
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.AppNotification
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.model.NotificationType
import vn.nhom2.lokap.domain.model.User
import vn.nhom2.lokap.domain.repository.AppException
import vn.nhom2.lokap.domain.repository.AuthRepository
import vn.nhom2.lokap.domain.repository.SocialGraph
import vn.nhom2.lokap.domain.repository.SocialRepository
import vn.nhom2.lokap.feature.notifications.NotificationsAction
import vn.nhom2.lokap.feature.notifications.NotificationsViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationRequestReliabilityTest {
    private val dispatcher = StandardTestDispatcher()
    private val content = ContentTestRepository()
    private val requester = User("requester", "requester", "Requester", null)
    private val social = ControlledSocial(requester)
    private val auth = ControlledAuth()
    private val models = mutableListOf<NotificationsViewModel>()
    private val first = AppNotification(
        "request-1", NotificationType.FRIEND_REQUEST, requester, null, "Request", Instant.parse("2026-11-17T10:00:00Z"), false
    )

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        content.notices.value = Result.success(listOf(first))
    }
    @After fun cleanup() {
        models.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }
    private fun model() = NotificationsViewModel(content, social, auth).also { models += it }

    @Test fun `repeated accept and opposing decline only mutate request once`() = runTest(dispatcher) {
        val model = model()
        runCurrent()
        social.actionGate = CompletableDeferred()
        model.onAction(NotificationsAction.AcceptFriendRequest(first.id))
        model.onAction(NotificationsAction.AcceptFriendRequest(first.id))
        model.onAction(NotificationsAction.DeclineFriendRequest(first.id))
        assertEquals(setOf(requester.id), model.uiState.value.pendingFriendRequestActorIds)
        runCurrent()
        assertEquals(1, social.acceptCalls)
        assertEquals(0, social.declineCalls)
        social.actionGate!!.complete(Result.success(Unit))
        runCurrent()
        assertTrue(model.uiState.value.pendingFriendRequestActorIds.isEmpty())
        assertEquals(true, model.uiState.value.friendRequestDecision(first))
        assertTrue(content.notices.value.getOrThrow().single().isRead)
        model.onAction(NotificationsAction.AcceptFriendRequest(first.id))
        runCurrent()
        assertEquals(1, social.acceptCalls)
    }

    @Test fun `failed request action unlocks row and can retry`() = runTest(dispatcher) {
        val model = model()
        runCurrent()
        social.actionGate = CompletableDeferred(Result.failure(AppException("offline")))
        model.onAction(NotificationsAction.DeclineFriendRequest(first.id))
        runCurrent()
        assertTrue(model.uiState.value.pendingFriendRequestActorIds.isEmpty())
        assertNull(model.uiState.value.friendRequestDecision(first))
        assertFalse(content.notices.value.getOrThrow().single().isRead)
        social.actionGate = null
        model.onAction(NotificationsAction.DeclineFriendRequest(first.id))
        runCurrent()
        assertEquals(2, social.declineCalls)
        assertEquals(false, model.uiState.value.friendRequestDecision(first))
    }

    @Test fun `history stays visible but only newest request can act on current relationship`() = runTest(dispatcher) {
        val newest = first.copy(id = "request-2", createdAt = first.createdAt.plusSeconds(60))
        content.notices.value = Result.success(listOf(newest, first.copy(isRead = true)))
        val model = model()
        runCurrent()
        val state = model.uiState.value
        assertEquals(2, state.unreadNotifications.size + state.earlierNotifications.size)
        assertEquals(false, state.friendRequestDecision(first))
        assertNull(state.friendRequestDecision(newest))
        model.onAction(NotificationsAction.AcceptFriendRequest(first.id))
        runCurrent()
        assertEquals(0, social.acceptCalls)
        model.onAction(NotificationsAction.AcceptFriendRequest(newest.id))
        runCurrent()
        assertEquals(1, social.acceptCalls)
    }

    @Test fun `failed graph refresh is not treated as a confirmed closed request and retry refreshes`() = runTest(dispatcher) {
        social.refreshResult = Result.failure(AppException("offline"))
        social.graph.value = SocialGraph()
        val model = model()
        runCurrent()
        assertFalse(model.uiState.value.isGraphChecked)
        assertNull(model.uiState.value.friendRequestDecision(first))
        assertEquals(1, social.refreshCalls)
        social.refreshResult = Result.success(Unit)
        model.onAction(NotificationsAction.Retry)
        runCurrent()
        assertEquals(2, social.refreshCalls)
        assertTrue(model.uiState.value.isGraphChecked)
        assertEquals(false, model.uiState.value.friendRequestDecision(first))
    }

    @Test fun `new invitation from previously checked actor refreshes relationship again`() = runTest(dispatcher) {
        social.graph.value = SocialGraph()
        val model = model()
        runCurrent()
        assertEquals(1, social.refreshCalls)
        val newest = first.copy(id = "request-2", createdAt = first.createdAt.plusSeconds(60))
        social.graph.value = SocialGraph(mapOf(requester.id to FriendStatus.REQUEST_RECEIVED), mapOf(requester.id to requester))
        content.notices.value = Result.success(listOf(newest, first))
        runCurrent()
        assertEquals(2, social.refreshCalls)
        assertNull(model.uiState.value.friendRequestDecision(newest))
        assertEquals(false, model.uiState.value.friendRequestDecision(first))
    }

    @Test fun `deleting newest row and retrying never reactivates historical request`() = runTest(dispatcher) {
        val newest = first.copy(id = "request-2", createdAt = first.createdAt.plusSeconds(60))
        content.notices.value = Result.success(listOf(newest, first))
        val model = model()
        runCurrent()
        // Firestore's next snapshot after deleting the newest notification still contains the old row.
        content.notices.value = Result.success(listOf(first))
        runCurrent()
        model.onAction(NotificationsAction.Retry)
        runCurrent()
        assertEquals(newest.id, model.uiState.value.newestSeenFriendRequests[requester.id]?.notificationId)
        assertEquals(false, model.uiState.value.friendRequestDecision(first))
        model.onAction(NotificationsAction.AcceptFriendRequest(first.id))
        model.onAction(NotificationsAction.DeclineFriendRequest(first.id))
        runCurrent()
        assertEquals(0, social.acceptCalls)
        assertEquals(0, social.declineCalls)
    }

    @Test fun `a genuinely newer invitation remains actionable after earlier newest was deleted`() = runTest(dispatcher) {
        val second = first.copy(id = "request-2", createdAt = first.createdAt.plusSeconds(60))
        content.notices.value = Result.success(listOf(second, first))
        val model = model()
        runCurrent()
        content.notices.value = Result.success(listOf(first))
        runCurrent()
        val third = first.copy(id = "request-3", createdAt = first.createdAt.plusSeconds(120))
        content.notices.value = Result.success(listOf(third, first))
        runCurrent()
        assertNull(model.uiState.value.friendRequestDecision(third))
        assertEquals(false, model.uiState.value.friendRequestDecision(first))
        model.onAction(NotificationsAction.AcceptFriendRequest(third.id))
        runCurrent()
        assertEquals(1, social.acceptCalls)
    }

    @Test fun `changing account clears newest seen markers and permits that accounts own request`() = runTest(dispatcher) {
        val newest = first.copy(id = "request-2", createdAt = first.createdAt.plusSeconds(60))
        content.notices.value = Result.success(listOf(newest, first))
        val model = model()
        runCurrent()
        // The second account can have an older request from the same actor; do not inherit the first account's marker.
        content.notices.value = Result.success(listOf(first))
        auth.currentUser.value = SampleData.currentUser.copy(id = "other-account")
        runCurrent()
        assertEquals(first.id, model.uiState.value.newestSeenFriendRequests[requester.id]?.notificationId)
        assertNull(model.uiState.value.friendRequestDecision(first))
        model.onAction(NotificationsAction.AcceptFriendRequest(first.id))
        runCurrent()
        assertEquals(1, social.acceptCalls)
        auth.currentUser.value = null
        runCurrent()
        assertTrue(model.uiState.value.newestSeenFriendRequests.isEmpty())
        assertTrue(model.uiState.value.handledFriendRequestIds.isEmpty())
        assertTrue(model.uiState.value.isEmpty)
    }

    @Test fun `completion from old account cannot mark a request handled in the new account`() = runTest(dispatcher) {
        val model = model()
        runCurrent()
        social.actionGate = CompletableDeferred()
        model.onAction(NotificationsAction.AcceptFriendRequest(first.id))
        runCurrent()
        content.notices.value = Result.success(emptyList())
        auth.currentUser.value = SampleData.currentUser.copy(id = "other-account")
        runCurrent()
        social.actionGate!!.complete(Result.success(Unit))
        runCurrent()
        assertTrue(model.uiState.value.newestSeenFriendRequests.isEmpty())
        assertTrue(model.uiState.value.handledFriendRequestIds.isEmpty())
        assertTrue(model.uiState.value.pendingFriendRequestActorIds.isEmpty())
    }

    @Test fun `fresh viewmodel rejects old notification when current pending invitation is newer`() = runTest(dispatcher) {
        // The newer notification was deleted before this process/screen was created, so no watermark exists for it.
        social.graph.value = social.graph.value.copy(
            pendingRequestCreatedAt = mapOf(requester.id to first.createdAt.plusSeconds(60))
        )
        val model = model()
        runCurrent()
        assertTrue(model.uiState.value.isGraphChecked)
        assertEquals(first.id, model.uiState.value.newestSeenFriendRequests[requester.id]?.notificationId)
        assertEquals(false, model.uiState.value.friendRequestDecision(first))
        model.onAction(NotificationsAction.AcceptFriendRequest(first.id))
        model.onAction(NotificationsAction.DeclineFriendRequest(first.id))
        runCurrent()
        assertEquals(0, social.acceptCalls)
        assertEquals(0, social.declineCalls)
    }

    @Test fun `matching server creation time identifies current invitation and permits accepting it`() = runTest(dispatcher) {
        social.graph.value = social.graph.value.copy(pendingRequestCreatedAt = mapOf(requester.id to first.createdAt))
        val model = model()
        runCurrent()
        assertNull(model.uiState.value.friendRequestDecision(first))
        model.onAction(NotificationsAction.AcceptFriendRequest(first.id))
        runCurrent()
        assertEquals(1, social.acceptCalls)
    }

    @Test fun `request cannot mutate relationship before graph verification finishes`() = runTest(dispatcher) {
        social.refreshGate = CompletableDeferred()
        val model = model()
        runCurrent()
        assertFalse(model.uiState.value.isGraphChecked)
        model.onAction(NotificationsAction.AcceptFriendRequest(first.id))
        model.onAction(NotificationsAction.DeclineFriendRequest(first.id))
        runCurrent()
        assertEquals(0, social.acceptCalls)
        assertEquals(0, social.declineCalls)
        social.refreshGate!!.complete(Result.success(Unit))
        runCurrent()
        model.onAction(NotificationsAction.AcceptFriendRequest(first.id))
        runCurrent()
        assertEquals(1, social.acceptCalls)
    }

    private class ControlledAuth : AuthRepository by FakeAuthRepository() {
        override val currentUser = MutableStateFlow<User?>(SampleData.currentUser)
    }

    private class ControlledSocial(private val requester: User) : SocialRepository by FakeSocialRepository() {
        override val graph = MutableStateFlow(
            SocialGraph(mapOf(requester.id to FriendStatus.REQUEST_RECEIVED), mapOf(requester.id to requester))
        )
        var refreshCalls = 0
        var refreshResult = Result.success(Unit)
        var refreshGate: CompletableDeferred<Result<Unit>>? = null
        var acceptCalls = 0
        var declineCalls = 0
        var actionGate: CompletableDeferred<Result<Unit>>? = null
        override suspend fun refresh(): Result<Unit> { refreshCalls++; return refreshGate?.await() ?: refreshResult }
        override suspend fun acceptFriendRequest(userId: String): Result<Unit> {
            acceptCalls++
            return finish(FriendStatus.FRIENDS)
        }
        override suspend fun declineFriendRequest(userId: String): Result<Unit> {
            declineCalls++
            return finish(FriendStatus.NONE)
        }
        private suspend fun finish(status: FriendStatus): Result<Unit> {
            val result = actionGate?.await() ?: Result.success(Unit)
            if (result.isSuccess) graph.value = graph.value.copy(statuses = mapOf(requester.id to status))
            return result
        }
    }
}
