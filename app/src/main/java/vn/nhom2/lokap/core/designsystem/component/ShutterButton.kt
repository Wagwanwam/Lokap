package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
@Composable
fun ShutterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = LokapTheme.colors
    val shapes = LokapTheme.shapes
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.86f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "shutter_scale"
    )

    Box(
        modifier = modifier
            .size(80.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .alpha(if (enabled) 1f else 0.45f)
            .clip(CircleShape)
            .border(
                width = 4.dp,
                color = colors.textPrimary,
                shape = CircleShape
            )
            .semantics {
                role = Role.Button
                contentDescription = "Chụp ảnh"
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val released = tryAwaitRelease()
                        isPressed = false
                        if (released) {
                            onClick()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (enabled) colors.primary else colors.outline)
        )
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
            ShutterButton(onClick = {}, enabled = false)
        }
    }
}
