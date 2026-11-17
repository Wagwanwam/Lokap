package vn.nhom2.lokap.feature

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import vn.nhom2.lokap.data.fake.FakeAuthRepository
import vn.nhom2.lokap.data.fake.FakeSocialRepository
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.data.fake.SampleSocialData
import vn.nhom2.lokap.domain.model.Message
import vn.nhom2.lokap.domain.model.Post
import vn.nhom2.lokap.domain.model.PostSendState
import vn.nhom2.lokap.domain.repository.AppException
import vn.nhom2.lokap.domain.repository.ChatData
import vn.nhom2.lokap.domain.repository.MessagingRepository
import vn.nhom2.lokap.feature.chat.ChatAction
import vn.nhom2.lokap.feature.chat.ChatViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class ChatReliabilityTest {
    private val dispatcher = StandardTestDispatcher()
    private val content = ContentTestRepository()
    private val messages = ControlledMessages(content)
    private val conversation = SampleData.conversations.first()
    private val models = mutableListOf<ChatViewModel>()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        SampleSocialData.resetAccountForDemo()
        content.chat.value = Result.success(ChatData(conversation.copy(unreadCount = 0), emptyList()))
    }
    @After fun cleanup() {
        models.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }
    private fun chatModel() = ChatViewModel(
        SavedStateHandle(mapOf("conversationId" to conversation.id)), messages, FakeAuthRepository(), FakeSocialRepository()
    ).also { models += it }
    private fun incoming(id: String) = Message(
        id, conversation.id, conversation.friend.id, "Incoming $id", null, null,
        Instant.parse("2026-11-17T10:00:00Z"), PostSendState.SENT, false
    )

    @Test fun `rapid send actions enqueue one mutation before dispatcher runs`() = runTest(dispatcher) {
        val model = chatModel()
        runCurrent()
        messages.sendGate = CompletableDeferred()
        model.onAction(ChatAction.InputChanged("hello"))
        model.onAction(ChatAction.SendMessage)
        model.onAction(ChatAction.SendMessage)
        assertTrue(model.uiState.value.isSaving)
        runCurrent()
        assertEquals(listOf("hello"), messages.sentTexts)
        messages.sendGate!!.complete(Result.success(Unit))
        runCurrent()
        assertEquals("", model.uiState.value.inputText)
        assertFalse(model.uiState.value.isSaving)
    }

    @Test fun `typing next draft while send waits does not lose it after success`() = runTest(dispatcher) {
        val model = chatModel()
        runCurrent()
        messages.sendGate = CompletableDeferred()
        model.onAction(ChatAction.InputChanged("first"))
        model.onAction(ChatAction.SendMessage)
        runCurrent()
        model.onAction(ChatAction.InputChanged("next draft"))
        messages.sendGate!!.complete(Result.success(Unit))
        runCurrent()
        assertEquals(listOf("first"), messages.sentTexts)
        assertEquals("next draft", model.uiState.value.inputText)
        assertTrue(model.uiState.value.canSend)
    }

    @Test fun `failed send keeps draft and observer retry never resends it`() = runTest(dispatcher) {
        val model = chatModel()
        runCurrent()
        messages.sendGate = CompletableDeferred(Result.failure(AppException("offline")))
        model.onAction(ChatAction.InputChanged("keep me"))
        model.onAction(ChatAction.SendMessage)
        runCurrent()
        model.onAction(ChatAction.RetryLoad)
        model.onAction(ChatAction.RetryMessage("pending"))
        runCurrent()
        assertEquals(listOf("keep me"), messages.sentTexts)
        assertEquals("keep me", model.uiState.value.inputText)
        messages.sendGate = null
        model.onAction(ChatAction.SendMessage)
        runCurrent()
        assertEquals(listOf("keep me", "keep me"), messages.sentTexts)
        assertEquals("", model.uiState.value.inputText)
    }

    @Test fun `chat only acknowledges new messages while visible`() = runTest(dispatcher) {
        content.chat.value = Result.success(ChatData(conversation.copy(unreadCount = 1), listOf(incoming("m1"))))
        val model = chatModel()
        runCurrent()
        assertEquals(0, messages.readCalls)
        model.onAction(ChatAction.VisibilityChanged(true))
        runCurrent()
        assertEquals(1, messages.readCalls)
        model.onAction(ChatAction.VisibilityChanged(false))
        content.chat.value = Result.success(ChatData(conversation.copy(unreadCount = 2), listOf(incoming("m1"), incoming("m2"))))
        runCurrent()
        assertEquals(1, messages.readCalls)
        model.onAction(ChatAction.VisibilityChanged(true))
        runCurrent()
        assertEquals(2, messages.readCalls)
        model.onAction(ChatAction.VisibilityChanged(true))
        runCurrent()
        assertEquals(2, messages.readCalls)
    }

    @Test fun `failed read receipt can retry the same incoming message`() = runTest(dispatcher) {
        content.chat.value = Result.success(ChatData(conversation.copy(unreadCount = 1), listOf(incoming("m1"))))
        messages.readResult = Result.failure(AppException("offline"))
        val model = chatModel()
        model.onAction(ChatAction.VisibilityChanged(true))
        runCurrent()
        assertEquals(1, messages.readCalls)
        assertEquals("offline", model.uiState.value.errorMessage)
        messages.readResult = Result.success(Unit)
        model.onAction(ChatAction.RetryLoad)
        runCurrent()
        assertEquals(2, messages.readCalls)
        assertNull(model.uiState.value.errorMessage)
        model.onAction(ChatAction.VisibilityChanged(true))
        runCurrent()
        assertEquals(2, messages.readCalls)
    }

    @Test fun `incoming while receipt is pending is acknowledged after current receipt finishes`() = runTest(dispatcher) {
        content.chat.value = Result.success(ChatData(conversation.copy(unreadCount = 1), listOf(incoming("m1"))))
        messages.readGate = CompletableDeferred()
        val model = chatModel()
        model.onAction(ChatAction.VisibilityChanged(true))
        runCurrent()
        content.chat.value = Result.success(ChatData(conversation.copy(unreadCount = 2), listOf(incoming("m1"), incoming("m2"))))
        runCurrent()
        assertEquals(1, messages.readCalls)
        messages.readGate!!.complete(Unit)
        runCurrent()
        assertEquals(2, messages.readCalls)
    }

    private class ControlledMessages(private val delegate: ContentTestRepository) : MessagingRepository by delegate {
        val sentTexts = mutableListOf<String>()
        var sendGate: CompletableDeferred<Result<Unit>>? = null
        var readCalls = 0
        var readResult = Result.success(Unit)
        var readGate: CompletableDeferred<Unit>? = null
        override suspend fun send(id: String, body: String, replyTo: Post?): Result<Unit> {
            sentTexts += body
            return sendGate?.await() ?: Result.success(Unit)
        }
        override suspend fun markRead(id: String): Result<Unit> {
            readCalls++
            readGate?.await()
            return readResult
        }
    }
}
