package vn.nhom2.lokap.feature.notifications
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import vn.nhom2.lokap.domain.model.*
import vn.nhom2.lokap.domain.repository.*
@HiltViewModel
class NotificationsViewModel @Inject constructor(private val notifications: NotificationRepository, private val social: SocialRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationsUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()
    private val _effects = Channel<NotificationsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()
    private var observer: Job? = null
    init {
        observe()
        // Đã là bạn (chấp nhận ở màn Bạn bè, máy khác…) thì không hiện ✕ ✓ cho lời mời cũ nữa
        viewModelScope.launch {
            social.graph.collect { graph ->
                _uiState.update { state ->
                    state.copy(friendIds = graph.statuses.filterValues { it == FriendStatus.FRIENDS }.keys)
                }
            }
        }
    }
    private fun observe() {
        observer?.cancel()
        observer = viewModelScope.launch { notifications.observeNotifications().collect { result ->
            _uiState.update { it.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message,
                unreadNotifications = result.getOrDefault(emptyList()).filter { !it.isRead },
                earlierNotifications = result.getOrDefault(emptyList()).filter { it.isRead }, now = Instant.now()) }
        } }
    }
    private fun perform(block: suspend () -> Result<Unit>) { viewModelScope.launch { block().onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } } } }
    fun onAction(action: NotificationsAction) { when(action) {
        is NotificationsAction.ClickNotification -> perform {
            notifications.markNotificationRead(action.notification.id).onSuccess {
                val n = action.notification
                when(n.type) {
                    NotificationType.NEW_MESSAGE -> _effects.trySend(n.conversationId?.let { NotificationsEffect.NavigateToChat(it) } ?: NotificationsEffect.NavigateToInbox)
                    NotificationType.FRIEND_REQUEST, NotificationType.FRIEND_ACCEPTED -> _effects.trySend(NotificationsEffect.NavigateToFriends)
                    else -> n.postId?.let { _effects.trySend(NotificationsEffect.NavigateToPostDetail(it)) }
                }
            }
        }
        NotificationsAction.MarkAllAsRead -> perform { notifications.markAllRead() }
        is NotificationsAction.ToggleRead -> perform { notifications.markNotificationRead(action.notification.id, !action.notification.isRead) }
        is NotificationsAction.DeleteNotification -> perform { notifications.deleteNotification(action.id) }
        is NotificationsAction.AcceptFriendRequest -> handleRequest(action.notificationId, true)
        is NotificationsAction.DeclineFriendRequest -> handleRequest(action.notificationId, false)
        NotificationsAction.Retry -> observe()
        NotificationsAction.NavigateBack -> _effects.trySend(NotificationsEffect.NavigateBack)
    } }
    private fun handleRequest(id: String, accept: Boolean) {
        val actor = (_uiState.value.unreadNotifications + _uiState.value.earlierNotifications).find { it.id == id }?.actor ?: return
        perform {
            val result = if (accept) social.acceptFriendRequest(actor.id) else social.declineFriendRequest(actor.id)
            if (result.isSuccess) {
                _uiState.update { it.copy(handledFriendRequestIds = it.handledFriendRequestIds + (id to accept)) }
                notifications.markNotificationRead(id)
            } else result
        }
    }
}
