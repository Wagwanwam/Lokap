package vn.nhom2.lokap.domain.model

import androidx.compose.runtime.Immutable
import java.time.Instant
enum class NotificationType {
    NEW_POST,
    REACTION,
    FRIEND_REQUEST,
    FRIEND_ACCEPTED,
    NEW_MESSAGE,
    POST_HIDDEN
}

@Immutable
data class AppNotification(
    val id: String,
    val type: NotificationType,
    val actor: User?,
    val postId: String?,
    val text: String,
    val createdAt: Instant,
    val isRead: Boolean
)
