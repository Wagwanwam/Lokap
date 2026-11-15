package vn.nhom2.lokap.domain.model

/** Cài đặt của người dùng: mức vị trí mặc định (profiles) + 4 công tắc thông báo (user_settings). */
data class AppSettingsData(
    val defaultLocationLevel: LocationLevel = LocationLevel.PROVINCE,
    val notifyNewPhotos: Boolean = true,
    val notifyReactions: Boolean = true,
    val notifyMessages: Boolean = true,
    val notifyFriendRequests: Boolean = true
)
