package vn.nhom2.lokap.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import vn.nhom2.lokap.data.local.RefreshThrottles
import vn.nhom2.lokap.data.remote.Fs
import vn.nhom2.lokap.data.remote.isPermissionDenied
import vn.nhom2.lokap.data.remote.toAppException
import vn.nhom2.lokap.domain.model.Landmark
import vn.nhom2.lokap.domain.repository.AppException
import vn.nhom2.lokap.domain.repository.AuthRepository
import vn.nhom2.lokap.domain.repository.LandmarkDraft
import vn.nhom2.lokap.domain.repository.LandmarkRepository
import vn.nhom2.lokap.domain.repository.LandmarkRepository.Companion.DESCRIPTION_MAX_LENGTH
import vn.nhom2.lokap.domain.repository.LandmarkRepository.Companion.DUPLICATE_RADIUS_M
import vn.nhom2.lokap.domain.repository.LandmarkRepository.Companion.NAME_MAX_LENGTH

/**
 * Danh mục địa danh trên Firestore (`landmarks`). Ai đã xác nhận email cũng thêm được; Rules kiểm tra
 * trường, toạ độ trong khung Việt Nam và không cho sửa/xoá. Địa danh rác bị ẩn sau 3 báo cáo.
 * Danh mục nhỏ (vài trăm document) nên giữ trong bộ nhớ; Firestore SDK có cache offline cho lần mở sau.
 */
@Singleton
class FirestoreLandmarkRepository @Inject constructor(
    private val dbLazy: dagger.Lazy<FirebaseFirestore>,
    private val firebaseAuthLazy: dagger.Lazy<FirebaseAuth>,
    private val auth: AuthRepository,
    private val refreshThrottles: RefreshThrottles
) : LandmarkRepository {

    private val db: FirebaseFirestore get() = dbLazy.get()
    private val collection get() = db.collection(Fs.LANDMARKS)
    private val cache = MutableStateFlow<Map<String, Landmark>>(emptyMap())

    override val landmarks: Flow<List<Landmark>> = cache.map { it.values.sortedBy { lm -> lm.name } }

    override fun observeLandmark(id: String): Flow<Landmark?> = cache.map { it[id] }.distinctUntilChanged()

    // Mỗi lần mở Bản đồ đều gọi; danh mục giống nhau cho mọi tài khoản, thêm/ẩn địa danh đã sửa thẳng vào cache
    override suspend fun refresh(force: Boolean): Result<Unit> =
        refreshThrottles.landmarks.run(CATALOG_THROTTLE_KEY, force) { refreshCatalog() }

    private suspend fun refreshCatalog(): Result<Unit> = appCall {
        requireSignedIn()
        // Rules chỉ cho liệt kê địa danh đang hoạt động → truy vấn phải có đúng điều kiện này
        val docs = collection.whereEqualTo(Fs.STATUS, Fs.ACTIVE).limit(CATALOG_LIMIT).get().await().documents
        // Đổi tới 500 document ngoài luồng chính
        cache.value = withContext(Dispatchers.Default) { docs.mapNotNull { it.toLandmark() }.associateBy { it.id } }
    }

    override suspend fun refreshLandmark(id: String): Result<Unit> = appCall {
        requireSignedIn()
        val snap = try {
            collection.document(id).get().await()
        } catch (e: Exception) {
            // Bị ẩn (đủ 3 báo cáo) thì người khác không còn quyền đọc
            if (e.isPermissionDenied) null else throw e
        }
        val landmark = snap?.takeIf { it.getString(Fs.STATUS) == Fs.ACTIVE }?.toLandmark()
        cache.update { if (landmark == null) it - id else it + (id to landmark) }
    }

    override suspend fun createLandmark(draft: LandmarkDraft): Result<Landmark> = appCall {
        val me = requireSignedIn()
        val place = draft.place
        if (!place.lat.isFinite() || !place.lng.isFinite()) throw AppException("Chưa có vị trí của địa điểm này")
        if (place.lat !in 8.0..24.0 || place.lng !in 102.0..118.0) throw AppException("Địa danh phải ở Việt Nam")
        val name = draft.name.trim()
        if (name.length !in 2..NAME_MAX_LENGTH) throw AppException("Tên địa danh cần 2–$NAME_MAX_LENGTH ký tự")
        val description = draft.description?.trim()?.ifEmpty { null }
        if (description != null && description.length > DESCRIPTION_MAX_LENGTH) {
            throw AppException("Mô tả tối đa $DESCRIPTION_MAX_LENGTH ký tự")
        }

        // Cùng tên ở gần đó (VietMap có thể trả 2 mã cho cùng một chỗ) → dùng địa danh có sẵn
        cache.value.values.firstOrNull { it.isDuplicateOf(name, place.lat, place.lng) }?.let { return@appCall it }

        val ref = collection.document(landmarkIdFor(place.id.ifBlank { "$name@${place.lat},${place.lng}" }))
        val data = mapOf(
            Fs.NAME to name,
            Fs.SEARCH_NAME to Fs.searchKey(name).take(120),
            Fs.ADDRESS to place.address.take(200),
            Fs.DESCRIPTION to description,
            Fs.PROVINCE_CODE to draft.region.provinceCode,
            Fs.PROVINCE_NAME to draft.region.provinceName,
            Fs.WARD_CODE to draft.region.wardCode,
            Fs.WARD_NAME to draft.region.wardName,
            Fs.LAT to place.lat,
            Fs.LNG to place.lng,
            Fs.VIETMAP_REF to place.id.take(300),
            Fs.CREATED_BY to me,
            Fs.CREATED_AT to FieldValue.serverTimestamp(),
            Fs.STATUS to Fs.ACTIVE,
            Fs.REPORT_COUNT to 0
        )
        // Transaction: hai người thêm cùng địa điểm một lúc thì người sau nhận địa danh của người trước
        val existing = try {
            db.runTransaction { tx ->
                val snap = tx.get(ref)
                when {
                    !snap.exists() -> {
                        tx.set(ref, data)
                        null
                    }
                    snap.getString(Fs.STATUS) == Fs.ACTIVE -> snap.toLandmark() ?: HIDDEN
                    else -> HIDDEN
                }
            }.await()
        } catch (e: Exception) {
            // Địa danh của người khác đã bị ẩn: Rules không cho đọc
            if (e.isPermissionDenied || (e.cause?.isPermissionDenied == true)) HIDDEN else throw e
        }
        if (existing === HIDDEN) throw AppException("Địa danh này đã bị ẩn do bị báo cáo")
        val landmark = existing ?: Landmark(
            id = ref.id, name = name, provinceCode = draft.region.provinceCode, wardCode = draft.region.wardCode,
            lat = place.lat, lng = place.lng, description = description.orEmpty(),
            provinceName = draft.region.provinceName, wardName = draft.region.wardName,
            address = place.address, createdBy = me
        )
        cache.update { it + (landmark.id to landmark) }
        landmark
    }

    override suspend fun reportLandmark(id: String, reason: String): Result<Unit> = appCall {
        val me = requireSignedIn()
        val createdAt = firebaseAuthLazy.get().currentUser?.metadata?.creationTimestamp ?: 0L
        if (System.currentTimeMillis() - createdAt < 24.hours.inWholeMilliseconds) {
            throw AppException("Tài khoản mới tạo dưới 24 giờ chưa báo cáo được", AppException.Reason.ACCOUNT_TOO_NEW)
        }
        val ref = collection.document(id)
        val reportRef = ref.collection(Fs.REPORTS).document(me)
        db.runTransaction { tx ->
            val fresh = tx.get(ref)
            if (fresh.getString(Fs.CREATED_BY) == me) throw AppException("Không thể báo cáo địa danh của chính mình")
            if (fresh.exists() && !tx.get(reportRef).exists()) {
                val count = (fresh.getLong(Fs.REPORT_COUNT) ?: 0L) + 1
                tx.set(reportRef, mapOf(Fs.REASON to reason, Fs.CREATED_AT to FieldValue.serverTimestamp()))
                tx.update(
                    ref,
                    mapOf(
                        Fs.REPORT_COUNT to count,
                        Fs.STATUS to if (count >= REPORTS_TO_HIDE) Fs.HIDDEN else Fs.ACTIVE
                    )
                )
            }
            null
        }.await()
        // Đã báo cáo thì không hiện địa danh này nữa trên máy mình
        cache.update { it - id }
    }

    private fun Landmark.isDuplicateOf(otherName: String, lat: Double, lng: Double): Boolean {
        if (Fs.searchKey(name) != Fs.searchKey(otherName)) return false
        val dLat = (this.lat - lat) * METERS_PER_DEGREE
        val dLng = (this.lng - lng) * METERS_PER_DEGREE * cos(Math.toRadians(lat))
        return sqrt(dLat * dLat + dLng * dLng) <= DUPLICATE_RADIUS_M
    }

    private fun DocumentSnapshot.toLandmark(): Landmark? {
        if (!exists()) return null
        val name = getString(Fs.NAME) ?: return null
        val province = getString(Fs.PROVINCE_CODE) ?: return null
        val lat = getDouble(Fs.LAT)?.takeIf { it.isFinite() } ?: return null
        val lng = getDouble(Fs.LNG)?.takeIf { it.isFinite() } ?: return null
        return Landmark(
            id = id,
            name = name,
            provinceCode = province,
            wardCode = getString(Fs.WARD_CODE),
            lat = lat,
            lng = lng,
            description = getString(Fs.DESCRIPTION).orEmpty(),
            provinceName = getString(Fs.PROVINCE_NAME).orEmpty(),
            wardName = getString(Fs.WARD_NAME),
            address = getString(Fs.ADDRESS).orEmpty(),
            createdBy = getString(Fs.CREATED_BY)
        )
    }

    private fun requireSignedIn(): String = auth.currentUser.value?.id
        ?: throw AppException("Bạn cần đăng nhập lại", AppException.Reason.NOT_SIGNED_IN)

    private inline fun <T> appCall(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: AppException) {
        Result.failure(e)
    } catch (e: Exception) {
        // Lỗi ném trong transaction bị Firestore bọc lại: lấy lỗi gốc nếu là AppException
        Result.failure((e.cause as? AppException) ?: e.toAppException())
    }

    companion object {
        private const val CATALOG_LIMIT = 500L
        private const val CATALOG_THROTTLE_KEY = "catalog"
        private const val REPORTS_TO_HIDE = 3L
        private const val METERS_PER_DEGREE = 111_320.0

        /** Đánh dấu "có document nhưng bị ẩn" trong transaction (null = vừa tạo mới). */
        private val HIDDEN = Landmark(id = "", name = "", provinceCode = "", wardCode = null, lat = 0.0, lng = 0.0)

        /** Id cố định theo mã địa điểm VietMap: hai người thêm cùng địa điểm ra cùng một document. */
        fun landmarkIdFor(vietmapRef: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(vietmapRef.toByteArray())
            return "vm_" + digest.joinToString("") { "%02x".format(it) }.take(20)
        }
    }
}
