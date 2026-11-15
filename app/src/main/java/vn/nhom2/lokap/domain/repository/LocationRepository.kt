package vn.nhom2.lokap.domain.repository

import vn.nhom2.lokap.domain.model.Place

/** Toạ độ gốc từ GPS. Chỉ dùng trên máy và ghi vào chỗ riêng của bài, không bao giờ hiện cho người khác. */
data class DeviceLocation(val lat: Double, val lng: Double, val accuracyM: Float?)

/** Tỉnh/phường chứa một toạ độ (đơn vị hành chính mới, không còn cấp quận/huyện). */
data class ResolvedRegion(
    val provinceCode: String,
    val provinceName: String,
    val wardCode: String?,
    val wardName: String?
)

interface LocationRepository {
    /** Vị trí hiện tại qua Fused Location Provider. Lỗi khi chưa cấp quyền hoặc tắt định vị. */
    suspend fun currentLocation(): Result<DeviceLocation>

    /** Toạ độ → tỉnh/phường (VietMap Reverse v4; không có key thì đoán tỉnh gần nhất, không có phường). */
    suspend fun resolveRegion(lat: Double, lng: Double): Result<ResolvedRegion>

    /** Tìm địa điểm quanh [near] (VietMap Autocomplete v4). Kết quả chưa có toạ độ: gọi [placeDetail] khi chọn. */
    suspend fun searchPlaces(query: String, near: DeviceLocation?): Result<List<Place>>

    /** Lấy toạ độ của địa điểm đã chọn. */
    suspend fun placeDetail(place: Place): Result<Place>
}
