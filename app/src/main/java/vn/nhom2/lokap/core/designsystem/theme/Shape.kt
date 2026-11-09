package vn.nhom2.lokap.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
@Immutable
data class LokapShapes(
    val cameraFrame: Shape = RoundedCornerShape(20.dp),
    val card: Shape = RoundedCornerShape(14.dp),
    val sheet: Shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    val dialog: Shape = RoundedCornerShape(16.dp),
    val button: Shape = RoundedCornerShape(12.dp),
    val pill: Shape = RoundedCornerShape(10.dp)
)

val LocalLokapShapes = staticCompositionLocalOf { LokapShapes() }

val MaterialLokapShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)
