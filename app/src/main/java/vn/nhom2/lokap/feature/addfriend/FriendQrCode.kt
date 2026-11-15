package vn.nhom2.lokap.feature.addfriend

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlin.math.hypot

/** Màu module QR: luôn tối trên nền trắng (đảo màu theo theme thì nhiều máy quét không đọc được). */
private val QrDark = Color(0xFF0B0B0D)

/** Tỉ lệ ô giữa QR dành cho avatar. Mức sửa lỗi H chịu được ~30% hỏng, ô này che ~5%. */
private const val CENTER_FRACTION = 0.24f

/**
 * Vẽ QR bằng Canvas từ ma trận ZXing (nét ở mọi kích thước), 3 ô định vị bo góc,
 * chừa lỗ tròn ở giữa cho [center] (avatar). Nền trắng và lề trắng do nơi gọi lo.
 */
@Composable
fun FriendQrCode(
    content: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    center: @Composable (size: Dp) -> Unit = {}
) {
    val matrix = remember(content) { encodeQr(content) }

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        val centerSize = maxWidth * (CENTER_FRACTION - 0.04f)
        Canvas(modifier = Modifier.fillMaxSize()) {
            matrix?.let { drawQr(it) }
        }
        if (matrix != null) {
            Box(contentAlignment = Alignment.Center) {
                center(centerSize)
            }
        }
    }
}

private fun encodeQr(content: String): BitMatrix? = runCatching {
    QRCodeWriter().encode(
        content,
        BarcodeFormat.QR_CODE,
        0,
        0,
        mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.CHARACTER_SET to "UTF-8",
            // Lề trắng (quiet zone) do padding của thẻ trắng bên ngoài
            EncodeHintType.MARGIN to 0
        )
    )
}.getOrNull()

private fun DrawScope.drawQr(matrix: BitMatrix) {
    val n = matrix.width
    val cell = size.minDimension / n
    val middle = n / 2f
    val holeRadius = n * CENTER_FRACTION / 2f

    for (y in 0 until n) {
        for (x in 0 until n) {
            if (!matrix[x, y] || isFinder(x, y, n)) continue
            if (hypot(x + 0.5f - middle, y + 0.5f - middle) < holeRadius) continue
            // +0.5 px để 2 ô cạnh nhau không hở đường kẻ mảnh do khử răng cưa
            drawRect(QrDark, topLeft = Offset(x * cell, y * cell), size = Size(cell + 0.5f, cell + 0.5f))
        }
    }

    listOf(0 to 0, n - 7 to 0, 0 to n - 7).forEach { (fx, fy) ->
        val origin = Offset(fx * cell, fy * cell)
        drawRoundRect(
            color = QrDark,
            topLeft = origin + Offset(cell / 2, cell / 2),
            size = Size(cell * 6, cell * 6),
            cornerRadius = CornerRadius(cell * 1.8f),
            style = Stroke(width = cell)
        )
        drawRoundRect(
            color = QrDark,
            topLeft = origin + Offset(cell * 2, cell * 2),
            size = Size(cell * 3, cell * 3),
            cornerRadius = CornerRadius(cell * 0.9f)
        )
    }
}

/** 3 ô định vị 7x7 ở góc trên trái, trên phải, dưới trái. */
private fun isFinder(x: Int, y: Int, n: Int): Boolean =
    (x < 7 && y < 7) || (x >= n - 7 && y < 7) || (x < 7 && y >= n - 7)
