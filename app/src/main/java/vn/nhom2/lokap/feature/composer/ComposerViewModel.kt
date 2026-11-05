package vn.nhom2.lokap.feature.composer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.data.fake.FakePostStore
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.data.fake.SampleData2
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.Post
import vn.nhom2.lokap.domain.model.PostLocation
import vn.nhom2.lokap.domain.model.PostSendState
import vn.nhom2.lokap.domain.model.PublishDelay
import java.time.Duration
import java.util.UUID

class ComposerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ComposerUiState())
    val uiState: StateFlow<ComposerUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ComposerEffect>(Channel.BUFFERED)
    val effects: Flow<ComposerEffect> = _effects.receiveAsFlow()

    fun initImagePath(path: String) {
        if (_uiState.value.imagePath.isEmpty()) {
            _uiState.update { it.copy(imagePath = path) }
        }
    }

    fun onAction(action: ComposerAction) {
        when (action) {
            is ComposerAction.OnCaptionChange -> {
                val trimmed = action.text.take(60)
                _uiState.update { it.copy(caption = trimmed) }
            }

            is ComposerAction.OnSelectAudience -> {
                if (action.audience == Audience.FRIENDS || action.audience == Audience.PUBLIC) {
                    _uiState.update { it.copy(audience = action.audience) }
                }
            }

            is ComposerAction.OnSelectLocationLevel -> {
                val current = _uiState.value
                if (current.locationStatus == LocationAcquisitionState.UNAVAILABLE &&
                    action.level != LocationLevel.COUNTRY
                ) {
                    return
                }
                val openSheet = action.level == LocationLevel.PLACE && current.selectedPlace == null
                _uiState.update {
                    it.copy(
                        locationLevel = action.level,
                        isPlaceSheetOpen = openSheet
                    )
                }
            }

            ComposerAction.OnOpenPlaceSheet -> {
                _uiState.update {
                    it.copy(
                        isPlaceSheetOpen = true,
                        placeSearchQuery = "",
                        filteredPlaces = SampleData2.daNangPlaces
                    )
                }
            }

            ComposerAction.OnDismissPlaceSheet -> {
                _uiState.update { it.copy(isPlaceSheetOpen = false) }
            }

            is ComposerAction.OnPlaceSearchQueryChange -> {
                val query = action.query
                val filtered = if (query.isBlank()) {
                    SampleData2.daNangPlaces
                } else {
                    SampleData2.daNangPlaces.filter { place ->
                        place.name.contains(query, ignoreCase = true) ||
                            place.address.contains(query, ignoreCase = true)
                    }
                }
                _uiState.update {
                    it.copy(
                        placeSearchQuery = query,
                        filteredPlaces = filtered
                    )
                }
            }

            is ComposerAction.OnSelectPlace -> {
                _uiState.update {
                    it.copy(
                        selectedPlace = action.place,
                        locationLevel = LocationLevel.PLACE,
                        isPlaceSheetOpen = false
                    )
                }
            }

            is ComposerAction.OnSelectPublishDelay -> {
                _uiState.update { it.copy(publishDelay = action.delay) }
            }

            ComposerAction.OnCancelClick -> {
                if (_uiState.value.caption.isNotBlank()) {
                    _uiState.update { it.copy(showDiscardDialog = true) }
                } else {
                    sendEffect(ComposerEffect.NavigateBackToCamera)
                }
            }

            ComposerAction.OnDismissDiscardDialog -> {
                _uiState.update { it.copy(showDiscardDialog = false) }
            }

            ComposerAction.OnConfirmDiscard -> {
                _uiState.update { it.copy(showDiscardDialog = false) }
                sendEffect(ComposerEffect.NavigateBackToCamera)
            }

            ComposerAction.OnSendClick -> {
                val state = _uiState.value
                if (!state.canSend) return

                _uiState.update { it.copy(isSubmitting = true) }

                val now = SampleData.NOW
                val visibleAt = if (state.audience == Audience.PUBLIC) {
                    now.plus(Duration.ofDays(state.publishDelay.days.toLong()))
                } else {
                    null
                }

                val postLocation = buildPostLocation(state)
                val newPost = Post(
                    id = "post_${UUID.randomUUID()}",
                    author = SampleData.currentUser,
                    imageUrl = state.imagePath.ifBlank {
                        "https://picsum.photos/seed/lokap_new/800/800"
                    },
                    caption = state.caption.trim().ifEmpty { null },
                    audience = state.audience,
                    location = postLocation,
                    takenAt = now,
                    publicVisibleAt = visibleAt,
                    reactions = emptyList(),
                    myReaction = null,
                    sendState = PostSendState.PENDING
                )

                FakePostStore.addPendingPostAndSimulateSend(newPost)
                sendEffect(ComposerEffect.NavigateBackToCamera)
            }
        }
    }

    private fun buildPostLocation(state: ComposerUiState): PostLocation {
        return when (state.locationLevel) {
            LocationLevel.PLACE -> {
                val place = state.selectedPlace
                PostLocation(
                    level = LocationLevel.PLACE,
                    provinceCode = state.currentProvinceCode,
                    provinceName = state.currentProvinceName,
                    wardCode = state.currentWardCode,
                    wardName = state.currentWardName,
                    placeName = place?.name,
                    landmarkId = place?.id?.let { SampleData2.placeToLandmarkMap[it] },
                    lat = place?.lat ?: 16.0678,
                    lng = place?.lng ?: 108.2208
                )
            }

            LocationLevel.WARD -> PostLocation(
                level = LocationLevel.WARD,
                provinceCode = state.currentProvinceCode,
                provinceName = state.currentProvinceName,
                wardCode = state.currentWardCode,
                wardName = state.currentWardName,
                placeName = null,
                landmarkId = null,
                lat = 16.0678,
                lng = 108.2208
            )

            LocationLevel.PROVINCE -> PostLocation(
                level = LocationLevel.PROVINCE,
                provinceCode = state.currentProvinceCode,
                provinceName = state.currentProvinceName,
                wardCode = null,
                wardName = null,
                placeName = null,
                landmarkId = null,
                lat = 16.0544,
                lng = 108.2022
            )

            LocationLevel.COUNTRY -> PostLocation(
                level = LocationLevel.COUNTRY,
                provinceCode = null,
                provinceName = null,
                wardCode = null,
                wardName = null,
                placeName = null,
                landmarkId = null,
                lat = null,
                lng = null
            )
        }
    }

    private fun sendEffect(effect: ComposerEffect) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }
}
