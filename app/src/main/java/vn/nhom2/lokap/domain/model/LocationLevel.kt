package vn.nhom2.lokap.domain.model

enum class LocationLevel(val labelVi: String, val descVi: String) {
    PLACE("Địa điểm cụ thể", "Hiển thị tên địa danh"),
    WARD("Phường, Xã", "Hiển thị cấp phường xã"),
    PROVINCE("Tỉnh, Thành phố", "Gom theo tỉnh thành"),
    COUNTRY("Toàn quốc", "Chỉ hiển thị Việt Nam")
}
