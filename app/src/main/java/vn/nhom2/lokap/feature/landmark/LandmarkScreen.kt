package vn.nhom2.lokap.feature.landmark

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import vn.nhom2.lokap.core.designsystem.component.*
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.core.navigation.navigateSingleTop
import vn.nhom2.lokap.core.navigation.popBackStackSafely
import vn.nhom2.lokap.domain.model.Landmark
import vn.nhom2.lokap.domain.model.LandmarkPhotoTab
import vn.nhom2.lokap.domain.model.Post
import vn.nhom2.lokap.domain.model.ReportReasons
import vn.nhom2.lokap.domain.model.reactionTotal

fun NavGraphBuilder.landmarkScreen(navController: NavController) {
    composable<Route.Landmark> {
        LandmarkRoute(
            onBackClick = { navController.popBackStackSafely() },
            onNavigateToPostDetail = { postId -> navController.navigateSingleTop(Route.PostDetail(id = postId)) },
            onNavigateToCamera = { id -> navController.navigateSingleTop(Route.LandmarkCamera(landmarkId = id)) },
            onNavigateToComposer = { path, id ->
                navController.navigateSingleTop(Route.Composer(imagePath = path, landmarkId = id))
            }
        )
    }
}

@Composable
fun LandmarkRoute(
    onBackClick: () -> Unit,
    onNavigateToPostDetail: (postId: String) -> Unit,
    onNavigateToCamera: (landmarkId: String) -> Unit,
    onNavigateToComposer: (imagePath: String, landmarkId: String) -> Unit,
    viewModel: LandmarkViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Photo Picker của hệ thống: chỉ trả ảnh người dùng chọn, không cần quyền đọc bộ nhớ
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        viewModel.onAction(LandmarkAction.OnGalleryResult(uri?.toString()))
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                LandmarkEffect.NavigateBack -> onBackClick()
                is LandmarkEffect.NavigateToPostDetail -> onNavigateToPostDetail(effect.postId)
                is LandmarkEffect.NavigateToCamera -> onNavigateToCamera(effect.landmarkId)
                is LandmarkEffect.NavigateToComposer -> onNavigateToComposer(effect.imagePath, effect.landmarkId)
                LandmarkEffect.LaunchGalleryPicker -> galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
                is LandmarkEffect.ShowMessage -> scope.launch { snackbarHostState.showSnackbar(effect.message) }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LandmarkScreen(uiState = uiState, onAction = viewModel::onAction)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 96.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandmarkScreen(
    uiState: LandmarkUiState,
    onAction: (LandmarkAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
    ) {
        val landmark = uiState.landmark
        when {
            uiState.errorMessage != null -> {
                ErrorState(
                    message = uiState.errorMessage,
                    onRetry = { onAction(LandmarkAction.OnRetry) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(spacing.lg)
                )
                CircleIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    description = "Quay lại",
                    onClick = { onAction(LandmarkAction.OnBackClick) },
                    modifier = Modifier.padding(spacing.md)
                )
            }

            uiState.isLoading || landmark == null -> {
                LandmarkLoadingContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(spacing.md)
                )
            }

            else -> {
                LandmarkContent(uiState = uiState, landmark = landmark, onAction = onAction)

                AddPhotoFab(
                    busy = uiState.isBusy,
                    onClick = { onAction(LandmarkAction.OnAddPhotoClick) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(spacing.lg)
                )
            }
        }
    }

    if (uiState.isAddSheetOpen) {
        AddPhotoSheet(onAction = onAction)
    }

    if (uiState.isAttachSheetOpen) {
        AttachPostSheet(posts = uiState.attachablePosts, busy = uiState.isBusy, onAction = onAction)
    }

    if (uiState.isReportSheetOpen) {
        ReportLandmarkSheet(onAction = onAction)
    }

    if (uiState.detachCandidate != null) {
        AlertDialog(
            onDismissRequest = { onAction(LandmarkAction.OnDismissDetach) },
            title = {
                Text(
                    text = "Gỡ ảnh khỏi địa danh?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            confirmButton = {
                TextButton(onClick = { onAction(LandmarkAction.OnConfirmDetach) }) {
                    Text(text = "Gỡ", color = colors.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(LandmarkAction.OnDismissDetach) }) {
                    Text(text = "Huỷ", color = colors.textPrimary)
                }
            },
            containerColor = colors.surface,
            shape = LokapTheme.shapes.dialog
        )
    }
}

@Composable
private fun LandmarkContent(
    uiState: LandmarkUiState,
    landmark: Landmark,
    onAction: (LandmarkAction) -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val isCreator = landmark.createdBy != null && landmark.createdBy == uiState.meId

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = spacing.md, end = spacing.md, top = spacing.sm, bottom = 112.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "header") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CircleIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        description = "Quay lại",
                        onClick = { onAction(LandmarkAction.OnBackClick) }
                    )
                    if (!isCreator) {
                        LandmarkMenu(onReport = { onAction(LandmarkAction.OnReportClick) })
                    }
                }

                LandmarkHero(
                    landmark = landmark,
                    provinceName = uiState.provinceName,
                    photoCount = uiState.totalPostCount
                )

                if (landmark.coverUrl != null && landmark.coverAttribution.isNotBlank()) {
                    Text(
                        text = "Ảnh: ${landmark.coverAttribution}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textTertiary,
                        modifier = Modifier.align(Alignment.End)
                    )
                }

                if (landmark.description.isNotBlank()) {
                    Text(
                        text = landmark.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    LandmarkPhotoTab.entries.forEach { tab ->
                        GlassChip(
                            label = tab.label,
                            selected = uiState.tab == tab,
                            onClick = { onAction(LandmarkAction.OnSelectTab(tab)) }
                        )
                    }
                }
            }
        }

        if (uiState.posts.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "empty") {
                val noPhotoAtAll = uiState.totalPostCount == 0
                EmptyState(
                    title = if (noPhotoAtAll) "Hãy là người đầu tiên" else "Chưa có ảnh",
                    actionLabel = if (noPhotoAtAll) "Thêm ảnh" else null,
                    onActionClick = if (noPhotoAtAll) ({ onAction(LandmarkAction.OnAddPhotoClick) }) else null,
                    modifier = Modifier.padding(vertical = spacing.lg)
                )
            }
        } else {
            items(items = uiState.posts, key = { it.id }) { post ->
                LandmarkPhotoCell(
                    post = post,
                    isMine = post.author.id == uiState.meId,
                    isCover = post.id == landmark.coverPostId,
                    onClick = { onAction(LandmarkAction.OnPostClick(post.id)) },
                    onLongClick = { onAction(LandmarkAction.OnPostLongClick(post)) }
                )
            }
        }
    }
}

/** Ảnh bìa (nhiều cảm xúc nhất) + tên địa danh căn giữa trên nền mờ tối. */
@Composable
private fun LandmarkHero(
    landmark: Landmark,
    provinceName: String,
    photoCount: Int
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val isPreview = LocalInspectionMode.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(LokapTheme.shapes.card)
            .background(
                Brush.linearGradient(listOf(colors.surfaceHigh, colors.primary.copy(alpha = 0.35f)))
            )
    ) {
        if (landmark.coverUrl != null && !isPreview) {
            AsyncImage(
                model = landmark.coverUrl,
                contentDescription = "Ảnh bìa ${landmark.name}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                tint = colors.accentYellow.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 48.dp)
                    .size(64.dp)
            )
        }

        if (landmark.coverUrl != null) {
            // Ảnh bìa được chọn vì có nhiều cảm xúc nhất
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(spacing.sm)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Ảnh bìa nhiều cảm xúc nhất, ${landmark.coverReactionCount} cảm xúc"
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = colors.accentYellow,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = landmark.coverReactionCount.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                .padding(horizontal = spacing.md, vertical = spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = landmark.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = listOf(provinceName, "$photoCount ảnh").filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LandmarkPhotoCell(
    post: Post,
    isMine: Boolean,
    isCover: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = LokapTheme.colors
    val isPreview = LocalInspectionMode.current
    val reactions = post.reactionTotal

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(LokapTheme.shapes.card)
            .background(colors.surfaceHigh)
            .then(if (isCover) Modifier.border(2.dp, colors.accentYellow, LokapTheme.shapes.card) else Modifier)
            .combinedClickable(
                onClick = onClick,
                onLongClick = if (isMine) onLongClick else null,
                onLongClickLabel = if (isMine) "Gỡ khỏi địa danh" else null,
                role = Role.Image
            )
            .semantics {
                contentDescription = buildString {
                    append("Ảnh của ${post.author.displayName}")
                    if (reactions > 0) append(", $reactions cảm xúc")
                }
            }
    ) {
        if (!isPreview) {
            AsyncImage(
                model = post.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (reactions > 0) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = reactions.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }

        if (isMine) {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.85f))
                    .padding(2.dp)
                    .size(16.dp)
            )
        }
    }
}

@Composable
private fun AddPhotoFab(busy: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LokapTheme.colors
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(colors.primary)
            .clickable(enabled = !busy, onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center
    ) {
        if (busy) {
            CircularProgressIndicator(color = colors.onPrimary, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
        } else {
            Icon(
                imageVector = Icons.Rounded.AddAPhoto,
                contentDescription = "Thêm ảnh vào địa danh",
                tint = colors.onPrimary,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun CircleIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(colors.surfaceHigh)
            .clickable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = description, tint = colors.textPrimary, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun LandmarkMenu(onReport: () -> Unit) {
    val colors = LokapTheme.colors
    var expanded by remember { mutableStateOf(false) }
    Box {
        CircleIconButton(icon = Icons.Rounded.MoreVert, description = "Thêm", onClick = { expanded = true })
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = colors.surfaceHigh
        ) {
            DropdownMenuItem(
                text = { Text("Báo cáo địa danh", color = colors.textPrimary) },
                onClick = {
                    expanded = false
                    onReport()
                }
            )
        }
    }
}

/** Ba nguồn ảnh: chụp mới · chọn từ thư viện · gắn bài public đã đăng. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPhotoSheet(onAction: (LandmarkAction) -> Unit) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    ModalBottomSheet(
        onDismissRequest = { onAction(LandmarkAction.OnDismissAddSheet) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        shape = LokapTheme.shapes.sheet
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg)
                .padding(top = spacing.sm, bottom = spacing.xl),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PhotoSourceOption(Icons.Rounded.PhotoCamera, "Chụp") { onAction(LandmarkAction.OnTakePhotoClick) }
            PhotoSourceOption(Icons.Rounded.PhotoLibrary, "Thư viện") { onAction(LandmarkAction.OnPickFromGalleryClick) }
            // Quả địa cầu = ảnh công khai đã đăng (khác hẳn icon Thư viện)
            PhotoSourceOption(Icons.Rounded.Public, "Đã đăng") { onAction(LandmarkAction.OnAttachExistingClick) }
        }
    }
}

@Composable
private fun PhotoSourceOption(icon: ImageVector, label: String, onClick: () -> Unit) {
    val colors = LokapTheme.colors
    Column(
        modifier = Modifier
            .clip(LokapTheme.shapes.card)
            .clickable(onClick = onClick, role = Role.Button)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(colors.surfaceHigh)
                .border(1.5.dp, colors.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(30.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary
        )
    }
}

/** Bài public đã đăng của mình: chạm một ảnh để gắn vào địa danh. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachPostSheet(posts: List<Post>, busy: Boolean, onAction: (LandmarkAction) -> Unit) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val isPreview = LocalInspectionMode.current
    ModalBottomSheet(
        onDismissRequest = { onAction(LandmarkAction.OnDismissAttachSheet) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        shape = LokapTheme.shapes.sheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg)
                .padding(bottom = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Icon(Icons.Rounded.Public, contentDescription = null, tint = colors.primary, modifier = Modifier.size(26.dp))
                Text(text = "Ảnh công khai", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                if (busy) CircularProgressIndicator(color = colors.primary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            }
            if (posts.isEmpty()) {
                EmptyState(title = "Chưa có ảnh công khai")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    items(items = posts, key = { it.id }) { post ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(LokapTheme.shapes.card)
                                .background(colors.surfaceHigh)
                                .clickable(enabled = !busy, role = Role.Button) {
                                    onAction(LandmarkAction.OnAttachPost(post.id))
                                }
                                .semantics {
                                    contentDescription = post.caption?.takeIf { it.isNotBlank() }
                                        ?.let { "Gắn ảnh \"$it\" vào địa danh" } ?: "Gắn ảnh vào địa danh"
                                }
                        ) {
                            if (!isPreview) {
                                AsyncImage(
                                    model = post.imageUrl,
                                    contentDescription = null,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportLandmarkSheet(onAction: (LandmarkAction) -> Unit) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes
    ModalBottomSheet(
        onDismissRequest = { onAction(LandmarkAction.OnDismissReport) },
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
            Text(text = "Báo cáo địa danh", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            ReportReasons.forEach { reason ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.card)
                        .background(colors.surfaceHigh)
                        .border(1.dp, colors.outline, shapes.card)
                        .clickable { onAction(LandmarkAction.OnSubmitReport(reason)) }
                        .padding(horizontal = spacing.lg, vertical = spacing.md)
                ) {
                    Text(text = reason, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
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
        ShimmerBox(modifier = Modifier.size(48.dp), shape = shapes.pill)
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f),
            shape = shapes.card
        )
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            repeat(3) { ShimmerBox(modifier = Modifier.size(width = 88.dp, height = 36.dp), shape = shapes.pill) }
        }
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

private val PreviewLandmark = Landmark(
    id = "vm_preview",
    name = "Cầu Rồng",
    provinceCode = "da-nang",
    wardCode = null,
    lat = 16.0611,
    lng = 108.2275,
    coverUrl = "https://example.com/cover.jpg",
    coverAttribution = "Minh Anh",
    description = "Cây cầu phun lửa cuối tuần bắc qua sông Hàn.",
    postCount = 0,
    provinceName = "Đà Nẵng",
    coverReactionCount = 12
)

@Preview(name = "Landmark - Chưa có ảnh", showBackground = true)
@Composable
private fun LandmarkEmptyPreview() {
    LokapTheme {
        LandmarkScreen(
            uiState = LandmarkUiState(
                isLoading = false,
                landmark = PreviewLandmark.copy(coverUrl = null),
                provinceName = "Đà Nẵng"
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Landmark - Đang tải", showBackground = true)
@Composable
private fun LandmarkLoadingPreview() {
    LokapTheme {
        LandmarkScreen(uiState = LandmarkUiState(isLoading = true), onAction = {})
    }
}

@Preview(name = "Landmark - Lỗi", showBackground = true)
@Composable
private fun LandmarkErrorPreview() {
    LokapTheme {
        LandmarkScreen(
            uiState = LandmarkUiState(isLoading = false, errorMessage = "Không tìm thấy địa danh này"),
            onAction = {}
        )
    }
}
