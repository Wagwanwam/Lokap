package vn.nhom2.lokap.data.repository

import android.content.Context
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.nhom2.lokap.domain.repository.AppCacheRepository

/**
 * Chỉ dọn cache của ImageLoader dùng chung với AsyncImage.
 * Không gọi LocalCache.clear(): hàm đó dành cho đăng xuất và xoá cả bài/ảnh chờ gửi.
 * Không xoá cacheDir vì Camera và Composer còn giữ ảnh nháp ở đó.
 */
@Singleton
class CoilAppCacheRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : AppCacheRepository {
    override suspend fun clearCache(): Result<Unit> = try {
        val imageLoader = withContext(Dispatchers.Main.immediate) {
            SingletonImageLoader.get(context)
        }
        clearImageCaches(imageLoader.memoryCache, imageLoader.diskCache)
        Result.success(Unit)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }
}

/** Giữ cache mở để các yêu cầu ảnh đang chạy vẫn hoạt động; ảnh có thể được cache lại khi tải. */
internal suspend fun clearImageCaches(
    memoryCache: MemoryCache?,
    diskCache: DiskCache?,
    memoryDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    diskDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    withContext(memoryDispatcher) { memoryCache?.clear() }
    withContext(diskDispatcher) { diskCache?.clear() }
}
