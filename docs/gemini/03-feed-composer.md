Gemini đã nói

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/data/fake/SampleData2.kt
package vn.nhom2.lokap.data.fake

import vn.nhom2.lokap.domain.model.Place

object SampleData2 {

    const val DEFAULT_PROVINCE_CODE = "da-nang"
    const val DEFAULT_PROVINCE_NAME = "Đà Nẵng"
    const val DEFAULT_WARD_CODE = "dn-hai-chau"
    const val DEFAULT_WARD_NAME = "Phường Hải Châu"

    /**
     * 8 địa điểm mẫu quanh Đà Nẵng để chọn trong Composer khi chọn LocationLevel.PLACE
     */
    val daNangPlaces: List<Place> = listOf(
        Place(
            id = "pl_dn_1",
            name = "Trình Cà Phê",
            address = "25 Phạm Hồng Thái, Phường Hải Châu, Đà Nẵng",
            lat = 16.0682,
            lng = 108.2215
        ),
        Place(
            id = "pl_dn_2",
            name = "Bãi biển Mỹ Khê",
            address = "Đường Võ Nguyên Giáp, Phường Sơn Trà, Đà Nẵng",
            lat = 16.0598,
            lng = 108.2465
        ),
        Place(
            id = "pl_dn_3",
            name = "Trường Đại học Bách khoa - ĐH Đà Nẵng",
            address = "54 Nguyễn Lương Bằng, Đà Nẵng",
            lat = 16.0738,
            lng = 108.1499
        ),
        Place(
            id = "pl_dn_4",
            name = "Cầu Rồng",
            address = "Đường Nguyễn Văn Linh, Phường Hải Châu, Đà Nẵng",
            lat = 16.0611,
            lng = 108.2275
        ),
        Place(
            id = "pl_dn_5",
            name = "Chợ Cồn",
            address = "290 Hùng Vương, Phường Hải Châu, Đà Nẵng",
            lat = 16.0679,
            lng = 108.2144
        ),
        Place(
            id = "pl_dn_6",
            name = "Bán đảo Sơn Trà - Đỉnh Bàn Cờ",
            address = "Phường Sơn Trà, Đà Nẵng",
            lat = 16.1189,
            lng = 108.2736
        ),
        Place(
            id = "pl_dn_7",
            name = "Phố cổ Hội An",
            address = "Đường Trần Phú, Phường Hội An, Đà Nẵng",
            lat = 15.8794,
            lng = 108.3350
        ),
        Place(
            id = "pl_dn_8",
            name = "Reply 1988 Cafe",
            address = "20 Lê Hồng Phong, Phường Hải Châu, Đà Nẵng",
            lat = 16.0635,
            lng = 108.2192
        )
    )

    val placeToLandmarkMap: Map<String, String> = mapOf(
        "pl_dn_4" to "lm_cau_rong",
        "pl_dn_6" to "lm_son_tra",
        "pl_dn_7" to "lm_hoi_an"
    )

    val reportReasons: List<String> = listOf(
        "Spam",
        "Nhạy cảm",
        "Quấy rối",
        "Sai vị trí",
        "Khác"
    )

    fun resolveConversationIdForUser(userId: String): String {
        return SampleData.conversations
            .firstOrNull { it.friend.id == userId }
            ?.id ?: "conv_${userId}"
    }
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/data/fake/FakePostStore.kt
package vn.nhom2.lokap.data.fake

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.domain.model.Post
import vn.nhom2.lokap.domain.model.PostSendState
import vn.nhom2.lokap.domain.model.Reaction

object FakePostStore {

    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _posts = MutableStateFlow(SampleData.posts)
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    fun addPendingPostAndSimulateSend(post: Post) {
        val pendingPost = post.copy(sendState = PostSendState.PENDING)
        _posts.update { current -> listOf(pendingPost) + current }
        storeScope.launch {
            delay(1_500L)
            _posts.update { current ->
                current.map { item ->
                    if (item.id == pendingPost.id) {
                        item.copy(sendState = PostSendState.SENT)
                    } else {
                        item
                    }
                }
            }
        }
    }

    fun retrySend(postId: String) {
        _posts.update { current ->
            current.map { item ->
                if (item.id == postId) item.copy(sendState = PostSendState.PENDING) else item
            }
        }
        storeScope.launch {
            delay(1_500L)
            _posts.update { current ->
                current.map { item ->
                    if (item.id == postId) item.copy(sendState = PostSendState.SENT) else item
                }
            }
        }
    }

    fun toggleReaction(postId: String, emoji: String) {
        _posts.update { current ->
            current.map { post ->
                if (post.id != postId) return@map post

                val previousEmoji = post.myReaction
                val nextMyReaction = if (previousEmoji == emoji) null else emoji
                val countsMap = post.reactions.associate { it.emoji to it.count }.toMutableMap()

                if (previousEmoji != null) {
                    val decremented = (countsMap[previousEmoji] ?: 1) - 1
                    if (decremented <= 0) {
                        countsMap.remove(previousEmoji)
                    } else {
                        countsMap[previousEmoji] = decremented
                    }
                }

                if (nextMyReaction != null) {
                    countsMap[nextMyReaction] = (countsMap[nextMyReaction] ?: 0) + 1
                }

                val updatedReactions = countsMap.entries.map { (k, v) -> Reaction(k, v) }
                post.copy(
                    reactions = updatedReactions,
                    myReaction = nextMyReaction
                )
            }
        }
    }

    fun removePost(postId: String) {
        _posts.update { current -> current.filterNot { it.id == postId } }
    }

    fun removePostsByAuthor(authorId: String) {
        _posts.update { current -> current.filterNot { it.author.id == authorId } }
    }
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/feed/FeedContract.kt
package vn.nhom2.lokap.feature.feed

import androidx.compose.runtime.Immutable
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.Post
import java.time.Instant

enum class FeedTab(val labelVi: String) {
    FRIENDS("Bạn bè"),
    EXPLORE("Khám phá")
}

@Immutable
data class FeedUiState(
    val selectedTab: FeedTab = FeedTab.FRIENDS,
    val friendPosts: List<Post> = emptyList(),
    val nearbyPosts: List<Post> = emptyList(),
    val landmarkPosts: List<Post> = emptyList(),
    val unreadNotificationsCount: Int = SampleData.notifications.count { !it.isRead },
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null,
    val currentProvinceName: String = "Đà Nẵng",
    val now: Instant = SampleData.NOW
)

sealed interface FeedAction {
    data class OnSelectTab(val tab: FeedTab) : FeedAction
    data object OnRefresh : FeedAction
    data object OnRetry : FeedAction
    data object OnSwipeDownToCamera : FeedAction
    data object OnNotificationsClick : FeedAction
    data class OnPostClick(val postId: String) : FeedAction
    data class OnLocationChipClick(val post: Post) : FeedAction
    data class OnAuthorClick(val userId: String) : FeedAction
    data class OnReplyClick(val post: Post) : FeedAction
    data class OnReact(val postId: String, val emoji: String) : FeedAction
    data class OnRetrySendPost(val postId: String) : FeedAction
    data class OnReportPost(val postId: String) : FeedAction
    data class OnBlockUser(val userId: String, val displayName: String) : FeedAction
}

sealed interface FeedEffect {
    data object NavigateBackToCamera : FeedEffect
    data object NavigateToNotifications : FeedEffect
    data class NavigateToPostDetail(val postId: String) : FeedEffect
    data class NavigateToRegionAlbum(val level: String, val code: String) : FeedEffect
    data class NavigateToProfile(val userId: String) : FeedEffect
    data class NavigateToChat(val conversationId: String) : FeedEffect
    data class ShowMessage(val message: String) : FeedEffect
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/feed/FeedViewModel.kt
package vn.nhom2.lokap.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.data.fake.FakePostStore
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.data.fake.SampleData2
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.Post
import vn.nhom2.lokap.domain.model.PostSendState
import vn.nhom2.lokap.domain.model.RegionLevel

class FeedViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private val _effects = Channel<FeedEffect>(Channel.BUFFERED)
    val effects: Flow<FeedEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            FakePostStore.posts.collect { allPosts ->
                updatePostsState(allPosts)
            }
        }
    }

    private fun updatePostsState(allPosts: List<Post>) {
        val now = SampleData.NOW
        val meId = SampleData.currentUser.id

        // Tab Bạn bè: Bài FRIENDS + DIRECT + bài của tôi vừa đăng, mới nhất trước
        val friendFeed = allPosts
            .filter { post ->
                post.audience == Audience.FRIENDS ||
                    post.audience == Audience.DIRECT ||
                    post.author.id == meId
            }
            .sortedByDescending { it.takenAt }

        // Tab Khám phá: chỉ lấy bài PUBLIC đã gửi thành công và KHÔNG đang hẹn giờ (publicVisibleAt <= now)
        val visiblePublicPosts = allPosts
            .filter { post ->
                post.audience == Audience.PUBLIC &&
                    post.sendState == PostSendState.SENT &&
                    (post.publicVisibleAt == null || !post.publicVisibleAt.isAfter(now))
            }
            .sortedByDescending { it.takenAt }

        val nearby = visiblePublicPosts.filter { post ->
            post.location.provinceCode == SampleData2.DEFAULT_PROVINCE_CODE
        }

        val landmarks = visiblePublicPosts.filter { post ->
            !post.location.landmarkId.isNullOrBlank()
        }

        _uiState.update { state ->
            state.copy(
                friendPosts = friendFeed,
                nearbyPosts = nearby,
                landmarkPosts = landmarks
            )
        }
    }

    fun onAction(action: FeedAction) {
        when (action) {
            is FeedAction.OnSelectTab -> {
                _uiState.update { it.copy(selectedTab = action.tab) }
            }

            FeedAction.OnRefresh -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
                    delay(600L)
                    updatePostsState(FakePostStore.posts.value)
                    _uiState.update { it.copy(isRefreshing = false) }
                }
            }

            FeedAction.OnRetry -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    delay(500L)
                    updatePostsState(FakePostStore.posts.value)
                    _uiState.update { it.copy(isLoading = false) }
                }
            }

            FeedAction.OnSwipeDownToCamera -> {
                sendEffect(FeedEffect.NavigateBackToCamera)
            }

            FeedAction.OnNotificationsClick -> {
                sendEffect(FeedEffect.NavigateToNotifications)
            }

            is FeedAction.OnPostClick -> {
                sendEffect(FeedEffect.NavigateToPostDetail(action.postId))
            }

            is FeedAction.OnLocationChipClick -> {
                val loc = action.post.location
                val (level, code) = when (loc.level) {
                    LocationLevel.PLACE, LocationLevel.WARD -> {
                        val wardCode = loc.wardCode
                        if (!wardCode.isNullOrBlank()) {
                            RegionLevel.WARD.name to wardCode
                        } else {
                            RegionLevel.PROVINCE.name to (loc.provinceCode ?: SampleData2.DEFAULT_PROVINCE_CODE)
                        }
                    }

                    LocationLevel.PROVINCE -> {
                        RegionLevel.PROVINCE.name to (loc.provinceCode ?: SampleData2.DEFAULT_PROVINCE_CODE)
                    }

                    LocationLevel.COUNTRY -> {
                        RegionLevel.PROVINCE.name to SampleData2.DEFAULT_PROVINCE_CODE
                    }
                }
                sendEffect(FeedEffect.NavigateToRegionAlbum(level = level, code = code))
            }

            is FeedAction.OnAuthorClick -> {
                sendEffect(FeedEffect.NavigateToProfile(action.userId))
            }

            is FeedAction.OnReplyClick -> {
                val convId = SampleData2.resolveConversationIdForUser(action.post.author.id)
                sendEffect(FeedEffect.NavigateToChat(convId))
            }

            is FeedAction.OnReact -> {
                FakePostStore.toggleReaction(action.postId, action.emoji)
            }

            is FeedAction.OnRetrySendPost -> {
                FakePostStore.retrySend(action.postId)
            }

            is FeedAction.OnReportPost -> {
                FakePostStore.removePost(action.postId)
                sendEffect(FeedEffect.ShowMessage("Đã gửi báo cáo và ẩn bài viết"))
            }

            is FeedAction.OnBlockUser -> {
                FakePostStore.removePostsByAuthor(action.userId)
                sendEffect(FeedEffect.ShowMessage("Đã chặn ${action.displayName}"))
            }
        }
    }

    private fun sendEffect(effect: FeedEffect) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/feed/FeedScreen.kt
package vn.nhom2.lokap.feature.feed

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
            onNavigateBackToCamera = { navController.popBackStack() },
            onNavigateToNotifications = { navController.navigate(Route.Notifications) },
            onNavigateToPostDetail = { id -> navController.navigate(Route.PostDetail(id)) },
            onNavigateToRegionAlbum = { level, code ->
                navController.navigate(Route.RegionAlbum(level, code))
            },
            onNavigateToProfile = { userId -> navController.navigate(Route.Profile(userId)) },
            onNavigateToChat = { convId -> navController.navigate(Route.Chat(convId)) }
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

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                FeedEffect.NavigateBackToCamera -> onNavigateBackToCamera()
                FeedEffect.NavigateToNotifications -> onNavigateToNotifications()
                is FeedEffect.NavigateToPostDetail -> onNavigateToPostDetail(effect.postId)
                is FeedEffect.NavigateToRegionAlbum -> onNavigateToRegionAlbum(effect.level, effect.code)
                is FeedEffect.NavigateToProfile -> onNavigateToProfile(effect.userId)
                is FeedEffect.NavigateToChat -> onNavigateToChat(effect.conversationId)
                is FeedEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
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
            onReplyClick = { onAction(FeedAction.OnReplyClick(post)) }
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
    onReplyClick: () -> Unit
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

            Row(
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

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/composer/ComposerContract.kt
package vn.nhom2.lokap.feature.composer

import androidx.compose.runtime.Immutable
import vn.nhom2.lokap.data.fake.SampleData2
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.Place
import vn.nhom2.lokap.domain.model.PublishDelay

enum class LocationAcquisitionState {
    READY,
    FETCHING,
    UNAVAILABLE
}

@Immutable
data class ComposerUiState(
    val imagePath: String = "",
    val caption: String = "",
    val audience: Audience = Audience.FRIENDS,
    val locationLevel: LocationLevel = LocationLevel.PROVINCE,
    val locationStatus: LocationAcquisitionState = LocationAcquisitionState.READY,
    val currentProvinceCode: String = SampleData2.DEFAULT_PROVINCE_CODE,
    val currentProvinceName: String = SampleData2.DEFAULT_PROVINCE_NAME,
    val currentWardCode: String = SampleData2.DEFAULT_WARD_CODE,
    val currentWardName: String = SampleData2.DEFAULT_WARD_NAME,
    val selectedPlace: Place? = null,
    val isPlaceSheetOpen: Boolean = false,
    val placeSearchQuery: String = "",
    val filteredPlaces: List<Place> = SampleData2.daNangPlaces,
    val publishDelay: PublishDelay = PublishDelay.NOW,
    val showDiscardDialog: Boolean = false,
    val isSubmitting: Boolean = false
) {
    val canSend: Boolean
        get() = !isSubmitting &&
            locationStatus != LocationAcquisitionState.FETCHING &&
            (locationLevel != LocationLevel.PLACE || selectedPlace != null)
}

sealed interface ComposerAction {
    data class OnCaptionChange(val text: String) : ComposerAction
    data class OnSelectAudience(val audience: Audience) : ComposerAction
    data class OnSelectLocationLevel(val level: LocationLevel) : ComposerAction
    data object OnOpenPlaceSheet : ComposerAction
    data object OnDismissPlaceSheet : ComposerAction
    data class OnPlaceSearchQueryChange(val query: String) : ComposerAction
    data class OnSelectPlace(val place: Place) : ComposerAction
    data class OnSelectPublishDelay(val delay: PublishDelay) : ComposerAction
    data object OnCancelClick : ComposerAction
    data object OnDismissDiscardDialog : ComposerAction
    data object OnConfirmDiscard : ComposerAction
    data object OnSendClick : ComposerAction
}

sealed interface ComposerEffect {
    data object NavigateBackToCamera : ComposerEffect
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/composer/ComposerViewModel.kt
package vn.nhom2.lokap.feature.composer

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
import vn.nhom2.lokap.data.fake.FakePostStore
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.data.fake.SampleData2
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.Post
import vn.nhom2.lokap.domain.model.PostLocation
import vn.nhom2.lokap.domain.model.PostSendState
import vn.nhom2.lokap.domain.model.PublishDelay
import java.time.Duration
import java.util.UUID

class ComposerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ComposerUiState())
    val uiState: StateFlow<ComposerUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ComposerEffect>(Channel.BUFFERED)
    val effects: Flow<ComposerEffect> = _effects.receiveAsFlow()

    fun initImagePath(path: String) {
        if (_uiState.value.imagePath.isEmpty()) {
            _uiState.update { it.copy(imagePath = path) }
        }
    }

    fun onAction(action: ComposerAction) {
        when (action) {
            is ComposerAction.OnCaptionChange -> {
                val trimmed = action.text.take(60)
                _uiState.update { it.copy(caption = trimmed) }
            }

            is ComposerAction.OnSelectAudience -> {
                if (action.audience == Audience.FRIENDS || action.audience == Audience.PUBLIC) {
                    _uiState.update { it.copy(audience = action.audience) }
                }
            }

            is ComposerAction.OnSelectLocationLevel -> {
                val current = _uiState.value
                if (current.locationStatus == LocationAcquisitionState.UNAVAILABLE &&
                    action.level != LocationLevel.COUNTRY
                ) {
                    return
                }
                val openSheet = action.level == LocationLevel.PLACE && current.selectedPlace == null
                _uiState.update {
                    it.copy(
                        locationLevel = action.level,
                        isPlaceSheetOpen = openSheet
                    )
                }
            }

            ComposerAction.OnOpenPlaceSheet -> {
                _uiState.update {
                    it.copy(
                        isPlaceSheetOpen = true,
                        placeSearchQuery = "",
                        filteredPlaces = SampleData2.daNangPlaces
                    )
                }
            }

            ComposerAction.OnDismissPlaceSheet -> {
                _uiState.update { it.copy(isPlaceSheetOpen = false) }
            }

            is ComposerAction.OnPlaceSearchQueryChange -> {
                val query = action.query
                val filtered = if (query.isBlank()) {
                    SampleData2.daNangPlaces
                } else {
                    SampleData2.daNangPlaces.filter { place ->
                        place.name.contains(query, ignoreCase = true) ||
                            place.address.contains(query, ignoreCase = true)
                    }
                }
                _uiState.update {
                    it.copy(
                        placeSearchQuery = query,
                        filteredPlaces = filtered
                    )
                }
            }

            is ComposerAction.OnSelectPlace -> {
                _uiState.update {
                    it.copy(
                        selectedPlace = action.place,
                        locationLevel = LocationLevel.PLACE,
                        isPlaceSheetOpen = false
                    )
                }
            }

            is ComposerAction.OnSelectPublishDelay -> {
                _uiState.update { it.copy(publishDelay = action.delay) }
            }

            ComposerAction.OnCancelClick -> {
                if (_uiState.value.caption.isNotBlank()) {
                    _uiState.update { it.copy(showDiscardDialog = true) }
                } else {
                    sendEffect(ComposerEffect.NavigateBackToCamera)
                }
            }

            ComposerAction.OnDismissDiscardDialog -> {
                _uiState.update { it.copy(showDiscardDialog = false) }
            }

            ComposerAction.OnConfirmDiscard -> {
                _uiState.update { it.copy(showDiscardDialog = false) }
                sendEffect(ComposerEffect.NavigateBackToCamera)
            }

            ComposerAction.OnSendClick -> {
                val state = _uiState.value
                if (!state.canSend) return

                _uiState.update { it.copy(isSubmitting = true) }

                val now = SampleData.NOW
                val visibleAt = if (state.audience == Audience.PUBLIC) {
                    now.plus(Duration.ofDays(state.publishDelay.days.toLong()))
                } else {
                    null
                }

                val postLocation = buildPostLocation(state)
                val newPost = Post(
                    id = "post_${UUID.randomUUID()}",
                    author = SampleData.currentUser,
                    imageUrl = state.imagePath.ifBlank {
                        "https://picsum.photos/seed/lokap_new/800/800"
                    },
                    caption = state.caption.trim().ifEmpty { null },
                    audience = state.audience,
                    location = postLocation,
                    takenAt = now,
                    publicVisibleAt = visibleAt,
                    reactions = emptyList(),
                    myReaction = null,
                    sendState = PostSendState.PENDING
                )

                FakePostStore.addPendingPostAndSimulateSend(newPost)
                sendEffect(ComposerEffect.NavigateBackToCamera)
            }
        }
    }

    private fun buildPostLocation(state: ComposerUiState): PostLocation {
        return when (state.locationLevel) {
            LocationLevel.PLACE -> {
                val place = state.selectedPlace
                PostLocation(
                    level = LocationLevel.PLACE,
                    provinceCode = state.currentProvinceCode,
                    provinceName = state.currentProvinceName,
                    wardCode = state.currentWardCode,
                    wardName = state.currentWardName,
                    placeName = place?.name,
                    landmarkId = place?.id?.let { SampleData2.placeToLandmarkMap[it] },
                    lat = place?.lat ?: 16.0678,
                    lng = place?.lng ?: 108.2208
                )
            }

            LocationLevel.WARD -> PostLocation(
                level = LocationLevel.WARD,
                provinceCode = state.currentProvinceCode,
                provinceName = state.currentProvinceName,
                wardCode = state.currentWardCode,
                wardName = state.currentWardName,
                placeName = null,
                landmarkId = null,
                lat = 16.0678,
                lng = 108.2208
            )

            LocationLevel.PROVINCE -> PostLocation(
                level = LocationLevel.PROVINCE,
                provinceCode = state.currentProvinceCode,
                provinceName = state.currentProvinceName,
                wardCode = null,
                wardName = null,
                placeName = null,
                landmarkId = null,
                lat = 16.0544,
                lng = 108.2022
            )

            LocationLevel.COUNTRY -> PostLocation(
                level = LocationLevel.COUNTRY,
                provinceCode = null,
                provinceName = null,
                wardCode = null,
                wardName = null,
                placeName = null,
                landmarkId = null,
                lat = null,
                lng = null
            )
        }
    }

    private fun sendEffect(effect: ComposerEffect) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/composer/ComposerScreen.kt
package vn.nhom2.lokap.feature.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import coil3.compose.AsyncImage
import vn.nhom2.lokap.core.designsystem.component.GhostButton
import vn.nhom2.lokap.core.designsystem.component.GlassChip
import vn.nhom2.lokap.core.designsystem.component.PinkButton
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.designsystem.theme.regionGradient
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.data.fake.SampleData2
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.Place
import vn.nhom2.lokap.domain.model.PublishDelay
import java.io.File

fun NavGraphBuilder.composerScreen(navController: NavController) {
    composable<Route.Composer> { backStackEntry ->
        val route = backStackEntry.toRoute<Route.Composer>()
        ComposerRoute(
            imagePath = route.imagePath,
            onNavigateBack = { navController.popBackStack() }
        )
    }
}

@Composable
fun ComposerRoute(
    imagePath: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ComposerViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(imagePath) {
        viewModel.initImagePath(imagePath)
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ComposerEffect.NavigateBackToCamera -> onNavigateBack()
            }
        }
    }

    ComposerScreen(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ComposerScreen(
    state: ComposerUiState,
    onAction: (ComposerAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes
    val isPreview = LocalInspectionMode.current

    val imageModel: Any = remember(state.imagePath) {
        if (state.imagePath.startsWith("http")) {
            state.imagePath
        } else {
            File(state.imagePath)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg)
    ) {
        // Thanh tiêu đề
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { onAction(ComposerAction.OnCancelClick) }) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Huỷ",
                    tint = colors.textPrimary
                )
            }
            Text(
                text = "Gửi khoảnh khắc",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        // Ảnh vuông bo 40dp (cameraFrame) + Ô nhập caption đè lên mép dưới ảnh
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shapes.cameraFrame)
                .background(regionGradient(state.currentProvinceCode))
                .border(1.5.dp, colors.outline, shapes.cameraFrame)
        ) {
            if (!isPreview && state.imagePath.isNotBlank()) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = "Ảnh vừa chụp",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Ô caption đè lên mép dưới ảnh (tối đa 60 ký tự + bộ đếm)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(spacing.md)
                    .clip(shapes.pill)
                    .background(colors.background.copy(alpha = 0.80f))
                    .border(1.dp, colors.outline, shapes.pill)
                    .padding(horizontal = spacing.lg, vertical = spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (state.caption.isEmpty()) {
                        Text(
                            text = "Thêm lời nhắn…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textTertiary
                        )
                    }
                    BasicTextField(
                        value = state.caption,
                        onValueChange = { onAction(ComposerAction.OnCaptionChange(it)) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = colors.textPrimary
                        ),
                        cursorBrush = SolidColor(colors.primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.width(spacing.sm))

                Text(
                    text = "${state.caption.length}/60",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (state.caption.length >= 60) colors.primary else colors.textSecondary
                )
            }
        }

        // 1. Chọn Đối tượng: Bạn bè / Public
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Text(
                text = "Đối tượng xem",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                GlassChip(
                    label = "Bạn bè",
                    selected = state.audience == Audience.FRIENDS,
                    onClick = { onAction(ComposerAction.OnSelectAudience(Audience.FRIENDS)) }
                )
                GlassChip(
                    label = "Public (Bản đồ)",
                    selected = state.audience == Audience.PUBLIC,
                    onClick = { onAction(ComposerAction.OnSelectAudience(Audience.PUBLIC)) }
                )
            }
        }

        // 2. Chọn Mức vị trí (4 chip LocationLevel)
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Mức hiển thị vị trí",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary
                )

                when (state.locationStatus) {
                    LocationAcquisitionState.FETCHING -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = colors.primary,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Đang lấy vị trí…",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.primary
                            )
                        }
                    }

                    LocationAcquisitionState.READY -> {
                        Text(
                            text = "${state.currentWardName}, ${state.currentProvinceName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.accentCyan
                        )
                    }

                    LocationAcquisitionState.UNAVAILABLE -> {
                        Text(
                            text = "Không có GPS",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.accentYellow
                        )
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                LocationLevel.entries.forEach { level ->
                    val isSelected = state.locationLevel == level
                    val chipText = when (level) {
                        LocationLevel.PLACE -> state.selectedPlace?.name ?: level.labelVi
                        LocationLevel.WARD -> "${level.labelVi} (${state.currentWardName})"
                        LocationLevel.PROVINCE -> "${level.labelVi} (${state.currentProvinceName})"
                        LocationLevel.COUNTRY -> level.labelVi
                    }

                    GlassChip(
                        label = chipText,
                        leadingIcon = Icons.Rounded.LocationOn,
                        selected = isSelected,
                        onClick = {
                            if (level == LocationLevel.PLACE && isSelected) {
                                onAction(ComposerAction.OnOpenPlaceSheet)
                            } else {
                                onAction(ComposerAction.OnSelectLocationLevel(level))
                            }
                        }
                    )
                }
            }

            if (state.locationStatus == LocationAcquisitionState.UNAVAILABLE) {
                Text(
                    text = "Chưa bật dịch vụ định vị nên bạn chỉ có thể gắn nhãn \"Việt Nam\".",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.accentYellow
                )
            } else if (state.locationLevel == LocationLevel.PLACE && state.selectedPlace == null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    Text(
                        text = "Vui lòng chọn một địa điểm cụ thể trước khi gửi.",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.error
                    )
                    Text(
                        text = "Chọn ngay",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary,
                        modifier = Modifier.clickable { onAction(ComposerAction.OnOpenPlaceSheet) }
                    )
                }
            }
        }

        // 3. Hẹn giờ lên bản đồ (chỉ hiện khi chọn Public)
        if (state.audience == Audience.PUBLIC) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hẹn giờ hiển thị trên bản đồ Public",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "Bạn bè vẫn thấy ngay",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primarySoft
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    PublishDelay.entries.forEach { delayOption ->
                        val delayLabel = when (delayOption) {
                            PublishDelay.NOW -> "Ngay"
                            PublishDelay.ONE_DAY -> "1 ngày"
                            PublishDelay.THREE_DAYS -> "3 ngày"
                            PublishDelay.SEVEN_DAYS -> "7 ngày"
                        }
                        GlassChip(
                            label = delayLabel,
                            selected = state.publishDelay == delayOption,
                            onClick = { onAction(ComposerAction.OnSelectPublishDelay(delayOption)) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.xs))

        // 4. Hàng nút Huỷ & Gửi
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            GhostButton(
                text = "Huỷ",
                onClick = { onAction(ComposerAction.OnCancelClick) },
                modifier = Modifier.weight(1f)
            )
            PinkButton(
                text = "Gửi",
                leadingIcon = Icons.AutoMirrored.Rounded.Send,
                enabled = state.canSend,
                loading = state.isSubmitting,
                onClick = { onAction(ComposerAction.OnSendClick) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    // BottomSheet tìm kiếm địa điểm quanh Đà Nẵng
    if (state.isPlaceSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { onAction(ComposerAction.OnDismissPlaceSheet) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = shapes.sheet,
            containerColor = colors.surface,
            contentColor = colors.textPrimary
        ) {
            PlacePickerSheetContent(
                query = state.placeSearchQuery,
                places = state.filteredPlaces,
                selectedPlaceId = state.selectedPlace?.id,
                onQueryChange = { onAction(ComposerAction.OnPlaceSearchQueryChange(it)) },
                onSelectPlace = { onAction(ComposerAction.OnSelectPlace(it)) }
            )
        }
    }

    // AlertDialog xác nhận huỷ nếu đã nhập caption
    if (state.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { onAction(ComposerAction.OnDismissDiscardDialog) },
            containerColor = colors.surfaceHigh,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textSecondary,
            title = {
                Text(
                    text = "Huỷ khoảnh khắc này?",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = "Bạn đã nhập lời nhắn cho ảnh. Nếu thoát bây giờ, ảnh và lời nhắn sẽ bị xoá.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { onAction(ComposerAction.OnConfirmDiscard) }) {
                    Text(
                        text = "Huỷ bỏ",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(ComposerAction.OnDismissDiscardDialog) }) {
                    Text(
                        text = "Tiếp tục sửa",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textPrimary
                    )
                }
            }
        )
    }
}

@Composable
private fun PlacePickerSheetContent(
    query: String,
    places: List<Place>,
    selectedPlaceId: String?,
    onQueryChange: (String) -> Unit,
    onSelectPlace: (Place) -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        Text(
            text = "Chọn địa điểm quanh Đà Nẵng",
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary
        )

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    text = "Tìm quán cà phê, bãi biển, trường đại học…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textTertiary
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = colors.textSecondary
                )
            },
            singleLine = true,
            shape = shapes.pill,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surfaceHigh,
                unfocusedContainerColor = colors.surfaceHigh,
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.outline,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                cursorColor = colors.primary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            items(
                items = places,
                key = { it.id }
            ) { place ->
                val isSelected = place.id == selectedPlaceId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.card)
                        .background(colors.surfaceHigh)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) colors.primary else colors.outline,
                            shape = shapes.card
                        )
                        .clickable { onSelectPlace(place) }
                        .padding(spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = if (isSelected) colors.primary else colors.textSecondary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = place.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = place.address,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "1. Composer - Bình thường (Public + Hẹn giờ)", showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun ComposerNormalPreview() {
    LokapTheme {
        ComposerScreen(
            state = ComposerUiState(
                caption = "Chiều hoàng hôn bên bờ sông Hàn ✨",
                audience = Audience.PUBLIC,
                locationLevel = LocationLevel.PLACE,
                selectedPlace = SampleData2.daNangPlaces.first(),
                publishDelay = PublishDelay.THREE_DAYS
            ),
            onAction = {}
        )
    }
}

@Preview(name = "2. Composer - Đang lấy vị trí", showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun ComposerFetchingLocationPreview() {
    LokapTheme {
        ComposerScreen(
            state = ComposerUiState(
                locationStatus = LocationAcquisitionState.FETCHING
            ),
            onAction = {}
        )
    }
}

@Preview(name = "3. Composer - Không có vị trí", showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun ComposerNoLocationPreview() {
    LokapTheme {
        ComposerScreen(
            state = ComposerUiState(
                locationStatus = LocationAcquisitionState.UNAVAILABLE,
                locationLevel = LocationLevel.COUNTRY
            ),
            onAction = {}
        )
    }
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/postdetail/PostDetailContract.kt
package vn.nhom2.lokap.feature.postdetail

import androidx.compose.runtime.Immutable
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.Post
import java.time.Instant

@Immutable
data class PostDetailUiState(
    val post: Post? = null,
    val isLoading: Boolean = false,
    val isNotFound: Boolean = false,
    val isReportSheetOpen: Boolean = false,
    val showBlockConfirmDialog: Boolean = false,
    val now: Instant = SampleData.NOW
)

sealed interface PostDetailAction {
    data object OnBackClick : PostDetailAction
    data class OnReact(val emoji: String) : PostDetailAction
    data object OnReplyClick : PostDetailAction
    data object OnAuthorClick : PostDetailAction
    data object OnLocationClick : PostDetailAction
    data object OnOpenReportSheet : PostDetailAction
    data object OnDismissReportSheet : PostDetailAction
    data class OnSubmitReport(val reason: String) : PostDetailAction
    data object OnOpenBlockDialog : PostDetailAction
    data object OnDismissBlockDialog : PostDetailAction
    data object OnConfirmBlock : PostDetailAction
}

sealed interface PostDetailEffect {
    data object NavigateBack : PostDetailEffect
    data class NavigateToChat(val conversationId: String) : PostDetailEffect
    data class NavigateToProfile(val userId: String) : PostDetailEffect
    data class NavigateToRegionAlbum(val level: String, val code: String) : PostDetailEffect
    data class ShowSnackbar(val message: String) : PostDetailEffect
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/postdetail/PostDetailViewModel.kt
package vn.nhom2.lokap.feature.postdetail

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
import vn.nhom2.lokap.data.fake.FakePostStore
import vn.nhom2.lokap.data.fake.SampleData2
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.RegionLevel

class PostDetailViewModel : ViewModel() {

    private var targetPostId: String? = null

    private val _uiState = MutableStateFlow(PostDetailUiState(isLoading = true))
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    private val _effects = Channel<PostDetailEffect>(Channel.BUFFERED)
    val effects: Flow<PostDetailEffect> = _effects.receiveAsFlow()

    fun loadPost(postId: String) {
        if (targetPostId == postId) return
        targetPostId = postId

        viewModelScope.launch {
            FakePostStore.posts.collect { list ->
                val found = list.firstOrNull { it.id == postId }
                _uiState.update { state ->
                    state.copy(
                        post = found,
                        isLoading = false,
                        isNotFound = (found == null)
                    )
                }
            }
        }
    }

    fun onAction(action: PostDetailAction) {
        val currentPost = _uiState.value.post

        when (action) {
            PostDetailAction.OnBackClick -> {
                sendEffect(PostDetailEffect.NavigateBack)
            }

            is PostDetailAction.OnReact -> {
                val id = currentPost?.id ?: return
                FakePostStore.toggleReaction(id, action.emoji)
            }

            PostDetailAction.OnReplyClick -> {
                val authorId = currentPost?.author?.id ?: return
                val convId = SampleData2.resolveConversationIdForUser(authorId)
                sendEffect(PostDetailEffect.NavigateToChat(convId))
            }

            PostDetailAction.OnAuthorClick -> {
                val authorId = currentPost?.author?.id ?: return
                sendEffect(PostDetailEffect.NavigateToProfile(authorId))
            }

            PostDetailAction.OnLocationClick -> {
                val loc = currentPost?.location ?: return
                val (level, code) = when (loc.level) {
                    LocationLevel.PLACE, LocationLevel.WARD -> {
                        val wardCode = loc.wardCode
                        if (!wardCode.isNullOrBlank()) {
                            RegionLevel.WARD.name to wardCode
                        } else {
                            RegionLevel.PROVINCE.name to (loc.provinceCode ?: SampleData2.DEFAULT_PROVINCE_CODE)
                        }
                    }

                    LocationLevel.PROVINCE -> {
                        RegionLevel.PROVINCE.name to (loc.provinceCode ?: SampleData2.DEFAULT_PROVINCE_CODE)
                    }

                    LocationLevel.COUNTRY -> {
                        RegionLevel.PROVINCE.name to SampleData2.DEFAULT_PROVINCE_CODE
                    }
                }
                sendEffect(PostDetailEffect.NavigateToRegionAlbum(level, code))
            }

            PostDetailAction.OnOpenReportSheet -> {
                _uiState.update { it.copy(isReportSheetOpen = true) }
            }

            PostDetailAction.OnDismissReportSheet -> {
                _uiState.update { it.copy(isReportSheetOpen = false) }
            }

            is PostDetailAction.OnSubmitReport -> {
                _uiState.update { it.copy(isReportSheetOpen = false) }
                sendEffect(PostDetailEffect.ShowSnackbar("Đã gửi báo cáo: ${action.reason}"))
            }

            PostDetailAction.OnOpenBlockDialog -> {
                _uiState.update { it.copy(showBlockConfirmDialog = true) }
            }

            PostDetailAction.OnDismissBlockDialog -> {
                _uiState.update { it.copy(showBlockConfirmDialog = false) }
            }

            PostDetailAction.OnConfirmBlock -> {
                val authorName = currentPost?.author?.displayName ?: "người dùng"
                _uiState.update { it.copy(showBlockConfirmDialog = false) }
                sendEffect(PostDetailEffect.ShowSnackbar("Đã chặn $authorName"))
            }
        }
    }

    private fun sendEffect(effect: PostDetailEffect) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/postdetail/PostDetailScreen.kt
package vn.nhom2.lokap.feature.postdetail

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
            onNavigateBack = { navController.popBackStack() },
            onNavigateToChat = { convId -> navController.navigate(Route.Chat(convId)) },
            onNavigateToProfile = { userId -> navController.navigate(Route.Profile(userId)) },
            onNavigateToRegionAlbum = { level, code ->
                navController.navigate(Route.RegionAlbum(level, code))
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
                    snackbarHostState.showSnackbar(effect.message)
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
```

### 1. Các dòng cần THAY trong `AppNavHost.kt`

Thêm 3 dòng import ở đầu file `AppNavHost.kt`:
```kotlin
import vn.nhom2.lokap.feature.composer.composerScreen
import vn.nhom2.lokap.feature.feed.feedScreen
import vn.nhom2.lokap.feature.postdetail.postDetailScreen
```

Trong thân `NavHost { ... }`, thay 3 khối `composable<Route.Feed>`, `composable<Route.Composer>` và `composable<Route.PostDetail>` cũ bằng 3 dòng:
```kotlin
        feedScreen(navController)

        composerScreen(navController)

        postDetailScreen(navController)
```

### 2. Dependency mới cần thêm
Không cần thêm dependency mới (sử dụng trực tiếp `VerticalPager` của `androidx.compose.foundation` và `PullToRefreshBox` / `ModalBottomSheet` của `androidx.compose.material3` đã có trong Compose BOM).