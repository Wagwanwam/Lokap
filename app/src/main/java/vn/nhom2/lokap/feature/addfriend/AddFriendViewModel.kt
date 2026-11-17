package vn.nhom2.lokap.feature.addfriend

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.domain.model.FriendCode
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.repository.AuthRepository
import vn.nhom2.lokap.domain.repository.SocialRepository

/**
 * Thêm bạn: mã QR của mình, chia sẻ link mời, mở người từ mã đã quét / link `lokap://add/<username>`.
 * Trạng thái bạn bè của người vừa mở luôn lấy từ SocialRepository.graph nên nút tự đổi sau khi gửi/chấp nhận.
 */
@HiltViewModel
class AddFriendViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val auth: AuthRepository,
    private val social: SocialRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AddFriendUiState(
            me = auth.currentUser.value,
            friendCount = social.graph.value.friendCount,
            maxFriends = social.maxFriends
        )
    )
    val uiState: StateFlow<AddFriendUiState> = _uiState.asStateFlow()

    private val _effects = Channel<AddFriendEffect>(Channel.BUFFERED)
    val effects: Flow<AddFriendEffect> = _effects.receiveAsFlow()

    private var lookupJob: Job? = null
    private val workingUserIds = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            auth.currentUser.collect { me -> _uiState.update { it.copy(me = me) } }
        }
        viewModelScope.launch {
            social.graph.collect { graph ->
                _uiState.update { state ->
                    val lookup = state.lookup
                    state.copy(
                        friendCount = graph.friendCount,
                        lookup = if (lookup is FriendLookup.Found && !lookup.isSelf) {
                            lookup.copy(status = graph.statusOf(lookup.user.id))
                        } else {
                            lookup
                        }
                    )
                }
            }
        }
        // Mở từ link mời: tham số chỉ là phần username, ghép lại thành link để kiểm tra đúng định dạng
        savedStateHandle.get<String>(ARG_USERNAME)
            ?.let { FriendCode.parse(FriendCode.linkFor(it)) }
            ?.let(::lookUp)
    }

    fun onAction(action: AddFriendAction) {
        when (action) {
            AddFriendAction.NavigateBack -> _effects.trySend(AddFriendEffect.NavigateBack)
            AddFriendAction.OpenScanner -> _effects.trySend(AddFriendEffect.OpenScanner)
            AddFriendAction.ShareInvite -> {
                val username = _uiState.value.me?.username
                if (username.isNullOrBlank()) {
                    _effects.trySend(AddFriendEffect.ShowMessage("Bạn cần đặt tên người dùng trước"))
                } else {
                    _effects.trySend(AddFriendEffect.ShareInvite(FriendCode.inviteText(username)))
                }
            }
            AddFriendAction.CopyLink -> _uiState.value.me?.username
                ?.takeIf { it.isNotBlank() }
                ?.let { _effects.trySend(AddFriendEffect.CopyLink(FriendCode.linkFor(it))) }
            is AddFriendAction.LookUp -> lookUp(action.username)
            AddFriendAction.RetryLookup -> _uiState.value.lookup?.let { lookUp(it.username) }
            AddFriendAction.DismissLookup -> {
                lookupJob?.cancel()
                _uiState.update { it.copy(lookup = null) }
            }
            AddFriendAction.SendRequest -> withFound(FriendStatus.NONE) { userId ->
                social.sendFriendRequest(userId).map { status ->
                    if (status == FriendStatus.FRIENDS) "Hai bạn đã là bạn bè" else "Đã gửi lời mời"
                }
            }
            AddFriendAction.CancelRequest -> withFound(FriendStatus.REQUEST_SENT) { userId ->
                social.cancelFriendRequest(userId).map { "Đã thu hồi lời mời" }
            }
            AddFriendAction.AcceptRequest -> withFound(FriendStatus.REQUEST_RECEIVED) { userId ->
                social.acceptFriendRequest(userId).map { "Hai bạn đã là bạn bè" }
            }
            is AddFriendAction.OpenProfile -> {
                _uiState.update { it.copy(lookup = null) }
                _effects.trySend(AddFriendEffect.NavigateToProfile(action.userId))
            }
        }
    }

    private fun lookUp(username: String) {
        val name = username.trim().removePrefix("@").lowercase()
        lookupJob?.cancel()
        _uiState.update { it.copy(lookup = FriendLookup.Loading(name)) }
        lookupJob = viewModelScope.launch {
            val result = social.findByUsername(name)
            val myId = auth.currentUser.value?.id
            val lookup = result.fold(
                onSuccess = { user ->
                    when {
                        user == null -> FriendLookup.NotFound(name)
                        user.id == myId -> FriendLookup.Found(user, FriendStatus.NONE, isSelf = true)
                        else -> FriendLookup.Found(user, social.graph.value.statusOf(user.id), isSelf = false, isWorking = user.id in workingUserIds)
                    }
                },
                onFailure = { FriendLookup.Failed(name, it.message ?: "Không mở được mã, thử lại sau") }
            )
            _uiState.update { it.copy(lookup = lookup) }
        }
    }

    /** Gọi server cho người đang hiện trong sheet; trạng thái mới tự về qua graph. */
    private fun withFound(expected: FriendStatus, block: suspend (String) -> Result<String>) {
        val found = _uiState.value.lookup as? FriendLookup.Found ?: return
        if (found.isSelf || found.user.id in workingUserIds) return
        if (social.graph.value.statusOf(found.user.id) != expected) return
        setWorking(found.user.id, true)
        viewModelScope.launch {
            try {
                block(found.user.id).onFailure {
                    _effects.trySend(AddFriendEffect.ShowMessage(it.message ?: "Không thực hiện được"))
                }
            } finally {
                setWorking(found.user.id, false)
            }
        }
    }

    private fun setWorking(userId: String, working: Boolean) {
        if (working) workingUserIds.add(userId) else workingUserIds.remove(userId)
        _uiState.update { state ->
            val found = state.lookup as? FriendLookup.Found ?: return@update state
            if (found.user.id != userId) return@update state
            state.copy(lookup = found.copy(isWorking = working))
        }
    }

    companion object {
        /** Trùng tên tham số của Route.AddFriend và `{username}` trong deep link. */
        const val ARG_USERNAME = "username"
    }
}
