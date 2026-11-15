package vn.nhom2.lokap.feature.composer

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.scaleIn
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CropPortrait
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MovieFilter
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import coil3.compose.AsyncImage
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import vn.nhom2.lokap.core.designsystem.component.*
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.core.navigation.popBackStackSafely
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.POST_CAPTION_MAX_LENGTH
fun NavGraphBuilder.composerScreen(navController: NavController) {
    composable<Route.Composer> { backStackEntry ->
        val route = backStackEntry.toRoute<Route.Composer>()
        val landmarkId = route.landmarkId
        ComposerRoute(
            imagePath = route.imagePath,
            landmarkId = landmarkId,
            onNavigateBack = { navController.popBackStackSafely() },
            onPostSent = {
                // Ảnh địa danh: về thẳng màn Địa danh (bỏ qua camera nếu chụp từ đó) để thấy ảnh vừa đăng
                val backToLandmark = landmarkId != null &&
                    navController.popBackStack(Route.Landmark(id = landmarkId), inclusive = false)
                if (!backToLandmark) navController.popBackStackSafely()
            }
        )
    }
}

@Composable
fun ComposerRoute(
    imagePath: String,
    onNavigateBack: () -> Unit,
    landmarkId: String? = null,
    onPostSent: () -> Unit = onNavigateBack,
    modifier: Modifier = Modifier,
    viewModel: ComposerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(imagePath) {
        viewModel.initImagePath(imagePath)
    }

    LaunchedEffect(landmarkId) {
        viewModel.initLandmark(landmarkId)
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ComposerEffect.NavigateBackToCamera -> onNavigateBack()
                ComposerEffect.PostSent -> onPostSent()
                is ComposerEffect.ShowMessage -> scope.launch { snackbarHostState.showSnackbar(effect.message) }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        ComposerScreen(
            state = state,
            onAction = viewModel::onAction
        )
        // Lỗi gửi bài hiện ở đáy màn; nút Gửi đã mở lại để thử lần nữa
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}

/**
 * Màn gửi ảnh (bố cục giống màn "Gửi đến…" kiểu Locket, theo màu của Lokap):
 * ảnh ở cùng khung với camera, chú thích nằm trên ảnh, vuốt ảnh để đổi khung/dấu chìm (chấm bên dưới),
 * hàng nút ✕ · Gửi · ✨ cùng vị trí với flash · chụp · lật, cuối màn chọn Bạn bè / Công khai.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposerScreen(
    state: ComposerUiState,
    onAction: (ComposerAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val isPreview = LocalInspectionMode.current
    val scope = rememberCoroutineScope()
    val stamps = PhotoWatermarkStamp.entries

    val imageModel: Any = remember(state.imagePath) {
        if (state.imagePath.startsWith("http")) {
            state.imagePath
        } else {
            File(state.imagePath)
        }
    }

    val pagerState = rememberPagerState(initialPage = state.selectedStamp.ordinal) { stamps.size }
    val currentOnAction by rememberUpdatedState(onAction)
    var showStampName by remember { mutableStateOf(false) }
    var showStampPicker by remember { mutableStateOf(false) }
    // Vuốt xong trang nào thì chọn dấu đó; hiện tên dấu ~1 giây (bỏ qua lần mở màn)
    LaunchedEffect(pagerState) {
        var first = true
        snapshotFlow { pagerState.settledPage }.collectLatest { page ->
            currentOnAction(ComposerAction.OnSelectStamp(stamps[page]))
            if (first) {
                first = false
                return@collectLatest
            }
            showStampName = true
            delay(1100)
            showStampName = false
        }
    }
    val captionBottom by animateDpAsState(
        targetValue = when (stamps[pagerState.currentPage]) {
            PhotoWatermarkStamp.ART_POLAROID -> 54.dp
            PhotoWatermarkStamp.KODAK_FILM_35MM -> 26.dp
            else -> 14.dp
        },
        label = "caption_bottom"
    )

    // Cao tối thiểu = màn hình; bàn phím mở (imePadding) thì nội dung cuộn, ô chú thích tự cuộn vào tầm nhìn.
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .imePadding()
    ) {
        val screenHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = screenHeight)
                .padding(bottom = spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ComposerTopBar(
                locationLabel = state.locationLabel,
                isLandmark = state.isLandmarkMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg, vertical = spacing.sm)
            )

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg)
                    .aspectRatio(1f)
                    .clip(LokapTheme.shapes.cameraFrame)
                    .background(colors.surface)
                    .border(1.5.dp, colors.outline, LokapTheme.shapes.cameraFrame)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    FramedPhoto(
                        stamp = stamps[page],
                        imageModel = imageModel,
                        showImage = !isPreview && state.imagePath.isNotBlank(),
                        provinceName = state.currentProvinceName,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showStampName,
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(220)),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    val stamp = stamps[pagerState.currentPage]
                    // Nền hồng chủ đạo như nhãn của vòng bộ lọc
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.primary)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(stamp.icon, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(18.dp))
                        Text(stamp.labelVi, color = colors.onPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Chú thích cố định trên ảnh (không trượt theo trang), cùng kiểu với ảnh ở bảng tin
                PhotoCaptionField(
                    value = state.caption,
                    onValueChange = { onAction(ComposerAction.OnCaptionChange(it)) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = spacing.md, end = spacing.md, bottom = captionBottom)
                )
            }

            Spacer(modifier = Modifier.height(spacing.md))
            PageDots(count = stamps.size, current = pagerState.currentPage)

            Spacer(modifier = Modifier.weight(1f))

            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ComposerSideButton(
                        icon = Icons.Rounded.Close,
                        description = "Huỷ",
                        onClick = { onAction(ComposerAction.OnCancelClick) }
                    )
                    SendButton(
                        enabled = state.canSend,
                        loading = state.isSubmitting,
                        onClick = { onAction(ComposerAction.OnSendClick) }
                    )
                    ComposerSideButton(
                        icon = Icons.Rounded.AutoAwesome,
                        description = "Chọn khung ảnh",
                        onClick = { showStampPicker = true }
                    )
                }

                if (showStampPicker) {
                    StampPickerPopup(
                        selected = stamps[pagerState.currentPage],
                        onSelect = { stamp ->
                            showStampPicker = false
                            scope.launch { pagerState.animateScrollToPage(stamp.ordinal) }
                        },
                        onDismiss = { showStampPicker = false }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            AudiencePicker(
                audience = state.audience,
                lockedPublic = state.isLandmarkMode,
                onSelectAudience = { onAction(ComposerAction.OnSelectAudience(it)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (state.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { onAction(ComposerAction.OnDismissDiscardDialog) },
            title = {
                Text(
                    text = "Bỏ ảnh này?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            confirmButton = {
                TextButton(onClick = { onAction(ComposerAction.OnConfirmDiscard) }) {
                    Text(text = "Bỏ ảnh", color = colors.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(ComposerAction.OnDismissDiscardDialog) }) {
                    Text(text = "Tiếp tục chỉnh sửa", color = colors.textPrimary)
                }
            },
            containerColor = colors.surface,
            shape = LokapTheme.shapes.dialog
        )
    }
}

/** Tiêu đề giữa + vị trí sẽ gắn vào bài ở góc phải (chỗ nút tải ảnh của màn mẫu). */
@Composable
private fun ComposerTopBar(
    locationLabel: String,
    isLandmark: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    Box(modifier = modifier.height(52.dp)) {
        Text(
            text = "Gửi đến…",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            modifier = Modifier.align(Alignment.Center)
        )
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .widthIn(max = if (isLandmark) 136.dp else 112.dp)
                .clip(CircleShape)
                .background(colors.surfaceHigh)
                .border(1.dp, if (isLandmark) colors.accentYellow else colors.outline, CircleShape)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isLandmark) Icons.Rounded.Star else Icons.Rounded.LocationOn,
                contentDescription = if (isLandmark) "Địa danh" else "Vị trí",
                tint = if (isLandmark) colors.accentYellowStrong else colors.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = locationLabel,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Chấm trang dưới ảnh: mỗi chấm là một khung/dấu chìm. */
@Composable
private fun PageDots(count: Int, current: Int) {
    val colors = LokapTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(12.dp)
    ) {
        repeat(count) { i ->
            val active = i == current
            val dotSize by animateDpAsState(if (active) 9.dp else 6.dp, label = "dot_size")
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(if (active) colors.primary else colors.textTertiary.copy(alpha = 0.6f))
            )
        }
    }
}

/**
 * Popup chọn khung/dấu chìm: lưới 3×3 nổi ngay trên hàng nút, chạm ra ngoài hoặc Back để đóng.
 * Màu hồng chủ đạo như vòng bộ lọc: icon hồng, đang chọn thì nền hồng.
 */
@Composable
private fun StampPickerPopup(
    selected: PhotoWatermarkStamp,
    onSelect: (PhotoWatermarkStamp) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LokapTheme.colors
    val density = LocalDensity.current
    // Đáy popup cách mép trên hàng nút (cao 96dp) 12dp
    val offsetY = with(density) { -(96.dp + 12.dp).roundToPx() }
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }

    Popup(
        alignment = Alignment.BottomCenter,
        offset = IntOffset(0, offsetY),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(tween(140)) + scaleIn(
                initialScale = 0.9f,
                transformOrigin = TransformOrigin(0.5f, 1f)
            )
        ) {
            Column(
                modifier = Modifier
                    .shadow(elevation = 12.dp, shape = LokapTheme.shapes.cameraFrame)
                    .clip(LokapTheme.shapes.cameraFrame)
                    .background(colors.surface)
                    .border(1.dp, colors.outline, LokapTheme.shapes.cameraFrame)
                    .padding(12.dp)
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PhotoWatermarkStamp.entries.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { stamp ->
                            StampPickerCell(
                                stamp = stamp,
                                selected = stamp == selected,
                                onClick = { onSelect(stamp) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StampPickerCell(
    stamp: PhotoWatermarkStamp,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LokapTheme.colors
    Column(
        modifier = Modifier
            .width(92.dp)
            .clip(LokapTheme.shapes.card)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (selected) colors.primary else colors.surfaceHigh)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) colors.primary else colors.primary.copy(alpha = 0.35f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = stamp.icon,
                contentDescription = null,
                tint = if (selected) colors.onPrimary else colors.primary,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = stamp.labelVi,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (selected) colors.primary else colors.textSecondary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            minLines = 2
        )
    }
}

/** Nút vuông bo góc 68dp, cùng cỡ và vị trí với nút flash / lật camera. */
@Composable
private fun ComposerSideButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    val colors = LokapTheme.colors
    val pill = LokapTheme.shapes.pill
    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(pill)
            .background(colors.surfaceHigh)
            .border(1.dp, colors.outline, pill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = colors.textPrimary,
            modifier = Modifier.size(34.dp)
        )
    }
}

/** Nút gửi tròn 96dp, cùng hình với nút chụp để chuyển màn liền mạch. */
@Composable
private fun SendButton(
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit
) {
    val colors = LokapTheme.colors
    Box(
        modifier = Modifier
            .size(96.dp)
            .alpha(if (enabled || loading) 1f else 0.45f)
            .clip(CircleShape)
            .border(4.8.dp, colors.textPrimary, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Đăng" },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(77.dp)
                .clip(CircleShape)
                .background(colors.primary),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(36.dp)
                        .offset(x = 2.dp)
                )
            }
        }
    }
}

/** Chọn người xem như hàng người nhận của màn mẫu: vòng tròn + tên, đang chọn có viền hồng. */
@Composable
private fun AudiencePicker(
    audience: Audience,
    onSelectAudience: (Audience) -> Unit,
    modifier: Modifier = Modifier,
    /** Ảnh địa danh luôn công khai: mục Bạn bè mờ đi. */
    lockedPublic: Boolean = false
) {
    Row(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally)
    ) {
        AudienceOption(
            icon = Icons.Rounded.Group,
            label = "Bạn bè",
            selected = audience == Audience.FRIENDS,
            onClick = { onSelectAudience(Audience.FRIENDS) },
            modifier = if (lockedPublic) Modifier.alpha(0.35f) else Modifier
        )
        AudienceOption(
            icon = Icons.Default.Public,
            label = "Công khai",
            selected = audience == Audience.PUBLIC,
            onClick = { onSelectAudience(Audience.PUBLIC) }
        )
    }
}

@Composable
private fun AudienceOption(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    Column(
        modifier = modifier
            .clip(LokapTheme.shapes.card)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .border(
                    width = if (selected) 3.dp else 1.5.dp,
                    color = if (selected) colors.primary else colors.outline,
                    shape = CircleShape
                )
                .padding(5.dp)
                .clip(CircleShape)
                .background(colors.surfaceHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) colors.primary else colors.textPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            color = if (selected) colors.primary else colors.textSecondary
        )
    }
}

/** Ảnh vuông kèm khung/dấu chìm (một trang của pager). */
@Composable
private fun FramedPhoto(
    stamp: PhotoWatermarkStamp,
    imageModel: Any,
    showImage: Boolean,
    provinceName: String,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors

    @Composable
    fun Photo() {
        if (showImage) {
            AsyncImage(
                model = imageModel,
                contentDescription = "Ảnh đăng",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    Box(modifier = modifier.background(colors.surface)) {
        when (stamp) {
            PhotoWatermarkStamp.ART_POLAROID -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFDFBF7))
                        .padding(top = 10.dp, start = 10.dp, end = 10.dp, bottom = 44.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.surfaceHigh)
                    ) {
                        Photo()
                    }
                }
                Text(
                    text = "Khoảnh khắc tại ${provinceName.ifBlank { "Việt Nam" }} · " +
                        LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFF64748B),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp)
                )
            }

            PhotoWatermarkStamp.KODAK_FILM_35MM -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F172A))
                ) {
                    FilmSprockets()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .background(colors.surfaceHigh)
                    ) {
                        Photo()
                        Text(
                            text = "KODAK GOLD 400 · ▶ 24A",
                            color = Color(0xFFF59E0B),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                        )
                    }
                    FilmSprockets()
                }
            }

            else -> {
                Photo()
                // Dấu chìm nằm phía trên để nhường đáy ảnh cho chú thích
                WatermarkStampOverlay(
                    stamp = stamp,
                    provinceName = provinceName,
                    modifier = Modifier.align(
                        when (stamp) {
                            PhotoWatermarkStamp.VIETNAM_PRIDE -> Alignment.TopCenter
                            PhotoWatermarkStamp.LOKAP_MINIMAL -> Alignment.TopStart
                            else -> Alignment.TopEnd
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun FilmSprockets() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(8) {
            Box(
                modifier = Modifier
                    .size(width = 12.dp, height = 8.dp)
                    .background(Color(0xFF334155), RoundedCornerShape(2.dp))
            )
        }
    }
}

/**
 * Ô chú thích trên ảnh: viên thuốc nền tối mờ, chữ trắng căn giữa, co giãn theo chữ,
 * tối đa [POST_CAPTION_MAX_LENGTH] ký tự, 2 dòng.
 */
@Composable
private fun PhotoCaptionField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val textStyle = MaterialTheme.typography.bodyMedium.copy(
        color = Color.White,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center
    )

    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.take(POST_CAPTION_MAX_LENGTH)) },
        textStyle = textStyle,
        cursorBrush = SolidColor(Color.White),
        maxLines = 2,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = "Thêm chú thích",
                        style = textStyle.copy(color = Color.White.copy(alpha = 0.8f)),
                        maxLines = 1
                    )
                }
                innerTextField()
            }
        }
    )
}

val PhotoWatermarkStamp.icon: ImageVector
    get() = when (this) {
        PhotoWatermarkStamp.NONE -> Icons.Rounded.Close
        PhotoWatermarkStamp.LOKAP_MINIMAL -> Icons.Rounded.Place
        PhotoWatermarkStamp.RETRO_LED_DATE -> Icons.Rounded.Schedule
        PhotoWatermarkStamp.TRAVEL_PASSPORT -> Icons.Rounded.FlightTakeoff
        PhotoWatermarkStamp.ART_POLAROID -> Icons.Rounded.CropPortrait
        PhotoWatermarkStamp.KODAK_FILM_35MM -> Icons.Rounded.MovieFilter
        PhotoWatermarkStamp.VHS_CAMCORDER -> Icons.Rounded.Videocam
        PhotoWatermarkStamp.MEMORY_RECEIPT -> Icons.AutoMirrored.Rounded.ReceiptLong
        PhotoWatermarkStamp.VIETNAM_PRIDE -> Icons.Rounded.Star
    }

@Composable
private fun WatermarkStampOverlay(
    stamp: PhotoWatermarkStamp,
    provinceName: String,
    modifier: Modifier = Modifier
) {
    val displayCity = provinceName.ifBlank { "Việt Nam" }
    val currentDate = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }
    val digitalDate = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("yy  MM  dd"))
    }

    when (stamp) {
        PhotoWatermarkStamp.NONE -> Unit

        PhotoWatermarkStamp.LOKAP_MINIMAL -> {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.35f)),
                modifier = modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    LokapLogo(
                        size = 14.dp,
                        showText = false
                    )
                    Text(
                        text = "Lokap · $displayCity · $currentDate",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        PhotoWatermarkStamp.RETRO_LED_DATE -> {
            Box(
                modifier = modifier.padding(14.dp)
            ) {
                Text(
                    text = "'$digitalDate",
                    color = Color(0xFFFF9E0B),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        shadow = Shadow(
                            color = Color(0xFFFF9E0B).copy(alpha = 0.6f),
                            offset = Offset(0f, 0f),
                            blurRadius = 6f
                        )
                    )
                )
            }
        }

        PhotoWatermarkStamp.TRAVEL_PASSPORT -> {
            Box(
                modifier = modifier
                    .padding(14.dp)
                    .rotate(-8f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.15f),
                    border = BorderStroke(1.8.dp, Color(0xFFE11D48).copy(alpha = 0.85f)),
                    modifier = Modifier.size(92.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                            .border(0.8.dp, Color(0xFFE11D48).copy(alpha = 0.65f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "★ LOKAP ★",
                                color = Color(0xFFE11D48),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ĐÃ KHÁM PHÁ",
                                color = Color(0xFFE11D48),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = displayCity.uppercase(),
                                color = Color(0xFFE11D48),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = currentDate,
                                color = Color(0xFFE11D48),
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }


        PhotoWatermarkStamp.VHS_CAMCORDER -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "● REC",
                        color = Color(0xFFEF4444),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "SP  [▮▮▮▯]",
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = currentDate,
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        PhotoWatermarkStamp.MEMORY_RECEIPT -> {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = modifier
                    .padding(12.dp)
                    .width(135.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "LOKAP MOMENT",
                        color = Color(0xFF0F172A),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "$displayCity · $currentDate",
                        color = Color(0xFF64748B),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFE2E8F0))
                    )
                    Text(
                        text = "||||| ||| |||| || |||||",
                        color = Color(0xFF334155),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        PhotoWatermarkStamp.VIETNAM_PRIDE -> {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFB91C1C).copy(alpha = 0.90f),
                border = BorderStroke(1.dp, Color(0xFFFBBF24)),
                modifier = modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Tự hào Việt Nam · $displayCity",
                        color = Color(0xFFFEF3C7),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        PhotoWatermarkStamp.ART_POLAROID,
        PhotoWatermarkStamp.KODAK_FILM_35MM -> Unit
    }
}
