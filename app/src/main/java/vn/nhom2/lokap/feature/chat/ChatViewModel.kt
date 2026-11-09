package vn.nhom2.lokap.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.data.fake.FakeChatStore
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.data.fake.SampleSocialData
import vn.nhom2.lokap.domain.model.*
class ChatViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val routeArgs: Route.Chat = runCatching {
        savedStateHandle.toRoute<Route.Chat>()
    }.getOrElse {
        Route.Chat(conversationId = savedStateHandle["conversationId"] ?: "")
    }

    private val conversationId: String = routeArgs.conversationId
    private val myUserId: String = SampleData.currentUser.id

    private val _uiState = MutableStateFlow(
        ChatUiState(
            isLoading = true,
            conversationId = conversationId
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ChatEffect>(Channel.BUFFERED)
    val effects: Flow<ChatEffect> = _effects.receiveAsFlow()

    private val friendSeenUpTo = MutableStateFlow<Instant?>(null)

    private var replySimulationJob: Job? = null
    private var nextReplyIndex: Int = 0

    init {
        FakeChatStore.markConversationRead(conversationId)
        observeConversationAndMessages()
        observeChatTheme()
    }

    private fun observeChatTheme() {
        viewModelScope.launch {
            FakeChatStore.conversationThemes.collect { themes ->
                val theme = themes[conversationId] ?: ChatThemeConfig()
                _uiState.update { it.copy(themeConfig = theme) }
            }
        }
    }

    private fun observeConversationAndMessages() {
        viewModelScope.launch {
            combine(
                FakeChatStore.conversations,
                FakeChatStore.messages,
                SampleSocialData.friendStatuses,
                friendSeenUpTo
            ) { conversations, allMessages, statuses, seenUpTo ->
                val conv = conversations.find { it.id == conversationId }
                val convMessages = allMessages
                    .filter { it.conversationId == conversationId }
                    .sortedBy { it.createdAt }
                    .map { msg ->
                        if (msg.senderId == myUserId && seenUpTo != null && !msg.createdAt.isAfter(seenUpTo)) {
                            msg.copy(seen = true)
                        } else {
                            msg
                        }
                    }
                val isStillFriend = conv != null && statuses[conv.friend.id] == FriendStatus.FRIENDS
                Triple(conv, convMessages, isStillFriend)
            }.collect { (conv, convMessages, isStillFriend) ->
                if (conv == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Không tìm thấy cuộc trò chuyện này."
                        )
                    }
                    return@collect
                }

                val now = FakeChatStore.clockNow()
                val timeline = buildReverseTimeline(
                    messagesAscending = convMessages,
                    myUserId = myUserId,
                    now = now
                )

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = null,
                        friend = conv.friend,
                        isFriendStatusValid = isStillFriend,
                        timelineItems = timeline,
                        now = now
                    )
                }
            }
        }
    }

    fun onAction(action: ChatAction) {
        when (action) {
            is ChatAction.InputChanged -> {
                val limited = action.text.take(1000)
                _uiState.update { it.copy(inputText = limited) }
            }

            ChatAction.SendMessage -> {
                handleSendMessage()
            }

            is ChatAction.RetryMessage -> {
                handleRetryMessage(action.messageId)
            }

            ChatAction.OpenCameraForDirectShot -> {

                sendEffect(ChatEffect.NavigateToCamera)
            }

            is ChatAction.OpenPostDetail -> {
                sendEffect(ChatEffect.NavigateToPostDetail(action.postId))
            }

            ChatAction.OpenFriendProfile -> {
                val friendId = _uiState.value.friend?.id ?: return
                sendEffect(ChatEffect.NavigateToProfile(friendId))
            }

            ChatAction.RetryLoad -> {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                observeConversationAndMessages()
            }

            ChatAction.NavigateBack -> {
                sendEffect(ChatEffect.NavigateBack)
            }

            ChatAction.OpenThemeSheet -> {
                _uiState.update { it.copy(isThemeSheetVisible = true) }
            }

            ChatAction.DismissThemeSheet -> {
                _uiState.update { it.copy(isThemeSheetVisible = false) }
            }

            is ChatAction.SelectThemePreset -> {
                val currentTheme = FakeChatStore.getChatTheme(conversationId)
                FakeChatStore.setChatTheme(
                    conversationId = conversationId,
                    theme = currentTheme.copy(presetId = action.presetId)
                )
            }

            is ChatAction.SetCustomWallpaper -> {
                val currentTheme = FakeChatStore.getChatTheme(conversationId)
                FakeChatStore.setChatTheme(
                    conversationId = conversationId,
                    theme = currentTheme.copy(
                        customWallpaperUri = action.uri,
                        wallpaperAlignment = action.alignment
                    )
                )
            }

            ChatAction.RemoveCustomWallpaper -> {
                val currentTheme = FakeChatStore.getChatTheme(conversationId)
                FakeChatStore.setChatTheme(
                    conversationId = conversationId,
                    theme = currentTheme.copy(customWallpaperUri = null)
                )
            }
        }
    }

    private fun handleSendMessage() {
        val currentState = _uiState.value
        val rawBody = currentState.inputText.trim()
        if (!currentState.isFriendStatusValid || rawBody.isEmpty()) return

        val pendingMsg = FakeChatStore.addOutgoingMessage(
            conversationId = conversationId,
            senderId = myUserId,
            body = rawBody
        ) ?: return
        _uiState.update { it.copy(inputText = "") }

        viewModelScope.launch {
            delay(800L)
            FakeChatStore.updateMessageSendState(
                messageId = pendingMsg.id,
                newState = PostSendState.SENT,
                seen = false
            )
        }

        scheduleSimulatedFriendReply()
    }

    private fun handleRetryMessage(messageId: String) {
        FakeChatStore.updateMessageSendState(
            messageId = messageId,
            newState = PostSendState.PENDING,
            seen = false
        )
        viewModelScope.launch {
            delay(800L)
            FakeChatStore.updateMessageSendState(
                messageId = messageId,
                newState = PostSendState.SENT,
                seen = false
            )
        }
    }

    private fun scheduleSimulatedFriendReply() {
        val friend = _uiState.value.friend ?: return
        replySimulationJob?.cancel()
        replySimulationJob = viewModelScope.launch {
            delay(2000L)
            friendSeenUpTo.value = FakeChatStore.clockNow()
            _uiState.update { it.copy(isFriendTyping = true) }

            delay(1500L)
            _uiState.update { it.copy(isFriendTyping = false) }

            val replyBody = CANNED_REPLIES[nextReplyIndex % CANNED_REPLIES.size]
            nextReplyIndex = (nextReplyIndex + 1) % CANNED_REPLIES.size

            FakeChatStore.addIncomingReply(
                conversationId = conversationId,
                friendId = friend.id,
                body = replyBody,
                isChatOpen = true
            )
        }
    }

    private fun sendEffect(effect: ChatEffect) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }

    companion object {
        private val DAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")
        private val ZONE_ID: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")

        private val CANNED_REPLIES = listOf(
            "Ảnh cháy quá trời ơi 🔥",
            "Đang ở đâu đấy? Tí qua đón nha!",
            "Haha nhìn mặt hài xỉu 😂",
            "Chụp thêm tấm nữa gửi qua Lokap xem nào 📸",
            "Uầy góc này đỉnh thực sự ✨"
        )

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
