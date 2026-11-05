package vn.nhom2.lokap.core.common

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val VIET_NAM_ZONE: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")
private val SHORT_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM").withZone(VIET_NAM_ZONE)

/**
 * Định dạng khoảng thời gian sang tiếng Việt:
 * - Dưới 1 phút: "vừa xong"
 * - Dưới 60 phút: "X phút trước"
 * - Dưới 24 giờ: "X giờ trước"
 * - Từ 1 đến 7 ngày: "X ngày trước"; chỉ bài public hẹn giờ mới truyền [isDelayedShot] = true để hiện "chụp X ngày trước"
 * - Quá 7 ngày: "dd/MM"
 */
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
    if (days <= 7) {
        return if (isDelayedShot) "chụp $days ngày trước" else "$days ngày trước"
    }
    return SHORT_DATE_FORMATTER.format(instant)
}
