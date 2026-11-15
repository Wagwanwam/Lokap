package vn.nhom2.lokap.core.image

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.nhom2.lokap.domain.repository.AppException
import vn.nhom2.lokap.domain.repository.ImageImporter

/**
 * Ảnh chọn từ Photo Picker là content:// chỉ đọc được tạm thời: chép ra cacheDir rồi đưa vào Composer.
 * Khi gửi, ImageProcessor vẫn xoay theo EXIF, cắt vuông và bỏ EXIF (không lộ toạ độ GPS trong ảnh).
 */
@Singleton
class ContentImageImporter @Inject constructor(
    @ApplicationContext private val context: Context
) : ImageImporter {

    override suspend fun importImage(uri: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.cacheDir, "gallery").apply { mkdirs() }
            // Chỉ giữ ảnh vừa chọn, ảnh chọn lần trước đã được Composer dùng xong
            dir.listFiles()?.forEach { it.delete() }
            val target = File(dir, "${UUID.randomUUID()}.jpg")
            val input = context.contentResolver.openInputStream(Uri.parse(uri))
                ?: throw AppException("Không mở được ảnh này")
            input.use { source -> target.outputStream().use { source.copyTo(it) } }
            if (target.length() == 0L) throw AppException("Ảnh trống, chọn ảnh khác")
            Result.success(target.absolutePath)
        } catch (e: CancellationException) {
            throw e
        } catch (e: AppException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(AppException("Không đọc được ảnh này", cause = e))
        }
    }
}
