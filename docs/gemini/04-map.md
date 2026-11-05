```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/data/fake/SampleMapData.kt
package vn.nhom2.lokap.data.fake

import vn.nhom2.lokap.domain.model.MapBubble
import vn.nhom2.lokap.domain.model.MapBubbleType

object SampleMapData {

    private val extraProvinceBubbles = listOf(
        MapBubble(
            id = "prov_hue",
            type = MapBubbleType.PROVINCE,
            code = "46",
            label = "TP. Huế",
            lat = 16.4637,
            lng = 107.5909,
            postCount = 48,
            coverImageUrl = "https://images.unsplash.com/photo-1583417319070-4a69db38a482?w=400",
            hasFriendPosts = true
        ),
        MapBubble(
            id = "prov_hn",
            type = MapBubbleType.PROVINCE,
            code = "01",
            label = "Hà Nội",
            lat = 21.0285,
            lng = 105.8542,
            postCount = 124,
            coverImageUrl = "https://images.unsplash.com/photo-1509030450996-dd1a26dda07a?w=400",
            hasFriendPosts = true
        ),
        MapBubble(
            id = "prov_hcm",
            type = MapBubbleType.PROVINCE,
            code = "79",
            label = "TP. Hồ Chí Minh",
            lat = 10.7769,
            lng = 106.7009,
            postCount = 215,
            coverImageUrl = "https://images.unsplash.com/photo-1583417267826-aebc4d1542e1?w=400",
            hasFriendPosts = true
        )
    )

    val allProvinceBubbles: List<MapBubble>
        get() = (SampleData.provinceMapBubbles + extraProvinceBubbles)
            .distinctBy { it.code ?: it.id }

    private val extraWardBubbles = listOf(
        // Đà Nẵng - Province Wide (nếu chưa có trong SampleData)
        MapBubble(
            id = "pw_dn",
            type = MapBubbleType.PROVINCE_WIDE,
            code = "48",
            label = "Khắp Đà Nẵng",
            lat = 16.0750,
            lng = 108.1800,
            postCount = 19,
            coverImageUrl = "https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?w=400",
            hasFriendPosts = true
        ),
        // TP. Huế - Phường/Xã & Province Wide
        MapBubble(
            id = "pw_hue",
            type = MapBubbleType.PROVINCE_WIDE,
            code = "46",
            label = "Khắp TP. Huế",
            lat = 16.4800,
            lng = 107.5600,
            postCount = 12,
            coverImageUrl = "https://images.unsplash.com/photo-1583417319070-4a69db38a482?w=400",
            hasFriendPosts = false
        ),
        MapBubble(
            id = "ward_hue_thuanhoa",
            type = MapBubbleType.WARD,
            code = "46_thuanhoa",
            label = "P. Thuận Hòa",
            lat = 16.4698,
            lng = 107.5786,
            postCount = 21,
            coverImageUrl = "https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?w=400",
            hasFriendPosts = true
        ),
        MapBubble(
            id = "ward_hue_vy_da",
            type = MapBubbleType.WARD,
            code = "46_vyda",
            label = "P. Vỹ Dạ",
            lat = 16.4765,
            lng = 107.6052,
            postCount = 15,
            coverImageUrl = "https://images.unsplash.com/photo-1528127269322-539801943592?w=400",
            hasFriendPosts = true
        ),
        // Hà Nội - Phường/Xã & Province Wide
        MapBubble(
            id = "pw_hn",
            type = MapBubbleType.PROVINCE_WIDE,
            code = "01",
            label = "Khắp Hà Nội",
            lat = 21.0450,
            lng = 105.8200,
            postCount = 34,
            coverImageUrl = "https://images.unsplash.com/photo-1509030450996-dd1a26dda07a?w=400",
            hasFriendPosts = true
        ),
        MapBubble(
            id = "ward_hn_hangbac",
            type = MapBubbleType.WARD,
            code = "01_hangbac",
            label = "P. Hàng Bạc",
            lat = 21.0328,
            lng = 105.8525,
            postCount = 52,
            coverImageUrl = "https://images.unsplash.com/photo-1555921015-5532091f6026?w=400",
            hasFriendPosts = true
        ),
        MapBubble(
            id = "ward_hn_quangan",
            type = MapBubbleType.WARD,
            code = "01_quangan",
            label = "P. Quảng An",
            lat = 21.0645,
            lng = 105.8235,
            postCount = 38,
            coverImageUrl = "https://images.unsplash.com/photo-1509030450996-dd1a26dda07a?w=400",
            hasFriendPosts = false
        ),
        // TP.HCM - Phường/Xã & Province Wide
        MapBubble(
            id = "pw_hcm",
            type = MapBubbleType.PROVINCE_WIDE,
            code = "79",
            label = "Khắp TP.HCM",
            lat = 10.7950,
            lng = 106.6750,
            postCount = 45,
            coverImageUrl = "https://images.unsplash.com/photo-1583417267826-aebc4d1542e1?w=400",
            hasFriendPosts = true
        ),
        MapBubble(
            id = "ward_hcm_bennghe",
            type = MapBubbleType.WARD,
            code = "79_bennghe",
            label = "P. Bến Nghé",
            lat = 10.7797,
            lng = 106.7019,
            postCount = 96,
            coverImageUrl = "https://images.unsplash.com/photo-1583417267826-aebc4d1542e1?w=400",
            hasFriendPosts = true
        ),
        MapBubble(
            id = "ward_hcm_thaodien",
            type = MapBubbleType.WARD,
            code = "79_thaodien",
            label = "P. Thảo Điền",
            lat = 10.8035,
            lng = 106.7369,
            postCount = 74,
            coverImageUrl = "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=400",
            hasFriendPosts = true
        )
    )

    val allWardAndProvinceWideBubbles: List<MapBubble>
        get() = (SampleData.wardMapBubblesDaNang + extraWardBubbles)
            .distinctBy { it.id }

    private val extraPlaceBubbles = listOf(
        // Huế Places
        MapBubble(
            id = "place_hue_1",
            type = MapBubbleType.PLACE,
            code = SampleData.posts.firstOrNull()?.id ?: "post_1",
            label = "Cà phê Muối Chú Long",
            lat = 16.4682,
            lng = 107.5841,
            postCount = 1,
            coverImageUrl = "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=400",
            hasFriendPosts = true
        ),
        MapBubble(
            id = "place_hue_2",
            type = MapBubbleType.PLACE,
            code = SampleData.posts.getOrNull(1)?.id ?: "post_2",
            label = "Đồi Vọng Cảnh hoàng hôn",
            lat = 16.4312,
            lng = 107.5612,
            postCount = 3,
            coverImageUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=400",
            hasFriendPosts = false
        ),
        // Hà Nội Places
        MapBubble(
            id = "place_hn_1",
            type = MapBubbleType.PLACE,
            code = SampleData.posts.firstOrNull()?.id ?: "post_1",
            label = "Trill Rooftop Cafe",
            lat = 21.0315,
            lng = 105.8512,
            postCount = 4,
            coverImageUrl = "https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=400",
            hasFriendPosts = true
        ),
        MapBubble(
            id = "place_hn_2",
            type = MapBubbleType.PLACE,
            code = SampleData.posts.getOrNull(1)?.id ?: "post_2",
            label = "Hồ Tây chiều thu",
            lat = 21.0610,
            lng = 105.8215,
            postCount = 7,
            coverImageUrl = "https://images.unsplash.com/photo-1509030450996-dd1a26dda07a?w=400",
            hasFriendPosts = true
        ),
        // TP.HCM Places
        MapBubble(
            id = "place_hcm_1",
            type = MapBubbleType.PLACE,
            code = SampleData.posts.firstOrNull()?.id ?: "post_1",
            label = "Bến Bạch Đằng chill đêm",
            lat = 10.7745,
            lng = 106.7068,
            postCount = 9,
            coverImageUrl = "https://images.unsplash.com/photo-1583417267826-aebc4d1542e1?w=400",
            hasFriendPosts = true
        ),
        MapBubble(
            id = "place_hcm_2",
            type = MapBubbleType.PLACE,
            code = SampleData.posts.getOrNull(1)?.id ?: "post_2",
            label = "Matcha Vibe Thảo Điền",
            lat = 10.8041,
            lng = 106.7340,
            postCount = 5,
            coverImageUrl = "https://images.unsplash.com/photo-1515823064-d6e0c04616a7?w=400",
            hasFriendPosts = false
        )
    )

    val allPlaceBubbles: List<MapBubble>
        get() = (SampleData.detailMapBubblesDaNang + extraPlaceBubbles)
            .distinctBy { it.id }

    fun findRegionName(level: String, code: String): String {
        val all = allProvinceBubbles + allWardAndProvinceWideBubbles
        all.firstOrNull { it.code == code }?.let { bubble ->
            return bubble.label.removePrefix("Khắp ").trim()
        }
        return when (code) {
            "48" -> "Đà Nẵng"
            "46" -> "TP. Huế"
            "01" -> "Hà Nội"
            "79" -> "TP. Hồ Chí Minh"
            else -> if (level.equals("WARD", ignoreCase = true)) "Phường $code" else "Khu vực $code"
        }
    }
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/map/LokapMap.kt
package vn.nhom2.lokap.feature.map

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
    modifier: Modifier = Modifier
) {
    FakeMapCanvas(
        bubbles = bubbles,
        landmarks = landmarks,
        camera = camera,
        onCameraChange = onCameraChange,
        onBubbleClick = onBubbleClick,
        onLandmarkClick = onLandmarkClick,
        modifier = modifier
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
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val density = LocalDensity.current.density
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
        return Offset(
            x = widthPx / 2f + dx,
            y = heightPx / 2f + dy
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

                for (i in 0 until landmarkCount) {
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
                    if (x in -placeable.width..width && y in -placeable.height..height) {
                        placeable.placeRelative(x, y)
                    }
                }

                for (j in bubbles.indices) {
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
                    if (x in -placeable.width..width && y in -placeable.height..height) {
                        placeable.placeRelative(x, y)
                    }
                }
            }
        }
    }
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/map/MapScreen.kt
package vn.nhom2.lokap.feature.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.core.designsystem.component.ErrorState
import vn.nhom2.lokap.core.designsystem.component.GhostButton
import vn.nhom2.lokap.core.designsystem.component.GlassChip
import vn.nhom2.lokap.core.designsystem.component.OfflineBanner
import vn.nhom2.lokap.core.designsystem.component.PinkButton
import vn.nhom2.lokap.core.designsystem.component.RegionCard
import vn.nhom2.lokap.core.designsystem.component.ShimmerBox
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.data.fake.FakePostStore
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.data.fake.SampleMapData
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.Landmark
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.MapBubble
import vn.nhom2.lokap.domain.model.MapBubbleType
import vn.nhom2.lokap.domain.model.Post
import java.time.Duration
import kotlin.math.abs

enum class MapAudienceFilter(val label: String) {
    ALL("Tất cả"),
    FRIENDS("Bạn bè"),
    PUBLIC("Public")
}

enum class MapTimeFilter(val label: String, val days: Long?) {
    DAYS_7("7 ngày", 7L),
    DAYS_30("30 ngày", 30L),
    ALL("Tất cả", null)
}

data class RegionSheetPreview(
    val level: String,
    val code: String,
    val regionName: String,
    val photoUrls: List<String>,
    val postCount: Int,
    val visited: Boolean
)

data class MapUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val errorMessage: String? = null,
    val camera: MapCameraState = MapCameraState(
        centerLat = 16.0544,
        centerLng = 108.2022,
        zoom = 6.2f
    ),
    val audienceFilter: MapAudienceFilter = MapAudienceFilter.ALL,
    val timeFilter: MapTimeFilter = MapTimeFilter.ALL,
    val visibleBubbles: List<MapBubble> = emptyList(),
    val landmarks: List<Landmark> = emptyList(),
    val isViewportEmpty: Boolean = false,
    val selectedRegionPreview: RegionSheetPreview? = null
)

sealed interface MapAction {
    data class OnCameraChanged(val camera: MapCameraState) : MapAction
    data class OnAudienceFilterChange(val filter: MapAudienceFilter) : MapAction
    data class OnTimeFilterChange(val filter: MapTimeFilter) : MapAction
    data class OnBubbleClick(val bubble: MapBubble) : MapAction
    data class OnLandmarkClick(val landmark: Landmark) : MapAction
    data object OnMyLocationClick : MapAction
    data object OnDismissSheet : MapAction
    data class OnOpenRegionAlbumClick(val level: String, val code: String) : MapAction
    data object OnRetry : MapAction
    data object OnBackClick : MapAction
}

sealed interface MapEffect {
    data object NavigateBack : MapEffect
    data class NavigateToRegionAlbum(val level: String, val code: String) : MapEffect
    data class NavigateToLandmark(val landmarkId: String) : MapEffect
    data class NavigateToPostDetail(val postId: String) : MapEffect
}

class MapViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState(isLoading = true))
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _effect = Channel<MapEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var cachedPosts: List<Post> = SampleData.posts

    init {
        observePostsAndLoad()
    }

    private fun observePostsAndLoad() {
        viewModelScope.launch {
            delay(350)
            FakePostStore.posts.collectLatest { storePosts ->
                cachedPosts = storePosts.ifEmpty { SampleData.posts }
                recalculateMapData(isLoading = false)
            }
        }
    }

    fun onAction(action: MapAction) {
        when (action) {
            is MapAction.OnCameraChanged -> {
                _uiState.update { it.copy(camera = action.camera) }
                recalculateMapData()
            }

            is MapAction.OnAudienceFilterChange -> {
                _uiState.update { it.copy(audienceFilter = action.filter) }
                recalculateMapData()
            }

            is MapAction.OnTimeFilterChange -> {
                _uiState.update { it.copy(timeFilter = action.filter) }
                recalculateMapData()
            }

            is MapAction.OnMyLocationClick -> {
                _uiState.update {
                    it.copy(
                        camera = MapCameraState(
                            centerLat = 16.0544,
                            centerLng = 108.2022,
                            zoom = 11f
                        )
                    )
                }
                recalculateMapData()
            }

            is MapAction.OnBubbleClick -> handleBubbleClick(action.bubble)

            is MapAction.OnLandmarkClick -> {
                viewModelScope.launch {
                    _effect.send(MapEffect.NavigateToLandmark(action.landmark.id))
                }
            }

            is MapAction.OnDismissSheet -> {
                _uiState.update { it.copy(selectedRegionPreview = null) }
            }

            is MapAction.OnOpenRegionAlbumClick -> {
                _uiState.update { it.copy(selectedRegionPreview = null) }
                viewModelScope.launch {
                    _effect.send(MapEffect.NavigateToRegionAlbum(action.level, action.code))
                }
            }

            is MapAction.OnRetry -> {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                viewModelScope.launch {
                    delay(300)
                    recalculateMapData(isLoading = false)
                }
            }

            is MapAction.OnBackClick -> {
                viewModelScope.launch {
                    _effect.send(MapEffect.NavigateBack)
                }
            }
        }
    }

    private fun handleBubbleClick(bubble: MapBubble) {
        val currentZoom = _uiState.value.camera.zoom
        when (bubble.type) {
            MapBubbleType.PROVINCE -> {
                if (currentZoom < 7.3f) {
                    _uiState.update {
                        it.copy(
                            camera = MapCameraState(
                                centerLat = bubble.lat,
                                centerLng = bubble.lng,
                                zoom = 7.6f
                            )
                        )
                    }
                    recalculateMapData()
                } else {
                    openRegionPreview(level = "PROVINCE", bubble = bubble)
                }
            }

            MapBubbleType.WARD, MapBubbleType.PROVINCE_WIDE -> {
                if (currentZoom < 10.5f) {
                    _uiState.update {
                        it.copy(
                            camera = MapCameraState(
                                centerLat = bubble.lat,
                                centerLng = bubble.lng,
                                zoom = 11.2f
                            )
                        )
                    }
                    recalculateMapData()
                } else {
                    val level = if (bubble.type == MapBubbleType.PROVINCE_WIDE) "PROVINCE" else "WARD"
                    openRegionPreview(level = level, bubble = bubble)
                }
            }

            MapBubbleType.PLACE -> {
                val postId = findPostIdForPlaceBubble(bubble)
                viewModelScope.launch {
                    _effect.send(MapEffect.NavigateToPostDetail(postId))
                }
            }

            MapBubbleType.LANDMARK -> {
                val landmarkId = bubble.code ?: bubble.id
                viewModelScope.launch {
                    _effect.send(MapEffect.NavigateToLandmark(landmarkId))
                }
            }
        }
    }

    private fun openRegionPreview(level: String, bubble: MapBubble) {
        val code = bubble.code ?: bubble.id
        val matchingPosts = filterPostsForRegion(level = level, code = code)
        val photoUrls = matchingPosts.map { it.imageUrl }
            .ifEmpty { listOfNotNull(bubble.coverImageUrl) }
            .take(4)

        val preview = RegionSheetPreview(
            level = level,
            code = code,
            regionName = bubble.label.removePrefix("Khắp ").trim(),
            photoUrls = photoUrls,
            postCount = bubble.postCount.coerceAtLeast(matchingPosts.size),
            visited = bubble.hasFriendPosts || matchingPosts.isNotEmpty()
        )
        _uiState.update { it.copy(selectedRegionPreview = preview) }
    }

    private fun filterPostsForRegion(level: String, code: String): List<Post> {
        // Loại bỏ bài ở mức "Việt Nam" (COUNTRY)
        val validPosts = cachedPosts.filter { it.location.level != LocationLevel.COUNTRY }
        return validPosts.filter { post ->
            if (level == "PROVINCE") {
                post.location.provinceCode == code
            } else {
                post.location.wardCode == code ||
                    (code.contains("_") && post.location.provinceCode == code.substringBefore("_"))
            }
        }
    }

    private fun findPostIdForPlaceBubble(bubble: MapBubble): String {
        val validPosts = cachedPosts.filter { it.location.level != LocationLevel.COUNTRY }
        validPosts.firstOrNull { it.id == bubble.code || it.id == bubble.id }?.let { return it.id }
        validPosts.firstOrNull {
            it.location.placeName?.equals(bubble.label, ignoreCase = true) == true
        }?.let { return it.id }
        return validPosts.firstOrNull()?.id ?: "post_1"
    }

    private fun recalculateMapData(isLoading: Boolean = _uiState.value.isLoading) {
        val state = _uiState.value
        val zoom = state.camera.zoom

        // 1. Lấy danh sách bubble theo mức zoom (Bài mức Việt Nam không lên bản đồ)
        val rawBubbles = when {
            zoom < 8f -> SampleMapData.allProvinceBubbles
            zoom < 13f -> SampleMapData.allWardAndProvinceWideBubbles
            else -> SampleMapData.allPlaceBubbles
        }

        // 2. Lọc theo Audience & Thời gian
        val filteredBubbles = rawBubbles.mapNotNull { bubble ->
            val passesAudience = when (state.audienceFilter) {
                MapAudienceFilter.ALL -> true
                MapAudienceFilter.FRIENDS -> bubble.hasFriendPosts
                MapAudienceFilter.PUBLIC -> true
            }
            if (!passesAudience) return@mapNotNull null

            val timeScale = when (state.timeFilter) {
                MapTimeFilter.ALL -> 1.0f
                MapTimeFilter.DAYS_30 -> 0.75f
                MapTimeFilter.DAYS_7 -> 0.45f
            }

            // Kiểm tra bài viết thực tế trong vùng thời gian (loại bài mức Việt Nam)
            val matchingRealPosts = cachedPosts.filter { post ->
                post.location.level != LocationLevel.COUNTRY &&
                    matchesTimeFilter(post, state.timeFilter) &&
                    matchesAudienceFilter(post, state.audienceFilter)
            }

            val adjustedCount = (bubble.postCount * timeScale).toInt()
                .coerceAtLeast(if (matchingRealPosts.isNotEmpty() && bubble.hasFriendPosts) 1 else 0)

            if (adjustedCount <= 0) null else bubble.copy(postCount = adjustedCount)
        }

        // 3. Kiểm tra vùng khung nhìn hiện tại có bubble hoặc landmark nào không
        val latSpan = 12.0 / Math.pow(2.0, (zoom - 5f).toDouble())
        val lngSpan = 10.0 / Math.pow(2.0, (zoom - 5f).toDouble())

        val bubblesInViewport = filteredBubbles.any { b ->
            abs(b.lat - state.camera.centerLat) <= latSpan &&
                abs(b.lng - state.camera.centerLng) <= lngSpan
        }
        val landmarksInViewport = zoom >= 8f && SampleData.landmarks.any { lm ->
            abs(lm.lat - state.camera.centerLat) <= latSpan &&
                abs(lm.lng - state.camera.centerLng) <= lngSpan
        }

        _uiState.update {
            it.copy(
                isLoading = isLoading,
                visibleBubbles = filteredBubbles,
                landmarks = SampleData.landmarks,
                isViewportEmpty = !isLoading && (!bubblesInViewport && !landmarksInViewport)
            )
        }
    }

    private fun matchesAudienceFilter(post: Post, filter: MapAudienceFilter): Boolean {
        return when (filter) {
            MapAudienceFilter.ALL -> true
            MapAudienceFilter.FRIENDS -> post.audience == Audience.FRIENDS || post.audience == Audience.DIRECT
            MapAudienceFilter.PUBLIC -> post.audience == Audience.PUBLIC
        }
    }

    private fun matchesTimeFilter(post: Post, filter: MapTimeFilter): Boolean {
        val maxDays = filter.days ?: return true
        val diffDays = Duration.between(post.takenAt, SampleData.NOW).toDays()
        return diffDays <= maxDays
    }
}

fun NavGraphBuilder.mapScreen(navController: NavController) {
    composable<Route.Map> {
        MapRoute(
            onBackClick = { navController.popBackStack() },
            onNavigateToRegionAlbum = { level, code ->
                navController.navigate(Route.RegionAlbum(level = level, code = code)) {
                    launchSingleTop = true
                }
            },
            onNavigateToLandmark = { landmarkId ->
                navController.navigate(Route.Landmark(id = landmarkId)) {
                    launchSingleTop = true
                }
            },
            onNavigateToPostDetail = { postId ->
                navController.navigate(Route.PostDetail(id = postId)) {
                    launchSingleTop = true
                }
            }
        )
    }
}

@Composable
fun MapRoute(
    onBackClick: () -> Unit,
    onNavigateToRegionAlbum: (level: String, code: String) -> Unit,
    onNavigateToLandmark: (landmarkId: String) -> Unit,
    onNavigateToPostDetail: (postId: String) -> Unit,
    viewModel: MapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                MapEffect.NavigateBack -> onBackClick()
                is MapEffect.NavigateToRegionAlbum -> onNavigateToRegionAlbum(effect.level, effect.code)
                is MapEffect.NavigateToLandmark -> onNavigateToLandmark(effect.landmarkId)
                is MapEffect.NavigateToPostDetail -> onNavigateToPostDetail(effect.postId)
            }
        }
    }

    MapScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    uiState: MapUiState,
    onAction: (MapAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        when {
            uiState.errorMessage != null -> {
                ErrorState(
                    message = uiState.errorMessage,
                    onRetry = { onAction(MapAction.OnRetry) },
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .padding(spacing.lg)
                )
            }

            uiState.isLoading -> {
                MapLoadingSkeleton(
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                LokapMap(
                    bubbles = uiState.visibleBubbles,
                    landmarks = uiState.landmarks,
                    camera = uiState.camera,
                    onCameraChange = { onAction(MapAction.OnCameraChanged(it)) },
                    onBubbleClick = { onAction(MapAction.OnBubbleClick(it)) },
                    onLandmarkClick = { onAction(MapAction.OnLandmarkClick(it)) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Top Controls: OfflineBanner + Header + Bộ lọc GlassChip
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            if (uiState.isOffline) {
                OfflineBanner(modifier = Modifier.fillMaxWidth())
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(shapes.pill)
                            .background(colors.surfaceHigh.copy(alpha = 0.9f))
                            .clickable { onAction(MapAction.OnBackClick) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = colors.textPrimary
                        )
                    }

                    Column(
                        modifier = Modifier
                            .clip(shapes.pill)
                            .background(colors.surfaceHigh.copy(alpha = 0.9f))
                            .padding(horizontal = spacing.md, vertical = spacing.xs)
                    ) {
                        Text(
                            text = "Bản đồ Lokap",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary
                        )
                        Text(
                            text = zoomLevelLabel(uiState.camera.zoom),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.primary
                        )
                    }
                }

                GlassChip(
                    label = "Vị trí của tôi",
                    leadingIcon = Icons.Default.LocationOn,
                    selected = true,
                    onClick = { onAction(MapAction.OnMyLocationClick) }
                )
            }

            // Hàng lọc 1: Tất cả / Bạn bè / Public
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MapAudienceFilter.entries.forEach { filter ->
                    GlassChip(
                        label = filter.label,
                        selected = uiState.audienceFilter == filter,
                        onClick = { onAction(MapAction.OnAudienceFilterChange(filter)) }
                    )
                }

                Spacer(modifier = Modifier.width(spacing.xs))

                // Hàng lọc 2: 7 ngày / 30 ngày / Tất cả
                MapTimeFilter.entries.forEach { timeFilter ->
                    GlassChip(
                        label = timeFilter.label,
                        selected = uiState.timeFilter == timeFilter,
                        onClick = { onAction(MapAction.OnTimeFilterChange(timeFilter)) }
                    )
                }
            }
        }

        // Chip nổi khi vùng bản đồ không có bài
        AnimatedVisibility(
            visible = !uiState.isLoading && uiState.errorMessage == null && uiState.isViewportEmpty,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
                .padding(bottom = spacing.xl)
        ) {
            GlassChip(
                label = "Chưa có bài ở khu vực này",
                leadingIcon = Icons.Default.Refresh,
                selected = false,
                onClick = { onAction(MapAction.OnMyLocationClick) }
            )
        }

        // ModalBottomSheet xem trước RegionCard + nút "Xem album"
        val regionPreview = uiState.selectedRegionPreview
        if (regionPreview != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { onAction(MapAction.OnDismissSheet) },
                sheetState = sheetState,
                containerColor = colors.surfaceHigh,
                contentColor = colors.textPrimary,
                shape = shapes.sheet
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.lg)
                        .padding(bottom = spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    RegionCard(
                        code = regionPreview.code,
                        regionName = regionPreview.regionName,
                        photoUrls = regionPreview.photoUrls,
                        postCount = regionPreview.postCount,
                        visited = regionPreview.visited,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onAction(
                                MapAction.OnOpenRegionAlbumClick(
                                    level = regionPreview.level,
                                    code = regionPreview.code
                                )
                            )
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        GhostButton(
                            text = "Đóng",
                            onClick = { onAction(MapAction.OnDismissSheet) },
                            modifier = Modifier.weight(1f)
                        )
                        PinkButton(
                            text = "Xem album",
                            onClick = {
                                onAction(
                                    MapAction.OnOpenRegionAlbumClick(
                                        level = regionPreview.level,
                                        code = regionPreview.code
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapLoadingSkeleton(modifier: Modifier = Modifier) {
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes

    Box(
        modifier = modifier
            .systemBarsPadding()
            .padding(spacing.lg)
    ) {
        ShimmerBox(
            modifier = Modifier
                .size(110.dp)
                .align(Alignment.Center),
            shape = shapes.pill
        )
        ShimmerBox(
            modifier = Modifier
                .size(84.dp)
                .align(Alignment.TopEnd)
                .padding(top = 120.dp),
            shape = shapes.pill
        )
        ShimmerBox(
            modifier = Modifier
                .size(92.dp)
                .align(Alignment.BottomStart)
                .padding(bottom = 96.dp),
            shape = shapes.pill
        )
    }
}

private fun zoomLevelLabel(zoom: Float): String = when {
    zoom < 8f -> "34 Tỉnh / Thành • Zoom ${"%.1f".format(zoom)}"
    zoom < 13f -> "Phường / Xã & Địa danh • Zoom ${"%.1f".format(zoom)}"
    else -> "Ghim địa điểm chi tiết • Zoom ${"%.1f".format(zoom)}"
}

@Preview(name = "Map - Tỉnh/Thành (Zoom < 8)", showBackground = true)
@Composable
private fun MapScreenProvincePreview() {
    LokapTheme {
        MapScreen(
            uiState = MapUiState(
                isLoading = false,
                visibleBubbles = SampleMapData.allProvinceBubbles,
                landmarks = SampleData.landmarks
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Map - Phường/Xã + Sheet Xem trước", showBackground = true)
@Composable
private fun MapScreenWardSheetPreview() {
    LokapTheme {
        MapScreen(
            uiState = MapUiState(
                isLoading = false,
                camera = MapCameraState(centerLat = 16.0544, centerLng = 108.2022, zoom = 11f),
                visibleBubbles = SampleMapData.allWardAndProvinceWideBubbles,
                landmarks = SampleData.landmarks,
                selectedRegionPreview = RegionSheetPreview(
                    level = "PROVINCE",
                    code = "48",
                    regionName = "Đà Nẵng",
                    photoUrls = SampleData.posts.map { it.imageUrl }.take(3),
                    postCount = 42,
                    visited = true
                )
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Map - Vùng trống & Offline", showBackground = true)
@Composable
private fun MapScreenEmptyOfflinePreview() {
    LokapTheme {
        MapScreen(
            uiState = MapUiState(
                isLoading = false,
                isOffline = true,
                isViewportEmpty = true,
                visibleBubbles = emptyList()
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Map - Đang tải", showBackground = true)
@Composable
private fun MapScreenLoadingPreview() {
    LokapTheme {
        MapScreen(
            uiState = MapUiState(isLoading = true),
            onAction = {}
        )
    }
}

@Preview(name = "Map - Lỗi", showBackground = true)
@Composable
private fun MapScreenErrorPreview() {
    LokapTheme {
        MapScreen(
            uiState = MapUiState(
                isLoading = false,
                errorMessage = "Không thể tải dữ liệu bản đồ. Kiểm tra kết nối mạng nhé!"
            ),
            onAction = {}
        )
    }
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/region/RegionAlbumScreen.kt
package vn.nhom2.lokap.feature.region

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import coil3.compose.AsyncImage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.core.designsystem.component.EmptyState
import vn.nhom2.lokap.core.designsystem.component.ErrorState
import vn.nhom2.lokap.core.designsystem.component.GhostButton
import vn.nhom2.lokap.core.designsystem.component.GlassChip
import vn.nhom2.lokap.core.designsystem.component.RegionCard
import vn.nhom2.lokap.core.designsystem.component.ShimmerBox
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.data.fake.FakePostStore
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.data.fake.SampleMapData
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.Post

enum class RegionAudienceFilter(val label: String) {
    FRIENDS("Bạn bè"),
    PUBLIC("Public")
}

data class RegionAlbumUiState(
    val level: String = "PROVINCE",
    val code: String = "48",
    val regionName: String = "Đà Nẵng",
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val filter: RegionAudienceFilter = RegionAudienceFilter.PUBLIC,
    val displayedPosts: List<Post> = emptyList(),
    val totalPostCount: Int = 0,
    val canLoadMore: Boolean = false,
    val visited: Boolean = true
)

sealed interface RegionAlbumAction {
    data class OnFilterSelected(val filter: RegionAudienceFilter) : RegionAlbumAction
    data class OnPostClick(val postId: String) : RegionAlbumAction
    data object OnLoadMoreClick : RegionAlbumAction
    data object OnShareClick : RegionAlbumAction
    data object OnRetry : RegionAlbumAction
    data object OnBackClick : RegionAlbumAction
}

sealed interface RegionAlbumEffect {
    data object NavigateBack : RegionAlbumEffect
    data class NavigateToPostDetail(val postId: String) : RegionAlbumEffect
    data class ShowSnackbar(val message: String) : RegionAlbumEffect
}

class RegionAlbumViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val routeArgs: Route.RegionAlbum = runCatching {
        savedStateHandle.toRoute<Route.RegionAlbum>()
    }.getOrElse {
        Route.RegionAlbum(
            level = savedStateHandle["level"] ?: "PROVINCE",
            code = savedStateHandle["code"] ?: "48"
        )
    }

    private val _uiState = MutableStateFlow(
        RegionAlbumUiState(
            level = routeArgs.level,
            code = routeArgs.code,
            regionName = SampleMapData.findRegionName(routeArgs.level, routeArgs.code),
            isLoading = true
        )
    )
    val uiState: StateFlow<RegionAlbumUiState> = _uiState.asStateFlow()

    private val _effect = Channel<RegionAlbumEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val pageSize = 9
    private var currentPage = 1
    private var allRegionPosts: List<Post> = emptyList()

    init {
        loadRegionPosts()
    }

    private fun loadRegionPosts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            delay(320)
            FakePostStore.posts.collectLatest { storePosts ->
                val base = storePosts.ifEmpty { SampleData.posts }
                val matched = base.filter { post ->
                    if (routeArgs.level.equals("WARD", ignoreCase = true)) {
                        post.location.wardCode == routeArgs.code ||
                            (routeArgs.code.contains("_") &&
                                post.location.provinceCode == routeArgs.code.substringBefore("_"))
                    } else {
                        post.location.provinceCode == routeArgs.code
                    }
                }

                // Tạo danh sách đủ nhiều để hỗ trợ phân trang giả mượt mà
                val sourceList = matched.ifEmpty { base }
                allRegionPosts = buildList {
                    repeat(3) { round ->
                        sourceList.forEachIndexed { idx, post ->
                            add(
                                if (round == 0) post
                                else post.copy(id = post.id)
                            )
                        }
                    }
                }
                applyFilterAndPagination(resetPage = true)
            }
        }
    }

    fun onAction(action: RegionAlbumAction) {
        when (action) {
            is RegionAlbumAction.OnFilterSelected -> {
                _uiState.update { it.copy(filter = action.filter) }
                applyFilterAndPagination(resetPage = true)
            }

            is RegionAlbumAction.OnPostClick -> {
                viewModelScope.launch {
                    _effect.send(RegionAlbumEffect.NavigateToPostDetail(action.postId))
                }
            }

            RegionAlbumAction.OnLoadMoreClick -> {
                if (_uiState.value.isLoadingMore || !_uiState.value.canLoadMore) return
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoadingMore = true) }
                    delay(350)
                    currentPage += 1
                    applyFilterAndPagination(resetPage = false)
                }
            }

            RegionAlbumAction.OnShareClick -> {
                viewModelScope.launch {
                    _effect.send(
                        RegionAlbumEffect.ShowSnackbar("Sắp có: chia sẻ thẻ khu vực")
                    )
                }
            }

            RegionAlbumAction.OnRetry -> loadRegionPosts()

            RegionAlbumAction.OnBackClick -> {
                viewModelScope.launch {
                    _effect.send(RegionAlbumEffect.NavigateBack)
                }
            }
        }
    }

    private fun applyFilterAndPagination(resetPage: Boolean) {
        if (resetPage) currentPage = 1
        val currentFilter = _uiState.value.filter

        val filtered = allRegionPosts.filter { post ->
            when (currentFilter) {
                RegionAudienceFilter.FRIENDS ->
                    post.audience == Audience.FRIENDS || post.audience == Audience.DIRECT
                RegionAudienceFilter.PUBLIC ->
                    post.audience == Audience.PUBLIC || post.audience == Audience.FRIENDS
            }
        }

        val limit = currentPage * pageSize
        val sliced = filtered.take(limit)

        _uiState.update {
            it.copy(
                isLoading = false,
                isLoadingMore = false,
                displayedPosts = sliced,
                totalPostCount = filtered.size,
                canLoadMore = filtered.size > sliced.size,
                visited = filtered.isNotEmpty()
            )
        }
    }
}

fun NavGraphBuilder.regionAlbumScreen(navController: NavController) {
    composable<Route.RegionAlbum> {
        RegionAlbumRoute(
            onBackClick = { navController.popBackStack() },
            onNavigateToPostDetail = { postId ->
                navController.navigate(Route.PostDetail(id = postId)) {
                    launchSingleTop = true
                }
            }
        )
    }
}

@Composable
fun RegionAlbumRoute(
    onBackClick: () -> Unit,
    onNavigateToPostDetail: (postId: String) -> Unit,
    viewModel: RegionAlbumViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                RegionAlbumEffect.NavigateBack -> onBackClick()
                is RegionAlbumEffect.NavigateToPostDetail -> onNavigateToPostDetail(effect.postId)
                is RegionAlbumEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    RegionAlbumScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction
    )
}

@Composable
fun RegionAlbumScreen(
    uiState: RegionAlbumUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (RegionAlbumAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.md, vertical = spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(shapes.pill)
                            .background(colors.surfaceHigh)
                            .clickable { onAction(RegionAlbumAction.OnBackClick) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = colors.textPrimary
                        )
                    }
                    Column {
                        Text(
                            text = uiState.regionName,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (uiState.level.equals("WARD", ignoreCase = true)) {
                                "Album Phường / Xã"
                            } else {
                                "Album Tỉnh / Thành phố"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(shapes.pill)
                        .background(colors.surfaceHigh)
                        .clickable { onAction(RegionAlbumAction.OnShareClick) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Chia sẻ thẻ khu vực",
                        tint = colors.primary
                    )
                }
            }

            when {
                uiState.errorMessage != null -> {
                    ErrorState(
                        message = uiState.errorMessage,
                        onRetry = { onAction(RegionAlbumAction.OnRetry) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.lg)
                    )
                }

                uiState.isLoading -> {
                    RegionAlbumLoadingContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = spacing.md)
                    )
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = spacing.md,
                            end = spacing.md,
                            top = spacing.xs,
                            bottom = spacing.xxl
                        ),
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        // Header: RegionCard lớn
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            RegionCard(
                                code = uiState.code,
                                regionName = uiState.regionName,
                                photoUrls = uiState.displayedPosts.map { it.imageUrl }.take(4),
                                postCount = uiState.totalPostCount,
                                visited = uiState.visited,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = spacing.sm)
                            )
                        }

                        // Hàng lọc Bạn bè / Public
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = spacing.sm),
                                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RegionAudienceFilter.entries.forEach { filter ->
                                    GlassChip(
                                        label = filter.label,
                                        selected = uiState.filter == filter,
                                        onClick = { onAction(RegionAlbumAction.OnFilterSelected(filter)) }
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "${uiState.totalPostCount} khoảnh khắc",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        if (uiState.displayedPosts.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                EmptyState(
                                    title = "Chưa ai đăng ở đây, hãy là người đầu tiên",
                                    subtitle = "Mở camera Lokap và thả chiếc ảnh đầu tiên tại ${uiState.regionName} nhé!",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = spacing.xxl)
                                )
                            }
                        } else {
                            items(
                                items = uiState.displayedPosts,
                                key = { post -> "${post.id}_${uiState.displayedPosts.indexOf(post)}" }
                            ) { post ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(shapes.card)
                                        .background(colors.surfaceHigh)
                                        .clickable { onAction(RegionAlbumAction.OnPostClick(post.id)) }
                                ) {
                                    AsyncImage(
                                        model = post.imageUrl,
                                        contentDescription = post.caption,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            if (uiState.canLoadMore) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = spacing.md),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        GhostButton(
                                            text = if (uiState.isLoadingMore) {
                                                "Đang tải thêm..."
                                            } else {
                                                "Xem thêm ảnh (${uiState.displayedPosts.size}/${uiState.totalPostCount})"
                                            },
                                            onClick = { onAction(RegionAlbumAction.OnLoadMoreClick) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(spacing.md)
        )
    }
}

@Composable
private fun RegionAlbumLoadingContent(modifier: Modifier = Modifier) {
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            shape = shapes.card
        )
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            ShimmerBox(modifier = Modifier.size(width = 88.dp, height = 34.dp), shape = shapes.pill)
            ShimmerBox(modifier = Modifier.size(width = 88.dp, height = 34.dp), shape = shapes.pill)
        }
        repeat(3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                repeat(3) {
                    ShimmerBox(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        shape = shapes.card
                    )
                }
            }
        }
    }
}

@Preview(name = "RegionAlbum - Đầy đủ dữ liệu", showBackground = true)
@Composable
private fun RegionAlbumPopulatedPreview() {
    LokapTheme {
        RegionAlbumScreen(
            uiState = RegionAlbumUiState(
                level = "PROVINCE",
                code = "48",
                regionName = "Đà Nẵng",
                isLoading = false,
                displayedPosts = SampleData.posts,
                totalPostCount = 24,
                canLoadMore = true,
                visited = true
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {}
        )
    }
}

@Preview(name = "RegionAlbum - Trống", showBackground = true)
@Composable
private fun RegionAlbumEmptyPreview() {
    LokapTheme {
        RegionAlbumScreen(
            uiState = RegionAlbumUiState(
                level = "WARD",
                code = "46_vyda",
                regionName = "P. Vỹ Dạ",
                isLoading = false,
                displayedPosts = emptyList(),
                totalPostCount = 0,
                visited = false
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {}
        )
    }
}

@Preview(name = "RegionAlbum - Đang tải", showBackground = true)
@Composable
private fun RegionAlbumLoadingPreview() {
    LokapTheme {
        RegionAlbumScreen(
            uiState = RegionAlbumUiState(isLoading = true),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {}
        )
    }
}

@Preview(name = "RegionAlbum - Lỗi", showBackground = true)
@Composable
private fun RegionAlbumErrorPreview() {
    LokapTheme {
        RegionAlbumScreen(
            uiState = RegionAlbumUiState(
                isLoading = false,
                errorMessage = "Không thể tải album khu vực này. Thử lại sau nhé!"
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {}
        )
    }
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/landmark/LandmarkScreen.kt
package vn.nhom2.lokap.feature.landmark

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import coil3.compose.AsyncImage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.core.designsystem.component.EmptyState
import vn.nhom2.lokap.core.designsystem.component.ErrorState
import vn.nhom2.lokap.core.designsystem.component.GlassChip
import vn.nhom2.lokap.core.designsystem.component.ShimmerBox
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.data.fake.FakePostStore
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.data.fake.SampleMapData
import vn.nhom2.lokap.domain.model.Landmark
import vn.nhom2.lokap.domain.model.Post

data class LandmarkUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val landmark: Landmark? = null,
    val provinceName: String = "",
    val posts: List<Post> = emptyList()
)

sealed interface LandmarkAction {
    data class OnPostClick(val postId: String) : LandmarkAction
    data object OnRetry : LandmarkAction
    data object OnBackClick : LandmarkAction
}

sealed interface LandmarkEffect {
    data object NavigateBack : LandmarkEffect
    data class NavigateToPostDetail(val postId: String) : LandmarkEffect
}

class LandmarkViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val landmarkId: String = runCatching {
        savedStateHandle.toRoute<Route.Landmark>().id
    }.getOrElse {
        savedStateHandle["id"] ?: SampleData.landmarks.firstOrNull()?.id.orEmpty()
    }

    private val _uiState = MutableStateFlow(LandmarkUiState(isLoading = true))
    val uiState: StateFlow<LandmarkUiState> = _uiState.asStateFlow()

    private val _effect = Channel<LandmarkEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadLandmarkDetails()
    }

    private fun loadLandmarkDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            delay(300)

            val foundLandmark = SampleData.landmarks.firstOrNull { it.id == landmarkId }
                ?: SampleData.landmarks.firstOrNull()

            if (foundLandmark == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Không tìm thấy thông tin địa danh này."
                    )
                }
                return@launch
            }

            val provinceLabel = SampleMapData.findRegionName(
                level = "PROVINCE",
                code = foundLandmark.provinceCode
            )

            FakePostStore.posts.collectLatest { storePosts ->
                val basePosts = storePosts.ifEmpty { SampleData.posts }
                val landmarkPosts = basePosts.filter { post ->
                    post.location.landmarkId == foundLandmark.id ||
                        post.location.provinceCode == foundLandmark.provinceCode
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        landmark = foundLandmark,
                        provinceName = provinceLabel,
                        posts = landmarkPosts
                    )
                }
            }
        }
    }

    fun onAction(action: LandmarkAction) {
        when (action) {
            is LandmarkAction.OnPostClick -> {
                viewModelScope.launch {
                    _effect.send(LandmarkEffect.NavigateToPostDetail(action.postId))
                }
            }

            LandmarkAction.OnRetry -> loadLandmarkDetails()

            LandmarkAction.OnBackClick -> {
                viewModelScope.launch {
                    _effect.send(LandmarkEffect.NavigateBack)
                }
            }
        }
    }
}

fun NavGraphBuilder.landmarkScreen(navController: NavController) {
    composable<Route.Landmark> {
        LandmarkRoute(
            onBackClick = { navController.popBackStack() },
            onNavigateToPostDetail = { postId ->
                navController.navigate(Route.PostDetail(id = postId)) {
                    launchSingleTop = true
                }
            }
        )
    }
}

@Composable
fun LandmarkRoute(
    onBackClick: () -> Unit,
    onNavigateToPostDetail: (postId: String) -> Unit,
    viewModel: LandmarkViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                LandmarkEffect.NavigateBack -> onBackClick()
                is LandmarkEffect.NavigateToPostDetail -> onNavigateToPostDetail(effect.postId)
            }
        }
    }

    LandmarkScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@Composable
fun LandmarkScreen(
    uiState: LandmarkUiState,
    onAction: (LandmarkAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
    ) {
        when {
            uiState.errorMessage != null -> {
                ErrorState(
                    message = uiState.errorMessage,
                    onRetry = { onAction(LandmarkAction.OnRetry) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(spacing.lg)
                )
            }

            uiState.isLoading || uiState.landmark == null -> {
                LandmarkLoadingContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(spacing.md)
                )
            }

            else -> {
                val landmark = uiState.landmark
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = spacing.md,
                        end = spacing.md,
                        top = spacing.sm,
                        bottom = spacing.xxl
                    ),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    // Header: Nút quay lại + Ảnh bìa lớn + Nguồn ảnh + Tên + Tỉnh + Mô tả + Số bài
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(spacing.sm)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(shapes.pill)
                                        .background(colors.surfaceHigh)
                                        .clickable { onAction(LandmarkAction.OnBackClick) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Quay lại",
                                        tint = colors.textPrimary
                                    )
                                }

                                GlassChip(
                                    label = uiState.provinceName,
                                    leadingIcon = Icons.Default.LocationOn,
                                    selected = true
                                )
                            }

                            // Ảnh bìa lớn
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(230.dp)
                                    .clip(shapes.card)
                                    .background(colors.surfaceHigh)
                            ) {
                                AsyncImage(
                                    model = landmark.coverUrl,
                                    contentDescription = landmark.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Dòng nguồn ảnh (coverAttribution) nhỏ ngay dưới ảnh bìa
                            Text(
                                text = landmark.coverAttribution,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textTertiary
                            )

                            // Tên địa danh (chữ lớn)
                            Text(
                                text = landmark.name,
                                style = MaterialTheme.typography.headlineMedium,
                                color = colors.textPrimary
                            )

                            // Tỉnh & Số bài
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                            ) {
                                GlassChip(
                                    label = uiState.provinceName,
                                    selected = false
                                )
                                GlassChip(
                                    label = "${landmark.postCount.coerceAtLeast(uiState.posts.size)} bài đăng",
                                    selected = true
                                )
                            }

                            // Mô tả
                            Text(
                                text = landmark.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )

                            Spacer(modifier = Modifier.height(spacing.xs))

                            Text(
                                text = "Khoảnh khắc chụp tại đây",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary,
                                modifier = Modifier.padding(bottom = spacing.xs)
                            )
                        }
                    }

                    if (uiState.posts.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            EmptyState(
                                title = "Hãy là người đầu tiên chụp ở đây",
                                subtitle = "Check-in tại ${landmark.name} để ghim ảnh của bạn lên bản đồ Lokap!",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = spacing.xxl)
                            )
                        }
                    } else {
                        items(
                            items = uiState.posts,
                            key = { it.id }
                        ) { post ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(shapes.card)
                                    .background(colors.surfaceHigh)
                                    .clickable { onAction(LandmarkAction.OnPostClick(post.id)) }
                            ) {
                                AsyncImage(
                                    model = post.imageUrl,
                                    contentDescription = post.caption,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LandmarkLoadingContent(modifier: Modifier = Modifier) {
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        ShimmerBox(modifier = Modifier.size(40.dp), shape = shapes.pill)
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp),
            shape = shapes.card
        )
        ShimmerBox(modifier = Modifier.size(width = 160.dp, height = 14.dp), shape = shapes.pill)
        ShimmerBox(modifier = Modifier.size(width = 240.dp, height = 32.dp), shape = shapes.pill)
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = shapes.card
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            repeat(3) {
                ShimmerBox(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    shape = shapes.card
                )
            }
        }
    }
}

@Preview(name = "Landmark - Có ảnh", showBackground = true)
@Composable
private fun LandmarkPopulatedPreview() {
    LokapTheme {
        LandmarkScreen(
            uiState = LandmarkUiState(
                isLoading = false,
                landmark = SampleData.landmarks.firstOrNull(),
                provinceName = "Đà Nẵng",
                posts = SampleData.posts
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Landmark - Chưa có ảnh", showBackground = true)
@Composable
private fun LandmarkEmptyPreview() {
    LokapTheme {
        LandmarkScreen(
            uiState = LandmarkUiState(
                isLoading = false,
                landmark = SampleData.landmarks.firstOrNull()?.copy(postCount = 0),
                provinceName = "TP. Huế",
                posts = emptyList()
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Landmark - Đang tải", showBackground = true)
@Composable
private fun LandmarkLoadingPreview() {
    LokapTheme {
        LandmarkScreen(
            uiState = LandmarkUiState(isLoading = true),
            onAction = {}
        )
    }
}

@Preview(name = "Landmark - Lỗi", showBackground = true)
@Composable
private fun LandmarkErrorPreview() {
    LokapTheme {
        LandmarkScreen(
            uiState = LandmarkUiState(
                isLoading = false,
                errorMessage = "Không thể tải thông tin địa danh. Vui lòng thử lại!"
            ),
            onAction = {}
        )
    }
}
```

```kotlin
// Thêm các import vào đầu file AppNavHost.kt:
import vn.nhom2.lokap.feature.landmark.landmarkScreen
import vn.nhom2.lokap.feature.map.mapScreen
import vn.nhom2.lokap.feature.region.regionAlbumScreen

// Bên trong khối NavHost(navController = navController, startDestination = ...) { ... }:
mapScreen(navController)
regionAlbumScreen(navController)
landmarkScreen(navController)
```

<!-- tail -->
       posts = emptyList()
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Landmark - Đang tải", showBackground = true)
@Composable
private fun LandmarkLoadingPreview() {
    LokapTheme {
        LandmarkScreen(
            uiState = LandmarkUiState(isLoading = true),
            onAction = {}
        )
    }
}

@Preview(name = "Landmark - Lỗi", showBackground = true)
@Composable
private fun LandmarkErrorPreview() {
    LokapTheme {
        LandmarkScreen(
            uiState = LandmarkUiState(
                isLoading = false,
                errorMessage = "Không thể tải thông tin địa danh. Vui lòng thử lại!"
            ),
            onAction = {}
        )
    }
}

1. Import và các dòng cần thêm vào AppNavHost.kt
Kotlin
// Thêm các import vào đầu file AppNavHost.kt:
import vn.nhom2.lokap.feature.landmark.landmarkScreen
import vn.nhom2.lokap.feature.map.mapScreen
import vn.nhom2.lokap.feature.region.regionAlbumScreen

// Bên trong khối NavHost(navController = navController, startDestination = ...) { ... }:
mapScreen(navController)
regionAlbumScreen(navController)
landmarkScreen(navController)

2. Dependency mới

Không cần thêm dependency mới (toàn bộ sử dụng các thư viện đã có sẵn trong project: androidx.compose.material3:material3, androidx.compose.material:material-icons-core, androidx.navigation:navigation-compose, androidx.lifecycle:lifecycle-viewmodel-compose,  androidx.lifecycle:lifecycle-runtime-compose, io.coil-kt.coil3:coil-compose).