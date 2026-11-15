package vn.nhom2.lokap.feature.auth.setup

import kotlinx.serialization.Serializable
@Serializable
data object SetupProfileRoute

enum class UsernameAvailability {
    IDLE,
    CHECKING,
    AVAILABLE,
    TAKEN,
    INVALID
}

data class SetupProfileUiState(
    val selectedAvatarUrl: String? = null,
    val presetAvatars: List<String> = emptyList(),
    val displayName: String = "",
    val username: String = "",
    val birthYear: Int? = null,
    val homeProvince: String = "",
    val bio: String = "",
    val displayNameTouched: Boolean = false,
    val usernameTouched: Boolean = false,
    val displayNameError: String? = null,
    val usernameError: String? = null,
    val birthYearError: String? = null,
    val bioError: String? = null,
    val isProvincePickerOpen: Boolean = false,
    val usernameAvailability: UsernameAvailability = UsernameAvailability.IDLE,
    val isSubmitting: Boolean = false,
    val serverError: String? = null,
    val isOffline: Boolean = false
) {
    val isFormValid: Boolean
        get() = displayName.trim().length in 2..30 &&
            displayNameError == null &&
            usernameError == null &&
            (usernameAvailability == UsernameAvailability.AVAILABLE || (username.isNotEmpty() && usernameError == null)) &&
            birthYearError == null &&
            bioError == null &&
            !isSubmitting
}

sealed interface SetupProfileAction {
    data class AvatarSelected(val avatarUrl: String?) : SetupProfileAction
    data class DisplayNameChanged(val value: String) : SetupProfileAction
    data class UsernameChanged(val value: String) : SetupProfileAction
    data class BirthYearChanged(val year: Int) : SetupProfileAction
    data class HomeProvinceChanged(val province: String) : SetupProfileAction
    data object ToggleProvincePicker : SetupProfileAction
    data class BioChanged(val bio: String) : SetupProfileAction
    data object DisplayNameFocusLost : SetupProfileAction
    data object UsernameFocusLost : SetupProfileAction
    data object SubmitClicked : SetupProfileAction
    data object RetryClicked : SetupProfileAction
}

sealed interface SetupProfileEffect {
    data object NavigateToCamera : SetupProfileEffect
}
