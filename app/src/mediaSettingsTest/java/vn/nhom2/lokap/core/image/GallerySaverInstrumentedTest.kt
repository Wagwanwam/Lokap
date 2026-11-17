package vn.nhom2.lokap.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Local synthetic photos only. Never uploads, signs in, or touches another gallery item. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29)
class GallerySaverInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun publishesJpegWithOriginalBytesAndLeavesDraftIntact() = runBlocking {
        val source = File(context.cacheDir, "gallery-test-${UUID.randomUUID()}.jpg")
        val bitmap = Bitmap.createBitmap(19, 13, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.MAGENTA) }
        source.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it)) }
        bitmap.recycle()
        val originalBytes = source.readBytes()
        var publishedUri: Uri? = null
        try {
            val gallery = MediaStorePhotoGallery(context)
            assertFalse(gallery.requiresWritePermission())
            val uri = Uri.parse(gallery.savePhoto(source.absolutePath).getOrThrow())
            publishedUri = uri
            val projection = arrayOf(
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.RELATIVE_PATH,
                MediaStore.Images.Media.IS_PENDING,
                MediaStore.Images.Media.SIZE
            )
            context.contentResolver.query(uri, projection, null, null, null)!!.use { row ->
                assertTrue(row.moveToFirst())
                assertTrue(row.getString(0).startsWith("Lokap_"))
                assertEquals("image/jpeg", row.getString(1))
                assertEquals("Pictures/Lokap/", row.getString(2))
                assertEquals(0, row.getInt(3))
                assertEquals(originalBytes.size.toLong(), row.getLong(4))
            }
            val copiedBytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
            assertArrayEquals(originalBytes, copiedBytes)
            val decoded = BitmapFactory.decodeByteArray(copiedBytes, 0, copiedBytes.size)
            assertEquals(19, decoded.width)
            assertEquals(13, decoded.height)
            decoded.recycle()
            assertTrue(source.exists())
            assertArrayEquals(originalBytes, source.readBytes())
        } finally {
            publishedUri?.let { context.contentResolver.delete(it, null, null) }
            source.delete()
        }
    }

    @Test
    fun missingCaptureFailsWithoutCreatingGalleryRow() = runBlocking {
        val before = ownGalleryCount()
        val missing = File(context.cacheDir, "missing-${UUID.randomUUID()}.jpg")
        val result = MediaStorePhotoGallery(context).savePhoto(missing.absolutePath)
        assertTrue(result.isFailure)
        assertEquals(before, ownGalleryCount())
    }

    private fun ownGalleryCount(): Int = context.contentResolver.query(
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
        arrayOf(MediaStore.Images.Media._ID),
        "${MediaStore.Images.Media.RELATIVE_PATH} = ? AND ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?",
        arrayOf("Pictures/Lokap/", "Lokap_%"),
        null
    )!!.use { it.count }
}
