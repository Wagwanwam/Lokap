package vn.nhom2.lokap.feature.settings

import vn.nhom2.lokap.data.fake.FakeAuthStore
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
import vn.nhom2.lokap.data.fake.SampleSocialData
import vn.nhom2.lokap.domain.model.FriendStatus

class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects: Flow<SettingsEffect> = _effects.receiveAsFlow()

    init {
        observeSettingsAndBlockedUsers()
    }

    private fun observeSettingsAndBlockedUsers() {
        viewModelScope.launch {
            combine(
                SampleSocialData.settings,
                SampleSocialData.friendStatuses
            ) { settings, statuses ->
                settings to statuses
            }.collect { (settings, statuses) ->
                val blocked = SampleSocialData.directoryUsers.filter { user ->
                    statuses[user.id] == FriendStatus.BLOCKED
                }
                _uiState.update { prev ->
                    prev.copy(
                        isLoading = false,
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

            is SettingsAction.SelectLocationLevel -> {
                SampleSocialData.updateSettings {
                    it.copy(defaultLocationLevel = action.level)
                }
                _effects.trySend(
                    SettingsEffect.ShowMessage("Đã đặt mức vị trí mặc định: ${action.level.labelVi}")
                )
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

            SettingsAction.ClickGrantNotificationPermission -> {
                _effects.trySend(SettingsEffect.RequestNotificationPermissionOrOpenSettings)
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
