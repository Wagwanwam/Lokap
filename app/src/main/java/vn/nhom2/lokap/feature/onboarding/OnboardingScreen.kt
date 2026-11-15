package vn.nhom2.lokap.feature.onboarding

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import vn.nhom2.lokap.feature.auth.LoginRoute
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
fun NavGraphBuilder.onboardingScreen(navController: NavController) {
    composable<OnboardingRoute> {
        OnboardingRoute(
            onNavigateToLogin = {
                navController.navigate(LoginRoute) {
                    popUpTo(OnboardingRoute) { inclusive = true }
                    launchSingleTop = true
                }
            }
        )
    }
}

@Composable
fun OnboardingRoute(
    onNavigateToLogin: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    fun syncPermissions() {
        val cameraGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        viewModel.onAction(
            OnboardingAction.SyncPermissionState(
                cameraGranted = cameraGranted,
                fineLocationGranted = fineGranted,
                coarseLocationGranted = coarseGranted,
                notificationGranted = notificationGranted
            )
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                syncPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val activity = context.findActivity()

        if (results.containsKey(Manifest.permission.CAMERA)) {
            val granted = results[Manifest.permission.CAMERA] == true
            val rationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
            } ?: false
            viewModel.onAction(OnboardingAction.CameraPermissionResult(granted, rationale))
        }

        if (results.containsKey(Manifest.permission.ACCESS_FINE_LOCATION) ||
            results.containsKey(Manifest.permission.ACCESS_COARSE_LOCATION)
        ) {
            val fine = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarse = results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            val rationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    it,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            } ?: false
            viewModel.onAction(OnboardingAction.LocationPermissionResult(fine, coarse, rationale))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            results.containsKey(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            val granted = results[Manifest.permission.POST_NOTIFICATIONS] == true
            val rationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    it,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            } ?: false
            viewModel.onAction(OnboardingAction.NotificationPermissionResult(granted, rationale))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                OnboardingEffect.NavigateToLogin -> onNavigateToLogin()
            }
        }
    }

    OnboardingScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onRequestCamera = {
            permissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA))
        },
        onRequestLocation = {
            permissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        },
        onRequestNotification = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            } else {
                viewModel.onAction(
                    OnboardingAction.NotificationPermissionResult(
                        granted = true,
                        shouldShowRationale = false
                    )
                )
            }
        },
        onOpenAppSettings = {
            context.openAppSettings()
        }
    )
}

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onAction: (OnboardingAction) -> Unit,
    onRequestCamera: () -> Unit,
    onRequestLocation: () -> Unit,
    onRequestNotification: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LokapTheme.colors.background)
            .systemBarsPadding()
            .padding(horizontal = LokapTheme.spacing.lg, vertical = LokapTheme.spacing.md)
    ) {
        LokapLogo(size = 36.dp, showText = true)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            PermissionsPage(
                uiState = uiState,
                onRequestCamera = onRequestCamera,
                onRequestLocation = onRequestLocation,
                onRequestNotification = onRequestNotification,
                onOpenAppSettings = onOpenAppSettings
            )
        }

        PinkButton(
            text = "Bắt đầu",
            onClick = { onAction(OnboardingAction.StartClicked) },
            enabled = uiState.canStart,
            loading = uiState.isSubmitting,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PermissionsPage(
    uiState: OnboardingUiState,
    onRequestCamera: () -> Unit,
    onRequestLocation: () -> Unit,
    onRequestNotification: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = spacing.md),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            PermissionMinimalCard(
                icon = Icons.Filled.CameraAlt,
                title = "Camera",
                isGranted = uiState.cameraStatus == PermissionStatus.GRANTED,
                isDenied = uiState.cameraStatus == PermissionStatus.DENIED,
                onClick = {
                    if (uiState.cameraStatus == PermissionStatus.DENIED) {
                        onOpenAppSettings()
                    } else {
                        onRequestCamera()
                    }
                },
                modifier = Modifier.weight(1f)
            )

            PermissionMinimalCard(
                icon = Icons.Filled.LocationOn,
                title = "Vị trí",
                isGranted = uiState.locationStatus == PermissionStatus.GRANTED,
                isDenied = uiState.locationStatus == PermissionStatus.DENIED,
                onClick = {
                    if (uiState.locationStatus == PermissionStatus.DENIED) {
                        onOpenAppSettings()
                    } else {
                        onRequestLocation()
                    }
                },
                modifier = Modifier.weight(1f)
            )

            PermissionMinimalCard(
                icon = Icons.Filled.Notifications,
                title = "Thông báo",
                isGranted = uiState.notificationStatus == PermissionStatus.GRANTED,
                isDenied = uiState.notificationStatus == PermissionStatus.DENIED,
                onClick = {
                    if (uiState.notificationStatus == PermissionStatus.DENIED) {
                        onOpenAppSettings()
                    } else {
                        onRequestNotification()
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PermissionMinimalCard(
    icon: ImageVector,
    title: String,
    isGranted: Boolean,
    isDenied: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val primaryColor = colors.primary
    val statusAccessibilityText = when {
        isGranted -> "$title, đã cấp"
        isDenied -> "$title, bị từ chối, bấm để mở cài đặt"
        else -> "$title, chưa cấp, bấm để cấp quyền"
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.surface,
        border = BorderStroke(
            width = if (isGranted) 2.dp else 1.dp,
            color = if (isGranted) primaryColor else colors.outline.copy(alpha = 0.25f)
        ),
        shadowElevation = if (isGranted) 3.dp else 1.dp,
        modifier = modifier
            .aspectRatio(0.95f)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            // Gộp cả ô thành một mục để TalkBack chỉ đọc tên + trạng thái một lần
            // (không có dòng này sẽ đọc "Camera, Camera, Đã cấp")
            .clearAndSetSemantics {
                role = Role.Button
                contentDescription = statusAccessibilityText
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) primaryColor else colors.surfaceHigh)
                    .border(
                        width = 0.5.dp,
                        color = if (isGranted) primaryColor else colors.outline.copy(alpha = 0.35f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Filled.Check else Icons.Filled.Close,
                    contentDescription = if (isGranted) "Đã cấp" else "Chưa cấp",
                    tint = if (isGranted) colors.buttonText else colors.textSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) primaryColor else colors.textPrimary.copy(alpha = 0.85f),
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                FitText(
                    text = title,
                    color = if (isGranted) colors.textPrimary else colors.textSecondary,
                    fontWeight = if (isGranted) FontWeight.ExtraBold else FontWeight.Bold,
                    widthFraction = 0.85f,
                    heightFraction = 1f,
                    maxFontSize = 15.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                )
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}

@Preview(showBackground = true, name = "Onboarding - Permissions Mixed & Coarse")
@Composable
private fun OnboardingPermissionsPreview() {
    LokapTheme {
        OnboardingScreen(
            uiState = OnboardingUiState(
                cameraStatus = PermissionStatus.GRANTED,
                locationStatus = PermissionStatus.GRANTED,
                locationAccuracy = LocationAccuracyState.COARSE_ONLY,
                notificationStatus = PermissionStatus.DENIED
            ),
            onAction = {},
            onRequestCamera = {},
            onRequestLocation = {},
            onRequestNotification = {},
            onOpenAppSettings = {}
        )
    }
}
