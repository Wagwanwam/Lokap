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

val PresetAvatars = listOf(
    "https://picsum.photos/seed/lokap_avatar_1/200/200",
    "https://picsum.photos/seed/lokap_avatar_2/200/200",
    "https://picsum.photos/seed/lokap_avatar_3/200/200",
    "https://picsum.photos/seed/lokap_avatar_4/200/200",
    "https://picsum.photos/seed/lokap_avatar_5/200/200",
    "https://picsum.photos/seed/lokap_avatar_6/200/200"
)

data class SetupProfileUiState(
    val selectedAvatarUrl: String? = PresetAvatars.first(),
    val presetAvatars: List<String> = PresetAvatars,
    val displayName: String = "",
    val username: String = "",
    val birthYear: Int = 2004,
    val homeProvince: String = "Đà Nẵng",
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
