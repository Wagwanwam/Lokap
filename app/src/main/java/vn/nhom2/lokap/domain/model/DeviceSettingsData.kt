package vn.nhom2.lokap.domain.model

/** Tuỳ chọn cục bộ dùng chung trên thiết bị, không chứa thông tin tài khoản. */
data class DeviceSettingsData(
    val dataSaverEnabled: Boolean = false,
    val autoSaveToGallery: Boolean = false,
    val highQualityUpload: Boolean = true
)
