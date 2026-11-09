package vn.nhom2.lokap.feature.composer

import androidx.compose.runtime.Immutable
import vn.nhom2.lokap.data.fake.SampleData2
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

enum class PhotoColorFilter(val labelVi: String) {
    ORIGINAL("Gốc"),
    VIVID("Tươi rực"),
    WARM_GOLDEN("Ấm áp"),
    COOL_BREEZE("Trong trẻo"),
    NOSTALGIA("Hoài niệm"),
    MONO_CHIC("Đen trắng"),
    DREAMY_PASTEL("Mộng mơ"),
    CINEMATIC("Điện ảnh")
}

enum class PhotoWatermarkStamp(val labelVi: String) {
    NONE("Không dấu"),
    LOKAP_MINIMAL("Lokap Minimal"),
    RETRO_LED_DATE("Ngày giờ LED"),
    TRAVEL_PASSPORT("Dấu mộc Visa"),
    ART_POLAROID("Khung Polaroid"),
    KODAK_FILM_35MM("Khung Film 35mm"),
    GPS_COORDINATES("Tọa độ GPS"),
    WEATHER_REPORT("Thời tiết"),
    VHS_CAMCORDER("Băng VHS 1998"),
    VINYL_MUSIC("Giai điệu"),
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
    val currentProvinceCode: String = SampleData2.DEFAULT_PROVINCE_CODE,
    val currentProvinceName: String = SampleData2.DEFAULT_PROVINCE_NAME,
    val currentWardCode: String = SampleData2.DEFAULT_WARD_CODE,
    val currentWardName: String = SampleData2.DEFAULT_WARD_NAME,
    val selectedPlace: Place? = null,
    val isPlaceSheetOpen: Boolean = false,
    val placeSearchQuery: String = "",
    val filteredPlaces: List<Place> = SampleData2.daNangPlaces,
    val publishDelay: PublishDelay = PublishDelay.NOW,
    val showDiscardDialog: Boolean = false,
    val isSubmitting: Boolean = false,
    val selectedTemplate: PostFilterTemplate = PostFilterTemplate.NONE,
    val selectedColorFilter: PhotoColorFilter = PhotoColorFilter.ORIGINAL,
    val selectedStamp: PhotoWatermarkStamp = PhotoWatermarkStamp.NONE,
    val streakCount: Int = 5
) {
    val canSend: Boolean
        get() = !isSubmitting &&
            locationStatus != LocationAcquisitionState.FETCHING &&
            (locationLevel != LocationLevel.PLACE || selectedPlace != null)
}

sealed interface ComposerAction {
    data class OnCaptionChange(val text: String) : ComposerAction
    data class OnSelectAudience(val audience: Audience) : ComposerAction
    data class OnSelectLocationLevel(val level: LocationLevel) : ComposerAction
    data class OnSelectTemplate(val template: PostFilterTemplate) : ComposerAction
    data class OnSelectColorFilter(val filter: PhotoColorFilter) : ComposerAction
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
    data object NavigateBackToCamera : ComposerEffect
}
