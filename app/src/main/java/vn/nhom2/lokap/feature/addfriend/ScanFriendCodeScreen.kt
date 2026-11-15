package vn.nhom2.lokap.feature.addfriend

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview as CameraXPreview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.util.concurrent.Executors
import kotlinx.coroutines.delay
import vn.nhom2.lokap.core.designsystem.component.PinkButton
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.domain.model.FriendCode
import vn.nhom2.lokap.feature.camera.CameraPermissionState

private val FrameSize = 260.dp
private val Scrim = Color.Black.copy(alpha = 0.6f)
private val Glass = Color.Black.copy(alpha = 0.45f)

/**
 * Quét mã QR kết bạn bằng CameraX + ZXing. Đọc được mã Lokap thì rung nhẹ và trả username về màn Thêm bạn;
 * QR khác thì báo "Không phải mã Lokap" rồi quét tiếp.
 */
@Composable
fun ScanFriendCodeRoute(
    onCodeScanned: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current

    // Nền camera luôn tối: icon thanh trạng thái/điều hướng phải sáng, trả lại như cũ khi rời màn
    DisposableEffect(view) {
        val controller = context.findActivity()?.window?.let { WindowCompat.getInsetsController(it, view) }
        val lightStatus = controller?.isAppearanceLightStatusBars
        val lightNav = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            lightStatus?.let { controller?.isAppearanceLightStatusBars = it }
            lightNav?.let { controller?.isAppearanceLightNavigationBars = it }
        }
    }

    fun isGranted() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

    fun shouldShowRationale(): Boolean = context.findActivity()?.let {
        ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
    } ?: false

    var permission by rememberSaveable {
        mutableStateOf(if (isGranted()) CameraPermissionState.GRANTED else CameraPermissionState.NOT_REQUESTED)
    }
    val rationaleBefore = remember { booleanArrayOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permission = when {
            granted -> CameraPermissionState.GRANTED
            shouldShowRationale() -> CameraPermissionState.DENIED
            // Hệ thống không hỏi nữa (đã chọn "Không hỏi lại") → phải mở Cài đặt
            rationaleBefore[0] -> CameraPermissionState.PERMANENTLY_DENIED
            permission == CameraPermissionState.NOT_REQUESTED -> CameraPermissionState.DENIED
            else -> CameraPermissionState.PERMANENTLY_DENIED
        }
    }
    fun requestPermission() {
        rationaleBefore[0] = shouldShowRationale()
        launcher.launch(Manifest.permission.CAMERA)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                when {
                    isGranted() -> permission = CameraPermissionState.GRANTED
                    permission == CameraPermissionState.NOT_REQUESTED -> requestPermission()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var handled by remember { mutableStateOf(false) }
    var showInvalid by remember { mutableStateOf(false) }

    LaunchedEffect(showInvalid) {
        if (showInvalid) {
            delay(2_000)
            showInvalid = false
        }
    }

    ScanFriendCodeScreen(
        permission = permission,
        isScanning = !handled,
        showInvalid = showInvalid,
        onRawCode = { raw ->
            if (handled) return@ScanFriendCodeScreen
            val username = FriendCode.parse(raw)
            if (username != null) {
                handled = true
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onCodeScanned(username)
            } else {
                showInvalid = true
            }
        },
        onRequestPermission = ::requestPermission,
        onOpenSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        },
        onClose = onClose
    )
}

@Composable
fun ScanFriendCodeScreen(
    permission: CameraPermissionState,
    isScanning: Boolean,
    showInvalid: Boolean,
    onRawCode: (String) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var torchOn by rememberSaveable { mutableStateOf(false) }
    var hasTorch by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (permission == CameraPermissionState.GRANTED) {
            QrCameraPreview(
                torchOn = torchOn,
                isScanning = isScanning,
                onTorchAvailable = { hasTorch = it },
                onRawCode = onRawCode,
                modifier = Modifier.fillMaxSize()
            )
            ScanOverlay(modifier = Modifier.fillMaxSize())

            AnimatedVisibility(
                visible = showInvalid,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = FrameSize / 2 + 48.dp)
            ) {
                GlassLabel(
                    icon = Icons.Filled.ErrorOutline,
                    text = "Không phải mã Lokap",
                    tint = LokapTheme.colors.error
                )
            }
            if (!showInvalid) {
                GlassLabel(
                    text = "Đưa mã QR vào khung",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = FrameSize / 2 + 48.dp)
                )
            }
        } else {
            CameraPermissionPrompt(
                permanentlyDenied = permission == CameraPermissionState.PERMANENTLY_DENIED,
                onRequestPermission = onRequestPermission,
                onOpenSettings = onOpenSettings,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = LokapTheme.spacing.lg, vertical = LokapTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconButton(icon = Icons.Filled.Close, contentDescription = "Đóng", onClick = onClose)
            Text(
                text = "Quét mã",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            if (hasTorch && permission == CameraPermissionState.GRANTED) {
                GlassIconButton(
                    icon = if (torchOn) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                    contentDescription = if (torchOn) "Tắt đèn" else "Bật đèn",
                    highlighted = torchOn,
                    onClick = { torchOn = !torchOn }
                )
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = LokapTheme.spacing.xxl)
                .clip(CircleShape)
                .background(Glass)
                .clickable(role = Role.Button, onClick = onClose)
                .padding(horizontal = LokapTheme.spacing.xl, vertical = LokapTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
        ) {
            Icon(
                imageVector = Icons.Filled.QrCode2,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
            Text(
                text = "Mã của tôi",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

/** Phủ tối quanh khung vuông bo góc, 4 góc hồng và vạch quét chạy lên xuống. */
@Composable
private fun ScanOverlay(modifier: Modifier = Modifier) {
    val primary = LokapTheme.colors.primary
    val transition = rememberInfiniteTransition(label = "scanLine")
    val progress by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(tween(1_600, easing = LinearEasing), RepeatMode.Reverse),
        label = "scanLineProgress"
    )

    Canvas(
        modifier = modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        val frame = FrameSize.toPx()
        val radius = 28.dp.toPx()
        val left = (size.width - frame) / 2
        val top = (size.height - frame) / 2
        val right = left + frame
        val bottom = top + frame

        drawRect(Scrim)
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(frame, frame),
            cornerRadius = CornerRadius(radius),
            blendMode = BlendMode.Clear
        )

        val arm = 44.dp.toPx()
        val corners = Path().apply {
            moveTo(left, top + arm); lineTo(left, top + radius)
            quadraticTo(left, top, left + radius, top); lineTo(left + arm, top)
            moveTo(right - arm, top); lineTo(right - radius, top)
            quadraticTo(right, top, right, top + radius); lineTo(right, top + arm)
            moveTo(right, bottom - arm); lineTo(right, bottom - radius)
            quadraticTo(right, bottom, right - radius, bottom); lineTo(right - arm, bottom)
            moveTo(left + arm, bottom); lineTo(left + radius, bottom)
            quadraticTo(left, bottom, left, bottom - radius); lineTo(left, bottom - arm)
        }
        drawPath(corners, primary, style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))

        val lineY = top + frame * progress
        val inset = 20.dp.toPx()
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, primary, Color.Transparent),
                startX = left + inset,
                endX = right - inset
            ),
            start = Offset(left + inset, lineY),
            end = Offset(right - inset, lineY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    highlighted: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (highlighted) LokapTheme.colors.primary else Glass)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (highlighted) LokapTheme.colors.onPrimary else Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun GlassLabel(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = Color.White
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Glass)
            .padding(horizontal = LokapTheme.spacing.lg, vertical = LokapTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(30.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

@Composable
private fun CameraPermissionPrompt(
    permanentlyDenied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(LokapTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LokapTheme.spacing.lg)
    ) {
        Icon(
            imageVector = Icons.Filled.QrCode2,
            contentDescription = null,
            tint = LokapTheme.colors.primary,
            modifier = Modifier.size(72.dp)
        )
        Text(
            text = if (permanentlyDenied) "Bật quyền máy ảnh trong Cài đặt" else "Cần máy ảnh để quét mã",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        PinkButton(
            text = if (permanentlyDenied) "Mở Cài đặt" else "Cho phép",
            onClick = if (permanentlyDenied) onOpenSettings else onRequestPermission
        )
    }
}

@Composable
private fun QrCameraPreview(
    torchOn: Boolean,
    isScanning: Boolean,
    onTorchAvailable: (Boolean) -> Unit,
    onRawCode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (LocalInspectionMode.current) {
        Box(modifier = modifier.background(Brush.verticalGradient(listOf(Color(0xFF2B2B33), Color(0xFF111114)))))
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnRawCode by rememberUpdatedState(onRawCode)
    val latestOnTorchAvailable by rememberUpdatedState(onTorchAvailable)
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val analyzer = remember {
        val main = ContextCompat.getMainExecutor(context)
        QrCodeAnalyzer { raw -> main.execute { latestOnRawCode(raw) } }
    }
    var camera by remember { mutableStateOf<Camera?>(null) }

    SideEffect { analyzer.enabled = isScanning }

    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        var provider: ProcessCameraProvider? = null
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val cameraProvider = future.get().also { provider = it }
                val resolution = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
                    )
                    .build()
                val preview = CameraXPreview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                    .setResolutionSelector(resolution)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(executor, analyzer) }
                cameraProvider.unbindAll()
                val bound = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
                camera = bound
                latestOnTorchAvailable(bound.cameraInfo.hasFlashUnit())
            } catch (_: Exception) {
                // Máy ảnh bận / không có camera sau: giữ màn đen, nút đóng vẫn dùng được
                latestOnTorchAvailable(false)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            provider?.unbindAll()
            camera = null
            executor.shutdown()
        }
    }

    LaunchedEffect(camera, torchOn) {
        camera?.cameraControl?.enableTorch(torchOn)
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

/** Đọc QR từ kênh sáng (Y) của khung hình CameraX bằng ZXing. */
private class QrCodeAnalyzer(private val onDecoded: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val reader = QRCodeReader()
    private val hints = mapOf(DecodeHintType.TRY_HARDER to true, DecodeHintType.CHARACTER_SET to "UTF-8")
    private var lastText: String? = null
    private var lastAt = 0L

    @Volatile
    var enabled: Boolean = true

    override fun analyze(image: ImageProxy) {
        image.use { frame ->
            if (!enabled) return
            val plane = frame.planes.firstOrNull() ?: return
            val buffer = plane.buffer.apply { rewind() }
            val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
            val source = PlanarYUVLuminanceSource(
                bytes, plane.rowStride, frame.height, 0, 0, frame.width, frame.height, false
            )
            val text = try {
                reader.decode(BinaryBitmap(HybridBinarizer(source)), hints).text
            } catch (_: ReaderException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            } finally {
                reader.reset()
            }
            if (text != null) {
                // Cùng một mã trong khung hình liên tiếp: báo 1 lần mỗi 1,5 giây
                val now = System.currentTimeMillis()
                if (text != lastText || now - lastAt > 1_500) {
                    lastText = text
                    lastAt = now
                    onDecoded(text)
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Preview(name = "Quét mã", showBackground = true)
@Composable
private fun ScanFriendCodePreview() {
    LokapTheme {
        ScanFriendCodeScreen(
            permission = CameraPermissionState.GRANTED,
            isScanning = true,
            showInvalid = false,
            onRawCode = {},
            onRequestPermission = {},
            onOpenSettings = {},
            onClose = {}
        )
    }
}

@Preview(name = "Quét mã - Chưa cấp quyền", showBackground = true)
@Composable
private fun ScanFriendCodeNoPermissionPreview() {
    LokapTheme {
        ScanFriendCodeScreen(
            permission = CameraPermissionState.DENIED,
            isScanning = true,
            showInvalid = false,
            onRawCode = {},
            onRequestPermission = {},
            onOpenSettings = {},
            onClose = {}
        )
    }
}
