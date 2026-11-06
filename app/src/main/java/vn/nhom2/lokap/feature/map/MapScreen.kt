package vn.nhom2.lokap.feature.map

import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.getValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import vn.nhom2.lokap.core.navigation.navigateSingleTop
import vn.nhom2.lokap.core.navigation.popBackStackSafely
import androidx.navigation.compose.composable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.core.designsystem.component.ErrorState
import vn.nhom2.lokap.core.designsystem.component.GhostButton
import vn.nhom2.lokap.core.designsystem.component.GlassChip
import vn.nhom2.lokap.core.designsystem.component.OfflineBanner
import vn.nhom2.lokap.core.designsystem.component.PinkButton
import vn.nhom2.lokap.core.designsystem.component.RegionCard
import vn.nhom2.lokap.core.designsystem.component.ShimmerBox
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.data.fake.FakePostStore
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.data.fake.SampleMapData
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.Landmark
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.MapBubble
import vn.nhom2.lokap.domain.model.MapBubbleType
import vn.nhom2.lokap.domain.model.Post
import vn.nhom2.lokap.domain.model.PostSendState
import java.time.Duration
import kotlin.math.abs

enum class MapAudienceFilter(val label: String) {
    ALL("Tất cả"),
    FRIENDS("Bạn bè"),
    PUBLIC("Public")
}

enum class MapTimeFilter(val label: String, val days: Long?) {
    DAYS_7("7 ngày", 7L),
    DAYS_30("30 ngày", 30L),
    ALL("Tất cả", null)
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
    val timeFilter: MapTimeFilter = MapTimeFilter.ALL,
    val visibleBubbles: List<MapBubble> = emptyList(),
    val landmarks: List<Landmark> = emptyList(),
    val isViewportEmpty: Boolean = false,
    val selectedRegionPreview: RegionSheetPreview? = null
)

sealed interface MapAction {
    data class OnCameraChanged(val camera: MapCameraState) : MapAction
    data class OnAudienceFilterChange(val filter: MapAudienceFilter) : MapAction
    data class OnTimeFilterChange(val filter: MapTimeFilter) : MapAction
    data class OnBubbleClick(val bubble: MapBubble) : MapAction
    data class OnLandmarkClick(val landmark: Landmark) : MapAction
    data object OnMyLocationClick : MapAction
    data object OnDismissSheet : MapAction
    data class OnOpenRegionAlbumClick(val level: String, val code: String) : MapAction
    data object OnRetry : MapAction
    data object OnBackClick : MapAction
}

sealed interface MapEffect {
    data object NavigateBack : MapEffect
    data class NavigateToRegionAlbum(val level: String, val code: String) : MapEffect
    data class NavigateToLandmark(val landmarkId: String) : MapEffect
    data class NavigateToPostDetail(val postId: String) : MapEffect
}

class MapViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState(isLoading = true))
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _effect = Channel<MapEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var cachedPosts: List<Post> = SampleData.posts

    init {
        observePostsAndLoad()
    }

    private fun observePostsAndLoad() {
        viewModelScope.launch {
            delay(350)
            FakePostStore.posts.collectLatest { storePosts ->
                cachedPosts = storePosts.ifEmpty { SampleData.posts }
                recalculateMapData(isLoading = false)
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
                _uiState.update { it.copy(audienceFilter = action.filter) }
                recalculateMapData()
            }

            is MapAction.OnTimeFilterChange -> {
                _uiState.update { it.copy(timeFilter = action.filter) }
                recalculateMapData()
            }

            is MapAction.OnMyLocationClick -> {
                _uiState.update {
                    it.copy(
                        camera = MapCameraState(
                            centerLat = 16.0544,
                            centerLng = 108.2022,
                            zoom = 11f
                        )
                    )
                }
                recalculateMapData()
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
                viewModelScope.launch {
                    delay(300)
                    recalculateMapData(isLoading = false)
                }
            }

            is MapAction.OnBackClick -> {
                viewModelScope.launch {
                    _effect.send(MapEffect.NavigateBack)
                }
            }
        }
    }

    private fun handleBubbleClick(bubble: MapBubble) {
        val currentZoom = _uiState.value.camera.zoom
        when (bubble.type) {
            MapBubbleType.PROVINCE -> {
                if (currentZoom < 8f) {
                    // Zoom qua ngưỡng 8 để chuyển sang bong bóng phường/xã (zoom 7.6 vẫn là mức tỉnh)
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

            MapBubbleType.WARD, MapBubbleType.PROVINCE_WIDE -> {
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
                    val level = if (bubble.type == MapBubbleType.PROVINCE_WIDE) "PROVINCE" else "WARD"
                    openRegionPreview(level = level, bubble = bubble)
                }
            }

            MapBubbleType.PLACE -> {
                // Không có bài khớp thì không mở gì (trước đây mở bừa bài đầu tiên)
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
        // Loại bỏ bài ở mức "Việt Nam" (COUNTRY)
        val validPosts = cachedPosts.filter { it.location.level != LocationLevel.COUNTRY }
        return validPosts.filter { post ->
            if (level == "PROVINCE") {
                post.location.provinceCode == code
            } else {
                post.location.wardCode == code
            }
        }
    }

    private fun findPostIdForPlaceBubble(bubble: MapBubble): String? {
        val validPosts = cachedPosts.filter { it.location.level != LocationLevel.COUNTRY }
        validPosts.firstOrNull { it.id == bubble.code || it.id == bubble.id }?.let { return it.id }
        validPosts.firstOrNull {
            it.location.placeName?.equals(bubble.label, ignoreCase = true) == true
        }?.let { return it.id }
        return null
    }

    private fun recalculateMapData(isLoading: Boolean = _uiState.value.isLoading) {
        val state = _uiState.value
        val zoom = state.camera.zoom

        // 1. Lấy danh sách bubble theo mức zoom (Bài mức Việt Nam không lên bản đồ)
        val rawBubbles = when {
            zoom < 8f -> SampleMapData.allProvinceBubbles
            zoom < 13f -> SampleMapData.allWardAndProvinceWideBubbles
            else -> SampleMapData.allPlaceBubbles
        }

        // 2. Đếm bài thật từ FakePostStore theo luật hiển thị + bộ lọc; bong bóng 0 bài thì không vẽ
        val countablePosts = cachedPosts.filter { post ->
            post.location.level != LocationLevel.COUNTRY && // bài mức "Việt Nam" không lên bản đồ
                post.sendState == PostSendState.SENT &&
                matchesTimeFilter(post, state.timeFilter) &&
                matchesAudienceFilter(post, state.audienceFilter)
        }
        val meId = SampleData.currentUser.id
        val filteredBubbles = rawBubbles.mapNotNull { bubble ->
            val posts = postsForBubble(bubble, countablePosts)
            if (posts.isEmpty()) return@mapNotNull null
            bubble.copy(
                postCount = posts.size,
                hasFriendPosts = posts.any { it.author.id != meId },
                coverImageUrl = posts.maxByOrNull { it.takenAt }?.imageUrl ?: bubble.coverImageUrl
            )
        }
        val countedLandmarks = SampleData.landmarks.map { lm ->
            lm.copy(postCount = countablePosts.count { it.location.landmarkId == lm.id })
        }

        // 3. Kiểm tra vùng khung nhìn hiện tại có bubble hoặc landmark nào không
        val latSpan = 12.0 / Math.pow(2.0, (zoom - 5f).toDouble())
        val lngSpan = 10.0 / Math.pow(2.0, (zoom - 5f).toDouble())

        val bubblesInViewport = filteredBubbles.any { b ->
            abs(b.lat - state.camera.centerLat) <= latSpan &&
                abs(b.lng - state.camera.centerLng) <= lngSpan
        }
        val landmarksInViewport = zoom >= 8f && SampleData.landmarks.any { lm ->
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

    /**
     * Bài thuộc bong bóng nào: tỉnh gom mọi bài trong tỉnh; phường gom bài mức phường + địa điểm;
     * "Khắp tỉnh" chỉ bài người đăng chọn mức tỉnh; ghim địa điểm chỉ bài mức địa điểm.
     */
    private fun postsForBubble(bubble: MapBubble, posts: List<Post>): List<Post> = when (bubble.type) {
        MapBubbleType.PROVINCE -> posts.filter { it.location.provinceCode == bubble.code }
        MapBubbleType.WARD -> posts.filter {
            it.location.wardCode == bubble.code &&
                (it.location.level == LocationLevel.WARD || it.location.level == LocationLevel.PLACE)
        }
        MapBubbleType.PROVINCE_WIDE -> posts.filter {
            it.location.provinceCode == bubble.code && it.location.level == LocationLevel.PROVINCE
        }
        MapBubbleType.PLACE -> posts.filter {
            it.location.level == LocationLevel.PLACE &&
                it.location.placeName?.equals(bubble.label, ignoreCase = true) == true
        }
        MapBubbleType.LANDMARK -> posts.filter { it.location.landmarkId == bubble.code }
    }

    private fun matchesAudienceFilter(post: Post, filter: MapAudienceFilter): Boolean {
        return when (filter) {
            MapAudienceFilter.ALL -> true
            MapAudienceFilter.FRIENDS -> post.author.id != SampleData.currentUser.id
            MapAudienceFilter.PUBLIC -> post.audience == Audience.PUBLIC &&
                (post.publicVisibleAt == null || !post.publicVisibleAt.isAfter(SampleData.NOW))
        }
    }

    private fun matchesTimeFilter(post: Post, filter: MapTimeFilter): Boolean {
        val maxDays = filter.days ?: return true
        val diffDays = Duration.between(post.takenAt, SampleData.NOW).toDays()
        return diffDays <= maxDays
    }
}

fun NavGraphBuilder.mapScreen(navController: NavController) {
    composable<Route.Map> {
        MapRoute(
            onBackClick = { navController.popBackStackSafely() },
            onNavigateToRegionAlbum = { level, code ->
                navController.navigateSingleTop(Route.RegionAlbum(level = level, code = code))
            },
            onNavigateToLandmark = { landmarkId ->
                navController.navigateSingleTop(Route.Landmark(id = landmarkId))
            },
            onNavigateToPostDetail = { postId ->
                navController.navigateSingleTop(Route.PostDetail(id = postId))
            }
        )
    }
}

@Composable
fun MapRoute(
    onBackClick: () -> Unit,
    onNavigateToRegionAlbum: (level: String, code: String) -> Unit,
    onNavigateToLandmark: (landmarkId: String) -> Unit,
    onNavigateToPostDetail: (postId: String) -> Unit,
    viewModel: MapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                MapEffect.NavigateBack -> onBackClick()
                is MapEffect.NavigateToRegionAlbum -> onNavigateToRegionAlbum(effect.level, effect.code)
                is MapEffect.NavigateToLandmark -> onNavigateToLandmark(effect.landmarkId)
                is MapEffect.NavigateToPostDetail -> onNavigateToPostDetail(effect.postId)
            }
        }
    }

    MapScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    uiState: MapUiState,
    onAction: (MapAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes
    val density = LocalDensity.current
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
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
                    camera = uiState.camera,
                    onCameraChange = { onAction(MapAction.OnCameraChanged(it)) },
                    onBubbleClick = { onAction(MapAction.OnBubbleClick(it)) },
                    onLandmarkClick = { onAction(MapAction.OnLandmarkClick(it)) },
                    modifier = Modifier.fillMaxSize(),
                    topInset = with(density) { headerHeightPx.toDp() },
                    bottomInset = navBarHeight
                )
            }
        }

        // Top Controls: OfflineBanner + Header + Bộ lọc GlassChip
        // Chỉ đệm cạnh trên + hai bên: systemBarsPadding() cũ cộng cả chiều cao thanh điều hướng
        // vào header nên tâm bản đồ bị đẩy xuống, bong bóng TP.HCM nằm dưới nút ◀ ● ■.
        // Lề ngang 16dp như các màn khác; hàng lọc vừa khít nên hai mép trái/phải cân nhau.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { headerHeightPx = it.height }
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(horizontal = spacing.lg, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            if (uiState.isOffline) {
                OfflineBanner(modifier = Modifier.fillMaxWidth())
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(shapes.pill)
                            .background(colors.surfaceHigh.copy(alpha = 0.9f))
                            .clickable { onAction(MapAction.OnBackClick) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = colors.textPrimary
                        )
                    }

                    Column(
                        modifier = Modifier
                            .clip(shapes.pill)
                            .background(colors.surfaceHigh.copy(alpha = 0.9f))
                            .padding(horizontal = spacing.md, vertical = spacing.xs)
                    ) {
                        Text(
                            text = "Bản đồ Lokap",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = zoomLevelLabel(uiState.camera.zoom),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                GlassChip(
                    label = "Vị trí của tôi",
                    leadingIcon = Icons.Default.LocationOn,
                    selected = true,
                    onClick = { onAction(MapAction.OnMyLocationClick) }
                )
            }

            // Hàng lọc 1: Tất cả / Bạn bè / Public
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MapAudienceFilter.entries.forEach { filter ->
                    GlassChip(
                        label = filter.label,
                        selected = uiState.audienceFilter == filter,
                        onClick = { onAction(MapAction.OnAudienceFilterChange(filter)) }
                    )
                }

                Spacer(modifier = Modifier.width(spacing.xs))

                // Hàng lọc 2: 7 ngày / 30 ngày / Tất cả
                MapTimeFilter.entries.forEach { timeFilter ->
                    GlassChip(
                        label = timeFilter.label,
                        selected = uiState.timeFilter == timeFilter,
                        onClick = { onAction(MapAction.OnTimeFilterChange(timeFilter)) }
                    )
                }
            }
        }

        // Chip nổi khi vùng bản đồ không có bài
        AnimatedVisibility(
            visible = !uiState.isLoading && uiState.errorMessage == null && uiState.isViewportEmpty,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
                .padding(bottom = spacing.xl)
        ) {
            GlassChip(
                label = "Chưa có bài ở khu vực này",
                leadingIcon = Icons.Default.Refresh,
                selected = false,
                onClick = { onAction(MapAction.OnMyLocationClick) }
            )
        }

        // ModalBottomSheet xem trước RegionCard + nút "Xem album"
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

private fun zoomLevelLabel(zoom: Float): String = when {
    zoom < 8f -> "34 Tỉnh / Thành"
    zoom < 13f -> "Phường / Xã"
    else -> "Địa điểm"
}

@Preview(name = "Map - Tỉnh/Thành (Zoom < 8)", showBackground = true)
@Composable
private fun MapScreenProvincePreview() {
    LokapTheme {
        MapScreen(
            uiState = MapUiState(
                isLoading = false,
                visibleBubbles = SampleMapData.allProvinceBubbles,
                landmarks = SampleData.landmarks
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Map - Phường/Xã + Sheet Xem trước", showBackground = true)
@Composable
private fun MapScreenWardSheetPreview() {
    LokapTheme {
        MapScreen(
            uiState = MapUiState(
                isLoading = false,
                camera = MapCameraState(centerLat = 16.0544, centerLng = 108.2022, zoom = 11f),
                visibleBubbles = SampleMapData.allWardAndProvinceWideBubbles,
                landmarks = SampleData.landmarks,
                selectedRegionPreview = RegionSheetPreview(
                    level = "PROVINCE",
                    code = "48",
                    regionName = "Đà Nẵng",
                    photoUrls = SampleData.posts.map { it.imageUrl }.take(3),
                    postCount = 42,
                    visited = true
                )
            ),
            onAction = {}
        )
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
                errorMessage = "Không thể tải dữ liệu bản đồ. Kiểm tra kết nối mạng nhé!"
            ),
            onAction = {}
        )
    }
}
