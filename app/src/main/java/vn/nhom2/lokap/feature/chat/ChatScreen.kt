package vn.nhom2.lokap.feature.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import androidx.navigation.compose.composable
import coil3.compose.AsyncImage
import vn.nhom2.lokap.core.common.formatTimeAgo
import vn.nhom2.lokap.core.designsystem.component.*
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.core.navigation.navigateSingleTop
import vn.nhom2.lokap.core.navigation.popBackStackSafely
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.*
private val WallpaperScrimBrush = Brush.verticalGradient(
    colors = listOf(
        Color.Black.copy(alpha = 0.20f),
        Color.Black.copy(alpha = 0.05f),
        Color.Black.copy(alpha = 0.20f)
    )
)

private val MineBubbleShape = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 16.dp,
    bottomStart = 16.dp,
    bottomEnd = 4.dp
)

private val OtherBubbleShape = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 16.dp,
    bottomStart = 4.dp,
    bottomEnd = 16.dp
)

private val CropOptions = listOf(
    "TOP" to "Phía trên",
    "CENTER" to "Ở giữa",
    "BOTTOM" to "Phía dưới"
)

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onAction: (ChatAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val themeConfig = uiState.themeConfig
    val activePreset = themeConfig.activePreset
    val hasCustomWallpaper = !themeConfig.customWallpaperUri.isNullOrBlank()

    var pendingWallpaperUri by remember { mutableStateOf<String?>(null) }

    val wallpaperAlignment = remember(themeConfig.wallpaperAlignment) {
        when (themeConfig.wallpaperAlignment) {
            "TOP" -> Alignment.TopCenter
            "BOTTOM" -> Alignment.BottomCenter
            else -> Alignment.Center
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (hasCustomWallpaper) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF181A20))
            ) {
                AsyncImage(
                    model = themeConfig.customWallpaperUri,
                    contentDescription = "Hình nền cuộc trò chuyện",
                    contentScale = ContentScale.Crop,
                    alignment = wallpaperAlignment,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.88f)
                        .blur(2.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(WallpaperScrimBrush)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            ChatTopBar(
                friend = uiState.friend,
                isFriendTyping = uiState.isFriendTyping,
                activePreset = activePreset,
                hasCustomWallpaper = hasCustomWallpaper,
                onBackClick = { onAction(ChatAction.NavigateBack) },
                onProfileClick = { onAction(ChatAction.OpenFriendProfile) },
                onThemeClick = { onAction(ChatAction.OpenThemeSheet) },
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
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
                            title = "Gửi lời chào đến $friendName",
                            subtitle = "Chụp một khoảnh khắc riêng hoặc nhắn gì đó cùng bạn bè",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(spacing.lg)
                        )
                    }

                    else -> {
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
                                horizontal = 12.dp,
                                vertical = 8.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = uiState.timelineItems,
                                key = { it.key },
                                contentType = {
                                    when (it) {
                                        is ChatTimelineItem.DateHeader -> 0
                                        is ChatTimelineItem.MessageNode -> 1
                                    }
                                }
                            ) { item ->
                                when (item) {
                                    is ChatTimelineItem.DateHeader -> {
                                        ChatDateSeparator(
                                            label = item.label,
                                            hasCustomWallpaper = hasCustomWallpaper
                                        )
                                    }

                                    is ChatTimelineItem.MessageNode -> {
                                        ChatMessageBubbleItem(
                                            node = item,
                                            now = uiState.now,
                                            themeConfig = themeConfig,
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
                activePreset = activePreset,
                hasCustomWallpaper = hasCustomWallpaper,
                onInputChanged = { onAction(ChatAction.InputChanged(it)) },
                onCameraClick = { onAction(ChatAction.OpenCameraForDirectShot) },
                onSendClick = { onAction(ChatAction.SendMessage) },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            )
        }

        if (uiState.isThemeSheetVisible) {
            ChatThemeBottomSheet(
                themeConfig = themeConfig,
                onSelectPreset = { onAction(ChatAction.SelectThemePreset(it)) },
                onRequestCropWallpaper = { uri ->
                    pendingWallpaperUri = uri
                },
                onRemoveCustomWallpaper = { onAction(ChatAction.RemoveCustomWallpaper) },
                onDismiss = { onAction(ChatAction.DismissThemeSheet) }
            )
        }

        if (pendingWallpaperUri != null) {
            WallpaperCropDialog(
                imageUri = pendingWallpaperUri!!,
                activePreset = activePreset,
                onConfirm = { alignment ->
                    onAction(ChatAction.SetCustomWallpaper(uri = pendingWallpaperUri!!, alignment = alignment))
                    pendingWallpaperUri = null
                },
                onDismiss = { pendingWallpaperUri = null }
            )
        }
    }
}

@Composable
private fun ChatTopBar(
    friend: User?,
    isFriendTyping: Boolean,
    activePreset: ChatThemePreset,
    hasCustomWallpaper: Boolean,
    onBackClick: () -> Unit,
    onProfileClick: () -> Unit,
    onThemeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors

    Surface(
        modifier = modifier,
        color = if (hasCustomWallpaper) Color.Black.copy(alpha = 0.48f) else colors.surface.copy(alpha = 0.95f),
        shadowElevation = if (hasCustomWallpaper) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = if (hasCustomWallpaper) Color.White else colors.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (friend != null) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onProfileClick)
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        AvatarRing(
                            avatarUrl = friend.avatarUrl,
                            displayName = friend.displayName,
                            size = 32.dp,
                            onClick = onProfileClick
                        )
                        OnlineIndicatorDot(
                            size = 8.dp,
                            isOnline = true
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = friend.displayName,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            ),
                            color = if (hasCustomWallpaper) Color.White else colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitleText = if (isFriendTyping) "Đang nhập…" else "Đang hoạt động"
                        val subtitleColor = if (isFriendTyping) activePreset.primaryColor else if (hasCustomWallpaper) Color.White.copy(alpha = 0.75f) else colors.textSecondary
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isFriendTyping) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 11.sp
                            ),
                            color = subtitleColor
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onThemeClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = "Đổi chủ đề",
                    tint = if (hasCustomWallpaper) Color.White else activePreset.primaryColor,
                    modifier = Modifier.size(17.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onProfileClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = "Thông tin",
                    tint = if (hasCustomWallpaper) Color.White.copy(alpha = 0.85f) else colors.textSecondary,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatDateSeparator(
    label: String,
    hasCustomWallpaper: Boolean
) {
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
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            ),
            color = if (hasCustomWallpaper) Color.White.copy(alpha = 0.85f) else colors.textSecondary,
            modifier = Modifier
                .clip(shapes.pill)
                .background(if (hasCustomWallpaper) Color.Black.copy(alpha = 0.45f) else colors.surface.copy(alpha = 0.9f))
                .padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun ChatMessageBubbleItem(
    node: ChatTimelineItem.MessageNode,
    now: java.time.Instant,
    themeConfig: ChatThemeConfig,
    onPostClick: (String) -> Unit,
    onRetryClick: (String) -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val message = node.message
    val isMine = node.isMine
    val isPending = message.sendState == PostSendState.PENDING
    val isFailed = message.sendState == PostSendState.FAILED
    val hasCustomWallpaper = !themeConfig.customWallpaperUri.isNullOrBlank()
    val activePreset = themeConfig.activePreset

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
                hasCustomWallpaper = hasCustomWallpaper,
                onClick = { onPostClick(message.replyToPost.id) }
            )
            Spacer(modifier = Modifier.height(spacing.xs))
        }

        if (message.attachedPost != null) {
            AttachedLocketPostCard(
                post = message.attachedPost,
                onClick = { onPostClick(message.attachedPost.id) }
            )
            if (!message.body.isNullOrBlank() && message.body != message.attachedPost.caption) {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        val shouldShowBody = !message.body.isNullOrBlank() &&
            (message.attachedPost == null || message.body != message.attachedPost.caption)

        if (shouldShowBody) {
            val bubbleShape = if (isMine) MineBubbleShape else OtherBubbleShape

            val bubbleModifier = if (isMine) {
                Modifier
                    .widthIn(max = 280.dp)
                    .clip(bubbleShape)
                    .background(activePreset.getBubbleBrush())
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            } else {
                Modifier
                    .widthIn(max = 280.dp)
                    .clip(bubbleShape)
                    .background(if (hasCustomWallpaper) Color.Black.copy(alpha = 0.68f) else colors.surfaceHigh)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            }

            val textColor = if (isMine) {
                activePreset.bubbleTextColor
            } else {
                if (hasCustomWallpaper) Color.White else colors.textPrimary
            }

            Box(modifier = bubbleModifier) {
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp
                    ),
                    color = textColor
                )
            }
        }

        MessageDeliveryFooter(
            message = message,
            isMine = isMine,
            isLatestMyMessage = node.isLatestMyMessage,
            isPending = isPending,
            isFailed = isFailed,
            activePreset = activePreset,
            hasCustomWallpaper = hasCustomWallpaper,
            now = now,
            onRetryClick = { onRetryClick(message.id) }
        )
    }
}

@Composable
private fun ReplyToPostHeader(
    post: Post,
    isMine: Boolean,
    hasCustomWallpaper: Boolean,
    onClick: () -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes

    Row(
        modifier = Modifier
            .clip(shapes.card)
            .background(if (hasCustomWallpaper) Color.Black.copy(alpha = 0.6f) else colors.surface.copy(alpha = 0.92f))
            .clickable(onClick = onClick)
            .padding(spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = post.imageUrl,
            contentDescription = "Ảnh được trả lời",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceHigh)
        )
        Spacer(modifier = Modifier.width(spacing.sm))
        Column(modifier = Modifier.padding(end = spacing.sm)) {
            Text(
                text = "Trả lời ảnh",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp
                ),
                color = if (isMine) colors.primary else colors.accentLilac
            )
            if (!post.caption.isNullOrBlank()) {
                Text(
                    text = post.caption,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = if (hasCustomWallpaper) Color.White.copy(alpha = 0.85f) else colors.textSecondary,
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
    val cardShape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .size(200.dp)
            .clip(cardShape)
            .background(colors.surfaceHigh)
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
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp
                    ),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(LokapTheme.shapes.pill)
                        .background(colors.surface.copy(alpha = 0.90f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
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
    activePreset: ChatThemePreset,
    hasCustomWallpaper: Boolean,
    now: java.time.Instant,
    onRetryClick: () -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    when {
        isMine && isPending -> {
            Text(
                text = "Đang gửi…",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                color = if (hasCustomWallpaper) Color.White.copy(alpha = 0.7f) else colors.textTertiary,
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
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
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
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Thử lại",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        ),
                        color = colors.primary
                    )
                }
            }
        }

        isMine && isLatestMyMessage -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = if (message.seen) Icons.Rounded.DoneAll else Icons.Rounded.Done,
                    contentDescription = null,
                    tint = if (message.seen) activePreset.primaryColor else if (hasCustomWallpaper) Color.White.copy(alpha = 0.75f) else colors.textTertiary,
                    modifier = Modifier.size(13.dp)
                )
                val statusLabel = if (message.seen) "Đã xem" else "Đã gửi"
                Text(
                    text = "$statusLabel • ${formatTimeAgo(message.createdAt, now)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    color = if (message.seen) activePreset.primaryColor else if (hasCustomWallpaper) Color.White.copy(alpha = 0.75f) else colors.textTertiary
                )
            }
        }

        else -> Unit
    }
}

@Composable
private fun ChatBottomInputBar(
    inputText: String,
    canSend: Boolean,
    isFriendStatusValid: Boolean,
    activePreset: ChatThemePreset,
    hasCustomWallpaper: Boolean,
    onInputChanged: (String) -> Unit,
    onCameraClick: () -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    if (!isFriendStatusValid) {
        Surface(
            modifier = modifier,
            color = if (hasCustomWallpaper) Color.Black.copy(alpha = 0.5f) else colors.surface.copy(alpha = 0.95f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.md, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(spacing.sm))
                Text(
                    text = "Bạn không thể nhắn tin với người này",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    ),
                    color = if (hasCustomWallpaper) Color.White.copy(alpha = 0.75f) else colors.textSecondary
                )
            }
        }
        return
    }

    Surface(
        modifier = modifier,
        color = if (hasCustomWallpaper) Color.Black.copy(alpha = 0.5f) else colors.surface.copy(alpha = 0.95f),
        shadowElevation = if (hasCustomWallpaper) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (hasCustomWallpaper) Color.White.copy(alpha = 0.16f) else activePreset.primaryColor.copy(alpha = 0.12f))
                    .clickable(onClick = onCameraClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Chụp ảnh gửi riêng",
                    tint = if (hasCustomWallpaper) Color.White else activePreset.primaryColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (hasCustomWallpaper) Color.White.copy(alpha = 0.14f) else colors.surfaceHigh)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                if (inputText.isEmpty()) {
                    Text(
                        text = "Nhắn tin…",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                        color = if (hasCustomWallpaper) Color.White.copy(alpha = 0.55f) else colors.textTertiary
                    )
                }
                BasicTextField(
                    value = inputText,
                    onValueChange = { newText ->
                        if (newText.length <= 1000) {
                            onInputChanged(newText)
                        }
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        color = if (hasCustomWallpaper) Color.White else colors.textPrimary
                    ),
                    cursorBrush = SolidColor(if (hasCustomWallpaper) Color.White else activePreset.primaryColor),
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (canSend) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(activePreset.primaryColor)
                        .clickable(onClick = onSendClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Gửi",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier.size(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Gửi",
                        tint = if (hasCustomWallpaper) Color.White.copy(alpha = 0.35f) else colors.textTertiary.copy(alpha = 0.5f),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatThemeBottomSheet(
    themeConfig: ChatThemeConfig,
    onSelectPreset: (String) -> Unit,
    onRequestCropWallpaper: (String) -> Unit,
    onRemoveCustomWallpaper: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onRequestCropWallpaper(uri.toString())
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Chủ đề & Hình nền",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = colors.textPrimary
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.surfaceHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (!themeConfig.customWallpaperUri.isNullOrBlank()) {
                            AsyncImage(
                                model = themeConfig.customWallpaperUri,
                                contentDescription = "Hình nền hiện tại",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.primary.copy(alpha = 0.12f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Image,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Column {
                            Text(
                                text = if (!themeConfig.customWallpaperUri.isNullOrBlank()) "Đang dùng ảnh thiết bị" else "Hình nền từ thư viện",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp
                                ),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Chọn ảnh và cắt chỉnh vừa màn hình",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = colors.textSecondary
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!themeConfig.customWallpaperUri.isNullOrBlank()) {
                            IconButton(
                                onClick = onRemoveCustomWallpaper,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Gỡ ảnh",
                                    tint = colors.error,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.primary,
                            modifier = Modifier
                                .height(32.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { galleryLauncher.launch("image/*") }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = if (!themeConfig.customWallpaperUri.isNullOrBlank()) "Đổi ảnh" else "Chọn ảnh",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Màu đơn sắc",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    ),
                    color = colors.textSecondary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ChatThemePresets.SOLID_PRESETS.forEach { preset ->
                        val isSelected = themeConfig.presetId == preset.id
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectPreset(preset.id) }
                                .padding(2.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(preset.primaryColor)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 0.dp,
                                        color = if (isSelected) colors.textPrimary else Color.Transparent,
                                        shape = CircleShape
                                    )
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = preset.nameVi,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) colors.primary else colors.textSecondary
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Gradient",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    ),
                    color = colors.textSecondary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ChatThemePresets.GRADIENT_PRESETS.forEach { preset ->
                        val isSelected = themeConfig.presetId == preset.id
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectPreset(preset.id) }
                                .padding(2.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(preset.getBubbleBrush())
                                    .border(
                                        width = if (isSelected) 2.5.dp else 0.dp,
                                        color = if (isSelected) colors.textPrimary else Color.Transparent,
                                        shape = CircleShape
                                    )
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = preset.nameVi,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) colors.primary else colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WallpaperCropDialog(
    imageUri: String,
    activePreset: ChatThemePreset,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LokapTheme.colors
    var selectedAlignment by remember { mutableStateOf("CENTER") }

    val alignment = when (selectedAlignment) {
        "TOP" -> Alignment.TopCenter
        "BOTTOM" -> Alignment.BottomCenter
        else -> Alignment.Center
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Crop,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Căn vị trí ảnh nền",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp
                            ),
                            color = colors.textPrimary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Huỷ",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF181A20))
                ) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Xem trước ảnh nền",
                        contentScale = ContentScale.Crop,
                        alignment = alignment,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.88f)
                            .blur(2.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(WallpaperScrimBrush)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.End
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp, 14.dp, 14.dp, 4.dp))
                                .background(activePreset.getBubbleBrush())
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Xem trước hình nền chat ✨",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = activePreset.bubbleTextColor
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CropOptions.forEach { (alignKey, label) ->
                        val isSelected = selectedAlignment == alignKey
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) colors.primary.copy(alpha = 0.15f) else colors.surfaceHigh,
                            border = if (isSelected) BorderStroke(1.dp, colors.primary) else null,
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedAlignment = alignKey }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) colors.primary else colors.textPrimary
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Huỷ",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.primary,
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onConfirm(selectedAlignment) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "Đặt làm hình nền",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }
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
                body = "Góc chụp đẹp ghê, đứng bên đường Trần Hưng Đạo hả?",
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
                body = "Đúng rồi, đứng ngay đầu cầu luôn.",
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
