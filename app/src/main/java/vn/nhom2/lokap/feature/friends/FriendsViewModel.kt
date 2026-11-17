package vn.nhom2.lokap.feature.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.repository.SocialRepository
import vn.nhom2.lokap.domain.repository.isOffline

/** Bạn bè: danh sách từ SocialRepository.graph, tìm kiếm hỏi server sau 300 ms ngừng gõ. */
@OptIn(FlowPreview::class)
@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val social: SocialRepository
) : ViewModel() {

    private val rawSearchQuery = MutableStateFlow("")

    private val _uiState = MutableStateFlow(FriendsUiState(isLoading = true, maxFriends = social.maxFriends))
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<FriendsEffect>(Channel.BUFFERED)
    val effects: Flow<FriendsEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            social.graph.collect { graph ->
                _uiState.update { prev ->
                    prev.copy(
                        // Còn chờ server chỉ khi trong máy chưa có quan hệ nào
                        isLoading = prev.isLoading && graph.statuses.isEmpty(),
                        errorMessage = null,
                        incomingRequests = graph.usersWith(FriendStatus.REQUEST_RECEIVED),
                        sentRequests = graph.usersWith(FriendStatus.REQUEST_SENT),
                        friends = graph.usersWith(FriendStatus.FRIENDS),
                        friendCount = graph.friendCount,
                        maxFriends = social.maxFriends,
                        // Kết quả tìm kiếm đang hiện: cập nhật nút theo trạng thái mới
                        searchResults = prev.searchResults
                            .filter { graph.statusOf(it.user.id) != FriendStatus.BLOCKED }
                            .map { it.copy(status = graph.statusOf(it.user.id)) }
                    )
                }
            }
        }
        viewModelScope.launch {
            rawSearchQuery
                .map { it.trim() }
                .debounce(300L)
                .distinctUntilChanged()
                .collect { query -> search(query) }
        }
        refresh()
    }

    /** Làm mới từ server khi mở màn: graph trong máy có thể trống nếu mở app lúc mất mạng. */
    private fun refresh() {
        _uiState.update { it.copy(errorMessage = null) }
        viewModelScope.launch {
            social.refresh().fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false, isOffline = false) } },
                onFailure = { error ->
                    _uiState.update { state ->
                        val empty = state.friends.isEmpty() && state.incomingRequests.isEmpty() && state.sentRequests.isEmpty()
                        state.copy(
                            isLoading = false,
                            isOffline = error.isOffline,
                            // Có dữ liệu trong máy thì chỉ hiện banner offline, không che bằng màn lỗi
                            errorMessage = if (empty) error.message else null
                        )
                    }
                }
            )
        }
    }

    private suspend fun search(query: String) {
        if (query.removePrefix("@").isBlank()) {
            _uiState.update { it.copy(debouncedQuery = "", searchResults = emptyList()) }
            return
        }
        social.search(query).fold(
            onSuccess = { users ->
                val graph = social.graph.value
                _uiState.update {
                    it.copy(
                        debouncedQuery = query,
                        isOffline = false,
                        searchResults = users
                            .filter { user -> graph.statusOf(user.id) != FriendStatus.BLOCKED }
                            .map { user -> FriendUserItemUi(user = user, status = graph.statusOf(user.id)) }
                    )
                }
            },
            onFailure = { error ->
                _uiState.update { it.copy(debouncedQuery = query, isOffline = error.isOffline) }
                _effects.trySend(FriendsEffect.ShowMessage(error.message ?: "Không tìm được, thử lại sau"))
            }
        )
    }

    fun onAction(action: FriendsAction) {
        when (action) {
            FriendsAction.Retry -> {
                _uiState.update { it.copy(isLoading = true) }
                refresh()
            }

            FriendsAction.NavigateBack -> {
                _effects.trySend(FriendsEffect.NavigateBack)
            }

            FriendsAction.OpenAddFriend -> {
                _effects.trySend(FriendsEffect.NavigateToAddFriend)
            }

            is FriendsAction.ChangeSearchQuery -> {
                rawSearchQuery.value = action.query
                _uiState.update { it.copy(searchQuery = action.query) }
            }

            FriendsAction.ClearSearch -> {
                rawSearchQuery.value = ""
                _uiState.update {
                    it.copy(
                        searchQuery = "",
                        debouncedQuery = "",
                        searchResults = emptyList()
                    )
                }
            }

            is FriendsAction.OpenProfile -> {
                _effects.trySend(FriendsEffect.NavigateToProfile(action.userId))
            }

            is FriendsAction.SendRequest -> perform(action.userId, FriendStatus.NONE) {
                social.sendFriendRequest(action.userId).map { status ->
                    if (status == FriendStatus.FRIENDS) "Đã thêm vào hội bạn thân!" else "Đã gửi lời mời kết bạn"
                }
            }

            is FriendsAction.CancelSentRequest -> perform(action.userId, FriendStatus.REQUEST_SENT) {
                social.cancelFriendRequest(action.userId).map { "Đã thu hồi lời mời kết bạn" }
            }

            is FriendsAction.AcceptRequest -> perform(action.userId, FriendStatus.REQUEST_RECEIVED) {
                social.acceptFriendRequest(action.userId).map { "Đã thêm vào hội bạn thân!" }
            }

            is FriendsAction.DeclineRequest -> perform(action.userId, FriendStatus.REQUEST_RECEIVED) {
                social.declineFriendRequest(action.userId).map { "Đã từ chối lời mời" }
            }

            is FriendsAction.UnblockUser -> perform(action.userId, FriendStatus.BLOCKED) {
                social.unblock(action.userId).map { "Đã bỏ chặn người dùng" }
            }

            is FriendsAction.RequestUnfriendConfirm -> {
                _uiState.update {
                    it.copy(
                        pendingConfirmation = PendingFriendConfirmation(
                            user = action.user,
                            actionType = FriendConfirmActionType.UNFRIEND
                        )
                    )
                }
            }

            is FriendsAction.RequestBlockConfirm -> {
                _uiState.update {
                    it.copy(
                        pendingConfirmation = PendingFriendConfirmation(
                            user = action.user,
                            actionType = FriendConfirmActionType.BLOCK
                        )
                    )
                }
            }

            FriendsAction.DismissConfirmDialog -> {
                _uiState.update { it.copy(pendingConfirmation = null) }
            }

            FriendsAction.ConfirmPendingAction -> {
                val pending = _uiState.value.pendingConfirmation ?: return
                _uiState.update { it.copy(pendingConfirmation = null) }
                when (pending.actionType) {
                    FriendConfirmActionType.UNFRIEND -> perform(pending.user.id, FriendStatus.FRIENDS) {
                        social.unfriend(pending.user.id).map { "Đã huỷ kết bạn với ${pending.user.displayName}" }
                    }

                    FriendConfirmActionType.BLOCK -> perform(pending.user.id) {
                        social.block(pending.user.id).map { "Đã chặn ${pending.user.displayName}" }
                    }
                }
            }
        }
    }

    /** Gọi server rồi báo một câu; trạng thái mới tự về qua graph. Lỗi (đủ 20 bạn, mất mạng…) báo đúng câu của server. */
    private fun perform(userId: String, expected: FriendStatus? = null, block: suspend () -> Result<String>) {
        if (userId in _uiState.value.workingUserIds) return
        if (expected != null && social.graph.value.statusOf(userId) != expected) return
        // Khoá đồng bộ trước launch: hai lần chạm trong cùng frame chỉ chạy một lời gọi.
        _uiState.update { it.copy(workingUserIds = it.workingUserIds + userId) }
        viewModelScope.launch {
            try {
                block().onFailure { _effects.trySend(FriendsEffect.ShowMessage(it.message ?: "Không thực hiện được")) }
            } finally {
                _uiState.update { it.copy(workingUserIds = it.workingUserIds - userId) }
            }
        }
    }
}
