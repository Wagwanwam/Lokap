package vn.nhom2.lokap.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.core.designsystem.theme.ThemeManager
import vn.nhom2.lokap.data.fake.FakeAuthStore
import vn.nhom2.lokap.data.fake.SampleSocialData
import vn.nhom2.lokap.domain.model.FriendStatus
class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects: Flow<SettingsEffect> = _effects.receiveAsFlow()

    init {
        observeSettingsAndBlockedUsers()
        observeThemeMode()
    }

    private fun observeThemeMode() {
        viewModelScope.launch {
            ThemeManager.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
    }

    private fun observeSettingsAndBlockedUsers() {
        viewModelScope.launch {
            combine(
                SampleSocialData.settings,
                SampleSocialData.friendStatuses,
                SampleSocialData.currentUserOverride
            ) { settings, statuses, _ ->
                Triple(settings, statuses, SampleSocialData.getCurrentUser())
            }.collect { (settings, statuses, currentUser) ->
                val blocked = SampleSocialData.directoryUsers.filter { user ->
                    statuses[user.id] == FriendStatus.BLOCKED
                }
                _uiState.update { prev ->
                    prev.copy(
                        isLoading = false,
                        currentUser = currentUser,
                        defaultLocationLevel = settings.defaultLocationLevel,
                        notifyNewPhotos = settings.notifyNewPhotos,
                        notifyReactions = settings.notifyReactions,
                        notifyMessages = settings.notifyMessages,
                        notifyFriendRequests = settings.notifyFriendRequests,
                        blockedUsers = blocked
                    )
                }
            }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            SettingsAction.NavigateBack -> {
                _effects.trySend(SettingsEffect.NavigateBack)
            }

            is SettingsAction.SelectThemeMode -> {
                ThemeManager.setThemeMode(action.mode)
                _uiState.update { it.copy(showThemeDialog = false) }
                _effects.trySend(
                    SettingsEffect.ShowMessage("Đã chuyển giao diện: ${action.mode.labelVi}")
                )
            }

            is SettingsAction.SelectLocationLevel -> {
                SampleSocialData.updateSettings {
                    it.copy(defaultLocationLevel = action.level)
                }
                _uiState.update { it.copy(showLocationLevelDialog = false) }
                _effects.trySend(
                    SettingsEffect.ShowMessage("Đã đặt mức vị trí mặc định: ${action.level.labelVi}")
                )
            }

            is SettingsAction.ToggleOnlineStatus -> {
                _uiState.update { it.copy(isOnlineStatusVisible = action.enabled) }
                _effects.trySend(
                    SettingsEffect.ShowMessage(
                        if (action.enabled) "Đã bật hiển thị trạng thái Online" else "Đã ẩn trạng thái Online"
                    )
                )
            }

            SettingsAction.OpenThemeDialog -> _uiState.update { it.copy(showThemeDialog = true) }
            SettingsAction.DismissThemeDialog -> _uiState.update { it.copy(showThemeDialog = false) }
            SettingsAction.OpenLocationLevelDialog -> _uiState.update { it.copy(showLocationLevelDialog = true) }
            SettingsAction.DismissLocationLevelDialog -> _uiState.update { it.copy(showLocationLevelDialog = false) }
            SettingsAction.OpenBlockedUsersDialog -> _uiState.update { it.copy(showBlockedUsersDialog = true) }
            SettingsAction.DismissBlockedUsersDialog -> _uiState.update { it.copy(showBlockedUsersDialog = false) }

            SettingsAction.OpenRecoveryPhoneDialog -> _uiState.update { it.copy(showRecoveryPhoneDialog = true) }
            SettingsAction.DismissRecoveryPhoneDialog -> _uiState.update { it.copy(showRecoveryPhoneDialog = false) }
            is SettingsAction.SaveRecoveryPhone -> {
                _uiState.update { it.copy(recoveryPhone = action.phone, showRecoveryPhoneDialog = false) }
                _effects.trySend(SettingsEffect.ShowMessage("Đã lưu số điện thoại khôi phục"))
            }

            SettingsAction.OpenLinkedEmailsDialog -> _uiState.update { it.copy(showLinkedEmailsDialog = true) }
            SettingsAction.DismissLinkedEmailsDialog -> _uiState.update { it.copy(showLinkedEmailsDialog = false) }
            is SettingsAction.AddLinkedEmail -> {
                _uiState.update { it.copy(linkedEmails = it.linkedEmails + action.email, showLinkedEmailsDialog = false) }
                _effects.trySend(SettingsEffect.ShowMessage("Đã thêm email liên kết: ${action.email}"))
            }

            SettingsAction.OpenChangePasswordDialog -> _uiState.update { it.copy(showChangePasswordDialog = true) }
            SettingsAction.DismissChangePasswordDialog -> _uiState.update { it.copy(showChangePasswordDialog = false) }
            is SettingsAction.ConfirmChangePassword -> {
                _uiState.update { it.copy(showChangePasswordDialog = false) }
                _effects.trySend(SettingsEffect.ShowMessage("Đã đổi mật khẩu thành công"))
            }

            is SettingsAction.UnblockUser -> {
                val target = _uiState.value.blockedUsers.find { it.id == action.userId }
                SampleSocialData.unblockUser(action.userId)
                _effects.trySend(
                    SettingsEffect.ShowMessage(
                        "Đã bỏ chặn ${target?.displayName ?: "người dùng"}"
                    )
                )
            }

            SettingsAction.OpenEditProfileSheet -> {
                val me = SampleSocialData.getCurrentUser()
                _uiState.update {
                    it.copy(
                        isEditProfileSheetOpen = true,
                        editDisplayNameInput = me.displayName,
                        editUsernameInput = me.username,
                        editBioInput = me.bio.ifBlank { "Ghi lại khoảnh khắc đẹp tại các tỉnh thành Việt Nam." },
                        editDisplayNameError = null,
                        editUsernameError = null
                    )
                }
            }

            SettingsAction.DismissEditProfileSheet -> {
                _uiState.update { it.copy(isEditProfileSheetOpen = false) }
            }

            is SettingsAction.ChangeEditDisplayName -> {
                val err = if (action.value.trim().length < 2) "Tên hiển thị tối thiểu 2 ký tự" else null
                _uiState.update { it.copy(editDisplayNameInput = action.value, editDisplayNameError = err) }
            }

            is SettingsAction.ChangeEditUsername -> {
                val clean = action.value.trim().removePrefix("@")
                val err = if (clean.length < 3) "Username tối thiểu 3 ký tự" else null
                _uiState.update { it.copy(editUsernameInput = clean, editUsernameError = err) }
            }

            is SettingsAction.ChangeEditBio -> {
                _uiState.update { it.copy(editBioInput = action.value) }
            }

            SettingsAction.SaveEditProfile -> {
                val state = _uiState.value
                val trimmedName = state.editDisplayNameInput.trim()
                val cleanUsername = state.editUsernameInput.trim().removePrefix("@")
                if (trimmedName.length < 2 || cleanUsername.length < 3) return
                SampleSocialData.updateMyProfile(trimmedName, cleanUsername, state.editBioInput)
                val updatedMe = SampleSocialData.getCurrentUser()
                _uiState.update {
                    it.copy(
                        isEditProfileSheetOpen = false,
                        currentUser = updatedMe
                    )
                }
                _effects.trySend(SettingsEffect.ShowMessage("Đã cập nhật hồ sơ cá nhân"))
            }

            SettingsAction.ClearCache -> {
                _uiState.update { it.copy(cacheSizeLabel = "0.0 MB") }
                _effects.trySend(SettingsEffect.ShowMessage("Đã giải phóng 34.5 MB bộ nhớ đệm"))
            }

            is SettingsAction.ToggleDataSaver -> {
                _uiState.update { it.copy(dataSaverEnabled = action.enabled) }
                val msg = if (action.enabled) "Đã bật tiết kiệm dữ liệu" else "Đã tắt tiết kiệm dữ liệu"
                _effects.trySend(SettingsEffect.ShowMessage(msg))
            }

            is SettingsAction.ToggleAutoSave -> {
                _uiState.update { it.copy(autoSaveToGallery = action.enabled) }
            }

            is SettingsAction.ToggleHighQuality -> {
                _uiState.update { it.copy(highQualityUpload = action.enabled) }
            }

            is SettingsAction.ToggleNotifyNewPhotos -> {
                SampleSocialData.updateSettings { it.copy(notifyNewPhotos = action.enabled) }
            }

            is SettingsAction.ToggleNotifyReactions -> {
                SampleSocialData.updateSettings { it.copy(notifyReactions = action.enabled) }
            }

            is SettingsAction.ToggleNotifyMessages -> {
                SampleSocialData.updateSettings { it.copy(notifyMessages = action.enabled) }
            }

            is SettingsAction.ToggleNotifyFriendRequests -> {
                SampleSocialData.updateSettings { it.copy(notifyFriendRequests = action.enabled) }
            }

            is SettingsAction.UpdateNotificationPermissionState -> {
                _uiState.update { it.copy(hasNotificationPermission = action.granted) }
            }

            is SettingsAction.UpdateAllPermissionsState -> {
                _uiState.update {
                    it.copy(
                        hasCameraPermission = action.cameraGranted,
                        hasLocationPermission = action.locationGranted,
                        hasNotificationPermission = action.notificationGranted
                    )
                }
            }

            SettingsAction.ClickGrantNotificationPermission -> {
                _effects.trySend(SettingsEffect.RequestNotificationPermissionOrOpenSettings)
            }

            SettingsAction.ClickOpenAppSettings -> {
                _effects.trySend(SettingsEffect.OpenAppSettings)
            }

            SettingsAction.OpenLogoutDialog -> {
                _uiState.update { it.copy(showLogoutDialog = true) }
            }

            SettingsAction.DismissLogoutDialog -> {
                _uiState.update { it.copy(showLogoutDialog = false) }
            }

            SettingsAction.ConfirmLogout -> {
                _uiState.update { it.copy(showLogoutDialog = false) }
                viewModelScope.launch {
                    FakeAuthStore.logout()
                    _effects.send(SettingsEffect.NavigateToLogin)
                }
            }

            SettingsAction.OpenDeleteAccountDialog -> {
                _uiState.update {
                    it.copy(
                        showDeleteAccountDialog = true,
                        deleteConfirmTextInput = ""
                    )
                }
            }

            SettingsAction.DismissDeleteAccountDialog -> {
                _uiState.update {
                    it.copy(
                        showDeleteAccountDialog = false,
                        deleteConfirmTextInput = ""
                    )
                }
            }

            is SettingsAction.ChangeDeleteConfirmText -> {
                _uiState.update { it.copy(deleteConfirmTextInput = action.text) }
            }

            SettingsAction.ConfirmDeleteAccount -> {
                if (!_uiState.value.isDeleteConfirmEnabled) return
                SampleSocialData.resetAccountForDemo()
                _uiState.update {
                    it.copy(
                        showDeleteAccountDialog = false,
                        deleteConfirmTextInput = ""
                    )
                }
                _effects.trySend(
                    SettingsEffect.ShowMessage("Tài khoản, ảnh và tin nhắn của bạn đã được xoá vĩnh viễn")
                )
                viewModelScope.launch {
                    FakeAuthStore.logout()
                    _effects.send(SettingsEffect.NavigateToLogin)
                }
            }
        }
    }
}
