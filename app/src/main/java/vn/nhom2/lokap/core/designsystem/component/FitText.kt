package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Chữ tự phóng to để lấp đầy khung chứa nó (mặc định ~62% bề ngang, ~60% chiều cao).
 * Dùng cho nhãn ngắn trong khung có kích thước cố định: tab, nút tròn, ô quyền…
 * Khung cha phải có kích thước xác định (height/size/weight), nếu không chữ chỉ lấy [maxFontSize].
 */
@Composable
fun FitText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight = FontWeight.Bold,
    widthFraction: Float = 0.62f,
    heightFraction: Float = 0.6f,
    maxFontSize: TextUnit = 40.sp,
    minFontSize: TextUnit = 10.sp,
    style: TextStyle = MaterialTheme.typography.titleMedium
) {
    val resolvedColor = if (color.isSpecified) color else LocalContentColor.current
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .fillMaxHeight(heightFraction),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = text,
                style = style.copy(
                    color = resolvedColor,
                    fontWeight = fontWeight,
                    textAlign = TextAlign.Center,
                    // Dòng cao theo cỡ chữ để autoSize đo đúng, bỏ khoảng trống trên/dưới
                    lineHeight = 1.15.em,
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.Both
                    )
                ),
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = minFontSize,
                    maxFontSize = maxFontSize,
                    stepSize = 0.5.sp
                )
            )
        }
    }
}
