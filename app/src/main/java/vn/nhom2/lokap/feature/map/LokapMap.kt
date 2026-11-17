package vn.nhom2.lokap.feature.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import vn.nhom2.lokap.BuildConfig
import vn.nhom2.lokap.core.designsystem.component.*
import vn.nhom2.lokap.core.designsystem.theme.DarkBackground
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.rememberEntrySettled
import vn.nhom2.lokap.domain.model.*
import vn.nhom2.lokap.domain.repository.DeviceLocation
import vn.vietmap.vietmapsdk.camera.CameraPosition
import vn.vietmap.vietmapsdk.camera.CameraUpdateFactory
import vn.vietmap.vietmapsdk.geometry.LatLng
import vn.vietmap.vietmapsdk.maps.MapView
import vn.vietmap.vietmapsdk.maps.Style
import vn.vietmap.vietmapsdk.maps.VietMapGL
import vn.vietmap.vietmapsdk.maps.VietMapGLOptions

data class MapCameraState(
    val centerLat: Double = 16.0544,
    val centerLng: Double = 108.2022,
    val zoom: Float = 6.2f
)

private val VIETNAM_OUTLINE_POINTS: List<Pair<Double, Double>> = listOf(
    21.50 to 107.97,
    21.00 to 107.40,
    20.80 to 106.70,
    20.25 to 106.58,
    19.80 to 105.85,
    18.80 to 105.72,
    18.08 to 106.45,
    17.48 to 106.62,
    16.82 to 107.18,
    16.46 to 107.65,
    16.12 to 108.22,
    15.88 to 108.38,
    15.12 to 108.88,
    14.40 to 109.12,
    13.77 to 109.24,
    13.08 to 109.32,
    12.25 to 109.20,
    11.58 to 109.02,
    10.92 to 108.10,
    10.34 to 107.08,
    9.60 to 106.60,
    9.15 to 105.72,
    8.60 to 104.72,
    9.20 to 104.82,
    10.00 to 105.08,
    10.38 to 104.48,
    10.85 to 105.10,
    11.65 to 105.95,
    12.80 to 107.55,
    14.35 to 107.50,
    16.60 to 106.58,
    18.45 to 105.15,
    20.20 to 104.60,
    21.40 to 103.00,
    22.48 to 103.97,
    23.37 to 105.32,
    22.80 to 106.75,
    21.85 to 106.76,
    21.50 to 107.97
)

private val HOANG_SA_POINTS = listOf(
    16.85 to 112.15,
    16.50 to 112.35,
    16.35 to 111.75
)

private val TRUONG_SA_POINTS = listOf(
    10.35 to 114.35,
    9.75 to 114.25,
    8.65 to 111.92
)

@Composable
fun LokapMap(
    bubbles: List<MapBubble>,
    landmarks: List<Landmark>,
    camera: MapCameraState,
    onCameraChange: (MapCameraState) -> Unit,
    onBubbleClick: (MapBubble) -> Unit,
    onLandmarkClick: (Landmark) -> Unit,
    modifier: Modifier = Modifier,
    /** Chiều cao phần header/thanh lọc phủ trên bản đồ: tâm camera nằm giữa phần còn lại. */
    topInset: Dp = 0.dp,
    /** Phần đáy bị thanh điều hướng hệ thống che (bong bóng không nằm dưới nút ◀ ● ■). */
    bottomInset: Dp = 0.dp,
    /** Vị trí của tôi (sau khi bấm nút định vị): vẽ chấm đỏ khi zoom ≥ 8, null = không vẽ. */
    myLocation: DeviceLocation? = null
) {
    // Có key Tilemap của VietMap: bản đồ thật (VietMap SDK). Chưa có key / đang Preview: bản đồ vẽ tay như tuần 2.
    if (BuildConfig.VIETMAP_TILE_KEY.isNotBlank() && !LocalInspectionMode.current) {
        VietMapCanvas(
            bubbles = bubbles,
            landmarks = landmarks,
            myLocation = myLocation,
            camera = camera,
            onCameraChange = onCameraChange,
            onBubbleClick = onBubbleClick,
            onLandmarkClick = onLandmarkClick,
            modifier = modifier,
            topInset = topInset,
            bottomInset = bottomInset
        )
    } else {
        FakeMapCanvas(
            bubbles = bubbles,
            landmarks = landmarks,
            myLocation = myLocation,
            camera = camera,
            onCameraChange = onCameraChange,
            onBubbleClick = onBubbleClick,
            onLandmarkClick = onLandmarkClick,
            modifier = modifier,
            topInset = topInset,
            bottomInset = bottomInset
        )
    }
}

/** Style tối của VietMap (app chỉ có giao diện tối). */
private const val VIETMAP_DARK_STYLE = "https://maps.vietmap.vn/maps/styles/dm/style.json?apikey="

/** Nền khi bản đồ chưa vẽ xong: style "dm" là style tối nên dùng nền tối cho cả 2 giao diện, không bị chớp sáng. */
private val MAP_LOADING_COLOR = DarkBackground

/**
 * Bản đồ VietMap SDK (MapView gói trong AndroidView). Người dùng kéo/phóng trực tiếp trên MapView;
 * khi camera dừng thì báo về ViewModel (để tính lại bong bóng theo mức zoom), còn khi ViewModel đổi
 * camera (bấm bong bóng tỉnh → phóng vào) thì bản đồ bay tới. Bong bóng vẫn là Compose, đặt theo
 * toạ độ màn hình do VietMap chiếu ra (Projection.toScreenLocation).
 */
@Composable
private fun VietMapCanvas(
    bubbles: List<MapBubble>,
    landmarks: List<Landmark>,
    myLocation: DeviceLocation?,
    camera: MapCameraState,
    onCameraChange: (MapCameraState) -> Unit,
    onBubbleClick: (MapBubble) -> Unit,
    onLandmarkClick: (Landmark) -> Unit,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
    bottomInset: Dp = 0.dp
) {
    // Tạo MapView trong frame đầu của animation chuyển màn (nhất là khi Back) làm animation giật:
    // đợi animation xong mới dựng bản đồ, trong lúc chờ chỉ vẽ nền cùng màu nền lúc bản đồ tải.
    if (rememberEntrySettled()) {
        VietMapCanvasContent(
            bubbles = bubbles,
            landmarks = landmarks,
            myLocation = myLocation,
            camera = camera,
            onCameraChange = onCameraChange,
            onBubbleClick = onBubbleClick,
            onLandmarkClick = onLandmarkClick,
            modifier = modifier,
            topInset = topInset,
            bottomInset = bottomInset
        )
    } else {
        Box(modifier.fillMaxSize().background(MAP_LOADING_COLOR))
    }
}

@Composable
private fun VietMapCanvasContent(
    bubbles: List<MapBubble>,
    landmarks: List<Landmark>,
    myLocation: DeviceLocation?,
    camera: MapCameraState,
    onCameraChange: (MapCameraState) -> Unit,
    onBubbleClick: (MapBubble) -> Unit,
    onLandmarkClick: (Landmark) -> Unit,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
    bottomInset: Dp = 0.dp
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val density = LocalDensity.current
    val currentOnCameraChange by rememberUpdatedState(onCameraChange)
    var map by remember { mutableStateOf<VietMapGL?>(null) }
    // Tăng mỗi khi camera nhích → đặt lại vị trí bong bóng theo khung hình mới
    var cameraTick by remember { mutableIntStateOf(0) }
    var zoom by remember { mutableFloatStateOf(camera.zoom) }

    val mapView = remember {
        MapView(
            context,
            VietMapGLOptions.createFromAttributes(context).foregroundLoadColor(MAP_LOADING_COLOR.toArgb())
        ).apply {
            onCreate(null)
            getMapAsync { vietMap ->
                vietMap.uiSettings.apply {
                    isRotateGesturesEnabled = false
                    isTiltGesturesEnabled = false
                    isCompassEnabled = false
                }
                vietMap.setMinZoomPreference(4.5)
                vietMap.setMaxZoomPreference(17.0)
                // Bản đồ nền dùng key consumer Tilemap, không dùng key API tìm kiếm
                vietMap.setStyle(Style.Builder().fromUri(VIETMAP_DARK_STYLE + BuildConfig.VIETMAP_TILE_KEY))
                vietMap.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(camera.centerLat, camera.centerLng))
                    .zoom(camera.zoom.toDouble())
                    .build()
                vietMap.addOnCameraMoveListener {
                    zoom = vietMap.cameraPosition.zoom.toFloat()
                    cameraTick++
                }
                vietMap.addOnCameraIdleListener {
                    val position = vietMap.cameraPosition
                    zoom = position.zoom.toFloat()
                    cameraTick++
                    position.target?.let { target ->
                        currentOnCameraChange(MapCameraState(target.latitude, target.longitude, position.zoom.toFloat()))
                    }
                }
                map = vietMap
            }
        }
    }

    // MapView cần nhận vòng đời của màn hình (start/resume/pause/stop) và được huỷ khi rời màn
    DisposableEffect(lifecycle, mapView) {
        var state = Lifecycle.Event.ON_CREATE
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
            state = event
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (state == Lifecycle.Event.ON_RESUME) mapView.onPause()
            if (state == Lifecycle.Event.ON_RESUME || state == Lifecycle.Event.ON_START || state == Lifecycle.Event.ON_PAUSE) {
                mapView.onStop()
            }
            mapView.onDestroy()
        }
    }

    // Header phủ phía trên, thanh điều hướng phía dưới: dời tâm bản đồ vào phần còn thấy được
    val topPx = with(density) { topInset.roundToPx() }
    val bottomPx = with(density) { bottomInset.roundToPx() }
    LaunchedEffect(map, topPx, bottomPx) {
        map?.setPadding(0, topPx, 0, bottomPx)
        cameraTick++
    }

    // ViewModel đổi camera (bấm bong bóng, nút vị trí của tôi) → bay tới; trùng vị trí hiện tại thì thôi
    LaunchedEffect(map, camera) {
        val vietMap = map ?: return@LaunchedEffect
        val current = vietMap.cameraPosition
        val target = current.target
        val moved = target == null ||
            abs(target.latitude - camera.centerLat) > 1e-4 ||
            abs(target.longitude - camera.centerLng) > 1e-4 ||
            abs(current.zoom - camera.zoom) > 0.01
        if (moved) {
            vietMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(camera.centerLat, camera.centerLng), camera.zoom.toDouble()),
                600
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        val vietMap = map
        if (vietMap != null) {
            MapMarkersLayer(
                bubbles = bubbles,
                landmarks = landmarks,
                myLocation = myLocation,
                zoom = zoom,
                onBubbleClick = onBubbleClick,
                onLandmarkClick = onLandmarkClick,
                invalidation = { cameraTick },
                project = { lat, lng, _, _ ->
                    val point = vietMap.projection.toScreenLocation(LatLng(lat, lng))
                    Offset(point.x, point.y)
                }
            )
        }
    }
}

@Composable
private fun FakeMapCanvas(
    bubbles: List<MapBubble>,
    landmarks: List<Landmark>,
    myLocation: DeviceLocation?,
    camera: MapCameraState,
    onCameraChange: (MapCameraState) -> Unit,
    onBubbleClick: (MapBubble) -> Unit,
    onLandmarkClick: (Landmark) -> Unit,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
    bottomInset: Dp = 0.dp
) {
    val colors = LokapTheme.colors
    val density = LocalDensity.current.density
    val topInsetPx = topInset.value * density
    val bottomInsetPx = bottomInset.value * density
    val currentCamera by rememberUpdatedState(camera)
    val currentOnCameraChange by rememberUpdatedState(onCameraChange)

    fun scaleForZoom(zoom: Float): Float = mapScaleForZoom(zoom, density)

    fun projectToPixel(
        lat: Double,
        lng: Double,
        cam: MapCameraState,
        widthPx: Float,
        heightPx: Float
    ): Offset = projectLatLngToPixel(lat, lng, cam, widthPx, heightPx, density, topInsetPx, bottomInsetPx)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoomChange, _ ->
                    val oldCam = currentCamera
                    val newZoom = (oldCam.zoom + (kotlin.math.ln(zoomChange) / kotlin.math.ln(2f)))
                        .coerceIn(5f, 16f)
                    val scale = scaleForZoom(newZoom)
                    val cosLat = cos(Math.toRadians(oldCam.centerLat.coerceIn(-75.0, 75.0))).toFloat()
                    val newLat = (oldCam.centerLat + (pan.y / scale)).coerceIn(7.0, 25.0)
                    val newLng = (oldCam.centerLng - (pan.x / (scale * cosLat))).coerceIn(101.0, 116.0)
                    currentOnCameraChange(
                        oldCam.copy(
                            centerLat = newLat,
                            centerLng = newLng,
                            zoom = newZoom
                        )
                    )
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val gridStep = when {
                camera.zoom < 7.5f -> 2.0
                camera.zoom < 10.5f -> 0.5
                camera.zoom < 13f -> 0.1
                else -> 0.02
            }
            val startLat = (kotlin.math.floor(6.0 / gridStep) * gridStep)
            var latLine = startLat
            while (latLine <= 25.0) {
                val p1 = projectToPixel(latLine, 100.0, camera, w, h)
                val p2 = projectToPixel(latLine, 117.0, camera, w, h)
                if (p1.y in -50f..(h + 50f)) {
                    drawLine(
                        color = colors.outline.copy(alpha = 0.22f),
                        start = Offset(0f, p1.y),
                        end = Offset(w, p2.y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                latLine += gridStep
            }

            val startLng = (kotlin.math.floor(101.0 / gridStep) * gridStep)
            var lngLine = startLng
            while (lngLine <= 116.0) {
                val p1 = projectToPixel(25.0, lngLine, camera, w, h)
                val p2 = projectToPixel(6.0, lngLine, camera, w, h)
                if (p1.x in -50f..(w + 50f)) {
                    drawLine(
                        color = colors.outline.copy(alpha = 0.22f),
                        start = Offset(p1.x, 0f),
                        end = Offset(p2.x, h),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                lngLine += gridStep
            }

            val vnPath = Path()
            VIETNAM_OUTLINE_POINTS.forEachIndexed { index, (lat, lng) ->
                val pt = projectToPixel(lat, lng, camera, w, h)
                if (index == 0) {
                    vnPath.moveTo(pt.x, pt.y)
                } else {
                    vnPath.lineTo(pt.x, pt.y)
                }
            }
            vnPath.close()

            drawPath(
                path = vnPath,
                color = colors.surfaceHigh.copy(alpha = 0.55f)
            )
            drawPath(
                path = vnPath,
                color = colors.primarySoft.copy(alpha = 0.45f),
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            HOANG_SA_POINTS.forEach { (lat, lng) ->
                val pt = projectToPixel(lat, lng, camera, w, h)
                drawCircle(
                    color = colors.primarySoft.copy(alpha = 0.65f),
                    radius = 4.dp.toPx(),
                    center = pt
                )
            }
            TRUONG_SA_POINTS.forEach { (lat, lng) ->
                val pt = projectToPixel(lat, lng, camera, w, h)
                drawCircle(
                    color = colors.primarySoft.copy(alpha = 0.65f),
                    radius = 4.dp.toPx(),
                    center = pt
                )
            }
        }

        MapMarkersLayer(
            bubbles = bubbles,
            landmarks = landmarks,
            myLocation = myLocation,
            zoom = camera.zoom,
            onBubbleClick = onBubbleClick,
            onLandmarkClick = onLandmarkClick,
            invalidation = { 0 },
            project = { lat, lng, width, height -> projectToPixel(lat, lng, camera, width.toFloat(), height.toFloat()) }
        )
    }
}

/**
 * Lớp bong bóng + địa danh phủ trên bản đồ (dùng chung cho bản đồ vẽ tay và VietMap).
 * [project]: toạ độ (+ kích thước lớp) → pixel; [invalidation] đọc trong lúc đặt vị trí để camera nhích là đặt lại.
 */
@Composable
private fun MapMarkersLayer(
    bubbles: List<MapBubble>,
    landmarks: List<Landmark>,
    myLocation: DeviceLocation?,
    zoom: Float,
    onBubbleClick: (MapBubble) -> Unit,
    onLandmarkClick: (Landmark) -> Unit,
    invalidation: () -> Int,
    project: (lat: Double, lng: Double, width: Int, height: Int) -> Offset
) {
    val visibleLandmarks = if (zoom >= 8f) landmarks else emptyList()
    // Thu nhỏ ra mức cả nước / vùng (dưới mức thấy địa danh) thì ẩn chấm vị trí của tôi: chỉ còn bong bóng tỉnh
    val dotLocation = myLocation?.takeIf { zoom >= 8f }

    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            visibleLandmarks.forEach { landmark ->
                LandmarkBubble(
                    landmark = landmark,
                    onClick = { onLandmarkClick(landmark) }
                )
            }
            bubbles.forEach { bubble ->
                MapBubbleItem(
                    bubble = bubble,
                    zoom = zoom,
                    onClick = { onBubbleClick(bubble) }
                )
            }
            // Phần tử cuối: đặt sau cùng nên vẽ đè lên mọi bong bóng
            if (dotLocation != null) MyLocationDot()
        }
    ) { measurables, constraints ->
        invalidation()
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val childConstraints = Constraints()
        val placeables = measurables.map { it.measure(childConstraints) }

        layout(width, height) {
            val landmarkCount = visibleLandmarks.size
            fun rectAt(lat: Double, lng: Double, w: Int, h: Int): IntRect {
                val pos = project(lat, lng, width, height)
                val x = (pos.x - w / 2f).roundToInt()
                val y = (pos.y - h / 2f).roundToInt()
                return IntRect(x, y, x + w, y + h)
            }
            val candidates = visibleLandmarks.mapIndexed { i, lm ->
                MapPlacementCandidate(
                    index = i,
                    rect = rectAt(lm.lat, lm.lng, placeables[i].width, placeables[i].height),
                    kind = if (lm.postCount > 0) MapPlacementKind.LANDMARK_WITH_POSTS else MapPlacementKind.LANDMARK_EMPTY,
                    postCount = lm.postCount
                )
            } + bubbles.mapIndexed { j, b ->
                val pl = placeables[landmarkCount + j]
                MapPlacementCandidate(
                    index = landmarkCount + j,
                    rect = rectAt(b.lat, b.lng, pl.width, pl.height),
                    kind = MapPlacementKind.BUBBLE,
                    postCount = b.postCount
                )
            }
            resolveMapPlacement(candidates, width, height, zoom).forEach { c ->
                placeables[c.index].placeRelative(c.rect.left, c.rect.top)
            }
            // Chấm vị trí của tôi không tham gia né chồng lấn (không che mất bong bóng nào vì không bắt chạm)
            if (dotLocation != null) {
                val dot = placeables.last()
                val rect = rectAt(dotLocation.lat, dotLocation.lng, dot.width, dot.height)
                dot.placeRelative(rect.left, rect.top)
            }
        }
    }
}

internal fun mapScaleForZoom(zoom: Float, density: Float): Float = 28f * density * 2f.pow(zoom - 5f)

/**
 * Toạ độ -> pixel; tâm camera ở giữa vùng KHÔNG bị che:
 * phía trên là header (topInsetPx), phía dưới là thanh điều hướng hệ thống (bottomInsetPx).
 */
internal fun projectLatLngToPixel(
    lat: Double,
    lng: Double,
    cam: MapCameraState,
    widthPx: Float,
    heightPx: Float,
    density: Float,
    topInsetPx: Float = 0f,
    bottomInsetPx: Float = 0f
): Offset {
    val scale = mapScaleForZoom(cam.zoom, density)
    val cosLat = cos(Math.toRadians(cam.centerLat.coerceIn(-75.0, 75.0))).toFloat()
    val dx = (lng - cam.centerLng).toFloat() * scale * cosLat
    val dy = (cam.centerLat - lat).toFloat() * scale
    val visibleTop = topInsetPx.coerceIn(0f, heightPx)
    val visibleBottom = (heightPx - bottomInsetPx).coerceIn(visibleTop, heightPx)
    return Offset(x = widthPx / 2f + dx, y = (visibleTop + visibleBottom) / 2f + dy)
}

internal enum class MapPlacementKind { LANDMARK_WITH_POSTS, BUBBLE, LANDMARK_EMPTY }

internal const val LANDMARK_PRIORITY_ZOOM = 11f

internal fun placementPriority(kind: MapPlacementKind, zoom: Float): Int = when (kind) {
    MapPlacementKind.BUBBLE -> if (zoom < LANDMARK_PRIORITY_ZOOM) 0 else 1
    MapPlacementKind.LANDMARK_WITH_POSTS -> if (zoom < LANDMARK_PRIORITY_ZOOM) 1 else 0
    MapPlacementKind.LANDMARK_EMPTY -> 2
}

internal data class MapPlacementCandidate(
    val index: Int,
    val rect: IntRect,
    val kind: MapPlacementKind,
    val postCount: Int
)

internal fun resolveMapPlacement(
    candidates: List<MapPlacementCandidate>,
    width: Int,
    height: Int,
    zoom: Float
): List<MapPlacementCandidate> {
    val placed = mutableListOf<MapPlacementCandidate>()
    candidates
        .sortedWith(compareBy<MapPlacementCandidate>({ placementPriority(it.kind, zoom) }, { -it.postCount }))
        .forEach { c ->
            val onScreen = c.rect.left in -c.rect.width..width && c.rect.top in -c.rect.height..height
            if (onScreen && placed.none { it.rect.overlaps(c.rect) }) placed += c
        }
    return placed
}
