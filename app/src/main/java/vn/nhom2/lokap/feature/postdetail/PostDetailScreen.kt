package vn.nhom2.lokap.feature.postdetail

import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import vn.nhom2.lokap.core.navigation.navigateSingleTop
import vn.nhom2.lokap.core.navigation.popBackStackSafely
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import coil3.compose.AsyncImage
import vn.nhom2.lokap.core.common.formatTimeAgo
import vn.nhom2.lokap.core.designsystem.component.AvatarRing
import vn.nhom2.lokap.core.designsystem.component.EmojiBar
import vn.nhom2.lokap.core.designsystem.component.ErrorState
import vn.nhom2.lokap.core.designsystem.component.GlassChip
import vn.nhom2.lokap.core.designsystem.component.PinkButton
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.designsystem.theme.regionGradient
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.data.fake.SampleData2
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.PostLocation
import java.io.File

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
                    style = MaterialTheme.typography.titleLarge,
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
            // Thông tin tác giả + vị trí
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
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "@${post.author.username} · $timeLabel",
                            style = MaterialTheme.typography.labelSmall,
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

            // Ảnh lớn vuông bo 28dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(shapes.card)
                    .background(regionGradient(post.location.provinceCode ?: post.id))
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

            // Caption
            if (!post.caption.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.card)
                        .background(colors.surface)
                        .border(1.dp, colors.outline, shapes.card)
                        .padding(spacing.lg)
                ) {
                    Text(
                        text = post.caption,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textPrimary
                    )
                }
            }

            // EmojiBar với số đếm từng emoji
            EmojiBar(
                reactions = post.reactions,
                selectedEmoji = post.myReaction,
                onSelectEmoji = { emoji -> onAction(PostDetailAction.OnReact(emoji)) }
            )

            // Nút "Trả lời" (-> Chat)
            PinkButton(
                text = "Trả lời ${post.author.displayName}",
                leadingIcon = Icons.AutoMirrored.Rounded.Send,
                onClick = { onAction(PostDetailAction.OnReplyClick) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ModalBottomSheet Báo cáo bài viết
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

        // AlertDialog xác nhận Chặn người dùng
        if (state.showBlockConfirmDialog) {
            AlertDialog(
                onDismissRequest = { onAction(PostDetailAction.OnDismissBlockDialog) },
                containerColor = colors.surfaceHigh,
                titleContentColor = colors.textPrimary,
                textContentColor = colors.textSecondary,
                title = {
                    Text(
                        text = "Chặn ${post.author.displayName}?",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Text(
                        text = "Họ sẽ không thể gửi ảnh, nhắn tin hoặc xem các khoảnh khắc của bạn trên Lokap nữa.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = { onAction(PostDetailAction.OnConfirmBlock) }) {
                        Text(
                            text = "Chặn",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onAction(PostDetailAction.OnDismissBlockDialog) }) {
                        Text(
                            text = "Huỷ",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textPrimary
                        )
                    }
                }
            )
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
