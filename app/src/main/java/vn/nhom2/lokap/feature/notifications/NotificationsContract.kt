package vn.nhom2.lokap.feature.notifications

import java.time.Instant
import vn.nhom2.lokap.domain.model.AppNotification
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.model.NotificationType

/** High-water mark for this screen session; deleting a row must not revive an older invitation. */
data class FriendRequestStamp(val notificationId: String, val createdAt: Instant)

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val unreadNotifications: List<AppNotification> = emptyList(),
    val earlierNotifications: List<AppNotification> = emptyList(),
    val handledFriendRequestIds: Map<String, Boolean> = emptyMap(),
    val pendingFriendRequestActorIds: Set<String> = emptySet(),
    val newestSeenFriendRequests: Map<String, FriendRequestStamp> = emptyMap(),
    /** Quan hệ hiện tại với từng người (SocialRepository.graph). */
    val friendStatuses: Map<String, FriendStatus> = emptyMap(),
    val pendingRequestCreatedAt: Map<String, Instant> = emptyMap(),
    /** true khi [friendStatuses] vừa tải lại từ Firestore sau lời mời mới nhất trong danh sách. */
    val isGraphChecked: Boolean = false,
    val now: Instant = Instant.now()
) {
    /**
     * null = lời mời còn chờ (hiện ✕ ✓); true = đã kết bạn; false = đã từ chối, đã bị thu hồi hoặc đã huỷ kết bạn.
     * Lời mời cũ không còn pending thì không cho bấm ✓ nữa: Firestore sẽ từ chối ("không có quyền").
     */
    fun friendRequestDecision(notification: AppNotification): Boolean? {
        handledFriendRequestIds[notification.id]?.let { return it }
        val actorId = notification.actor?.id ?: return null
        // Keep request history, but an old request must not act on a newer invitation from this person.
        val newestVisibleRequest = (unreadNotifications + earlierNotifications)
            .filter { it.type == NotificationType.FRIEND_REQUEST && it.actor?.id == actorId }
            .maxWithOrNull(compareBy<AppNotification> { it.createdAt }.thenBy { it.id })
        val newestRequestId = newestSeenFriendRequests[actorId]?.notificationId ?: newestVisibleRequest?.id
        if (newestRequestId != null && newestRequestId != notification.id) return false
        // This survives a new screen/process: the active friendship is newer than this historical row.
        val currentRequestCreatedAt = pendingRequestCreatedAt[actorId]
        if (friendStatuses[actorId] == FriendStatus.REQUEST_RECEIVED && currentRequestCreatedAt != null &&
            notification.createdAt.isBefore(currentRequestCreatedAt)) return false
        return when (friendStatuses[actorId]) {
            FriendStatus.FRIENDS -> true
            FriendStatus.REQUEST_RECEIVED -> null
            // Chưa tải lại graph: giữ ✕ ✓ (lời mời vừa tới có thể chưa có trong graph cũ)
            else -> if (isGraphChecked) false else null
        }
    }

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
    /** Thao tác (chấp nhận lời mời, đánh dấu đã đọc…) lỗi: báo bằng snackbar, không thay cả danh sách bằng màn lỗi. */
    data class ShowMessage(val message: String, val retryFriendStatuses: Boolean = false) : NotificationsEffect
}
