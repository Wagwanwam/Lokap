package vn.nhom2.lokap.feature.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Size
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalZeroShutterLag
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview as CameraXPreview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import vn.nhom2.lokap.core.designsystem.component.*
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.designsystem.theme.regionGradient
@Composable
fun CameraRoute(
    onNavigateToComposer: (String) -> Unit,
    onNavigateToFeed: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToProfile: (String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var captureTriggerTick by remember { mutableIntStateOf(0) }

    fun shouldShowRationale(): Boolean = context.findActivity()?.let {
        ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
    } ?: false

    val rationaleBeforeRequest = remember { booleanArrayOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onAction(
            CameraAction.OnPermissionResult(
                isGranted = isGranted,
                shouldShowRationale = shouldShowRationale(),
                shouldShowRationaleBefore = rationaleBeforeRequest[0]
            )
        )
    }

    fun requestCameraPermission() {
        rationaleBeforeRequest[0] = shouldShowRationale()
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    viewModel.onAction(CameraAction.OnSyncPermissionGranted)
                } else if (viewModel.uiState.value.permission == CameraPermissionState.NOT_REQUESTED) {

                    requestCameraPermission()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CameraEffect.LaunchPermissionRequest -> {
                    requestCameraPermission()
                }

                CameraEffect.OpenAppSettings -> {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }

                CameraEffect.TriggerCapture -> {
                    captureTriggerTick++
                }

                is CameraEffect.NavigateToComposer -> onNavigateToComposer(effect.imagePath)
                CameraEffect.NavigateToFeed -> onNavigateToFeed()
                CameraEffect.NavigateToMap -> onNavigateToMap()
                CameraEffect.NavigateToFriends -> onNavigateToFriends()
                CameraEffect.NavigateToInbox -> onNavigateToInbox()
                is CameraEffect.NavigateToProfile -> onNavigateToProfile(effect.userId)
            }
        }
    }

    CameraScreen(
        state = state,
        onAction = viewModel::onAction,
        onNavigateToTab = { tab ->
            when (tab) {
                MainTab.FEED -> onNavigateToFeed()
                MainTab.MAP -> onNavigateToMap()
                MainTab.CAMERA -> Unit
                MainTab.INBOX -> onNavigateToInbox()
                MainTab.PROFILE -> onNavigateToProfile(null)
            }
        },
        captureTriggerTick = captureTriggerTick,
        modifier = modifier
    )
}

@Composable
fun CameraScreen(
    state: CameraUiState,
    onAction: (CameraAction) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToTab: (MainTab) -> Unit = {},
    captureTriggerTick: Int = 0
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes
    var accumulatedDragY by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { accumulatedDragY = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDragY += dragAmount
                    },
                    onDragEnd = {
                        if (accumulatedDragY < -90f) {
                            onAction(CameraAction.OnSwipeUpToFeed)
                        }
                        accumulatedDragY = 0f
                    }
                )
            },
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        CameraTopBar(
            state = state,
            onAction = onAction,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg, vertical = spacing.sm)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg)
                .aspectRatio(1f)
                .clip(shapes.cameraFrame)
                .background(colors.surface)
                .border(1.5.dp, colors.outline, shapes.cameraFrame),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.permission != CameraPermissionState.GRANTED -> {
                    PermissionPromptContent(
                        permissionState = state.permission,
                        onRequestPermission = { onAction(CameraAction.OnRequestPermissionClick) },
                        onOpenSettings = { onAction(CameraAction.OnOpenSettingsClick) }
                    )
                }

                state.cameraError != null -> {
                    ErrorState(
                        message = state.cameraError,
                        onRetry = { onAction(CameraAction.OnRetryCamera) },
                        modifier = Modifier.padding(spacing.lg)
                    )
                }

                else -> {
                    CameraPreviewViewport(
                        state = state,
                        captureTriggerTick = captureTriggerTick,
                        onDoubleTapFlip = { onAction(CameraAction.OnToggleLens) },
                        onZoomChange = { onAction(CameraAction.OnZoomChange(it)) },
                        onPhotoSaved = { onAction(CameraAction.OnPhotoSaved(it)) },
                        onCameraError = { onAction(CameraAction.OnCameraError(it)) }
                    )
                }
            }

            if (state.isCapturing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.background.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        CircularProgressIndicator(color = colors.primary)
                        Text(
                            text = "Đang xử lý ảnh…",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textPrimary
                        )
                    }
                }
            }
        }

        CameraControlsBar(
            state = state,
            onAction = onAction,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.xl)
        )

        Spacer(
            modifier = Modifier
                .navigationBarsPadding()
                .height(64.dp)
        )
    }
}

@Composable
private fun CameraTopBar(
    state: CameraUiState,
    onAction: (CameraAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val pill = LokapTheme.shapes.pill

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AvatarRing(
            avatarUrl = state.me.avatarUrl,
            displayName = state.me.displayName,
            size = 44.dp,
            hasNewPost = true,
            onClick = { onAction(CameraAction.OnProfileClick) }
        )

        LokapLogo(size = 28.dp, showText = true)

        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(pill)
                    .background(colors.surfaceHigh)
                    .border(1.dp, colors.outline, pill)
                    .clickable { onAction(CameraAction.OnMapClick) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Place,
                    contentDescription = "Bản đồ",
                    tint = colors.textPrimary
                )
            }

            Box(contentAlignment = Alignment.TopEnd) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(pill)
                        .background(colors.surfaceHigh)
                        .border(1.dp, colors.outline, pill)
                        .clickable { onAction(CameraAction.OnInboxClick) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Email,
                        contentDescription = "Tin nhắn",
                        tint = colors.textPrimary
                    )
                }
                BadgeDot(count = state.unreadMessages)
            }
        }
    }
}

@Composable
private fun CameraControlsBar(
    state: CameraUiState,
    onAction: (CameraAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val pill = LokapTheme.shapes.pill
    val controlsEnabled = state.permission == CameraPermissionState.GRANTED &&
        !state.isCapturing &&
        state.cameraError == null

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {

        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(pill)
                .background(colors.surfaceHigh)
                .border(
                    width = 1.dp,
                    color = if (state.flashMode != FlashMode.OFF) colors.primary else colors.outline,
                    shape = pill
                )
                .clickable(enabled = controlsEnabled) { onAction(CameraAction.OnCycleFlash) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (state.flashMode) {
                    FlashMode.OFF -> Icons.Filled.FlashOff
                    FlashMode.ON -> Icons.Filled.FlashOn
                    FlashMode.AUTO -> Icons.Filled.FlashAuto
                },
                contentDescription = "Flash",
                tint = if (state.flashMode != FlashMode.OFF) colors.primary else colors.textSecondary,
                modifier = Modifier.size(24.dp)
            )
        }

        ShutterButton(
            onClick = { onAction(CameraAction.OnShutterClick) },
            enabled = controlsEnabled
        )

        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(pill)
                .background(colors.surfaceHigh)
                .border(1.dp, colors.outline, pill)
                .clickable(enabled = controlsEnabled) { onAction(CameraAction.OnToggleLens) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "Lật máy ảnh",
                tint = colors.textPrimary
            )
        }
    }
}

@Composable
private fun PermissionPromptContent(
    permissionState: CameraPermissionState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = when (permissionState) {
                CameraPermissionState.PERMANENTLY_DENIED -> "Cần mở quyền máy ảnh trong Cài đặt"
                else -> "Cho phép Lokap dùng máy ảnh"
            },
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            text = when (permissionState) {
                CameraPermissionState.PERMANENTLY_DENIED ->
                    "Bạn đã tắt quyền truy cập máy ảnh. Hãy mở Cài đặt ứng dụng để bật lại và chụp khoảnh khắc cùng bạn bè."
                else ->
                    "Lokap chỉ chụp ảnh trực tiếp từ camera để giữ mọi khoảnh khắc chân thật nhất."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.sm, bottom = spacing.lg)
        )

        if (permissionState == CameraPermissionState.PERMANENTLY_DENIED) {
            PinkButton(
                text = "Mở Cài đặt",
                onClick = onOpenSettings
            )
        } else {
            PinkButton(
                text = "Cho phép",
                onClick = onRequestPermission
            )
        }
    }
}

@OptIn(ExperimentalZeroShutterLag::class)
@SuppressLint("MissingPermission")
@Composable
private fun CameraPreviewViewport(
    state: CameraUiState,
    captureTriggerTick: Int,
    onDoubleTapFlip: () -> Unit,
    onZoomChange: (Float) -> Unit,
    onPhotoSaved: (String) -> Unit,
    onCameraError: (String) -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val isPreview = LocalInspectionMode.current

    if (isPreview) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(regionGradient("da-nang")),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                Text(
                    text = if (state.lensFacing == CameraLens.FRONT) "Camera trước · 1080p" else "Camera sau · 1080p",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onPrimary
                )
                Text(
                    text = "Chạm đúp để lật · Chụm 2 ngón để thu phóng",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onPrimary
                )
            }
        }
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    var cameraRef by remember { mutableStateOf<Camera?>(null) }
    var imageCaptureRef by remember { mutableStateOf<ImageCapture?>(null) }

    val currentZoom by rememberUpdatedState(state.zoomRatio)

    LaunchedEffect(state.lensFacing, lifecycleOwner) {
        try {
            val cameraProvider = ProcessCameraProvider.awaitInstance(context)
            val lensInt = if (state.lensFacing == CameraLens.FRONT) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensInt)
                .build()

            val resolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(1080, 1920),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                    )
                )
                .build()

            val previewUseCase = CameraXPreview.Builder()
                .setResolutionSelector(resolutionSelector)
                .build()
                .also { it.surfaceProvider = previewView.surfaceProvider }

            val zslSupported = cameraProvider.isZslSupported(cameraSelector)
            val captureMode = if (zslSupported) {
                ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG
            } else {
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
            }

            val captureUseCase = ImageCapture.Builder()
                .setCaptureMode(captureMode)
                .setResolutionSelector(resolutionSelector)
                .setTargetRotation(previewView.display?.rotation ?: Surface.ROTATION_0)
                .build()

            cameraProvider.unbindAll()
            val boundCamera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                previewUseCase,
                captureUseCase
            )
            cameraRef = boundCamera
            imageCaptureRef = captureUseCase
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            onCameraError("Máy ảnh đang bận hoặc không thể khởi tạo. Vui lòng thử lại.")
        }
    }

    LaunchedEffect(state.flashMode, imageCaptureRef) {
        imageCaptureRef?.flashMode = when (state.flashMode) {
            FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
            FlashMode.ON -> ImageCapture.FLASH_MODE_ON
            FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
        }
    }

    LaunchedEffect(state.zoomRatio, cameraRef) {
        cameraRef?.cameraControl?.setZoomRatio(state.zoomRatio)
    }

    LaunchedEffect(captureTriggerTick) {
        if (captureTriggerTick == 0) return@LaunchedEffect
        val capture = imageCaptureRef ?: run {
            onCameraError("Máy ảnh chưa sẵn sàng để chụp.")
            return@LaunchedEffect
        }

        val outputFile = File(context.cacheDir, "lokap_${System.currentTimeMillis()}.jpg")
        val metadata = ImageCapture.Metadata().apply {
            isReversedHorizontal = (state.lensFacing == CameraLens.FRONT)
        }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile)
            .setMetadata(metadata)
            .build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onPhotoSaved(outputFile.absolutePath)
                }

                override fun onError(exception: ImageCaptureException) {
                    onCameraError("Không thể lưu ảnh chụp. Vui lòng thử lại.")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTapFlip() }
                )
            }
            .pointerInput(Unit) {

                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.count { it.pressed } >= 2) {
                            val zoomChange = event.calculateZoom()
                            if (zoomChange != 1f) {
                                onZoomChange(currentZoom * zoomChange)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(ExperimentalZeroShutterLag::class)
private fun ProcessCameraProvider.isZslSupported(selector: CameraSelector): Boolean {
    return try {
        availableCameraInfos
            .let { selector.filter(it) }
            .firstOrNull()
            ?.isZslSupported == true
    } catch (_: Exception) {
        false
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Preview(name = "1. Camera - Bình thường", showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun CameraScreenNormalPreview() {
    LokapTheme {
        CameraScreen(
            state = CameraUiState(
                permission = CameraPermissionState.GRANTED,
                flashMode = FlashMode.AUTO
            ),
            onAction = {}
        )
    }
}

@Preview(name = "2. Camera - Chưa cấp quyền", showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun CameraScreenPermissionDeniedPreview() {
    LokapTheme {
        CameraScreen(
            state = CameraUiState(
                permission = CameraPermissionState.DENIED
            ),
            onAction = {}
        )
    }
}

@Preview(name = "3. Camera - Từ chối vĩnh viễn", showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun CameraScreenPermanentlyDeniedPreview() {
    LokapTheme {
        CameraScreen(
            state = CameraUiState(
                permission = CameraPermissionState.PERMANENTLY_DENIED
            ),
            onAction = {}
        )
    }
}

@Preview(name = "4. Camera - Đang chụp", showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun CameraScreenCapturingPreview() {
    LokapTheme {
        CameraScreen(
            state = CameraUiState(
                permission = CameraPermissionState.GRANTED,
                isCapturing = true
            ),
            onAction = {}
        )
    }
}
