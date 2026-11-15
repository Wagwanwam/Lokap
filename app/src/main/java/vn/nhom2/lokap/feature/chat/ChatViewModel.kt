package vn.nhom2.lokap.feature.chat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.time.*
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.domain.model.*
import vn.nhom2.lokap.domain.repository.*

@HiltViewModel
class ChatViewModel @Inject constructor(savedStateHandle: SavedStateHandle,
    private val messages: MessagingRepository, private val auth: AuthRepository,
    private val social: SocialRepository
) : ViewModel() {
    private val conversationId = runCatching { savedStateHandle.toRoute<Route.Chat>().conversationId }
        .getOrElse { savedStateHandle.get<String>("conversationId").orEmpty() }
    private val _uiState = MutableStateFlow(ChatUiState(isLoading = true, conversationId = conversationId))
    val uiState = _uiState.asStateFlow()
    private val _effects = Channel<ChatEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()
    private var observer: Job? = null
    private var latestIncoming: String? = null
    init {
        observe()
        viewModelScope.launch { social.refresh() }
        viewModelScope.launch { messages.observeTheme(conversationId).collect { theme -> _uiState.update { it.copy(themeConfig = theme) } } }
    }
    private fun observe() {
        observer?.cancel()
        observer = viewModelScope.launch {
            combine(messages.observeChat(conversationId), social.graph) { result, graph -> result to graph }.collect { (result, graph) ->
                result.fold(onSuccess = { chat ->
                    val conv = chat.conversation
                    val uid = auth.currentUser.value?.id.orEmpty()
                    _uiState.update { it.copy(isLoading = false,
                        errorMessage = if (conv == null) "Không tìm thấy cuộc trò chuyện" else null,
                        friend = conv?.friend, isFriendStatusValid = conv != null && graph.statusOf(conv.friend.id) == FriendStatus.FRIENDS,
                        timelineItems = buildReverseTimeline(chat.messages, uid, Instant.now()), now = Instant.now()) }
                    val incoming = chat.messages.lastOrNull { it.senderId != uid }?.id
                    if (incoming != null && incoming != latestIncoming) {
                        latestIncoming = incoming
                        // Chỉ ghi readAt khi thật sự có tin chưa đọc: ghi thừa làm document hội thoại đổi, Hộp thư tính lại
                        if ((conv?.unreadCount ?: 0) > 0) {
                            messages.markRead(conversationId).onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
                        }
                    }
                }, onFailure = { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message) } })
            }
        }
    }
    private fun perform(block: suspend () -> Result<Unit>) { viewModelScope.launch {
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        val result = block()
        _uiState.update { it.copy(isSaving = false, errorMessage = result.exceptionOrNull()?.message) }
    } }
    fun onAction(action: ChatAction) {
        when(action) {
            is ChatAction.InputChanged -> _uiState.update { it.copy(inputText = action.text.take(1000)) }
            ChatAction.SendMessage -> if (_uiState.value.canSend) {
                val text = _uiState.value.inputText
                perform { messages.send(conversationId, text).onSuccess { _uiState.update { it.copy(inputText = "") } } }
            }
            is ChatAction.EditMessage -> _uiState.update { it.copy(editingMessage = action.message, editText = action.message.body.orEmpty()) }
            is ChatAction.EditTextChanged -> _uiState.update { it.copy(editText = action.text.take(1000)) }
            ChatAction.DismissEdit -> if (!_uiState.value.isSaving) _uiState.update { it.copy(editingMessage = null) }
            ChatAction.SaveEdit -> _uiState.value.editingMessage?.let { msg ->
                val text = _uiState.value.editText
                perform { messages.edit(conversationId, msg.id, text).onSuccess { _uiState.update { it.copy(editingMessage = null) } } }
            }
            is ChatAction.DeleteMessage -> _uiState.update { it.copy(deletingMessageId = action.id) }
            ChatAction.DismissDelete -> _uiState.update { it.copy(deletingMessageId = null) }
            ChatAction.ConfirmDelete -> _uiState.value.deletingMessageId?.let { id ->
                perform { messages.delete(conversationId, id).onSuccess { _uiState.update { it.copy(deletingMessageId = null) } } }
            }
            is ChatAction.RetryMessage -> observe()
            ChatAction.RetryLoad -> { observe(); viewModelScope.launch { social.refresh() } }
            ChatAction.NavigateBack -> _effects.trySend(ChatEffect.NavigateBack)
            ChatAction.OpenCameraForDirectShot -> _effects.trySend(ChatEffect.NavigateToCamera)
            is ChatAction.OpenPostDetail -> _effects.trySend(ChatEffect.NavigateToPostDetail(action.postId))
            ChatAction.OpenFriendProfile -> _uiState.value.friend?.let { _effects.trySend(ChatEffect.NavigateToProfile(it.id)) }
            ChatAction.OpenThemeSheet -> _uiState.update { it.copy(isThemeSheetVisible = true) }
            ChatAction.DismissThemeSheet -> _uiState.update { it.copy(isThemeSheetVisible = false) }
            is ChatAction.SelectThemePreset -> perform { messages.saveTheme(conversationId, _uiState.value.themeConfig.copy(presetId = action.presetId)) }
            is ChatAction.SetCustomWallpaper -> perform { messages.saveTheme(conversationId, _uiState.value.themeConfig.copy(customWallpaperUri = action.uri, wallpaperAlignment = action.alignment)) }
            ChatAction.RemoveCustomWallpaper -> perform { messages.saveTheme(conversationId, _uiState.value.themeConfig.copy(customWallpaperUri = null)) }
        }
    }
    companion object {
        private val DAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")
        private val ZONE_ID: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")

        fun buildReverseTimeline(
            messagesAscending: List<Message>,
            myUserId: String,
            now: Instant
        ): List<ChatTimelineItem> {
            if (messagesAscending.isEmpty()) return emptyList()

            val today: LocalDate = now.atZone(ZONE_ID).toLocalDate()
            val yesterday: LocalDate = today.minusDays(1)
            val latestMyMessageId: String? = messagesAscending
                .lastOrNull { it.senderId == myUserId }
                ?.id

            val groupedByDate: Map<LocalDate, List<Message>> = messagesAscending.groupBy { msg ->
                msg.createdAt.atZone(ZONE_ID).toLocalDate()
            }

            val chronologicalItems = mutableListOf<ChatTimelineItem>()
            groupedByDate.entries
                .sortedBy { it.key }
                .forEach { (date, dayMessages) ->
                    val label = when (date) {
                        today -> "Hôm nay"
                        yesterday -> "Hôm qua"
                        else -> date.format(DAY_FORMATTER)
                    }
                    chronologicalItems.add(
                        ChatTimelineItem.DateHeader(
                            id = date.toString(),
                            label = label
                        )
                    )
                    dayMessages.forEach { message ->
                        chronologicalItems.add(
                            ChatTimelineItem.MessageNode(
                                message = message,
                                isMine = message.senderId == myUserId,
                                isLatestMyMessage = message.id == latestMyMessageId
                            )
                        )
                    }
                }

            return chronologicalItems.asReversed()
        }
    }
}
