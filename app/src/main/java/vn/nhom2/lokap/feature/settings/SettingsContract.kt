package vn.nhom2.lokap.feature.settings

import androidx.compose.runtime.Immutable
import vn.nhom2.lokap.core.designsystem.theme.ThemeMode
import vn.nhom2.lokap.domain.model.*
@Immutable
data class SettingsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentUser: User? = null,
    val isEditProfileSheetOpen: Boolean = false,
    val editDisplayNameInput: String = "",
    val editUsernameInput: String = "",
    val editBioInput: String = "",
    val editDisplayNameError: String? = null,
    val editUsernameError: String? = null,
    val cacheSizeLabel: String = "34.5 MB",
    val dataSaverEnabled: Boolean = false,
    val autoSaveToGallery: Boolean = false,
    val highQualityUpload: Boolean = true,
    val defaultLocationLevel: LocationLevel = LocationLevel.PROVINCE,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val blockedUsers: List<User> = emptyList(),
    val notifyNewPhotos: Boolean = true,
    val notifyReactions: Boolean = true,
    val notifyMessages: Boolean = true,
    val notifyFriendRequests: Boolean = true,
    val hasNotificationPermission: Boolean = true,
    val hasCameraPermission: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val showDeleteAccountDialog: Boolean = false,
    val deleteConfirmTextInput: String = "",
    val appVersionLabel: String = "1.0.0",
    val recoveryPhone: String? = null,
    val linkedEmails: List<String> = listOf("user@lokap.vn"),
    val isOnlineStatusVisible: Boolean = true,
    val showThemeDialog: Boolean = false,
    val showLocationLevelDialog: Boolean = false,
    val showBlockedUsersDialog: Boolean = false,
    val showRecoveryPhoneDialog: Boolean = false,
    val showLinkedEmailsDialog: Boolean = false,
    val showChangePasswordDialog: Boolean = false
) {
    val isDeleteConfirmEnabled: Boolean
        get() = java.text.Normalizer.normalize(deleteConfirmTextInput.trim(), java.text.Normalizer.Form.NFC) in
            setOf("XOÁ", "XÓA")
}

sealed interface SettingsAction {
    data object NavigateBack : SettingsAction
    data class SelectThemeMode(val mode: ThemeMode) : SettingsAction
    data class SelectLocationLevel(val level: LocationLevel) : SettingsAction
    data class UnblockUser(val userId: String) : SettingsAction

    data class ToggleOnlineStatus(val enabled: Boolean) : SettingsAction

    data object OpenThemeDialog : SettingsAction
    data object DismissThemeDialog : SettingsAction
    data object OpenLocationLevelDialog : SettingsAction
    data object DismissLocationLevelDialog : SettingsAction
    data object OpenBlockedUsersDialog : SettingsAction
    data object DismissBlockedUsersDialog : SettingsAction

    data object OpenRecoveryPhoneDialog : SettingsAction
    data object DismissRecoveryPhoneDialog : SettingsAction
    data class SaveRecoveryPhone(val phone: String) : SettingsAction
    data object OpenLinkedEmailsDialog : SettingsAction
    data object DismissLinkedEmailsDialog : SettingsAction
    data class AddLinkedEmail(val email: String) : SettingsAction
    data object OpenChangePasswordDialog : SettingsAction
    data object DismissChangePasswordDialog : SettingsAction
    data class ConfirmChangePassword(val oldPass: String, val newPass: String) : SettingsAction

    data object OpenEditProfileSheet : SettingsAction
    data object DismissEditProfileSheet : SettingsAction
    data class ChangeEditDisplayName(val value: String) : SettingsAction
    data class ChangeEditUsername(val value: String) : SettingsAction
    data class ChangeEditBio(val value: String) : SettingsAction
    data object SaveEditProfile : SettingsAction

    data object ClearCache : SettingsAction
    data class ToggleDataSaver(val enabled: Boolean) : SettingsAction
    data class ToggleAutoSave(val enabled: Boolean) : SettingsAction
    data class ToggleHighQuality(val enabled: Boolean) : SettingsAction

    data class ToggleNotifyNewPhotos(val enabled: Boolean) : SettingsAction
    data class ToggleNotifyReactions(val enabled: Boolean) : SettingsAction
    data class ToggleNotifyMessages(val enabled: Boolean) : SettingsAction
    data class ToggleNotifyFriendRequests(val enabled: Boolean) : SettingsAction
    data class UpdateNotificationPermissionState(val granted: Boolean) : SettingsAction
    data class UpdateAllPermissionsState(
        val cameraGranted: Boolean,
        val locationGranted: Boolean,
        val notificationGranted: Boolean
    ) : SettingsAction
    data object ClickGrantNotificationPermission : SettingsAction
    data object ClickOpenAppSettings : SettingsAction

    data object OpenLogoutDialog : SettingsAction
    data object DismissLogoutDialog : SettingsAction
    data object ConfirmLogout : SettingsAction

    data object OpenDeleteAccountDialog : SettingsAction
    data object DismissDeleteAccountDialog : SettingsAction
    data class ChangeDeleteConfirmText(val text: String) : SettingsAction
    data object ConfirmDeleteAccount : SettingsAction
}

sealed interface SettingsEffect {
    data object NavigateBack : SettingsEffect
    data object NavigateToLogin : SettingsEffect
    data object RequestNotificationPermissionOrOpenSettings : SettingsEffect
    data object OpenAppSettings : SettingsEffect
    data class ShowMessage(val message: String) : SettingsEffect
}
