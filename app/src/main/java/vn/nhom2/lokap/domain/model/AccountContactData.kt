package vn.nhom2.lokap.domain.model

import kotlinx.serialization.Serializable

/** Thông tin liên hệ lưu trên thiết bị; không phải phương thức xác thực Firebase. */
@Serializable
data class AccountContactData(
    val recoveryPhone: String? = null,
    val linkedEmails: List<String> = emptyList()
)
