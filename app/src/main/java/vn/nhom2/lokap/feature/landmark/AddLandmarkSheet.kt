package vn.nhom2.lokap.feature.landmark

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import vn.nhom2.lokap.core.designsystem.component.GlassChip
import vn.nhom2.lokap.core.designsystem.component.PinkButton
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.domain.model.Place
import vn.nhom2.lokap.domain.repository.LandmarkRepository

/** Bảng "Thêm địa danh" mở từ Bản đồ. Lưu xong (hoặc địa điểm đã là địa danh) thì mở màn Địa danh. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLandmarkSheet(
    onDismiss: () -> Unit,
    onOpenLandmark: (landmarkId: String) -> Unit,
    viewModel: AddLandmarkViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(viewModel) { viewModel.reset() }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AddLandmarkEffect.OpenLandmark -> onOpenLandmark(effect.landmarkId)
                is AddLandmarkEffect.ShowMessage -> message = effect.message
            }
        }
    }

    val colors = LokapTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        shape = LokapTheme.shapes.sheet
    ) {
        AddLandmarkContent(
            state = state,
            message = message,
            onAction = { action ->
                message = null
                viewModel.onAction(action)
            }
        )
    }
}

@Composable
private fun AddLandmarkContent(
    state: AddLandmarkUiState,
    message: String?,
    onAction: (AddLandmarkAction) -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val selected = state.selected

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = spacing.lg)
            .padding(bottom = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            if (selected != null) {
                IconButton(onClick = { onAction(AddLandmarkAction.OnBackToSearch) }) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Chọn địa điểm khác",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            } else {
                Icon(Icons.Rounded.Star, contentDescription = null, tint = colors.accentYellow, modifier = Modifier.size(28.dp))
            }
            Text(
                text = "Thêm địa danh",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        }

        if (selected == null) {
            SearchStep(state = state, onAction = onAction)
        } else {
            DetailsStep(state = state, place = selected, onAction = onAction)
        }

        if (message != null) {
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = colors.error)
        }
    }
}

@Composable
private fun SearchStep(state: AddLandmarkUiState, onAction: (AddLandmarkAction) -> Unit) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    LandmarkTextField(
        value = state.query,
        onValueChange = { onAction(AddLandmarkAction.OnQueryChange(it)) },
        placeholder = "Tìm địa điểm",
        leading = {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.primary, modifier = Modifier.size(24.dp))
        },
        trailing = {
            when {
                state.isSearching || state.isResolving ->
                    CircularProgressIndicator(color = colors.primary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                state.query.isNotEmpty() -> IconButton(onClick = { onAction(AddLandmarkAction.OnQueryChange("")) }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Xoá", tint = colors.textSecondary, modifier = Modifier.size(22.dp))
                }
            }
        },
        imeAction = ImeAction.Search
    )

    state.searchError?.let {
        Text(text = it, style = MaterialTheme.typography.bodyMedium, color = colors.error)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        items(items = state.results, key = { it.id.ifBlank { it.name + it.address } }) { place ->
            PlaceRow(place = place, enabled = !state.isResolving, onClick = { onAction(AddLandmarkAction.OnPickPlace(place)) })
        }
    }
}

@Composable
private fun PlaceRow(place: Place, enabled: Boolean, onClick: () -> Unit) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(LokapTheme.shapes.card)
            .clickable(enabled = enabled, onClick = onClick, role = Role.Button)
            .padding(horizontal = spacing.sm, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = colors.primary, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = place.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (place.address.isNotBlank()) {
                Text(
                    text = place.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DetailsStep(state: AddLandmarkUiState, place: Place, onAction: (AddLandmarkAction) -> Unit) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LokapTheme.shapes.card)
            .background(colors.surfaceHigh)
            .border(1.dp, colors.outline, LokapTheme.shapes.card)
            .padding(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.accentYellow.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Star, contentDescription = null, tint = colors.accentYellow, modifier = Modifier.size(26.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = place.address.ifBlank { place.name },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            state.region?.let { GlassChip(label = it.provinceName, leadingIcon = Icons.Rounded.LocationOn, selected = true) }
        }
    }

    LandmarkTextField(
        value = state.name,
        onValueChange = { onAction(AddLandmarkAction.OnNameChange(it)) },
        placeholder = "Tên địa danh",
        imeAction = ImeAction.Next,
        isError = state.name.isNotEmpty() && state.name.trim().length < 2
    )

    LandmarkTextField(
        value = state.description,
        onValueChange = { onAction(AddLandmarkAction.OnDescriptionChange(it)) },
        placeholder = "Mô tả ngắn (không bắt buộc)",
        imeAction = ImeAction.Done,
        singleLine = false,
        supporting = "${state.description.length}/${LandmarkRepository.DESCRIPTION_MAX_LENGTH}"
    )

    PinkButton(
        text = "Thêm",
        onClick = { onAction(AddLandmarkAction.OnSave) },
        enabled = state.canSave,
        loading = state.isSaving,
        leadingIcon = Icons.Rounded.Star,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun LandmarkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    imeAction: ImeAction,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    isError: Boolean = false,
    supporting: String? = null
) {
    val colors = LokapTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = colors.textTertiary) },
        leadingIcon = leading,
        trailingIcon = trailing,
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 3,
        isError = isError,
        supportingText = supporting?.let { { Text(it, color = colors.textTertiary) } },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = imeAction),
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.outline,
            focusedContainerColor = colors.surfaceHigh,
            unfocusedContainerColor = colors.surfaceHigh,
            cursorColor = colors.primary,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
