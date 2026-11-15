package vn.nhom2.lokap.data.local

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Cache có hạn (TTL) cho lần tải lại từ server khi mở màn: cùng [key] (tài khoản) vừa tải thành công chưa quá
 * [ttlMillis] thì bỏ qua, vì dữ liệu đã nằm trong Room. Kéo để làm mới / Thử lại gọi với `force = true`.
 * Các lần gọi trùng lúc chạy lần lượt: màn mở sau chờ màn trước tải xong, thấy còn hạn thì bỏ qua.
 */
class RefreshThrottle(
    private val ttlMillis: Long,
    private val clock: () -> Long = SystemClock::elapsedRealtime
) {
    private val mutex = Mutex()
    @Volatile private var freshKey: String? = null
    @Volatile private var freshUntil = 0L
    // Tăng mỗi lần reset: lần tải bắt đầu trước khi xoá dữ liệu thì xong cũng không được tính là còn hạn
    @Volatile private var generation = 0

    suspend fun run(key: String, force: Boolean = false, load: suspend () -> Result<Unit>): Result<Unit> =
        mutex.withLock {
            if (!force && key == freshKey && clock() < freshUntil) return@withLock Result.success(Unit)
            val startedAt = generation
            load().onSuccess {
                if (startedAt == generation) {
                    freshKey = key
                    freshUntil = clock() + ttlMillis
                }
            }
        }

    /** Dữ liệu trên máy vừa bị xoá (đăng xuất): lần mở màn sau phải tải lại. */
    fun reset() {
        generation++
        freshKey = null
        freshUntil = 0L
    }
}

/** Các cache có hạn dùng chung trong app; [LocalCache.clear] đặt lại tất cả. */
@Singleton
class RefreshThrottles @Inject constructor() {
    /** Feed bạn bè + Khám phá (FirestorePostRepository.refreshFeeds). */
    val feeds = RefreshThrottle(REFRESH_TTL_MS)

    /** Danh mục địa danh (tới 500 document mỗi lần). */
    val landmarks = RefreshThrottle(REFRESH_TTL_MS)

    fun resetAll() {
        feeds.reset()
        landmarks.reset()
    }

    private companion object {
        const val REFRESH_TTL_MS = 30_000L
    }
}
