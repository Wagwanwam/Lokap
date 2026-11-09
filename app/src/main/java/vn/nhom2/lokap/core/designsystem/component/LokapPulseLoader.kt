package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
@Composable
fun LokapPulseLoader(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    message: String? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LokapPulseTransition")

    val rippleScale1 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Ripple1Scale"
    )

    val rippleAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Ripple1Alpha"
    )

    val rippleScale2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Ripple2Scale"
    )

    val rippleAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Ripple2Alpha"
    )

    val primaryColor = LokapTheme.colors.primary
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            primaryColor,
            Color(0xFFFF6644)
        )
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(size * 2),
            contentAlignment = Alignment.Center
        ) {

            Box(
                modifier = Modifier
                    .size(size)
                    .scale(rippleScale1)
                    .border(1.5.dp, primaryColor.copy(alpha = rippleAlpha1), CircleShape)
            )

            Box(
                modifier = Modifier
                    .size(size)
                    .scale(rippleScale2)
                    .border(1.5.dp, Color(0xFFFF6644).copy(alpha = rippleAlpha2), CircleShape)
            )

            Box(
                modifier = Modifier
                    .size(size)
                    .background(gradientBrush, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                LokapLogo(size = size * 0.55f, showText = false)
            }
        }

        if (message != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = LokapTheme.colors.textSecondary
            )
        }
    }
}
