Gemini đã nói
```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/onboarding/OnboardingScreen.kt
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import vn.nhom2.lokap.core.designsystem.component.GlassChip
import vn.nhom2.lokap.core.designsystem.component.PinkButton
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.designsystem.theme.regionGradient
import vn.nhom2.lokap.feature.auth.LoginRoute

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
    viewModel: OnboardingViewModel = viewModel()
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
    val pagerState = rememberPagerState(
        initialPage = uiState.currentPage,
        pageCount = { uiState.totalPages }
    )

    LaunchedEffect(uiState.currentPage) {
        if (pagerState.currentPage != uiState.currentPage) {
            pagerState.animateScrollToPage(uiState.currentPage)
        }
    }

    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage != uiState.currentPage) {
            onAction(OnboardingAction.PageChanged(pagerState.settledPage))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LokapTheme.colors.background)
            .systemBarsPadding()
            .padding(horizontal = LokapTheme.spacing.lg, vertical = LokapTheme.spacing.md)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassChip(
                label = "LOKAP ✨",
                selected = true
            )
            if (uiState.currentPage < 2) {
                TextButton(
                    onClick = { onAction(OnboardingAction.SkipToPermissionsClicked) },
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Text(
                        text = "Bỏ qua",
                        style = MaterialTheme.typography.labelLarge,
                        color = LokapTheme.colors.textSecondary
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            when (page) {
                0 -> IntroSlidePage(
                    title = "Chụp & gửi tức thì",
                    illustration = { InstantSnapIllustration() }
                )

                1 -> IntroSlidePage(
                    title = "Bản đồ khoảnh khắc",
                    illustration = { VietnamMapIllustration() }
                )

                2 -> IntroSlidePage(
                    title = "Làm chủ riêng tư",
                    illustration = { PrivacyControlIllustration() }
                )

                else -> PermissionsPage(
                    uiState = uiState,
                    onRequestCamera = onRequestCamera,
                    onRequestLocation = onRequestLocation,
                    onRequestNotification = onRequestNotification,
                    onOpenAppSettings = onOpenAppSettings
                )
            }
        }

        Spacer(modifier = Modifier.height(LokapTheme.spacing.md))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PageDotsIndicator(
                totalPages = uiState.totalPages,
                currentPage = uiState.currentPage
            )

            Spacer(modifier = Modifier.height(LokapTheme.spacing.lg))

            if (uiState.currentPage < uiState.totalPages - 1) {
                PinkButton(
                    text = "Tiếp theo",
                    onClick = { onAction(OnboardingAction.NextPageClicked) },
                    leadingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                PinkButton(
                    text = "Bắt đầu",
                    onClick = { onAction(OnboardingAction.StartClicked) },
                    enabled = uiState.canStart,
                    loading = uiState.isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PageDotsIndicator(
    totalPages: Int,
    currentPage: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalPages) { index ->
            val isSelected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (isSelected) 28.dp else 8.dp,
                label = "dot_width"
            )
            val color by animateColorAsState(
                targetValue = if (isSelected) LokapTheme.colors.primary else LokapTheme.colors.surfaceHigh,
                label = "dot_color"
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(LokapTheme.shapes.pill)
                    .background(color)
            )
        }
    }
}

@Composable
private fun IntroSlidePage(
    title: String,
    illustration: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = LokapTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp),
            contentAlignment = Alignment.Center
        ) {
            illustration()
        }

        Spacer(modifier = Modifier.height(LokapTheme.spacing.xl))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = LokapTheme.colors.textPrimary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun InstantSnapIllustration() {
    val primaryColor = LokapTheme.colors.primary
    val lilacColor = LokapTheme.colors.accentLilac
    Box(
        modifier = Modifier
            .size(320.dp)
            .semantics { contentDescription = "Minh họa chụp và gửi ảnh tức thì" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = primaryColor.copy(alpha = 0.16f),
                radius = size.minDimension * 0.48f
            )
            drawCircle(
                color = lilacColor.copy(alpha = 0.25f),
                radius = size.minDimension * 0.38f,
                style = Stroke(width = 3.dp.toPx())
            )
        }

        Box(
            modifier = Modifier
                .size(210.dp)
                .rotate(-8f)
                .clip(LokapTheme.shapes.cameraFrame)
                .background(regionGradient("79"))
                .border(2.dp, LokapTheme.colors.outline, LokapTheme.shapes.cameraFrame)
        )

        Box(
            modifier = Modifier
                .size(220.dp)
                .rotate(4f)
                .clip(LokapTheme.shapes.cameraFrame)
                .background(regionGradient("01"))
                .border(3.dp, LokapTheme.colors.primary, LokapTheme.shapes.cameraFrame),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "📷", fontSize = 80.sp)
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-10).dp, y = 14.dp)
                .size(60.dp)
                .clip(CircleShape)
                .background(LokapTheme.colors.surfaceHigh)
                .border(1.5.dp, LokapTheme.colors.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "💖", fontSize = 28.sp)
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 14.dp, y = (-14).dp)
                .size(60.dp)
                .clip(CircleShape)
                .background(LokapTheme.colors.surfaceHigh)
                .border(1.5.dp, LokapTheme.colors.accentLime, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "⚡", fontSize = 28.sp)
        }
    }
}

@Composable
private fun VietnamMapIllustration() {
    val cyan = LokapTheme.colors.accentCyan
    val primary = LokapTheme.colors.primary
    Box(
        modifier = Modifier
            .size(320.dp)
            .semantics { contentDescription = "Minh họa bản đồ khoảnh khắc ba miền" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
            drawCircle(
                color = cyan.copy(alpha = 0.22f),
                radius = size.minDimension * 0.46f,
                style = Stroke(width = 2.5.dp.toPx(), pathEffect = dashEffect)
            )
            drawLine(
                color = primary.copy(alpha = 0.6f),
                start = Offset(size.width * 0.25f, size.height * 0.22f),
                end = Offset(size.width * 0.68f, size.height * 0.48f),
                strokeWidth = 3.5.dp.toPx(),
                pathEffect = dashEffect
            )
            drawLine(
                color = primary.copy(alpha = 0.6f),
                start = Offset(size.width * 0.68f, size.height * 0.48f),
                end = Offset(size.width * 0.32f, size.height * 0.78f),
                strokeWidth = 3.5.dp.toPx(),
                pathEffect = dashEffect
            )
        }

        RegionClusterPin(
            emoji = "☕",
            provinceCode = "01",
            borderColor = LokapTheme.colors.primary,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 24.dp, y = 20.dp)
        )

        RegionClusterPin(
            emoji = "🌊",
            provinceCode = "48",
            borderColor = LokapTheme.colors.accentCyan,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-16).dp, y = (-4).dp)
        )

        RegionClusterPin(
            emoji = "🛵",
            provinceCode = "79",
            borderColor = LokapTheme.colors.accentLime,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 40.dp, y = (-20).dp)
        )
    }
}

@Composable
private fun RegionClusterPin(
    emoji: String,
    provinceCode: String,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(88.dp)
            .clip(LokapTheme.shapes.card)
            .background(regionGradient(provinceCode))
            .border(2.5.dp, borderColor, LokapTheme.shapes.card),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = 40.sp)
    }
}

@Composable
private fun PrivacyControlIllustration() {
    val primaryColor = LokapTheme.colors.primary
    Box(
        modifier = Modifier
            .size(320.dp)
            .semantics { contentDescription = "Minh họa quyền riêng tư và tự hủy sau 24 giờ" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = primaryColor.copy(alpha = 0.14f),
                radius = size.minDimension * 0.46f
            )
        }

        Column(
            modifier = Modifier
                .size(230.dp)
                .clip(LokapTheme.shapes.cameraFrame)
                .background(regionGradient("92"))
                .border(3.dp, LokapTheme.colors.primary, LokapTheme.shapes.cameraFrame)
                .padding(LokapTheme.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "🛡️", fontSize = 76.sp)
            Spacer(modifier = Modifier.height(LokapTheme.spacing.md))
            Row(
                horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassChip(label = "👑", selected = true)
                GlassChip(label = "24h ⏳", selected = true)
            }
        }
    }
}

@Composable
private fun PermissionsPage(
    uiState: OnboardingUiState,
    onRequestCamera: () -> Unit,
    onRequestLocation: () -> Unit,
    onRequestNotification: () -> Unit,
    onOpenAppSettings: () -> Unit,
    showNotificationTile: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = LokapTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Cấp quyền",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = LokapTheme.colors.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.xl))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                LokapTheme.spacing.md,
                Alignment.CenterHorizontally
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PermissionTile(
                title = "Camera",
                status = uiState.cameraStatus,
                isCoarseLocation = false,
                onClick = {
                    if (uiState.cameraStatus == PermissionStatus.DENIED) {
                        onOpenAppSettings()
                    } else {
                        onRequestCamera()
                    }
                },
                modifier = Modifier.weight(1f),
                iconContent = { tint, contentDesc ->
                    CameraGlyphIcon(
                        tint = tint,
                        contentDescription = contentDesc,
                        modifier = Modifier.size(36.dp)
                    )
                }
            )

            PermissionTile(
                title = "Vị trí",
                status = uiState.locationStatus,
                isCoarseLocation = uiState.locationStatus == PermissionStatus.GRANTED &&
                    uiState.locationAccuracy == LocationAccuracyState.COARSE_ONLY,
                onClick = {
                    if (uiState.locationStatus == PermissionStatus.DENIED) {
                        onOpenAppSettings()
                    } else {
                        onRequestLocation()
                    }
                },
                modifier = Modifier.weight(1f),
                iconContent = { tint, contentDesc ->
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = contentDesc,
                        tint = tint,
                        modifier = Modifier.size(36.dp)
                    )
                }
            )

            if (showNotificationTile) {
                PermissionTile(
                    title = "Thông báo",
                    status = uiState.notificationStatus,
                    isCoarseLocation = false,
                    onClick = {
                        if (uiState.notificationStatus == PermissionStatus.DENIED) {
                            onOpenAppSettings()
                        } else {
                            onRequestNotification()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    iconContent = { tint, contentDesc ->
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = contentDesc,
                            tint = tint,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun PermissionTile(
    title: String,
    status: PermissionStatus,
    isCoarseLocation: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconContent: @Composable (tint: Color, contentDescription: String) -> Unit
) {
    val isGranted = status == PermissionStatus.GRANTED
    val isDenied = status == PermissionStatus.DENIED

    val borderColor by animateColorAsState(
        targetValue = when {
            isGranted && isCoarseLocation -> LokapTheme.colors.primarySoft
            isGranted -> LokapTheme.colors.primary
            else -> LokapTheme.colors.outline
        },
        label = "tile_border_color"
    )

    val containerColor by animateColorAsState(
        targetValue = if (isGranted) LokapTheme.colors.surfaceHigh else LokapTheme.colors.surface,
        label = "tile_bg_color"
    )

    val iconTint by animateColorAsState(
        targetValue = when {
            isGranted && isCoarseLocation -> LokapTheme.colors.primarySoft
            isGranted -> LokapTheme.colors.primary
            else -> LokapTheme.colors.textSecondary
        },
        label = "tile_icon_tint"
    )

    val statusAccessibilityText = when {
        isGranted && isCoarseLocation -> "$title, đã cấp vị trí tương đối"
        isGranted -> "$title, đã cấp"
        isDenied -> "$title, bị từ chối, bấm để mở cài đặt"
        else -> "$title, chưa cấp, bấm để cấp quyền"
    }

    val borderWidth = if (isGranted) 2.5.dp else 1.5.dp

    Surface(
        shape = LokapTheme.shapes.card,
        color = containerColor,
        modifier = modifier
            .aspectRatio(0.9f)
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clip(LokapTheme.shapes.card)
            .border(borderWidth, borderColor, LokapTheme.shapes.card)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = statusAccessibilityText
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(LokapTheme.spacing.sm)
        ) {
            if (isGranted) {
                val badgeBg = if (isCoarseLocation) {
                    LokapTheme.colors.primarySoft
                } else {
                    LokapTheme.colors.primary
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Đã cấp quyền $title",
                        tint = LokapTheme.colors.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            } else if (isDenied) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(LokapTheme.colors.surfaceHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Mở cài đặt cho quyền $title",
                        tint = LokapTheme.colors.textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
            ) {
                iconContent(iconTint, statusAccessibilityText)

                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isGranted) LokapTheme.colors.textPrimary else LokapTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CameraGlyphIcon(
    tint: Color,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.semantics {
            this.contentDescription = contentDescription
        }
    ) {
        val strokeWidth = 2.8.dp.toPx()
        val bodyWidth = size.width * 0.84f
        val bodyHeight = size.height * 0.62f
        val bodyLeft = (size.width - bodyWidth) / 2f
        val bodyTop = size.height * 0.24f

        drawRoundRect(
            color = tint,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
            style = Stroke(width = strokeWidth)
        )

        val topBumpWidth = size.width * 0.34f
        val topBumpHeight = size.height * 0.11f
        drawRoundRect(
            color = tint,
            topLeft = Offset((size.width - topBumpWidth) / 2f, bodyTop - topBumpHeight),
            size = Size(topBumpWidth, topBumpHeight),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = Stroke(width = strokeWidth)
        )

        drawCircle(
            color = tint,
            radius = size.minDimension * 0.17f,
            center = Offset(size.width / 2f, bodyTop + bodyHeight / 2f),
            style = Stroke(width = strokeWidth)
        )
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

@Preview(showBackground = true, name = "Onboarding - Slide 1")
@Composable
private fun OnboardingSlide1Preview() {
    LokapTheme {
        OnboardingScreen(
            uiState = OnboardingUiState(currentPage = 0),
            onAction = {},
            onRequestCamera = {},
            onRequestLocation = {},
            onRequestNotification = {},
            onOpenAppSettings = {}
        )
    }
}

@Preview(showBackground = true, name = "Onboarding - Slide 2 Map")
@Composable
private fun OnboardingSlide2Preview() {
    LokapTheme {
        OnboardingScreen(
            uiState = OnboardingUiState(currentPage = 1),
            onAction = {},
            onRequestCamera = {},
            onRequestLocation = {},
            onRequestNotification = {},
            onOpenAppSettings = {}
        )
    }
}

@Preview(showBackground = true, name = "Onboarding - Slide 3 Privacy")
@Composable
private fun OnboardingSlide3Preview() {
    LokapTheme {
        OnboardingScreen(
            uiState = OnboardingUiState(currentPage = 2),
            onAction = {},
            onRequestCamera = {},
            onRequestLocation = {},
            onRequestNotification = {},
            onOpenAppSettings = {}
        )
    }
}

@Preview(showBackground = true, name = "Onboarding - Permissions Not Granted")
@Composable
private fun OnboardingPermissionsNotGrantedPreview() {
    LokapTheme {
        OnboardingScreen(
            uiState = OnboardingUiState(
                currentPage = 3,
                cameraStatus = PermissionStatus.NOT_GRANTED,
                locationStatus = PermissionStatus.NOT_GRANTED,
                notificationStatus = PermissionStatus.NOT_GRANTED
            ),
            onAction = {},
            onRequestCamera = {},
            onRequestLocation = {},
            onRequestNotification = {},
            onOpenAppSettings = {}
        )
    }
}

@Preview(showBackground = true, name = "Onboarding - Permissions Mixed & Coarse")
@Composable
private fun OnboardingPermissionsPreview() {
    LokapTheme {
        OnboardingScreen(
            uiState = OnboardingUiState(
                currentPage = 3,
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

@Preview(showBackground = true, name = "Onboarding - Permissions All Granted (Fine)")
@Composable
private fun OnboardingPermissionsAllGrantedPreview() {
    LokapTheme {
        OnboardingScreen(
            uiState = OnboardingUiState(
                currentPage = 3,
                cameraStatus = PermissionStatus.GRANTED,
                locationStatus = PermissionStatus.GRANTED,
                locationAccuracy = LocationAccuracyState.FINE,
                notificationStatus = PermissionStatus.GRANTED
            ),
            onAction = {},
            onRequestCamera = {},
            onRequestLocation = {},
            onRequestNotification = {},
            onOpenAppSettings = {}
        )
    }
}
```
Hạng mục	Trước	Sau
3 trang giới thiệu	Có chip phụ, tiêu đề dài, mô tả 2 dòng, bong bóng chữ nhiều chữ	Chỉ còn minh họa lớn (320.dp, icon/emoji phóng to) + tiêu đề ≤ 4 chữ
Trang xin quyền	3 thẻ dọc nhiều chữ: nhãn Bắt buộc/Có thể bỏ qua, trạng thái chữ, mô tả, cảnh báo vàng, nút GhostButton riêng	Tiêu đề "Cấp quyền" + 3 ô vuông ngang tối giản (Camera, Vị trí, Thông báo), chỉ gồm icon 36.dp + 1 tên ngắn, bấm trực tiếp cả ô
Biểu đạt trạng thái	Dùng màu success (xanh), error (đỏ) và nhãn chữ "Đã cấp" / "Chưa cấp" / "Bị từ chối"	Đã cấp: viền primary + tích Check hồng (onPrimary trên nền primary); Vị trí COARSE: viền & badge primarySoft; Chưa cấp: viền outline + icon textSecondary; Từ chối: thêm badge Settings góc trên, bấm mở Cài đặt
Android < 13 & Accessibility	Luôn hiện thẻ Thông báo; icon thiếu contentDescription trạng thái	Ẩn ô Thông báo khi SDK_INT < TIRAMISU; mọi icon và ô quyền đều có contentDescription tiếng Việt đầy đủ trạng thái và vùng chạm ≥ 48.dp