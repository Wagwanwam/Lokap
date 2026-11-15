package vn.nhom2.lokap.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.core.designsystem.theme.ThemeManager
import vn.nhom2.lokap.domain.model.AppSettingsData
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.repository.AuthRepository
import vn.nhom2.lokap.domain.repository.AuthStoreException
import vn.nhom2.lokap.domain.repository.SettingsRepository
import vn.nhom2.lokap.domain.repository.SocialRepository
import vn.nhom2.lokap.feature.auth.AuthValidators

/**
 * Cài đặt: hồ sơ / đăng xuất / đổi mật khẩu / xoá tài khoản qua AuthRepository, mức vị trí và công tắc
 * thông báo qua SettingsRepository, danh sách chặn qua SocialRepository. Giao diện (sáng/tối) vẫn ở máy.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val social: SocialRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects: Flow<SettingsEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            ThemeManager.themeMode.collect { mode -> _uiState.update { it.copy(themeMode = mode) } }
        }
        viewModelScope.launch {
            auth.currentUser.collect { me -> _uiState.update { it.copy(currentUser = me) } }
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        defaultLocationLevel = settings.defaultLocationLevel,
                        notifyNewPhotos = settings.notifyNewPhotos,
                        notifyReactions = settings.notifyReactions,
                        notifyMessages = settings.notifyMessages,
                        notifyFriendRequests = settings.notifyFriendRequests
                    )
                }
            }
        }
        viewModelScope.launch {
            social.graph.collect { graph -> _uiState.update { it.copy(blockedUsers = graph.usersWith(FriendStatus.BLOCKED)) } }
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
            }

            is SettingsAction.SelectLocationLevel -> {
                _uiState.update { it.copy(showLocationLevelDialog = false) }
                updateSettings {
                    it.copy(defaultLocationLevel = action.level)
                }
            }

            is SettingsAction.ToggleOnlineStatus -> {
                _uiState.update { it.copy(isOnlineStatusVisible = action.enabled) }
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
            }

            SettingsAction.OpenLinkedEmailsDialog -> _uiState.update { it.copy(showLinkedEmailsDialog = true) }
            SettingsAction.DismissLinkedEmailsDialog -> _uiState.update { it.copy(showLinkedEmailsDialog = false) }
            is SettingsAction.AddLinkedEmail -> {
                _uiState.update { it.copy(linkedEmails = it.linkedEmails + action.email, showLinkedEmailsDialog = false) }
            }

            SettingsAction.OpenChangePasswordDialog -> _uiState.update { it.copy(showChangePasswordDialog = true) }
            SettingsAction.DismissChangePasswordDialog -> _uiState.update { it.copy(showChangePasswordDialog = false) }
            is SettingsAction.ConfirmChangePassword -> {
                _uiState.update { it.copy(showChangePasswordDialog = false) }
                // Cùng luật với Đăng ký: ≥ 8 ký tự, có cả chữ và số
                val error = AuthValidators.validatePassword(action.newPass)
                if (error != null) {
                    _effects.trySend(SettingsEffect.ShowMessage(error))
                    return
                }
                viewModelScope.launch {
                    auth.changePassword(action.newPass).onFailure {
                        _effects.trySend(SettingsEffect.ShowMessage(it.message ?: "Không đổi được mật khẩu"))
                    }
                }
            }

            is SettingsAction.UnblockUser -> {
                viewModelScope.launch {
                    social.unblock(action.userId).onFailure {
                        _effects.trySend(SettingsEffect.ShowMessage(it.message ?: "Không bỏ chặn được"))
                    }
                }
            }

            SettingsAction.OpenEditProfileSheet -> {
                val me = _uiState.value.currentUser ?: return
                _uiState.update {
                    it.copy(
                        isEditProfileSheetOpen = true,
                        editDisplayNameInput = me.displayName,
                        editUsernameInput = me.username,
                        editBioInput = me.bio,
                        editDisplayNameError = null,
                        editUsernameError = null
                    )
                }
            }

            SettingsAction.DismissEditProfileSheet -> {
                _uiState.update { it.copy(isEditProfileSheetOpen = false) }
            }

            is SettingsAction.ChangeEditDisplayName -> {
                val err = AuthValidators.validateDisplayName(action.value)
                _uiState.update { it.copy(editDisplayNameInput = action.value, editDisplayNameError = err) }
            }

            is SettingsAction.ChangeEditUsername -> {
                val clean = action.value.trim().removePrefix("@").lowercase()
                _uiState.update { it.copy(editUsernameInput = clean, editUsernameError = AuthValidators.validateUsernameFormat(clean)) }
            }

            is SettingsAction.ChangeEditBio -> {
                // Ràng buộc profiles_bio_length: tối đa 60 ký tự
                _uiState.update { it.copy(editBioInput = action.value.take(60)) }
            }

            SettingsAction.SaveEditProfile -> saveProfile()

            SettingsAction.ClearCache -> {
                _effects.trySend(SettingsEffect.ShowMessage("Chưa khả dụng"))
            }

            is SettingsAction.ToggleDataSaver -> {
                _uiState.update { it.copy(dataSaverEnabled = action.enabled) }
            }

            is SettingsAction.ToggleAutoSave -> {
                _uiState.update { it.copy(autoSaveToGallery = action.enabled) }
            }

            is SettingsAction.ToggleHighQuality -> {
                _uiState.update { it.copy(highQualityUpload = action.enabled) }
            }

            is SettingsAction.ToggleNotifyNewPhotos -> updateSettings { it.copy(notifyNewPhotos = action.enabled) }

            is SettingsAction.ToggleNotifyReactions -> updateSettings { it.copy(notifyReactions = action.enabled) }

            is SettingsAction.ToggleNotifyMessages -> updateSettings { it.copy(notifyMessages = action.enabled) }

            is SettingsAction.ToggleNotifyFriendRequests -> updateSettings { it.copy(notifyFriendRequests = action.enabled) }

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
                    auth.logout()
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
                _uiState.update { it.copy(showDeleteAccountDialog = false, deleteConfirmTextInput = "") }
                viewModelScope.launch {
                    auth.deleteAccount().fold(
                        onSuccess = {
                            _effects.send(SettingsEffect.NavigateToLogin)
                        },
                        onFailure = { error ->
                            _effects.send(SettingsEffect.ShowMessage(error.message ?: "Không xoá được tài khoản, thử lại sau"))
                        }
                    )
                }
            }
        }
    }

    /** Đổi ngay trên máy rồi lưu lên server (Repository tự trả lại giá trị cũ nếu lỗi). */
    private fun updateSettings(successMessage: String? = null, transform: (AppSettingsData) -> AppSettingsData) {
        viewModelScope.launch {
            settingsRepository.update(transform).fold(
                onSuccess = { if (successMessage != null) _effects.trySend(SettingsEffect.ShowMessage(successMessage)) },
                onFailure = { error -> _effects.trySend(SettingsEffect.ShowMessage(error.message ?: "Không lưu được cài đặt")) }
            )
        }
    }

    private fun saveProfile() {
        val state = _uiState.value
        val trimmedName = state.editDisplayNameInput.trim()
        val cleanUsername = state.editUsernameInput.trim().removePrefix("@").lowercase()
        val nameError = AuthValidators.validateDisplayName(trimmedName)
        val usernameError = AuthValidators.validateUsernameFormat(cleanUsername)
        if (nameError != null || usernameError != null) {
            _uiState.update { it.copy(editDisplayNameError = nameError, editUsernameError = usernameError) }
            return
        }
        val bioError = AuthValidators.validateBio(state.editBioInput)
        if (bioError != null) {
            _effects.trySend(SettingsEffect.ShowMessage(bioError))
            return
        }
        viewModelScope.launch {
            auth.updateProfile(displayName = trimmedName, username = cleanUsername, bio = state.editBioInput).fold(
                onSuccess = { updated ->
                    _uiState.update { it.copy(isEditProfileSheetOpen = false, currentUser = updated) }
                },
                onFailure = { error ->
                    if (error is AuthStoreException.UsernameAlreadyTaken) {
                        _uiState.update { it.copy(editUsernameError = error.message) }
                    } else {
                        _effects.trySend(SettingsEffect.ShowMessage(error.message ?: "Không lưu được, thử lại sau"))
                    }
                }
            )
        }
    }
}
