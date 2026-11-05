package vn.nhom2.lokap.feature.region

import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import vn.nhom2.lokap.core.navigation.navigateSingleTop
import vn.nhom2.lokap.core.navigation.popBackStackSafely
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import coil3.compose.AsyncImage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.core.designsystem.component.EmptyState
import vn.nhom2.lokap.core.designsystem.component.ErrorState
import vn.nhom2.lokap.core.designsystem.component.GhostButton
import vn.nhom2.lokap.core.designsystem.component.GlassChip
import vn.nhom2.lokap.core.designsystem.component.RegionCard
import vn.nhom2.lokap.core.designsystem.component.ShimmerBox
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.data.fake.FakePostStore
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.data.fake.SampleMapData
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.Post

enum class RegionAudienceFilter(val label: String) {
    FRIENDS("Bạn bè"),
    PUBLIC("Public")
}

data class RegionAlbumUiState(
    val level: String = "PROVINCE",
    val code: String = "48",
    val regionName: String = "Đà Nẵng",
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val filter: RegionAudienceFilter = RegionAudienceFilter.PUBLIC,
    val displayedPosts: List<Post> = emptyList(),
    val totalPostCount: Int = 0,
    val canLoadMore: Boolean = false,
    val visited: Boolean = true
)

sealed interface RegionAlbumAction {
    data class OnFilterSelected(val filter: RegionAudienceFilter) : RegionAlbumAction
    data class OnPostClick(val postId: String) : RegionAlbumAction
    data object OnLoadMoreClick : RegionAlbumAction
    data object OnShareClick : RegionAlbumAction
    data object OnRetry : RegionAlbumAction
    data object OnBackClick : RegionAlbumAction
}

sealed interface RegionAlbumEffect {
    data object NavigateBack : RegionAlbumEffect
    data class NavigateToPostDetail(val postId: String) : RegionAlbumEffect
    data class ShowSnackbar(val message: String) : RegionAlbumEffect
}

class RegionAlbumViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val routeArgs: Route.RegionAlbum = runCatching {
        savedStateHandle.toRoute<Route.RegionAlbum>()
    }.getOrElse {
        Route.RegionAlbum(
            level = savedStateHandle["level"] ?: "PROVINCE",
            code = savedStateHandle["code"] ?: "da-nang"
        )
    }

    private val _uiState = MutableStateFlow(
        RegionAlbumUiState(
            level = routeArgs.level,
            code = routeArgs.code,
            regionName = SampleMapData.findRegionName(routeArgs.level, routeArgs.code),
            isLoading = true
        )
    )
    val uiState: StateFlow<RegionAlbumUiState> = _uiState.asStateFlow()

    private val _effect = Channel<RegionAlbumEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val pageSize = 9
    private var currentPage = 1
    private var allRegionPosts: List<Post> = emptyList()

    init {
        loadRegionPosts()
    }

    private fun loadRegionPosts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            delay(320)
            FakePostStore.posts.collectLatest { storePosts ->
                val base = storePosts.ifEmpty { SampleData.posts }
                // Chỉ bài đúng khu vực; khu vực không có bài thì để trống (màn hình có trạng thái rỗng).
                // Không nhân bản bài để giả phân trang: bản sao trùng id làm trùng key của lưới.
                allRegionPosts = base
                    .filter { it.location.level != LocationLevel.COUNTRY }
                    .filter { post ->
                        if (routeArgs.level.equals("WARD", ignoreCase = true)) {
                            post.location.wardCode == routeArgs.code
                        } else {
                            post.location.provinceCode == routeArgs.code
                        }
                    }
                    .sortedByDescending { it.takenAt }
                applyFilterAndPagination(resetPage = true)
            }
        }
    }

    fun onAction(action: RegionAlbumAction) {
        when (action) {
            is RegionAlbumAction.OnFilterSelected -> {
                _uiState.update { it.copy(filter = action.filter) }
                applyFilterAndPagination(resetPage = true)
            }

            is RegionAlbumAction.OnPostClick -> {
                viewModelScope.launch {
                    _effect.send(RegionAlbumEffect.NavigateToPostDetail(action.postId))
                }
            }

            RegionAlbumAction.OnLoadMoreClick -> {
                if (_uiState.value.isLoadingMore || !_uiState.value.canLoadMore) return
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoadingMore = true) }
                    delay(350)
                    currentPage += 1
                    applyFilterAndPagination(resetPage = false)
                }
            }

            RegionAlbumAction.OnShareClick -> {
                viewModelScope.launch {
                    _effect.send(
                        RegionAlbumEffect.ShowSnackbar("Sắp có: chia sẻ thẻ khu vực")
                    )
                }
            }

            RegionAlbumAction.OnRetry -> loadRegionPosts()

            RegionAlbumAction.OnBackClick -> {
                viewModelScope.launch {
                    _effect.send(RegionAlbumEffect.NavigateBack)
                }
            }
        }
    }

    private fun applyFilterAndPagination(resetPage: Boolean) {
        if (resetPage) currentPage = 1
        val currentFilter = _uiState.value.filter

        val filtered = allRegionPosts.filter { post ->
            when (currentFilter) {
                RegionAudienceFilter.FRIENDS ->
                    post.audience == Audience.FRIENDS || post.audience == Audience.DIRECT
                RegionAudienceFilter.PUBLIC -> post.audience == Audience.PUBLIC
            }
        }

        val limit = currentPage * pageSize
        val sliced = filtered.take(limit)

        _uiState.update {
            it.copy(
                isLoading = false,
                isLoadingMore = false,
                displayedPosts = sliced,
                totalPostCount = filtered.size,
                canLoadMore = filtered.size > sliced.size,
                visited = filtered.isNotEmpty()
            )
        }
    }
}

fun NavGraphBuilder.regionAlbumScreen(navController: NavController) {
    composable<Route.RegionAlbum> {
        RegionAlbumRoute(
            onBackClick = { navController.popBackStackSafely() },
            onNavigateToPostDetail = { postId ->
                navController.navigateSingleTop(Route.PostDetail(id = postId))
            }
        )
    }
}

@Composable
fun RegionAlbumRoute(
    onBackClick: () -> Unit,
    onNavigateToPostDetail: (postId: String) -> Unit,
    viewModel: RegionAlbumViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                RegionAlbumEffect.NavigateBack -> onBackClick()
                is RegionAlbumEffect.NavigateToPostDetail -> onNavigateToPostDetail(effect.postId)
                is RegionAlbumEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    RegionAlbumScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction
    )
}

@Composable
fun RegionAlbumScreen(
    uiState: RegionAlbumUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (RegionAlbumAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.md, vertical = spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(shapes.pill)
                            .background(colors.surfaceHigh)
                            .clickable { onAction(RegionAlbumAction.OnBackClick) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = colors.textPrimary
                        )
                    }
                    Column {
                        Text(
                            text = uiState.regionName,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (uiState.level.equals("WARD", ignoreCase = true)) {
                                "Album Phường / Xã"
                            } else {
                                "Album Tỉnh / Thành phố"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(shapes.pill)
                        .background(colors.surfaceHigh)
                        .clickable { onAction(RegionAlbumAction.OnShareClick) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Chia sẻ thẻ khu vực",
                        tint = colors.primary
                    )
                }
            }

            when {
                uiState.errorMessage != null -> {
                    ErrorState(
                        message = uiState.errorMessage,
                        onRetry = { onAction(RegionAlbumAction.OnRetry) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.lg)
                    )
                }

                uiState.isLoading -> {
                    RegionAlbumLoadingContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = spacing.md)
                    )
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = spacing.md,
                            end = spacing.md,
                            top = spacing.xs,
                            bottom = spacing.xxl
                        ),
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        // Header: RegionCard lớn
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            RegionCard(
                                code = uiState.code,
                                regionName = uiState.regionName,
                                photoUrls = uiState.displayedPosts.map { it.imageUrl }.take(4),
                                postCount = uiState.totalPostCount,
                                visited = uiState.visited,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = spacing.sm)
                            )
                        }

                        // Hàng lọc Bạn bè / Public
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = spacing.sm),
                                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RegionAudienceFilter.entries.forEach { filter ->
                                    GlassChip(
                                        label = filter.label,
                                        selected = uiState.filter == filter,
                                        onClick = { onAction(RegionAlbumAction.OnFilterSelected(filter)) }
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "${uiState.totalPostCount} khoảnh khắc",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        if (uiState.displayedPosts.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                EmptyState(
                                    title = "Chưa ai đăng ở đây, hãy là người đầu tiên",
                                    subtitle = "Mở camera Lokap và thả chiếc ảnh đầu tiên tại ${uiState.regionName} nhé!",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = spacing.xxl)
                                )
                            }
                        } else {
                            items(
                                items = uiState.displayedPosts,
                                key = { post -> post.id }
                            ) { post ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(shapes.card)
                                        .background(colors.surfaceHigh)
                                        .clickable { onAction(RegionAlbumAction.OnPostClick(post.id)) }
                                ) {
                                    AsyncImage(
                                        model = post.imageUrl,
                                        contentDescription = post.caption,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            if (uiState.canLoadMore) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = spacing.md),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        GhostButton(
                                            text = if (uiState.isLoadingMore) {
                                                "Đang tải thêm..."
                                            } else {
                                                "Xem thêm ảnh (${uiState.displayedPosts.size}/${uiState.totalPostCount})"
                                            },
                                            onClick = { onAction(RegionAlbumAction.OnLoadMoreClick) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(spacing.md)
        )
    }
}

@Composable
private fun RegionAlbumLoadingContent(modifier: Modifier = Modifier) {
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            shape = shapes.card
        )
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            ShimmerBox(modifier = Modifier.size(width = 88.dp, height = 34.dp), shape = shapes.pill)
            ShimmerBox(modifier = Modifier.size(width = 88.dp, height = 34.dp), shape = shapes.pill)
        }
        repeat(3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                repeat(3) {
                    ShimmerBox(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        shape = shapes.card
                    )
                }
            }
        }
    }
}

@Preview(name = "RegionAlbum - Đầy đủ dữ liệu", showBackground = true)
@Composable
private fun RegionAlbumPopulatedPreview() {
    LokapTheme {
        RegionAlbumScreen(
            uiState = RegionAlbumUiState(
                level = "PROVINCE",
                code = "48",
                regionName = "Đà Nẵng",
                isLoading = false,
                displayedPosts = SampleData.posts,
                totalPostCount = 24,
                canLoadMore = true,
                visited = true
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {}
        )
    }
}

@Preview(name = "RegionAlbum - Trống", showBackground = true)
@Composable
private fun RegionAlbumEmptyPreview() {
    LokapTheme {
        RegionAlbumScreen(
            uiState = RegionAlbumUiState(
                level = "WARD",
                code = "46_vyda",
                regionName = "P. Vỹ Dạ",
                isLoading = false,
                displayedPosts = emptyList(),
                totalPostCount = 0,
                visited = false
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {}
        )
    }
}

@Preview(name = "RegionAlbum - Đang tải", showBackground = true)
@Composable
private fun RegionAlbumLoadingPreview() {
    LokapTheme {
        RegionAlbumScreen(
            uiState = RegionAlbumUiState(isLoading = true),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {}
        )
    }
}

@Preview(name = "RegionAlbum - Lỗi", showBackground = true)
@Composable
private fun RegionAlbumErrorPreview() {
    LokapTheme {
        RegionAlbumScreen(
            uiState = RegionAlbumUiState(
                isLoading = false,
                errorMessage = "Không thể tải album khu vực này. Thử lại sau nhé!"
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {}
        )
    }
}
