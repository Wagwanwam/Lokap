package vn.nhom2.lokap.domain.repository

import vn.nhom2.lokap.domain.model.AccountContactData

/** Tách dữ liệu liên hệ cục bộ theo UID, không liên kết hay xác minh danh tính đăng nhập. */
interface AccountContactRepository {
    fun read(userId: String): AccountContactData
    fun update(userId: String, transform: (AccountContactData) -> AccountContactData): Result<AccountContactData>
    fun clear(userId: String): Result<Unit>
}
