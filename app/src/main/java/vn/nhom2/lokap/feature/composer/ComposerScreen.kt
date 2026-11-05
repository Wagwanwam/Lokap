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
import vn.nhom2.lokap.core.navigation.navigateSingleTop
import vn.nhom2.lokap.core.navigation.popBackStackSafely
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
