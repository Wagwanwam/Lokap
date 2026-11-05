package vn.nhom2.lokap.domain.model

enum class LocationLevel(val labelVi: String) {
    PLACE("Địa điểm cụ thể"),
    WARD("Phường-xã"),
    PROVINCE("Tỉnh-thành"),
    COUNTRY("Chỉ \"Việt Nam\"")
}
