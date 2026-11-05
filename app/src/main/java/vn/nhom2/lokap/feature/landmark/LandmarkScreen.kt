package vn.nhom2.lokap.feature.landmark

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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import vn.nhom2.lokap.core.designsystem.component.GlassChip
import vn.nhom2.lokap.core.designsystem.component.ShimmerBox
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.data.fake.FakePostStore
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.data.fake.SampleMapData
import vn.nhom2.lokap.domain.model.Landmark
import vn.nhom2.lokap.domain.model.Post

data class LandmarkUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val landmark: Landmark? = null,
    val provinceName: String = "",
    val posts: List<Post> = emptyList()
)

sealed interface LandmarkAction {
    data class OnPostClick(val postId: String) : LandmarkAction
    data object OnRetry : LandmarkAction
    data object OnBackClick : LandmarkAction
}

sealed interface LandmarkEffect {
    data object NavigateBack : LandmarkEffect
    data class NavigateToPostDetail(val postId: String) : LandmarkEffect
}

class LandmarkViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val landmarkId: String = runCatching {
        savedStateHandle.toRoute<Route.Landmark>().id
    }.getOrElse {
        savedStateHandle["id"] ?: SampleData.landmarks.firstOrNull()?.id.orEmpty()
    }

    private val _uiState = MutableStateFlow(LandmarkUiState(isLoading = true))
    val uiState: StateFlow<LandmarkUiState> = _uiState.asStateFlow()

    private val _effect = Channel<LandmarkEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadLandmarkDetails()
    }

    private fun loadLandmarkDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            delay(300)

            // Không tìm thấy thì báo lỗi, không lặng lẽ mở địa danh khác
            val foundLandmark = SampleData.landmarks.firstOrNull { it.id == landmarkId }

            if (foundLandmark == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Không tìm thấy thông tin địa danh này."
                    )
                }
                return@launch
            }

            val provinceLabel = SampleMapData.findRegionName(
                level = "PROVINCE",
                code = foundLandmark.provinceCode
            )

            FakePostStore.posts.collectLatest { storePosts ->
                val basePosts = storePosts.ifEmpty { SampleData.posts }
                // Chỉ bài gắn đúng địa danh này (trước đây lấy cả mọi bài cùng tỉnh)
                val landmarkPosts = basePosts
                    .filter { it.location.landmarkId == foundLandmark.id }
                    .sortedByDescending { it.takenAt }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        landmark = foundLandmark,
                        provinceName = provinceLabel,
                        posts = landmarkPosts
                    )
                }
            }
        }
    }

    fun onAction(action: LandmarkAction) {
        when (action) {
            is LandmarkAction.OnPostClick -> {
                viewModelScope.launch {
                    _effect.send(LandmarkEffect.NavigateToPostDetail(action.postId))
                }
            }

            LandmarkAction.OnRetry -> loadLandmarkDetails()

            LandmarkAction.OnBackClick -> {
                viewModelScope.launch {
                    _effect.send(LandmarkEffect.NavigateBack)
                }
            }
        }
    }
}

fun NavGraphBuilder.landmarkScreen(navController: NavController) {
    composable<Route.Landmark> {
        LandmarkRoute(
            onBackClick = { navController.popBackStackSafely() },
            onNavigateToPostDetail = { postId ->
                navController.navigateSingleTop(Route.PostDetail(id = postId))
            }
        )
    }
}

@Composable
fun LandmarkRoute(
    onBackClick: () -> Unit,
    onNavigateToPostDetail: (postId: String) -> Unit,
    viewModel: LandmarkViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                LandmarkEffect.NavigateBack -> onBackClick()
                is LandmarkEffect.NavigateToPostDetail -> onNavigateToPostDetail(effect.postId)
            }
        }
    }

    LandmarkScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@Composable
fun LandmarkScreen(
    uiState: LandmarkUiState,
    onAction: (LandmarkAction) -> Unit,
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
        when {
            uiState.errorMessage != null -> {
                ErrorState(
                    message = uiState.errorMessage,
                    onRetry = { onAction(LandmarkAction.OnRetry) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(spacing.lg)
                )
            }

            uiState.isLoading || uiState.landmark == null -> {
                LandmarkLoadingContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(spacing.md)
                )
            }

            else -> {
                val landmark = uiState.landmark
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = spacing.md,
                        end = spacing.md,
                        top = spacing.sm,
                        bottom = spacing.xxl
                    ),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    // Header: Nút quay lại + Ảnh bìa lớn + Nguồn ảnh + Tên + Tỉnh + Mô tả + Số bài
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(spacing.sm)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(shapes.pill)
                                        .background(colors.surfaceHigh)
                                        .clickable { onAction(LandmarkAction.OnBackClick) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Quay lại",
                                        tint = colors.textPrimary
                                    )
                                }

                                GlassChip(
                                    label = uiState.provinceName,
                                    leadingIcon = Icons.Default.LocationOn,
                                    selected = true
                                )
                            }

                            // Ảnh bìa lớn
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(230.dp)
                                    .clip(shapes.card)
                                    .background(colors.surfaceHigh)
                            ) {
                                AsyncImage(
                                    model = landmark.coverUrl,
                                    contentDescription = landmark.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Dòng nguồn ảnh (coverAttribution) nhỏ ngay dưới ảnh bìa
                            Text(
                                text = landmark.coverAttribution,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textTertiary
                            )

                            // Tên địa danh (chữ lớn)
                            Text(
                                text = landmark.name,
                                style = MaterialTheme.typography.headlineMedium,
                                color = colors.textPrimary
                            )

                            // Tỉnh & Số bài
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                            ) {
                                GlassChip(
                                    label = uiState.provinceName,
                                    selected = false
                                )
                                GlassChip(
                                    label = "${uiState.posts.size} bài đăng",
                                    selected = true
                                )
                            }

                            // Mô tả
                            Text(
                                text = landmark.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )

                            Spacer(modifier = Modifier.height(spacing.xs))

                            Text(
                                text = "Khoảnh khắc chụp tại đây",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary,
                                modifier = Modifier.padding(bottom = spacing.xs)
                            )
                        }
                    }

                    if (uiState.posts.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            EmptyState(
                                title = "Hãy là người đầu tiên chụp ở đây",
                                subtitle = "Check-in tại ${landmark.name} để ghim ảnh của bạn lên bản đồ Lokap!",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = spacing.xxl)
                            )
                        }
                    } else {
                        items(
                            items = uiState.posts,
                            key = { it.id }
                        ) { post ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(shapes.card)
                                    .background(colors.surfaceHigh)
                                    .clickable { onAction(LandmarkAction.OnPostClick(post.id)) }
                            ) {
                                AsyncImage(
                                    model = post.imageUrl,
                                    contentDescription = post.caption,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LandmarkLoadingContent(modifier: Modifier = Modifier) {
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        ShimmerBox(modifier = Modifier.size(40.dp), shape = shapes.pill)
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp),
            shape = shapes.card
        )
        ShimmerBox(modifier = Modifier.size(width = 160.dp, height = 14.dp), shape = shapes.pill)
        ShimmerBox(modifier = Modifier.size(width = 240.dp, height = 32.dp), shape = shapes.pill)
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = shapes.card
        )
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

@Preview(name = "Landmark - Có ảnh", showBackground = true)
@Composable
private fun LandmarkPopulatedPreview() {
    LokapTheme {
        LandmarkScreen(
            uiState = LandmarkUiState(
                isLoading = false,
                landmark = SampleData.landmarks.firstOrNull(),
                provinceName = "Đà Nẵng",
                posts = SampleData.posts
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Landmark - Chưa có ảnh", showBackground = true)
@Composable
private fun LandmarkEmptyPreview() {
    LokapTheme {
        LandmarkScreen(
            uiState = LandmarkUiState(
                isLoading = false,
                landmark = SampleData.landmarks.firstOrNull()?.copy(postCount = 0),
                provinceName = "TP. Huế",
                posts = emptyList()
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Landmark - Đang tải", showBackground = true)
@Composable
private fun LandmarkLoadingPreview() {
    LokapTheme {
        LandmarkScreen(
            uiState = LandmarkUiState(isLoading = true),
            onAction = {}
        )
    }
}

@Preview(name = "Landmark - Lỗi", showBackground = true)
@Composable
private fun LandmarkErrorPreview() {
    LokapTheme {
        LandmarkScreen(
            uiState = LandmarkUiState(
                isLoading = false,
                errorMessage = "Không thể tải thông tin địa danh. Vui lòng thử lại!"
            ),
            onAction = {}
        )
    }
}
