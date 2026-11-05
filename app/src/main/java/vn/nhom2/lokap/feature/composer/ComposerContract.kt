package vn.nhom2.lokap.feature.composer

import androidx.compose.runtime.Immutable
import vn.nhom2.lokap.data.fake.SampleData2
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.Place
import vn.nhom2.lokap.domain.model.PublishDelay

enum class LocationAcquisitionState {
    READY,
    FETCHING,
    UNAVAILABLE
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
    val isSubmitting: Boolean = false
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
