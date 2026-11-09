package vn.nhom2.lokap.feature.auth.setup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import vn.nhom2.lokap.core.designsystem.component.*
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.core.navigation.navigateClearingBackStack
import vn.nhom2.lokap.data.fake.ProvinceCatalog
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

@Composable
fun SetupProfileScreen(
    uiState: SetupProfileUiState,
    onAction: (SetupProfileAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val focusManager = LocalFocusManager.current

    var displayNameHadFocus by remember { mutableStateOf(false) }
    var usernameHadFocus by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.lg, vertical = spacing.md)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LokapLogo(size = 28.dp, showText = true)

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.primary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.35f))
            ) {
                Text(
                    text = "Bước 2/2: Hồ sơ cá nhân",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.md))

        if (uiState.isOffline) {
            OfflineBanner(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(spacing.sm))
        }

        Text(
            text = "Thiết lập hồ sơ Lokap",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(spacing.xs))

        Text(
            text = "Tạo dấu ấn cá nhân của bạn trên bản đồ khoảnh khắc.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(spacing.lg))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                AvatarRing(
                    avatarUrl = uiState.selectedAvatarUrl,
                    displayName = uiState.displayName.ifEmpty { "Bạn" },
                    size = 88.dp
                )

                Surface(
                    shape = CircleShape,
                    color = colors.primary,
                    border = BorderStroke(2.dp, colors.background),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = "Đổi ảnh đại diện",
                            tint = colors.buttonText,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.sm))

            Text(
                text = "Chọn hình đại diện nhanh:",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(spacing.xs))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                items(uiState.presetAvatars) { avatarUrl ->
                    val isSelected = uiState.selectedAvatarUrl == avatarUrl
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) colors.primary else colors.outline.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickable { onAction(SetupProfileAction.AvatarSelected(avatarUrl)) }
                    ) {
                        AvatarRing(
                            avatarUrl = avatarUrl,
                            displayName = uiState.displayName.ifEmpty { "Bạn" },
                            size = 46.dp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.lg))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.md)
            ) {

                Column {
                    OutlinedTextField(
                        value = uiState.displayName,
                        onValueChange = { onAction(SetupProfileAction.DisplayNameChanged(it)) },
                        label = { Text("Họ và tên") },
                        placeholder = { Text("ví dụ: Nguyễn Minh Khôi") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = if (uiState.displayNameError != null) colors.error else colors.primary
                            )
                        },
                        isError = uiState.displayNameError != null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        colors = lokapTextFieldColors(),
                        shape = RoundedCornerShape(14.dp),
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
                    if (uiState.displayNameError != null) {
                        Text(
                            text = uiState.displayNameError,
                            color = colors.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                        )
                    }
                }

                Column {
                    OutlinedTextField(
                        value = uiState.username,
                        onValueChange = { onAction(SetupProfileAction.UsernameChanged(it)) },
                        label = { Text("Tên người dùng (@username)") },
                        placeholder = { Text("ví dụ: minhkhoi") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.AlternateEmail,
                                contentDescription = null,
                                tint = if (uiState.usernameError != null) colors.error else colors.primary
                            )
                        },
                        trailingIcon = {
                            when (uiState.usernameAvailability) {
                                UsernameAvailability.CHECKING -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = colors.primary
                                    )
                                }
                                UsernameAvailability.AVAILABLE -> {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Hợp lệ",
                                        tint = Color(0xFF10B981)
                                    )
                                }
                                UsernameAvailability.TAKEN, UsernameAvailability.INVALID -> {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Không hợp lệ",
                                        tint = colors.error
                                    )
                                }
                                UsernameAvailability.IDLE -> Unit
                            }
                        },
                        isError = uiState.usernameError != null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Next
                        ),
                        colors = lokapTextFieldColors(),
                        shape = RoundedCornerShape(14.dp),
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
                    if (uiState.usernameError != null) {
                        Text(
                            text = uiState.usernameError,
                            color = colors.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                        )
                    }
                }

                Column {
                    OutlinedTextField(
                        value = if (uiState.birthYear > 0) uiState.birthYear.toString() else "",
                        onValueChange = { str ->
                            val year = str.filter { it.isDigit() }.take(4).toIntOrNull() ?: 0
                            onAction(SetupProfileAction.BirthYearChanged(year))
                        },
                        label = { Text("Năm sinh") },
                        placeholder = { Text("ví dụ: 2004") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Cake,
                                contentDescription = null,
                                tint = if (uiState.birthYearError != null) colors.error else colors.primary
                            )
                        },
                        isError = uiState.birthYearError != null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        colors = lokapTextFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (uiState.birthYearError != null) {
                        Text(
                            text = uiState.birthYearError,
                            color = colors.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                        )
                    } else {
                        Text(
                            text = "Yêu cầu đủ 13 tuổi trở lên theo quy định an toàn",
                            color = colors.textTertiary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                        )
                    }
                }

                Column {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = uiState.homeProvince,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tỉnh / Thành phố sinh sống") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = colors.primary
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { onAction(SetupProfileAction.ToggleProvincePicker) }) {
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardArrowDown,
                                        contentDescription = "Chọn tỉnh thành",
                                        tint = colors.textSecondary
                                    )
                                }
                            },
                            colors = lokapTextFieldColors(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAction(SetupProfileAction.ToggleProvincePicker) }
                        )

                        DropdownMenu(
                            expanded = uiState.isProvincePickerOpen,
                            onDismissRequest = { onAction(SetupProfileAction.ToggleProvincePicker) },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(260.dp)
                                .background(colors.surface)
                        ) {
                            ProvinceCatalog.allProvinces.forEach { province ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = province.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (province.name == uiState.homeProvince) colors.primary else colors.textPrimary,
                                            fontWeight = if (province.name == uiState.homeProvince) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        onAction(SetupProfileAction.HomeProvinceChanged(province.name))
                                    }
                                )
                            }
                        }
                    }
                }

                Column {
                    OutlinedTextField(
                        value = uiState.bio,
                        onValueChange = { onAction(SetupProfileAction.BioChanged(it.take(60))) },
                        label = { Text("Giới thiệu bản thân (Bio)") },
                        placeholder = { Text("Chia sẻ đôi lời về bạn…") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = colors.primary
                            )
                        },
                        isError = uiState.bioError != null,
                        maxLines = 2,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            if (uiState.isFormValid) onAction(SetupProfileAction.SubmitClicked)
                        }),
                        colors = lokapTextFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (uiState.bioError != null) {
                            Text(
                                text = uiState.bioError,
                                color = colors.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }
                        Text(
                            text = "${uiState.bio.length}/60",
                            color = colors.textTertiary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        if (uiState.serverError != null) {
            Spacer(modifier = Modifier.height(spacing.sm))
            ErrorState(
                message = uiState.serverError,
                onRetry = { onAction(SetupProfileAction.RetryClicked) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(spacing.lg))

        PinkButton(
            text = "Hoàn tất & Bắt đầu",
            onClick = { onAction(SetupProfileAction.SubmitClicked) },
            loading = uiState.isSubmitting,
            enabled = uiState.isFormValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )

        Spacer(modifier = Modifier.height(spacing.xl))
    }
}
