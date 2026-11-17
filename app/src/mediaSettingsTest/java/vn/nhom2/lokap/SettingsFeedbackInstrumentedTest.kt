package vn.nhom2.lokap

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.domain.model.AccountContactData
import vn.nhom2.lokap.domain.model.AppSettingsData
import vn.nhom2.lokap.domain.model.DeviceSettingsData
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.model.User
import vn.nhom2.lokap.domain.repository.AccountContactRepository
import vn.nhom2.lokap.domain.repository.AppCacheRepository
import vn.nhom2.lokap.domain.repository.AuthRepository
import vn.nhom2.lokap.domain.repository.AuthSession
import vn.nhom2.lokap.domain.repository.DeviceSettingsRepository
import vn.nhom2.lokap.domain.repository.ProfileDraft
import vn.nhom2.lokap.domain.repository.RegisterResult
import vn.nhom2.lokap.domain.repository.SettingsRepository
import vn.nhom2.lokap.domain.repository.SocialGraph
import vn.nhom2.lokap.domain.repository.SocialRepository
import vn.nhom2.lokap.feature.settings.SettingsAction
import vn.nhom2.lokap.feature.settings.SettingsRoute
import vn.nhom2.lokap.feature.settings.SettingsViewModel

/**
 * LQA-004 UI regression: real SettingsRoute/ViewModel, an in-memory authentication stub.
 * This checks visible feedback, not Firebase password changing; the installed user's credentials
 * and contact settings are never submitted or modified.
 */
@RunWith(AndroidJUnit4::class)
class SettingsFeedbackInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val store = ViewModelStore()
    private lateinit var auth: PasswordAuth
    private lateinit var viewModel: SettingsViewModel
    private var originalUid: String? = null

    @Before
    fun showIsolatedSettingsRoute() {
        originalUid = FirebaseAuth.getInstance().currentUser?.uid
        compose.runOnUiThread {
            auth = PasswordAuth()
            viewModel = SettingsViewModel(auth, MemorySettings(), EmptySocial(), MemoryDevice(),
                object : AppCacheRepository {
                    override suspend fun clearCache(): Result<Unit> = unexpected()
                }, object : AccountContactRepository {
                    override fun read(userId: String) = AccountContactData()
                    override fun update(userId: String, transform: (AccountContactData) -> AccountContactData): Result<AccountContactData> = unexpected()
                    override fun clear(userId: String): Result<Unit> = unexpected()
                })
            store.put("settings-feedback-test", viewModel)
            // MainActivity already owns Compose content; replace it rather than call rule.setContent.
            compose.activity.setContent {
                LokapTheme {
                    SettingsRoute(onNavigateBack = {}, onNavigateToLogin = {}, viewModel = viewModel)
                }
            }
        }
        compose.waitForIdle()
    }

    @After
    fun preserveLiveSessionAndReleaseCollectors() {
        compose.runOnUiThread { store.clear() }
        assertEquals("UI test must preserve the installed account", originalUid,
            FirebaseAuth.getInstance().currentUser?.uid)
    }

    @Test
    fun passwordFailureKeepsFormAndRetryDisplaysSuccessSnackbar() {
        compose.runOnUiThread {
            auth.result = Result.failure(IllegalArgumentException("Mật khẩu hiện tại không đúng"))
            viewModel.onAction(SettingsAction.OpenChangePasswordDialog)
        }
        compose.onNodeWithText("Mật khẩu hiện tại").performTextInput("OldPass123")
        compose.onNodeWithText("Mật khẩu mới (≥ 8 ký tự, có chữ và số)").performTextInput("NewPass123")
        compose.onNodeWithText("Xác nhận mật khẩu mới").performTextInput("NewPass123")
        compose.onNodeWithText("Lưu").performClick()
        compose.onNodeWithText("Mật khẩu hiện tại không đúng").assertIsDisplayed()
        compose.runOnIdle {
            assertTrue(viewModel.uiState.value.showChangePasswordDialog)
            assertFalse(viewModel.uiState.value.isChangingPassword)
            assertEquals(1, auth.calls)
            auth.result = Result.success(Unit)
        }
        compose.onNodeWithText("Lưu").performClick()
        compose.onNodeWithText("Đã đổi mật khẩu thành công").assertIsDisplayed()
        compose.onNodeWithText("Mật khẩu hiện tại").assertDoesNotExist()
        compose.runOnIdle {
            assertFalse(viewModel.uiState.value.showChangePasswordDialog)
            assertFalse(viewModel.uiState.value.isChangingPassword)
            assertEquals(2, auth.calls)
            assertEquals("OldPass123" to "NewPass123", auth.received)
        }
    }

    private class PasswordAuth : AuthRepository {
        private val user = User("settings-feedback-fixture", "fixture", "UI fixture", null)
        override val currentUser = MutableStateFlow<User?>(user)
        override val session = MutableStateFlow<AuthSession>(AuthSession.SignedIn(user))
        override val onboardingDone = MutableStateFlow(true)
        var result: Result<Unit> = Result.success(Unit)
        var calls = 0
        var received: Pair<String, String>? = null
        override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
            calls++
            received = currentPassword to newPassword
            return result
        }
        override fun markOnboardingDone(): Unit = unexpected()
        override suspend fun login(email: String, password: String): Result<User> = unexpected()
        override suspend fun loginWithGoogle(idToken: String): Result<User> = unexpected()
        override suspend fun register(email: String, password: String): Result<RegisterResult> = unexpected()
        override suspend fun confirmEmailVerified(): Result<Unit> = unexpected()
        override suspend fun resendVerificationEmail(email: String): Result<Unit> = unexpected()
        override suspend fun isEmailAvailable(email: String): Result<Boolean> = unexpected()
        override suspend fun isUsernameAvailable(username: String): Result<Boolean> = unexpected()
        override suspend fun completeProfile(draft: ProfileDraft): Result<User> = unexpected()
        override suspend fun updateProfile(displayName: String, username: String, bio: String): Result<User> = unexpected()
        override suspend fun resetPassword(email: String): Result<Unit> = unexpected()
        override suspend fun logout(): Unit = unexpected()
        override suspend fun deleteAccount(): Result<Unit> = unexpected()
    }

    private class MemorySettings : SettingsRepository {
        override val settings = MutableStateFlow(AppSettingsData())
        override suspend fun refresh(): Result<Unit> = unexpected()
        override suspend fun update(transform: (AppSettingsData) -> AppSettingsData): Result<Unit> = unexpected()
    }

    private class MemoryDevice : DeviceSettingsRepository {
        override val settings = MutableStateFlow(DeviceSettingsData())
        override fun setDataSaver(enabled: Boolean): Unit = unexpected()
        override fun setAutoSave(enabled: Boolean): Unit = unexpected()
        override fun setHighQuality(enabled: Boolean): Unit = unexpected()
    }

    private class EmptySocial : SocialRepository {
        override val maxFriends = 100
        override val graph = MutableStateFlow(SocialGraph())
        override suspend fun refresh(): Result<Unit> = unexpected()
        override suspend fun search(query: String): Result<List<User>> = unexpected()
        override suspend fun suggestions(): Result<List<User>> = unexpected()
        override suspend fun getUser(userId: String): Result<User?> = unexpected()
        override suspend fun getFriendCount(userId: String): Result<Int> = unexpected()
        override suspend fun sendFriendRequest(userId: String): Result<FriendStatus> = unexpected()
        override suspend fun cancelFriendRequest(userId: String): Result<Unit> = unexpected()
        override suspend fun acceptFriendRequest(userId: String): Result<Unit> = unexpected()
        override suspend fun declineFriendRequest(userId: String): Result<Unit> = unexpected()
        override suspend fun unfriend(userId: String): Result<Unit> = unexpected()
        override suspend fun block(userId: String): Result<Unit> = unexpected()
        override suspend fun unblock(userId: String): Result<Unit> = unexpected()
    }

    private companion object {
        fun <T> unexpected(): T = error("Unexpected repository mutation in isolated password UI test")
    }
}
