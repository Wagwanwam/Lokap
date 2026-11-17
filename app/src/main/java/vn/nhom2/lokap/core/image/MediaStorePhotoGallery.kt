package vn.nhom2.lokap.core.image

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import vn.nhom2.lokap.domain.repository.PhotoGalleryRepository

/** Adds only photos explicitly passed by the camera; it does not scan old drafts or imports. */
@Singleton
class MediaStorePhotoGallery @Inject constructor(
    @ApplicationContext private val context: Context
) : PhotoGalleryRepository {
    override fun requiresWritePermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED

    override suspend fun savePhoto(imagePath: String): Result<String> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var insertedUri: Uri? = null
        var legacyFile: File? = null
        try {
            val source = File(imagePath)
            require(source.isFile && source.length() > 0L) { "Không tìm thấy ảnh vừa chụp" }
            check(!requiresWritePermission()) { "Chưa có quyền lưu ảnh vào thư viện" }
            currentCoroutineContext().ensureActive()
            val name = "Lokap_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            }
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Lokap")
                values.put(MediaStore.Images.Media.IS_PENDING, 1)
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                @Suppress("DEPRECATION")
                val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Lokap")
                check(directory.isDirectory || directory.mkdirs()) { "Không tạo được thư mục ảnh Lokap" }
                val targetFile = File(directory, name)
                legacyFile = targetFile
                @Suppress("DEPRECATION")
                values.put(MediaStore.Images.Media.DATA, targetFile.absolutePath)
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val uri = resolver.insert(collection, values) ?: throw IOException("Không tạo được ảnh trong thư viện")
            insertedUri = uri
            val output = resolver.openOutputStream(uri, "w") ?: throw IOException("Không ghi được ảnh vào thư viện")
            output.use { target -> source.inputStream().use { input -> input.copyTo(target) } }
            currentCoroutineContext().ensureActive()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val published = resolver.update(uri, ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }, null, null)
                check(published == 1) { "Không hoàn tất lưu ảnh vào thư viện" }
            }
            Result.success(uri.toString())
        } catch (error: Exception) {
            // A partial gallery copy must not remain visible. The camera/composer file is untouched.
            insertedUri?.let { uri -> runCatching { resolver.delete(uri, null, null) } }
            legacyFile?.let { file -> runCatching { file.delete() } }
            if (error is CancellationException) throw error
            Result.failure(error)
        }
    }
}
