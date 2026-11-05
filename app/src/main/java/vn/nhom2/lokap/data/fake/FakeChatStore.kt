package vn.nhom2.lokap.data.fake

import vn.nhom2.lokap.domain.model.FriendStatus
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import vn.nhom2.lokap.domain.model.Conversation
import vn.nhom2.lokap.domain.model.Message
import vn.nhom2.lokap.domain.model.PostSendState
import vn.nhom2.lokap.domain.model.User

object FakeChatStore {

    private val startedAtMillis = System.currentTimeMillis()

    /**
     * Đồng hồ giả: SampleData.NOW + thời gian đã trôi từ lúc mở app. Cả app dùng NOW cố định,
     * nếu tin nhắn mới dùng Instant.now() thì bị xếp sang ngày khác ("06/11" thay vì "Hôm nay").
     */
    fun clockNow(): Instant = SampleData.NOW.plusMillis(System.currentTimeMillis() - startedAtMillis)

    private val _conversations = MutableStateFlow(SampleData.conversations)
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _messages = MutableStateFlow(SampleData.messages)
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    /** Tổng tin chưa đọc, dùng cho badge trên Camera. */
    val totalUnread: Flow<Int> = conversations.map { list -> list.sumOf { it.unreadCount } }

    fun getConversation(conversationId: String): Conversation? {
        return _conversations.value.find { it.id == conversationId }
    }

    fun getOrCreateConversationWithFriend(friend: User): Conversation {
        val existing = _conversations.value.find { it.friend.id == friend.id }
        if (existing != null) return existing

        val newConversation = Conversation(
            id = "conv_${friend.id}",
            friend = friend,
            lastMessagePreview = "",
            lastMessageAt = SampleData.NOW,
            unreadCount = 0
        )
        _conversations.update { current -> listOf(newConversation) + current }
        return newConversation
    }

    fun markConversationRead(conversationId: String) {
        _conversations.update { list ->
            list.map { conv ->
                if (conv.id == conversationId && conv.unreadCount > 0) {
                    conv.copy(unreadCount = 0)
                } else {
                    conv
                }
            }
        }
    }

    /**
     * Giống RPC send_message: chỉ gửi được khi hai người còn là bạn (không chặn nhau).
     * Trả về null nếu bị từ chối hoặc nội dung rỗng.
     */
    fun addOutgoingMessage(
        conversationId: String,
        senderId: String,
        body: String,
        createdAt: Instant = clockNow()
    ): Message? {
        val conversation = getConversation(conversationId) ?: return null
        if (SampleSocialData.getFriendStatus(conversation.friend.id) != FriendStatus.FRIENDS) return null
        val trimmed = body.trim().take(1000)
        if (trimmed.isEmpty()) return null
        val message = Message(
            id = "msg_${UUID.randomUUID()}",
            conversationId = conversationId,
            senderId = senderId,
            body = trimmed,
            replyToPost = null,
            attachedPost = null,
            createdAt = createdAt,
            sendState = PostSendState.PENDING,
            seen = false
        )
        _messages.update { current -> current + message }
        updateConversationPreview(
            conversationId = conversationId,
            preview = previewForMessage(message),
            timestamp = createdAt,
            incrementUnread = false
        )
        return message
    }

    fun updateMessageSendState(messageId: String, newState: PostSendState, seen: Boolean = false) {
        _messages.update { list ->
            list.map { msg ->
                if (msg.id == messageId) {
                    msg.copy(sendState = newState, seen = seen)
                } else {
                    msg
                }
            }
        }
    }

    fun addIncomingReply(
        conversationId: String,
        friendId: String,
        body: String,
        createdAt: Instant = clockNow(),
        isChatOpen: Boolean = true
    ): Message {
        val message = Message(
            id = "msg_${UUID.randomUUID()}",
            conversationId = conversationId,
            senderId = friendId,
            body = body,
            replyToPost = null,
            attachedPost = null,
            createdAt = createdAt,
            sendState = PostSendState.SENT,
            seen = isChatOpen
        )
        _messages.update { current -> current + message }
        updateConversationPreview(
            conversationId = conversationId,
            preview = previewForMessage(message),
            timestamp = createdAt,
            incrementUnread = !isChatOpen
        )
        return message
    }

    fun previewForConversation(conversation: Conversation, allMessages: List<Message>): String {
        val latest = allMessages
            .filter { it.conversationId == conversation.id }
            .maxByOrNull { it.createdAt }
        return when {
            latest == null -> conversation.lastMessagePreview
            latest.attachedPost != null -> "📷 Đã gửi một ảnh"
            !latest.body.isNullOrBlank() -> latest.body
            else -> conversation.lastMessagePreview
        }
    }

    private fun previewForMessage(message: Message): String {
        return when {
            message.attachedPost != null -> "📷 Đã gửi một ảnh"
            !message.body.isNullOrBlank() -> message.body
            else -> ""
        }
    }

    private fun updateConversationPreview(
        conversationId: String,
        preview: String,
        timestamp: Instant,
        incrementUnread: Boolean
    ) {
        _conversations.update { list ->
            list.map { conv ->
                if (conv.id == conversationId) {
                    conv.copy(
                        lastMessagePreview = preview,
                        lastMessageAt = timestamp,
                        unreadCount = if (incrementUnread) conv.unreadCount + 1 else conv.unreadCount
                    )
                } else {
                    conv
                }
            }
        }
    }
}

/** Chỉ dùng trong unit test. */
internal fun FakeChatStore.lastMessageIn(conversationId: String): Message? =
    messages.value.filter { it.conversationId == conversationId }.maxByOrNull { it.createdAt }
