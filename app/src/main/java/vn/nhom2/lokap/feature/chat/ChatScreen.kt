package vn.nhom2.lokap.feature.chat

import android.content.ClipData
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.composable
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import vn.nhom2.lokap.core.common.formatTimeAgo
import vn.nhom2.lokap.core.designsystem.component.*
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.designsystem.theme.readableOn
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.core.navigation.navigateSingleTop
import vn.nhom2.lokap.core.navigation.popBackStackSafely
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
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val lifecycle = lifecycleOwner.lifecycle
        fun updateVisibility() = viewModel.onAction(
            ChatAction.VisibilityChanged(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        )
        val observer = LifecycleEventObserver { _, _ -> updateVisibility() }
        lifecycle.addObserver(observer)
        updateVisibility()
        onDispose {
            lifecycle.removeObserver(observer)
            viewModel.onAction(ChatAction.VisibilityChanged(false))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                // Bỏ focus ô nhập trước khi rời màn: bàn phím ẩn ngay, không che màn kế tiếp suốt animation
                ChatEffect.NavigateBack -> {
                    focusManager.clearFocus(force = true)
                    onNavigateBack()
                }
                ChatEffect.NavigateToCamera -> {
                    focusManager.clearFocus(force = true)
                    onNavigateToCamera()
                }
                is ChatEffect.NavigateToPostDetail -> {
                    focusManager.clearFocus(force = true)
                    onNavigateToPostDetail(effect.postId)
                }
                is ChatEffect.NavigateToProfile -> {
                    focusManager.clearFocus(force = true)
                    onNavigateToProfile(effect.userId)
                }
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
    if (uiState.editingMessage != null) ContentEditDialog("Sửa tin nhắn", uiState.editText, uiState.isSaving,
        { onAction(ChatAction.EditTextChanged(it)) }, { onAction(ChatAction.SaveEdit) }, { onAction(ChatAction.DismissEdit) })
    if (uiState.deletingMessageId != null) ContentDeleteDialog("Xoá tin nhắn?", uiState.isSaving,
        { onAction(ChatAction.ConfirmDelete) }, { onAction(ChatAction.DismissDelete) })
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
                            title = "Chào $friendName 👋",
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
                                            onEdit = { onAction(ChatAction.EditMessage(item.message)) },
                                            onDelete = { onAction(ChatAction.DeleteMessage(item.message.id)) },
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
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = if (hasCustomWallpaper) Color.White else colors.textPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }

            if (friend != null) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onProfileClick)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        AvatarRing(
                            avatarUrl = friend.avatarUrl,
                            displayName = friend.displayName,
                            size = 42.dp,
                            onClick = onProfileClick
                        )
                        OnlineIndicatorDot(
                            size = 11.dp,
                            isOnline = true
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = friend.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = if (hasCustomWallpaper) Color.White else colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitleText = if (isFriendTyping) "Đang nhập…" else "Đang hoạt động"
                        val subtitleColor = if (isFriendTyping) activePreset.primaryColor else if (hasCustomWallpaper) Color.White.copy(alpha = 0.75f) else colors.textSecondary
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isFriendTyping) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 14.sp
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
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onThemeClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = "Đổi chủ đề",
                    tint = if (hasCustomWallpaper) Color.White else activePreset.primaryColor.readableOn(colors.surface, colors.textSecondary),
                    modifier = Modifier.size(26.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onProfileClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = "Thông tin",
                    tint = if (hasCustomWallpaper) Color.White.copy(alpha = 0.85f) else colors.textSecondary,
                    modifier = Modifier.size(26.dp)
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
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 13.5.sp
            ),
            color = if (hasCustomWallpaper) Color.White.copy(alpha = 0.85f) else colors.textSecondary,
            modifier = Modifier
                .clip(shapes.pill)
                .background(if (hasCustomWallpaper) Color.Black.copy(alpha = 0.45f) else colors.surface.copy(alpha = 0.9f))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ChatMessageBubbleItem(
    node: ChatTimelineItem.MessageNode,
    now: java.time.Instant,
    themeConfig: ChatThemeConfig,
    onPostClick: (String) -> Unit,
    onRetryClick: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val message = node.message
    val isMine = node.isMine
    val isPending = message.sendState == PostSendState.PENDING
    val isFailed = message.sendState == PostSendState.FAILED
    val hasCustomWallpaper = !themeConfig.customWallpaperUri.isNullOrBlank()
    val activePreset = themeConfig.activePreset

    val shouldShowBody = !message.body.isNullOrBlank() &&
        (message.attachedPost == null || message.body != message.attachedPost.caption)
    // Nhấn giữ tin nhắn → popup tuỳ chọn. Chỉ sửa/xoá tin của mình đã gửi xong.
    val canCopy = shouldShowBody
    val canEdit = isMine && !isPending && shouldShowBody
    val canDelete = isMine && !isPending
    val hasMenu = canCopy || canEdit || canDelete
    var isMenuOpen by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val openMenu: (() -> Unit)? = if (hasMenu) {
        {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            isMenuOpen = true
        }
    } else {
        null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isPending) 0.58f else 1f),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        // Box làm mốc cho popup: menu hiện ngay dưới ảnh/bong bóng được nhấn giữ
        Box {
            Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
                if (message.replyToPost != null) {
                    // Tin nhắn gửi từ thanh dưới ảnh: ảnh lớn, nội dung tin nằm trên ảnh (kiểu Locket)
                    PhotoReplyCard(
                        post = message.replyToPost,
                        body = message.body,
                        now = now,
                        onClick = { onPostClick(message.replyToPost.id) },
                        onLongClick = openMenu
                    )
                } else {
                if (message.attachedPost != null) {
                    AttachedLocketPostCard(
                        post = message.attachedPost,
                        onClick = { onPostClick(message.attachedPost.id) },
                        onLongClick = openMenu
                    )
                    if (shouldShowBody) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                if (shouldShowBody) {
                    val bubbleShape = if (isMine) MineBubbleShape else OtherBubbleShape
                    val bubbleBackground = if (isMine) {
                        Modifier.background(activePreset.getBubbleBrush())
                    } else {
                        Modifier.background(if (hasCustomWallpaper) Color.Black.copy(alpha = 0.68f) else colors.surfaceHigh)
                    }

                    val textColor = if (isMine) {
                        activePreset.bubbleTextColor
                    } else {
                        if (hasCustomWallpaper) Color.White else colors.textPrimary
                    }

                    Box(
                        modifier = Modifier
                            .widthIn(max = 300.dp)
                            .clip(bubbleShape)
                            .then(bubbleBackground)
                            .combinedClickable(
                                onClick = {},
                                onLongClick = openMenu,
                                onLongClickLabel = "Tuỳ chọn tin nhắn"
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = message.body,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 17.sp,
                                lineHeight = 24.sp
                            ),
                            color = textColor
                        )
                    }
                }
                }
            }

            DropdownMenu(
                expanded = isMenuOpen,
                onDismissRequest = { isMenuOpen = false },
                containerColor = colors.surface,
                shape = RoundedCornerShape(14.dp)
            ) {
                if (canCopy) {
                    MessageMenuItem(
                        label = "Sao chép",
                        icon = Icons.Rounded.ContentCopy,
                        tint = colors.textPrimary,
                        onClick = {
                            isMenuOpen = false
                            scope.launch {
                                clipboard.setClipEntry(
                                    ClipData.newPlainText("Tin nhắn", message.body).toClipEntry()
                                )
                            }
                        }
                    )
                }
                if (canEdit) {
                    MessageMenuItem(
                        label = "Sửa",
                        icon = Icons.Rounded.Edit,
                        tint = colors.textPrimary,
                        onClick = {
                            isMenuOpen = false
                            onEdit()
                        }
                    )
                }
                if (canDelete) {
                    MessageMenuItem(
                        label = "Xoá",
                        icon = Icons.Rounded.Delete,
                        tint = colors.error,
                        onClick = {
                            isMenuOpen = false
                            onDelete()
                        }
                    )
                }
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
private fun MessageMenuItem(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                ),
                color = tint
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        },
        onClick = onClick,
        modifier = Modifier.heightIn(min = 48.dp)
    )
}

/**
 * Ảnh được trả lời trong Chat (giống Locket): ảnh vuông bo góc, chip tác giả + thời gian ở góc trên,
 * nội dung tin nhắn căn giữa phía dưới trên nền tối mờ. Bấm mở trang ảnh, nhấn giữ mở menu tin nhắn.
 */
@Composable
private fun PhotoReplyCard(
    post: Post,
    body: String?,
    now: java.time.Instant,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    val colors = LokapTheme.colors
    val shape = RoundedCornerShape(28.dp)
    val timeLabel = remember(post.takenAt, now) { formatTimeAgo(post.takenAt, now) }

    Box(
        modifier = Modifier
            .fillMaxWidth(0.86f)
            .widthIn(max = 360.dp)
            .aspectRatio(1f)
            .clip(shape)
            .background(colors.surfaceHigh)
            .combinedClickable(
                onClick = onClick,
                onClickLabel = "Mở ảnh",
                onLongClick = onLongClick,
                onLongClickLabel = if (onLongClick != null) "Tuỳ chọn tin nhắn" else null
            )
    ) {
        AsyncImage(
            model = post.imageUrl,
            contentDescription = post.caption ?: "Ảnh của ${post.author.displayName}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(LokapTheme.shapes.pill)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AvatarRing(
                avatarUrl = post.author.avatarUrl,
                displayName = post.author.displayName,
                size = 30.dp
            )
            Text(
                text = post.author.displayName,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 150.dp)
            )
            Text(
                text = timeLabel,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1
            )
        }

        if (!body.isNullOrBlank()) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    lineHeight = 24.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 18.dp)
                    .clip(LokapTheme.shapes.pill)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun AttachedLocketPostCard(
    post: Post,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val cardShape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .size(220.dp)
            .clip(cardShape)
            .background(colors.surfaceHigh)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = if (onLongClick != null) "Tuỳ chọn tin nhắn" else null
            )
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
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.5.sp
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
            // Cả tin đang gửi đã bị làm mờ 58% (alpha của Column cha), nên chữ phải đậm:
            // textTertiary chỉ còn 1,6:1 ở giao diện sáng, textPrimary còn khoảng 4,3:1
            Text(
                text = "Đang gửi…",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                color = if (hasCustomWallpaper) Color.White else colors.textPrimary,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .wallpaperPill(hasCustomWallpaper)
            )
        }

        isMine && isFailed -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = "Gửi thất bại",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
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
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Thử lại",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
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
                modifier = Modifier
                    .padding(top = 2.dp)
                    .wallpaperPill(hasCustomWallpaper)
            ) {
                // Màu chủ đề chat có thể gần như vô hình trên nền (chủ đề tối ở giao diện tối, chủ đề sáng ở giao diện sáng).
                // Icon "đã xem" giữ màu chủ đề khi đạt 3:1, chữ cần 4,5:1; ở chế độ hình nền dùng màu trắng trên viên nền tối.
                val iconColor = when {
                    hasCustomWallpaper -> Color.White
                    message.seen -> activePreset.primaryColor.readableOn(colors.background, colors.textSecondary)
                    else -> colors.textSecondary
                }
                val textColor = when {
                    hasCustomWallpaper -> Color.White
                    message.seen -> activePreset.primaryColor.readableOn(colors.background, colors.textSecondary, minRatio = 4.5f)
                    else -> colors.textSecondary
                }
                Icon(
                    imageVector = if (message.seen) Icons.Rounded.DoneAll else Icons.Rounded.Done,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
                val statusLabel = if (message.seen) "Đã xem" else "Đã gửi"
                Text(
                    text = "$statusLabel • ${formatTimeAgo(message.createdAt, now)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                    color = textColor
                )
            }
        }

        else -> Unit
    }
}

/**
 * Ở chế độ hình nền, đặt dòng trạng thái lên viên nền tối (như nhãn ngày), để chữ trắng đọc được trên mọi ảnh.
 * Đậm 60% để cả dòng "Đang gửi…" (bị làm mờ 58% cùng tin nhắn) vẫn đạt 3:1 trên ảnh trắng.
 */
private fun Modifier.wallpaperPill(enabled: Boolean): Modifier =
    if (enabled) {
        clip(RoundedCornerShape(percent = 50))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    } else {
        this
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
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.width(spacing.sm))
                // Chốt 06/11: icon khoá + chữ ngắn nói rõ lý do bị khoá
                Text(
                    text = "Chưa là bạn",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
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
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dấu + nằm trên vòng tròn tô 12% màu chủ đề, nên đo tương phản với đúng màu vòng tròn đó
            val plusCircle = activePreset.primaryColor.copy(alpha = 0.12f).compositeOver(colors.surface)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (hasCustomWallpaper) Color.White.copy(alpha = 0.16f) else activePreset.primaryColor.copy(alpha = 0.12f))
                    .clickable(onClick = onCameraClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Chụp ảnh gửi riêng",
                    tint = if (hasCustomWallpaper) Color.White else activePreset.primaryColor.readableOn(plusCircle, colors.textPrimary),
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (hasCustomWallpaper) Color.White.copy(alpha = 0.14f) else colors.surfaceHigh)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (inputText.isEmpty()) {
                    Text(
                        text = "Nhắn tin…",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
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
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        lineHeight = 23.sp,
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
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(activePreset.primaryColor)
                        .clickable(onClick = onSendClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Gửi",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier.size(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Gửi",
                        tint = if (hasCustomWallpaper) Color.White.copy(alpha = 0.35f) else colors.textTertiary.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
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
                        fontSize = 17.5.sp
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
                        modifier = Modifier.size(22.dp)
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
                                        modifier = Modifier.size(25.dp)
                                    )
                                }
                            }
                        }

                        Column {
                            Text(
                                text = if (!themeConfig.customWallpaperUri.isNullOrBlank()) "Đang dùng ảnh thiết bị" else "Hình nền từ thư viện",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.5.sp
                                ),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Chọn ảnh và cắt chỉnh vừa màn hình",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
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
                                    modifier = Modifier.size(21.dp)
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
                                        fontSize = 13.5.sp
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
                        fontSize = 14.sp
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
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Text(
                                text = preset.nameVi,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 12.sp,
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
                        fontSize = 14.sp
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
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Text(
                                text = preset.nameVi,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 12.sp,
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
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Căn vị trí ảnh nền",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
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
                            modifier = Modifier.size(20.dp)
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
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp),
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
                                        fontSize = 13.sp,
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
                                    fontSize = 14.sp
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
