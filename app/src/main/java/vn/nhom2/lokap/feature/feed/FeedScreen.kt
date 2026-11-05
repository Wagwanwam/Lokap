package vn.nhom2.lokap.feature.feed

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import vn.nhom2.lokap.core.designsystem.component.BlockUserDialog
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import vn.nhom2.lokap.core.navigation.navigateSingleTop
import vn.nhom2.lokap.core.navigation.popBackStackSafely
import androidx.navigation.compose.composable
import coil3.compose.AsyncImage
import vn.nhom2.lokap.core.common.formatTimeAgo
import vn.nhom2.lokap.core.designsystem.component.AvatarRing
import vn.nhom2.lokap.core.designsystem.component.BadgeDot
import vn.nhom2.lokap.core.designsystem.component.EmojiBar
import vn.nhom2.lokap.core.designsystem.component.EmptyState
import vn.nhom2.lokap.core.designsystem.component.ErrorState
import vn.nhom2.lokap.core.designsystem.component.GlassChip
import vn.nhom2.lokap.core.designsystem.component.OfflineBanner
import vn.nhom2.lokap.core.designsystem.component.PhotoCard
import vn.nhom2.lokap.core.designsystem.component.PinkButton
import vn.nhom2.lokap.core.designsystem.component.ShimmerBox
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.designsystem.theme.regionGradient
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.Post
import vn.nhom2.lokap.domain.model.PostLocation
import vn.nhom2.lokap.domain.model.PostSendState
import java.io.File

private const val FEED_TRANSITION_MS = 320

fun NavGraphBuilder.feedScreen(navController: NavController) {
    composable<Route.Feed>(
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Up,
                animationSpec = tween(FEED_TRANSITION_MS)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Down,
                animationSpec = tween(FEED_TRANSITION_MS)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Up,
                animationSpec = tween(FEED_TRANSITION_MS)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Down,
                animationSpec = tween(FEED_TRANSITION_MS)
            )
        }
    ) {
        FeedRoute(
            onNavigateBackToCamera = { navController.popBackStackSafely() },
            onNavigateToNotifications = { navController.navigateSingleTop(Route.Notifications) },
            onNavigateToPostDetail = { id -> navController.navigateSingleTop(Route.PostDetail(id)) },
            onNavigateToRegionAlbum = { level, code ->
                navController.navigateSingleTop(Route.RegionAlbum(level, code))
            },
            onNavigateToProfile = { userId -> navController.navigateSingleTop(Route.Profile(userId)) },
            onNavigateToChat = { convId -> navController.navigateSingleTop(Route.Chat(convId)) }
        )
    }
}

@Composable
fun FeedRoute(
    onNavigateBackToCamera: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToRegionAlbum: (String, String) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                FeedEffect.NavigateBackToCamera -> onNavigateBackToCamera()
                FeedEffect.NavigateToNotifications -> onNavigateToNotifications()
                is FeedEffect.NavigateToPostDetail -> onNavigateToPostDetail(effect.postId)
                is FeedEffect.NavigateToRegionAlbum -> onNavigateToRegionAlbum(effect.level, effect.code)
                is FeedEffect.NavigateToProfile -> onNavigateToProfile(effect.userId)
                is FeedEffect.NavigateToChat -> onNavigateToChat(effect.conversationId)
                // launch riêng: showSnackbar treo ~4 giây, nếu chờ ở đây thì mọi điều hướng sau đó bị kẹt
                is FeedEffect.ShowMessage -> scope.launch { snackbarHostState.showSnackbar(effect.message) }
            }
        }
    }

    FeedScreen(
        state = state,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    state: FeedUiState,
    onAction: (FeedAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    state.pendingBlock?.let { target ->
        BlockUserDialog(
            displayName = target.displayName,
            onConfirm = { onAction(FeedAction.OnConfirmBlock) },
            onDismiss = { onAction(FeedAction.OnDismissBlockDialog) }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding(),
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background)
                    .padding(horizontal = spacing.lg, vertical = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                FeedTopBar(
                    selectedTab = state.selectedTab,
                    unreadNotificationsCount = state.unreadNotificationsCount,
                    onSelectTab = { onAction(FeedAction.OnSelectTab(it)) },
                    onBackToCamera = { onAction(FeedAction.OnSwipeDownToCamera) },
                    onNotificationsClick = { onAction(FeedAction.OnNotificationsClick) }
                )

                if (state.isOffline) {
                    OfflineBanner()
                }
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onAction(FeedAction.OnRefresh) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.isLoading -> {
                    FeedLoadingContent()
                }

                state.errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.lg),
                        contentAlignment = Alignment.Center
                    ) {
                        ErrorState(
                            message = state.errorMessage,
                            onRetry = { onAction(FeedAction.OnRetry) }
                        )
                    }
                }

                state.selectedTab == FeedTab.FRIENDS -> {
                    if (state.friendPosts.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyState(
                                title = "Chưa có bài nào, rủ bạn bè vào Lokap nhé",
                                subtitle = "Chụp khoảnh khắc đầu tiên hoặc kết bạn để xem ảnh mới mỗi ngày",
                                actionLabel = "Mở máy ảnh",
                                onActionClick = { onAction(FeedAction.OnSwipeDownToCamera) }
                            )
                        }
                    } else {
                        FriendsVerticalPager(
                            posts = state.friendPosts,
                            now = state.now,
                            currentUserId = state.currentUserId,
                            onAction = onAction
                        )
                    }
                }

                state.selectedTab == FeedTab.EXPLORE -> {
                    if (state.nearbyPosts.isEmpty() && state.landmarkPosts.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyState(
                                title = "Chưa có bài nào, rủ bạn bè vào Lokap nhé",
                                subtitle = "Khám phá các khoảnh khắc công khai quanh ${state.currentProvinceName}"
                            )
                        }
                    } else {
                        ExploreLazyColumn(
                            nearbyPosts = state.nearbyPosts,
                            landmarkPosts = state.landmarkPosts,
                            currentProvinceName = state.currentProvinceName,
                            now = state.now,
                            onAction = onAction
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedTopBar(
    selectedTab: FeedTab,
    unreadNotificationsCount: Int,
    onSelectTab: (FeedTab) -> Unit,
    onBackToCamera: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val pill = LokapTheme.shapes.pill

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Nút quay nhanh về Camera
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(pill)
                .background(colors.surfaceHigh)
                .border(1.dp, colors.outline, pill)
                .clickable(onClick = onBackToCamera),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Về máy ảnh",
                tint = colors.textPrimary
            )
        }

        // Hai chip chọn Tab: Bạn bè / Khám phá
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FeedTab.entries.forEach { tab ->
                GlassChip(
                    label = tab.labelVi,
                    selected = selectedTab == tab,
                    onClick = { onSelectTab(tab) }
                )
            }
        }

        // Chuông thông báo kèm BadgeDot
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(pill)
                    .background(colors.surfaceHigh)
                    .border(1.dp, colors.outline, pill)
                    .clickable(onClick = onNotificationsClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Notifications,
                    contentDescription = "Thông báo",
                    tint = colors.textPrimary
                )
            }
            BadgeDot(count = unreadNotificationsCount)
        }
    }
}

@Composable
private fun FriendsVerticalPager(
    posts: List<Post>,
    now: java.time.Instant,
    currentUserId: String,
    onAction: (FeedAction) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { posts.size })
    var overscrollPullDown by remember { mutableFloatStateOf(0f) }

    VerticalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(pagerState.currentPage) {
                if (pagerState.currentPage == 0) {
                    detectVerticalDragGestures(
                        onDragStart = { overscrollPullDown = 0f },
                        onVerticalDrag = { _, dragAmount ->
                            if (dragAmount > 0f) {
                                overscrollPullDown += dragAmount
                            }
                        },
                        onDragEnd = {
                            if (overscrollPullDown > 130f) {
                                onAction(FeedAction.OnSwipeDownToCamera)
                            }
                            overscrollPullDown = 0f
                        }
                    )
                }
            }
    ) { page ->
        val post = posts[page]
        LocketFriendPageItem(
            post = post,
            now = now,
            onPostClick = { onAction(FeedAction.OnPostClick(post.id)) },
            onAuthorClick = { onAction(FeedAction.OnAuthorClick(post.author.id)) },
            onLocationClick = { onAction(FeedAction.OnLocationChipClick(post)) },
            onReact = { emoji -> onAction(FeedAction.OnReact(post.id, emoji)) },
            onRetrySend = { onAction(FeedAction.OnRetrySendPost(post.id)) },
            onReplyClick = { onAction(FeedAction.OnReplyClick(post)) },
            showReply = post.author.id != currentUserId
        )
    }
}

@Composable
private fun LocketFriendPageItem(
    post: Post,
    now: java.time.Instant,
    onPostClick: () -> Unit,
    onAuthorClick: () -> Unit,
    onLocationClick: () -> Unit,
    onReact: (String) -> Unit,
    onRetrySend: () -> Unit,
    onReplyClick: () -> Unit,
    showReply: Boolean = true
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes
    val isPreview = LocalInspectionMode.current

    val isDelayedShot = post.audience == Audience.PUBLIC &&
        post.publicVisibleAt != null &&
        post.publicVisibleAt.isAfter(now)

    val timeLabel = remember(post.takenAt, now, isDelayedShot) {
        formatTimeAgo(
            instant = post.takenAt,
            now = now,
            isDelayedShot = isDelayedShot
        )
    }

    val locationLabel = remember(post.location) {
        formatFeedLocation(post.location)
    }

    val imageModel: Any = remember(post.imageUrl) {
        if (post.imageUrl.startsWith("/")) File(post.imageUrl) else post.imageUrl
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(spacing.xs))

        // Ảnh vuông lớn bo 28dp ở giữa
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shapes.card)
                .background(regionGradient(post.location.provinceCode ?: post.id))
                .border(1.5.dp, colors.outline, shapes.card)
                .clickable(onClick = onPostClick)
        ) {
            if (!isPreview) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = post.caption ?: post.author.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (post.audience == Audience.DIRECT) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(spacing.md)
                        .clip(shapes.pill)
                        .background(colors.primary)
                        .padding(horizontal = spacing.md, vertical = spacing.xs)
                ) {
                    Text(
                        text = "Gửi riêng cho bạn",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onPrimary
                    )
                }
            }

            if (!post.caption.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(spacing.md)
                        .clip(shapes.pill)
                        .background(colors.background.copy(alpha = 0.78f))
                        .border(1.dp, colors.outline, shapes.pill)
                        .padding(horizontal = spacing.lg, vertical = spacing.sm)
                ) {
                    Text(
                        text = post.caption,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Trạng thái PENDING / FAILED
            when (post.sendState) {
                PostSendState.PENDING -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.background.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(shapes.pill)
                                .background(colors.surfaceHigh)
                                .border(1.dp, colors.outline, shapes.pill)
                                .padding(horizontal = spacing.lg, vertical = spacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = colors.primary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(spacing.sm))
                            Text(
                                text = "Đang gửi…",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.textPrimary
                            )
                        }
                    }
                }

                PostSendState.FAILED -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.background.copy(alpha = 0.78f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(spacing.md)
                        ) {
                            Text(
                                text = "Gửi ảnh không thành công",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.error
                            )
                            PinkButton(
                                text = "Thử lại",
                                leadingIcon = Icons.Rounded.Refresh,
                                onClick = onRetrySend
                            )
                        }
                    }
                }

                PostSendState.SENT -> Unit
            }
        }

        // Thông tin tác giả + thời gian + chip vị trí bên dưới ảnh
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                AvatarRing(
                    avatarUrl = post.author.avatarUrl,
                    displayName = post.author.displayName,
                    size = 36.dp,
                    onClick = onAuthorClick
                )
                Text(
                    text = post.author.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary
                )
                Text(
                    text = "· $timeLabel",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary
                )
            }

            GlassChip(
                label = locationLabel,
                leadingIcon = Icons.Rounded.LocationOn,
                onClick = onLocationClick
            )
        }

        // Tương tác: EmojiBar + Ô "Trả lời…"
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            EmojiBar(
                reactions = post.reactions,
                selectedEmoji = post.myReaction,
                onSelectEmoji = onReact
            )

            // Không hiện ô Trả lời với bài của chính mình
            if (showReply) Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.pill)
                    .background(colors.surfaceHigh)
                    .border(1.dp, colors.outline, shapes.pill)
                    .clickable(onClick = onReplyClick)
                    .padding(horizontal = spacing.lg, vertical = spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Trả lời ${post.author.displayName}…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "Trả lời",
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ExploreLazyColumn(
    nearbyPosts: List<Post>,
    landmarkPosts: List<Post>,
    currentProvinceName: String,
    now: java.time.Instant,
    onAction: (FeedAction) -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = spacing.lg,
            end = spacing.lg,
            top = spacing.sm,
            bottom = spacing.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.xl)
    ) {
        if (nearbyPosts.isNotEmpty()) {
            item(key = "header_nearby") {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Text(
                        text = "Gần bạn · $currentProvinceName",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Khoảnh khắc công khai mới nhất quanh khu vực của bạn",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textSecondary
                    )
                }
            }

            items(
                items = nearbyPosts,
                key = { "nearby_${it.id}" }
            ) { post ->
                Box(modifier = Modifier.clickable { onAction(FeedAction.OnPostClick(post.id)) }) {
                    PhotoCard(
                        post = post,
                        now = now,
                        onReact = { emoji -> onAction(FeedAction.OnReact(post.id, emoji)) },
                        onRetrySend = { onAction(FeedAction.OnRetrySendPost(post.id)) },
                        onReport = { onAction(FeedAction.OnReportPost(post.id)) },
                        onBlockUser = {
                            onAction(FeedAction.OnBlockUser(post.author.id, post.author.displayName))
                        },
                        onAuthorClick = { onAction(FeedAction.OnAuthorClick(post.author.id)) },
                        onLocationClick = { onAction(FeedAction.OnLocationChipClick(post)) }
                    )
                }
            }
        }

        if (landmarkPosts.isNotEmpty()) {
            item(key = "header_landmark") {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Text(
                        text = "Địa danh nổi bật",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.primary
                    )
                    Text(
                        text = "Các điểm check-in được gắn thẻ nhiều nhất trên bản đồ",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textSecondary
                    )
                }
            }

            items(
                items = landmarkPosts,
                key = { "landmark_${it.id}" }
            ) { post ->
                Box(modifier = Modifier.clickable { onAction(FeedAction.OnPostClick(post.id)) }) {
                    PhotoCard(
                        post = post,
                        now = now,
                        onReact = { emoji -> onAction(FeedAction.OnReact(post.id, emoji)) },
                        onRetrySend = { onAction(FeedAction.OnRetrySendPost(post.id)) },
                        onReport = { onAction(FeedAction.OnReportPost(post.id)) },
                        onBlockUser = {
                            onAction(FeedAction.OnBlockUser(post.author.id, post.author.displayName))
                        },
                        onAuthorClick = { onAction(FeedAction.OnAuthorClick(post.author.id)) },
                        onLocationClick = { onAction(FeedAction.OnLocationChipClick(post)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedLoadingContent() {
    val spacing = LokapTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        )
        ShimmerBox(
            modifier = Modifier
                .width(180.dp)
                .height(32.dp),
            shape = LokapTheme.shapes.pill
        )
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = LokapTheme.shapes.pill
        )
    }
}

private fun formatFeedLocation(location: PostLocation): String {
    return when (location.level) {
        LocationLevel.PLACE -> location.placeName ?: location.wardName ?: location.provinceName ?: "Việt Nam"
        LocationLevel.WARD -> location.wardName ?: location.provinceName ?: "Việt Nam"
        LocationLevel.PROVINCE -> location.provinceName ?: "Việt Nam"
        LocationLevel.COUNTRY -> "Việt Nam"
    }
}

@Preview(name = "1. Feed - Tab Bạn bè", showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun FeedFriendsPreview() {
    LokapTheme {
        FeedScreen(
            state = FeedUiState(
                selectedTab = FeedTab.FRIENDS,
                friendPosts = SampleData.posts.take(5)
            ),
            onAction = {}
        )
    }
}

@Preview(name = "2. Feed - Tab Khám phá", showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun FeedExplorePreview() {
    LokapTheme {
        FeedScreen(
            state = FeedUiState(
                selectedTab = FeedTab.EXPLORE,
                nearbyPosts = SampleData.posts.filter { it.location.provinceCode == "da-nang" }.take(2),
                landmarkPosts = SampleData.posts.filter { it.location.landmarkId != null }.take(2),
                isOffline = true
            ),
            onAction = {}
        )
    }
}

@Preview(name = "3. Feed - Đang tải (Shimmer)", showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun FeedLoadingPreview() {
    LokapTheme {
        FeedScreen(
            state = FeedUiState(isLoading = true),
            onAction = {}
        )
    }
}

@Preview(name = "4. Feed - Trống", showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun FeedEmptyPreview() {
    LokapTheme {
        FeedScreen(
            state = FeedUiState(
                selectedTab = FeedTab.FRIENDS,
                friendPosts = emptyList()
            ),
            onAction = {}
        )
    }
}

@Preview(name = "5. Feed - Lỗi", showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun FeedErrorPreview() {
    LokapTheme {
        FeedScreen(
            state = FeedUiState(
                errorMessage = "Không thể tải bảng tin lúc này. Vui lòng kiểm tra kết nối mạng."
            ),
            onAction = {}
        )
    }
}
