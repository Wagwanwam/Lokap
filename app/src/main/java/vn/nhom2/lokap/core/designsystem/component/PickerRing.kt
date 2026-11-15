package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme

/**
 * Một lựa chọn trên vòng chọn (bộ lọc màu ở Camera, nút lọc ở Bản đồ), theo màu hồng chủ đạo của app.
 * - Thường: nền surface, viền + icon hồng nhạt.
 * - [selected]: viền hồng dày.
 * - [hovered] (ngón tay đang trỏ): nền hồng, icon màu onPrimary; lựa chọn avatar chỉ đổi viền.
 * Truyền [icon] cho lựa chọn dạng icon, hoặc [content] (ví dụ avatar).
 */
@Composable
fun PickerRingItem(
    selected: Boolean,
    hovered: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    icon: ImageVector? = null,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val colors = LokapTheme.colors
    val filled = hovered && icon != null
    val emphasized = selected || hovered
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (filled) colors.primary else colors.surface.copy(alpha = 0.92f))
            .border(
                width = if (emphasized) 3.dp else 1.5.dp,
                color = if (emphasized) colors.primary else colors.primary.copy(alpha = 0.45f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (filled) colors.onPrimary else colors.primary,
                modifier = Modifier.size(size * 0.48f)
            )
        }
        content()
    }
}

/** Nhãn tên lựa chọn (trên vòng chọn, hoặc cạnh nút sau khi đổi): nền hồng, chữ onPrimary. */
@Composable
fun PickerRingLabel(text: String, modifier: Modifier = Modifier) {
    val colors = LokapTheme.colors
    Text(
        text = text,
        color = colors.onPrimary,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.titleSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(CircleShape)
            .background(colors.primary)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
