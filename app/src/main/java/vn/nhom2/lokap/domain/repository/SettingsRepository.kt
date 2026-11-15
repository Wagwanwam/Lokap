package vn.nhom2.lokap.domain.repository

import kotlinx.coroutines.flow.StateFlow
import vn.nhom2.lokap.domain.model.AppSettingsData

interface SettingsRepository {
    val settings: StateFlow<AppSettingsData>

    suspend fun refresh(): Result<Unit>

    /** Cập nhật ngay trên máy rồi lưu lên server; lỗi thì trả lại giá trị cũ. */
    suspend fun update(transform: (AppSettingsData) -> AppSettingsData): Result<Unit>
}
