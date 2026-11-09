package vn.nhom2.lokap.feature.feed

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
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
import coil3.compose.AsyncImage
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.nhom2.lokap.core.common.formatTimeAgo
import vn.nhom2.lokap.core.designsystem.component.*
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.designsystem.theme.ThemeManager
import vn.nhom2.lokap.core.designsystem.theme.regionGradient
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.core.navigation.navigateSingleTop
import vn.nhom2.lokap.core.navigation.popBackStackSafely
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.*
private const val FEED_TRANSITION_MS = 320

fun NavGraphBuilder.feedScreen(navController: NavController) {
    composable<Route.Feed> {
        FeedRoute(
            onNavigateBackToCamera = { navController.popBackStackSafely() },
            onNavigateToNotifications = { navController.navigateSingleTop(Route.Notifications) },
            onNavigateToSearch = { navController.navigateSingleTop(Route.Search) },
            onNavigateToPostDetail = { id -> navController.navigateSingleTop(Route.PostDetail(id)) },
            onNavigateToRegionAlbum = { level, code ->
                navController.navigateSingleTop(Route.RegionAlbum(level, code))
            },
            onNavigateToProfile = { userId -> navController.navigateSingleTop(Route.Profile(userId)) },
            onNavigateToChat = { convId -> navController.navigateSingleTop(Route.Chat(convId)) },
            onNavigateToTab = { tab ->
                when (tab) {
                    MainTab.FEED -> Unit
                    MainTab.MAP -> navController.navigateSingleTop(Route.Map)
                    MainTab.CAMERA -> navController.navigateSingleTop(Route.Camera)
                    MainTab.INBOX -> navController.navigateSingleTop(Route.Inbox)
                    MainTab.PROFILE -> navController.navigateSingleTop(Route.Profile())
                }
            }
        )
    }
}

@Composable
fun FeedRoute(
    onNavigateBackToCamera: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToRegionAlbum: (String, String) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToTab: (MainTab) -> Unit = {},
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
                FeedEffect.NavigateToSearch -> onNavigateToSearch()
                is FeedEffect.NavigateToPostDetail -> onNavigateToPostDetail(effect.postId)
                is FeedEffect.NavigateToRegionAlbum -> onNavigateToRegionAlbum(effect.level, effect.code)
                is FeedEffect.NavigateToProfile -> onNavigateToProfile(effect.userId)
                is FeedEffect.NavigateToChat -> onNavigateToChat(effect.conversationId)

                is FeedEffect.ShowMessage -> scope.launch { snackbarHostState.showSnackbar(effect.message) }
            }
        }
    }

    FeedScreen(
        state = state,
        onAction = viewModel::onAction,
        onNavigateToTab = onNavigateToTab,
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
    onNavigateToTab: (MainTab) -> Unit = {},
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
        bottomBar = {
            Spacer(
                modifier = Modifier
                    .navigationBarsPadding()
                    .height(64.dp)
            )
        },
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
                    onNotificationsClick = { onAction(FeedAction.OnNotificationsClick) },
                    onSearchClick = { onAction(FeedAction.OnSearchClick) }
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
                    val postsToShow = state.explorePosts.ifEmpty {
                        if (state.currentProvinceName == NATIONWIDE_VIETNAM) {
                            state.allPublicPosts.ifEmpty { (state.nearbyPosts + state.landmarkPosts).distinctBy { it.id } }
                        } else {
                            state.explorePosts
                        }
                    }

                    TwitterExploreFeed(
                        posts = postsToShow,
                        currentProvinceName = state.currentProvinceName,
                        locationSearchQuery = state.locationSearchQuery,
                        repostedPostIds = state.repostedPostIds,
                        commentsCountMap = state.commentsCountMap,
                        activeReactionPickerPostId = state.activeReactionPickerPostId,
                        now = state.now,
                        onAction = onAction
                    )
                }
            }
        }

        if (state.activeCommentPostId != null) {
            val postId = state.activeCommentPostId
            val comments = vn.nhom2.lokap.data.fake.FakeCommentStore.getComments(postId)
            CommentBottomSheet(
                comments = comments,
                onDismiss = { onAction(FeedAction.OnDismissComments) },
                onSendComment = { text -> onAction(FeedAction.OnAddComment(postId, text)) }
            )
        }

        if (state.isProvincePickerOpen) {
            ProvincePickerBottomSheet(
                currentProvinceName = state.currentProvinceName,
                allPublicPosts = state.allPublicPosts,
                searchQuery = state.provinceFilterSearch,
                onSearchQueryChange = { onAction(FeedAction.OnProvinceFilterSearchChange(it)) },
                onSelectProvince = { onAction(FeedAction.OnSelectProvince(it)) },
                onDismiss = { onAction(FeedAction.OnDismissProvincePicker) }
            )
        }
    }
}

@Composable
private fun FeedTopBar(
    selectedTab: FeedTab,
    unreadNotificationsCount: Int,
    onSelectTab: (FeedTab) -> Unit,
    onBackToCamera: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LokapLogo(
                size = 30.dp,
                showText = true
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    shape = CircleShape,
                    color = colors.surfaceHigh,
                    border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onSearchClick)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Tìm kiếm",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Box(contentAlignment = Alignment.TopEnd) {
                    Surface(
                        shape = CircleShape,
                        color = colors.surfaceHigh,
                        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onNotificationsClick)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Notifications,
                                contentDescription = "Thông báo",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                    BadgeDot(count = unreadNotificationsCount)
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = colors.surfaceHigh.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.18f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FeedTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Surface(
                        shape = RoundedCornerShape(19.dp),
                        color = if (isSelected) colors.surface else Color.Transparent,
                        border = if (isSelected) BorderStroke(1.dp, colors.primary.copy(alpha = 0.35f)) else null,
                        shadowElevation = if (isSelected) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(19.dp))
                            .clickable { onSelectTab(tab) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = tab.labelVi,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
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

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(spacing.sm)
                    .clip(shapes.pill)
                    .background(colors.primary)
                    .padding(horizontal = 8.dp, vertical = 3.5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.People,
                        contentDescription = null,
                        tint = colors.buttonText,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (post.audience == Audience.PUBLIC) "Công khai" else "Bạn bè",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = colors.buttonText
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(spacing.sm)
                    .clip(shapes.pill)
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 8.dp, vertical = 3.5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        color = androidx.compose.ui.graphics.Color.White
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
private fun TwitterExploreFeed(
    posts: List<Post>,
    currentProvinceName: String,
    locationSearchQuery: String,
    repostedPostIds: Set<String>,
    commentsCountMap: Map<String, Int>,
    activeReactionPickerPostId: String?,
    now: java.time.Instant,
    onAction: (FeedAction) -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = spacing.md,
            end = spacing.md,
            top = spacing.sm,
            bottom = spacing.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        item(key = "header_explore_title") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.xs),
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val displayTitle = if (currentProvinceName == NATIONWIDE_VIETNAM) "Việt Nam" else currentProvinceName
                    Text(
                        text = "Khám phá · $displayTitle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "${posts.size} khoảnh khắc",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surfaceHigh,
                    border = BorderStroke(
                        1.dp,
                        if (locationSearchQuery.isNotEmpty()) colors.primary else colors.outline.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Tìm kiếm",
                            tint = if (locationSearchQuery.isNotEmpty()) colors.primary else colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (locationSearchQuery.isEmpty()) {
                                Text(
                                    text = "Tìm tỉnh thành, địa danh…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textTertiary,
                                    maxLines = 1
                                )
                            }
                            BasicTextField(
                                value = locationSearchQuery,
                                onValueChange = { onAction(FeedAction.OnLocationSearchQueryChange(it)) },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
                                cursorBrush = SolidColor(colors.primary),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (locationSearchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Xoá",
                                tint = colors.textSecondary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .clickable { onAction(FeedAction.OnClearLocationSearch) }
                            )
                        }
                    }
                }
            }
        }

        if (posts.isEmpty()) {
            item(key = "empty_posts_state") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, bottom = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        EmptyState(
                            title = if (currentProvinceName == NATIONWIDE_VIETNAM) "Chưa có bài nào công khai" else "Chưa có bài nào tại $currentProvinceName",
                            subtitle = if (currentProvinceName == NATIONWIDE_VIETNAM) "Rủ bạn bè vào Lokap để cùng chia sẻ nhé" else "Hãy là người đầu tiên chia sẻ khoảnh khắc tại đây!"
                        )
                        if (currentProvinceName != NATIONWIDE_VIETNAM) {
                            PinkButton(
                                text = "Khám phá toàn quốc",
                                onClick = { onAction(FeedAction.OnSelectProvince(NATIONWIDE_VIETNAM)) }
                            )
                        }
                    }
                }
            }
        } else {
            items(
                items = posts,
                key = { "twitter_post_${it.id}" }
            ) { post ->
                val isReposted = repostedPostIds.contains(post.id)
                val commentsCount = commentsCountMap[post.id] ?: vn.nhom2.lokap.data.fake.FakeCommentStore.getTotalCount(post.id)
                val showReactionPicker = activeReactionPickerPostId == post.id

                TwitterPostItem(
                    post = post,
                    isReposted = isReposted,
                    commentsCount = commentsCount,
                    showReactionPicker = showReactionPicker,
                    now = now,
                    onAction = onAction
                )
            }
        }
    }
}

@Composable
private fun TwitterPostItem(
    post: Post,
    isReposted: Boolean,
    commentsCount: Int,
    showReactionPicker: Boolean,
    now: java.time.Instant,
    onAction: (FeedAction) -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val isPreview = LocalInspectionMode.current
    val coroutineScope = rememberCoroutineScope()
    var showHeartAnim by remember { mutableStateOf(false) }
    val photoHeartScale by animateFloatAsState(
        targetValue = if (showHeartAnim) 1.25f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LokapTheme.shapes.card)
            .background(colors.surface)
            .border(1.dp, colors.outline, LokapTheme.shapes.card)
            .clickable { onAction(FeedAction.OnPostClick(post.id)) }
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            AvatarRing(
                avatarUrl = post.author.avatarUrl,
                displayName = post.author.displayName,
                size = 38.dp,
                onClick = { onAction(FeedAction.OnAuthorClick(post.author.id)) }
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = post.author.displayName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatTimeAgo(post.takenAt, now),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = colors.textSecondary,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                val locText = listOfNotNull(post.location.wardName, post.location.provinceName)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                if (locText.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.padding(top = 1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = locText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = colors.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        val captionText = post.caption.orEmpty()
        if (captionText.isNotBlank()) {
            Text(
                text = captionText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp
                ),
                color = colors.textPrimary
            )
        }

        if (post.imageUrl.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.25f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceHigh)
                    .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
                    .pointerInput(post.id) {
                        detectTapGestures(
                            onTap = { onAction(FeedAction.OnPostClick(post.id)) },
                            onDoubleTap = {
                                showHeartAnim = true
                                onAction(FeedAction.OnReact(post.id, "❤️"))
                                coroutineScope.launch {
                                    delay(650)
                                    showHeartAnim = false
                                }
                            }
                        )
                    }
            ) {
                if (!isPreview) {
                    AsyncImage(
                        model = post.imageUrl,
                        contentDescription = "Ảnh bài viết",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.primary.copy(alpha = 0.92f))
                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.People,
                            contentDescription = null,
                            tint = colors.buttonText,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = if (post.audience == Audience.PUBLIC) "Công khai" else "Bạn bè",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.buttonText,
                            fontSize = 10.5.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = formatTimeAgo(post.takenAt, now),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = androidx.compose.ui.graphics.Color.White,
                            fontSize = 10.5.sp
                        )
                    }
                }

                if (photoHeartScale > 0.05f) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier
                                .size(72.dp)
                                .scale(photoHeartScale)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showReactionPicker,
            enter = fadeIn() + scaleIn(initialScale = 0.8f) + slideInVertically { it / 2 },
            exit = fadeOut() + scaleOut()
        ) {
            Row(
                modifier = Modifier
                    .clip(LokapTheme.shapes.pill)
                    .background(colors.surfaceHigh)
                    .border(1.dp, colors.primary, LokapTheme.shapes.pill)
                    .padding(horizontal = spacing.md, vertical = spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("❤️", "🔥", "😂", "😮", "😢", "👍").forEach { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 20.sp,
                        modifier = Modifier
                            .clickable {
                                onAction(FeedAction.OnReact(post.id, emoji))
                                onAction(FeedAction.OnDismissReactionPicker)
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            val isLiked = post.myReaction != null
            val totalReactions = post.reactions.sumOf { it.count }
            val likeScale by animateFloatAsState(
                targetValue = if (isLiked) 1.25f else 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(LokapTheme.shapes.pill)
                    .pointerInput(post.id) {
                        detectTapGestures(
                            onTap = { onAction(FeedAction.OnReact(post.id, "❤️")) },
                            onLongPress = { onAction(FeedAction.OnShowReactionPicker(post.id)) }
                        )
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Thích",
                    tint = if (isLiked) colors.primary else colors.textSecondary,
                    modifier = Modifier
                        .size(18.dp)
                        .scale(likeScale)
                )
                Text(
                    text = totalReactions.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isLiked) colors.primary else colors.textSecondary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(LokapTheme.shapes.pill)
                    .clickable { onAction(FeedAction.OnOpenComments(post.id)) }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = "Bình luận",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = commentsCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(LokapTheme.shapes.pill)
                    .clickable { onAction(FeedAction.OnToggleRepost(post.id)) }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = "Đăng lại",
                    tint = if (isReposted) colors.primary else colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (isReposted) "1" else "0",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isReposted) colors.primary else colors.textSecondary
                )
            }

            IconButton(
                onClick = { onAction(FeedAction.OnShareLink(post)) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Chia sẻ liên kết",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentBottomSheet(
    comments: List<vn.nhom2.lokap.feature.postdetail.RedditCommentUi>,
    onDismiss: () -> Unit,
    onSendComment: (String) -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    var input by remember { androidx.compose.runtime.mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val allFlattenedComments = remember(comments) {
        val result = mutableListOf<vn.nhom2.lokap.feature.postdetail.RedditCommentUi>()
        fun collect(list: List<vn.nhom2.lokap.feature.postdetail.RedditCommentUi>) {
            for (c in list) {
                result.add(c)
                collect(c.replies)
            }
        }
        collect(comments)
        result
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = LokapTheme.shapes.sheet,
        containerColor = colors.surface,
        contentColor = colors.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md, vertical = spacing.sm)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Text(
                text = "Bình luận · ${allFlattenedComments.size}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = colors.textPrimary
            )

            if (allFlattenedComments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chưa có bình luận nào. Hãy là người đầu tiên!",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = colors.textSecondary
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allFlattenedComments.forEach { comment ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.surfaceHigh)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            AvatarRing(
                                avatarUrl = comment.authorAvatarUrl,
                                displayName = comment.authorName,
                                size = 28.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = comment.authorName,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = comment.timeAgo,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = colors.textSecondary
                                    )
                                }
                                Text(
                                    text = comment.content,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    ),
                                    color = colors.textPrimary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Viết bình luận…") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.outline,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            onSendComment(input)
                            input = ""
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Gửi",
                        tint = colors.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProvincePickerBottomSheet(
    currentProvinceName: String,
    allPublicPosts: List<Post>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectProvince: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val postCounts = remember(allPublicPosts) {
        val map = mutableMapOf<String, Int>()
        map[NATIONWIDE_VIETNAM] = allPublicPosts.size
        allPublicPosts.forEach { post ->
            val pName = post.location.provinceName.orEmpty()
            if (pName.isNotBlank()) {
                val matched = ALL_VIETNAM_PROVINCES.firstOrNull {
                    it.equals(pName, ignoreCase = true) ||
                        it.contains(pName, ignoreCase = true) ||
                        pName.contains(it, ignoreCase = true)
                } ?: pName
                map[matched] = (map[matched] ?: 0) + 1
            }
        }
        map
    }

    val filteredProvinces = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            ALL_VIETNAM_PROVINCES
        } else {
            ALL_VIETNAM_PROVINCES.filter {
                it.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = LokapTheme.shapes.sheet,
        containerColor = colors.surface,
        contentColor = colors.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md, vertical = spacing.sm)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Chọn khu vực",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = colors.textSecondary
                    )
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Tìm tỉnh, thành phố...",
                        color = colors.textTertiary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colors.textTertiary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Xoá",
                                tint = colors.textTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.outline.copy(alpha = 0.3f),
                    focusedContainerColor = colors.surfaceHigh,
                    unfocusedContainerColor = colors.surfaceHigh,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                )
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredProvinces) { province ->
                    val isSelected = province == currentProvinceName
                    val count = postCounts[province] ?: 0

                    Surface(
                        onClick = { onSelectProvince(province) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) colors.primary.copy(alpha = 0.12f) else Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (province == NATIONWIDE_VIETNAM) {
                                    Icon(
                                        imageVector = Icons.Rounded.Public,
                                        contentDescription = null,
                                        tint = if (isSelected) colors.primary else colors.textTertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.LocationOn,
                                        contentDescription = null,
                                        tint = if (isSelected) colors.primary else colors.textTertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = province,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) colors.primary else colors.textPrimary
                                    )
                                    if (province == NATIONWIDE_VIETNAM) {
                                        Text(
                                            text = "Tất cả bài đăng trên toàn quốc",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (count > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) colors.primary.copy(alpha = 0.2f) else colors.surfaceHigh,
                                        border = BorderStroke(1.dp, if (isSelected) colors.primary.copy(alpha = 0.4f) else colors.outline.copy(alpha = 0.2f))
                                    ) {
                                        Text(
                                            text = "$count bài",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) colors.primary else colors.textSecondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Đã chọn",
                                        tint = colors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendSearchTabContent(
    state: FeedUiState,
    onAction: (FeedAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { onAction(FeedAction.OnSearchQueryChange(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Tìm theo tên hoặc tag name…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textTertiary
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Tìm kiếm",
                    tint = colors.primary
                )
            },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onAction(FeedAction.OnSearchQueryChange("")) }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Xoá",
                            tint = colors.textSecondary
                        )
                    }
                }
            },
            singleLine = true,
            shape = shapes.button,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.outline,
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
                cursorColor = colors.primary,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            )
        )

        Text(
            text = if (state.searchQuery.isBlank()) "Gợi ý bạn bè" else "Kết quả tìm kiếm (${state.searchResults.size})",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = colors.textSecondary
        )

        if (state.searchResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.xl),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = "Không tìm thấy bạn bè nào",
                    subtitle = "Thử tìm kiếm với tên hoặc tag name khác nhé."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                items(state.searchResults, key = { it.id }) { user ->
                    val status = state.friendStatuses[user.id] ?: FriendStatus.NONE
                    val isOnline = (user.id.hashCode() and 1) == 0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.outline.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                            .clickable { onAction(FeedAction.OnAuthorClick(user.id)) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AvatarRing(
                            avatarUrl = user.avatarUrl,
                            displayName = user.displayName,
                            size = 42.dp,
                            isOnline = isOnline,
                            onClick = { onAction(FeedAction.OnAuthorClick(user.id)) }
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.displayName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.5.sp
                                ),
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "@${user.username}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp
                                ),
                                color = colors.textTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        when (status) {
                            FriendStatus.REQUEST_SENT -> {
                                Surface(
                                    shape = CircleShape,
                                    color = colors.surfaceHigh,
                                    border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .clickable { onAction(FeedAction.OnToggleFriendRequest(user.id)) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = colors.textSecondary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Đã gửi",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 12.sp
                                            ),
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                            }
                            FriendStatus.FRIENDS -> {
                                Surface(
                                    shape = CircleShape,
                                    color = colors.surfaceHigh,
                                    border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .clickable { onAction(FeedAction.OnAuthorClick(user.id)) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DoneAll,
                                            contentDescription = null,
                                            tint = colors.primary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Bạn bè",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp
                                            ),
                                            color = colors.primary
                                        )
                                    }
                                }
                            }
                            else -> {
                                Surface(
                                    shape = CircleShape,
                                    color = colors.primary,
                                    modifier = Modifier
                                        .height(32.dp)
                                        .clickable { onAction(FeedAction.OnToggleFriendRequest(user.id)) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PersonAdd,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Kết bạn",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp
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
