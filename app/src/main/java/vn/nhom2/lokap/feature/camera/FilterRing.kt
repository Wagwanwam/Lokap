package vn.nhom2.lokap.feature.camera

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.FilterVintage
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.nhom2.lokap.core.designsystem.component.PickerRingItem
import vn.nhom2.lokap.core.designsystem.component.PickerRingLabel
import vn.nhom2.lokap.core.image.PhotoColorFilter

/**
 * Hình học của vòng bộ lọc: các nút xếp trên nửa vòng tròn phía trên nút chụp,
 * từ trái (180°) sang phải (0°). Dùng chung cho vẽ và nhận ngón tay.
 */
internal object FilterRing {
    val Radius = 136.dp
    val ItemSize = 54.dp
    /** Ngón tay phải ra khỏi vùng nút chụp mới bắt đầu chọn. */
    val ActivationRadius = 56.dp
    /** Cho phép lệch xuống dưới đường ngang bao nhiêu độ ở hai đầu vòng. */
    const val BELOW_TOLERANCE_DEG = 35f

    val filters: List<PhotoColorFilter> = PhotoColorFilter.entries

    fun angleDeg(index: Int, count: Int = filters.size): Float =
        180f - index * (180f / (count - 1))

    /** Vị trí tâm nút thứ [index], so với tâm nút chụp (px; y hướng xuống). */
    fun itemOffset(index: Int, radiusPx: Float, count: Int = filters.size): Offset {
        val rad = Math.toRadians(angleDeg(index, count).toDouble())
        return Offset((cos(rad) * radiusPx).toFloat(), (-sin(rad) * radiusPx).toFloat())
    }

    /**
     * Nút nào đang dưới ngón tay. [dx], [dy]: vị trí ngón tay so với tâm nút chụp (px; y hướng xuống).
     * null khi ngón tay còn trong vùng nút chụp hoặc kéo hẳn xuống dưới.
     */
    fun indexAt(dx: Float, dy: Float, activationPx: Float, count: Int = filters.size): Int? {
        if (hypot(dx, dy) < activationPx) return null
        var angle = Math.toDegrees(atan2(-dy, dx).toDouble()).toFloat() // 90° = thẳng lên
        if (angle < 0f) {
            angle = when {
                angle > -BELOW_TOLERANCE_DEG -> 0f
                angle < -180f + BELOW_TOLERANCE_DEG -> 180f
                else -> return null
            }
        }
        val step = 180f / (count - 1)
        return ((180f - angle) / step).roundToInt().coerceIn(0, count - 1)
    }
}

val PhotoColorFilter.icon: ImageVector
    get() = when (this) {
        PhotoColorFilter.ORIGINAL -> Icons.Rounded.Tune
        PhotoColorFilter.VIVID -> Icons.Rounded.Palette
        PhotoColorFilter.WARM_GOLDEN -> Icons.Rounded.WbSunny
        PhotoColorFilter.COOL_BREEZE -> Icons.Rounded.Air
        PhotoColorFilter.NOSTALGIA -> Icons.Rounded.FilterVintage
        PhotoColorFilter.MONO_CHIC -> Icons.Rounded.Contrast
        PhotoColorFilter.DREAMY_PASTEL -> Icons.Rounded.AutoAwesome
        PhotoColorFilter.CINEMATIC -> Icons.Rounded.Movie
    }

/** ColorFilter của Compose cho bộ lọc; null = Gốc. */
fun PhotoColorFilter.composeColorFilter(): ColorFilter? =
    matrix?.let { ColorFilter.colorMatrix(ColorMatrix(it)) }

/**
 * Vòng bộ lọc hiện quanh nút chụp khi nhấn giữ. Không nhận chạm (ngón tay vẫn thuộc nút chụp);
 * [center] là tâm nút chụp trong toạ độ của khung chứa overlay.
 */
@Composable
internal fun BoxScope.FilterRingOverlay(
    open: Boolean,
    selected: PhotoColorFilter,
    hovered: PhotoColorFilter?,
    center: Offset
) {
    val density = LocalDensity.current
    val radiusPx = with(density) { FilterRing.Radius.toPx() }
    val itemPx = with(density) { FilterRing.ItemSize.toPx() }
    val labelGapPx = with(density) { 14.dp.toPx() }
    val filters = FilterRing.filters

    // Mỗi nút bật ra từ tâm nút chụp, lệch nhau một chút cho cảm giác "nở" ra
    val progress = remember { filters.map { Animatable(0f) } }
    LaunchedEffect(open) {
        progress.forEachIndexed { i, anim ->
            launch {
                if (open) {
                    delay(i * 18L)
                    anim.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow))
                } else {
                    anim.animateTo(0f, tween(durationMillis = 140))
                }
            }
        }
    }
    val labelAlpha by animateFloatAsState(if (open) 1f else 0f, tween(160), label = "ring_label")
    if (!open && progress.all { it.value == 0f }) return

    filters.forEachIndexed { i, filter ->
        val p = progress[i].value
        val target = FilterRing.itemOffset(i, radiusPx)
        val isHovered = filter == hovered
        val isSelected = filter == selected
        val hoverScale by animateFloatAsState(
            targetValue = if (isHovered) 1.25f else 1f,
            animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
            label = "ring_item_scale"
        )
        // Màu hồng chủ đạo (token của theme): đang trỏ thì nền hồng, đang dùng thì viền hồng dày
        PickerRingItem(
            selected = isSelected,
            hovered = isHovered,
            icon = filter.icon,
            size = FilterRing.ItemSize,
            modifier = Modifier
                .centerAt { center + target * p }
                .graphicsLayer {
                    val s = (0.35f + 0.65f * p) * hoverScale
                    scaleX = s
                    scaleY = s
                    alpha = p.coerceIn(0f, 1f)
                }
        )
    }

    // Tên bộ lọc: căn giữa phía trên vòng, nền hồng để đọc được trên ảnh
    val labelTop = center.y - radiusPx - itemPx / 2f - labelGapPx
    AnimatedContent(
        targetState = (hovered ?: selected).labelVi,
        transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(90)) },
        label = "ring_label_text",
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                layout(placeable.width, placeable.height) {
                    placeable.place(
                        (center.x - placeable.width / 2f).roundToInt(),
                        (labelTop - placeable.height).roundToInt()
                    )
                }
            }
            .graphicsLayer { alpha = labelAlpha }
    ) { label ->
        PickerRingLabel(text = label)
    }
}

/** Đặt tâm phần tử tại [position] (px, toạ độ khung chứa), giống offset nhưng tính theo tâm. */
private fun Modifier.centerAt(position: () -> Offset): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    layout(placeable.width, placeable.height) {
        val p = position()
        placeable.place((p.x - placeable.width / 2f).roundToInt(), (p.y - placeable.height / 2f).roundToInt())
    }
}
