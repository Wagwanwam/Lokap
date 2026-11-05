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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import vn.nhom2.lokap.core.designsystem.component.AvatarRing
import vn.nhom2.lokap.core.designsystem.component.GhostButton
import vn.nhom2.lokap.core.designsystem.component.PinkButton
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.core.navigation.navigateSingleTop
import vn.nhom2.lokap.core.navigation.popBackStackSafely
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.data.fake.SampleSocialData
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.User

fun NavGraphBuilder.settingsScreen(navController: NavController) {
    composable<Route.Settings> {
        SettingsRoute(
            onNavigateBack = { navController.popBackStackSafely() }
        )
    }
}

@Composable
fun SettingsRoute(
    onNavigateBack: () -> Unit,
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
                viewModel.onAction(
                    SettingsAction.UpdateNotificationPermissionState(
                        hasPostNotificationsPermission(context)
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
                SettingsEffect.RequestNotificationPermissionOrOpenSettings -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        openAppNotificationSettings(context)
                    }
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

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(LokapTheme.colors.background)
            .systemBarsPadding(),
        containerColor = LokapTheme.colors.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = LokapTheme.spacing.sm,
                        vertical = LokapTheme.spacing.xs
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onAction(SettingsAction.NavigateBack) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = LokapTheme.colors.textPrimary
                    )
                }
                Text(
                    text = "Cài đặt",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LokapTheme.colors.textPrimary
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = LokapTheme.spacing.md,
                end = LokapTheme.spacing.md,
                top = LokapTheme.spacing.sm,
                bottom = LokapTheme.spacing.xxl
            ),
            verticalArrangement = Arrangement.spacedBy(LokapTheme.spacing.md)
        ) {
            // Nhóm 1: Riêng tư
            item {
                SettingsGroupCard(title = "Riêng tư") {
                    Text(
                        text = "Mức vị trí mặc định khi chụp ảnh",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = LokapTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))
                    Text(
                        text = "Chọn độ chi tiết địa điểm gắn lên ảnh công khai trên bản đồ 34 tỉnh thành.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LokapTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))

                    LocationLevel.entries.forEach { level ->
                        val selected = uiState.defaultLocationLevel == level
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(LokapTheme.shapes.card)
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = { onAction(SettingsAction.SelectLocationLevel(level)) }
                                )
                                .padding(vertical = LokapTheme.spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = LokapTheme.colors.primary,
                                    unselectedColor = LokapTheme.colors.textSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(LokapTheme.spacing.sm))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = level.labelVi,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) {
                                        LokapTheme.colors.primary
                                    } else {
                                        LokapTheme.colors.textPrimary
                                    }
                                )
                                Text(
                                    text = locationLevelDescriptionVi(level),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LokapTheme.colors.textSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(LokapTheme.spacing.md))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(LokapTheme.colors.outline)
                    )

                    Spacer(modifier = Modifier.height(LokapTheme.spacing.md))

                    Text(
                        text = "Người đã chặn (${uiState.blockedUsers.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = LokapTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))

                    if (uiState.blockedUsers.isEmpty()) {
                        Text(
                            text = "Bạn chưa chặn ai trên Lokap.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LokapTheme.colors.textSecondary
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
                        ) {
                            uiState.blockedUsers.forEach { blockedUser ->
                                BlockedUserItemRow(
                                    user = blockedUser,
                                    onUnblock = { onAction(SettingsAction.UnblockUser(blockedUser.id)) }
                                )
                            }
                        }
                    }
                }
            }

            // Nhóm 2: Thông báo
            item {
                SettingsGroupCard(title = "Thông báo") {
                    if (!uiState.hasNotificationPermission) {
                        NotificationPermissionWarningCard(
                            onGrantClick = {
                                onAction(SettingsAction.ClickGrantNotificationPermission)
                            }
                        )
                        Spacer(modifier = Modifier.height(LokapTheme.spacing.md))
                    }

                    NotificationSwitchRow(
                        title = "Ảnh mới từ bạn bè",
                        subtitle = "Báo ngay khi hội bạn thân vừa thả ảnh mới",
                        checked = uiState.notifyNewPhotos,
                        onCheckedChange = { onAction(SettingsAction.ToggleNotifyNewPhotos(it)) }
                    )

                    NotificationSwitchRow(
                        title = "Cảm xúc",
                        subtitle = "Khi có người thả tim hoặc reaction vào ảnh của bạn",
                        checked = uiState.notifyReactions,
                        onCheckedChange = { onAction(SettingsAction.ToggleNotifyReactions(it)) }
                    )

                    NotificationSwitchRow(
                        title = "Tin nhắn",
                        subtitle = "Tin nhắn phản hồi trực tiếp từ khoảnh khắc",
                        checked = uiState.notifyMessages,
                        onCheckedChange = { onAction(SettingsAction.ToggleNotifyMessages(it)) }
                    )

                    NotificationSwitchRow(
                        title = "Lời mời kết bạn",
                        subtitle = "Khi có người tìm thấy bạn và gửi lời mời kết bạn",
                        checked = uiState.notifyFriendRequests,
                        onCheckedChange = { onAction(SettingsAction.ToggleNotifyFriendRequests(it)) }
                    )
                }
            }

            // Nhóm 3: Tài khoản
            item {
                SettingsGroupCard(title = "Tài khoản") {
                    GhostButton(
                        text = "Đăng xuất",
                        onClick = { onAction(SettingsAction.OpenLogoutDialog) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(LokapTheme.shapes.pill)
                            .border(
                                width = 1.dp,
                                color = LokapTheme.colors.error,
                                shape = LokapTheme.shapes.pill
                            )
                            .clickable { onAction(SettingsAction.OpenDeleteAccountDialog) }
                            .padding(vertical = LokapTheme.spacing.sm),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Xoá tài khoản vĩnh viễn",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = LokapTheme.colors.error
                        )
                    }
                }
            }

            // Phiên bản App cuối trang
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = LokapTheme.spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Lokap · Bản đồ khoảnh khắc Việt Nam",
                        style = MaterialTheme.typography.labelMedium,
                        color = LokapTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Phiên bản ${uiState.appVersionLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = LokapTheme.colors.textTertiary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
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
private fun SettingsGroupCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LokapTheme.shapes.card)
            .background(LokapTheme.colors.surface)
            .border(
                width = 1.dp,
                color = LokapTheme.colors.outline,
                shape = LokapTheme.shapes.card
            )
            .padding(LokapTheme.spacing.md)
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = LokapTheme.colors.primary
        )
        Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))
        content()
    }
}

@Composable
private fun BlockedUserItemRow(
    user: User,
    onUnblock: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LokapTheme.shapes.card)
            .background(LokapTheme.colors.surfaceHigh)
            .padding(LokapTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarRing(
            avatarUrl = user.avatarUrl,
            displayName = user.displayName,
            size = 40.dp
        )
        Spacer(modifier = Modifier.width(LokapTheme.spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = LokapTheme.colors.textPrimary
            )
            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.labelSmall,
                color = LokapTheme.colors.textSecondary
            )
        }
        GhostButton(
            text = "Bỏ chặn",
            onClick = onUnblock
        )
    }
}

@Composable
private fun NotificationPermissionWarningCard(
    onGrantClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LokapTheme.shapes.card)
            .background(LokapTheme.colors.surfaceHigh)
            .border(
                width = 1.dp,
                color = LokapTheme.colors.accentYellow,
                shape = LokapTheme.shapes.card
            )
            .padding(LokapTheme.spacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = LokapTheme.colors.accentYellow
            )
            Spacer(modifier = Modifier.width(LokapTheme.spacing.sm))
            Text(
                text = "Chưa bật quyền thông báo",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = LokapTheme.colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))

        Text(
            text = "Cấp quyền thông báo để không bỏ lỡ khoảnh khắc mới và lời nhắn từ bạn bè trên Lokap.",
            style = MaterialTheme.typography.bodySmall,
            color = LokapTheme.colors.textSecondary
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))

        PinkButton(
            text = "Cấp quyền",
            leadingIcon = Icons.Default.Notifications,
            onClick = onGrantClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun NotificationSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = LokapTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = LokapTheme.colors.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = LokapTheme.colors.textSecondary
            )
        }
        Spacer(modifier = Modifier.width(LokapTheme.spacing.sm))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = LokapTheme.colors.onPrimary,
                checkedTrackColor = LokapTheme.colors.primary,
                uncheckedThumbColor = LokapTheme.colors.textSecondary,
                uncheckedTrackColor = LokapTheme.colors.surfaceHigh,
                uncheckedBorderColor = LokapTheme.colors.outline
            )
        )
    }
}

@Composable
private fun LogoutConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LokapTheme.colors.surface,
        titleContentColor = LokapTheme.colors.textPrimary,
        textContentColor = LokapTheme.colors.textSecondary,
        title = {
            Text(
                text = "Đăng xuất khỏi Lokap?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Bạn vẫn có thể đăng nhập lại bất cứ lúc nào để xem lại hành trình 34 tỉnh thành của mình.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Ở lại",
                    color = LokapTheme.colors.textSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Đăng xuất",
                    color = LokapTheme.colors.primary,
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
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LokapTheme.colors.surface,
        titleContentColor = LokapTheme.colors.textPrimary,
        textContentColor = LokapTheme.colors.textSecondary,
        title = {
            Text(
                text = "Xoá tài khoản vĩnh viễn?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LokapTheme.colors.error
            )
        },
        text = {
            Column {
                Text(
                    text = "Hành động này không thể hoàn tác. Toàn bộ ảnh đã chụp, dấu chân bản đồ và tin nhắn của bạn sẽ bị xoá vĩnh viễn khỏi máy chủ Lokap.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LokapTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(LokapTheme.spacing.md))

                Text(
                    text = "Nhập chính xác chữ XOÁ (viết hoa) để xác nhận:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = LokapTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))

                OutlinedTextField(
                    value = confirmInput,
                    onValueChange = onInputChange,
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = "XOÁ",
                            color = LokapTheme.colors.textTertiary
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LokapTheme.colors.error,
                        unfocusedBorderColor = LokapTheme.colors.outline,
                        cursorColor = LokapTheme.colors.error,
                        focusedTextColor = LokapTheme.colors.textPrimary,
                        unfocusedTextColor = LokapTheme.colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Huỷ",
                    color = LokapTheme.colors.textSecondary
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = isConfirmEnabled
            ) {
                Text(
                    text = "Xoá vĩnh viễn",
                    color = if (isConfirmEnabled) {
                        LokapTheme.colors.error
                    } else {
                        LokapTheme.colors.textTertiary
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

private fun locationLevelDescriptionVi(level: LocationLevel): String {
    return when (level) {
        LocationLevel.PLACE -> "Hiện tên quán, địa danh cụ thể nơi bạn đứng"
        LocationLevel.WARD -> "Chỉ hiện cấp Phường / Xã"
        LocationLevel.PROVINCE -> "Chỉ gom vào bong bóng Tỉnh / Thành phố"
        LocationLevel.COUNTRY -> "Ẩn chi tiết tỉnh, chỉ hiện Việt Nam"
    }
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

@Preview(name = "Settings - Mặc định (Chưa cấp quyền TB)", showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    LokapTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                defaultLocationLevel = LocationLevel.PROVINCE,
                blockedUsers = SampleSocialData.directoryUsers.take(1),
                hasNotificationPermission = false,
                appVersionLabel = "1.2.0 (24)"
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Settings - Hộp thoại Xoá tài khoản", showBackground = true)
@Composable
private fun SettingsDeleteAccountDialogPreview() {
    LokapTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                hasNotificationPermission = true,
                showDeleteAccountDialog = true,
                deleteConfirmTextInput = "XOÁ"
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Settings - Hộp thoại Đăng xuất", showBackground = true)
@Composable
private fun SettingsLogoutDialogPreview() {
    LokapTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                hasNotificationPermission = true,
                showLogoutDialog = true
            ),
            onAction = {}
        )
    }
}
