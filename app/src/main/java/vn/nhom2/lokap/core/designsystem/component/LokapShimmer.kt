package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
fun Modifier.lokapShimmer(shape: Shape = RoundedCornerShape(8.dp)): Modifier = composed {
    val isDark = LokapTheme.colors.isDark
    val shimmerColors = if (isDark) {
        listOf(
            LokapTheme.colors.surfaceHigh,
            LokapTheme.colors.surfaceHigh.copy(alpha = 0.5f),
            LokapTheme.colors.surfaceHigh
        )
    } else {
        listOf(
            LokapTheme.colors.surfaceHigh.copy(alpha = 0.85f),
            LokapTheme.colors.surface.copy(alpha = 0.35f),
            LokapTheme.colors.surfaceHigh.copy(alpha = 0.85f)
        )
    }

    val transition = rememberInfiniteTransition(label = "ShimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = -500f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerTranslation"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim, translateAnim),
        end = Offset(translateAnim + 300f, translateAnim + 300f)
    )

    this
        .clip(shape)
        .background(brush)
}

@Composable
fun LokapShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    Box(modifier = modifier.lokapShimmer(shape))
}
