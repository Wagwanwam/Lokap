package vn.nhom2.lokap.data.remote

import com.cloudinary.Cloudinary
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.nhom2.lokap.BuildConfig

/** Ảnh đã nằm trên Cloudinary. [url] là https, dùng thẳng cho Coil. */
data class UploadedImage(val url: String, val publicId: String)

/**
 * Upload ảnh lên Cloudinary bằng upload preset kiểu Unsigned: app chỉ biết cloud name + tên preset,
 * KHÔNG có API secret. Đánh đổi (gói Firebase miễn phí không có server để ký):
 * - ảnh là URL công khai, chỉ khó đoán nhờ postId ngẫu nhiên (UUID);
 * - app không xoá được ảnh trên Cloudinary (xoá bài chỉ xoá trên Firestore).
 * Upload lại cùng public_id không ghi đè (unsigned) mà trả về ảnh cũ → worker thử lại nhiều lần vẫn an toàn.
 */
@Singleton
class CloudinaryUploader @Inject constructor() {

    private val cloudinary: Cloudinary by lazy {
        Cloudinary(mapOf("cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME, "secure" to true))
    }

    val isConfigured: Boolean
        get() = BuildConfig.CLOUDINARY_CLOUD_NAME.isNotBlank() && BuildConfig.CLOUDINARY_UPLOAD_PRESET.isNotBlank()

    suspend fun uploadPostImage(file: File, authorId: String, postId: String): UploadedImage =
        upload(file, folder = "lokap/posts/$authorId", publicId = postId)

    suspend fun uploadAvatar(file: File, userId: String): UploadedImage =
        upload(file, folder = "lokap/avatars/$userId", publicId = "avatar_${System.currentTimeMillis()}")

    private suspend fun upload(file: File, folder: String, publicId: String): UploadedImage {
        if (!isConfigured) {
            throw CloudinaryException("Chưa cấu hình Cloudinary (CLOUDINARY_CLOUD_NAME, CLOUDINARY_UPLOAD_PRESET trong .env)", retryable = false)
        }
        return withContext(Dispatchers.IO) {
            val result = try {
                cloudinary.uploader().unsignedUpload(
                    file,
                    BuildConfig.CLOUDINARY_UPLOAD_PRESET,
                    mapOf("folder" to folder, "public_id" to publicId, "resource_type" to "image")
                )
            } catch (e: IOException) {
                throw CloudinaryException("Mất kết nối khi tải ảnh lên", retryable = true, cause = e)
            } catch (e: Exception) {
                // Lỗi do cấu hình (sai preset, preset không phải Unsigned, ảnh hỏng…) → không thử lại
                val serverBusy = e.message.orEmpty().let { it.contains("timeout", true) || it.contains(" 5") || it.contains("429") }
                throw CloudinaryException("Không tải được ảnh lên: ${e.message ?: "lỗi Cloudinary"}", retryable = serverBusy, cause = e)
            }
            val url = result["secure_url"] as? String
                ?: throw CloudinaryException("Cloudinary không trả về đường dẫn ảnh", retryable = true)
            UploadedImage(url = url, publicId = result["public_id"] as? String ?: "$folder/$publicId")
        }
    }
}
