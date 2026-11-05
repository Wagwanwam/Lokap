package vn.nhom2.lokap.feature.map

import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import vn.nhom2.lokap.core.designsystem.component.LandmarkBubble
import vn.nhom2.lokap.core.designsystem.component.MapBubbleItem
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.domain.model.Landmark
import vn.nhom2.lokap.domain.model.MapBubble
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt

data class MapCameraState(
    val centerLat: Double = 16.0544,
    val centerLng: Double = 108.2022,
    val zoom: Float = 6.2f
)

private val VIETNAM_OUTLINE_POINTS: List<Pair<Double, Double>> = listOf(
    21.50 to 107.97, // Móng Cái
    21.00 to 107.40, // Hạ Long
    20.80 to 106.70, // Hải Phòng
    20.25 to 106.58, // Thái Bình
    19.80 to 105.85, // Thanh Hóa
    18.80 to 105.72, // Vinh
    18.08 to 106.45, // Kỳ Anh
    17.48 to 106.62, // Đồng Hới
    16.82 to 107.18, // Cửa Việt
    16.46 to 107.65, // Thuận An - Huế
    16.12 to 108.22, // Bán đảo Sơn Trà - Đà Nẵng
    15.88 to 108.38, // Hội An
    15.12 to 108.88, // Quảng Ngãi
    14.40 to 109.12, // Bồng Sơn
    13.77 to 109.24, // Quy Nhơn
    13.08 to 109.32, // Tuy Hòa
    12.25 to 109.20, // Nha Trang
    11.58 to 109.02, // Phan Rang
    10.92 to 108.10, // Mũi Né - Phan Thiết
    10.34 to 107.08, // Vũng Tàu
    9.60 to 106.60,  // Trà Vinh
    9.15 to 105.72,  // Bạc Liêu
    8.60 to 104.72,  // Mũi Cà Mau
    9.20 to 104.82,  // Bờ Tây Cà Mau
    10.00 to 105.08, // Rạch Giá
    10.38 to 104.48, // Hà Tiên
    10.85 to 105.10, // Châu Đốc
    11.65 to 105.95, // Tây Ninh
    12.80 to 107.55, // Đắk Nông
    14.35 to 107.50, // Kon Tum
    16.60 to 106.58, // Lao Bảo
    18.45 to 105.15, // Hương Sơn
    20.20 to 104.60, // Mộc Châu
    21.40 to 103.00, // Điện Biên
    22.48 to 103.97, // Lào Cai
    23.37 to 105.32, // Lũng Cú - Hà Giang
    22.80 to 106.75, // Cao Bằng
    21.85 to 106.76, // Lạng Sơn
    21.50 to 107.97  // Khép kín về Móng Cái
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
    topInset: Dp = 0.dp
) {
    FakeMapCanvas(
        bubbles = bubbles,
        landmarks = landmarks,
        camera = camera,
        onCameraChange = onCameraChange,
        onBubbleClick = onBubbleClick,
        onLandmarkClick = onLandmarkClick,
        modifier = modifier,
        topInset = topInset
    )
}

@Composable
private fun FakeMapCanvas(
    bubbles: List<MapBubble>,
    landmarks: List<Landmark>,
    camera: MapCameraState,
    onCameraChange: (MapCameraState) -> Unit,
    onBubbleClick: (MapBubble) -> Unit,
    onLandmarkClick: (Landmark) -> Unit,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp
) {
    val colors = LokapTheme.colors
    val density = LocalDensity.current.density
    val topInsetPx = topInset.value * density
    val currentCamera by rememberUpdatedState(camera)
    val currentOnCameraChange by rememberUpdatedState(onCameraChange)

    // Mỗi bậc zoom gấp đôi tỉ lệ, ở zoom = 5.0 có 28dp cho mỗi độ vĩ tuyến
    val basePxPerLatDegree = 28f * density

    fun scaleForZoom(zoom: Float): Float {
        return basePxPerLatDegree * 2f.pow(zoom - 5f)
    }

    fun projectToPixel(
        lat: Double,
        lng: Double,
        cam: MapCameraState,
        widthPx: Float,
        heightPx: Float
    ): Offset {
        val scale = scaleForZoom(cam.zoom)
        val cosLat = cos(Math.toRadians(cam.centerLat.coerceIn(-75.0, 75.0))).toFloat()
        val dx = (lng - cam.centerLng).toFloat() * scale * cosLat
        val dy = (cam.centerLat - lat).toFloat() * scale
        // Tâm camera ở giữa vùng KHÔNG bị header che, để bong bóng không nằm dưới thanh lọc
        val visibleTop = topInsetPx.coerceIn(0f, heightPx)
        return Offset(
            x = widthPx / 2f + dx,
            y = visibleTop + (heightPx - visibleTop) / 2f + dy
        )
    }

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

            // 1. Vẽ lưới kinh/vĩ tuyến mờ
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

            // 2. Vẽ phác hoạ bờ biển & biên giới Việt Nam
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

            // 3. Quần đảo Hoàng Sa & Trường Sa
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

        // 4. Đặt các LandmarkBubble & MapBubbleItem theo toạ độ thực
        val visibleLandmarks = if (camera.zoom >= 8f) landmarks else emptyList()

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
                        onClick = { onBubbleClick(bubble) }
                    )
                }
            }
        ) { measurables, constraints ->
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            val childConstraints = Constraints()
            val placeables = measurables.map { it.measure(childConstraints) }

            layout(width, height) {
                val landmarkCount = visibleLandmarks.size
                val placedRects = mutableListOf<IntRect>()

                // Địa danh đặt trước (ưu tiên hiện), trong nhóm địa danh thì cái nhiều bài trước; đè nhau thì ẩn
                val landmarkOrder = (0 until landmarkCount).sortedByDescending { visibleLandmarks[it].postCount }
                for (i in landmarkOrder) {
                    val lm = visibleLandmarks[i]
                    val placeable = placeables[i]
                    val pos = projectToPixel(
                        lat = lm.lat,
                        lng = lm.lng,
                        cam = camera,
                        widthPx = width.toFloat(),
                        heightPx = height.toFloat()
                    )
                    val x = (pos.x - placeable.width / 2f).roundToInt()
                    val y = (pos.y - placeable.height / 2f).roundToInt()
                    val rect = IntRect(x, y, x + placeable.width, y + placeable.height)
                    if (x in -placeable.width..width && y in -placeable.height..height &&
                        placedRects.none { it.overlaps(rect) }
                    ) {
                        placeable.placeRelative(x, y)
                        placedRects += rect
                    }
                }

                // Bong bóng nhiều bài được đặt trước; bong bóng nào đè lên cái đã đặt (kể cả địa danh) thì ẩn
                val bubbleOrder = bubbles.indices.sortedByDescending { bubbles[it].postCount }
                for (j in bubbleOrder) {
                    val bubble = bubbles[j]
                    val placeable = placeables[landmarkCount + j]
                    val pos = projectToPixel(
                        lat = bubble.lat,
                        lng = bubble.lng,
                        cam = camera,
                        widthPx = width.toFloat(),
                        heightPx = height.toFloat()
                    )
                    val x = (pos.x - placeable.width / 2f).roundToInt()
                    val y = (pos.y - placeable.height / 2f).roundToInt()
                    val rect = IntRect(x, y, x + placeable.width, y + placeable.height)
                    if (x in -placeable.width..width && y in -placeable.height..height &&
                        placedRects.none { it.overlaps(rect) }
                    ) {
                        placeable.placeRelative(x, y)
                        placedRects += rect
                    }
                }
            }
        }
    }
}
