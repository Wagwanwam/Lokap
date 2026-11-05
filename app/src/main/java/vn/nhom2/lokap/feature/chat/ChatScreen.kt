package vn.nhom2.lokap.feature.chat

import androidx.compose.foundation.lazy.rememberLazyListState
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
import vn.nhom2.lokap.core.navigation.navigateSingleTop
import vn.nhom2.lokap.core.navigation.popBackStackSafely
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.Message
import vn.nhom2.lokap.domain.model.Post
import vn.nhom2.lokap.domain.model.PostSendState
import vn.nhom2.lokap.domain.model.User

fun NavGraphBuilder.chatScreen(navController: NavController) {
    composable<Route.Chat> {
        ChatRoute(
            onNavigateBack = {
                navController.popBackStackSafely()
            },
            onNavigateToCamera = {
                navController.navigateSingleTop(Route.Camera)
            },
            onNavigateToPostDetail = { postId ->
                navController.navigateSingleTop(Route.PostDetail(id = postId))
            },
            onNavigateToProfile = { userId ->
                navController.navigateSingleTop(Route.Profile(userId = userId))
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
                    // Danh sách đảo chiều giữ vị trí theo key, tin mới chèn ở vị trí 0 sẽ nằm ngoài màn hình:
                    // mỗi khi mục mới nhất đổi thì cuộn về cuối cuộc trò chuyện
                    val listState = rememberLazyListState()
                    val newestKey = uiState.timelineItems.firstOrNull()?.key
                    LaunchedEffect(newestKey) {
                        if (newestKey != null) listState.animateScrollToItem(0)
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
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
