package vn.nhom2.lokap.feature.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.AddLocationAlt
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import vn.nhom2.lokap.domain.repository.DeviceLocation
import vn.nhom2.lokap.domain.repository.LandmarkRepository
import vn.nhom2.lokap.domain.repository.PostRepository
import vn.nhom2.lokap.domain.repository.SocialRepository
import vn.nhom2.lokap.feature.landmark.AddLandmarkSheet
import vn.nhom2.lokap.domain.repository.observeMapPosts
import vn.nhom2.lokap.domain.repository.LocationRepository
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlin.math.abs
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.core.designsystem.component.*
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.core.navigation.navigateSingleTop
import vn.nhom2.lokap.core.navigation.navigateToTab
import vn.nhom2.lokap.domain.model.*
enum class MapAudienceFilter(val label: String) {
    ALL("Tất cả"),
    FRIENDS("Bạn bè"),
    PUBLIC("Công khai")
}

data class RegionSheetPreview(
    val level: String,
    val code: String,
    val regionName: String,
    val photoUrls: List<String>,
    val postCount: Int,
    val visited: Boolean
)

data class MapUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val errorMessage: String? = null,
    val camera: MapCameraState = MapCameraState(
        centerLat = 16.0544,
        centerLng = 108.2022,
        zoom = 6.2f
    ),
    val audienceFilter: MapAudienceFilter = MapAudienceFilter.ALL,
    /** Chỉ dùng khi lọc Bạn bè: null = mọi bạn bè, có giá trị = chỉ ảnh của người bạn đó. */
    val selectedFriendId: String? = null,
    /** Bạn bè để chọn ở nút lọc; ai có nhiều ảnh trên bản đồ đứng trước. */
    val friends: List<User> = emptyList(),
    val visibleBubbles: List<MapBubble> = emptyList(),
    val landmarks: List<Landmark> = emptyList(),
    val isViewportEmpty: Boolean = false,
    val selectedRegionPreview: RegionSheetPreview? = null,
    /** Bảng "Thêm địa danh" đang mở. */
    val isAddLandmarkOpen: Boolean = false,
    /**
     * Vị trí của tôi sau khi bấm "Về vị trí của tôi": chỉ để vẽ chấm đỏ trên máy này,
     * không lưu, không gửi lên server (null = chưa xác định).
     */
    val myLocation: DeviceLocation? = null
)

sealed interface MapAction {
    data class OnCameraChanged(val camera: MapCameraState) : MapAction
    data class OnAudienceFilterChange(val filter: MapAudienceFilter) : MapAction
    /** Chọn một người bạn (null = tất cả bạn bè); tự chuyển sang lọc Bạn bè. */
    data class OnFriendFilterChange(val friendId: String?) : MapAction
    data class OnBubbleClick(val bubble: MapBubble) : MapAction
    data class OnLandmarkClick(val landmark: Landmark) : MapAction
    data object OnMyLocationClick : MapAction
    data object OnDismissSheet : MapAction
    data class OnOpenRegionAlbumClick(val level: String, val code: String) : MapAction
    data object OnRetry : MapAction
    data object OnAddLandmarkClick : MapAction
    data object OnDismissAddLandmark : MapAction
    /** Đã thêm địa danh (hoặc địa điểm đã là địa danh): đóng bảng và mở màn Địa danh. */
    data class OnLandmarkAdded(val landmarkId: String) : MapAction
}

sealed interface MapEffect {
    data class NavigateToRegionAlbum(val level: String, val code: String) : MapEffect
    data class NavigateToLandmark(val landmarkId: String) : MapEffect
    data class NavigateToPostDetail(val postId: String) : MapEffect
}

/** Bản đồ gom các bài có quyền xem từ repository. */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val location: LocationRepository,
    private val posts: PostRepository,
    private val social: SocialRepository,
    private val landmarks: LandmarkRepository
) : ViewModel() {


    private val _uiState = MutableStateFlow(MapUiState(isLoading = true))
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _effect = Channel<MapEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var cachedPosts: List<Post> = emptyList()
    /** Danh mục địa danh trên server (kể cả địa danh chưa có ảnh). */
    private var cachedLandmarks: List<Landmark> = emptyList()
    private var friendIds: Set<String> = emptySet()

    init {
        observePostsAndLoad()
    }

    private fun observePostsAndLoad() {
        viewModelScope.launch {
            combine(posts.observeMapPosts(), social.graph, landmarks.landmarks) { visible, graph, catalog ->
                Triple(visible, graph, catalog)
            }
                .collectLatest { (visible, graph, catalog) ->
                    cachedPosts = visible
                    cachedLandmarks = catalog
                    friendIds = graph.statuses.filterValues { it == FriendStatus.FRIENDS }.keys
                    updateFriendOptions(graph.users)
                    recalculateMapData(isLoading = false)
                }
        }
        // Mở màn: dùng cache có hạn cho feed và danh mục địa danh
        refreshPosts(force = false)
    }

    /** Danh sách bạn cho nút chọn bạn bè; bỏ chọn nếu người đó không còn là bạn. */
    private fun updateFriendOptions(users: Map<String, User>) {
        val postCounts = cachedPosts.groupingBy { it.author.id }.eachCount()
        val friends = friendIds
            .mapNotNull { id -> users[id] ?: cachedPosts.firstOrNull { it.author.id == id }?.author }
            .sortedWith(compareByDescending<User> { postCounts[it.id] ?: 0 }.thenBy { it.displayName.lowercase() })
        _uiState.update { state ->
            state.copy(
                friends = friends,
                selectedFriendId = state.selectedFriendId?.takeIf { it in friendIds }
            )
        }
    }

    private fun refreshPosts(force: Boolean = true) {
        // Danh mục địa danh lỗi (mất mạng) thì vẫn còn địa danh suy ra từ bài đã tải
        viewModelScope.launch { landmarks.refresh(force) }
        viewModelScope.launch {
            posts.refreshFeeds(force).onSuccess {
                _uiState.update { it.copy(errorMessage = null) }
                recalculateMapData(isLoading = false)
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
            }
        }
    }

    fun onAction(action: MapAction) {
        when (action) {
            is MapAction.OnCameraChanged -> {
                _uiState.update { it.copy(camera = action.camera) }
                recalculateMapData()
            }

            is MapAction.OnAudienceFilterChange -> {
                // Đổi nhóm đối tượng thì bỏ chọn bạn cụ thể
                _uiState.update {
                    if (it.audienceFilter == action.filter) it
                    else it.copy(audienceFilter = action.filter, selectedFriendId = null)
                }
                recalculateMapData()
            }

            is MapAction.OnFriendFilterChange -> {
                _uiState.update {
                    it.copy(
                        audienceFilter = MapAudienceFilter.FRIENDS,
                        selectedFriendId = action.friendId?.takeIf { id -> id in friendIds }
                    )
                }
                recalculateMapData()
            }

            is MapAction.OnMyLocationClick -> {
                viewModelScope.launch {
                    // Chưa cấp quyền / tắt định vị thì giữ nguyên bản đồ
                    val here = location.currentLocation().getOrNull() ?: return@launch
                    _uiState.update {
                        it.copy(
                            myLocation = here,
                            camera = MapCameraState(centerLat = here.lat, centerLng = here.lng, zoom = 13f)
                        )
                    }
                    recalculateMapData()
                }
            }

            is MapAction.OnBubbleClick -> handleBubbleClick(action.bubble)

            is MapAction.OnLandmarkClick -> {
                viewModelScope.launch {
                    _effect.send(MapEffect.NavigateToLandmark(action.landmark.id))
                }
            }

            is MapAction.OnDismissSheet -> {
                _uiState.update { it.copy(selectedRegionPreview = null) }
            }

            is MapAction.OnOpenRegionAlbumClick -> {
                _uiState.update { it.copy(selectedRegionPreview = null) }
                viewModelScope.launch {
                    _effect.send(MapEffect.NavigateToRegionAlbum(action.level, action.code))
                }
            }

            is MapAction.OnRetry -> {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                refreshPosts()
            }

            MapAction.OnAddLandmarkClick -> _uiState.update { it.copy(isAddLandmarkOpen = true) }

            MapAction.OnDismissAddLandmark -> _uiState.update { it.copy(isAddLandmarkOpen = false) }

            is MapAction.OnLandmarkAdded -> {
                _uiState.update { it.copy(isAddLandmarkOpen = false) }
                viewModelScope.launch { _effect.send(MapEffect.NavigateToLandmark(action.landmarkId)) }
            }
        }
    }

    private fun handleBubbleClick(bubble: MapBubble) {
        val currentZoom = _uiState.value.camera.zoom
        when (bubble.type) {
            MapBubbleType.PROVINCE -> {
                if (currentZoom < 8f) {

                    _uiState.update {
                        it.copy(
                            camera = MapCameraState(
                                centerLat = bubble.lat,
                                centerLng = bubble.lng,
                                zoom = 9f
                            )
                        )
                    }
                    recalculateMapData()
                } else {
                    openRegionPreview(level = "PROVINCE", bubble = bubble)
                }
            }

            // "Khắp <tỉnh>": phóng to thì bong bóng ẩn (từ PROVINCE_WIDE_MAX_ZOOM), nên mở album tỉnh luôn
            MapBubbleType.PROVINCE_WIDE -> openRegionPreview(level = "PROVINCE", bubble = bubble)

            MapBubbleType.WARD -> {
                if (currentZoom < 10.5f) {
                    _uiState.update {
                        it.copy(
                            camera = MapCameraState(
                                centerLat = bubble.lat,
                                centerLng = bubble.lng,
                                zoom = 11.2f
                            )
                        )
                    }
                    recalculateMapData()
                } else {
                    openRegionPreview(level = "WARD", bubble = bubble)
                }
            }

            MapBubbleType.PLACE -> {

                val postId = findPostIdForPlaceBubble(bubble) ?: return
                viewModelScope.launch {
                    _effect.send(MapEffect.NavigateToPostDetail(postId))
                }
            }

            MapBubbleType.LANDMARK -> {
                val landmarkId = bubble.code ?: bubble.id
                viewModelScope.launch {
                    _effect.send(MapEffect.NavigateToLandmark(landmarkId))
                }
            }
        }
    }

    private fun openRegionPreview(level: String, bubble: MapBubble) {
        val code = bubble.code ?: bubble.id
        val matchingPosts = filterPostsForRegion(level = level, code = code)
        val photoUrls = matchingPosts.map { it.imageUrl }
            .ifEmpty { listOfNotNull(bubble.coverImageUrl) }
            .take(4)

        val preview = RegionSheetPreview(
            level = level,
            code = code,
            regionName = bubble.label.removePrefix("Khắp ").trim(),
            photoUrls = photoUrls,
            postCount = bubble.postCount,
            visited = bubble.hasFriendPosts || matchingPosts.isNotEmpty()
        )
        _uiState.update { it.copy(selectedRegionPreview = preview) }
    }

    private fun filterPostsForRegion(level: String, code: String): List<Post> {

        val validPosts = filteredPosts()
        return validPosts.filter { post ->
            if (level == "PROVINCE") {
                post.location.provinceCode == code
            } else {
                post.location.wardCode == code
            }
        }
    }

    private fun findPostIdForPlaceBubble(bubble: MapBubble): String? {
        val validPosts = filteredPosts()
        validPosts.firstOrNull { it.id == bubble.code || it.id == bubble.id }?.let { return it.id }
        validPosts.firstOrNull {
            it.location.placeName?.equals(bubble.label, ignoreCase = true) == true
        }?.let { return it.id }
        return null
    }

    private fun recalculateMapData(isLoading: Boolean = _uiState.value.isLoading) {
        val state = _uiState.value
        val zoom = state.camera.zoom

        val countablePosts = filteredPosts()
        val filteredBubbles = mapBubblesFromPosts(countablePosts, zoom, friendIds)
        // Lọc Bạn bè: chỉ địa danh có ảnh của bạn; còn lại hiện cả địa danh chưa có ảnh (mời người đầu tiên)
        val catalog = if (state.audienceFilter == MapAudienceFilter.FRIENDS) emptyList() else cachedLandmarks
        val countedLandmarks = landmarksForMap(catalog, countablePosts, posts.now())

        val latSpan = 12.0 / Math.pow(2.0, (zoom - 5f).toDouble())
        val lngSpan = 10.0 / Math.pow(2.0, (zoom - 5f).toDouble())

        val bubblesInViewport = filteredBubbles.any { b ->
            abs(b.lat - state.camera.centerLat) <= latSpan &&
                abs(b.lng - state.camera.centerLng) <= lngSpan
        }
        val landmarksInViewport = zoom >= 8f && countedLandmarks.any { lm ->
            abs(lm.lat - state.camera.centerLat) <= latSpan &&
                abs(lm.lng - state.camera.centerLng) <= lngSpan
        }

        _uiState.update {
            it.copy(
                isLoading = isLoading,
                visibleBubbles = filteredBubbles,
                landmarks = countedLandmarks,
                isViewportEmpty = !isLoading && (!bubblesInViewport && !landmarksInViewport)
            )
        }
    }

    private fun filteredPosts(): List<Post> {
        val state = _uiState.value
        return cachedPosts.filter { post -> matchesAudienceFilter(post, state.audienceFilter, state.selectedFriendId) }
    }

    private fun matchesAudienceFilter(post: Post, filter: MapAudienceFilter, friendId: String?): Boolean {
        return when (filter) {
            MapAudienceFilter.ALL -> true
            MapAudienceFilter.FRIENDS -> post.author.id in friendIds &&
                (friendId == null || post.author.id == friendId)
            MapAudienceFilter.PUBLIC -> post.audience == Audience.PUBLIC &&
                (post.publicVisibleAt == null || !post.publicVisibleAt.isAfter(posts.now()))
        }
    }
}

fun NavGraphBuilder.mapScreen(navController: NavController) {
    composable<Route.Map> {
        MapRoute(
            onNavigateToRegionAlbum = { level, code ->
                navController.navigateSingleTop(Route.RegionAlbum(level = level, code = code))
            },
            onNavigateToLandmark = { landmarkId ->
                navController.navigateSingleTop(Route.Landmark(id = landmarkId))
            },
            onNavigateToPostDetail = { postId ->
                navController.navigateSingleTop(Route.PostDetail(id = postId))
            },
            onNavigateToTab = { tab ->
                when (tab) {
                    MainTab.FEED -> navController.navigateToTab(Route.Feed)
                    MainTab.MAP -> Unit
                    MainTab.CAMERA -> navController.navigateToTab(Route.Camera)
                    MainTab.INBOX -> navController.navigateToTab(Route.Inbox)
                    MainTab.PROFILE -> navController.navigateToTab(Route.Profile())
                }
            }
        )
    }
}

@Composable
fun MapRoute(
    onNavigateToRegionAlbum: (level: String, code: String) -> Unit,
    onNavigateToLandmark: (landmarkId: String) -> Unit,
    onNavigateToPostDetail: (postId: String) -> Unit,
    onNavigateToTab: (MainTab) -> Unit = {},
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is MapEffect.NavigateToRegionAlbum -> onNavigateToRegionAlbum(effect.level, effect.code)
                is MapEffect.NavigateToLandmark -> onNavigateToLandmark(effect.landmarkId)
                is MapEffect.NavigateToPostDetail -> onNavigateToPostDetail(effect.postId)
            }
        }
    }

    MapScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onNavigateToTab = onNavigateToTab
    )
}

private const val ALL_FRIENDS_LABEL = "Tất cả bạn bè"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    uiState: MapUiState,
    onAction: (MapAction) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToTab: (MainTab) -> Unit = {}
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    var headerHeightPx by remember { mutableIntStateOf(0) }
    // Đáy bản đồ bị thanh tab của app + thanh điều hướng hệ thống che: tâm bản đồ nằm giữa phần còn thấy
    val bottomBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
        LokapBottomNavBarHeight

    // Tâm hai nút kéo trong toạ độ của Box gốc: vòng lựa chọn vẽ quanh các điểm này
    var rootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var rootHeightPx by remember { mutableIntStateOf(0) }
    var audienceAnchor by remember { mutableStateOf<Offset?>(null) }
    var friendAnchor by remember { mutableStateOf<Offset?>(null) }
    fun centerInRoot(coords: LayoutCoordinates): Offset? {
        val root = rootCoords ?: return null
        if (!root.isAttached || !coords.isAttached) return null
        return root.localPositionOf(coords, Offset(coords.size.width / 2f, coords.size.height / 2f))
    }

    // Vòng đang mở; giữ lại sau khi thả tay để chạy hiệu ứng thu vào
    var ringPicker by remember { mutableStateOf(MapPicker.AUDIENCE) }
    var ringOpen by remember { mutableStateOf(false) }
    var hoveredIndex by remember { mutableStateOf<Int?>(null) }
    // Chạm nhanh để đổi lựa chọn: tên lựa chọn mới hiện cạnh nút một lúc
    var hint by remember { mutableStateOf<Pair<MapPicker, String>?>(null) }
    var hintTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(hintTick) {
        if (hint != null) {
            delay(1200)
            hint = null
        }
    }

    val audienceOptions = remember {
        MapAudienceFilter.entries.map { MapPickerOption(label = it.label, icon = it.icon) }
    }
    val friendOptions = remember(uiState.friends) {
        listOf(MapPickerOption(label = ALL_FRIENDS_LABEL, icon = Icons.Rounded.Groups)) +
            uiState.friends.map { MapPickerOption(label = it.displayName, user = it) }
    }
    val audienceIndex = uiState.audienceFilter.ordinal
    val selectedFriend = uiState.friends.firstOrNull { it.id == uiState.selectedFriendId }
    val friendIndex = selectedFriend?.let { uiState.friends.indexOf(it) + 1 } ?: 0
    val showFriendPicker = uiState.audienceFilter == MapAudienceFilter.FRIENDS && uiState.friends.isNotEmpty()

    // Vòng không đè lên logo (chừa chỗ cho nhãn) và không lọt xuống dưới thanh tab
    val ringItemPx = with(density) { MapPickerRing.ItemSize.toPx() }
    val firstRadiusPx = with(density) { MapPickerRing.FirstRadius.toPx() }
    val ringGapPx = with(density) { MapPickerRing.RingGap.toPx() }
    val itemSpacingPx = with(density) { MapPickerRing.ItemSpacing.toPx() }
    val activationPx = with(density) { MapPickerRing.ActivationRadius.toPx() }
    val topLimitPx = headerHeightPx + with(density) { MapPickerRing.LabelSpace.toPx() } + ringItemPx * 0.65f
    val bottomLimitPx = rootHeightPx - with(density) { (bottomBarHeight + 8.dp).toPx() } - ringItemPx * 0.65f
    fun ringPositions(count: Int, anchor: Offset?): List<Offset> =
        if (anchor == null) emptyList()
        else MapPickerRing.layout(
            count = count,
            firstRadiusPx = firstRadiusPx,
            ringGapPx = ringGapPx,
            spacingPx = itemSpacingPx,
            maxUpPx = anchor.y - topLimitPx,
            maxDownPx = bottomLimitPx - anchor.y
        )
    val audiencePositions = remember(audienceAnchor, topLimitPx, bottomLimitPx, density) {
        ringPositions(audienceOptions.size, audienceAnchor)
    }
    val friendPositions = remember(friendOptions.size, friendAnchor, topLimitPx, bottomLimitPx, density) {
        ringPositions(friendOptions.size, friendAnchor)
    }

    fun openRing(picker: MapPicker) {
        ringPicker = picker
        hoveredIndex = null
        hint = null
        ringOpen = true
    }

    fun dragRing(finger: Offset, positions: List<Offset>) {
        val index = MapPickerRing.indexAt(finger, positions, activationPx, hitRadiusPx = itemSpacingPx * 0.75f)
        if (index != hoveredIndex) {
            hoveredIndex = index
            if (index != null) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // Thả tay ngoài các lựa chọn thì giữ nguyên lựa chọn cũ
    fun releaseRing(pick: (Int) -> Unit) {
        hoveredIndex?.let(pick)
        hoveredIndex = null
        ringOpen = false
    }

    fun pickAudience(index: Int) =
        onAction(MapAction.OnAudienceFilterChange(MapAudienceFilter.entries[index]))

    fun pickFriend(index: Int) =
        onAction(MapAction.OnFriendFilterChange(uiState.friends.getOrNull(index - 1)?.id))

    // Đang kéo một nút: ẩn các nút còn lại để vòng có chỗ (giống Camera)
    val locationAlpha by animateFloatAsState(if (ringOpen) 0f else 1f, tween(160), label = "map_location_alpha")
    val audienceAlpha by animateFloatAsState(
        if (ringOpen && ringPicker != MapPicker.AUDIENCE) 0f else 1f, tween(160), label = "map_audience_alpha"
    )
    val friendAlpha by animateFloatAsState(
        if (ringOpen && ringPicker != MapPicker.FRIEND) 0f else 1f, tween(160), label = "map_friend_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .onGloballyPositioned {
                rootCoords = it
                rootHeightPx = it.size.height
            }
    ) {
        when {
            uiState.errorMessage != null -> {
                ErrorState(
                    message = uiState.errorMessage,
                    onRetry = { onAction(MapAction.OnRetry) },
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .padding(spacing.lg)
                )
            }

            uiState.isLoading -> {
                MapLoadingSkeleton(
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                LokapMap(
                    bubbles = uiState.visibleBubbles,
                    landmarks = uiState.landmarks,
                    myLocation = uiState.myLocation,
                    camera = uiState.camera,
                    onCameraChange = { onAction(MapAction.OnCameraChanged(it)) },
                    onBubbleClick = { onAction(MapAction.OnBubbleClick(it)) },
                    onLandmarkClick = { onAction(MapAction.OnLandmarkClick(it)) },
                    modifier = Modifier.fillMaxSize(),
                    topInset = with(density) { headerHeightPx.toDp() },
                    bottomInset = bottomBarHeight
                )
            }
        }

        // Góc trên bên trái chỉ còn logo. Chỉ đệm cạnh trên + hai bên: systemBarsPadding() cộng cả
        // chiều cao thanh điều hướng vào header nên tâm bản đồ bị đẩy xuống.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { headerHeightPx = it.height }
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(horizontal = spacing.lg, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = CircleShape,
                    color = colors.surface.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.22f)),
                    shadowElevation = 4.dp
                ) {
                    LokapLogo(
                        size = 30.dp,
                        showText = true,
                        modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 6.dp, bottom = 6.dp)
                    )
                }
                if (uiState.errorMessage == null) {
                    MapControlButton(onClick = { onAction(MapAction.OnAddLandmarkClick) }) {
                        Icon(
                            imageVector = Icons.Rounded.AddLocationAlt,
                            contentDescription = "Thêm địa danh",
                            tint = colors.accentYellow,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            if (uiState.isOffline) {
                OfflineBanner(modifier = Modifier.fillMaxWidth())
            }
        }

        AnimatedVisibility(
            visible = !uiState.isLoading && uiState.errorMessage == null && uiState.isViewportEmpty,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
                .padding(bottom = 76.dp)
        ) {
            GlassChip(
                label = "Chưa có bài ở khu vực này",
                leadingIcon = Icons.Default.Refresh,
                selected = false,
                onClick = { onAction(MapAction.OnMyLocationClick) }
            )
        }

        if (uiState.errorMessage == null) {
            MapPickerScrim(open = ringOpen)

            // Cạnh phải, ở khoảng 2/3 chiều cao màn hình: về vị trí của tôi · lọc đối tượng ·
            // lọc bạn bè (chỉ khi đang lọc Bạn bè)
            Column(
                modifier = Modifier
                    .placeAtTwoThirdsEnd()
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                    .padding(end = spacing.lg),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(MapControlGap)
            ) {
                MapControlButton(
                    onClick = { onAction(MapAction.OnMyLocationClick) },
                    modifier = Modifier.graphicsLayer { alpha = locationAlpha }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MyLocation,
                        contentDescription = "Về vị trí của tôi",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                MapPickerRow(hint = hint?.takeIf { it.first == MapPicker.AUDIENCE }?.second) {
                    val filtered = uiState.audienceFilter != MapAudienceFilter.ALL
                    MapPickerButton(
                        contentDescription = "Lọc ảnh trên bản đồ",
                        stateDescription = uiState.audienceFilter.label,
                        highlighted = filtered,
                        onTap = {
                            val next = (audienceIndex + 1) % audienceOptions.size
                            pickAudience(next)
                            hint = MapPicker.AUDIENCE to audienceOptions[next].label
                            hintTick++
                        },
                        onOpen = { openRing(MapPicker.AUDIENCE) },
                        onDrag = { dragRing(it, audiencePositions) },
                        onRelease = { releaseRing { index -> pickAudience(index) } },
                        modifier = Modifier
                            .onGloballyPositioned { audienceAnchor = centerInRoot(it) }
                            .graphicsLayer { alpha = audienceAlpha }
                    ) {
                        Icon(
                            imageVector = uiState.audienceFilter.icon,
                            contentDescription = null,
                            tint = if (filtered) colors.primary else colors.textPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Luôn giữ chỗ cho nút thứ ba để hai nút trên không xê dịch khi nút này hiện / ẩn
                MapPickerRow(hint = hint?.takeIf { it.first == MapPicker.FRIEND }?.second) {
                    Box(modifier = Modifier.size(MapPickerRing.ButtonSize)) {
                        // Gọi bản không phạm vi: bên ngoài còn ColumnScope nên phải ghi rõ
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showFriendPicker,
                            enter = fadeIn() + scaleIn(initialScale = 0.6f),
                            exit = fadeOut() + scaleOut(targetScale = 0.6f)
                        ) {
                            MapPickerButton(
                                contentDescription = "Chọn bạn bè",
                                stateDescription = selectedFriend?.displayName ?: ALL_FRIENDS_LABEL,
                                highlighted = selectedFriend != null,
                                onTap = {
                                    val next = (friendIndex + 1) % friendOptions.size
                                    pickFriend(next)
                                    hint = MapPicker.FRIEND to friendOptions[next].label
                                    hintTick++
                                },
                                onOpen = { openRing(MapPicker.FRIEND) },
                                onDrag = { dragRing(it, friendPositions) },
                                onRelease = { releaseRing { index -> pickFriend(index) } },
                                modifier = Modifier
                                    .onGloballyPositioned { friendAnchor = centerInRoot(it) }
                                    .graphicsLayer { alpha = friendAlpha }
                            ) {
                                if (selectedFriend != null) {
                                    AvatarRing(
                                        avatarUrl = selectedFriend.avatarUrl,
                                        displayName = selectedFriend.displayName,
                                        size = 44.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Groups,
                                        contentDescription = null,
                                        tint = colors.textPrimary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val isAudienceRing = ringPicker == MapPicker.AUDIENCE
            MapPickerRingOverlay(
                open = ringOpen,
                anchor = if (isAudienceRing) audienceAnchor else friendAnchor,
                positions = if (isAudienceRing) audiencePositions else friendPositions,
                options = if (isAudienceRing) audienceOptions else friendOptions,
                selectedIndex = if (isAudienceRing) audienceIndex else friendIndex,
                hoveredIndex = hoveredIndex
            )
        }

        if (uiState.isAddLandmarkOpen) {
            AddLandmarkSheet(
                onDismiss = { onAction(MapAction.OnDismissAddLandmark) },
                onOpenLandmark = { onAction(MapAction.OnLandmarkAdded(it)) }
            )
        }

        val regionPreview = uiState.selectedRegionPreview
        if (regionPreview != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { onAction(MapAction.OnDismissSheet) },
                sheetState = sheetState,
                containerColor = colors.surfaceHigh,
                contentColor = colors.textPrimary,
                shape = shapes.sheet
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.lg)
                        .padding(bottom = spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    RegionCard(
                        code = regionPreview.code,
                        regionName = regionPreview.regionName,
                        photoUrls = regionPreview.photoUrls,
                        postCount = regionPreview.postCount,
                        visited = regionPreview.visited,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onAction(
                                MapAction.OnOpenRegionAlbumClick(
                                    level = regionPreview.level,
                                    code = regionPreview.code
                                )
                            )
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        GhostButton(
                            text = "Đóng",
                            onClick = { onAction(MapAction.OnDismissSheet) },
                            modifier = Modifier.weight(1f)
                        )
                        PinkButton(
                            text = "Xem album",
                            onClick = {
                                onAction(
                                    MapAction.OnOpenRegionAlbumClick(
                                        level = regionPreview.level,
                                        code = regionPreview.code
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

    }
}

@Composable
private fun MapLoadingSkeleton(modifier: Modifier = Modifier) {
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes

    Box(
        modifier = modifier
            .systemBarsPadding()
            .padding(spacing.lg)
    ) {
        ShimmerBox(
            modifier = Modifier
                .size(110.dp)
                .align(Alignment.Center),
            shape = shapes.pill
        )
        ShimmerBox(
            modifier = Modifier
                .size(84.dp)
                .align(Alignment.TopEnd)
                .padding(top = 120.dp),
            shape = shapes.pill
        )
        ShimmerBox(
            modifier = Modifier
                .size(92.dp)
                .align(Alignment.BottomStart)
                .padding(bottom = 96.dp),
            shape = shapes.pill
        )
    }
}

/**
 * Đặt cụm nút sát cạnh phải sao cho khoảng giữa nút vị trí và nút lọc nằm ở 2/3 chiều cao khung chứa.
 * Phần tử chiếm cả khung nhưng không nhận chạm (chỉ các nút bên trong nhận), nên bản đồ vẫn kéo được.
 */
private fun Modifier.placeAtTwoThirdsEnd(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    val pairCenter = (MapPickerRing.ButtonSize + MapControlGap / 2).roundToPx()
    val maxY = (constraints.maxHeight - placeable.height).coerceAtLeast(0)
    val y = (constraints.maxHeight * 2 / 3 - pairCenter).coerceIn(0, maxY)
    layout(constraints.maxWidth, constraints.maxHeight) {
        placeable.place(constraints.maxWidth - placeable.width, y)
    }
}

private val MapControlGap = 12.dp

/** Một nút kéo + nhãn ngắn hiện bên trái nút sau khi chạm đổi lựa chọn. */
@Composable
private fun MapPickerRow(hint: String?, content: @Composable () -> Unit) {
    // Giữ chữ cũ trong lúc nhãn đang mờ dần
    val lastHint = remember { arrayOf("") }
    if (hint != null) lastHint[0] = hint
    Row(verticalAlignment = Alignment.CenterVertically) {
        AnimatedVisibility(
            visible = hint != null,
            enter = fadeIn() + slideInHorizontally { it / 3 },
            exit = fadeOut()
        ) {
            MapPickerHintPill(
                text = lastHint[0],
                modifier = Modifier
                    .padding(end = 10.dp)
                    .widthIn(max = 200.dp)
            )
        }
        content()
    }
}





@Preview(name = "Map - Vùng trống & Offline", showBackground = true)
@Composable
private fun MapScreenEmptyOfflinePreview() {
    LokapTheme {
        MapScreen(
            uiState = MapUiState(
                isLoading = false,
                isOffline = true,
                isViewportEmpty = true,
                visibleBubbles = emptyList()
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Map - Đang tải", showBackground = true)
@Composable
private fun MapScreenLoadingPreview() {
    LokapTheme {
        MapScreen(
            uiState = MapUiState(isLoading = true),
            onAction = {}
        )
    }
}

@Preview(name = "Map - Lỗi", showBackground = true)
@Composable
private fun MapScreenErrorPreview() {
    LokapTheme {
        MapScreen(
            uiState = MapUiState(
                isLoading = false,
                errorMessage = "Không tải được"
            ),
            onAction = {}
        )
    }
}
