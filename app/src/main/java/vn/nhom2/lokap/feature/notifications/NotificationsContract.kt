package vn.nhom2.lokap.feature.notifications

import java.time.Instant
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.AppNotification

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val unreadNotifications: List<AppNotification> = emptyList(),
    val earlierNotifications: List<AppNotification> = emptyList(),
    val handledFriendRequestIds: Map<String, Boolean> = emptyMap(),
    val now: Instant = SampleData.NOW
) {
    val isEmpty: Boolean
        get() = unreadNotifications.isEmpty() && earlierNotifications.isEmpty()

    val hasUnread: Boolean
        get() = unreadNotifications.isNotEmpty()
}

sealed interface NotificationsAction {
    data class ClickNotification(val notification: AppNotification) : NotificationsAction
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
