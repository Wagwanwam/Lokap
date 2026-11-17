package vn.nhom2.lokap.core.image

import android.net.Uri as AndroidUri
import coil3.Uri as CoilUri
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import java.net.URI
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import vn.nhom2.lokap.domain.repository.DeviceSettingsRepository

/** Áp dụng lựa chọn hiện tại cho mỗi lần tải ảnh, trước bước tạo key/đọc cache của Coil. */
@Singleton
class DataSaverImageInterceptor @Inject constructor(
    private val deviceSettings: DeviceSettingsRepository
) : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val data = dataForRequest(request.data)
        if (data === request.data) return chain.proceed()
        val url = data as String
        val modified = request.newBuilder()
            .data(url)
            // Giữ key/extras do caller đặt, nhưng tách ảnh tiết kiệm khỏi ảnh đủ chất lượng.
            // Khi không đặt custom key, Coil tự dùng URL mới và vẫn giữ logic size/transform.
            .memoryCacheKeyExtra(DATA_SAVER_CACHE_KEY, url)
            .diskCacheKey(dataSaverDiskCacheKey(request.diskCacheKey, url))
            .build()
        return chain.withRequest(modified).proceed()
    }

    /** Tách phần chọn dữ liệu để test chuyển cờ bằng repository thật/fake mà không cần Android UI. */
    internal fun dataForRequest(data: Any): Any {
        if (!deviceSettings.settings.value.dataSaverEnabled) return data
        val url = when (data) {
            is String, is CoilUri, is AndroidUri, is URI, is URL -> data.toString()
            else -> return data
        }
        val transformed = CloudinaryImagePolicy.dataSaverUrl(url, enabled = true)
        return if (transformed == url) data else transformed
    }

    internal companion object {
        const val DATA_SAVER_CACHE_KEY = "lokap#data-saver"

        fun dataSaverDiskCacheKey(customKey: String?, transformedUrl: String): String? =
            customKey?.let { "$it|$DATA_SAVER_CACHE_KEY=$transformedUrl" }
    }
}
