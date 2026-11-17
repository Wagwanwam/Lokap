package vn.nhom2.lokap.feature

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import vn.nhom2.lokap.data.fake.FakeAuthRepository
import vn.nhom2.lokap.data.fake.FakeSettingsRepository
import vn.nhom2.lokap.data.fake.FakeSocialRepository
import vn.nhom2.lokap.data.local.PersistentAccountContactRepository
import vn.nhom2.lokap.domain.model.AccountContactData
import vn.nhom2.lokap.domain.model.User
import vn.nhom2.lokap.domain.repository.AccountContactRepository
import vn.nhom2.lokap.domain.repository.AuthRepository
import vn.nhom2.lokap.feature.settings.SettingsAction
import vn.nhom2.lokap.feature.settings.SettingsEffect
import vn.nhom2.lokap.feature.settings.SettingsViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRegressionTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    private fun settings(
        auth: AuthRepository = FakeAuthRepository(),
        device: TestDeviceSettingsRepository = TestDeviceSettingsRepository(),
        cache: TestAppCacheRepository = TestAppCacheRepository(),
        contacts: AccountContactRepository = PersistentAccountContactRepository(TestContactPreferences())
    ) = SettingsViewModel(auth, FakeSettingsRepository(), FakeSocialRepository(), device, cache, contacts)

    @Test fun `invalid phone and email actions cannot bypass UI validation`() = runTest(dispatcher) {
        val vm = settings()
        vm.onAction(SettingsAction.OpenRecoveryPhoneDialog)
        vm.onAction(SettingsAction.SaveRecoveryPhone("abcdefghi"))
        assertNull(vm.uiState.value.recoveryPhone)
        assertTrue(vm.uiState.value.showRecoveryPhoneDialog)
        assertTrue(vm.effects.first() is SettingsEffect.ShowMessage)

        vm.onAction(SettingsAction.OpenLinkedEmailsDialog)
        vm.onAction(SettingsAction.AddLinkedEmail("@.com"))
        assertTrue(vm.uiState.value.linkedEmails.isEmpty())
        assertTrue(vm.uiState.value.showLinkedEmailsDialog)
        assertTrue(vm.effects.first() is SettingsEffect.ShowMessage)
    }

    @Test fun `valid contact input is accepted and normalized without duplicates`() {
        val vm = settings()
        vm.onAction(SettingsAction.SaveRecoveryPhone("0912 345 678"))
        assertEquals("0912345678", vm.uiState.value.recoveryPhone)
        vm.onAction(SettingsAction.AddLinkedEmail(" person+qa@example.com "))
        vm.onAction(SettingsAction.AddLinkedEmail("PERSON+QA@example.com"))
        assertEquals(listOf("person+qa@example.com"), vm.uiState.value.linkedEmails)
    }

    @Test fun `settings screen restores both toggles from repository on recreation`() {
        val device = TestDeviceSettingsRepository()
        val first = settings(device = device)
        first.onAction(SettingsAction.ToggleDataSaver(true))
        first.onAction(SettingsAction.ToggleAutoSave(true))
        val reopened = settings(device = device)
        assertTrue(reopened.uiState.value.dataSaverEnabled)
        assertTrue(reopened.uiState.value.autoSaveToGallery)
    }

    @Test fun `contacts survive leaving settings and recreation of repository and screen`() {
        val disk = TestContactPreferences()
        val auth = ContactAuth()
        val first = settings(auth = auth, contacts = PersistentAccountContactRepository(disk))
        first.onAction(SettingsAction.SaveRecoveryPhone("0912 345 678"))
        first.onAction(SettingsAction.AddLinkedEmail(" person+qa@example.com "))
        first.onAction(SettingsAction.NavigateBack)

        val reopened = settings(auth = auth, contacts = PersistentAccountContactRepository(disk))
        assertEquals("0912345678", reopened.uiState.value.recoveryPhone)
        assertEquals(listOf("person+qa@example.com"), reopened.uiState.value.linkedEmails)
        reopened.onAction(SettingsAction.AddLinkedEmail("PERSON+QA@example.com"))
        assertEquals(listOf("person+qa@example.com"), PersistentAccountContactRepository(disk).read("account-a").linkedEmails)
    }

    @Test fun `switching accounts clears visible contacts and never mixes saved contacts`() {
        val disk = TestContactPreferences()
        val auth = ContactAuth()
        val vm = settings(auth = auth, contacts = PersistentAccountContactRepository(disk))
        vm.onAction(SettingsAction.SaveRecoveryPhone("0912345678"))
        vm.onAction(SettingsAction.AddLinkedEmail("first@example.com"))
        vm.onAction(SettingsAction.OpenRecoveryPhoneDialog)
        vm.onAction(SettingsAction.OpenLinkedEmailsDialog)

        auth.currentUser.value = User("account-b", "other", "Other", null)
        assertNull(vm.uiState.value.recoveryPhone)
        assertTrue(vm.uiState.value.linkedEmails.isEmpty())
        assertFalse(vm.uiState.value.showRecoveryPhoneDialog)
        assertFalse(vm.uiState.value.showLinkedEmailsDialog)
        vm.onAction(SettingsAction.SaveRecoveryPhone("0987654321"))
        vm.onAction(SettingsAction.AddLinkedEmail("second@example.com"))

        auth.currentUser.value = null
        assertNull(vm.uiState.value.recoveryPhone)
        assertTrue(vm.uiState.value.linkedEmails.isEmpty())
        auth.currentUser.value = User("account-a", "first", "First", null)
        assertEquals("0912345678", vm.uiState.value.recoveryPhone)
        assertEquals(listOf("first@example.com"), vm.uiState.value.linkedEmails)
        assertEquals("0987654321", PersistentAccountContactRepository(disk).read("account-b").recoveryPhone)
    }

    @Test fun `removing contacts persists and preserves other emails`() {
        val disk = TestContactPreferences()
        val auth = ContactAuth()
        val vm = settings(auth = auth, contacts = PersistentAccountContactRepository(disk))
        vm.onAction(SettingsAction.SaveRecoveryPhone("0912345678"))
        vm.onAction(SettingsAction.AddLinkedEmail("first@example.com"))
        vm.onAction(SettingsAction.AddLinkedEmail("second@example.com"))
        vm.onAction(SettingsAction.RemoveRecoveryPhone)
        vm.onAction(SettingsAction.RemoveLinkedEmail("FIRST@example.com"))

        val reopened = settings(auth = auth, contacts = PersistentAccountContactRepository(disk))
        assertNull(reopened.uiState.value.recoveryPhone)
        assertEquals(listOf("second@example.com"), reopened.uiState.value.linkedEmails)
        reopened.onAction(SettingsAction.RemoveLinkedEmail("second@example.com"))
        assertEquals(AccountContactData(), PersistentAccountContactRepository(disk).read("account-a"))
    }

    @Test fun `failed contact write keeps original data and dialog for retry`() = runTest(dispatcher) {
        val disk = TestContactPreferences()
        val vm = settings(auth = ContactAuth(), contacts = PersistentAccountContactRepository(disk))
        vm.onAction(SettingsAction.SaveRecoveryPhone("0912345678"))
        vm.onAction(SettingsAction.OpenRecoveryPhoneDialog)
        disk.failWrites = true
        vm.onAction(SettingsAction.SaveRecoveryPhone("0987654321"))
        assertEquals("0912345678", vm.uiState.value.recoveryPhone)
        assertTrue(vm.uiState.value.showRecoveryPhoneDialog)
        assertTrue(vm.effects.first() is SettingsEffect.ShowMessage)
        vm.onAction(SettingsAction.OpenLinkedEmailsDialog)
        vm.onAction(SettingsAction.AddLinkedEmail("first@example.com"))
        assertTrue(vm.uiState.value.linkedEmails.isEmpty())
        assertTrue(vm.uiState.value.showLinkedEmailsDialog)
        assertTrue(vm.effects.first() is SettingsEffect.ShowMessage)
        disk.failWrites = false
        vm.onAction(SettingsAction.SaveRecoveryPhone("0987654321"))
        assertEquals("0987654321", vm.uiState.value.recoveryPhone)
        assertFalse(vm.uiState.value.showRecoveryPhoneDialog)
    }

    @Test fun `signed out actions cannot persist contact data`() = runTest(dispatcher) {
        val disk = TestContactPreferences()
        val auth = ContactAuth().apply { currentUser.value = null }
        val vm = settings(auth = auth, contacts = PersistentAccountContactRepository(disk))
        vm.onAction(SettingsAction.SaveRecoveryPhone("0912345678"))
        assertEquals(SettingsEffect.ShowMessage("Bạn cần đăng nhập lại"), vm.effects.first())
        vm.onAction(SettingsAction.AddLinkedEmail("first@example.com"))
        assertEquals(SettingsEffect.ShowMessage("Bạn cần đăng nhập lại"), vm.effects.first())
        assertEquals(AccountContactData(), PersistentAccountContactRepository(disk).read("account-a"))
    }

    @Test fun `successful password change announces success and forwards current password`() = runTest(dispatcher) {
        val auth = PasswordAuth()
        val vm = settings(auth)
        vm.onAction(SettingsAction.OpenChangePasswordDialog)
        vm.onAction(SettingsAction.ConfirmChangePassword("old-password", "NewPass123"))
        assertEquals("old-password" to "NewPass123", auth.received)
        assertFalse(vm.uiState.value.showChangePasswordDialog)
        assertFalse(vm.uiState.value.isChangingPassword)
        assertEquals(SettingsEffect.ShowMessage("Đã đổi mật khẩu thành công"), vm.effects.first())
    }

    @Test fun `password failure stays in dialog and can be retried`() = runTest(dispatcher) {
        val auth = PasswordAuth().apply { result = Result.failure(IllegalArgumentException("Mật khẩu hiện tại không đúng")) }
        val vm = settings(auth)
        vm.onAction(SettingsAction.OpenChangePasswordDialog)
        vm.onAction(SettingsAction.ConfirmChangePassword("wrong", "NewPass123"))
        assertTrue(vm.uiState.value.showChangePasswordDialog)
        assertFalse(vm.uiState.value.isChangingPassword)
        assertEquals("Mật khẩu hiện tại không đúng", vm.uiState.value.changePasswordError)
        auth.result = Result.success(Unit)
        vm.onAction(SettingsAction.ConfirmChangePassword("correct", "NewPass123"))
        assertFalse(vm.uiState.value.showChangePasswordDialog)
        assertNull(vm.uiState.value.changePasswordError)
        assertEquals(2, auth.calls)
        assertEquals(SettingsEffect.ShowMessage("Đã đổi mật khẩu thành công"), vm.effects.first())
    }

    @Test fun `pending password change rejects duplicate submission and dismissal`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<Unit>>()
        val auth = PasswordAuth().apply { this.gate = gate }
        val vm = settings(auth)
        vm.onAction(SettingsAction.OpenChangePasswordDialog)
        vm.onAction(SettingsAction.ConfirmChangePassword("current", "NewPass123"))
        vm.onAction(SettingsAction.ConfirmChangePassword("current", "NewPass123"))
        vm.onAction(SettingsAction.DismissChangePasswordDialog)
        assertTrue(vm.uiState.value.isChangingPassword)
        assertTrue(vm.uiState.value.showChangePasswordDialog)
        assertEquals(1, auth.calls)
        gate.complete(Result.success(Unit))
        assertFalse(vm.uiState.value.isChangingPassword)
    }

    @Test fun `invalid password action never reaches authentication backend`() {
        val auth = PasswordAuth()
        val vm = settings(auth)
        vm.onAction(SettingsAction.OpenChangePasswordDialog)
        vm.onAction(SettingsAction.ConfirmChangePassword("", "NewPass123"))
        assertEquals(0, auth.calls)
        assertNotNull(vm.uiState.value.changePasswordError)
        vm.onAction(SettingsAction.ConfirmChangePassword("current", "weak"))
        assertEquals(0, auth.calls)
        assertTrue(vm.uiState.value.showChangePasswordDialog)
    }

    @Test fun `cache clear reports actual success or failure`() = runTest(dispatcher) {
        val cache = TestAppCacheRepository()
        val vm = settings(cache = cache)
        vm.onAction(SettingsAction.ClearCache)
        assertEquals(1, cache.calls)
        assertFalse(vm.uiState.value.isClearingCache)
        assertEquals(SettingsEffect.ShowMessage("Đã xoá bộ nhớ đệm ảnh"), vm.effects.first())
        cache.result = Result.failure(IllegalStateException("Không xoá được bộ nhớ đệm ảnh"))
        vm.onAction(SettingsAction.ClearCache)
        assertEquals(SettingsEffect.ShowMessage("Không xoá được bộ nhớ đệm ảnh"), vm.effects.first())
        assertFalse(vm.uiState.value.isClearingCache)
    }

    private class PasswordAuth : AuthRepository by FakeAuthRepository() {
        var calls = 0
        var received: Pair<String, String>? = null
        var result: Result<Unit> = Result.success(Unit)
        var gate: CompletableDeferred<Result<Unit>>? = null
        override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
            calls++
            received = currentPassword to newPassword
            return gate?.await() ?: result
        }
    }

    private class ContactAuth : AuthRepository by FakeAuthRepository() {
        override val currentUser = MutableStateFlow<User?>(User("account-a", "first", "First", null))
    }
}
