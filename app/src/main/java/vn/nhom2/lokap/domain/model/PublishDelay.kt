package vn.nhom2.lokap.domain.model

enum class PublishDelay(val days: Int) {
    NOW(0),
    ONE_DAY(1),
    THREE_DAYS(3),
    SEVEN_DAYS(7)
}
