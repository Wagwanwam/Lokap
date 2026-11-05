package vn.nhom2.lokap.feature.auth.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.data.fake.AuthStoreException
import vn.nhom2.lokap.data.fake.FakeAuthStore
import vn.nhom2.lokap.feature.auth.AuthValidators

class SetupProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SetupProfileUiState())
    val uiState: StateFlow<SetupProfileUiState> = _uiState.asStateFlow()

    private val _effect = Channel<SetupProfileEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var usernameCheckJob: Job? = null

    fun onAction(action: SetupProfileAction) {
        when (action) {
            is SetupProfileAction.AvatarSelected -> {
                _uiState.update { it.copy(selectedAvatarUrl = action.avatarUrl) }
            }

            is SetupProfileAction.DisplayNameChanged -> {
                _uiState.update { state ->
                    state.copy(
                        displayName = action.value,
                        displayNameError = if (state.displayNameTouched) {
                            AuthValidators.validateDisplayName(action.value)
                        } else {
                            null
                        },
                        serverError = null,
                        isOffline = false
                    )
                }
            }

            is SetupProfileAction.UsernameChanged -> {
                val lowered = action.value.lowercase()
                onUsernameInputChanged(lowered)
            }

            SetupProfileAction.DisplayNameFocusLost -> {
                _uiState.update { state ->
                    state.copy(
                        displayNameTouched = true,
                        displayNameError = AuthValidators.validateDisplayName(state.displayName)
                    )
                }
            }

            SetupProfileAction.UsernameFocusLost -> {
                _uiState.update { state ->
                    val formatError = AuthValidators.validateUsernameFormat(state.username)
                    state.copy(
                        usernameTouched = true,
                        usernameError = formatError ?: if (state.usernameAvailability == UsernameAvailability.TAKEN) {
                            "Username này đã có người dùng"
                        } else {
                            null
                        }
                    )
                }
            }

            SetupProfileAction.SubmitClicked, SetupProfileAction.RetryClicked -> submitProfile()
        }
    }

    private fun onUsernameInputChanged(lowered: String) {
        usernameCheckJob?.cancel()

        val formatError = AuthValidators.validateUsernameFormat(lowered)
        if (formatError != null) {
            _uiState.update { state ->
                state.copy(
                    username = lowered,
                    usernameAvailability = if (lowered.isEmpty()) {
                        UsernameAvailability.IDLE
                    } else {
                        UsernameAvailability.INVALID
                    },
                    usernameError = if (state.usernameTouched) formatError else null,
                    serverError = null,
                    isOffline = false
                )
            }
            return
        }

        _uiState.update { state ->
            state.copy(
                username = lowered,
                usernameAvailability = UsernameAvailability.CHECKING,
                usernameError = null,
                serverError = null,
                isOffline = false
            )
        }

        usernameCheckJob = viewModelScope.launch {
            delay(400) // Debounce 400ms
            val result = FakeAuthStore.isUsernameAvailable(lowered)
            result.fold(
                onSuccess = { available ->
                    _uiState.update { state ->
                        if (state.username != lowered) return@update state
                        state.copy(
                            usernameAvailability = if (available) {
                                UsernameAvailability.AVAILABLE
                            } else {
                                UsernameAvailability.TAKEN
                            },
                            usernameError = if (available) {
                                null
                            } else {
                                "Username này đã có người dùng"
                            }
                        )
                    }
                },
                onFailure = {
                    _uiState.update { state ->
                        state.copy(
                            usernameAvailability = UsernameAvailability.IDLE,
                            isOffline = true,
                            serverError = "Mất kết nối mạng khi kiểm tra username"
                        )
                    }
                }
            )
        }
    }

    private fun submitProfile() {
        val current = _uiState.value
        if (!current.isFormValid || current.isSubmitting) return

        val displayNameError = AuthValidators.validateDisplayName(current.displayName)
        val usernameFormatError = AuthValidators.validateUsernameFormat(current.username)
        if (displayNameError != null || usernameFormatError != null) {
            _uiState.update {
                it.copy(
                    displayNameTouched = true,
                    usernameTouched = true,
                    displayNameError = displayNameError,
                    usernameError = usernameFormatError
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isSubmitting = true,
                serverError = null,
                isOffline = false
            )
        }

        viewModelScope.launch {
            val result = FakeAuthStore.completeProfile(
                username = current.username,
                displayName = current.displayName,
                avatarUrl = current.selectedAvatarUrl
            )
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effect.send(SetupProfileEffect.NavigateToCamera)
                },
                onFailure = { error ->
                    val isOffline = error is AuthStoreException.NetworkError
                    val isTaken = error is AuthStoreException.UsernameAlreadyTaken
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            isOffline = isOffline,
                            usernameAvailability = if (isTaken) UsernameAvailability.TAKEN else it.usernameAvailability,
                            usernameError = if (isTaken) error.message else it.usernameError,
                            serverError = if (!isTaken) error.message else null
                        )
                    }
                }
            )
        }
    }
}
