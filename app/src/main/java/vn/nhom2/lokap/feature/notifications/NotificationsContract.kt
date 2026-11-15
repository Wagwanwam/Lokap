package vn.nhom2.lokap.feature.notifications

import java.time.Instant
import vn.nhom2.lokap.domain.model.AppNotification
data class NotificationsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val unreadNotifications: List<AppNotification> = emptyList(),
    val earlierNotifications: List<AppNotification> = emptyList(),
    val handledFriendRequestIds: Map<String, Boolean> = emptyMap(),
    /** Bạn bè hiện tại: lời mời của người đã là bạn coi như đã chấp nhận (kể cả chấp nhận ở màn khác / máy khác). */
    val friendIds: Set<String> = emptySet(),
    val now: Instant = Instant.now()
) {
    /** null = lời mời còn chờ (hiện ✕ ✓); true = đã kết bạn; false = đã từ chối trong phiên này. */
    fun friendRequestDecision(notification: AppNotification): Boolean? =
        handledFriendRequestIds[notification.id]
            ?: notification.actor?.id?.takeIf { it in friendIds }?.let { true }

    val isEmpty: Boolean
        get() = unreadNotifications.isEmpty() && earlierNotifications.isEmpty()

    val hasUnread: Boolean
        get() = unreadNotifications.isNotEmpty()
}

sealed interface NotificationsAction {
    data class ClickNotification(val notification: AppNotification) : NotificationsAction
    data class ToggleRead(val notification: AppNotification) : NotificationsAction
    data class DeleteNotification(val id: String) : NotificationsAction
    data object MarkAllAsRead : NotificationsAction
    data class AcceptFriendRequest(val notificationId: String) : NotificationsAction
    data class DeclineFriendRequest(val notificationId: String) : NotificationsAction
    data object Retry : NotificationsAction
    data object NavigateBack : NotificationsAction
}

sealed interface NotificationsEffect {
    data class NavigateToPostDetail(val postId: String) : NotificationsEffect
    data object NavigateToFriends : NotificationsEffect
    data object NavigateToInbox : NotificationsEffect
    data class NavigateToChat(val conversationId: String) : NotificationsEffect
    data object NavigateBack : NotificationsEffect
}
