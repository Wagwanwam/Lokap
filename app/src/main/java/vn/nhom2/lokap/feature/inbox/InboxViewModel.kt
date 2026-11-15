package vn.nhom2.lokap.feature.inbox
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
class InboxViewModel @Inject constructor(private val messages: MessagingRepository, private val social: SocialRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(InboxUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()
    private val _effects = Channel<InboxEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()
    private var observer: Job? = null
    init { observe() }
    private fun observe() {
        observer?.cancel()
        observer = viewModelScope.launch {
            social.refresh()
            combine(messages.observeInbox(), social.graph) { result, graph -> result to graph }.collect { (result, graph) ->
                _uiState.update { it.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message,
                    items = result.getOrDefault(emptyList()).map { conv -> InboxConversationItem(conv, conv.lastMessagePreview.ifBlank { "Bắt đầu cuộc trò chuyện" }) },
                    friends = graph.usersWith(FriendStatus.FRIENDS), now = Instant.now()) }
            }
        }
    }
    fun onAction(action: InboxAction) { when(action) {
        is InboxAction.OpenConversation -> _effects.trySend(InboxEffect.NavigateToChat(action.conversationId))
        is InboxAction.MarkAsRead -> viewModelScope.launch { messages.markRead(action.conversationId).onFailure { e -> _uiState.update { it.copy(errorMessage = e.message) } } }
        InboxAction.OpenNewMessageSheet -> { _uiState.update { it.copy(isNewMessageSheetOpen = true) }; viewModelScope.launch { social.refresh() } }
        InboxAction.DismissNewMessageSheet -> _uiState.update { it.copy(isNewMessageSheetOpen = false) }
        is InboxAction.SelectFriendToChat -> viewModelScope.launch {
            messages.openConversation(action.friend.id).fold(onSuccess = { id ->
                _uiState.update { it.copy(isNewMessageSheetOpen = false) }; _effects.send(InboxEffect.NavigateToChat(id))
            }, onFailure = { e -> _uiState.update { it.copy(errorMessage = e.message) } })
        }
        InboxAction.Retry -> observe()
        InboxAction.NavigateBack -> _effects.trySend(InboxEffect.NavigateBack)
    } }
}
