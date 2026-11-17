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
class NotificationsViewModel @Inject constructor(
    private val notifications: NotificationRepository,
    private val social: SocialRepository,
    private val auth: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationsUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()
    private val _effects = Channel<NotificationsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()
    private var observer: Job? = null
    /** Mỗi vòng lời mời mới phải đối chiếu lại quan hệ; refresh thất bại vẫn cho phép thử lại. */
    private var checkedRequestIds = emptySet<String>()
    private var isRefreshingGraph = false
    private var graphRefreshJob: Job? = null
    private var activeAccountId: String? = null
    private var accountGeneration = 0
    init {
        viewModelScope.launch {
            auth.currentUser.map { it?.id }.distinctUntilChanged().collect { accountId ->
                // No notification watermark or completed action belongs to the next signed-in account.
                accountGeneration++
                activeAccountId = accountId
                observer?.cancel()
                graphRefreshJob?.cancel()
                checkedRequestIds = emptySet()
                isRefreshingGraph = false
                _uiState.value = NotificationsUiState(isLoading = accountId != null)
                if (accountId != null) observe()
            }
        }
        // Đã là bạn / lời mời đã bị thu hồi (màn khác, máy khác…) thì không hiện ✕ ✓ cho lời mời cũ nữa
        viewModelScope.launch {
            social.graph.collect { graph ->
                if (activeAccountId != null) _uiState.update {
                    it.copy(friendStatuses = graph.statuses, pendingRequestCreatedAt = graph.pendingRequestCreatedAt)
                }
            }
        }
    }
    private fun observe() {
        observer?.cancel()
        if (activeAccountId == null) return
        val generation = accountGeneration
        observer = viewModelScope.launch { notifications.observeNotifications().collect { result ->
            if (generation != accountGeneration) return@collect
            val list = result.getOrDefault(emptyList())
            _uiState.update { it.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message,
                unreadNotifications = list.filter { !it.isRead },
                earlierNotifications = list.filter { it.isRead },
                newestSeenFriendRequests = rememberNewestRequests(it.newestSeenFriendRequests, list), now = Instant.now()) }
            refreshGraphFor(list)
        } }
    }
    private fun rememberNewestRequests(
        previous: Map<String, FriendRequestStamp>, list: List<AppNotification>
    ): Map<String, FriendRequestStamp> = previous.toMutableMap().apply {
        list.filter { it.type == NotificationType.FRIEND_REQUEST }.forEach { notification ->
            val actorId = notification.actor?.id ?: return@forEach
            val known = this[actorId]
            if (known == null || notification.createdAt > known.createdAt ||
                (notification.createdAt == known.createdAt && notification.id > known.notificationId)) {
                this[actorId] = FriendRequestStamp(notification.id, notification.createdAt)
            }
        }
    }
    private fun refreshGraphFor(list: List<AppNotification>) {
        val requestIds = list.filter { it.type == NotificationType.FRIEND_REQUEST && it.actor != null }.map { it.id }.toSet()
        if (checkedRequestIds.containsAll(requestIds) || isRefreshingGraph) return
        isRefreshingGraph = true
        val generation = accountGeneration
        _uiState.update { it.copy(isGraphChecked = false) }
        graphRefreshJob = viewModelScope.launch {
            var succeeded = false
            try {
                val result = social.refresh()
                if (generation != accountGeneration) return@launch
                succeeded = result.isSuccess
                if (succeeded) checkedRequestIds = checkedRequestIds + requestIds
                _uiState.update { it.copy(isGraphChecked = succeeded,
                    friendStatuses = if (succeeded) social.graph.value.statuses else it.friendStatuses,
                    pendingRequestCreatedAt = if (succeeded) social.graph.value.pendingRequestCreatedAt else it.pendingRequestCreatedAt) }
                result.onFailure { error ->
                    _effects.trySend(NotificationsEffect.ShowMessage(error.message ?: "Không tải được trạng thái lời mời", retryFriendStatuses = true))
                }
            } finally { if (generation == accountGeneration) isRefreshingGraph = false }
            if (succeeded) refreshGraphFor(_uiState.value.unreadNotifications + _uiState.value.earlierNotifications)
        }
    }
    private fun perform(block: suspend () -> Result<Unit>) { viewModelScope.launch { block().onFailure { error ->
        _effects.trySend(NotificationsEffect.ShowMessage(error.message ?: "Không thực hiện được, thử lại sau")) } } }
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
        NotificationsAction.Retry -> { checkedRequestIds = emptySet(); observe() }
        NotificationsAction.NavigateBack -> _effects.trySend(NotificationsEffect.NavigateBack)
    } }
    private fun handleRequest(id: String, accept: Boolean) {
        val state = _uiState.value
        val notification = (state.unreadNotifications + state.earlierNotifications).find { it.id == id } ?: return
        val actor = notification.actor ?: return
        if (!state.isGraphChecked || notification.type != NotificationType.FRIEND_REQUEST || state.friendRequestDecision(notification) != null ||
            actor.id in state.pendingFriendRequestActorIds) return
        // The same actor can have historical notification rows. Lock the relationship, not only this row.
        _uiState.update { it.copy(pendingFriendRequestActorIds = it.pendingFriendRequestActorIds + actor.id) }
        val generation = accountGeneration
        viewModelScope.launch {
            try {
                if (generation != accountGeneration) return@launch
                val result = if (accept) social.acceptFriendRequest(actor.id) else social.declineFriendRequest(actor.id)
                if (generation != accountGeneration) return@launch
                val completed = if (result.isSuccess) {
                    _uiState.update { it.copy(handledFriendRequestIds = it.handledFriendRequestIds + (id to accept)) }
                    notifications.markNotificationRead(id)
                } else {
                    // Lời mời đã bị thu hồi / hết hiệu lực: đối chiếu lại quan hệ trước khi đổi trạng thái nút.
                    val refreshed = social.refresh()
                    if (generation != accountGeneration) return@launch
                    _uiState.update { it.copy(isGraphChecked = refreshed.isSuccess) }
                    result
                }
                completed.onFailure { error ->
                    _effects.trySend(NotificationsEffect.ShowMessage(error.message ?: "Không thực hiện được, thử lại sau"))
                }
            } finally {
                if (generation == accountGeneration) {
                    _uiState.update { it.copy(pendingFriendRequestActorIds = it.pendingFriendRequestActorIds - actor.id) }
                }
            }
        }
    }
}
