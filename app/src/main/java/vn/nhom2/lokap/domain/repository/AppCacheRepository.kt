package vn.nhom2.lokap.domain.repository

/** Bộ nhớ đệm ảnh có thể tải lại, tách khỏi dữ liệu phiên và ảnh/bài chưa gửi. */
interface AppCacheRepository {
    suspend fun clearCache(): Result<Unit>
}
