package vn.nhom2.lokap.core.image

import coil3.toUri
import java.io.File
import java.net.URI
import java.net.URL
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import vn.nhom2.lokap.domain.model.DeviceSettingsData
import vn.nhom2.lokap.domain.repository.DeviceSettingsRepository

class DataSaverImageInterceptorTest {
    private val original = "https://res.cloudinary.com/demo/image/upload/v123/lokap/posts/user/photo.jpg"

    @Test fun `same interceptor reads latest setting for every request`() {
        val repository = Preferences()
        val interceptor = DataSaverImageInterceptor(repository)
        assertSame(original, interceptor.dataForRequest(original))
        repository.setDataSaver(true)
        val reduced = interceptor.dataForRequest(original)
        assertEquals(CloudinaryImagePolicy.dataSaverUrl(original, true), reduced)
        assertNotEquals(original, reduced)
        repository.setDataSaver(false)
        assertSame(original, interceptor.dataForRequest(original))
    }

    @Test fun `uri request data receives same policy as string URLs`() {
        val interceptor = DataSaverImageInterceptor(Preferences().apply { setDataSaver(true) })
        val expected = CloudinaryImagePolicy.dataSaverUrl(original, true)
        listOf(original.toUri(), URI(original), URL(original)).forEach {
            assertEquals(expected, interceptor.dataForRequest(it))
        }
    }

    @Test fun `disabled or ineligible requests preserve original object and cache identity`() {
        val repository = Preferences()
        val interceptor = DataSaverImageInterceptor(repository)
        val remoteUri = original.toUri()
        assertSame(remoteUri, interceptor.dataForRequest(remoteUri))
        repository.setDataSaver(true)
        val file = File("/tmp/lokap-photo.jpg")
        val contentUri = "content://media/images/1".toUri()
        val externalUri = URI("https://example.com/photo.jpg")
        val unknownModel = object { override fun toString() = original }
        listOf(file, contentUri, externalUri, unknownModel).forEach {
            assertSame(it, interceptor.dataForRequest(it))
        }
    }

    @Test fun `custom disk keys separate reduced image from full quality and other URLs`() {
        val reduced = CloudinaryImagePolicy.dataSaverUrl(original, true)
        assertNull(DataSaverImageInterceptor.dataSaverDiskCacheKey(null, reduced))
        val lowKey = DataSaverImageInterceptor.dataSaverDiskCacheKey("custom", reduced)
        assertNotEquals("custom", lowKey)
        assertEquals(lowKey, DataSaverImageInterceptor.dataSaverDiskCacheKey("custom", reduced))
        assertNotEquals(lowKey, DataSaverImageInterceptor.dataSaverDiskCacheKey("other", reduced))
        assertNotEquals(lowKey, DataSaverImageInterceptor.dataSaverDiskCacheKey("custom", reduced.replace("photo.jpg", "other.jpg")))
    }

    private class Preferences : DeviceSettingsRepository {
        override val settings = MutableStateFlow(DeviceSettingsData())
        override fun setDataSaver(enabled: Boolean) { settings.value = settings.value.copy(dataSaverEnabled = enabled) }
        override fun setAutoSave(enabled: Boolean) { settings.value = settings.value.copy(autoSaveToGallery = enabled) }
        override fun setHighQuality(enabled: Boolean) { settings.value = settings.value.copy(highQualityUpload = enabled) }
    }
}
