package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme

/**
 * Nút chụp.
 * - Chạm nhanh: [onClick].
 * - Nhấn giữ (không xê dịch) quá thời gian long-press: [onLongPressStart], sau đó mỗi lần ngón tay di chuyển
 *   gọi [onLongPressDrag] với vị trí ngón tay so với TÂM nút (px), thả tay gọi [onLongPressEnd].
 * - [centerIcon]: icon nhỏ trong lõi hồng (ví dụ bộ lọc đang bật).
 */
@Composable
fun ShutterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 80.dp,
    centerIcon: ImageVector? = null,
    longPressLabel: String? = null,
    onLongPressStart: (() -> Unit)? = null,
    onLongPressDrag: ((Offset) -> Unit)? = null,
    onLongPressEnd: (() -> Unit)? = null,
    onAccessibilityLongClick: (() -> Unit)? = null
) {
    val colors = LokapTheme.colors
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    var isHolding by remember { mutableStateOf(false) }

    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnStart by rememberUpdatedState(onLongPressStart)
    val currentOnDrag by rememberUpdatedState(onLongPressDrag)
    val currentOnEnd by rememberUpdatedState(onLongPressEnd)

    val scale by animateFloatAsState(
        targetValue = when {
            !enabled -> 1f
            isHolding -> 0.8f
            isPressed -> 0.86f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "shutter_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .alpha(if (enabled) 1f else 0.45f)
            .clip(CircleShape)
            .border(
                width = size * 0.05f,
                color = colors.textPrimary,
                shape = CircleShape
            )
            .semantics {
                role = Role.Button
                contentDescription = "Chụp ảnh"
                val a11yLongClick = onAccessibilityLongClick
                if (enabled && a11yLongClick != null) {
                    onLongClick(label = longPressLabel) {
                        a11yLongClick()
                        true
                    }
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    isPressed = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val center = Offset(this.size.width / 2f, this.size.height / 2f)

                    // Chờ thả tay hoặc hết thời gian long-press; xê dịch quá touch slop = vuốt (nhường cho màn hình)
                    var released = false
                    var cancelled = false
                    withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || change.isConsumed) {
                                cancelled = true
                                break
                            }
                            if (!change.pressed) {
                                released = true
                                change.consume()
                                break
                            }
                            if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                cancelled = true
                                break
                            }
                        }
                    }

                    if (released) {
                        isPressed = false
                        currentOnClick()
                        return@awaitEachGesture
                    }
                    val onStart = currentOnStart
                    if (cancelled || onStart == null) {
                        // Không có chế độ nhấn giữ: thả tay vẫn tính là chụp (giống hành vi cũ)
                        if (!cancelled) {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    change.consume()
                                    currentOnClick()
                                    break
                                }
                            }
                        }
                        isPressed = false
                        return@awaitEachGesture
                    }

                    // Nhấn giữ: mở vòng, theo ngón tay tới khi thả
                    isHolding = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onStart()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        change.consume()
                        if (!change.pressed) break
                        currentOnDrag?.invoke(change.position - center)
                    }
                    isHolding = false
                    isPressed = false
                    currentOnEnd?.invoke()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.8f)
                .clip(CircleShape)
                .background(if (enabled) colors.primary else colors.outline),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = centerIcon,
                transitionSpec = { (fadeIn(tween(150)) + scaleIn(initialScale = 0.6f)) togetherWith fadeOut(tween(100)) },
                label = "shutter_icon"
            ) { icon ->
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.34f)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun ShutterButtonPreview() {
    LokapTheme {
        Row(
            modifier = Modifier
                .background(LokapTheme.colors.background)
                .padding(LokapTheme.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.xl)
        ) {
            ShutterButton(onClick = {}, enabled = true)
            ShutterButton(onClick = {}, enabled = true, size = 96.dp, centerIcon = Icons.Rounded.WbSunny)
            ShutterButton(onClick = {}, enabled = false)
        }
    }
}
