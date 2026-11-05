package vn.nhom2.lokap.feature.settings

import androidx.compose.runtime.Immutable
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.User

@Immutable
data class SettingsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val defaultLocationLevel: LocationLevel = LocationLevel.PROVINCE,
    val blockedUsers: List<User> = emptyList(),
    val notifyNewPhotos: Boolean = true,
    val notifyReactions: Boolean = true,
    val notifyMessages: Boolean = true,
    val notifyFriendRequests: Boolean = true,
    val hasNotificationPermission: Boolean = true,
    val showLogoutDialog: Boolean = false,
    val showDeleteAccountDialog: Boolean = false,
    val deleteConfirmTextInput: String = "",
    val appVersionLabel: String = "1.0.0"
) {
    /**
     * Chấp nhận cả hai kiểu đặt dấu "XOÁ" và "XÓA", chuẩn hoá NFC vì bàn phím có thể gửi
     * ký tự dựng sẵn hoặc tổ hợp (O + dấu sắc).
     */
    val isDeleteConfirmEnabled: Boolean
        get() = java.text.Normalizer.normalize(deleteConfirmTextInput.trim(), java.text.Normalizer.Form.NFC) in
            setOf("XOÁ", "XÓA")
}

sealed interface SettingsAction {
    data object NavigateBack : SettingsAction
    data class SelectLocationLevel(val level: LocationLevel) : SettingsAction
    data class UnblockUser(val userId: String) : SettingsAction

    data class ToggleNotifyNewPhotos(val enabled: Boolean) : SettingsAction
    data class ToggleNotifyReactions(val enabled: Boolean) : SettingsAction
    data class ToggleNotifyMessages(val enabled: Boolean) : SettingsAction
    data class ToggleNotifyFriendRequests(val enabled: Boolean) : SettingsAction
    data class UpdateNotificationPermissionState(val granted: Boolean) : SettingsAction
    data object ClickGrantNotificationPermission : SettingsAction

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
    data object RequestNotificationPermissionOrOpenSettings : SettingsEffect
    data class ShowMessage(val message: String) : SettingsEffect
}
