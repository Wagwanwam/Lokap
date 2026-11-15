package vn.nhom2.lokap.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import vn.nhom2.lokap.data.remote.toAppException
import vn.nhom2.lokap.domain.model.Place
import vn.nhom2.lokap.domain.model.Province
import vn.nhom2.lokap.domain.repository.AppException
import vn.nhom2.lokap.domain.repository.DeviceLocation
import vn.nhom2.lokap.domain.repository.LocationRepository
import vn.nhom2.lokap.domain.repository.ResolvedRegion

/**
 * Vị trí hiện tại bằng Fused Location Provider (Google Play services), tên tỉnh/phường và tìm địa điểm bằng
 * VietMap API. Chưa có key VietMap: đoán tỉnh theo tâm gần nhất (không có phường), tìm địa điểm báo chưa khả dụng.
 */
@Singleton
class FusedLocationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vietMap: VietMapApi
) : LocationRepository {

    private val fused by lazy { LocationServices.getFusedLocationProviderClient(context) }

    @SuppressLint("MissingPermission") // đã kiểm tra quyền ngay bên dưới
    override suspend fun currentLocation(): Result<DeviceLocation> = call {
        if (!hasPermission()) throw AppException("Chưa cấp quyền vị trí")
        val cts = CancellationTokenSource()
        // Cân bằng pin: độ chính xác ~100 m là đủ để biết phường; GPS tắt thì dùng vị trí gần nhất đã biết
        val fresh = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token).await(cts)
        }
        val location = fresh ?: fused.lastLocation.await()
            ?: throw AppException("Không lấy được vị trí. Bật định vị rồi thử lại.")
        DeviceLocation(
            lat = location.latitude,
            lng = location.longitude,
            accuracyM = if (location.hasAccuracy()) location.accuracy else null
        )
    }

    override suspend fun resolveRegion(lat: Double, lng: Double): Result<ResolvedRegion> = call {
        val fromVietMap = if (vietMap.isConfigured) runCatching { vietMap.reverse(lat, lng) }.getOrNull() else null
        val cityName = fromVietMap?.boundaries?.firstOrNull { it.type == 0 }
        val ward = fromVietMap?.boundaries?.firstOrNull { it.type == 2 }
        val province = cityName?.let { findProvince(it.name.ifBlank { it.fullName }) } ?: nearestProvince(lat, lng)
        ResolvedRegion(
            provinceCode = province.code,
            provinceName = province.name,
            wardCode = ward?.let { "${province.code}.${slug(it.fullName.ifBlank { it.name })}" },
            wardName = ward?.fullName?.ifBlank { ward.name }
        )
    }

    override suspend fun searchPlaces(query: String, near: DeviceLocation?): Result<List<Place>> = call {
        val q = query.trim()
        if (q.length < 2) return@call emptyList()
        if (!vietMap.isConfigured) throw AppException("Tìm địa điểm hiện chưa khả dụng")
        vietMap.autocomplete(q, near?.lat, near?.lng).map { result ->
            Place(
                id = result.refId,
                name = result.name.ifBlank { result.display },
                address = result.address,
                lat = Double.NaN,
                lng = Double.NaN
            )
        }
    }

    override suspend fun placeDetail(place: Place): Result<Place> = call {
        if (!place.lat.isNaN() && !place.lng.isNaN()) return@call place
        val detail = vietMap.place(place.id)
        if (detail.lat.isNaN() || detail.lng.isNaN()) throw AppException("Không lấy được vị trí của địa điểm này")
        place.copy(lat = detail.lat, lng = detail.lng)
    }

    private fun hasPermission(): Boolean =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    /** "Thành phố Đà Nẵng" / "Đà Nẵng" → tỉnh trong danh mục 34 tỉnh. */
    private fun findProvince(name: String): Province? {
        val key = slug(name).removePrefix("thanh-pho-").removePrefix("tinh-").removePrefix("tp-")
        return ProvinceCatalog.allProvinces.firstOrNull { slug(it.name).removePrefix("thanh-pho-") == key }
    }

    private fun nearestProvince(lat: Double, lng: Double): Province =
        ProvinceCatalog.allProvinces.minBy { p ->
            val dLat = p.lat - lat
            val dLng = (p.lng - lng) * cos(Math.toRadians(lat))
            dLat * dLat + dLng * dLng
        }

    private inline fun <T> call(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(
            if (e is VietMapException) AppException("Dịch vụ bản đồ đang lỗi (mã ${e.httpCode}), thử lại sau") else e.toAppException()
        )
    }

    companion object {
        private const val LOCATION_TIMEOUT_MS = 10_000L

        /** "Phường Hải Châu" → "phuong-hai-chau". */
        fun slug(text: String): String =
            Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "")
                .replace('đ', 'd')
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
    }
}
