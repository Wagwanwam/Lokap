package vn.nhom2.lokap.feature.composer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.CropPortrait
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FilterVintage
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MovieFilter
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.WbSunny
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
fun NavGraphBuilder.composerScreen(navController: NavController) {
    composable<Route.Composer> { backStackEntry ->
        val route = backStackEntry.toRoute<Route.Composer>()
        ComposerRoute(
            imagePath = route.imagePath,
            onNavigateBack = { navController.popBackStackSafely() }
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
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {

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
                text = "Tạo bài viết",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            PinkButton(
                text = "Đăng",
                leadingIcon = Icons.AutoMirrored.Rounded.Send,
                enabled = state.canSend,
                loading = state.isSubmitting,
                onClick = { onAction(ComposerAction.OnSendClick) }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(LokapTheme.shapes.card)
                .background(colors.surface)
                .border(1.dp, colors.primary.copy(alpha = 0.5f), LokapTheme.shapes.card)
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.FlashOn,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Chuỗi ${state.streakCount} ngày liên tiếp",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                Text(
                    text = "Đăng ảnh hôm nay để tiếp tục chuỗi khoảnh khắc!",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
            }
        }

        val activeStamp = when {
            state.selectedStamp != PhotoWatermarkStamp.NONE -> state.selectedStamp
            state.selectedTemplate == PostFilterTemplate.POLAROID -> PhotoWatermarkStamp.ART_POLAROID
            state.selectedTemplate == PostFilterTemplate.FILM_FRAME -> PhotoWatermarkStamp.KODAK_FILM_35MM
            state.selectedTemplate == PostFilterTemplate.DATE_STAMP -> PhotoWatermarkStamp.RETRO_LED_DATE
            else -> PhotoWatermarkStamp.NONE
        }

        val activeColorFilter = when {
            state.selectedColorFilter != PhotoColorFilter.ORIGINAL -> state.selectedColorFilter
            state.selectedTemplate == PostFilterTemplate.VINTAGE -> PhotoColorFilter.NOSTALGIA
            else -> PhotoColorFilter.ORIGINAL
        }

        val imageColorFilter = remember(activeColorFilter) {
            getColorFilter(activeColorFilter)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(1.5.dp, colors.outline, RoundedCornerShape(16.dp))
        ) {
            when (activeStamp) {
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
                            if (!isPreview && state.imagePath.isNotBlank()) {
                                AsyncImage(
                                    model = imageModel,
                                    contentDescription = "Ảnh đăng",
                                    colorFilter = imageColorFilter,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                    Text(
                        text = "Khoảnh khắc tại ${state.currentProvinceName} · " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
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
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .background(colors.surfaceHigh)
                        ) {
                            if (!isPreview && state.imagePath.isNotBlank()) {
                                AsyncImage(
                                    model = imageModel,
                                    contentDescription = "Ảnh đăng",
                                    colorFilter = imageColorFilter,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Text(
                                text = "KODAK GOLD 400 · ▶ 24A",
                                color = Color(0xFFF59E0B),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(6.dp)
                            )
                        }

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
                }

                else -> {

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (!isPreview && state.imagePath.isNotBlank()) {
                            AsyncImage(
                                model = imageModel,
                                contentDescription = "Ảnh đăng",
                                colorFilter = imageColorFilter,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        WatermarkStampOverlay(
                            stamp = activeStamp,
                            provinceName = state.currentProvinceName,
                            modifier = Modifier.align(
                                when (activeStamp) {
                                    PhotoWatermarkStamp.TRAVEL_PASSPORT -> Alignment.TopEnd
                                    PhotoWatermarkStamp.RETRO_LED_DATE -> Alignment.BottomEnd
                                    PhotoWatermarkStamp.MEMORY_RECEIPT -> Alignment.BottomEnd
                                    PhotoWatermarkStamp.WEATHER_REPORT -> Alignment.TopEnd
                                    PhotoWatermarkStamp.VIETNAM_PRIDE -> Alignment.BottomCenter
                                    else -> Alignment.BottomStart
                                }
                            )
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Bộ lọc màu",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = colors.textSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PhotoColorFilter.entries.forEach { filter ->
                        val isSelected = activeColorFilter == filter
                        Surface(
                            shape = LokapTheme.shapes.pill,
                            color = if (isSelected) colors.primary.copy(alpha = 0.15f) else colors.surface,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 0.8.dp,
                                color = if (isSelected) colors.primary else colors.outline
                            ),
                            modifier = Modifier
                                .clip(LokapTheme.shapes.pill)
                                .clickable { onAction(ComposerAction.OnSelectColorFilter(filter)) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = filter.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) colors.primary else colors.textSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = filter.labelVi,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) colors.primary else colors.textPrimary
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Khung ảnh & Dấu chìm",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = colors.textSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PhotoWatermarkStamp.entries.forEach { stamp ->
                        val isSelected = activeStamp == stamp
                        Surface(
                            shape = LokapTheme.shapes.pill,
                            color = if (isSelected) colors.primary.copy(alpha = 0.15f) else colors.surface,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 0.8.dp,
                                color = if (isSelected) colors.primary else colors.outline
                            ),
                            modifier = Modifier
                                .clip(LokapTheme.shapes.pill)
                                .clickable { onAction(ComposerAction.OnSelectStamp(stamp)) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = stamp.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) colors.primary else colors.textSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = stamp.labelVi,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) colors.primary else colors.textPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(LokapTheme.shapes.card)
                .background(colors.surface)
                .border(1.dp, colors.outline, LokapTheme.shapes.card)
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(64.dp)) {
                if (state.caption.isEmpty()) {
                    Text(
                        text = "Viết chú thích cho khoảnh khắc này…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textTertiary
                    )
                }
                BasicTextField(
                    value = state.caption,
                    onValueChange = { onAction(ComposerAction.OnCaptionChange(it)) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.primary),
                    modifier = Modifier.fillMaxSize()
                )
            }
            Text(
                text = "${state.caption.length}/60",
                style = MaterialTheme.typography.labelSmall,
                color = if (state.caption.length >= 60) colors.primary else colors.textSecondary,
                modifier = Modifier.align(Alignment.End)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            ComposerAudienceSelector(
                audience = state.audience,
                onSelectAudience = { onAction(ComposerAction.OnSelectAudience(it)) }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${state.currentWardName}, ${state.currentProvinceName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
            }
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
            text = {
                Text(
                    text = "Ảnh và lời nhắn bạn vừa nhập sẽ không được lưu lại.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
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

val PhotoColorFilter.icon: ImageVector
    get() = when (this) {
        PhotoColorFilter.ORIGINAL -> Icons.Rounded.Tune
        PhotoColorFilter.VIVID -> Icons.Rounded.Palette
        PhotoColorFilter.WARM_GOLDEN -> Icons.Rounded.WbSunny
        PhotoColorFilter.COOL_BREEZE -> Icons.Rounded.Air
        PhotoColorFilter.NOSTALGIA -> Icons.Rounded.FilterVintage
        PhotoColorFilter.MONO_CHIC -> Icons.Rounded.Contrast
        PhotoColorFilter.DREAMY_PASTEL -> Icons.Rounded.AutoAwesome
        PhotoColorFilter.CINEMATIC -> Icons.Rounded.Movie
    }

val PhotoWatermarkStamp.icon: ImageVector
    get() = when (this) {
        PhotoWatermarkStamp.NONE -> Icons.Rounded.Close
        PhotoWatermarkStamp.LOKAP_MINIMAL -> Icons.Rounded.Place
        PhotoWatermarkStamp.RETRO_LED_DATE -> Icons.Rounded.Schedule
        PhotoWatermarkStamp.TRAVEL_PASSPORT -> Icons.Rounded.FlightTakeoff
        PhotoWatermarkStamp.ART_POLAROID -> Icons.Rounded.CropPortrait
        PhotoWatermarkStamp.KODAK_FILM_35MM -> Icons.Rounded.MovieFilter
        PhotoWatermarkStamp.GPS_COORDINATES -> Icons.Rounded.Explore
        PhotoWatermarkStamp.WEATHER_REPORT -> Icons.Rounded.WbSunny
        PhotoWatermarkStamp.VHS_CAMCORDER -> Icons.Rounded.Videocam
        PhotoWatermarkStamp.VINYL_MUSIC -> Icons.Rounded.MusicNote
        PhotoWatermarkStamp.MEMORY_RECEIPT -> Icons.AutoMirrored.Rounded.ReceiptLong
        PhotoWatermarkStamp.VIETNAM_PRIDE -> Icons.Rounded.Star
    }

private fun getColorFilter(filter: PhotoColorFilter): ColorFilter? {
    val matrix = when (filter) {
        PhotoColorFilter.ORIGINAL -> return null
        PhotoColorFilter.VIVID -> ColorMatrix().apply { setToSaturation(1.4f) }
        PhotoColorFilter.WARM_GOLDEN -> ColorMatrix(
            floatArrayOf(
                1.12f, 0f, 0f, 0f, 15f,
                0f, 1.02f, 0f, 0f, 8f,
                0f, 0f, 0.88f, 0f, -10f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        PhotoColorFilter.COOL_BREEZE -> ColorMatrix(
            floatArrayOf(
                0.92f, 0f, 0f, 0f, -5f,
                0f, 1.02f, 0f, 0f, 5f,
                0f, 0f, 1.18f, 0f, 18f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        PhotoColorFilter.NOSTALGIA -> ColorMatrix(
            floatArrayOf(
                0.95f, 0.05f, 0.05f, 0f, 20f,
                0.05f, 0.90f, 0.05f, 0f, 15f,
                0.05f, 0.05f, 0.75f, 0f, -10f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        PhotoColorFilter.MONO_CHIC -> ColorMatrix().apply { setToSaturation(0f) }
        PhotoColorFilter.DREAMY_PASTEL -> ColorMatrix(
            floatArrayOf(
                1.08f, 0f, 0f, 0f, 16f,
                0f, 0.98f, 0f, 0f, 10f,
                0f, 0f, 1.10f, 0f, 16f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        PhotoColorFilter.CINEMATIC -> ColorMatrix(
            floatArrayOf(
                1.16f, 0f, 0f, 0f, -8f,
                0f, 1.05f, 0f, 0f, 2f,
                0f, 0f, 1.22f, 0f, 14f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }
    return ColorFilter.colorMatrix(matrix)
}

@Composable
private fun WatermarkStampOverlay(
    stamp: PhotoWatermarkStamp,
    provinceName: String,
    modifier: Modifier = Modifier
) {
    val displayCity = provinceName.ifBlank { "Đà Nẵng" }
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
                        fontSize = 11.sp,
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
                    text = "'$digitalDate  17:30",
                    color = Color(0xFFFF9E0B),
                    fontSize = 17.sp,
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
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ĐÃ KHÁM PHÁ",
                                color = Color(0xFFE11D48),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = displayCity.uppercase(),
                                color = Color(0xFFE11D48),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = currentDate,
                                color = Color(0xFFE11D48),
                                fontSize = 7.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        PhotoWatermarkStamp.GPS_COORDINATES -> {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.65f),
                border = BorderStroke(0.8.dp, Color(0xFF10B981).copy(alpha = 0.6f)),
                modifier = modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "16°04'N 108°13'E · Alt 18m · $displayCity",
                        color = Color(0xFFE2E8F0),
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        PhotoWatermarkStamp.WEATHER_REPORT -> {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.55f),
                border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.35f)),
                modifier = modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WbSunny,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "28°C · Nắng đẹp · $displayCity",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        PhotoWatermarkStamp.VHS_CAMCORDER -> {
            Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "● REC",
                        color = Color(0xFFEF4444),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "SP  [▮▮▮▯]",
                        color = Color.White,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "00:14:28",
                        color = Color.White,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "NOV 09 2026",
                        color = Color.White,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        PhotoWatermarkStamp.VINYL_MUSIC -> {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f),
                border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.3f)),
                modifier = modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Album,
                        contentDescription = null,
                        tint = Color(0xFFA855F7),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Bài ca tuổi trẻ - Da LAB",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
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
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "$displayCity · $currentDate",
                        color = Color(0xFF64748B),
                        fontSize = 8.5.sp,
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
                        fontSize = 9.sp,
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
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Tự hào Việt Nam · $displayCity",
                        color = Color(0xFFFEF3C7),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        PhotoWatermarkStamp.ART_POLAROID,
        PhotoWatermarkStamp.KODAK_FILM_35MM -> Unit
    }
}

@Composable
private fun ComposerAudienceSelector(
    audience: Audience,
    onSelectAudience: (Audience) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = LokapTheme.colors

    Box {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.surfaceHigh,
            border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.5f)),
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = if (audience == Audience.FRIENDS) Icons.Rounded.Group else Icons.Default.Public,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = if (audience == Audience.FRIENDS) "Bạn bè" else "Công khai",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    ),
                    color = colors.textPrimary
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = "Chọn đối tượng",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(colors.surface)
                .border(1.dp, colors.outline.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
        ) {
            DropdownMenuItem(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "Bạn bè",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Chỉ bạn bè trên Lokap nhìn thấy",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp
                            ),
                            color = colors.textSecondary
                        )
                    }
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Group,
                        contentDescription = null,
                        tint = if (audience == Audience.FRIENDS) colors.primary else colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (audience == Audience.FRIENDS) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                onClick = {
                    onSelectAudience(Audience.FRIENDS)
                    expanded = false
                }
            )

            DropdownMenuItem(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "Công khai",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Hiện trên bản đồ & bảng tin toàn quốc",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp
                            ),
                            color = colors.textSecondary
                        )
                    }
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = if (audience == Audience.PUBLIC) colors.primary else colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (audience == Audience.PUBLIC) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                onClick = {
                    onSelectAudience(Audience.PUBLIC)
                    expanded = false
                }
            )
        }
    }
}
