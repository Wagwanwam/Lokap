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
import vn.nhom2.lokap.domain.model.AccountContactData
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.repository.AuthRepository
import vn.nhom2.lokap.domain.repository.AccountContactRepository
import vn.nhom2.lokap.domain.repository.AuthStoreException
import vn.nhom2.lokap.domain.repository.AppCacheRepository
import vn.nhom2.lokap.domain.repository.DeviceSettingsRepository
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
    private val social: SocialRepository,
    private val deviceSettings: DeviceSettingsRepository,
    private val appCache: AppCacheRepository,
    private val accountContacts: AccountContactRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects: Flow<SettingsEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            deviceSettings.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        dataSaverEnabled = settings.dataSaverEnabled,
                        autoSaveToGallery = settings.autoSaveToGallery,
                        highQualityUpload = settings.highQualityUpload
                    )
                }
            }
        }
        viewModelScope.launch {
            ThemeManager.themeMode.collect { mode -> _uiState.update { it.copy(themeMode = mode) } }
        }
        viewModelScope.launch {
            auth.currentUser.collect { me ->
                val contacts = me?.id?.let(accountContacts::read) ?: AccountContactData()
                _uiState.update {
                    it.copy(
                        currentUser = me,
                        recoveryPhone = contacts.recoveryPhone,
                        linkedEmails = contacts.linkedEmails,
                        showRecoveryPhoneDialog = if (it.currentUser?.id == me?.id) it.showRecoveryPhoneDialog else false,
                        showLinkedEmailsDialog = if (it.currentUser?.id == me?.id) it.showLinkedEmailsDialog else false
                    )
                }
            }
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
                val error = SettingsValidators.validateRecoveryPhone(action.phone)
                if (error != null) {
                    _effects.trySend(SettingsEffect.ShowMessage(error))
                    return
                }
                updateContacts(closePhoneDialog = true) {
                    it.copy(recoveryPhone = SettingsValidators.normalizeRecoveryPhone(action.phone))
                }
            }
            SettingsAction.RemoveRecoveryPhone -> updateContacts(closePhoneDialog = true) { it.copy(recoveryPhone = null) }

            SettingsAction.OpenLinkedEmailsDialog -> _uiState.update { it.copy(showLinkedEmailsDialog = true) }
            SettingsAction.DismissLinkedEmailsDialog -> _uiState.update { it.copy(showLinkedEmailsDialog = false) }
            is SettingsAction.AddLinkedEmail -> {
                val email = action.email.trim()
                val error = SettingsValidators.validateLinkedEmail(email)
                if (error != null) {
                    _effects.trySend(SettingsEffect.ShowMessage(error))
                    return
                }
                updateContacts(closeEmailDialog = true) { contacts ->
                    contacts.copy(
                        linkedEmails = if (contacts.linkedEmails.any { it.equals(email, ignoreCase = true) }) contacts.linkedEmails else contacts.linkedEmails + email
                    )
                }
            }
            is SettingsAction.RemoveLinkedEmail -> updateContacts {
                it.copy(linkedEmails = it.linkedEmails.filterNot { email -> email.equals(action.email.trim(), ignoreCase = true) })
            }

            SettingsAction.OpenChangePasswordDialog -> _uiState.update { it.copy(showChangePasswordDialog = true, changePasswordError = null) }
            SettingsAction.DismissChangePasswordDialog -> if (!_uiState.value.isChangingPassword) {
                _uiState.update { it.copy(showChangePasswordDialog = false, changePasswordError = null) }
            }
            is SettingsAction.ConfirmChangePassword -> {
                if (_uiState.value.isChangingPassword) return
                val error = if (action.oldPass.isBlank()) "Vui lòng nhập mật khẩu hiện tại"
                    else AuthValidators.validatePassword(action.newPass)
                if (error != null) {
                    _uiState.update { it.copy(changePasswordError = error) }
                    return
                }
                _uiState.update { it.copy(isChangingPassword = true, changePasswordError = null) }
                viewModelScope.launch {
                    auth.changePassword(action.oldPass, action.newPass).fold(
                        onSuccess = {
                            _uiState.update { it.copy(isChangingPassword = false, showChangePasswordDialog = false) }
                            _effects.send(SettingsEffect.ShowMessage("Đã đổi mật khẩu thành công"))
                        },
                        onFailure = { error ->
                            _uiState.update { it.copy(isChangingPassword = false, changePasswordError = error.message ?: "Không đổi được mật khẩu") }
                        }
                    )
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
                if (_uiState.value.isClearingCache) return
                _uiState.update { it.copy(isClearingCache = true) }
                viewModelScope.launch {
                    val result = appCache.clearCache()
                    _uiState.update { it.copy(isClearingCache = false) }
                    _effects.send(SettingsEffect.ShowMessage(result.fold(
                        onSuccess = { "Đã xoá bộ nhớ đệm ảnh" },
                        onFailure = { it.message ?: "Không xoá được bộ nhớ đệm ảnh" }
                    )))
                }
            }

            SettingsAction.OpenPrivacyPolicy -> _uiState.update { it.copy(showPrivacyPolicy = true) }
            SettingsAction.DismissPrivacyPolicy -> _uiState.update { it.copy(showPrivacyPolicy = false) }

            is SettingsAction.ToggleDataSaver -> {
                deviceSettings.setDataSaver(action.enabled)
            }

            is SettingsAction.ToggleAutoSave -> {
                deviceSettings.setAutoSave(action.enabled)
            }

            is SettingsAction.ToggleHighQuality -> {
                deviceSettings.setHighQuality(action.enabled)
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
                val userId = auth.currentUser.value?.id
                _uiState.update { it.copy(showDeleteAccountDialog = false, deleteConfirmTextInput = "") }
                viewModelScope.launch {
                    auth.deleteAccount().fold(
                        onSuccess = {
                            userId?.let(accountContacts::clear)
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

    private fun updateContacts(
        closePhoneDialog: Boolean = false,
        closeEmailDialog: Boolean = false,
        transform: (AccountContactData) -> AccountContactData
    ) {
        val userId = auth.currentUser.value?.id
        if (userId == null) {
            _effects.trySend(SettingsEffect.ShowMessage("Bạn cần đăng nhập lại"))
            return
        }
        accountContacts.update(userId, transform).fold(
            onSuccess = { contacts ->
                _uiState.update {
                    it.copy(
                        recoveryPhone = contacts.recoveryPhone,
                        linkedEmails = contacts.linkedEmails,
                        showRecoveryPhoneDialog = it.showRecoveryPhoneDialog && !closePhoneDialog,
                        showLinkedEmailsDialog = it.showLinkedEmailsDialog && !closeEmailDialog
                    )
                }
            },
            onFailure = { _effects.trySend(SettingsEffect.ShowMessage("Không lưu được thông tin liên hệ, vui lòng thử lại")) }
        )
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
