Bạn là UI designer kiêm lập trình viên Android senior. Thiết kế lại màn **Giới thiệu + Xin quyền** của app Lokap (kiểu Locket, chỉ dark mode, màu nhấn hồng, GenZ) theo **quy chuẩn tối giản**. Code hiện tại nằm ở cuối prompt: `OnboardingContract.kt` chỉ để tham khảo (KHÔNG sửa), bạn chỉ trả lại `OnboardingScreen.kt`.

## Quy chuẩn tối giản
- Bớt chữ tối đa: bỏ mọi dòng phụ, dòng giải thích, nhãn "Bắt buộc"/"Có thể bỏ qua", chữ "Đã cấp"/"Chưa cấp". Tiêu đề chỉ vài chữ.
- Ưu tiên icon hơn chữ; icon to (≥ 32.dp trong ô quyền), vùng chạm ≥ 48.dp; mọi icon có `contentDescription` tiếng Việt (người dùng trình đọc màn hình vẫn nghe được trạng thái, ví dụ "Camera, đã cấp").

## Màn xin quyền (trang cuối)
- Mỗi quyền chỉ còn **icon + một chữ**: "Camera", "Vị trí", "Thông báo". Bạn tự chọn bố cục đẹp nhất (ví dụ 3 ô vuông kiểu icon lựa chọn).
- Trạng thái thể hiện bằng hình, không bằng chữ:
  - Đã cấp: **viền hồng** (`primary`) + dấu ✓ hồng, giống ô đang được chọn. Không dùng màu xanh lá.
  - Chưa cấp: viền `outline`, icon `textSecondary`.
  - Bị từ chối vĩnh viễn: thêm icon nhỏ `Settings`; bấm ô thì mở Cài đặt app.
  - Vị trí chỉ được COARSE: viền `primarySoft` (hồng nhạt) thay vì `primary`.
- **Bấm cả ô để xin quyền**, bỏ các nút "Cấp quyền …" riêng.
- Camera bắt buộc: nút "Bắt đầu" chỉ bật khi Camera đã cấp (giữ đúng logic hiện tại). Vị trí và Thông báo có thể bỏ qua. Android < 13 thì ẩn ô Thông báo.
- Tiêu đề màn này ngắn (ví dụ "Cấp quyền"), bỏ câu giải thích bên dưới.

## 3 trang giới thiệu
- Áp cùng quy chuẩn: mỗi trang một tiêu đề ngắn (≤ 6 chữ), bỏ đoạn mô tả dài; minh hoạ (vẽ bằng Compose, gradient `regionGradient`, emoji lớn) được làm to hơn để bù cho phần chữ.

## Ràng buộc
- KHÔNG đổi `OnboardingUiState`, `OnboardingAction`, `OnboardingEffect`, ViewModel, `OnboardingRoute`, chữ ký `fun NavGraphBuilder.onboardingScreen(navController: NavController)` và các hàm public/internal hiện có. Giữ nguyên toàn bộ logic xin quyền (launcher RequestMultiplePermissions, FINE + COARSE, mở Cài đặt, Android 13+).
- Màu chỉ từ `LokapTheme.colors` (background, surface, surfaceHigh, outline, primary, primarySoft, onPrimary, textPrimary, textSecondary, textTertiary, accentLilac, accentLime, accentCyan, accentYellow, error, success); khoảng cách `LokapTheme.spacing`; bo góc `LokapTheme.shapes`. Không hard-code màu. Chữ trên nền hồng dùng `onPrimary`.
- Icon chỉ dùng gói material-icons-core (`Icons.Default.*`, `Icons.AutoMirrored.Filled.*`; có `LocationOn`, `Notifications`, `Check`, `CheckCircle`, `Settings`, `Lock`, `Warning`). Không có icon camera trong gói core → dùng emoji 📷 hoặc vẽ bằng Compose.
- Giữ đủ Preview cho từng trạng thái (bọc LokapTheme).

## Định dạng trả lời
- Trả lại **nguyên file `OnboardingScreen.kt` đầy đủ** (không rút gọn, không "// ... giữ nguyên"), trong một khối ```kotlin … ``` với dòng ĐẦU TIÊN là `// FILE: app/src/main/java/vn/nhom2/lokap/feature/onboarding/OnboardingScreen.kt`.
- Cuối cùng: bảng ngắn "Trước → Sau".
- Quá dài thì dừng ở cuối file hoàn chỉnh, ghi `CÒN TIẾP`.

## Code hiện tại

### OnboardingContract.kt (CHỈ THAM KHẢO, KHÔNG SỬA)
```kotlin
package vn.nhom2.lokap.feature.onboarding

import kotlinx.serialization.Serializable

@Serializable
data object OnboardingRoute

enum class PermissionStatus {
    NOT_GRANTED,
    GRANTED,
    DENIED
}

enum class LocationAccuracyState {
    NONE,
    COARSE_ONLY,
    FINE
}

data class OnboardingUiState(
    val currentPage: Int = 0,
    val totalPages: Int = 4,
    val cameraStatus: PermissionStatus = PermissionStatus.NOT_GRANTED,
    val locationStatus: PermissionStatus = PermissionStatus.NOT_GRANTED,
    val locationAccuracy: LocationAccuracyState = LocationAccuracyState.NONE,
    val notificationStatus: PermissionStatus = PermissionStatus.NOT_GRANTED,
    val isSubmitting: Boolean = false
) {
    val canStart: Boolean
        get() = cameraStatus == PermissionStatus.GRANTED && !isSubmitting
}

sealed interface OnboardingAction {
    data class PageChanged(val page: Int) : OnboardingAction
    data object NextPageClicked : OnboardingAction
    data object SkipToPermissionsClicked : OnboardingAction
    data class SyncPermissionState(
        val cameraGranted: Boolean,
        val fineLocationGranted: Boolean,
        val coarseLocationGranted: Boolean,
        val notificationGranted: Boolean
    ) : OnboardingAction
    data class CameraPermissionResult(
        val granted: Boolean,
        val shouldShowRationale: Boolean
    ) : OnboardingAction
    data class LocationPermissionResult(
        val fineGranted: Boolean,
        val coarseGranted: Boolean,
        val shouldShowRationale: Boolean
    ) : OnboardingAction
    data class NotificationPermissionResult(
        val granted: Boolean,
        val shouldShowRationale: Boolean
    ) : OnboardingAction
    data object StartClicked : OnboardingAction
}

sealed interface OnboardingEffect {
    data object NavigateToLogin : OnboardingEffect
}
```

### OnboardingScreen.kt (SỬA FILE NÀY)
```kotlin
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
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
import vn.nhom2.lokap.core.designsystem.component.GhostButton
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
        // Top bar with "Bỏ qua" on first 2 pages (indices 0 and 1)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassChip(
                label = "LOKAP ✨",
                selected = true
            )
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
                    badge = "WIDGET TỨC THÌ",
                    title = "Chụp & gửi tức thì cho bạn thân",
                    subtitle = "Mở app là thấy camera. Chụp 1 chạm, ảnh bay thẳng lên màn hình chính của hội bạn thân.",
                    illustration = { InstantSnapIllustration() }
                )

                1 -> IntroSlidePage(
                    badge = "BẢN ĐỒ SỐNG",
                    title = "Ghim khoảnh khắc lên bản đồ Việt Nam",
                    subtitle = "Khám phá ảnh public gom theo từng Tỉnh/Thành → Phường/Xã khắp 3 miền.",
                    illustration = { VietnamMapIllustration() }
                )

                2 -> IntroSlidePage(
                    badge = "QUYỀN RIÊNG TƯ",
                    title = "Bạn chọn ai thấy, thấy tới đâu",
                    subtitle = "Tuỳ chỉnh mức hiển thị vị trí (Phường/Xã hoặc chỉ Tỉnh/Thành) và hẹn giờ tự xoá ảnh 24h.",
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

        // Page indicator dots + bottom action button
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
                if (uiState.cameraStatus != PermissionStatus.GRANTED) {
                    Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))
                    Text(
                        text = "Cần cấp quyền Camera để tiếp tục vào Lokap",
                        style = MaterialTheme.typography.bodySmall,
                        color = LokapTheme.colors.textTertiary,
                        textAlign = TextAlign.Center
                    )
                }
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
    val lilacColor = LokapTheme.colors.accentLilac
    Box(
        modifier = Modifier.size(260.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = primaryColor.copy(alpha = 0.14f),
                radius = size.minDimension * 0.48f
            )
            drawCircle(
                color = lilacColor.copy(alpha = 0.22f),
                radius = size.minDimension * 0.36f,
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // Back card tilted
        Box(
            modifier = Modifier
                .size(168.dp)
                .rotate(-8f)
                .clip(LokapTheme.shapes.cameraFrame)
                .background(regionGradient("79"))
                .border(2.dp, LokapTheme.colors.outline, LokapTheme.shapes.cameraFrame)
        )

        // Front Locket-style square camera frame
        Box(
            modifier = Modifier
                .size(176.dp)
                .rotate(4f)
                .clip(LokapTheme.shapes.cameraFrame)
                .background(regionGradient("01"))
                .border(3.dp, LokapTheme.colors.primary, LokapTheme.shapes.cameraFrame),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "📸", fontSize = 54.sp)
                Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))
                Box(
                    modifier = Modifier
                        .clip(LokapTheme.shapes.pill)
                        .background(LokapTheme.colors.surface.copy(alpha = 0.8f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Gửi hội bestie ⚡",
                        style = MaterialTheme.typography.labelMedium,
                        color = LokapTheme.colors.textPrimary
                    )
                }
            }
        }

        // Floating emoji stickers
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-12).dp, y = 12.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(LokapTheme.colors.surfaceHigh)
                .border(1.dp, LokapTheme.colors.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "💖", fontSize = 22.sp)
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 12.dp, y = (-16).dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(LokapTheme.colors.surfaceHigh)
                .border(1.dp, LokapTheme.colors.accentLime, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🔥", fontSize = 22.sp)
        }
    }
}

@Composable
private fun VietnamMapIllustration() {
    val cyan = LokapTheme.colors.accentCyan
    val primary = LokapTheme.colors.primary
    Box(
        modifier = Modifier.size(260.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
            drawCircle(
                color = cyan.copy(alpha = 0.2f),
                radius = size.minDimension * 0.45f,
                style = Stroke(width = 2.dp.toPx(), pathEffect = dashEffect)
            )
            drawLine(
                color = primary.copy(alpha = 0.5f),
                start = Offset(size.width * 0.28f, size.height * 0.25f),
                end = Offset(size.width * 0.55f, size.height * 0.5f),
                strokeWidth = 3.dp.toPx(),
                pathEffect = dashEffect
            )
            drawLine(
                color = primary.copy(alpha = 0.5f),
                start = Offset(size.width * 0.55f, size.height * 0.5f),
                end = Offset(size.width * 0.38f, size.height * 0.78f),
                strokeWidth = 3.dp.toPx(),
                pathEffect = dashEffect
            )
        }

        // North card
        RegionClusterBubble(
            emoji = "☕",
            title = "Hà Nội · Hoàn Kiếm",
            provinceCode = "01",
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 8.dp, y = 18.dp)
        )

        // Central card
        RegionClusterBubble(
            emoji = "🌊",
            title = "Đà Nẵng · Hải Châu",
            provinceCode = "48",
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        // South card
        RegionClusterBubble(
            emoji = "🛵",
            title = "TP.HCM · Bến Nghé",
            provinceCode = "79",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 16.dp, y = (-12).dp)
        )
    }
}

@Composable
private fun RegionClusterBubble(
    emoji: String,
    title: String,
    provinceCode: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(LokapTheme.shapes.card)
            .background(regionGradient(provinceCode))
            .border(1.5.dp, LokapTheme.colors.outline, LokapTheme.shapes.card)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(LokapTheme.colors.background.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 18.sp)
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = LokapTheme.colors.textPrimary
            )
            Text(
                text = "📍 +28 ảnh mới",
                style = MaterialTheme.typography.labelSmall,
                color = LokapTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun PrivacyControlIllustration() {
    Box(
        modifier = Modifier.size(260.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(LokapTheme.shapes.card)
                .background(regionGradient("92"))
                .border(2.dp, LokapTheme.colors.primary, LokapTheme.shapes.card)
                .padding(LokapTheme.spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
        ) {
            Text(text = "🛡️⏱️", fontSize = 44.sp)

            Text(
                text = "Chế độ hiển thị thông minh",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = LokapTheme.colors.textPrimary
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.xs)
            ) {
                GlassChip(label = "👑 Bạn thân", selected = true)
                GlassChip(label = "🌏 Public Tỉnh/Xã", selected = false)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.xs)
            ) {
                GlassChip(label = "⏳ Tự huỷ 24h", selected = true)
                GlassChip(label = "📍 Ẩn toạ độ nhà", selected = false)
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = LokapTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(LokapTheme.spacing.md)
    ) {
        Text(
            text = "Cấp quyền để quẩy Lokap 🚀",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = LokapTheme.colors.textPrimary
        )
        Text(
            text = "Lokap tôn trọng quyền riêng tư của bạn. Bạn luôn kiểm soát được từng quyền bên dưới.",
            style = MaterialTheme.typography.bodyMedium,
            color = LokapTheme.colors.textSecondary
        )

        // 1. Camera card (Required)
        PermissionItemCard(
            emoji = "📸",
            title = "Camera",
            badgeText = "Bắt buộc",
            badgeColor = LokapTheme.colors.primary,
            description = "Mở app là chụp ngay khoảnh khắc gửi hội bạn thân.",
            extraWarning = null,
            status = uiState.cameraStatus,
            onRequestPermission = onRequestCamera,
            onOpenSettings = onOpenAppSettings
        )

        // 2. Location card (Optional + COARSE note)
        val coarseNote = if (uiState.locationAccuracy == LocationAccuracyState.COARSE_ONLY) {
            "Chỉ đăng được ở mức Tỉnh trở lên"
        } else {
            null
        }
        PermissionItemCard(
            emoji = "📍",
            title = "Vị trí",
            badgeText = "Có thể bỏ qua",
            badgeColor = LokapTheme.colors.accentCyan,
            description = "Để gắn khu vực Tỉnh/Thành → Phường/Xã. Chỉ lấy vị trí lúc chụp, không theo dõi.",
            extraWarning = coarseNote,
            status = uiState.locationStatus,
            onRequestPermission = onRequestLocation,
            onOpenSettings = onOpenAppSettings
        )

        // 3. Notification card (Android 13+, Optional)
        PermissionItemCard(
            emoji = "🔔",
            title = "Thông báo",
            badgeText = "Có thể bỏ qua",
            badgeColor = LokapTheme.colors.accentLilac,
            description = "Báo ngay khi bạn thân vừa thả ảnh mới lên widget của bạn.",
            extraWarning = null,
            status = uiState.notificationStatus,
            onRequestPermission = onRequestNotification,
            onOpenSettings = onOpenAppSettings
        )
    }
}

@Composable
private fun PermissionItemCard(
    emoji: String,
    title: String,
    badgeText: String,
    badgeColor: Color,
    description: String,
    extraWarning: String?,
    status: PermissionStatus,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val borderColor = when (status) {
        PermissionStatus.GRANTED -> LokapTheme.colors.success
        PermissionStatus.DENIED -> LokapTheme.colors.error
        PermissionStatus.NOT_GRANTED -> LokapTheme.colors.outline
    }

    Surface(
        shape = LokapTheme.shapes.card,
        color = LokapTheme.colors.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, LokapTheme.shapes.card)
    ) {
        Column(
            modifier = Modifier.padding(LokapTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(LokapTheme.colors.surfaceHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 20.sp)
                    }
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LokapTheme.colors.textPrimary
                        )
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor
                        )
                    }
                }

                PermissionStatusBadge(status = status)
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = LokapTheme.colors.textSecondary
            )

            if (extraWarning != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(LokapTheme.shapes.pill)
                        .background(LokapTheme.colors.surfaceHigh)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = LokapTheme.colors.accentYellow,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = extraWarning,
                        style = MaterialTheme.typography.labelMedium,
                        color = LokapTheme.colors.accentYellow
                    )
                }
            }

            when (status) {
                PermissionStatus.NOT_GRANTED -> {
                    GhostButton(
                        text = "Cấp quyền $title",
                        onClick = onRequestPermission,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                PermissionStatus.DENIED -> {
                    GhostButton(
                        text = "Mở Cài đặt",
                        onClick = onOpenSettings,
                        leadingIcon = Icons.Default.Settings,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                PermissionStatus.GRANTED -> Unit
            }
        }
    }
}

@Composable
private fun PermissionStatusBadge(status: PermissionStatus) {
    val (label, color) = when (status) {
        PermissionStatus.NOT_GRANTED -> "Chưa cấp" to LokapTheme.colors.textTertiary
        PermissionStatus.GRANTED -> "Đã cấp" to LokapTheme.colors.success
        PermissionStatus.DENIED -> "Bị từ chối" to LokapTheme.colors.error
    }

    Row(
        modifier = Modifier
            .clip(LokapTheme.shapes.pill)
            .background(LokapTheme.colors.surfaceHigh)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (status == PermissionStatus.GRANTED) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = color
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
```
