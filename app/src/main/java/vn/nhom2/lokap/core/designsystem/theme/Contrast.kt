package vn.nhom2.lokap.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Tỉ lệ tương phản WCAG 2 giữa hai màu đục: từ 1 (trùng màu) đến 21 (đen trên trắng). */
fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
}

/**
 * Giữ màu nhấn nếu đủ tương phản với nền, nếu không thì dùng [fallback].
 * Dành cho màu người dùng chọn (chủ đề chat): không biết trước màu sáng hay tối, nên chủ đề tối
 * trên giao diện tối, chủ đề sáng trên giao diện sáng có thể gần như vô hình (dưới 1,5:1).
 * Ngưỡng mặc định 3:1 là mức tối thiểu cho icon và nhãn đi kèm icon.
 */
fun Color.readableOn(background: Color, fallback: Color, minRatio: Float = 3f): Color =
    if (contrastRatio(this, background) >= minRatio) this else fallback
