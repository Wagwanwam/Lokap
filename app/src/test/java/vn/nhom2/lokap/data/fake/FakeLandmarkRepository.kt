package vn.nhom2.lokap.data.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import vn.nhom2.lokap.domain.model.Landmark
import vn.nhom2.lokap.domain.repository.AppException
import vn.nhom2.lokap.domain.repository.ImageImporter
import vn.nhom2.lokap.domain.repository.LandmarkDraft
import vn.nhom2.lokap.domain.repository.LandmarkRepository

/** Danh mục địa danh giả trong bộ nhớ; mặc định rỗng như project Firebase mới. */
class FakeLandmarkRepository(initial: List<Landmark> = emptyList()) : LandmarkRepository {

    val store = MutableStateFlow(initial.associateBy { it.id })
    val created = mutableListOf<LandmarkDraft>()
    val reported = mutableListOf<Pair<String, String>>()

    override val landmarks: Flow<List<Landmark>> = store.map { it.values.sortedBy { lm -> lm.name } }

    override fun observeLandmark(id: String): Flow<Landmark?> = store.map { it[id] }

    override suspend fun refresh(force: Boolean): Result<Unit> = Result.success(Unit)

    override suspend fun refreshLandmark(id: String): Result<Unit> = Result.success(Unit)

    override suspend fun createLandmark(draft: LandmarkDraft): Result<Landmark> {
        val name = draft.name.trim()
        if (name.length < 2) return Result.failure(AppException("Tên địa danh cần 2–60 ký tự"))
        store.value.values.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { return Result.success(it) }
        created += draft
        val landmark = Landmark(
            id = "vm_${draft.place.id}", name = name, provinceCode = draft.region.provinceCode,
            wardCode = draft.region.wardCode, lat = draft.place.lat, lng = draft.place.lng,
            description = draft.description.orEmpty(), provinceName = draft.region.provinceName,
            wardName = draft.region.wardName, address = draft.place.address, createdBy = SampleData.currentUser.id
        )
        store.update { it + (landmark.id to landmark) }
        return Result.success(landmark)
    }

    override suspend fun reportLandmark(id: String, reason: String): Result<Unit> {
        reported += id to reason
        store.update { it - id }
        return Result.success(Unit)
    }
}

/** Nhập ảnh từ thư viện: trả đường dẫn cố định, hoặc lỗi khi [fail]. */
class FakeImageImporter(private val fail: Boolean = false) : ImageImporter {
    val imported = mutableListOf<String>()

    override suspend fun importImage(uri: String): Result<String> {
        imported += uri
        return if (fail) Result.failure(AppException("Không đọc được ảnh này")) else Result.success("/cache/gallery/$uri.jpg")
    }
}
