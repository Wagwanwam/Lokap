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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
                .height(44.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LokapLogo(size = 28.dp, showText = true)

            if (uiState.currentPage < 2) {
                TextButton(
                    onClick = { onAction(OnboardingAction.SkipToPermissionsClicked) }
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
                    badge = "KHOẢNH KHẮC",
                    title = "Chụp & chia sẻ tức thì",
                    subtitle = "Gửi ảnh nhanh đến màn hình chính bạn bè.",
                    illustration = { InstantSnapIllustration() }
                )

                1 -> IntroSlidePage(
                    badge = "BẢN ĐỒ",
                    title = "Ghim lên bản đồ Việt Nam",
                    subtitle = "Khám phá ảnh từ đất liền tới Hoàng Sa, Trường Sa.",
                    illustration = { VietnamMapIllustration() }
                )

                2 -> IntroSlidePage(
                    badge = "RIÊNG TƯ",
                    title = "Kiểm soát riêng tư",
                    subtitle = "Bạn quyết định ai xem ảnh và vị trí của mình.",
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
                    text = if (uiState.currentPage == 2) "Thiết lập quyền" else "Tiếp theo",
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
    badge: String,
    title: String,
    subtitle: String,
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
                .height(290.dp),
            contentAlignment = Alignment.Center
        ) {
            illustration()
        }

        Spacer(modifier = Modifier.height(LokapTheme.spacing.xl))

        GlassChip(label = badge, selected = false)

        Spacer(modifier = Modifier.height(LokapTheme.spacing.md))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = LokapTheme.colors.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = LokapTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = LokapTheme.spacing.sm)
        )
    }
}

@Composable
private fun InstantSnapIllustration() {
    val primaryColor = LokapTheme.colors.primary
    val colors = LokapTheme.colors
    Box(
        modifier = Modifier.size(260.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = primaryColor.copy(alpha = 0.12f),
                radius = size.minDimension * 0.48f
            )
            drawCircle(
                color = colors.outline.copy(alpha = 0.35f),
                radius = size.minDimension * 0.36f,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        Box(
            modifier = Modifier
                .size(168.dp)
                .rotate(-8f)
                .clip(LokapTheme.shapes.cameraFrame)
                .background(colors.surfaceHigh)
                .border(1.5.dp, colors.outline, LokapTheme.shapes.cameraFrame)
        )

        Box(
            modifier = Modifier
                .size(176.dp)
                .rotate(4f)
                .clip(LokapTheme.shapes.cameraFrame)
                .background(colors.surface)
                .border(2.dp, primaryColor, LokapTheme.shapes.cameraFrame),
            contentAlignment = Alignment.Center
        ) {
            LokapLogo(size = 64.dp)
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-12).dp, y = 12.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.surface)
                .border(1.5.dp, primaryColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 12.dp, y = (-16).dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.surface)
                .border(1.5.dp, colors.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.FlashOn,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun VietnamMapIllustration() {
    val colors = LokapTheme.colors
    val primary = colors.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(310.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .width(280.dp)
                .height(300.dp)
        ) {
            val w = size.width
            val h = size.height

            val mapHeight = h * 0.88f
            val mapWidth = mapHeight * 0.48f
            val offsetX = (w - mapWidth) / 2f - 30.dp.toPx()
            val offsetY = (h - mapHeight) / 2f

            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            drawLine(
                color = colors.outline.copy(alpha = 0.25f),
                start = Offset(0f, offsetY + mapHeight * 0.44f),
                end = Offset(w, offsetY + mapHeight * 0.44f),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect
            )
            drawLine(
                color = colors.outline.copy(alpha = 0.25f),
                start = Offset(0f, offsetY + mapHeight * 0.78f),
                end = Offset(w, offsetY + mapHeight * 0.78f),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect
            )

            val vnMainlandPath = buildPolygonPath(
                points = VietnamMainlandPoints,
                mapWidth = mapWidth,
                mapHeight = mapHeight,
                offsetX = offsetX,
                offsetY = offsetY
            )

            drawPath(
                path = vnMainlandPath,
                color = primary.copy(alpha = 0.12f)
            )
            drawPath(
                path = vnMainlandPath,
                color = colors.textPrimary,
                style = Stroke(width = 1.8.dp.toPx())
            )

            val phuQuocPath = buildPolygonPath(
                points = PhuQuocPoints,
                mapWidth = mapWidth,
                mapHeight = mapHeight,
                offsetX = offsetX,
                offsetY = offsetY
            )
            drawPath(
                path = phuQuocPath,
                color = primary.copy(alpha = 0.25f)
            )
            drawPath(
                path = phuQuocPath,
                color = colors.textPrimary,
                style = Stroke(width = 1.5.dp.toPx())
            )

            val conDaoPath = buildPolygonPath(
                points = ConDaoPoints,
                mapWidth = mapWidth,
                mapHeight = mapHeight,
                offsetX = offsetX,
                offsetY = offsetY
            )
            drawPath(
                path = conDaoPath,
                color = colors.textPrimary,
                style = Stroke(width = 1.5.dp.toPx())
            )

            val hoangSaCenter = Offset(offsetX + mapWidth * 1.50f, offsetY + mapHeight * 0.42f)
            val hoangSaDots = listOf(
                hoangSaCenter + Offset((-8).dp.toPx(), (-4).dp.toPx()),
                hoangSaCenter + Offset(0f, (-8).dp.toPx()),
                hoangSaCenter + Offset(8.dp.toPx(), (-2).dp.toPx()),
                hoangSaCenter + Offset((-3).dp.toPx(), 6.dp.toPx()),
                hoangSaCenter + Offset(6.dp.toPx(), 7.dp.toPx())
            )
            hoangSaDots.forEach { dot ->
                drawCircle(color = primary, radius = 3.dp.toPx(), center = dot)
                drawCircle(color = primary.copy(alpha = 0.3f), radius = 5.5.dp.toPx(), center = dot)
            }

            val truongSaCenter = Offset(offsetX + mapWidth * 1.58f, offsetY + mapHeight * 0.76f)
            val truongSaDots = listOf(
                truongSaCenter + Offset((-10).dp.toPx(), (-8).dp.toPx()),
                truongSaCenter + Offset(0f, (-12).dp.toPx()),
                truongSaCenter + Offset(10.dp.toPx(), (-6).dp.toPx()),
                truongSaCenter + Offset((-6).dp.toPx(), 4.dp.toPx()),
                truongSaCenter + Offset(4.dp.toPx(), 8.dp.toPx()),
                truongSaCenter + Offset(12.dp.toPx(), 4.dp.toPx())
            )
            truongSaDots.forEach { dot ->
                drawCircle(color = primary, radius = 3.dp.toPx(), center = dot)
                drawCircle(color = primary.copy(alpha = 0.3f), radius = 5.5.dp.toPx(), center = dot)
            }

            val routeDash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            val hanoi = Offset(offsetX + mapWidth * 0.44f, offsetY + mapHeight * 0.16f)
            val danang = Offset(offsetX + mapWidth * 0.72f, offsetY + mapHeight * 0.47f)
            val hcm = Offset(offsetX + mapWidth * 0.58f, offsetY + mapHeight * 0.78f)

            drawLine(
                color = primary.copy(alpha = 0.5f),
                start = hanoi,
                end = danang,
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = routeDash
            )
            drawLine(
                color = primary.copy(alpha = 0.5f),
                start = danang,
                end = hcm,
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = routeDash
            )

            listOf(hanoi, danang, hcm).forEach { city ->
                drawCircle(color = colors.background, radius = 5.dp.toPx(), center = city)
                drawCircle(color = primary, radius = 3.5.dp.toPx(), center = city)
            }
        }

        IslandBadge(
            name = "Q.Đ Hoàng Sa",
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-4).dp, y = (-26).dp)
        )

        IslandBadge(
            name = "Q.Đ Trường Sa",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-4).dp, y = (-56).dp)
        )

        CityPinBadge(
            name = "Hà Nội",
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 18.dp, y = 30.dp)
        )

        CityPinBadge(
            name = "Đà Nẵng",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 14.dp, y = (-4).dp)
        )

        CityPinBadge(
            name = "TP.Hồ Chí Minh",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 8.dp, y = (-26).dp)
        )
    }
}

@Composable
private fun IslandBadge(
    name: String,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.surface.copy(alpha = 0.92f))
            .border(1.dp, colors.primary.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(colors.primary)
        )
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun CityPinBadge(
    name: String,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.surface.copy(alpha = 0.90f))
            .border(0.5.dp, colors.outline, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun PrivacyControlIllustration() {
    val colors = LokapTheme.colors
    val primaryColor = colors.primary

    Box(
        modifier = Modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {

        Canvas(modifier = Modifier.size(260.dp)) {
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

            drawCircle(
                color = colors.outline.copy(alpha = 0.25f),
                radius = size.width * 0.46f,
                center = centerOffset,
                style = Stroke(width = 1.dp.toPx(), pathEffect = dashEffect)
            )

            drawCircle(
                color = primaryColor.copy(alpha = 0.06f),
                radius = size.width * 0.36f,
                center = centerOffset
            )
            drawCircle(
                color = primaryColor.copy(alpha = 0.15f),
                radius = size.width * 0.36f,
                center = centerOffset,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(colors.surface)
                .border(2.dp, primaryColor, RoundedCornerShape(30.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(primaryColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-18).dp, y = 28.dp),
            shape = CircleShape,
            color = colors.surface,
            border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.35f)),
            shadowElevation = 4.dp
        ) {
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.People,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 18.dp, y = (-24).dp),
            shape = CircleShape,
            color = colors.surface,
            border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.35f)),
            shadowElevation = 4.dp
        ) {
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-14).dp, y = (-14).dp),
            shape = CircleShape,
            color = colors.surface,
            border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.35f)),
            shadowElevation = 4.dp
        ) {
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 24.dp, y = 38.dp),
            shape = CircleShape,
            color = colors.surfaceHigh,
            border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.3f)),
            shadowElevation = 4.dp
        ) {
            Box(
                modifier = Modifier.size(34.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(16.dp)
                )
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
        Text(
            text = "Cấp quyền",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(36.dp))

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
                    modifier = Modifier.size(13.dp)
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
                    contentDescription = title,
                    tint = if (isGranted) primaryColor else colors.textPrimary.copy(alpha = 0.85f),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isGranted) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = if (isGranted) colors.textPrimary else colors.textSecondary,
                    textAlign = TextAlign.Center
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
