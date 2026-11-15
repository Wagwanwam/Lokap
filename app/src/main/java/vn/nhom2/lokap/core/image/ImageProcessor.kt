package vn.nhom2.lokap.core.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Chuẩn bị ảnh trước khi upload (tài liệu: "Rà soát lỗi tiềm ẩn" — file ảnh còn EXIF GPS):
 * xoay theo EXIF, cắt vuông giữa ảnh, thu về tối đa 1080×1080, nén JPEG 80 rồi ghi FILE MỚI.
 * Bitmap.compress không ghi EXIF nên toạ độ GPS, đời máy… trong ảnh gốc không bao giờ lên server.
 */
@Singleton
class ImageProcessor @Inject constructor() {

    fun prepareForUpload(input: File, output: File, maxSize: Int = 1080, quality: Int = 80): File {
        require(input.exists()) { "Không tìm thấy ảnh vừa chụp" }

        val orientation = readOrientation(input)

        // Đọc kích thước trước, giải mã thu nhỏ ngay từ đầu (ảnh 12MP giải mã nguyên cỡ ~48 MB)
        val bounds = readBounds(input)
        var sampleSize = 1
        while (min(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= maxSize) sampleSize *= 2
        val decoded = BitmapFactory.decodeFile(
            input.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sampleSize }
        ) ?: error("Không đọc được ảnh")

        val side = min(decoded.width, decoded.height)
        val scale = min(1f, maxSize.toFloat() / side)
        val matrix = orientationMatrix(orientation).apply { postScale(scale, scale) }
        val square = Bitmap.createBitmap(
            decoded,
            (decoded.width - side) / 2,
            (decoded.height - side) / 2,
            side,
            side,
            matrix,
            true
        )

        output.parentFile?.mkdirs()
        output.outputStream().use { stream -> square.compress(Bitmap.CompressFormat.JPEG, quality, stream) }
        if (square !== decoded) square.recycle()
        decoded.recycle()
        return output
    }

    /**
     * Áp bộ lọc màu vào ảnh vừa chụp (chọn ở màn camera) và ghi FILE MỚI đã xoay đúng chiều.
     * Ảnh giữ cạnh dài ≤ [maxSide]; bước prepareForUpload phía sau vẫn cắt vuông và nén như cũ.
     */
    fun applyColorFilter(
        input: File,
        output: File,
        filter: PhotoColorFilter,
        maxSide: Int = 2160,
        quality: Int = 92
    ): File {
        val values = filter.matrix ?: return input
        require(input.exists()) { "Không tìm thấy ảnh vừa chụp" }

        val orientation = readOrientation(input)
        val bounds = readBounds(input)
        var sampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= maxSide) sampleSize *= 2
        val decoded = BitmapFactory.decodeFile(
            input.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sampleSize }
        ) ?: error("Không đọc được ảnh")

        val oriented = Bitmap.createBitmap(
            decoded, 0, 0, decoded.width, decoded.height, orientationMatrix(orientation), true
        )
        if (oriented !== decoded) decoded.recycle()

        val filtered = Bitmap.createBitmap(oriented.width, oriented.height, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix(values))
        }
        Canvas(filtered).drawBitmap(oriented, 0f, 0f, paint)
        oriented.recycle()

        output.parentFile?.mkdirs()
        output.outputStream().use { stream -> filtered.compress(Bitmap.CompressFormat.JPEG, quality, stream) }
        filtered.recycle()
        return output
    }

    private fun readOrientation(input: File): Int = runCatching {
        ExifInterface(input.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    private fun readBounds(input: File): BitmapFactory.Options {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(input.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Không đọc được ảnh" }
        return bounds
    }

    private fun orientationMatrix(orientation: Int) = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                postRotate(90f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                postRotate(270f)
                postScale(-1f, 1f)
            }
        }
    }
}
