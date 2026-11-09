package vn.nhom2.lokap.core.common

import java.time.Duration
import java.time.Instant
fun formatTimeAgo(
    instant: Instant,
    now: Instant,
    isDelayedShot: Boolean = false
): String {
    val duration = Duration.between(instant, now)
    if (duration.isNegative || duration.seconds < 60) {
        return "vừa xong"
    }
    val minutes = duration.toMinutes()
    if (minutes < 60) {
        return "$minutes phút trước"
    }
    val hours = duration.toHours()
    if (hours < 24) {
        return "$hours giờ trước"
    }
    val days = duration.toDays()
    if (days < 30) {
        return if (isDelayedShot) "chụp $days ngày trước" else "$days ngày trước"
    }
    val months = (days / 30).coerceAtLeast(1)
    if (months < 12) {
        return "$months tháng trước"
    }
    val years = (days / 365).coerceAtLeast(1)
    return "$years năm trước"
}
