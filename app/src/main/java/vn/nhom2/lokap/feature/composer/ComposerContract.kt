package vn.nhom2.lokap.feature.composer

import androidx.compose.runtime.Immutable
import vn.nhom2.lokap.domain.model.*
enum class LocationAcquisitionState {
    READY,
    FETCHING,
    UNAVAILABLE
}

enum class PostFilterTemplate(val labelVi: String) {
    NONE("Gốc"),
    POLAROID("Polaroid"),
    DATE_STAMP("Đóng dấu"),
    FILM_FRAME("Khung Film"),
    VINTAGE("Cổ điển")
}

enum class PhotoWatermarkStamp(val labelVi: String) {
    NONE("Không dấu"),
    LOKAP_MINIMAL("Lokap Minimal"),
    RETRO_LED_DATE("Ngày giờ LED"),
    TRAVEL_PASSPORT("Dấu mộc Visa"),
    ART_POLAROID("Khung Polaroid"),
    KODAK_FILM_35MM("Khung Film 35mm"),
    VHS_CAMCORDER("Băng VHS 1998"),
    MEMORY_RECEIPT("Vé kỷ niệm"),
    VIETNAM_PRIDE("Tự hào VN")
}

@Immutable
data class ComposerUiState(
    val imagePath: String = "",
    val caption: String = "",
    val audience: Audience = Audience.FRIENDS,
    val locationLevel: LocationLevel = LocationLevel.PROVINCE,
    val locationStatus: LocationAcquisitionState = LocationAcquisitionState.READY,
    val currentProvinceCode: String = "",
    val currentProvinceName: String = "",
    val currentWardCode: String = "",
    val currentWardName: String = "",
    val selectedPlace: Place? = null,
    val isPlaceSheetOpen: Boolean = false,
    val placeSearchQuery: String = "",
    val filteredPlaces: List<Place> = emptyList(),
    val publishDelay: PublishDelay = PublishDelay.NOW,
    val showDiscardDialog: Boolean = false,
    val isSubmitting: Boolean = false,
    val selectedTemplate: PostFilterTemplate = PostFilterTemplate.NONE,
    val selectedStamp: PhotoWatermarkStamp = PhotoWatermarkStamp.NONE,
    /** Mở từ màn Địa danh: ảnh luôn Công khai, vị trí = điểm công khai của địa danh. */
    val isLandmarkMode: Boolean = false,
    /** Địa danh đã tải (null khi đang tải hoặc địa danh không còn). */
    val landmark: Landmark? = null
) {
    val canSend: Boolean
        get() = !isSubmitting && when {
            // Vị trí lấy từ địa danh nên không cần chờ GPS
            isLandmarkMode -> landmark != null
            else -> locationStatus != LocationAcquisitionState.FETCHING &&
                (locationLevel != LocationLevel.PLACE || selectedPlace?.lat?.isNaN() == false)
        }

    /** Tên vị trí sẽ gắn vào bài, đúng mức đang chọn (mặc định: tỉnh). */
    val locationLabel: String
        get() = when {
            isLandmarkMode -> landmark?.name ?: "Đang tải…"
            locationStatus == LocationAcquisitionState.FETCHING -> "Đang định vị…"
            locationStatus == LocationAcquisitionState.UNAVAILABLE ||
                locationLevel == LocationLevel.COUNTRY -> "Việt Nam"
            locationLevel == LocationLevel.PLACE -> selectedPlace?.name ?: currentProvinceName
            locationLevel == LocationLevel.WARD ->
                listOf(currentWardName, currentProvinceName).filter { it.isNotBlank() }.joinToString(", ")
            else -> currentProvinceName
        }.ifBlank { "Việt Nam" }
}

sealed interface ComposerAction {
    data class OnCaptionChange(val text: String) : ComposerAction
    data class OnSelectAudience(val audience: Audience) : ComposerAction
    data class OnSelectLocationLevel(val level: LocationLevel) : ComposerAction
    data class OnSelectTemplate(val template: PostFilterTemplate) : ComposerAction
    data class OnSelectStamp(val stamp: PhotoWatermarkStamp) : ComposerAction
    data object OnOpenPlaceSheet : ComposerAction
    data object OnDismissPlaceSheet : ComposerAction
    data class OnPlaceSearchQueryChange(val query: String) : ComposerAction
    data class OnSelectPlace(val place: Place) : ComposerAction
    data class OnSelectPublishDelay(val delay: PublishDelay) : ComposerAction
    data object OnCancelClick : ComposerAction
    data object OnDismissDiscardDialog : ComposerAction
    data object OnConfirmDiscard : ComposerAction
    data object OnSendClick : ComposerAction
}

sealed interface ComposerEffect {
    /** Huỷ: quay lại camera (hoặc màn Địa danh nếu chọn ảnh từ thư viện). */
    data object NavigateBackToCamera : ComposerEffect
    /** Đã lưu bài vào hàng đợi gửi. */
    data object PostSent : ComposerEffect
    /** Gửi thất bại (mất mạng, ảnh hỏng…): hiện lỗi, nút Gửi mở lại. */
    data class ShowMessage(val message: String) : ComposerEffect
}
