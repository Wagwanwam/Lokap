package vn.nhom2.lokap.feature.notifications

import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.data.fake.SampleSocialData
import vn.nhom2.lokap.data.fake.FakeChatStore
import vn.nhom2.lokap.data.fake.FakeNotificationStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.AppNotification
import vn.nhom2.lokap.domain.model.NotificationType

class NotificationsViewModel : ViewModel() {

    private val handledRequests = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    private val _uiState = MutableStateFlow(NotificationsUiState(isLoading = true))
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<NotificationsEffect>(Channel.BUFFERED)
    val effects: Flow<NotificationsEffect> = _effects.receiveAsFlow()

    init {
        observeNotifications()
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            FakeNotificationStore.notifications.collect { all ->
                val sorted = all.sortedByDescending { it.createdAt }
                val unread = sorted.filter { !it.isRead }
                val earlier = sorted.filter { it.isRead }

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = null,
                        unreadNotifications = unread,
                        earlierNotifications = earlier,
                        now = SampleData.NOW
                    )
                }
            }
        }
    }

    fun onAction(action: NotificationsAction) {
        when (action) {
            is NotificationsAction.ClickNotification -> {
                handleNotificationClick(action.notification)
            }

            NotificationsAction.MarkAllAsRead -> {
                FakeNotificationStore.markAllRead()
            }

            is NotificationsAction.AcceptFriendRequest -> {
                // Cập nhật danh sách bạn thật, không chỉ đánh dấu trên màn hình
                val actorId = findActorId(action.notificationId)
                if (actorId != null && !SampleSocialData.acceptFriendRequest(actorId)) {
                    return // đã đủ 20 bạn: giữ thông báo để người dùng xử lý sau
                }
                markRequestHandled(action.notificationId, accepted = true)
            }

            is NotificationsAction.DeclineFriendRequest -> {
                findActorId(action.notificationId)?.let { SampleSocialData.declineFriendRequest(it) }
                markRequestHandled(action.notificationId, accepted = false)
            }

            NotificationsAction.Retry -> {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                observeNotifications()
            }

            NotificationsAction.NavigateBack -> {
                sendEffect(NotificationsEffect.NavigateBack)
            }
        }
    }

    private fun handleNotificationClick(notification: AppNotification) {
        markSingleAsRead(notification.id)

        when (notification.type) {
            NotificationType.NEW_POST,
            NotificationType.REACTION,
            NotificationType.POST_HIDDEN -> {
                // Không có bài thì không mở gì (trước đây mở bừa bài đầu tiên)
                notification.postId?.let { sendEffect(NotificationsEffect.NavigateToPostDetail(it)) }
            }

            NotificationType.FRIEND_REQUEST,
            NotificationType.FRIEND_ACCEPTED -> {
                sendEffect(NotificationsEffect.NavigateToFriends)
            }

            NotificationType.NEW_MESSAGE -> {
                // Mở đúng cuộc trò chuyện với người gửi; không rõ người gửi thì về Hộp thư
                val actor = notification.actor
                if (actor != null && SampleSocialData.getFriendStatus(actor.id) == FriendStatus.FRIENDS) {
                    val conv = FakeChatStore.getOrCreateConversationWithFriend(actor)
                    sendEffect(NotificationsEffect.NavigateToChat(conv.id))
                } else {
                    sendEffect(NotificationsEffect.NavigateToInbox)
                }
            }
        }
    }

    private fun markSingleAsRead(notificationId: String) {
        FakeNotificationStore.markRead(notificationId)
    }

    private fun findActorId(notificationId: String): String? =
        FakeNotificationStore.notifications.value.firstOrNull { it.id == notificationId }?.actor?.id

    private fun markRequestHandled(notificationId: String, accepted: Boolean) {
        markSingleAsRead(notificationId)
        handledRequests.update { current -> current + (notificationId to accepted) }
        _uiState.update { state ->
            state.copy(handledFriendRequestIds = handledRequests.value)
        }
    }

    private fun sendEffect(effect: NotificationsEffect) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }
}
