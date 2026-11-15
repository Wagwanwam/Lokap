package vn.nhom2.lokap.feature.landmark

import androidx.compose.runtime.Immutable
import vn.nhom2.lokap.domain.model.Place
import vn.nhom2.lokap.domain.repository.LandmarkRepository
import vn.nhom2.lokap.domain.repository.ResolvedRegion

/** Thêm địa danh: bước 1 tìm địa điểm VietMap, bước 2 đặt tên (+ mô tả ngắn) rồi lưu. */
@Immutable
data class AddLandmarkUiState(
    val query: String = "",
    val results: List<Place> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null,
    /** Địa điểm đã chọn, đã có toạ độ. */
    val selected: Place? = null,
    val region: ResolvedRegion? = null,
    val name: String = "",
    val description: String = "",
    val isResolving: Boolean = false,
    val isSaving: Boolean = false
) {
    val canSave: Boolean
        get() = selected != null && region != null && !isSaving &&
            name.trim().length in 2..LandmarkRepository.NAME_MAX_LENGTH
}

sealed interface AddLandmarkAction {
    data class OnQueryChange(val query: String) : AddLandmarkAction
    data class OnPickPlace(val place: Place) : AddLandmarkAction
    data object OnBackToSearch : AddLandmarkAction
    data class OnNameChange(val name: String) : AddLandmarkAction
    data class OnDescriptionChange(val description: String) : AddLandmarkAction
    data object OnSave : AddLandmarkAction
}

sealed interface AddLandmarkEffect {
    /** Đã thêm (hoặc địa điểm này đã là địa danh): mở màn Địa danh. */
    data class OpenLandmark(val landmarkId: String) : AddLandmarkEffect
    data class ShowMessage(val message: String) : AddLandmarkEffect
}
