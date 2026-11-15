package vn.nhom2.lokap.feature.map

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.nhom2.lokap.core.designsystem.component.AvatarRing
import vn.nhom2.lokap.core.designsystem.component.PickerRingItem
import vn.nhom2.lokap.core.designsystem.component.PickerRingLabel
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.domain.model.User

/** Hai nút kéo ở cạnh phải bản đồ. */
internal enum class MapPicker { AUDIENCE, FRIEND }

/** Một lựa chọn trên vòng: icon (Tất cả, Bạn bè…) hoặc avatar của một người bạn. */
@Immutable
internal data class MapPickerOption(
    val label: String,
    val icon: ImageVector? = null,
    val user: User? = null
)

val MapAudienceFilter.icon: ImageVector
    get() = when (this) {
        MapAudienceFilter.ALL -> Icons.Rounded.Layers
        MapAudienceFilter.FRIENDS -> Icons.Rounded.People
        MapAudienceFilter.PUBLIC -> Icons.Rounded.Public
    }

/**
 * Hình học vòng chọn của bản đồ. Nút nằm sát cạnh phải nên các lựa chọn xếp trên cung tròn
 * phía bên trái nút (góc 0 = thẳng sang trái, dương = xuống dưới). Cung trong hết chỗ thì sang cung ngoài,
 * nên đủ chỗ cho 20 bạn bè + "Tất cả bạn bè".
 */
internal object MapPickerRing {
    val ButtonSize = 52.dp
    val ItemSize = 52.dp
    /** Khoảng cách tối thiểu giữa tâm hai lựa chọn cạnh nhau trên cùng một cung. */
    val ItemSpacing = 62.dp
    val FirstRadius = 100.dp
    val RingGap = 64.dp
    /** Ngón tay phải ra khỏi vùng nút mới bắt đầu chọn. */
    val ActivationRadius = 36.dp
    /** Chỗ chừa phía trên vòng cho nhãn tên lựa chọn. */
    val LabelSpace = 44.dp

    private const val MAX_TILT_DEG = 80.0
    private const val PREFERRED_STEP_DEG = 42.0

    /**
     * Tâm từng lựa chọn so với tâm nút (px; y hướng xuống).
     * [maxUpPx], [maxDownPx]: khoảng trống phía trên / dưới tâm nút để lựa chọn không tràn khỏi phần bản đồ thấy được.
     */
    fun layout(
        count: Int,
        firstRadiusPx: Float,
        ringGapPx: Float,
        spacingPx: Float,
        maxUpPx: Float,
        maxDownPx: Float
    ): List<Offset> {
        if (count <= 0) return emptyList()
        val out = ArrayList<Offset>(count)
        var radius = firstRadiusPx.toDouble()
        while (out.size < count) {
            val up = maxTiltDeg(maxUpPx, radius)
            val down = maxTiltDeg(maxDownPx, radius)
            val range = up + down
            // Dây cung giữa hai lựa chọn liền nhau phải ≥ spacing
            val minStep = Math.toDegrees(2 * asin(min(1.0, spacingPx / (2 * radius))))
            val capacity = (range / minStep).toInt() + 1
            val n = min(count - out.size, capacity)
            // Ít lựa chọn thì giãn vừa phải, nhiều thì trải hết cung
            val step = if (n > 1) min(max(minStep, PREFERRED_STEP_DEG), range / (n - 1)) else 0.0
            val span = step * (n - 1)
            val low = -up + span / 2
            val center = 0.0.coerceIn(low, max(low, down - span / 2))
            for (i in 0 until n) {
                val phi = Math.toRadians(center - span / 2 + i * step)
                out += Offset((-radius * cos(phi)).toFloat(), (radius * sin(phi)).toFloat())
            }
            radius += ringGapPx
        }
        return out
    }

    private fun maxTiltDeg(spacePx: Float, radius: Double): Double {
        val ratio = spacePx.coerceAtLeast(0f) / radius
        return if (ratio >= 1.0) MAX_TILT_DEG else min(MAX_TILT_DEG, Math.toDegrees(asin(ratio)))
    }

    /**
     * Lựa chọn gần ngón tay nhất. [finger]: vị trí ngón tay so với tâm nút (px; y hướng xuống).
     * null khi ngón tay còn trong vùng nút hoặc không ở gần lựa chọn nào.
     */
    fun indexAt(finger: Offset, positions: List<Offset>, activationPx: Float, hitRadiusPx: Float): Int? {
        if (finger.getDistance() < activationPx) return null
        var best = -1
        var bestDistance = Float.MAX_VALUE
        positions.forEachIndexed { i, p ->
            val d = (p - finger).getDistance()
            if (d < bestDistance) {
                best = i
                bestDistance = d
            }
        }
        return best.takeIf { it >= 0 && bestDistance <= hitRadiusPx }
    }
}

/** Nút tròn nổi trên bản đồ (vị trí của tôi, nút lọc). */
@Composable
internal fun MapControlButton(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = LokapTheme.colors
    Box(
        modifier = modifier
            .size(MapPickerRing.ButtonSize)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(colors.surface.copy(alpha = 0.94f))
            .border(
                width = if (highlighted) 2.dp else 1.dp,
                color = if (highlighted) colors.primary else colors.outline.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/**
 * Nút kéo (giống nút chụp ở Camera):
 * - Chạm nhanh: [onTap] (đổi sang lựa chọn kế tiếp).
 * - Nhấn giữ hoặc kéo ra khỏi nút: [onOpen], sau đó mỗi lần ngón tay di chuyển gọi [onDrag]
 *   với vị trí ngón tay so với TÂM nút (px), thả tay gọi [onRelease].
 */
@Composable
internal fun MapPickerButton(
    contentDescription: String,
    stateDescription: String,
    highlighted: Boolean,
    onTap: () -> Unit,
    onOpen: () -> Unit,
    onDrag: (Offset) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var holding by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (holding) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "map_picker_scale"
    )
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnOpen by rememberUpdatedState(onOpen)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnRelease by rememberUpdatedState(onRelease)

    MapControlButton(
        highlighted = highlighted,
        modifier = modifier
            .semantics(mergeDescendants = true) {
                role = Role.Button
                this.contentDescription = contentDescription
                this.stateDescription = stateDescription
                onClick(label = "Đổi lựa chọn") {
                    currentOnTap()
                    true
                }
            }
            // pointerInput đặt trước graphicsLayer: toạ độ ngón tay không bị co theo hiệu ứng nhấn
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    val center = Offset(size.width / 2f, size.height / 2f)
                    var last = down.position
                    var released = false
                    var lost = false
                    withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null) {
                                lost = true
                                break
                            }
                            change.consume()
                            if (!change.pressed) {
                                released = true
                                break
                            }
                            last = change.position
                            // Kéo ra khỏi nút thì mở vòng ngay, không cần chờ nhấn giữ
                            if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) break
                        }
                    }
                    if (lost) return@awaitEachGesture
                    if (released) {
                        currentOnTap()
                        return@awaitEachGesture
                    }

                    holding = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    currentOnOpen()
                    currentOnDrag(last - center)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        change.consume()
                        if (!change.pressed) break
                        currentOnDrag(change.position - center)
                    }
                    holding = false
                    currentOnRelease()
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        content = content
    )
}

/** Làm tối bản đồ khi vòng chọn mở để các lựa chọn và nhãn dễ đọc. */
@Composable
internal fun BoxScope.MapPickerScrim(open: Boolean) {
    val scrim = MaterialTheme.colorScheme.scrim
    val alpha by animateFloatAsState(if (open) 0.4f else 0f, tween(180), label = "map_picker_scrim")
    if (alpha > 0f) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind { drawRect(scrim, alpha = alpha) }
        )
    }
}

/**
 * Vòng lựa chọn quanh nút đang kéo. Không nhận chạm (ngón tay vẫn thuộc về nút);
 * [anchor]: tâm nút trong toạ độ khung chứa, [positions]: từ [MapPickerRing.layout].
 */
@Composable
internal fun BoxScope.MapPickerRingOverlay(
    open: Boolean,
    anchor: Offset?,
    positions: List<Offset>,
    options: List<MapPickerOption>,
    selectedIndex: Int,
    hoveredIndex: Int?
) {
    val density = LocalDensity.current
    val itemPx = with(density) { MapPickerRing.ItemSize.toPx() }
    val labelGapPx = with(density) { 12.dp.toPx() }
    val marginPx = with(density) { 12.dp.toPx() }
    val count = min(options.size, positions.size)

    // Mỗi lựa chọn bật ra từ tâm nút, lệch nhau một chút cho cảm giác "nở" ra
    val progress = remember(options.size) { List(options.size) { Animatable(0f) } }
    LaunchedEffect(open, progress) {
        val stagger = if (progress.size > 1) min(18L, 240L / progress.size) else 0L
        progress.forEachIndexed { i, anim ->
            launch {
                if (open) {
                    delay(i * stagger)
                    anim.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow))
                } else {
                    anim.animateTo(0f, tween(durationMillis = 140))
                }
            }
        }
    }
    val labelAlpha by animateFloatAsState(if (open) 1f else 0f, tween(160), label = "map_ring_label")
    if (anchor == null || count == 0) return
    if (!open && progress.all { it.value == 0f }) return

    for (i in 0 until count) {
        key(i) {
            val isHovered = i == hoveredIndex
            val hoverScale by animateFloatAsState(
                targetValue = if (isHovered) 1.25f else 1f,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
                label = "map_ring_item_scale"
            )
            MapRingItem(
                option = options[i],
                isSelected = i == selectedIndex,
                isHovered = isHovered,
                modifier = Modifier
                    .zIndex(if (isHovered) 1f else 0f)
                    .centerAt { anchor + positions[i] * progress[i].value }
                    .graphicsLayer {
                        val p = progress[i].value
                        val s = (0.35f + 0.65f * p) * hoverScale
                        scaleX = s
                        scaleY = s
                        alpha = p.coerceIn(0f, 1f)
                    }
            )
        }
    }

    // Tên lựa chọn: phía trên vòng, nền hồng để đọc được trên bản đồ
    val label = options.getOrNull(hoveredIndex ?: selectedIndex)?.label ?: return
    val shown = positions.take(count)
    val labelBottom = anchor.y + shown.minOf { it.y } - itemPx / 2f - labelGapPx
    val labelCenterX = anchor.x + shown.map { it.x }.average().toFloat()
    AnimatedContent(
        targetState = label,
        transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(90)) },
        label = "map_ring_label_text",
        modifier = Modifier
            .zIndex(2f)
            .layout { measurable, constraints ->
                val maxWidth = (constraints.maxWidth - 2 * marginPx).roundToInt().coerceAtLeast(0)
                val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0, maxWidth = maxWidth))
                layout(placeable.width, placeable.height) {
                    val x = (labelCenterX - placeable.width / 2f)
                        .coerceIn(marginPx, max(marginPx, constraints.maxWidth - placeable.width - marginPx))
                    placeable.place(x.roundToInt(), (labelBottom - placeable.height).roundToInt())
                }
            }
            .graphicsLayer { alpha = labelAlpha }
    ) { text ->
        PickerRingLabel(text = text)
    }
}

/** Nhãn ngắn hiện cạnh nút sau khi chạm để đổi lựa chọn. */
@Composable
internal fun MapPickerHintPill(text: String, modifier: Modifier = Modifier) {
    PickerRingLabel(text = text, modifier = modifier)
}

/** Một lựa chọn trên vòng: cùng kiểu hồng với vòng bộ lọc của Camera. */
@Composable
private fun MapRingItem(
    option: MapPickerOption,
    isSelected: Boolean,
    isHovered: Boolean,
    modifier: Modifier = Modifier
) {
    val user = option.user
    PickerRingItem(
        selected = isSelected,
        hovered = isHovered,
        size = MapPickerRing.ItemSize,
        icon = if (user == null) option.icon else null,
        modifier = modifier
    ) {
        if (user != null) {
            AvatarRing(
                avatarUrl = user.avatarUrl,
                displayName = user.displayName,
                size = MapPickerRing.ItemSize - 8.dp
            )
        }
    }
}

/** Đặt tâm phần tử tại [position] (px, toạ độ khung chứa). */
private fun Modifier.centerAt(position: () -> Offset): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    layout(placeable.width, placeable.height) {
        val p = position()
        placeable.place((p.x - placeable.width / 2f).roundToInt(), (p.y - placeable.height / 2f).roundToInt())
    }
}
