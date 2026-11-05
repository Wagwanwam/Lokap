package vn.nhom2.lokap.feature.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.data.fake.SampleSocialData
import vn.nhom2.lokap.domain.model.FriendStatus

@OptIn(FlowPreview::class)
class FriendsViewModel : ViewModel() {

    private val rawSearchQuery = MutableStateFlow("")

    private val _uiState = MutableStateFlow(FriendsUiState(isLoading = true))
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<FriendsEffect>(Channel.BUFFERED)
    val effects: Flow<FriendsEffect> = _effects.receiveAsFlow()

    init {
        observeFriendsAndSearch()
    }

    private fun observeFriendsAndSearch() {
        val debouncedQueryFlow = rawSearchQuery
            .map { it.trim() }
            .debounce(300L)
            .distinctUntilChanged()

        viewModelScope.launch {
            combine(
                SampleSocialData.friendStatuses,
                debouncedQueryFlow
            ) { statuses, query ->
                statuses to query
            }.collect { (statuses, query) ->
                val currentUserId = SampleSocialData.getCurrentUser().id
                val allOthers = SampleSocialData.directoryUsers.filter { it.id != currentUserId }

                val incoming = allOthers.filter { statuses[it.id] == FriendStatus.REQUEST_RECEIVED }
                val sent = allOthers.filter { statuses[it.id] == FriendStatus.REQUEST_SENT }
                val friends = allOthers.filter { statuses[it.id] == FriendStatus.FRIENDS }

                val normalizedQuery = query.removePrefix("@").lowercase()
                val searchResults = if (normalizedQuery.isBlank()) {
                    emptyList()
                } else {
                    // Người mình đã chặn không hiện trong kết quả tìm kiếm
                    allOthers.filter { user ->
                        statuses[user.id] != FriendStatus.BLOCKED &&
                            (user.username.lowercase().contains(normalizedQuery) ||
                                user.displayName.lowercase().contains(normalizedQuery))
                    }.map { user ->
                        FriendUserItemUi(
                            user = user,
                            status = statuses[user.id] ?: FriendStatus.NONE
                        )
                    }
                }

                _uiState.update { prev ->
                    prev.copy(
                        isLoading = false,
                        errorMessage = null,
                        debouncedQuery = query,
                        searchResults = searchResults,
                        incomingRequests = incoming,
                        sentRequests = sent,
                        friends = friends,
                        maxFriends = SampleSocialData.MAX_FRIENDS
                    )
                }
            }
        }
    }

    fun onAction(action: FriendsAction) {
        when (action) {
            FriendsAction.Retry -> {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                observeFriendsAndSearch()
            }

            FriendsAction.NavigateBack -> {
                _effects.trySend(FriendsEffect.NavigateBack)
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

            is FriendsAction.SendRequest -> {
                val ok = SampleSocialData.sendFriendRequest(action.userId)
                if (!ok) {
                    _effects.trySend(
                        FriendsEffect.ShowMessage("Đã chạm mốc 20 bạn bè! Hãy dọn danh sách nếu muốn thêm bạn mới.")
                    )
                } else {
                    _effects.trySend(FriendsEffect.ShowMessage("Đã gửi lời mời kết bạn"))
                }
            }

            is FriendsAction.CancelSentRequest -> {
                SampleSocialData.cancelFriendRequest(action.userId)
                _effects.trySend(FriendsEffect.ShowMessage("Đã thu hồi lời mời kết bạn"))
            }

            is FriendsAction.AcceptRequest -> {
                val ok = SampleSocialData.acceptFriendRequest(action.userId)
                if (!ok) {
                    _effects.trySend(
                        FriendsEffect.ShowMessage("Đã đủ 20 người bạn, không thể chấp nhận thêm")
                    )
                } else {
                    _effects.trySend(FriendsEffect.ShowMessage("Đã thêm vào hội bạn thân!"))
                }
            }

            is FriendsAction.DeclineRequest -> {
                SampleSocialData.declineFriendRequest(action.userId)
                _effects.trySend(FriendsEffect.ShowMessage("Đã từ chối lời mời"))
            }

            is FriendsAction.UnblockUser -> {
                SampleSocialData.unblockUser(action.userId)
                _effects.trySend(FriendsEffect.ShowMessage("Đã bỏ chặn người dùng"))
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
                when (pending.actionType) {
                    FriendConfirmActionType.UNFRIEND -> {
                        SampleSocialData.unfriend(pending.user.id)
                        _effects.trySend(
                            FriendsEffect.ShowMessage("Đã huỷ kết bạn với ${pending.user.displayName}")
                        )
                    }

                    FriendConfirmActionType.BLOCK -> {
                        SampleSocialData.blockUser(pending.user.id)
                        _effects.trySend(
                            FriendsEffect.ShowMessage("Đã chặn ${pending.user.displayName}")
                        )
                    }
                }
                _uiState.update { it.copy(pendingConfirmation = null) }
            }
        }
    }
}
