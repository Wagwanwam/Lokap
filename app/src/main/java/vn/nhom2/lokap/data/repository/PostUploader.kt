package vn.nhom2.lokap.data.repository

enum class UploadOutcome { DONE, RETRY, FAILED }

/** Việc upload một bài chờ gửi; UploadPostWorker gọi, FirestorePostRepository thực hiện. */
interface PostUploader {
    suspend fun upload(postId: String): UploadOutcome

    suspend fun markFailed(postId: String)
}
