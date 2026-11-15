package vn.nhom2.lokap.data.fake

import vn.nhom2.lokap.domain.model.Place
import vn.nhom2.lokap.domain.repository.DeviceLocation
import vn.nhom2.lokap.domain.repository.LocationRepository
import vn.nhom2.lokap.domain.repository.ResolvedRegion

/** Vị trí cố định ở Hải Châu, Đà Nẵng (khớp dữ liệu mẫu). Dùng cho unit test và Preview. */
class FakeLocationRepository(private val available: Boolean = true) : LocationRepository {

    override suspend fun currentLocation(): Result<DeviceLocation> =
        if (available) Result.success(DeviceLocation(16.0678, 108.2208, 20f)) else Result.failure(Exception("Chưa cấp quyền vị trí"))

    override suspend fun resolveRegion(lat: Double, lng: Double): Result<ResolvedRegion> = Result.success(
        ResolvedRegion(
            provinceCode = SampleData2.DEFAULT_PROVINCE_CODE,
            provinceName = SampleData2.DEFAULT_PROVINCE_NAME,
            wardCode = SampleData2.DEFAULT_WARD_CODE,
            wardName = SampleData2.DEFAULT_WARD_NAME
        )
    )

    override suspend fun searchPlaces(query: String, near: DeviceLocation?): Result<List<Place>> = Result.success(
        SampleData2.daNangPlaces.filter {
            query.isBlank() || it.name.contains(query, ignoreCase = true) || it.address.contains(query, ignoreCase = true)
        }
    )

    override suspend fun placeDetail(place: Place): Result<Place> = Result.success(place)
}
