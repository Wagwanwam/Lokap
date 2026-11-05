package vn.nhom2.lokap.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class LokapShapes(
    val cameraFrame: Shape = RoundedCornerShape(40.dp),
    val card: Shape = RoundedCornerShape(28.dp),
    val sheet: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    val pill: Shape = CircleShape
)

val LocalLokapShapes = staticCompositionLocalOf { LokapShapes() }

val MaterialLokapShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(28.dp),
    large = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    extraLarge = RoundedCornerShape(40.dp)
)
