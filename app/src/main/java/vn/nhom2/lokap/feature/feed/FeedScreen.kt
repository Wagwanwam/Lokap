package vn.nhom2.lokap.feature.feed

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ripple
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import coil3.compose.AsyncImage
import java.io.File
import kotlinx.coroutines.launch
import vn.nhom2.lokap.core.common.formatTimeAgo
import vn.nhom2.lokap.core.designsystem.component.*
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.designsystem.theme.regionGradient
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.core.navigation.navigateSingleTop
import vn.nhom2.lokap.core.navigation.navigateToTab
import vn.nhom2.lokap.domain.model.*
import vn.nhom2.lokap.feature.post.CommentCountButton
import vn.nhom2.lokap.feature.post.PhotoMessageBar
import vn.nhom2.lokap.feature.post.PostCard
import vn.nhom2.lokap.feature.post.PostCommentsSheet
import vn.nhom2.lokap.feature.post.ReactionCountButton
import vn.nhom2.lokap.feature.post.RepostButton
import vn.nhom2.lokap.feature.post.RepostToggle
import vn.nhom2.lokap.feature.post.showsRepostButton
private const val FEED_TRANSITION_MS = 320

fun NavGraphBuilder.feedScreen(navController: NavController) {
    composable<Route.Feed> {
        FeedRoute(
            // Feed là màn gốc khi mở app đã đăng nhập: pop thì không về được Camera, nên mở tab Camera
            onNavigateBackToCamera = { navController.navigateToTab(Route.Camera) },
            onNavigateToNotifications = { navController.navigateSingleTop(Route.Notifications) },
            onNavigateToSearch = { navController.navigateSingleTop(Route.Search) },
            onNavigateToRegionAlbum = { level, code ->
                navController.navigateSingleTop(Route.RegionAlbum(level, code))
            },
            onNavigateToProfile = { userId -> navController.navigateSingleTop(Route.Profile(userId)) },
            onNavigateToTab = { tab ->
                when (tab) {
                    MainTab.FEED -> Unit
                    MainTab.MAP -> navController.navigateToTab(Route.Map)
                    MainTab.CAMERA -> navController.navigateToTab(Route.Camera)
                    MainTab.INBOX -> navController.navigateToTab(Route.Inbox)
                    MainTab.PROFILE -> navController.navigateToTab(Route.Profile())
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
    onNavigateToRegionAlbum: (String, String) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToTab: (MainTab) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = hiltViewModel()
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
                is FeedEffect.NavigateToRegionAlbum -> onNavigateToRegionAlbum(effect.level, effect.code)
                is FeedEffect.NavigateToProfile -> onNavigateToProfile(effect.userId)
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
                    onSearchClick = { onAction(FeedAction.OnSearchClick) },
                    friendFilterOptions = state.friendFilterOptions,
                    selectedFriendId = state.selectedFriendId,
                    currentUserId = state.currentUserId,
                    onSelectFriend = { onAction(FeedAction.OnSelectFriendFilter(it)) }
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
                    val visiblePosts = remember(state.friendPosts, state.selectedFriendId) {
                        state.visibleFriendPosts
                    }
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (visiblePosts.isEmpty()) {
                                if (state.selectedFriendId == null) {
                                    EmptyState(
                                        title = "Chưa có bài",
                                        actionLabel = "Mở máy ảnh",
                                        onActionClick = { onAction(FeedAction.OnSwipeDownToCamera) }
                                    )
                                } else {
                                    EmptyState(title = "Chưa có bài")
                                }
                            } else {
                                // Đổi người xem thì về ảnh đầu tiên
                                key(state.selectedFriendId) {
                                    FriendsVerticalPager(
                                        posts = visiblePosts,
                                        state = state,
                                        onAction = onAction
                                    )
                                }
                            }
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
                        repostStates = state.repostStates,
                        canRepost = state::canRepost,
                        commentsCountMap = state.commentsCountMap,
                        now = state.now,
                        onAction = onAction
                    )
                }
            }
        }

        state.activeCommentPostId?.let { postId ->
            PostCommentsSheet(postId = postId, onDismiss = { onAction(FeedAction.OnDismissComments) })
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
    onSearchClick: () -> Unit,
    friendFilterOptions: List<User> = emptyList(),
    selectedFriendId: String? = null,
    currentUserId: String = "",
    onSelectFriend: (String?) -> Unit = {}
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    var friendMenuExpanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(if (friendMenuExpanded) 180f else 0f, label = "friendMenuArrow")

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
                            modifier = Modifier.size(25.dp)
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
                                modifier = Modifier.size(24.dp)
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
                .height(48.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FeedTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val isFriendsTab = tab == FeedTab.FRIENDS
                    Surface(
                        shape = RoundedCornerShape(19.dp),
                        color = if (isSelected) colors.surface else Color.Transparent,
                        border = if (isSelected) BorderStroke(1.dp, colors.primary.copy(alpha = 0.35f)) else null,
                        shadowElevation = if (isSelected) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(19.dp))
                            .clickable {
                                // Tab Bạn bè đang mở: bấm lần nữa thì mở menu chọn người
                                if (isFriendsTab && isSelected && friendFilterOptions.isNotEmpty()) {
                                    friendMenuExpanded = true
                                } else {
                                    onSelectTab(tab)
                                }
                            }
                    ) {
                        if (isFriendsTab) {
                            val selectedFriend = friendFilterOptions.firstOrNull { it.id == selectedFriendId }
                            Box {
                                FitText(
                                    text = selectedFriend?.let { friendFilterLabel(it, currentUserId) } ?: tab.labelVi,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (isSelected) colors.primary else colors.textSecondary,
                                    widthFraction = 0.6f
                                )
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = "Chọn người",
                                    tint = if (isSelected) colors.primary else colors.textSecondary,
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 10.dp)
                                        .size(24.dp)
                                        .rotate(arrowRotation)
                                )
                                FriendFilterMenu(
                                    expanded = friendMenuExpanded,
                                    onDismiss = { friendMenuExpanded = false },
                                    options = friendFilterOptions,
                                    selectedId = selectedFriendId,
                                    currentUserId = currentUserId,
                                    onSelect = onSelectFriend
                                )
                            }
                        } else {
                            FitText(
                                text = tab.labelVi,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isSelected) colors.primary else colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Menu của tab Bạn bè: xem ảnh của tất cả hoặc của một người. */
@Composable
private fun FriendFilterMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    options: List<User>,
    selectedId: String?,
    currentUserId: String,
    onSelect: (String?) -> Unit
) {
    val colors = LokapTheme.colors
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .background(colors.surfaceHigh)
            .heightIn(max = 360.dp)
    ) {
        FriendFilterMenuItem(
            label = "Tất cả",
            isSelected = selectedId == null,
            leading = {
                Icon(
                    imageVector = Icons.Filled.People,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            onClick = {
                onDismiss()
                onSelect(null)
            }
        )
        options.forEach { user ->
            FriendFilterMenuItem(
                label = friendFilterLabel(user, currentUserId),
                isSelected = user.id == selectedId,
                leading = {
                    AvatarRing(
                        avatarUrl = user.avatarUrl,
                        displayName = user.displayName,
                        size = 28.dp
                    )
                },
                onClick = {
                    onDismiss()
                    onSelect(user.id)
                }
            )
        }
    }
}

private fun friendFilterLabel(user: User, currentUserId: String): String =
    if (user.id == currentUserId) "Tôi" else user.displayName

@Composable
private fun FriendFilterMenuItem(
    label: String,
    isSelected: Boolean,
    leading: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val colors = LokapTheme.colors
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (isSelected) colors.primary else colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = leading,
        trailingIcon = if (isSelected) {
            {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Đang chọn",
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else null,
        onClick = onClick
    )
}

@Composable
private fun FriendsVerticalPager(
    posts: List<Post>,
    state: FeedUiState,
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
            now = state.now,
            canMessage = state.canMessage(post),
            messageDraft = state.messageDrafts[post.id].orEmpty(),
            isSendingMessage = post.id in state.sendingMessagePostIds,
            commentsCount = if (post.audience == Audience.PUBLIC) state.commentsCountMap[post.id] ?: 0 else null,
            repostState = if (showsRepostButton(post)) state.repostStates[post.id] ?: RepostState() else null,
            canRepost = state.canRepost(post),
            isMine = post.repostedBy?.id == state.currentUserId,
            onAction = onAction
        )
    }
}

/** Chiều cao thanh điều hướng dưới (nổi trên Feed); Scaffold chừa sẵn khoảng này. */
private val FEED_BOTTOM_NAV_HEIGHT = 64.dp

/**
 * 1 ảnh ở tab Bạn bè (kiểu Locket): không mở màn nào khác.
 * Ảnh bạn bè (không công khai): thanh "Gửi tin nhắn…" + thả emoji. Ảnh của mình / ảnh công khai: số cảm xúc (+ bình luận).
 */
@Composable
private fun LocketFriendPageItem(
    post: Post,
    now: java.time.Instant,
    canMessage: Boolean,
    messageDraft: String,
    isSendingMessage: Boolean,
    commentsCount: Int?,
    /** null = không phải ảnh công khai (không có nút đăng lại). */
    repostState: RepostState?,
    canRepost: Boolean,
    /** Ảnh này do chính mình đăng lại. */
    isMine: Boolean,
    onAction: (FeedAction) -> Unit
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

    // Bàn phím mở: đẩy thanh nhắn tin lên sát bàn phím, ảnh tự thu nhỏ cho vừa (khu nội dung đã chừa thanh điều hướng)
    val density = LocalDensity.current
    val imeLift = with(density) {
        (WindowInsets.ime.getBottom(density) - WindowInsets.navigationBars.getBottom(density)).coerceAtLeast(0).toDp()
    }
    val keyboardLift = (imeLift - FEED_BOTTOM_NAV_HEIGHT).coerceAtLeast(0.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = keyboardLift)
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        post.repostedBy?.let { reposter ->
            RepostedByLabel(
                name = if (isMine) "Bạn" else reposter.displayName,
                onClick = { onAction(FeedAction.OnAuthorClick(reposter.id)) }
            )
        }
        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .aspectRatio(1f, matchHeightConstraintsFirst = true)
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
                        imageVector = if (post.audience == Audience.PUBLIC) Icons.Rounded.Public else Icons.Filled.People,
                        contentDescription = null,
                        tint = colors.buttonText,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (post.audience == Audience.PUBLIC) "Công khai" else "Bạn bè",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        color = colors.buttonText
                    )
                }
            }

            // Vị trí ở góc phải trên ảnh; vùng chạm phủ cả lề quanh pill để đủ lớn
            val locationInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clickable(
                        interactionSource = locationInteraction,
                        indication = null,
                        onClickLabel = "Mở album $locationLabel",
                        role = Role.Button,
                        onClick = { onAction(FeedAction.OnLocationChipClick(post)) }
                    )
                    .padding(spacing.sm)
                    .widthIn(max = 190.dp)
                    .clip(shapes.pill)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .indication(locationInteraction, ripple())
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = "Vị trí",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = locationLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                                onClick = { onAction(FeedAction.OnRetrySendPost(post.id)) }
                            )
                        }
                    }
                }

                PostSendState.SENT -> Unit
            }
        }

            Spacer(modifier = Modifier.height(spacing.md))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                AvatarRing(
                    avatarUrl = post.author.avatarUrl,
                    displayName = post.author.displayName,
                    size = 36.dp,
                    onClick = { onAction(FeedAction.OnAuthorClick(post.author.id)) }
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
        }

        Spacer(modifier = Modifier.height(spacing.md))

        when {
            canMessage -> PhotoMessageBar(
                text = messageDraft,
                onTextChange = { onAction(FeedAction.OnMessageDraftChange(post.id, it)) },
                isSending = isSendingMessage,
                onSend = { onAction(FeedAction.OnSendPhotoMessage(post)) },
                reactions = post.reactions,
                myReaction = post.myReaction,
                onReact = { emoji -> onAction(FeedAction.OnReact(post.id, emoji)) },
                modifier = Modifier.padding(bottom = spacing.sm)
            )

            post.sendState == PostSendState.SENT -> Row(
                modifier = Modifier
                    .padding(bottom = spacing.sm)
                    .clip(shapes.pill)
                    .background(colors.surfaceHigh)
                    .border(1.dp, colors.outline, shapes.pill)
                    .padding(horizontal = spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                ReactionCountButton(
                    reactions = post.reactions,
                    myReaction = post.myReaction,
                    onReact = { emoji -> onAction(FeedAction.OnReact(post.id, emoji)) }
                )
                if (commentsCount != null) {
                    CommentCountButton(
                        count = commentsCount,
                        onClick = { onAction(FeedAction.OnOpenComments(post.id)) }
                    )
                }
                if (repostState != null) {
                    RepostButton(
                        count = repostState.count,
                        isReposted = repostState.isMine,
                        enabled = canRepost,
                        onClick = { onAction(FeedAction.OnToggleRepost(post.id)) }
                    )
                }
            }

            // Ảnh đang gửi / gửi lỗi: giữ chỗ để ảnh không nhảy khi gửi xong
            else -> Spacer(modifier = Modifier.height(56.dp))
        }
    }
}

/** "↻ Lan đã đăng lại" phía trên ảnh đăng lại ở tab Bạn bè; bấm mở hồ sơ người đăng lại. */
@Composable
private fun RepostedByLabel(name: String, onClick: () -> Unit) {
    val colors = LokapTheme.colors
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(LokapTheme.shapes.pill)
            .clickable(role = Role.Button, onClickLabel = "Xem hồ sơ", onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Repeat,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = "$name đã đăng lại",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TwitterExploreFeed(
    posts: List<Post>,
    currentProvinceName: String,
    locationSearchQuery: String,
    repostStates: Map<String, RepostState>,
    canRepost: (Post) -> Boolean,
    commentsCountMap: Map<String, Int>,
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = "Khu vực",
                            tint = colors.primary,
                            modifier = Modifier.size(35.dp)
                        )
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.textPrimary,
                            maxLines = 1
                        )
                    }
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
                        .height(50.dp)
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
                            modifier = Modifier.size(22.dp)
                        )
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (locationSearchQuery.isEmpty()) {
                                Text(
                                    text = "Tìm địa danh",
                                    style = MaterialTheme.typography.bodyLarge,
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
                                    .size(22.dp)
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
                            title = "Chưa có bài"
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
                val repostState = repostStates[post.id] ?: RepostState()
                // Bài công khai: không có thanh nhắn tin (kể cả của bạn bè)
                PostCard(
                    post = post,
                    now = now,
                    onReact = { emoji -> onAction(FeedAction.OnReact(post.id, emoji)) },
                    onAuthorClick = { onAction(FeedAction.OnAuthorClick(post.author.id)) },
                    commentsCount = commentsCountMap[post.id] ?: 0,
                    onOpenComments = { onAction(FeedAction.OnOpenComments(post.id)) },
                    onLocationClick = { onAction(FeedAction.OnLocationChipClick(post)) },
                    repost = RepostToggle(
                        isReposted = repostState.isMine,
                        count = repostState.count,
                        enabled = canRepost(post)
                    ) { onAction(FeedAction.OnToggleRepost(post.id)) },
                    onShare = { onAction(FeedAction.OnShareLink(post)) }
                )
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
                                modifier = Modifier.size(22.dp)
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
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.LocationOn,
                                        contentDescription = null,
                                        tint = if (isSelected) colors.primary else colors.textTertiary,
                                        modifier = Modifier.size(22.dp)
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
                                        modifier = Modifier.size(25.dp)
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
                    title = "Không tìm thấy"
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
                                    fontSize = 16.sp
                                ),
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "@${user.username}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.5.sp
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
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Đã gửi",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 13.5.sp
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
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Bạn bè",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.5.sp
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
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Kết bạn",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.5.sp
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
