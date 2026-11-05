```kotlin
// ============================================================================
// FILE: app/src/main/java/vn/nhom2/lokap/data/fake/FakeChatStore.kt
// ============================================================================
package vn.nhom2.lokap.data.fake

import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import vn.nhom2.lokap.domain.model.Conversation
import vn.nhom2.lokap.domain.model.Message
import vn.nhom2.lokap.domain.model.PostSendState
import vn.nhom2.lokap.domain.model.User

object FakeChatStore {

    private val _conversations = MutableStateFlow(SampleData.conversations)
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _messages = MutableStateFlow(SampleData.messages)
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    fun getConversation(conversationId: String): Conversation? {
        return _conversations.value.find { it.id == conversationId }
    }

    fun getOrCreateConversationWithFriend(friend: User): Conversation {
        val existing = _conversations.value.find { it.friend.id == friend.id }
        if (existing != null) return existing

        val newConversation = Conversation(
            id = "conv_${friend.id}",
            friend = friend,
            lastMessagePreview = "",
            lastMessageAt = SampleData.NOW,
            unreadCount = 0
        )
        _conversations.update { current -> listOf(newConversation) + current }
        return newConversation
    }

    fun markConversationRead(conversationId: String) {
        _conversations.update { list ->
            list.map { conv ->
                if (conv.id == conversationId && conv.unreadCount > 0) {
                    conv.copy(unreadCount = 0)
                } else {
                    conv
                }
            }
        }
    }

    fun addOutgoingMessage(
        conversationId: String,
        senderId: String,
        body: String,
        createdAt: Instant = Instant.now()
    ): Message {
        val trimmed = body.trim().take(1000)
        val message = Message(
            id = "msg_${UUID.randomUUID()}",
            conversationId = conversationId,
            senderId = senderId,
            body = trimmed,
            replyToPost = null,
            attachedPost = null,
            createdAt = createdAt,
            sendState = PostSendState.PENDING,
            seen = false
        )
        _messages.update { current -> current + message }
        updateConversationPreview(
            conversationId = conversationId,
            preview = previewForMessage(message),
            timestamp = createdAt,
            incrementUnread = false
        )
        return message
    }

    fun updateMessageSendState(messageId: String, newState: PostSendState, seen: Boolean = false) {
        _messages.update { list ->
            list.map { msg ->
                if (msg.id == messageId) {
                    msg.copy(sendState = newState, seen = seen)
                } else {
                    msg
                }
            }
        }
    }

    fun markMyMessagesSeen(conversationId: String, myUserId: String) {
        _messages.update { list ->
            list.map { msg ->
                if (msg.conversationId == conversationId && msg.senderId == myUserId && !msg.seen) {
                    msg.copy(seen = true)
                } else {
                    msg
                }
            }
        }
    }

    fun addIncomingReply(
        conversationId: String,
        friendId: String,
        body: String,
        createdAt: Instant = Instant.now(),
        isChatOpen: Boolean = true
    ): Message {
        val message = Message(
            id = "msg_${UUID.randomUUID()}",
            conversationId = conversationId,
            senderId = friendId,
            body = body,
            replyToPost = null,
            attachedPost = null,
            createdAt = createdAt,
            sendState = PostSendState.SENT,
            seen = isChatOpen
        )
        _messages.update { current -> current + message }
        updateConversationPreview(
            conversationId = conversationId,
            preview = previewForMessage(message),
            timestamp = createdAt,
            incrementUnread = !isChatOpen
        )
        return message
    }

    fun previewForConversation(conversation: Conversation, allMessages: List<Message>): String {
        val latest = allMessages
            .filter { it.conversationId == conversation.id }
            .maxByOrNull { it.createdAt }
        return when {
            latest == null -> conversation.lastMessagePreview
            latest.attachedPost != null -> "📷 Đã gửi một ảnh"
            !latest.body.isNullOrBlank() -> latest.body
            else -> conversation.lastMessagePreview
        }
    }

    private fun previewForMessage(message: Message): String {
        return when {
            message.attachedPost != null -> "📷 Đã gửi một ảnh"
            !message.body.isNullOrBlank() -> message.body
            else -> ""
        }
    }

    private fun updateConversationPreview(
        conversationId: String,
        preview: String,
        timestamp: Instant,
        incrementUnread: Boolean
    ) {
        _conversations.update { list ->
            list.map { conv ->
                if (conv.id == conversationId) {
                    conv.copy(
                        lastMessagePreview = preview,
                        lastMessageAt = timestamp,
                        unreadCount = if (incrementUnread) conv.unreadCount + 1 else conv.unreadCount
                    )
                } else {
                    conv
                }
            }
        }
    }
}


// ============================================================================
// FILE: app/src/main/java/vn/nhom2/lokap/feature/inbox/InboxContract.kt
// ============================================================================
package vn.nhom2.lokap.feature.inbox

import java.time.Instant
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.Conversation
import vn.nhom2.lokap.domain.model.User

data class InboxConversationItem(
    val conversation: Conversation,
    val displayPreview: String,
    val hasNewPostFromFriend: Boolean = false
)

data class InboxUiState(
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null,
    val items: List<InboxConversationItem> = emptyList(),
    val friends: List<User> = emptyList(),
    val isNewMessageSheetOpen: Boolean = false,
    val now: Instant = SampleData.NOW
)

sealed interface InboxAction {
    data class OpenConversation(val conversationId: String) : InboxAction
    data class MarkAsRead(val conversationId: String) : InboxAction
    data object OpenNewMessageSheet : InboxAction
    data object DismissNewMessageSheet : InboxAction
    data class SelectFriendToChat(val friend: User) : InboxAction
    data object Retry : InboxAction
    data object NavigateBack : InboxAction
}

sealed interface InboxEffect {
    data class NavigateToChat(val conversationId: String) : InboxEffect
    data object NavigateBack : InboxEffect
}


// ============================================================================
// FILE: app/src/main/java/vn/nhom2/lokap/feature/inbox/InboxViewModel.kt
// ============================================================================
package vn.nhom2.lokap.feature.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.data.fake.FakeChatStore
import vn.nhom2.lokap.data.fake.FakePostStore
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.User

class InboxViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(InboxUiState(isLoading = true))
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    private val _effects = Channel<InboxEffect>(Channel.BUFFERED)
    val effects: Flow<InboxEffect> = _effects.receiveAsFlow()

    init {
        observeInboxData()
    }

    private fun observeInboxData() {
        viewModelScope.launch {
            combine(
                FakeChatStore.conversations,
                FakeChatStore.messages,
                FakePostStore.posts
            ) { conversations, messages, posts ->
                val myId = SampleData.currentUser.id
                val authorIdsWithPosts = posts.map { it.author.id }.toSet()

                val sortedItems = conversations
                    .sortedByDescending { it.lastMessageAt }
                    .map { conv ->
                        val preview = FakeChatStore.previewForConversation(conv, messages)
                        InboxConversationItem(
                            conversation = conv,
                            displayPreview = preview.ifBlank { "Bắt đầu cuộc trò chuyện 👋" },
                            hasNewPostFromFriend = conv.friend.id in authorIdsWithPosts
                        )
                    }

                val availableFriends = SampleData.users.filter { it.id != myId }
                sortedItems to availableFriends
            }.collect { (sortedItems, availableFriends) ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = null,
                        items = sortedItems,
                        friends = availableFriends,
                        now = SampleData.NOW
                    )
                }
            }
        }
    }

    fun onAction(action: InboxAction) {
        when (action) {
            is InboxAction.OpenConversation -> {
                FakeChatStore.markConversationRead(action.conversationId)
                sendEffect(InboxEffect.NavigateToChat(action.conversationId))
            }

            is InboxAction.MarkAsRead -> {
                FakeChatStore.markConversationRead(action.conversationId)
            }

            InboxAction.OpenNewMessageSheet -> {
                _uiState.update { it.copy(isNewMessageSheetOpen = true) }
            }

            InboxAction.DismissNewMessageSheet -> {
                _uiState.update { it.copy(isNewMessageSheetOpen = false) }
            }

            is InboxAction.SelectFriendToChat -> {
                handleSelectFriend(action.friend)
            }

            InboxAction.Retry -> {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                observeInboxData()
            }

            InboxAction.NavigateBack -> {
                sendEffect(InboxEffect.NavigateBack)
            }
        }
    }

    private fun handleSelectFriend(friend: User) {
        _uiState.update { it.copy(isNewMessageSheetOpen = false) }
        val conversation = FakeChatStore.getOrCreateConversationWithFriend(friend)
        sendEffect(InboxEffect.NavigateToChat(conversation.id))
    }

    private fun sendEffect(effect: InboxEffect) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }
}


// ============================================================================
// FILE: app/src/main/java/vn/nhom2/lokap/feature/inbox/InboxScreen.kt
// ============================================================================
package vn.nhom2.lokap.feature.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import vn.nhom2.lokap.core.common.formatTimeAgo
import vn.nhom2.lokap.core.designsystem.component.AvatarRing
import vn.nhom2.lokap.core.designsystem.component.BadgeDot
import vn.nhom2.lokap.core.designsystem.component.EmptyState
import vn.nhom2.lokap.core.designsystem.component.ErrorState
import vn.nhom2.lokap.core.designsystem.component.GlassChip
import vn.nhom2.lokap.core.designsystem.component.OfflineBanner
import vn.nhom2.lokap.core.designsystem.component.ShimmerBox
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.User

fun NavGraphBuilder.inboxScreen(navController: NavController) {
    composable<Route.Inbox> {
        InboxRoute(
            onNavigateToChat = { conversationId ->
                navController.navigate(Route.Chat(conversationId = conversationId)) {
                    launchSingleTop = true
                }
            },
            onNavigateBack = {
                navController.popBackStack()
            }
        )
    }
}

@Composable
fun InboxRoute(
    onNavigateToChat: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: InboxViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is InboxEffect.NavigateToChat -> onNavigateToChat(effect.conversationId)
                InboxEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    InboxScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    uiState: InboxUiState,
    onAction: (InboxAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
    ) {
        InboxTopBar(
            onBackClick = { onAction(InboxAction.NavigateBack) },
            onNewMessageClick = { onAction(InboxAction.OpenNewMessageSheet) }
        )

        if (uiState.isOffline) {
            OfflineBanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.md, vertical = spacing.xs)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                uiState.isLoading -> {
                    InboxLoadingList()
                }

                uiState.errorMessage != null -> {
                    ErrorState(
                        message = uiState.errorMessage,
                        onRetry = { onAction(InboxAction.Retry) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.lg)
                    )
                }

                uiState.items.isEmpty() -> {
                    EmptyState(
                        title = "Chưa có tin nhắn. Trả lời một bức ảnh để bắt đầu",
                        subtitle = "Hoặc nhấn Soạn mới để gửi lời chào đến hội bạn thân",
                        actionLabel = "Soạn mới",
                        onActionClick = { onAction(InboxAction.OpenNewMessageSheet) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.lg)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = spacing.md,
                            vertical = spacing.sm
                        ),
                        verticalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        items(
                            items = uiState.items,
                            key = { it.conversation.id }
                        ) { item ->
                            SwipeableConversationRow(
                                item = item,
                                now = uiState.now,
                                onClick = {
                                    onAction(InboxAction.OpenConversation(item.conversation.id))
                                },
                                onMarkRead = {
                                    onAction(InboxAction.MarkAsRead(item.conversation.id))
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.isNewMessageSheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { onAction(InboxAction.DismissNewMessageSheet) },
            sheetState = sheetState,
            shape = LokapTheme.shapes.sheet,
            containerColor = colors.surface,
            contentColor = colors.textPrimary
        ) {
            NewMessageFriendPickerSheet(
                friends = uiState.friends,
                onSelectFriend = { friend ->
                    onAction(InboxAction.SelectFriendToChat(friend))
                }
            )
        }
    }
}

@Composable
private fun InboxTopBar(
    onBackClick: () -> Unit,
    onNewMessageClick: () -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = colors.textPrimary
                )
            }
            Text(
                text = "Tin nhắn",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )
        }

        GlassChip(
            label = "Soạn mới",
            leadingIcon = Icons.Default.Add,
            selected = true,
            onClick = onNewMessageClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableConversationRow(
    item: InboxConversationItem,
    now: java.time.Instant,
    onClick: () -> Unit,
    onMarkRead: () -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onMarkRead()
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shapes.card)
                    .background(colors.surfaceHigh)
                    .padding(horizontal = spacing.md),
                contentAlignment = Alignment.CenterEnd
            ) {
                TextButton(onClick = onMarkRead) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(spacing.xs))
                    Text(
                        text = "Đánh dấu đã đọc",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.primary
                    )
                }
            }
        }
    ) {
        ConversationRowContent(
            item = item,
            now = now,
            onClick = onClick
        )
    }
}

@Composable
private fun ConversationRowContent(
    item: InboxConversationItem,
    now: java.time.Instant,
    onClick: () -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes
    val conv = item.conversation
    val isUnread = conv.unreadCount > 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.card)
            .background(if (isUnread) colors.surfaceHigh else colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarRing(
            avatarUrl = conv.friend.avatarUrl,
            displayName = conv.friend.displayName,
            size = 52.dp,
            hasNewPost = item.hasNewPostFromFriend
        )

        Spacer(modifier = Modifier.width(spacing.md))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conv.friend.displayName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(spacing.sm))

                Text(
                    text = formatTimeAgo(conv.lastMessageAt, now),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isUnread) colors.primary else colors.textTertiary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.displayPreview,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (isUnread) colors.textPrimary else colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (conv.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(spacing.sm))
                    BadgeDot(
                        count = conv.unreadCount,
                        modifier = Modifier
                    )
                }
            }
        }
    }
}

@Composable
private fun NewMessageFriendPickerSheet(
    friends: List<User>,
    onSelectFriend: (User) -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.sm)
    ) {
        Text(
            text = "Trò chuyện mới",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = spacing.xs, vertical = spacing.xs)
        )
        Text(
            text = "Chọn một người bạn để gửi tin nhắn hoặc ảnh Locket riêng",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            modifier = Modifier.padding(horizontal = spacing.xs)
        )

        Spacer(modifier = Modifier.height(spacing.md))

        if (friends.isEmpty()) {
            EmptyState(
                title = "Chưa có bạn bè nào",
                subtitle = "Hãy kết bạn để bắt đầu nhắn tin trên Lokap",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.xl)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = spacing.xl),
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                items(
                    items = friends,
                    key = { it.id }
                ) { friend ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.card)
                            .background(colors.surfaceHigh)
                            .clickable { onSelectFriend(friend) }
                            .padding(horizontal = spacing.md, vertical = spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarRing(
                            avatarUrl = friend.avatarUrl,
                            displayName = friend.displayName,
                            size = 44.dp
                        )
                        Spacer(modifier = Modifier.width(spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = friend.displayName,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "@${friend.username}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                        Text(
                            text = "Nhắn tin",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = colors.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InboxLoadingList() {
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        repeat(6) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp),
                shape = shapes.card
            )
        }
    }
}

@Preview(name = "Inbox - Đầy đủ", showBackground = true)
@Composable
private fun InboxScreenPopulatedPreview() {
    LokapTheme {
        val sampleItems = SampleData.conversations.mapIndexed { index, conv ->
            InboxConversationItem(
                conversation = conv,
                displayPreview = if (index == 0) "📷 Đã gửi một ảnh" else conv.lastMessagePreview,
                hasNewPostFromFriend = index % 2 == 0
            )
        }
        InboxScreen(
            uiState = InboxUiState(
                items = sampleItems,
                friends = SampleData.users
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Inbox - Trống", showBackground = true)
@Composable
private fun InboxScreenEmptyPreview() {
    LokapTheme {
        InboxScreen(
            uiState = InboxUiState(items = emptyList()),
            onAction = {}
        )
    }
}

@Preview(name = "Inbox - Đang tải", showBackground = true)
@Composable
private fun InboxScreenLoadingPreview() {
    LokapTheme {
        InboxScreen(
            uiState = InboxUiState(isLoading = true),
            onAction = {}
        )
    }
}

@Preview(name = "Inbox - Lỗi & Offline", showBackground = true)
@Composable
private fun InboxScreenErrorOfflinePreview() {
    LokapTheme {
        InboxScreen(
            uiState = InboxUiState(
                isOffline = true,
                errorMessage = "Không thể đồng bộ hộp thư. Kiểm tra kết nối mạng của bạn."
            ),
            onAction = {}
        )
    }
}


// ============================================================================
// FILE: app/src/main/java/vn/nhom2/lokap/feature/chat/ChatContract.kt
// ============================================================================
package vn.nhom2.lokap.feature.chat

import java.time.Instant
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.Message
import vn.nhom2.lokap.domain.model.User

sealed interface ChatTimelineItem {
    val key: String

    data class MessageNode(
        val message: Message,
        val isMine: Boolean,
        val isLatestMyMessage: Boolean
    ) : ChatTimelineItem {
        override val key: String = "msg_${message.id}"
    }

    data class DateHeader(
        val id: String,
        val label: String
    ) : ChatTimelineItem {
        override val key: String = "date_$id"
    }
}

data class ChatUiState(
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null,
    val conversationId: String = "",
    val friend: User? = null,
    val isFriendStatusValid: Boolean = true,
    val timelineItems: List<ChatTimelineItem> = emptyList(),
    val inputText: String = "",
    val isFriendTyping: Boolean = false,
    val now: Instant = SampleData.NOW
) {
    val canSend: Boolean
        get() = isFriendStatusValid && inputText.trim().isNotEmpty() && inputText.length <= 1000
}

sealed interface ChatAction {
    data class InputChanged(val text: String) : ChatAction
    data object SendMessage : ChatAction
    data class RetryMessage(val messageId: String) : ChatAction
    data object OpenCameraForDirectShot : ChatAction
    data class OpenPostDetail(val postId: String) : ChatAction
    data object OpenFriendProfile : ChatAction
    data object RetryLoad : ChatAction
    data object NavigateBack : ChatAction
}

sealed interface ChatEffect {
    data object NavigateBack : ChatEffect
    data object NavigateToCamera : ChatEffect
    data class NavigateToPostDetail(val postId: String) : ChatEffect
    data class NavigateToProfile(val userId: String) : ChatEffect
}


// ============================================================================
// FILE: app/src/main/java/vn/nhom2/lokap/feature/chat/ChatViewModel.kt
// ============================================================================
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
import vn.nhom2.lokap.domain.model.Message
import vn.nhom2.lokap.domain.model.PostSendState

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

    private var replySimulationJob: Job? = null
    private var nextReplyIndex: Int = 0

    init {
        FakeChatStore.markConversationRead(conversationId)
        observeConversationAndMessages()
    }

    private fun observeConversationAndMessages() {
        viewModelScope.launch {
            combine(
                FakeChatStore.conversations,
                FakeChatStore.messages
            ) { conversations, allMessages ->
                val conv = conversations.find { it.id == conversationId }
                val convMessages = allMessages
                    .filter { it.conversationId == conversationId }
                    .sortedBy { it.createdAt }
                conv to convMessages
            }.collect { (conv, convMessages) ->
                if (conv == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Không tìm thấy cuộc trò chuyện này."
                        )
                    }
                    return@collect
                }

                val isStillFriend = SampleData.users.any { it.id == conv.friend.id }
                val timeline = buildReverseTimeline(
                    messagesAscending = convMessages,
                    myUserId = myUserId,
                    now = SampleData.NOW
                )

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = null,
                        friend = conv.friend,
                        isFriendStatusValid = isStillFriend,
                        timelineItems = timeline,
                        now = SampleData.NOW
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
                // TODO tuần 4: chế độ gửi riêng (audience DIRECT) cho đúng friend hiện tại
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
        }
    }

    private fun handleSendMessage() {
        val currentState = _uiState.value
        val rawBody = currentState.inputText.trim()
        if (!currentState.isFriendStatusValid || rawBody.isEmpty()) return

        _uiState.update { it.copy(inputText = "") }

        val messageTimestamp = Instant.now()
        val pendingMsg = FakeChatStore.addOutgoingMessage(
            conversationId = conversationId,
            senderId = myUserId,
            body = rawBody,
            createdAt = messageTimestamp
        )

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
            FakeChatStore.markMyMessagesSeen(conversationId, myUserId)
            _uiState.update { it.copy(isFriendTyping = true) }

            delay(1500L)
            _uiState.update { it.copy(isFriendTyping = false) }

            val replyBody = CANNED_REPLIES[nextReplyIndex % CANNED_REPLIES.size]
            nextReplyIndex = (nextReplyIndex + 1) % CANNED_REPLIES.size

            FakeChatStore.addIncomingReply(
                conversationId = conversationId,
                friendId = friend.id,
                body = replyBody,
                createdAt = Instant.now(),
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


// ============================================================================
// FILE: app/src/main/java/vn/nhom2/lokap/feature/chat/ChatScreen.kt
// ============================================================================
package vn.nhom2.lokap.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import coil3.compose.AsyncImage
import vn.nhom2.lokap.core.common.formatTimeAgo
import vn.nhom2.lokap.core.designsystem.component.AvatarRing
import vn.nhom2.lokap.core.designsystem.component.EmptyState
import vn.nhom2.lokap.core.designsystem.component.ErrorState
import vn.nhom2.lokap.core.designsystem.component.OfflineBanner
import vn.nhom2.lokap.core.designsystem.component.ShimmerBox
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.Message
import vn.nhom2.lokap.domain.model.Post
import vn.nhom2.lokap.domain.model.PostSendState
import vn.nhom2.lokap.domain.model.User

fun NavGraphBuilder.chatScreen(navController: NavController) {
    composable<Route.Chat> {
        ChatRoute(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToCamera = {
                navController.navigate(Route.Camera) {
                    launchSingleTop = true
                }
            },
            onNavigateToPostDetail = { postId ->
                navController.navigate(Route.PostDetail(id = postId)) {
                    launchSingleTop = true
                }
            },
            onNavigateToProfile = { userId ->
                navController.navigate(Route.Profile(userId = userId)) {
                    launchSingleTop = true
                }
            }
        )
    }
}

@Composable
fun ChatRoute(
    onNavigateBack: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ChatEffect.NavigateBack -> onNavigateBack()
                ChatEffect.NavigateToCamera -> onNavigateToCamera()
                is ChatEffect.NavigateToPostDetail -> onNavigateToPostDetail(effect.postId)
                is ChatEffect.NavigateToProfile -> onNavigateToProfile(effect.userId)
            }
        }
    }

    ChatScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onAction: (ChatAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .imePadding()
    ) {
        ChatTopBar(
            friend = uiState.friend,
            isFriendTyping = uiState.isFriendTyping,
            onBackClick = { onAction(ChatAction.NavigateBack) },
            onProfileClick = { onAction(ChatAction.OpenFriendProfile) }
        )

        if (uiState.isOffline) {
            OfflineBanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.md, vertical = spacing.xs)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                uiState.isLoading -> {
                    ChatLoadingSkeleton()
                }

                uiState.errorMessage != null -> {
                    ErrorState(
                        message = uiState.errorMessage,
                        onRetry = { onAction(ChatAction.RetryLoad) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.lg)
                    )
                }

                uiState.timelineItems.isEmpty() -> {
                    val friendName = uiState.friend?.displayName ?: "bạn mới"
                    EmptyState(
                        title = "Gửi lời chào đến $friendName 👋",
                        subtitle = "Chụp một tấm ảnh Locket riêng hoặc nhắn gì đó thật vui",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.lg)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        reverseLayout = true,
                        contentPadding = PaddingValues(
                            horizontal = spacing.md,
                            vertical = spacing.md
                        ),
                        verticalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        items(
                            items = uiState.timelineItems,
                            key = { it.key }
                        ) { item ->
                            when (item) {
                                is ChatTimelineItem.DateHeader -> {
                                    ChatDateSeparator(label = item.label)
                                }

                                is ChatTimelineItem.MessageNode -> {
                                    ChatMessageBubbleItem(
                                        node = item,
                                        now = uiState.now,
                                        onPostClick = { postId ->
                                            onAction(ChatAction.OpenPostDetail(postId))
                                        },
                                        onRetryClick = { messageId ->
                                            onAction(ChatAction.RetryMessage(messageId))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        ChatBottomInputBar(
            inputText = uiState.inputText,
            canSend = uiState.canSend,
            isFriendStatusValid = uiState.isFriendStatusValid,
            onInputChanged = { onAction(ChatAction.InputChanged(it)) },
            onCameraClick = { onAction(ChatAction.OpenCameraForDirectShot) },
            onSendClick = { onAction(ChatAction.SendMessage) }
        )
    }
}

@Composable
private fun ChatTopBar(
    friend: User?,
    isFriendTyping: Boolean,
    onBackClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(horizontal = spacing.sm, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Quay lại",
                tint = colors.textPrimary
            )
        }

        if (friend != null) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(LokapTheme.shapes.pill)
                    .clickable(onClick = onProfileClick)
                    .padding(horizontal = spacing.xs, vertical = spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarRing(
                    avatarUrl = friend.avatarUrl,
                    displayName = friend.displayName,
                    size = 40.dp,
                    onClick = onProfileClick
                )
                Spacer(modifier = Modifier.width(spacing.sm))
                Column {
                    Text(
                        text = friend.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitleText = if (isFriendTyping) "Đang nhập…" else "@${friend.username}"
                    val subtitleColor = if (isFriendTyping) colors.primary else colors.textSecondary
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isFriendTyping) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = subtitleColor
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ChatDateSeparator(label: String) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.xs),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = colors.textSecondary,
            modifier = Modifier
                .clip(shapes.pill)
                .background(colors.surface)
                .padding(horizontal = spacing.md, vertical = spacing.xs)
        )
    }
}

@Composable
private fun ChatMessageBubbleItem(
    node: ChatTimelineItem.MessageNode,
    now: java.time.Instant,
    onPostClick: (String) -> Unit,
    onRetryClick: (String) -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes
    val message = node.message
    val isMine = node.isMine
    val isPending = message.sendState == PostSendState.PENDING
    val isFailed = message.sendState == PostSendState.FAILED

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isPending) 0.58f else 1f),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        if (message.replyToPost != null) {
            ReplyToPostHeader(
                post = message.replyToPost,
                isMine = isMine,
                onClick = { onPostClick(message.replyToPost.id) }
            )
            Spacer(modifier = Modifier.height(spacing.xs))
        }

        if (message.attachedPost != null) {
            AttachedLocketPostCard(
                post = message.attachedPost,
                onClick = { onPostClick(message.attachedPost.id) }
            )
            if (!message.body.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(spacing.xs))
            }
        }

        if (!message.body.isNullOrBlank()) {
            val bubbleBackground = if (isMine) colors.primary else colors.surfaceHigh
            val bubbleTextColor = if (isMine) colors.onPrimary else colors.textPrimary

            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(shapes.card)
                    .background(bubbleBackground)
                    .padding(horizontal = spacing.md, vertical = spacing.sm)
            ) {
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = bubbleTextColor
                )
            }
        }

        MessageDeliveryFooter(
            message = message,
            isMine = isMine,
            isLatestMyMessage = node.isLatestMyMessage,
            isPending = isPending,
            isFailed = isFailed,
            now = now,
            onRetryClick = { onRetryClick(message.id) }
        )
    }
}

@Composable
private fun ReplyToPostHeader(
    post: Post,
    isMine: Boolean,
    onClick: () -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes

    Row(
        modifier = Modifier
            .clip(shapes.card)
            .background(colors.surface)
            .border(1.dp, colors.outline, shapes.card)
            .clickable(onClick = onClick)
            .padding(spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = post.imageUrl,
            contentDescription = "Ảnh được trả lời",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceHigh)
        )
        Spacer(modifier = Modifier.width(spacing.sm))
        Column(modifier = Modifier.padding(end = spacing.sm)) {
            Text(
                text = "Trả lời ảnh",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isMine) colors.primary else colors.accentLilac
            )
            if (!post.caption.isNullOrBlank()) {
                Text(
                    text = post.caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 160.dp)
                )
            }
        }
    }
}

@Composable
private fun AttachedLocketPostCard(
    post: Post,
    onClick: () -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val cardShape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .size(220.dp)
            .clip(cardShape)
            .background(colors.surfaceHigh)
            .border(1.5.dp, colors.primarySoft, cardShape)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = post.imageUrl,
            contentDescription = post.caption ?: "Ảnh Locket",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (!post.caption.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(spacing.sm)
            ) {
                Text(
                    text = post.caption,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(LokapTheme.shapes.pill)
                        .background(colors.surface.copy(alpha = 0.82f))
                        .padding(horizontal = spacing.sm, vertical = spacing.xs)
                        .align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun MessageDeliveryFooter(
    message: Message,
    isMine: Boolean,
    isLatestMyMessage: Boolean,
    isPending: Boolean,
    isFailed: Boolean,
    now: java.time.Instant,
    onRetryClick: () -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    when {
        isMine && isPending -> {
            Text(
                text = "Đang gửi…",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textTertiary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        isMine && isFailed -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = "Gửi thất bại",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.error
                )
                Spacer(modifier = Modifier.width(spacing.xs))
                TextButton(
                    onClick = onRetryClick,
                    contentPadding = PaddingValues(horizontal = spacing.xs, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Thử lại",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = colors.primary
                    )
                }
            }
        }

        isMine && isLatestMyMessage -> {
            val statusLabel = if (message.seen) "Đã xem" else "Đã gửi"
            Text(
                text = "$statusLabel • ${formatTimeAgo(message.createdAt, now)}",
                style = MaterialTheme.typography.labelSmall,
                color = if (message.seen) colors.primarySoft else colors.textTertiary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        else -> {
            Text(
                text = formatTimeAgo(message.createdAt, now),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textTertiary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun ChatBottomInputBar(
    inputText: String,
    canSend: Boolean,
    isFriendStatusValid: Boolean,
    onInputChanged: (String) -> Unit,
    onCameraClick: () -> Unit,
    onSendClick: () -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes

    if (!isFriendStatusValid) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = spacing.md, vertical = spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(spacing.sm))
            Text(
                text = "Bạn không thể nhắn tin với người này",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = colors.textSecondary
            )
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // TODO tuần 4: chế độ gửi riêng (mở Camera với target friend hiện tại)
        IconButton(
            onClick = onCameraClick,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(colors.surfaceHigh)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Chụp ảnh gửi riêng",
                tint = colors.primary
            )
        }

        Spacer(modifier = Modifier.width(spacing.sm))

        Box(
            modifier = Modifier
                .weight(1f)
                .clip(shapes.pill)
                .background(colors.surfaceHigh)
                .border(1.dp, colors.outline, shapes.pill)
                .padding(horizontal = spacing.md, vertical = spacing.sm)
        ) {
            if (inputText.isEmpty()) {
                Text(
                    text = "Nhắn tin…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textTertiary
                )
            }
            BasicTextField(
                value = inputText,
                onValueChange = { newText ->
                    if (newText.length <= 1000) {
                        onInputChanged(newText)
                    }
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.primary),
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.width(spacing.sm))

        IconButton(
            onClick = onSendClick,
            enabled = canSend,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (canSend) colors.primary else colors.surfaceHigh)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Gửi tin nhắn",
                tint = if (canSend) colors.onPrimary else colors.textTertiary
            )
        }
    }
}

@Composable
private fun ChatLoadingSkeleton() {
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        repeat(5) { idx ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (idx % 2 == 0) Arrangement.End else Arrangement.Start
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .width(if (idx % 2 == 0) 200.dp else 230.dp)
                        .height(52.dp),
                    shape = shapes.card
                )
            }
        }
    }
}

@Preview(name = "Chat - Đang trò chuyện", showBackground = true)
@Composable
private fun ChatScreenActivePreview() {
    LokapTheme {
        val friend = SampleData.users.firstOrNull { it.id != SampleData.currentUser.id }
            ?: SampleData.currentUser
        val samplePost = SampleData.posts.firstOrNull()
        val messages = listOf(
            Message(
                id = "m1",
                conversationId = "c1",
                senderId = friend.id,
                body = "Ê nhìn tấm ảnh mới chụp ở Đà Nẵng nè!",
                replyToPost = null,
                attachedPost = samplePost,
                createdAt = SampleData.NOW.minusSeconds(3600),
                sendState = PostSendState.SENT,
                seen = true
            ),
            Message(
                id = "m2",
                conversationId = "c1",
                senderId = SampleData.currentUser.id,
                body = "Màu đẹp thực sự luôn á 🔥",
                replyToPost = samplePost,
                attachedPost = null,
                createdAt = SampleData.NOW.minusSeconds(300),
                sendState = PostSendState.SENT,
                seen = true
            )
        )
        val timeline = ChatViewModel.buildReverseTimeline(
            messagesAscending = messages,
            myUserId = SampleData.currentUser.id,
            now = SampleData.NOW
        )
        ChatScreen(
            uiState = ChatUiState(
                conversationId = "c1",
                friend = friend,
                timelineItems = timeline,
                inputText = "Tối nay đi cà phê không?",
                isFriendTyping = true
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Chat - Cuộc trò chuyện mới", showBackground = true)
@Composable
private fun ChatScreenNewConversationPreview() {
    LokapTheme {
        val friend = SampleData.users.firstOrNull { it.id != SampleData.currentUser.id }
            ?: SampleData.currentUser
        ChatScreen(
            uiState = ChatUiState(
                conversationId = "c_new",
                friend = friend,
                timelineItems = emptyList()
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Chat - Không còn là bạn bè", showBackground = true)
@Composable
private fun ChatScreenNotFriendPreview() {
    LokapTheme {
        val friend = SampleData.users.firstOrNull { it.id != SampleData.currentUser.id }
            ?: SampleData.currentUser
        ChatScreen(
            uiState = ChatUiState(
                conversationId = "c_blocked",
                friend = friend,
                isFriendStatusValid = false,
                timelineItems = emptyList()
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Chat - Đang tải & Offline", showBackground = true)
@Composable
private fun ChatScreenLoadingOfflinePreview() {
    LokapTheme {
        ChatScreen(
            uiState = ChatUiState(
                isLoading = true,
                isOffline = true
            ),
            onAction = {}
        )
    }
}


// ============================================================================
// FILE: app/src/main/java/vn/nhom2/lokap/feature/notifications/NotificationsContract.kt
// ============================================================================
package vn.nhom2.lokap.feature.notifications

import java.time.Instant
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.AppNotification

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val unreadNotifications: List<AppNotification> = emptyList(),
    val earlierNotifications: List<AppNotification> = emptyList(),
    val handledFriendRequestIds: Map<String, Boolean> = emptyMap(),
    val now: Instant = SampleData.NOW
) {
    val isEmpty: Boolean
        get() = unreadNotifications.isEmpty() && earlierNotifications.isEmpty()

    val hasUnread: Boolean
        get() = unreadNotifications.isNotEmpty()
}

sealed interface NotificationsAction {
    data class ClickNotification(val notification: AppNotification) : NotificationsAction
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
    data object NavigateBack : NotificationsEffect
}


// ============================================================================
// FILE: app/src/main/java/vn/nhom2/lokap/feature/notifications/NotificationsViewModel.kt
// ============================================================================
package vn.nhom2.lokap.feature.notifications

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

    private val notificationsSource = MutableStateFlow(
        SampleData.notifications.sortedByDescending { it.createdAt }
    )
    private val handledRequests = MutableStateFlow>(emptyMap())

    private val _uiState = MutableStateFlow(NotificationsUiState(isLoading = true))
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<NotificationsEffect>(Channel.BUFFERED)
    val effects: Flow<NotificationsEffect> = _effects.receiveAsFlow()

    init {
        observeNotifications()
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            notificationsSource.collect { all ->
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
                notificationsSource.update { list ->
                    list.map { if (it.isRead) it else it.copy(isRead = true) }
                }
            }

            is NotificationsAction.AcceptFriendRequest -> {
                markRequestHandled(action.notificationId, accepted = true)
            }

            is NotificationsAction.DeclineFriendRequest -> {
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
                val targetPostId = notification.postId
                    ?: SampleData.posts.firstOrNull()?.id
                if (targetPostId != null) {
                    sendEffect(NotificationsEffect.NavigateToPostDetail(targetPostId))
                }
            }

            NotificationType.FRIEND_REQUEST,
            NotificationType.FRIEND_ACCEPTED -> {
                sendEffect(NotificationsEffect.NavigateToFriends)
            }

            NotificationType.NEW_MESSAGE -> {
                sendEffect(NotificationsEffect.NavigateToInbox)
            }
        }
    }

    private fun markSingleAsRead(notificationId: String) {
        notificationsSource.update { list ->
            list.map { item ->
                if (item.id == notificationId && !item.isRead) {
                    item.copy(isRead = true)
                } else {
                    item
                }
            }
        }
    }

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


// ============================================================================
// FILE: app/src/main/java/vn/nhom2/lokap/feature/notifications/NotificationsScreen.kt
// ============================================================================
package vn.nhom2.lokap.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import vn.nhom2.lokap.core.common.formatTimeAgo
import vn.nhom2.lokap.core.designsystem.component.AvatarRing
import vn.nhom2.lokap.core.designsystem.component.EmptyState
import vn.nhom2.lokap.core.designsystem.component.ErrorState
import vn.nhom2.lokap.core.designsystem.component.GhostButton
import vn.nhom2.lokap.core.designsystem.component.GlassChip
import vn.nhom2.lokap.core.designsystem.component.PinkButton
import vn.nhom2.lokap.core.designsystem.component.ShimmerBox
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.AppNotification
import vn.nhom2.lokap.domain.model.NotificationType

fun NavGraphBuilder.notificationsScreen(navController: NavController) {
    composable<Route.Notifications> {
        NotificationsRoute(
            onNavigateToPostDetail = { postId ->
                navController.navigate(Route.PostDetail(id = postId)) {
                    launchSingleTop = true
                }
            },
            onNavigateToFriends = {
                navController.navigate(Route.Friends) {
                    launchSingleTop = true
                }
            },
            onNavigateToInbox = {
                navController.navigate(Route.Inbox) {
                    launchSingleTop = true
                }
            },
            onNavigateBack = {
                navController.popBackStack()
            }
        )
    }
}

@Composable
fun NotificationsRoute(
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: NotificationsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is NotificationsEffect.NavigateToPostDetail -> onNavigateToPostDetail(effect.postId)
                NotificationsEffect.NavigateToFriends -> onNavigateToFriends()
                NotificationsEffect.NavigateToInbox -> onNavigateToInbox()
                NotificationsEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    NotificationsScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@Composable
fun NotificationsScreen(
    uiState: NotificationsUiState,
    onAction: (NotificationsAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
    ) {
        NotificationsTopBar(
            hasUnread = uiState.hasUnread,
            onBackClick = { onAction(NotificationsAction.NavigateBack) },
            onMarkAllReadClick = { onAction(NotificationsAction.MarkAllAsRead) }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                uiState.isLoading -> {
                    NotificationsLoadingSkeleton()
                }

                uiState.errorMessage != null -> {
                    ErrorState(
                        message = uiState.errorMessage,
                        onRetry = { onAction(NotificationsAction.Retry) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.lg)
                    )
                }

                uiState.isEmpty -> {
                    EmptyState(
                        title = "Chưa có thông báo nào",
                        subtitle = "Khi bạn bè thả cảm xúc hoặc đăng ảnh Locket mới, thông báo sẽ hiện tại đây",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.lg)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = spacing.md,
                            vertical = spacing.sm
                        ),
                        verticalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        if (uiState.unreadNotifications.isNotEmpty()) {
                            item(key = "section_new") {
                                NotificationSectionHeader(title = "Mới")
                            }
                            items(
                                items = uiState.unreadNotifications,
                                key = { it.id }
                            ) { notification ->
                                NotificationRowItem(
                                    notification = notification,
                                    now = uiState.now,
                                    friendRequestDecision = uiState.handledFriendRequestIds[notification.id],
                                    onClick = {
                                        onAction(NotificationsAction.ClickNotification(notification))
                                    },
                                    onAcceptFriend = {
                                        onAction(NotificationsAction.AcceptFriendRequest(notification.id))
                                    },
                                    onDeclineFriend = {
                                        onAction(NotificationsAction.DeclineFriendRequest(notification.id))
                                    }
                                )
                            }
                        }

                        if (uiState.earlierNotifications.isNotEmpty()) {
                            item(key = "section_earlier") {
                                NotificationSectionHeader(title = "Trước đó")
                            }
                            items(
                                items = uiState.earlierNotifications,
                                key = { it.id }
                            ) { notification ->
                                NotificationRowItem(
                                    notification = notification,
                                    now = uiState.now,
                                    friendRequestDecision = uiState.handledFriendRequestIds[notification.id],
                                    onClick = {
                                        onAction(NotificationsAction.ClickNotification(notification))
                                    },
                                    onAcceptFriend = {
                                        onAction(NotificationsAction.AcceptFriendRequest(notification.id))
                                    },
                                    onDeclineFriend = {
                                        onAction(NotificationsAction.DeclineFriendRequest(notification.id))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationsTopBar(
    hasUnread: Boolean,
    onBackClick: () -> Unit,
    onMarkAllReadClick: () -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = colors.textPrimary
                )
            }
            Text(
                text = "Thông báo",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )
        }

        GlassChip(
            label = "Đọc tất cả",
            leadingIcon = Icons.Default.Done,
            selected = hasUnread,
            onClick = if (hasUnread) onMarkAllReadClick else null
        )
    }
}

@Composable
private fun NotificationSectionHeader(title: String) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = colors.textSecondary,
        modifier = Modifier.padding(
            horizontal = spacing.xs,
            vertical = spacing.sm
        )
    )
}

@Composable
private fun NotificationRowItem(
    notification: AppNotification,
    now: java.time.Instant,
    friendRequestDecision: Boolean?,
    onClick: () -> Unit,
    onAcceptFriend: () -> Unit,
    onDeclineFriend: () -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes
    val isUnread = !notification.isRead

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.card)
            .background(if (isUnread) colors.surfaceHigh else colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (isUnread) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.primary)
                )
            }
        }

        if (notification.type == NotificationType.POST_HIDDEN) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Bài đăng bị ẩn",
                    tint = colors.accentYellow
                )
            }
        } else {
            AvatarRing(
                avatarUrl = notification.actor?.avatarUrl,
                displayName = notification.actor?.displayName ?: "Lokap",
                size = 44.dp
            )
        }

        Spacer(modifier = Modifier.width(spacing.md))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            Text(
                text = notification.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = colors.textPrimary
            )

            Text(
                text = formatTimeAgo(notification.createdAt, now),
                style = MaterialTheme.typography.labelSmall,
                color = if (isUnread) colors.primary else colors.textTertiary
            )

            if (notification.type == NotificationType.FRIEND_REQUEST) {
                Spacer(modifier = Modifier.height(spacing.xs))
                if (friendRequestDecision == null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PinkButton(
                            text = "Chấp nhận",
                            onClick = onAcceptFriend,
                            modifier = Modifier.weight(1f)
                        )
                        GhostButton(
                            text = "Từ chối",
                            onClick = onDeclineFriend,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    val statusText = if (friendRequestDecision) {
                        "Đã chấp nhận lời mời kết bạn"
                    } else {
                        "Đã từ chối lời mời kết bạn"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (friendRequestDecision) colors.success else colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationsLoadingSkeleton() {
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        repeat(6) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp),
                    shape = shapes.card
            )
        }
    }
}

@Preview(name = "Notifications - Đầy đủ", showBackground = true)
@Composable
private fun NotificationsScreenPopulatedPreview() {
    LokapTheme {
        val all = SampleData.notifications
        NotificationsScreen(
            uiState = NotificationsUiState(
                unreadNotifications = all.filter { !it.isRead },
                earlierNotifications = all.filter { it.isRead }
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Notifications - Trống", showBackground = true)
@Composable
private fun NotificationsScreenEmptyPreview() {
    LokapTheme {
        NotificationsScreen(
            uiState = NotificationsUiState(),
            onAction = {}
        )
    }
}

@Preview(name = "Notifications - Đang tải", showBackground = true)
@Composable
private fun NotificationsScreenLoadingPreview() {
    LokapTheme {
        NotificationsScreen(
            uiState = NotificationsUiState(isLoading = true),
            onAction = {}
        )
    }
}

@Preview(name = "Notifications - Lỗi", showBackground = true)
@Composable
private fun NotificationsScreenErrorPreview() {
    LokapTheme {
        NotificationsScreen(
            uiState = NotificationsUiState(
                errorMessage = "Không thể tải thông báo. Vui lòng thử lại sau."
            ),
            onAction = {}
        )
    }
}


// ============================================================================
// CẤU HÌNH THÊM VÀO AppNavHost.kt & DEPENDENCY
// ============================================================================
// 1. Thêm các import vào AppNavHost.kt:
// import vn.nhom2.lokap.feature.chat.chatScreen
// import vn.nhom2.lokap.feature.inbox.inboxScreen
// import vn.nhom2.lokap.feature.notifications.notificationsScreen
//
// 2. Thêm các dòng gọi vào trong NavHost(...) { ... }:
// inboxScreen(navController = navController)
// chatScreen(navController = navController)
// notificationsScreen(navController = navController)
//
// 3. Dependency mới: Không cần thêm.
```

<!-- tail -->
 true)
@Composable
private fun NotificationsScreenEmptyPreview() {
    LokapTheme {
        NotificationsScreen(
            uiState = NotificationsUiState(),
            onAction = {}
        )
    }
}

@Preview(name = "Notifications - Đang tải", showBackground = true)
@Composable
private fun NotificationsScreenLoadingPreview() {
    LokapTheme {
        NotificationsScreen(
            uiState = NotificationsUiState(isLoading = true),
            onAction = {}
        )
    }
}

@Preview(name = "Notifications - Lỗi", showBackground = true)
@Composable
private fun NotificationsScreenErrorPreview() {
    LokapTheme {
        NotificationsScreen(
            uiState = NotificationsUiState(
                errorMessage = "Không thể tải thông báo. Vui lòng thử lại sau."
            ),
            onAction = {}
        )
    }
}


// ============================================================================
// CẤU HÌNH THÊM VÀO AppNavHost.kt & DEPENDENCY
// ============================================================================
// 1. Thêm các import vào AppNavHost.kt:
// import vn.nhom2.lokap.feature.chat.chatScreen
// import vn.nhom2.lokap.feature.inbox.inboxScreen
// import vn.nhom2.lokap.feature.notifications.notificationsScreen
//
// 2. Thêm các dòng gọi vào trong NavHost(...) { ... }:
// inboxScreen(navController = navController)
// chatScreen(navController = navController)
// notificationsScreen(navController = navController)
//
// 3. Dependency mới: Không cần thêm.
