package vn.nhom2.lokap.data.repository

import vn.nhom2.lokap.domain.repository.AppException

/** Không xếp hàng thao tác từ màn khác: một lệnh huỷ cũ không được chạy sau lệnh gửi mới. */
internal class SocialMutationGate {
    private val activePairs = mutableSetOf<String>()

    suspend fun <T> run(pairId: String, block: suspend () -> T): T {
        if (!synchronized(activePairs) { activePairs.add(pairId) }) {
            throw AppException("Đang cập nhật quan hệ bạn bè. Vui lòng chờ một chút")
        }
        return try {
            block()
        } finally {
            synchronized(activePairs) { activePairs.remove(pairId) }
        }
    }
}
