package vn.nhom2.lokap.feature.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import java.time.Duration
import kotlin.math.abs
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.core.designsystem.component.*
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.core.navigation.navigateSingleTop
import vn.nhom2.lokap.core.navigation.popBackStackSafely
import vn.nhom2.lokap.data.fake.FakePostStore
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.data.fake.SampleMapData
import vn.nhom2.lokap.domain.model.*
enum class MapAudienceFilter(val label: String) {
    ALL("Tất cả"),
    FRIENDS("Bạn bè"),
    PUBLIC("Công khai")
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

        val rawBubbles = when {
            zoom < 8f -> SampleMapData.allProvinceBubbles
            zoom < 13f -> SampleMapData.allWardAndProvinceWideBubbles
            else -> SampleMapData.allPlaceBubbles
        }

        val countablePosts = cachedPosts.filter { post ->
            post.location.level != LocationLevel.COUNTRY &&
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
            },
            onNavigateToTab = { tab ->
                when (tab) {
                    MainTab.FEED -> navController.navigateSingleTop(Route.Feed)
                    MainTab.MAP -> Unit
                    MainTab.CAMERA -> navController.navigateSingleTop(Route.Camera)
                    MainTab.INBOX -> navController.navigateSingleTop(Route.Inbox)
                    MainTab.PROFILE -> navController.navigateSingleTop(Route.Profile())
                }
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
    onNavigateToTab: (MainTab) -> Unit = {},
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
        onAction = viewModel::onAction,
        onNavigateToTab = onNavigateToTab
    )
}

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
    var headerHeightPx by remember { mutableIntStateOf(0) }

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
                    topInset = with(density) { headerHeightPx.toDp() }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { headerHeightPx = it.height }
                .systemBarsPadding()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            if (uiState.isOffline) {
                OfflineBanner(modifier = Modifier.fillMaxWidth())
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.surface.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.22f)),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = colors.surfaceHigh,
                            border = BorderStroke(0.8.dp, colors.outline.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { onAction(MapAction.OnBackClick) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Quay lại",
                                    tint = colors.textPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bản đồ Lokap",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.5.sp
                                ),
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(colors.primary)
                                )
                                Text(
                                    text = zoomLevelLabel(uiState.camera.zoom),
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

                        Surface(
                            shape = CircleShape,
                            color = colors.primary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .height(34.dp)
                                .clip(CircleShape)
                                .clickable { onAction(MapAction.OnMyLocationClick) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MyLocation,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Vị trí của tôi",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.5.sp
                                    ),
                                    color = colors.primary
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.surfaceHigh.copy(alpha = 0.75f),
                            border = BorderStroke(0.8.dp, colors.outline.copy(alpha = 0.18f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MapAudienceFilter.entries.forEach { filter ->
                                    val isSelected = uiState.audienceFilter == filter
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) colors.surface else Color.Transparent,
                                        border = if (isSelected) BorderStroke(1.dp, colors.primary.copy(alpha = 0.3f)) else null,
                                        shadowElevation = if (isSelected) 1.5.dp else 0.dp,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { onAction(MapAction.OnAudienceFilterChange(filter)) }
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Text(
                                                text = filter.label,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 11.5.sp
                                                ),
                                                color = if (isSelected) colors.primary else colors.textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Box {
                            var showTimeMenu by remember { mutableStateOf(false) }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = colors.surfaceHigh.copy(alpha = 0.75f),
                                border = BorderStroke(0.8.dp, colors.outline.copy(alpha = 0.18f)),
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { showTimeMenu = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        tint = if (uiState.timeFilter != MapTimeFilter.ALL) colors.primary else colors.textSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = uiState.timeFilter.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.5.sp
                                        ),
                                        color = if (uiState.timeFilter != MapTimeFilter.ALL) colors.primary else colors.textPrimary
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowDropDown,
                                        contentDescription = null,
                                        tint = colors.textTertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showTimeMenu,
                                onDismissRequest = { showTimeMenu = false },
                                modifier = Modifier.background(colors.surfaceHigh)
                            ) {
                                MapTimeFilter.entries.forEach { timeFilter ->
                                    val isSelected = uiState.timeFilter == timeFilter
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = null,
                                                        tint = colors.primary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                                Text(
                                                    text = timeFilter.label,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) colors.primary else colors.textPrimary
                                                    )
                                                )
                                            }
                                        },
                                        onClick = {
                                            onAction(MapAction.OnTimeFilterChange(timeFilter))
                                            showTimeMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
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
