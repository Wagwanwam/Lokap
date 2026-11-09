package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
@Composable
fun OnlineIndicatorDot(
    modifier: Modifier = Modifier,
    size: Dp = 10.dp,
    isOnline: Boolean = true
) {
    if (!isOnline) return

    val borderColor = LokapTheme.colors.surface
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF10B981))
            .border(1.5.dp, borderColor, CircleShape)
    )
}
