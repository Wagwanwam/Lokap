package vn.nhom2.lokap.domain.model

import androidx.compose.runtime.Immutable
import java.time.Instant

@Immutable
data class Conversation(
    val id: String,
    val friend: User,
    val lastMessagePreview: String,
    val lastMessageAt: Instant,
    val unreadCount: Int
)

@Immutable
data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val body: String?,
    val replyToPost: Post?,
    val attachedPost: Post?,
    val createdAt: Instant,
    val sendState: PostSendState,
    val seen: Boolean
)
