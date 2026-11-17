package vn.nhom2.lokap.feature

import androidx.lifecycle.viewModelScope
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import vn.nhom2.lokap.data.fake.FakeSocialRepository
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.data.fake.SampleSocialData
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.repository.AppException
import vn.nhom2.lokap.domain.repository.ChatData
import vn.nhom2.lokap.domain.repository.SocialGraph
import vn.nhom2.lokap.domain.repository.SocialRepository
import vn.nhom2.lokap.feature.inbox.InboxAction
import vn.nhom2.lokap.feature.inbox.InboxViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class InboxReliabilityTest {
    private val dispatcher = StandardTestDispatcher()
    private val content = ContentTestRepository()
    private val conversation = SampleData.conversations.first()
    private val models = mutableListOf<InboxViewModel>()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        SampleSocialData.resetAccountForDemo()
        content.chat.value = Result.success(ChatData(conversation, emptyList()))
    }

    @After fun cleanup() {
        models.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    private fun model(social: PendingRefresh) = InboxViewModel(content, social).also { models += it }

    @Test fun `cached inbox and incoming updates appear while friend refresh is suspended`() = runTest(dispatcher) {
        val social = PendingRefresh()
        val model = model(social)
        runCurrent()
        assertEquals(1, social.activeRefreshes)
        assertFalse(model.uiState.value.isLoading)
        assertEquals(conversation.id, model.uiState.value.items.single().conversation.id)

        val updated = conversation.copy(lastMessagePreview = "new message", unreadCount = 2)
        content.chat.value = Result.success(ChatData(updated, emptyList()))
        runCurrent()
        assertEquals("new message", model.uiState.value.items.single().displayPreview)
        assertEquals(2, model.uiState.value.items.single().conversation.unreadCount)
        assertEquals(1, social.activeRefreshes)
    }

    @Test fun `retry and opening friend picker share one pending refresh`() = runTest(dispatcher) {
        val social = PendingRefresh()
        val model = model(social)
        repeat(3) {
            model.onAction(InboxAction.Retry)
            model.onAction(InboxAction.OpenNewMessageSheet)
        }
        runCurrent()
        assertEquals(1, social.refreshCalls)
        assertEquals(1, social.maxActiveRefreshes)
        assertFalse(model.uiState.value.isLoading)

        social.gate.complete(Result.success(Unit))
        runCurrent()
        social.gate = CompletableDeferred()
        model.onAction(InboxAction.Retry)
        model.onAction(InboxAction.OpenNewMessageSheet)
        runCurrent()
        assertEquals(2, social.refreshCalls)
        assertEquals(1, social.maxActiveRefreshes)
    }

    @Test fun `friend refresh failure does not hide readable inbox or cached friends`() = runTest(dispatcher) {
        val social = PendingRefresh()
        val friend = conversation.friend
        social.graph.value = SocialGraph(mapOf(friend.id to FriendStatus.FRIENDS), mapOf(friend.id to friend))
        val model = model(social)
        runCurrent()
        social.gate.complete(Result.failure(AppException("friend refresh offline")))
        runCurrent()
        assertNull(model.uiState.value.errorMessage)
        assertEquals(conversation.id, model.uiState.value.items.single().conversation.id)
        assertEquals(listOf(friend), model.uiState.value.friends)
    }

    @Test fun `inbox failure is visible without waiting for refresh and survives its completion`() = runTest(dispatcher) {
        val social = PendingRefresh()
        content.chat.value = Result.failure(AppException("messages unavailable"))
        val model = model(social)
        runCurrent()
        assertFalse(model.uiState.value.isLoading)
        assertEquals("messages unavailable", model.uiState.value.errorMessage)
        social.gate.complete(Result.success(Unit))
        runCurrent()
        assertEquals("messages unavailable", model.uiState.value.errorMessage)

        content.chat.value = Result.success(ChatData(conversation, emptyList()))
        model.onAction(InboxAction.Retry)
        runCurrent()
        assertNull(model.uiState.value.errorMessage)
        assertEquals(conversation.id, model.uiState.value.items.single().conversation.id)
    }

    private class PendingRefresh : SocialRepository by FakeSocialRepository() {
        override val graph = MutableStateFlow(SocialGraph())
        var gate = CompletableDeferred<Result<Unit>>()
        var refreshCalls = 0
        var activeRefreshes = 0
        var maxActiveRefreshes = 0

        override suspend fun refresh(): Result<Unit> {
            refreshCalls++
            activeRefreshes++
            maxActiveRefreshes = maxOf(maxActiveRefreshes, activeRefreshes)
            return try { gate.await() } finally { activeRefreshes-- }
        }
    }
}
