Bạn là lập trình viên Android senior kiêm UI designer. Sửa giao diện 3 file Kotlin (Jetpack Compose + Material 3) của app Lokap (kiểu Locket, dark mode, màu nhấn hồng, GenZ) theo **quy chuẩn tối giản** dưới đây. Code hiện tại của 3 file nằm ở cuối prompt.

## Quy chuẩn tối giản (áp dụng cho mọi chỗ trong 3 file)
1. **Bớt chữ**: bỏ dòng phụ, dòng giải thích, chữ thừa. Tiêu đề chỉ giữ danh từ chính.
   Ví dụ: "Gần bạn · Đà Nẵng" + dòng "Khoảnh khắc công khai mới nhất quanh khu vực của bạn" → chỉ còn icon `LocationOn` + "Đà Nẵng".
2. **Ưu tiên icon hơn chữ**: chỗ nào icon đủ rõ nghĩa thì dùng icon thay chữ (nút, nhãn, trạng thái). Chỉ giữ chữ cạnh icon khi icon dễ hiểu nhầm. Mọi icon giữ `contentDescription` tiếng Việt.
3. **Icon to hơn**: icon thường ≥ 24.dp, icon nút chính 28–32.dp, vùng chạm ≥ 48.dp.
4. **Chữ trên ảnh căn giữa**: caption trên ảnh căn giữa ngang; hàng tác giả (avatar + tên + thời gian) và chip vị trí căn giữa theo chiều dọc với nhau. Tên + thời gian trên ảnh hiện khó đọc khi ảnh sáng → thêm nền mờ tối (scrim/gradient từ `LokapTheme.colors.background` với alpha) phía sau.
5. **Chat — lỗi lặp chú thích**: tin nhắn kèm ảnh Locket đang hiện caption 2 lần (trên ảnh và trong bong bóng chữ bên dưới). Caption chỉ hiện một lần, trên ảnh. Nếu nội dung tin nhắn trùng caption thì ẩn bong bóng chữ; nếu khác thì vẫn hiện bong bóng.

## Ràng buộc
- KHÔNG đổi logic, ViewModel, UiState, Action, Effect, chữ ký các hàm public/internal, tên Route, hàm `NavGraphBuilder.xxxScreen`. Giữ đủ Preview (bọc LokapTheme).
- Màu chỉ từ `LokapTheme.colors` (background, surface, surfaceHigh, outline, primary, primarySoft, onPrimary, textPrimary, textSecondary, textTertiary, accentLilac, accentLime, accentCyan, accentYellow, error, success); khoảng cách `LokapTheme.spacing` (xs, sm, md, lg, xl, xxl); bo góc `LokapTheme.shapes`. Không hard-code màu. Chữ trên nền hồng dùng `onPrimary`.
- Icon chỉ dùng gói material-icons-core (`Icons.Default.*`, `Icons.AutoMirrored.Filled.*`). Không dùng Visibility, CameraAlt, PhotoCamera… (thuộc gói extended).
- Chỉ sửa đúng 3 file dưới đây, không tạo file mới.

## Định dạng trả lời
- Trả lại **nguyên file đầy đủ** (không rút gọn, không "// ... giữ nguyên"), mỗi file một khối ```kotlin … ``` RIÊNG, dòng ĐẦU TIÊN là `// FILE: <đường dẫn y như bên dưới>`.
- Cuối cùng: bảng ngắn "Trước → Sau" liệt kê từng chỗ đã đổi.
- Quá dài thì dừng ở cuối một file hoàn chỉnh, ghi `CÒN TIẾP`.

## Code hiện tại

### app/src/main/java/vn/nhom2/lokap/feature/feed/FeedScreen.kt
```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/feed/FeedScreen.kt
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
```

### app/src/main/java/vn/nhom2/lokap/core/designsystem/component/PhotoCard.kt
```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/core/designsystem/component/PhotoCard.kt
package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import vn.nhom2.lokap.core.common.formatTimeAgo
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.designsystem.theme.regionGradient
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.Post
import vn.nhom2.lokap.domain.model.PostLocation
import vn.nhom2.lokap.domain.model.PostSendState
import java.time.Instant

@Composable
fun PhotoCard(
    post: Post,
    now: Instant,
    onReact: (String) -> Unit,
    onRetrySend: () -> Unit,
    onReport: () -> Unit,
    onBlockUser: () -> Unit,
    modifier: Modifier = Modifier,
    onAuthorClick: (() -> Unit)? = null,
    onLocationClick: (() -> Unit)? = null
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes
    val isPreview = LocalInspectionMode.current
    var menuExpanded by remember { mutableStateOf(false) }

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
        formatPostLocationLabel(post.location)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shapes.card)
                .background(colors.surface)
                .border(1.dp, colors.outline, shapes.card)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(regionGradient(post.location.provinceCode ?: post.id))
            )

            if (!isPreview) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = post.caption ?: post.author.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Lớp phủ gradient trên & dưới để chữ nổi bật
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to colors.background.copy(alpha = 0.65f),
                            0.28f to colors.background.copy(alpha = 0.0f),
                            0.68f to colors.background.copy(alpha = 0.0f),
                            1.0f to colors.background.copy(alpha = 0.80f)
                        )
                    )
            )

            // Thanh trên: Avatar + tên + thời gian + vị trí + menu ⋮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(spacing.md),
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
                        size = 40.dp,
                        hasNewPost = false,
                        onClick = onAuthorClick
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = post.author.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = timeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }

                GlassChip(
                    label = locationLabel,
                    leadingIcon = Icons.Rounded.LocationOn,
                    onClick = onLocationClick
                )

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Tùy chọn bài đăng",
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
                                    text = "Báo cáo bài đăng",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textPrimary
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onReport()
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
                                onBlockUser()
                            }
                        )
                    }
                }
            }

            // Chú thích ảnh phía dưới
            if (!post.caption.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(spacing.md)
                        .clip(shapes.pill)
                        .background(colors.background.copy(alpha = 0.72f))
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
                            .background(colors.background.copy(alpha = 0.75f)),
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

        // Hàng thả cảm xúc bên dưới thẻ ảnh
        EmojiBar(
            reactions = post.reactions,
            selectedEmoji = post.myReaction,
            onSelectEmoji = onReact,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

private fun formatPostLocationLabel(location: PostLocation): String {
    return when (location.level) {
        LocationLevel.PLACE -> location.placeName ?: location.wardName ?: location.provinceName ?: "Việt Nam"
        LocationLevel.WARD -> location.wardName ?: location.provinceName ?: "Việt Nam"
        LocationLevel.PROVINCE -> location.provinceName ?: "Việt Nam"
        LocationLevel.COUNTRY -> "Việt Nam"
    }
}

@Preview(name = "PhotoCard - Đã gửi", showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun PhotoCardSentPreview() {
    LokapTheme {
        Box(
            modifier = Modifier
                .background(LokapTheme.colors.background)
                .padding(LokapTheme.spacing.lg)
        ) {
            PhotoCard(
                post = SampleData.posts[0],
                now = SampleData.NOW,
                onReact = {},
                onRetrySend = {},
                onReport = {},
                onBlockUser = {}
            )
        }
    }
}

@Preview(name = "PhotoCard - Gửi lỗi", showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun PhotoCardFailedPreview() {
    LokapTheme {
        Box(
            modifier = Modifier
                .background(LokapTheme.colors.background)
                .padding(LokapTheme.spacing.lg)
        ) {
            PhotoCard(
                post = SampleData.posts[6],
                now = SampleData.NOW,
                onReact = {},
                onRetrySend = {},
                onReport = {},
                onBlockUser = {}
            )
        }
    }
}
```

### app/src/main/java/vn/nhom2/lokap/feature/chat/ChatScreen.kt
```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/chat/ChatScreen.kt
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
```
