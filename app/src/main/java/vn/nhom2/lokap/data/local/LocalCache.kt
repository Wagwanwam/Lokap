package vn.nhom2.lokap.data.local

import android.content.Context
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.nhom2.lokap.data.worker.UploadPostWorker

/** Dữ liệu của người đang đăng nhập nằm trên máy. Đăng xuất / xoá tài khoản thì xoá sạch. */
@Singleton
class LocalCache @Inject constructor(
    private val dao: PostDao,
    private val workManager: Provider<WorkManager>,
    @ApplicationContext private val context: Context,
    private val refreshThrottles: RefreshThrottles
) {
    /** Ảnh đã xoá EXIF chờ upload (filesDir để hệ thống không tự dọn như cacheDir). */
    val pendingImagesDir: File
        get() = File(context.filesDir, "pending_posts")

    suspend fun clear() = withContext(Dispatchers.IO) {
        runCatching { workManager.get().cancelAllWorkByTag(UploadPostWorker.TAG) }
        dao.clearAll()
        // Máy đã trống: lần mở màn sau phải tải lại dù vừa tải chưa tới 30 giây
        refreshThrottles.resetAll()
        pendingImagesDir.deleteRecursively()
        Unit
    }
}
