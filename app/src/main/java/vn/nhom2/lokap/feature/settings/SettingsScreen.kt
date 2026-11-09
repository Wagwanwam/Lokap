package vn.nhom2.lokap.feature.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DataSaverOn
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import vn.nhom2.lokap.core.designsystem.component.*
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.designsystem.theme.ThemeMode
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.core.navigation.navigateClearingBackStack
import vn.nhom2.lokap.core.navigation.popBackStackSafely
import vn.nhom2.lokap.data.fake.SampleSocialData
import vn.nhom2.lokap.domain.model.*
import vn.nhom2.lokap.feature.auth.LoginRoute
fun NavGraphBuilder.settingsScreen(navController: NavController) {
    composable<Route.Settings> {
        SettingsRoute(
            onNavigateBack = { navController.popBackStackSafely() },
            onNavigateToLogin = { navController.navigateClearingBackStack(LoginRoute) }
        )
    }
}

@Composable
fun SettingsRoute(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    val appVersionLabel = remember(context) { resolveAppVersion(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onAction(SettingsAction.UpdateNotificationPermissionState(granted))
        if (!granted) {
            openAppNotificationSettings(context)
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val cameraGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                val locationGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                val notificationGranted = hasPostNotificationsPermission(context)
                viewModel.onAction(
                    SettingsAction.UpdateAllPermissionsState(
                        cameraGranted = cameraGranted,
                        locationGranted = locationGranted,
                        notificationGranted = notificationGranted
                    )
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SettingsEffect.NavigateBack -> onNavigateBack()
                SettingsEffect.NavigateToLogin -> onNavigateToLogin()
                SettingsEffect.RequestNotificationPermissionOrOpenSettings -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        openAppNotificationSettings(context)
                    }
                }
                SettingsEffect.OpenAppSettings -> {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                is SettingsEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    SettingsScreen(
        uiState = uiState.copy(appVersionLabel = appVersionLabel),
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding(),
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.sm, vertical = spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onAction(SettingsAction.NavigateBack) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = colors.textPrimary
                    )
                }
                Text(
                    text = "Cài đặt và quyền riêng tư",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = colors.textPrimary
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = spacing.md,
                end = spacing.md,
                top = spacing.sm,
                bottom = spacing.xxl
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.lg)
        ) {

            item {
                val user = uiState.currentUser ?: SampleSocialData.getCurrentUser()
                SettingsGroup(title = "Tài khoản") {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAction(SettingsAction.OpenEditProfileSheet) }
                            .padding(horizontal = spacing.md, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AvatarRing(
                            avatarUrl = user.avatarUrl,
                            displayName = user.displayName,
                            size = 46.dp,
                            isOnline = uiState.isOnlineStatusVisible
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.displayName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "@${user.username}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = colors.textTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = colors.primary.copy(alpha = 0.12f),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onAction(SettingsAction.OpenEditProfileSheet) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Sửa",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    ),
                                    color = colors.primary
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = colors.outline.copy(alpha = 0.12f),
                        thickness = 0.6.dp,
                        modifier = Modifier.padding(start = 50.dp)
                    )

                    SettingsItemRow(
                        icon = Icons.Rounded.Lock,
                        title = "Đổi mật khẩu",
                        onClick = { onAction(SettingsAction.OpenChangePasswordDialog) }
                    )

                    SettingsItemRow(
                        icon = Icons.Rounded.Phone,
                        title = "Số điện thoại khôi phục",
                        trailingText = uiState.recoveryPhone ?: "Chưa liên kết",
                        onClick = { onAction(SettingsAction.OpenRecoveryPhoneDialog) }
                    )

                    SettingsItemRow(
                        icon = Icons.Rounded.AlternateEmail,
                        title = "Email liên kết",
                        trailingText = "${uiState.linkedEmails.size} email",
                        onClick = { onAction(SettingsAction.OpenLinkedEmailsDialog) },
                        showDivider = false
                    )
                }
            }

            item {
                SettingsGroup(title = "Cài đặt hiển thị") {
                    val themeLabel = when (uiState.themeMode) {
                        ThemeMode.SYSTEM -> "Theo hệ thống"
                        ThemeMode.DARK -> "Tối"
                        ThemeMode.LIGHT -> "Sáng"
                    }
                    SettingsItemRow(
                        icon = Icons.Rounded.Palette,
                        title = "Giao diện màu",
                        trailingText = themeLabel,
                        onClick = { onAction(SettingsAction.OpenThemeDialog) }
                    )

                    SettingsItemRow(
                        icon = Icons.Rounded.DataSaverOn,
                        title = "Tiết kiệm dữ liệu di động",
                        subtitle = "Giảm dung lượng ảnh khi dùng 4G/5G",
                        trailingContent = {
                            Switch(
                                checked = uiState.dataSaverEnabled,
                                onCheckedChange = { onAction(SettingsAction.ToggleDataSaver(it)) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = colors.primary,
                                    uncheckedThumbColor = colors.textSecondary,
                                    uncheckedTrackColor = colors.surfaceHigh
                                )
                            )
                        }
                    )

                    SettingsItemRow(
                        icon = Icons.Rounded.SaveAlt,
                        title = "Tự động lưu ảnh vào máy",
                        subtitle = "Lưu ảnh gốc chụp từ Lokap vào bộ nhớ máy",
                        trailingContent = {
                            Switch(
                                checked = uiState.autoSaveToGallery,
                                onCheckedChange = { onAction(SettingsAction.ToggleAutoSave(it)) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = colors.primary,
                                    uncheckedThumbColor = colors.textSecondary,
                                    uncheckedTrackColor = colors.surfaceHigh
                                )
                            )
                        },
                        showDivider = false
                    )
                }
            }

            item {
                SettingsGroup(title = "Quyền riêng tư & Bản đồ") {
                    SettingsItemRow(
                        icon = Icons.Rounded.LocationOn,
                        title = "Mức vị trí gắn trên ảnh",
                        trailingText = uiState.defaultLocationLevel.labelVi,
                        onClick = { onAction(SettingsAction.OpenLocationLevelDialog) }
                    )

                    SettingsItemRow(
                        icon = Icons.Rounded.Circle,
                        iconTint = Color(0xFF10B981),
                        title = "Trạng thái hoạt động (Online)",
                        subtitle = "Hiển thị chấm xanh khi bạn bè đang xem bản đồ",
                        trailingContent = {
                            Switch(
                                checked = uiState.isOnlineStatusVisible,
                                onCheckedChange = { onAction(SettingsAction.ToggleOnlineStatus(it)) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = colors.primary,
                                    uncheckedThumbColor = colors.textSecondary,
                                    uncheckedTrackColor = colors.surfaceHigh
                                )
                            )
                        }
                    )

                    SettingsItemRow(
                        icon = Icons.Rounded.Block,
                        title = "Danh sách chặn",
                        trailingText = "${uiState.blockedUsers.size} người",
                        onClick = { onAction(SettingsAction.OpenBlockedUsersDialog) },
                        showDivider = false
                    )
                }
            }

            item {
                SettingsGroup(title = "Quyền ứng dụng") {
                    SettingsItemRow(
                        icon = Icons.Rounded.PhotoCamera,
                        title = "Camera",
                        subtitle = "Chụp và chia sẻ khoảnh khắc",
                        trailingContent = {
                            PermissionStatusBadge(isGranted = uiState.hasCameraPermission)
                        },
                        onClick = { onAction(SettingsAction.ClickOpenAppSettings) }
                    )

                    SettingsItemRow(
                        icon = Icons.Rounded.MyLocation,
                        title = "Vị trí GPS",
                        subtitle = "Ghim khoảnh khắc lên bản đồ Việt Nam",
                        trailingContent = {
                            PermissionStatusBadge(isGranted = uiState.hasLocationPermission)
                        },
                        onClick = { onAction(SettingsAction.ClickOpenAppSettings) }
                    )

                    SettingsItemRow(
                        icon = Icons.Rounded.Notifications,
                        title = "Thông báo",
                        subtitle = "Nhận tin nhắn, reaction và ảnh mới từ bạn bè",
                        trailingContent = {
                            PermissionStatusBadge(isGranted = uiState.hasNotificationPermission)
                        },
                        onClick = {
                            if (!uiState.hasNotificationPermission) {
                                onAction(SettingsAction.ClickGrantNotificationPermission)
                            } else {
                                onAction(SettingsAction.ClickOpenAppSettings)
                            }
                        }
                    )

                    SettingsItemRow(
                        icon = Icons.Rounded.Settings,
                        title = "Quản lý quyền trong Cài đặt hệ thống",
                        onClick = { onAction(SettingsAction.ClickOpenAppSettings) },
                        showDivider = false
                    )
                }
            }

            item {
                SettingsGroup(title = "Dữ liệu & Bộ nhớ") {
                    SettingsItemRow(
                        icon = Icons.Rounded.CleaningServices,
                        title = "Bộ nhớ đệm (Cache)",
                        subtitle = "Đang chiếm ${uiState.cacheSizeLabel}",
                        trailingContent = {
                            Surface(
                                shape = CircleShape,
                                color = colors.surfaceHigh,
                                border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onAction(SettingsAction.ClearCache) }
                            ) {
                                Text(
                                    text = "Xoá cache",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    ),
                                    color = colors.textPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        },
                        showDivider = false
                    )
                }
            }

            item {
                SettingsGroup(title = "Về Lokap & Tài khoản") {
                    SettingsItemRow(
                        icon = Icons.Rounded.Description,
                        title = "Điều khoản & Chính sách bảo mật",
                        onClick = {  }
                    )

                    SettingsItemRow(
                        icon = Icons.Rounded.Info,
                        title = "Phiên bản ứng dụng",
                        trailingText = "v${uiState.appVersionLabel}"
                    )

                    SettingsItemRow(
                        icon = Icons.AutoMirrored.Rounded.Logout,
                        iconTint = colors.error,
                        title = "Đăng xuất",
                        titleColor = colors.error,
                        onClick = { onAction(SettingsAction.OpenLogoutDialog) }
                    )

                    SettingsItemRow(
                        icon = Icons.Rounded.DeleteForever,
                        iconTint = colors.error,
                        title = "Xoá tài khoản vĩnh viễn",
                        titleColor = colors.error,
                        onClick = { onAction(SettingsAction.OpenDeleteAccountDialog) },
                        showDivider = false
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LokapLogo(size = 36.dp, showText = true)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Bản đồ khoảnh khắc Việt Nam",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                        color = colors.textTertiary
                    )
                }
            }
        }
    }

    if (uiState.isEditProfileSheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { onAction(SettingsAction.DismissEditProfileSheet) },
            sheetState = sheetState,
            shape = LokapTheme.shapes.sheet,
            containerColor = colors.surface,
            contentColor = colors.textPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = spacing.lg, vertical = spacing.md)
            ) {
                Text(
                    text = "Chỉnh sửa hồ sơ",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    text = "Cập nhật thông tin nhận diện của bạn trên Lokap.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(spacing.md))

                OutlinedTextField(
                    value = uiState.editDisplayNameInput,
                    onValueChange = { onAction(SettingsAction.ChangeEditDisplayName(it)) },
                    singleLine = true,
                    label = { Text("Tên hiển thị") },
                    isError = uiState.editDisplayNameError != null,
                    supportingText = {
                        Text(
                            text = uiState.editDisplayNameError ?: "${uiState.editDisplayNameInput.length}/30",
                            color = if (uiState.editDisplayNameError != null) colors.error else colors.textTertiary
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(spacing.sm))

                OutlinedTextField(
                    value = uiState.editUsernameInput,
                    onValueChange = { onAction(SettingsAction.ChangeEditUsername(it)) },
                    singleLine = true,
                    label = { Text("Username (@)") },
                    isError = uiState.editUsernameError != null,
                    supportingText = {
                        Text(
                            text = uiState.editUsernameError ?: "Tối thiểu 3 ký tự chữ thường",
                            color = if (uiState.editUsernameError != null) colors.error else colors.textTertiary
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(spacing.sm))

                OutlinedTextField(
                    value = uiState.editBioInput,
                    onValueChange = { onAction(SettingsAction.ChangeEditBio(it)) },
                    maxLines = 3,
                    label = { Text("Tiểu sử") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(spacing.lg))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    GhostButton(
                        text = "Huỷ",
                        onClick = { onAction(SettingsAction.DismissEditProfileSheet) },
                        modifier = Modifier.weight(1f)
                    )
                    PinkButton(
                        text = "Lưu thay đổi",
                        enabled = uiState.editDisplayNameError == null && uiState.editUsernameError == null && uiState.editDisplayNameInput.isNotBlank(),
                        onClick = { onAction(SettingsAction.SaveEditProfile) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(spacing.md))
            }
        }
    }

    if (uiState.showThemeDialog) {
        AlertDialog(
            onDismissRequest = { onAction(SettingsAction.DismissThemeDialog) },
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Chọn giao diện",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
            },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        val selected = uiState.themeMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = { onAction(SettingsAction.SelectThemeMode(mode)) }
                                )
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = colors.primary,
                                    unselectedColor = colors.textSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = mode.labelVi,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onAction(SettingsAction.DismissThemeDialog) }) {
                    Text("Đóng", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (uiState.showLocationLevelDialog) {
        AlertDialog(
            onDismissRequest = { onAction(SettingsAction.DismissLocationLevelDialog) },
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Mức độ chi tiết vị trí",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
            },
            text = {
                Column {
                    LocationLevel.entries.forEach { level ->
                        val selected = uiState.defaultLocationLevel == level
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = { onAction(SettingsAction.SelectLocationLevel(level)) }
                                )
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = colors.primary,
                                    unselectedColor = colors.textSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = level.labelVi,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = level.descVi,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textTertiary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onAction(SettingsAction.DismissLocationLevelDialog) }) {
                    Text("Đóng", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (uiState.showBlockedUsersDialog) {
        AlertDialog(
            onDismissRequest = { onAction(SettingsAction.DismissBlockedUsersDialog) },
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Danh sách chặn (${uiState.blockedUsers.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
            },
            text = {
                if (uiState.blockedUsers.isEmpty()) {
                    Text(
                        text = "Bạn chưa chặn người dùng nào.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.blockedUsers.forEach { user ->
                            BlockedUserItemRow(
                                user = user,
                                onUnblock = { onAction(SettingsAction.UnblockUser(user.id)) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onAction(SettingsAction.DismissBlockedUsersDialog) }) {
                    Text("Đóng", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (uiState.showRecoveryPhoneDialog) {
        var phoneInput by remember { mutableStateOf(uiState.recoveryPhone ?: "") }
        AlertDialog(
            onDismissRequest = { onAction(SettingsAction.DismissRecoveryPhoneDialog) },
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Số điện thoại khôi phục",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Dùng số điện thoại để lấy lại tài khoản và nhận mã xác thực bảo mật.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        singleLine = true,
                        placeholder = { Text("0912 345 678", color = colors.textTertiary) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(SettingsAction.DismissRecoveryPhoneDialog) }) {
                    Text("Huỷ", color = colors.textSecondary)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onAction(SettingsAction.SaveRecoveryPhone(phoneInput.trim())) },
                    enabled = phoneInput.trim().length >= 9
                ) {
                    Text("Lưu", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (uiState.showLinkedEmailsDialog) {
        var newEmailInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { onAction(SettingsAction.DismissLinkedEmailsDialog) },
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Email liên kết",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Danh sách email liên kết với tài khoản này:",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    uiState.linkedEmails.forEach { email ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceHigh)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AlternateEmail,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = newEmailInput,
                        onValueChange = { newEmailInput = it },
                        singleLine = true,
                        placeholder = { Text("Thêm email phụ...", color = colors.textTertiary) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(SettingsAction.DismissLinkedEmailsDialog) }) {
                    Text("Đóng", color = colors.textSecondary)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newEmailInput.contains("@") && newEmailInput.contains(".")) {
                            onAction(SettingsAction.AddLinkedEmail(newEmailInput.trim()))
                            newEmailInput = ""
                        }
                    },
                    enabled = newEmailInput.contains("@") && newEmailInput.contains(".")
                ) {
                    Text("Thêm", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (uiState.showChangePasswordDialog) {
        var oldPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        val canSave = oldPassword.isNotBlank() && newPassword.length >= 6 && newPassword == confirmPassword

        AlertDialog(
            onDismissRequest = { onAction(SettingsAction.DismissChangePasswordDialog) },
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Đổi mật khẩu",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = { Text("Mật khẩu hiện tại") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Mật khẩu mới (tối thiểu 6 ký tự)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Xác nhận mật khẩu mới") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(SettingsAction.DismissChangePasswordDialog) }) {
                    Text("Huỷ", color = colors.textSecondary)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onAction(SettingsAction.ConfirmChangePassword(oldPassword, newPassword)) },
                    enabled = canSave
                ) {
                    Text("Lưu", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (uiState.showLogoutDialog) {
        LogoutConfirmDialog(
            onDismiss = { onAction(SettingsAction.DismissLogoutDialog) },
            onConfirm = { onAction(SettingsAction.ConfirmLogout) }
        )
    }

    if (uiState.showDeleteAccountDialog) {
        DeleteAccountConfirmDialog(
            confirmInput = uiState.deleteConfirmTextInput,
            isConfirmEnabled = uiState.isDeleteConfirmEnabled,
            onInputChange = { onAction(SettingsAction.ChangeDeleteConfirmText(it)) },
            onDismiss = { onAction(SettingsAction.DismissDeleteAccountDialog) },
            onConfirm = { onAction(SettingsAction.ConfirmDeleteAccount) }
        )
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp
            ),
            color = colors.textTertiary,
            modifier = Modifier.padding(start = spacing.sm, bottom = 2.dp)
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceHigh.copy(alpha = 0.75f),
            border = BorderStroke(0.8.dp, colors.outline.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailingText: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    iconTint: Color = LokapTheme.colors.textPrimary,
    titleColor: Color = LokapTheme.colors.textPrimary,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = true
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
                )
                .padding(horizontal = spacing.md, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(21.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.5.sp
                    ),
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = colors.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (trailingContent != null) {
                trailingContent()
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!trailingText.isNullOrBlank()) {
                        Text(
                            text = trailingText,
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                            color = colors.textSecondary
                        )
                    }
                    if (onClick != null) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (showDivider) {
            HorizontalDivider(
                color = colors.outline.copy(alpha = 0.12f),
                thickness = 0.6.dp,
                modifier = Modifier.padding(start = 48.dp)
            )
        }
    }
}

@Composable
private fun PermissionStatusBadge(isGranted: Boolean) {
    val colors = LokapTheme.colors
    if (isGranted) {
        Surface(
            shape = CircleShape,
            color = Color(0xFF10B981).copy(alpha = 0.16f),
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Đã cấp",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    } else {
        Surface(
            shape = CircleShape,
            color = colors.error.copy(alpha = 0.14f),
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Chưa cấp",
                    tint = colors.error,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun BlockedUserItemRow(
    user: User,
    onUnblock: () -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceHigh)
            .padding(spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarRing(
            avatarUrl = user.avatarUrl,
            displayName = user.displayName,
            size = 38.dp
        )
        Spacer(modifier = Modifier.width(spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                ),
                color = colors.textPrimary
            )
            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                color = colors.textSecondary
            )
        }
        GhostButton(
            text = "Bỏ chặn",
            onClick = onUnblock
        )
    }
}

@Composable
private fun LogoutConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = LokapTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        title = {
            Text(
                text = "Đăng xuất khỏi Lokap?",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Text(
                text = "Bạn vẫn có thể đăng nhập lại bất cứ lúc nào để xem lại hành trình của mình.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Ở lại", color = colors.textSecondary)
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Đăng xuất",
                    color = colors.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
private fun DeleteAccountConfirmDialog(
    confirmInput: String,
    isConfirmEnabled: Boolean,
    onInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        title = {
            Text(
                text = "Xoá tài khoản vĩnh viễn?",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.error
            )
        },
        text = {
            Column {
                Text(
                    text = "Hành động này không thể hoàn tác. Toàn bộ ảnh đã chụp, dấu chân bản đồ và tin nhắn của bạn sẽ bị xoá vĩnh viễn khỏi máy chủ Lokap.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(spacing.md))

                Text(
                    text = "Nhập chính xác chữ XOÁ (viết hoa) để xác nhận:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(spacing.xs))

                OutlinedTextField(
                    value = confirmInput,
                    onValueChange = onInputChange,
                    singleLine = true,
                    placeholder = {
                        Text(text = "XOÁ", color = colors.textTertiary)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.error,
                        unfocusedBorderColor = colors.outline,
                        cursorColor = colors.error,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Huỷ", color = colors.textSecondary)
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = isConfirmEnabled
            ) {
                Text(
                    text = "Xoá vĩnh viễn",
                    color = if (isConfirmEnabled) colors.error else colors.textTertiary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

private fun hasPostNotificationsPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun openAppNotificationSettings(context: Context) {
    val intent = Intent().apply {
        action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching {
        context.startActivity(intent)
    }.onFailure {
        val fallback = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(fallback) }
    }
}

private fun resolveAppVersion(context: Context): String {
    val fromBuildConfig = runCatching {
        val clazz = Class.forName("vn.nhom2.lokap.BuildConfig")
        val vName = clazz.getField("VERSION_NAME").get(null) as? String
        val vCode = clazz.getField("VERSION_CODE").get(null) as? Number
        if (!vName.isNullOrBlank()) {
            if (vCode != null) "$vName ($vCode)" else vName
        } else {
            null
        }
    }.getOrNull()

    if (fromBuildConfig != null) return fromBuildConfig

    return runCatching {
        val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        val versionName = pkgInfo.versionName ?: "1.0.0"
        val versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo)
        "$versionName ($versionCode)"
    }.getOrDefault("1.0.0 (1)")
}
