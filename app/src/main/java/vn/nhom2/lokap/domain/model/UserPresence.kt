package vn.nhom2.lokap.domain.model

import java.time.Duration
import java.time.Instant
enum class PresenceStatus {
    ONLINE,
    RECENTLY_ACTIVE,
    OFFLINE
}

data class UserPresence(
    val userId: String,
    val status: PresenceStatus = PresenceStatus.OFFLINE,
    val lastSeenAt: Instant = Instant.now()
) {
    val isOnline: Boolean
        get() = status == PresenceStatus.ONLINE

    fun formatStatus(now: Instant = Instant.now()): String {
        return when (status) {
            PresenceStatus.ONLINE -> "Đang hoạt động"
            PresenceStatus.RECENTLY_ACTIVE -> {
                val minutes = Duration.between(lastSeenAt, now).toMinutes().coerceAtLeast(1)
                if (minutes < 60) {
                    "Hoạt động $minutes phút trước"
                } else {
                    val hours = minutes / 60
                    if (hours < 24) {
                        "Hoạt động $hours giờ trước"
                    } else {
                        "Hoạt động ${hours / 24} ngày trước"
                    }
                }
            }
            PresenceStatus.OFFLINE -> "Ngoại tuyến"
        }
    }
}
