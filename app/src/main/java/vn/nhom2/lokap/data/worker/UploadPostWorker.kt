package vn.nhom2.lokap.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import vn.nhom2.lokap.data.repository.PostUploader
import vn.nhom2.lokap.data.repository.UploadOutcome

/**
 * Gửi bài chạy nền (tài liệu "Luồng đăng bài" bước 5–7): upload ảnh lên Cloudinary (public_id = postId)
 * rồi ghi `posts/{postId}` lên Firestore với cùng postId, nên thử lại nhiều lần cũng không tạo bài trùng.
 * Chỉ chạy khi có mạng; lỗi mạng tự thử lại, quá 5 lần → FAILED, Feed hiện nút Thử lại.
 */
@HiltWorker
class UploadPostWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val uploader: PostUploader
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val postId = inputData.getString(KEY_POST_ID) ?: return Result.failure()
        return when (uploader.upload(postId)) {
            UploadOutcome.DONE -> Result.success()
            UploadOutcome.FAILED -> {
                uploader.markFailed(postId)
                Result.failure()
            }
            UploadOutcome.RETRY -> if (runAttemptCount + 1 >= MAX_ATTEMPTS) {
                uploader.markFailed(postId)
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }

    companion object {
        const val TAG = "upload_post"
        const val KEY_POST_ID = "post_id"
        const val MAX_ATTEMPTS = 5

        fun uniqueName(postId: String) = "upload_post_$postId"
    }
}
