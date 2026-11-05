package vn.nhom2.lokap.feature.auth.setup

import vn.nhom2.lokap.core.navigation.navigateClearingBackStack
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import vn.nhom2.lokap.core.designsystem.component.AvatarRing
import vn.nhom2.lokap.core.designsystem.component.ErrorState
import vn.nhom2.lokap.core.designsystem.component.GlassChip
import vn.nhom2.lokap.core.designsystem.component.OfflineBanner
import vn.nhom2.lokap.core.designsystem.component.PinkButton
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.feature.auth.lokapTextFieldColors

fun NavGraphBuilder.setupProfileScreen(navController: NavController) {
    composable<SetupProfileRoute> {
        SetupProfileRoute(
            onNavigateToCamera = {
                navController.navigateClearingBackStack(Route.Camera)
            }
        )
    }
}

@Composable
fun SetupProfileRoute(
    onNavigateToCamera: () -> Unit,
    viewModel: SetupProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SetupProfileEffect.NavigateToCamera -> onNavigateToCamera()
            }
        }
    }

    SetupProfileScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupProfileScreen(
    uiState: SetupProfileUiState,
    onAction: (SetupProfileAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var displayNameHadFocus by remember { mutableStateOf(false) }
    var usernameHadFocus by remember { mutableStateOf(false) }

    val previewName = uiState.displayName.trim().ifEmpty { "Lokap" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LokapTheme.colors.background)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = LokapTheme.spacing.lg, vertical = LokapTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uiState.isOffline) {
            OfflineBanner(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))
        }

        Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))

        Text(
            text = "Tạo chiếc hồ sơ chất riêng 💅",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = LokapTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))

        Text(
            text = "Chọn avatar và đặt chiếc username để hội bạn dễ tìm thấy bạn.",
            style = MaterialTheme.typography.bodyMedium,
            color = LokapTheme.colors.textSecondary
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.lg))

        // Main preview AvatarRing (shows initial if selectedAvatarUrl == null)
        AvatarRing(
            avatarUrl = uiState.selectedAvatarUrl,
            displayName = previewName,
            size = 96.dp,
            hasNewPost = true
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))

        GlassChip(
            label = if (uiState.selectedAvatarUrl == null) {
                "Đang dùng chữ cái đầu"
            } else {
                "Dùng chữ cái đầu"
            },
            selected = uiState.selectedAvatarUrl == null,
            onClick = { onAction(SetupProfileAction.AvatarSelected(null)) }
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.md))

        Text(
            text = "Hoặc chọn nhanh 1 trong 6 avatar mẫu:",
            style = MaterialTheme.typography.labelLarge,
            color = LokapTheme.colors.textSecondary
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
        ) {
            uiState.presetAvatars.forEach { avatarUrl ->
                val isSelected = uiState.selectedAvatarUrl == avatarUrl
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) LokapTheme.colors.primary else LokapTheme.colors.outline,
                            shape = CircleShape
                        )
                        .clickable(enabled = !uiState.isSubmitting) {
                            onAction(SetupProfileAction.AvatarSelected(avatarUrl))
                        }
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AvatarRing(
                        avatarUrl = avatarUrl,
                        displayName = previewName,
                        size = 56.dp,
                        hasNewPost = isSelected,
                        onClick = { onAction(SetupProfileAction.AvatarSelected(avatarUrl)) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(LokapTheme.spacing.xl))

        // Display name (2 - 30 chars)
        OutlinedTextField(
            value = uiState.displayName,
            onValueChange = { onAction(SetupProfileAction.DisplayNameChanged(it)) },
            enabled = !uiState.isSubmitting,
            label = { Text("Tên hiển thị (2–30 ký tự)") },
            placeholder = { Text("Ví dụ: Minh Anh 🍓") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Person, contentDescription = null)
            },
            isError = uiState.displayNameError != null,
            supportingText = {
                if (uiState.displayNameError != null) {
                    Text(text = uiState.displayNameError, color = LokapTheme.colors.error)
                } else {
                    Text(
                        text = "${uiState.displayName.trim().length}/30 ký tự",
                        color = LokapTheme.colors.textTertiary
                    )
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            shape = LokapTheme.shapes.card,
            colors = lokapTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        displayNameHadFocus = true
                    } else if (displayNameHadFocus) {
                        onAction(SetupProfileAction.DisplayNameFocusLost)
                    }
                }
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))

        // Username (3-20 chars, lowercase, debounce 400ms with ✓ / ✗ inside field)
        OutlinedTextField(
            value = uiState.username,
            onValueChange = { onAction(SetupProfileAction.UsernameChanged(it)) },
            enabled = !uiState.isSubmitting,
            label = { Text("Username (3–20 ký tự)") },
            placeholder = { Text("minhanh.2k5") },
            prefix = {
                Text(
                    text = "@",
                    color = LokapTheme.colors.primary,
                    fontWeight = FontWeight.Bold
                )
            },
            trailingIcon = {
                UsernameAvailabilityTrailingIcon(availability = uiState.usernameAvailability)
            },
            isError = uiState.usernameError != null || uiState.usernameAvailability == UsernameAvailability.TAKEN,
            supportingText = {
                when {
                    uiState.usernameError != null -> {
                        Text(text = uiState.usernameError, color = LokapTheme.colors.error)
                    }

                    uiState.usernameAvailability == UsernameAvailability.AVAILABLE -> {
                        Text(
                            text = "✓ Username hợp lệ và chưa ai dùng",
                            color = LokapTheme.colors.success
                        )
                    }

                    uiState.usernameAvailability == UsernameAvailability.CHECKING -> {
                        Text(
                            text = "Đang kiểm tra độ trùng...",
                            color = LokapTheme.colors.textSecondary
                        )
                    }

                    else -> {
                        Text(
                            text = "Chỉ dùng a–z, 0–9, dấu chấm (.) và gạch dưới (_)",
                            color = LokapTheme.colors.textTertiary
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (uiState.isFormValid) {
                        onAction(SetupProfileAction.SubmitClicked)
                    }
                }
            ),
            shape = LokapTheme.shapes.card,
            colors = lokapTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        usernameHadFocus = true
                    } else if (usernameHadFocus) {
                        onAction(SetupProfileAction.UsernameFocusLost)
                    }
                }
        )

        if (uiState.serverError != null) {
            Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))
            ErrorState(
                message = uiState.serverError,
                onRetry = { onAction(SetupProfileAction.RetryClicked) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(LokapTheme.spacing.xl))

        PinkButton(
            text = "Vào Lokap",
            onClick = {
                focusManager.clearFocus()
                onAction(SetupProfileAction.SubmitClicked)
            },
            enabled = uiState.isFormValid,
            loading = uiState.isSubmitting,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun UsernameAvailabilityTrailingIcon(availability: UsernameAvailability) {
    when (availability) {
        UsernameAvailability.IDLE -> Unit
        UsernameAvailability.CHECKING -> {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = LokapTheme.colors.primary
            )
        }

        UsernameAvailability.AVAILABLE -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Hợp lệ",
                tint = LokapTheme.colors.success
            )
        }

        UsernameAvailability.TAKEN, UsernameAvailability.INVALID -> {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Không hợp lệ",
                tint = LokapTheme.colors.error
            )
        }
    }
}

@Preview(showBackground = true, name = "SetupProfile - Available Ready")
@Composable
private fun SetupProfileAvailablePreview() {
    LokapTheme {
        SetupProfileScreen(
            uiState = SetupProfileUiState(
                displayName = "Minh Anh",
                username = "minhanh.2k5",
                usernameAvailability = UsernameAvailability.AVAILABLE
            ),
            onAction = {}
        )
    }
}

@Preview(showBackground = true, name = "SetupProfile - Username Taken")
@Composable
private fun SetupProfileTakenPreview() {
    LokapTheme {
        SetupProfileScreen(
            uiState = SetupProfileUiState(
                displayName = "Lokap Official",
                username = "lokap",
                usernameTouched = true,
                usernameAvailability = UsernameAvailability.TAKEN,
                usernameError = "Username này đã có người dùng"
            ),
            onAction = {}
        )
    }
}
