package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme

/** Đỏ của chấm "Vị trí của tôi": khác hồng chủ đạo của bong bóng để không lẫn với ảnh trên bản đồ. */
val MyLocationRed = Color(0xFFEF4444)

/**
 * Chấm đỏ "Vị trí của tôi" trên bản đồ: lõi đỏ viền trắng + vòng sóng mờ lan ra.
 * Không bắt chạm (không clickable) nên bong bóng nằm dưới vẫn bấm được.
 */
@Composable
fun MyLocationDot(modifier: Modifier = Modifier) {
    val pulse = if (LocalInspectionMode.current) {
        0.5f
    } else {
        val transition = rememberInfiniteTransition(label = "my_location_pulse")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
            label = "my_location_pulse_value"
        )
        value
    }
    Canvas(
        modifier = modifier
            .size(48.dp)
            .semantics { contentDescription = "Vị trí của tôi" }
    ) {
        val core = 9.dp.toPx()
        // Vòng sóng: to dần và mờ dần
        drawCircle(
            color = MyLocationRed.copy(alpha = 0.35f * (1f - pulse)),
            radius = core + (size.minDimension / 2f - core) * pulse
        )
        // Quầng cố định giúp thấy chấm trên nền bản đồ tối
        drawCircle(color = MyLocationRed.copy(alpha = 0.18f), radius = core * 1.9f)
        drawCircle(color = Color.White, radius = core + 3.dp.toPx())
        drawCircle(color = MyLocationRed, radius = core)
    }
}

@Preview(name = "Chấm vị trí của tôi", showBackground = true, backgroundColor = 0xFF121214)
@Composable
private fun MyLocationDotPreview() {
    LokapTheme { MyLocationDot() }
}
