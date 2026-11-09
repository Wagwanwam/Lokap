package vn.nhom2.lokap.feature.postdetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import coil3.compose.AsyncImage
import java.io.File
import kotlinx.coroutines.launch
import vn.nhom2.lokap.core.common.formatTimeAgo
import vn.nhom2.lokap.core.designsystem.component.*
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.designsystem.theme.regionGradient
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.core.navigation.navigateSingleTop
import vn.nhom2.lokap.core.navigation.popBackStackSafely
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.data.fake.SampleData2
import vn.nhom2.lokap.domain.model.*
fun NavGraphBuilder.postDetailScreen(navController: NavController) {
    composable<Route.PostDetail> { backStackEntry ->
        val route = backStackEntry.toRoute<Route.PostDetail>()
        PostDetailRoute(
            postId = route.id,
            onNavigateBack = { navController.popBackStackSafely() },
            onNavigateToChat = { convId -> navController.navigateSingleTop(Route.Chat(convId)) },
            onNavigateToProfile = { userId -> navController.navigateSingleTop(Route.Profile(userId)) },
            onNavigateToRegionAlbum = { level, code ->
                navController.navigateSingleTop(Route.RegionAlbum(level, code))
            }
        )
    }
}

@Composable
fun PostDetailRoute(
    postId: String,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToRegionAlbum: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PostDetailViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(postId) {
        viewModel.loadPost(postId)
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PostDetailEffect.NavigateBack -> onNavigateBack()
                is PostDetailEffect.NavigateToChat -> onNavigateToChat(effect.conversationId)
                is PostDetailEffect.NavigateToProfile -> onNavigateToProfile(effect.userId)
                is PostDetailEffect.NavigateToRegionAlbum -> {
                    onNavigateToRegionAlbum(effect.level, effect.code)
                }

                is PostDetailEffect.ShowSnackbar -> {
                    scope.launch { snackbarHostState.showSnackbar(effect.message) }
                }
            }
        }
    }

    PostDetailScreen(
        state = state,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    state: PostDetailUiState,
    onAction: (PostDetailAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes
    val isPreview = LocalInspectionMode.current
    var menuExpanded by remember { mutableStateOf(false) }

    val post = state.post

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding(),
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.md, vertical = spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { onAction(PostDetailAction.OnBackClick) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = colors.textPrimary
                    )
                }

                Text(
                    text = "Chi tiết khoảnh khắc",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    ),
                    color = colors.textPrimary
                )

                if (post != null) {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Tùy chọn",
                                tint = colors.textPrimary
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(colors.surfaceHigh)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Báo cáo bài viết",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.textPrimary
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onAction(PostDetailAction.OnOpenReportSheet)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Chặn ${post.author.displayName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.error
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onAction(PostDetailAction.OnOpenBlockDialog)
                                }
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.padding(spacing.lg))
                }
            }
        },
        bottomBar = {
            if (post != null && !state.isNotFound) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface)
                        .border(width = 0.5.dp, color = colors.outline)
                        .navigationBarsPadding()
                        .padding(horizontal = spacing.md, vertical = spacing.xs)
                ) {
                    if (state.replyingToComment != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Đang trả lời ${state.replyingToComment.authorName}…",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.primary
                            )
                            IconButton(
                                onClick = { onAction(PostDetailAction.OnReplyToComment(null)) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Huỷ",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        OutlinedTextField(
                            value = state.commentInput,
                            onValueChange = { onAction(PostDetailAction.OnChangeCommentInput(it)) },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    text = if (state.replyingToComment != null) "Viết câu trả lời…" else "Thêm bình luận…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textTertiary
                                )
                            },
                            singleLine = true,
                            shape = shapes.button,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.outline,
                                focusedContainerColor = colors.surfaceHigh,
                                unfocusedContainerColor = colors.surfaceHigh,
                                cursorColor = colors.primary,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            )
                        )

                        IconButton(
                            onClick = { onAction(PostDetailAction.OnSendComment) },
                            enabled = state.commentInput.isNotBlank(),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (state.commentInput.isNotBlank()) colors.primary else colors.surfaceHigh)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Gửi",
                                tint = if (state.commentInput.isNotBlank()) colors.buttonText else colors.textTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (state.isNotFound || post == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(spacing.lg),
                contentAlignment = Alignment.Center
            ) {
                ErrorState(
                    message = "Bài đã bị xoá hoặc ẩn",
                    retryLabel = "Quay lại",
                    onRetry = { onAction(PostDetailAction.OnBackClick) }
                )
            }
            return@Scaffold
        }

        val isDelayedShot = post.audience == Audience.PUBLIC &&
            post.publicVisibleAt != null &&
            post.publicVisibleAt.isAfter(state.now)

        val timeLabel = remember(post.takenAt, state.now, isDelayedShot) {
            formatTimeAgo(
                instant = post.takenAt,
                now = state.now,
                isDelayedShot = isDelayedShot
            )
        }

        val locationLabel = remember(post.location) {
            formatDetailLocation(post.location)
        }

        val imageModel: Any = remember(post.imageUrl) {
            if (post.imageUrl.startsWith("/")) File(post.imageUrl) else post.imageUrl
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    modifier = Modifier.weight(1f)
                ) {
                    AvatarRing(
                        avatarUrl = post.author.avatarUrl,
                        displayName = post.author.displayName,
                        size = 44.dp,
                        onClick = { onAction(PostDetailAction.OnAuthorClick) }
                    )
                    Column {
                        Text(
                            text = post.author.displayName,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = colors.textPrimary
                        )
                        Text(
                            text = timeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                            color = colors.textSecondary
                        )
                    }
                }

                GlassChip(
                    label = locationLabel,
                    leadingIcon = Icons.Rounded.LocationOn,
                    onClick = { onAction(PostDetailAction.OnLocationClick) }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(shapes.card)
                    .background(colors.surfaceHigh)
                    .border(1.5.dp, colors.outline, shapes.card)
            ) {
                if (!isPreview) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = post.caption ?: post.author.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (!post.caption.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.card)
                        .background(colors.surface)
                        .border(1.dp, colors.outline, shapes.card)
                        .padding(spacing.md)
                ) {
                    Text(
                        text = post.caption,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        ),
                        color = colors.textPrimary
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {

                val isLiked = post.myReaction != null
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(shapes.pill)
                        .clickable { onAction(PostDetailAction.OnReact("❤️")) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Thích",
                        tint = if (isLiked) colors.primary else colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = post.reactions.sumOf { it.count }.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isLiked) colors.primary else colors.textSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(shapes.pill)
                        .clickable { onAction(PostDetailAction.OnReplyClick) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Bình luận",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "${countTotalComments(state.comments)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(shapes.pill)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Đăng lại",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary
                    )
                }

                IconButton(
                    onClick = {  },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Chia sẻ liên kết",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            EmojiBar(
                reactions = post.reactions,
                selectedEmoji = post.myReaction,
                onSelectEmoji = { emoji -> onAction(PostDetailAction.OnReact(emoji)) }
            )

            if (post.author.id != state.currentUserId) {
                PinkButton(
                    text = "Nhắn tin cho ${post.author.displayName}",
                    leadingIcon = Icons.AutoMirrored.Rounded.Send,
                    onClick = { onAction(PostDetailAction.OnReplyClick) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                )
            }

            RedditThreadedCommentsSection(
                comments = state.comments,
                onVote = { id, isUp -> onAction(PostDetailAction.OnVoteComment(id, isUp)) },
                onReply = { comment -> onAction(PostDetailAction.OnReplyToComment(comment)) }
            )
        }

        if (state.isReportSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { onAction(PostDetailAction.OnDismissReportSheet) },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                shape = shapes.sheet,
                containerColor = colors.surface,
                contentColor = colors.textPrimary
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.lg, vertical = spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    Text(
                        text = "Báo cáo bài viết",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Hãy chọn lý do bạn muốn báo cáo khoảnh khắc này:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )

                    SampleData2.reportReasons.forEach { reason ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shapes.card)
                                .background(colors.surfaceHigh)
                                .border(1.dp, colors.outline, shapes.card)
                                .clickable { onAction(PostDetailAction.OnSubmitReport(reason)) }
                                .padding(horizontal = spacing.lg, vertical = spacing.md)
                        ) {
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }
        }

        if (state.showBlockConfirmDialog) {
            BlockUserDialog(
                displayName = post.author.displayName,
                onConfirm = { onAction(PostDetailAction.OnConfirmBlock) },
                onDismiss = { onAction(PostDetailAction.OnDismissBlockDialog) }
            )
        }
    }
}

@Composable
private fun RedditThreadedCommentsSection(
    comments: List<RedditCommentUi>,
    onVote: (String, Boolean) -> Unit,
    onReply: (RedditCommentUi) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(LokapTheme.shapes.card)
            .background(colors.surface)
            .border(1.dp, colors.outline, LokapTheme.shapes.card)
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Bình luận · ${countTotalComments(comments)}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                ),
                color = colors.textPrimary
            )
        }

        if (comments.isEmpty()) {
            Text(
                text = "Chưa có bình luận nào. Hãy là người đầu tiên chia sẻ suy nghĩ!",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(vertical = spacing.md)
            )
        } else {
            comments.forEach { comment ->
                RedditCommentTreeItem(
                    comment = comment,
                    depth = 0,
                    onVote = onVote,
                    onReply = onReply
                )
            }
        }
    }
}

private fun countTotalComments(list: List<RedditCommentUi>): Int {
    return list.sumOf { 1 + countTotalComments(it.replies) }
}

@Composable
private fun RedditCommentTreeItem(
    comment: RedditCommentUi,
    depth: Int,
    onVote: (String, Boolean) -> Unit,
    onReply: (RedditCommentUi) -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val isReply = depth > 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isReply) 16.dp else 0.dp)
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {

            if (isReply) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .width(2.dp)
                        .height(24.dp)
                        .background(colors.outline.copy(alpha = 0.35f), RoundedCornerShape(1.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            AvatarRing(
                avatarUrl = comment.authorAvatarUrl,
                displayName = comment.authorName,
                size = if (isReply) 26.dp else 30.dp
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = comment.authorName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                fontSize = 12.5.sp
                            ),
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (comment.replyToAuthorName != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = colors.primary.copy(alpha = 0.12f),
                                border = BorderStroke(0.5.dp, colors.primary.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Reply,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = comment.replyToAuthorName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        color = colors.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = comment.timeAgo,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = colors.textTertiary,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Text(
                    text = comment.content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        lineHeight = 18.5.sp
                    ),
                    color = colors.textPrimary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    modifier = Modifier.padding(top = 2.dp)
                ) {

                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.surfaceHigh)
                            .border(0.5.dp, colors.outline.copy(alpha = 0.3f), CircleShape)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = { onVote(comment.id, true) },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Thích",
                                tint = if (comment.userVote == 1) colors.primary else colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = comment.score.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = when (comment.userVote) {
                                1 -> colors.primary
                                -1 -> Color(0xFF6366F1)
                                else -> colors.textSecondary
                            }
                        )

                        IconButton(
                            onClick = { onVote(comment.id, false) },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Không thích",
                                tint = if (comment.userVote == -1) Color(0xFF6366F1) else colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onReply(comment) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Reply,
                                contentDescription = "Trả lời",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Trả lời",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                    fontSize = 11.5.sp
                                ),
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }
        }

        if (comment.replies.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                comment.replies.forEach { reply ->
                    RedditCommentTreeItem(
                        comment = reply,
                        depth = 1,
                        onVote = onVote,
                        onReply = onReply
                    )
                }
            }
        }
    }
}

private fun formatDetailLocation(location: PostLocation): String {
    return when (location.level) {
        LocationLevel.PLACE -> listOfNotNull(location.placeName, location.provinceName).joinToString(", ")
        LocationLevel.WARD -> listOfNotNull(location.wardName, location.provinceName).joinToString(", ")
        LocationLevel.PROVINCE -> location.provinceName ?: "Việt Nam"
        LocationLevel.COUNTRY -> "Việt Nam"
    }
}

@Preview(name = "1. PostDetail - Bình thường", showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun PostDetailNormalPreview() {
    LokapTheme {
        PostDetailScreen(
            state = PostDetailUiState(
                post = SampleData.posts.first()
            ),
            onAction = {}
        )
    }
}

@Preview(name = "2. PostDetail - Không tồn tại", showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun PostDetailNotFoundPreview() {
    LokapTheme {
        PostDetailScreen(
            state = PostDetailUiState(
                post = null,
                isNotFound = true
            ),
            onAction = {}
        )
    }
}

@Preview(name = "3. PostDetail - Xác nhận chặn", showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun PostDetailBlockDialogPreview() {
    LokapTheme {
        PostDetailScreen(
            state = PostDetailUiState(
                post = SampleData.posts.first(),
                showBlockConfirmDialog = true
            ),
            onAction = {}
        )
    }
}
