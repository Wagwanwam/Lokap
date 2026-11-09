package vn.nhom2.lokap.feature

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.data.fake.AuthStoreException
import vn.nhom2.lokap.data.fake.FakeAuthStore
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.feature.auth.AuthValidators
import vn.nhom2.lokap.feature.auth.LoginAction
import vn.nhom2.lokap.feature.auth.LoginRoute
import vn.nhom2.lokap.feature.auth.LoginViewModel
import vn.nhom2.lokap.feature.auth.RegisterAction
import vn.nhom2.lokap.feature.auth.RegisterViewModel
import vn.nhom2.lokap.feature.auth.setup.SetupProfileAction
import vn.nhom2.lokap.feature.auth.setup.SetupProfileRoute
import vn.nhom2.lokap.feature.auth.setup.SetupProfileViewModel
import vn.nhom2.lokap.feature.auth.setup.UsernameAvailability
import vn.nhom2.lokap.feature.launch.startDestinationFor
import vn.nhom2.lokap.feature.onboarding.OnboardingAction
import vn.nhom2.lokap.feature.onboarding.OnboardingRoute
import vn.nhom2.lokap.feature.onboarding.OnboardingViewModel
import vn.nhom2.lokap.feature.onboarding.PermissionStatus

@OptIn(ExperimentalCoroutinesApi::class)
class AuthLogicTest {

    private lateinit var dispatcher: TestDispatcher

    @Before
    fun setUp() {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        resetAuth()
    }

    @After
    fun tearDown() {
        resetAuth()
        Dispatchers.resetMain()
    }

    // FakeAuthStore là object dùng chung: trả về trạng thái ban đầu sau mỗi test
    private fun resetAuth() {
        FakeAuthStore.simulateOffline = false
        FakeAuthStore.currentUser.value = null
        FakeAuthStore.onboardingDone.value = false
    }

    private fun uniqueEmail() = "u${System.nanoTime()}@lokap.vn"

    // ---------- AuthValidators ----------

    @Test
    fun `email`() {
        assertNull(AuthValidators.validateEmail("  ban@lokap.vn "))
        assertNotNull(AuthValidators.validateEmail(""))
        assertNotNull(AuthValidators.validateEmail("ban@lokap"))
        assertNotNull(AuthValidators.validateEmail("ban lokap.vn"))
    }

    @Test
    fun `mat khau it nhat 8 ky tu co ca chu va so`() {
        assertNull(AuthValidators.validatePassword("lokap123"))
        assertNotNull(AuthValidators.validatePassword("lok123"))      // ngắn
        assertNotNull(AuthValidators.validatePassword("lokaplokap"))  // không số
        assertNotNull(AuthValidators.validatePassword("12345678"))    // không chữ
        assertNull(AuthValidators.validateConfirmPassword("lokap123", "lokap123"))
        assertNotNull(AuthValidators.validateConfirmPassword("lokap123", "lokap124"))
    }

    @Test
    fun `username 3 den 20 ky tu a-z 0-9 cham gach duoi`() {
        assertNull(AuthValidators.validateUsernameFormat("minh.khoi_99"))
        assertNotNull(AuthValidators.validateUsernameFormat("ab"))
        assertNotNull(AuthValidators.validateUsernameFormat("a".repeat(21)))
        assertNotNull(AuthValidators.validateUsernameFormat("minh-khoi"))
        assertNotNull(AuthValidators.validateUsernameFormat("MinhKhoi"))
    }

    @Test
    fun `ten hien thi 2 den 30 ky tu sau trim`() {
        assertNull(AuthValidators.validateDisplayName("  An  "))
        assertNotNull(AuthValidators.validateDisplayName("  A  "))
        assertNotNull(AuthValidators.validateDisplayName("a".repeat(31)))
    }

    @Test
    fun `nam sinh tu 13 tuoi tro len va hop le`() {
        val currentYear = java.time.LocalDate.now().year
        assertNull(AuthValidators.validateBirthYear(currentYear - 15))
        assertNotNull(AuthValidators.validateBirthYear(currentYear - 10)) // < 13 tuổi
        assertNotNull(AuthValidators.validateBirthYear(currentYear + 1))
        assertNotNull(AuthValidators.validateBirthYear(1800))
    }

    @Test
    fun `bio toi da 60 ky tu`() {
        assertNull(AuthValidators.validateBio("Xin chào Lokap!"))
        assertNotNull(AuthValidators.validateBio("a".repeat(61)))
    }

    // ---------- FakeAuthStore ----------

    @Test
    fun `dang nhap sai chi bao chung, dung thi vao duoc`() = runTest(dispatcher) {
        val wrongPass = FakeAuthStore.login("demo@lokap.vn", "sai12345").exceptionOrNull()
        val noAccount = FakeAuthStore.login("khongco@lokap.vn", "lokap123").exceptionOrNull()
        assertTrue(wrongPass is AuthStoreException.InvalidCredentials)
        assertEquals(wrongPass!!.message, noAccount!!.message) // không nói sai ô nào
        assertEquals(SampleData.currentUser, FakeAuthStore.login("DEMO@lokap.vn ", "lokap123").getOrNull())
    }

    @Test
    fun `email da dang ky thi bao trung`() = runTest(dispatcher) {
        assertTrue(FakeAuthStore.register("taken@lokap.vn", "lokap123").exceptionOrNull() is AuthStoreException.EmailAlreadyTaken)
        val email = uniqueEmail()
        assertTrue(FakeAuthStore.register(email, "lokap123").isSuccess)
        assertTrue(FakeAuthStore.register(email, "lokap123").exceptionOrNull() is AuthStoreException.EmailAlreadyTaken)
    }

    @Test
    fun `username lokap va username trong SampleData da co nguoi dung`() = runTest(dispatcher) {
        assertEquals(false, FakeAuthStore.isUsernameAvailable("lokap").getOrNull())
        assertEquals(false, FakeAuthStore.isUsernameAvailable(SampleData.users[1].username.uppercase()).getOrNull())
        assertTrue(FakeAuthStore.completeProfile("lokap", "Ai Do").exceptionOrNull() is AuthStoreException.UsernameAlreadyTaken)
    }

    @Test
    fun `mat mang thi bao loi mang va quen mat khau luon bao da gui`() = runTest(dispatcher) {
        assertTrue(FakeAuthStore.login("offline@lokap.vn", "lokap123").exceptionOrNull() is AuthStoreException.NetworkError)
        assertTrue(FakeAuthStore.resetPassword("khongtontai@lokap.vn").isSuccess)
        FakeAuthStore.simulateOffline = true
        assertTrue(FakeAuthStore.login("demo@lokap.vn", "lokap123").exceptionOrNull() is AuthStoreException.NetworkError)
    }

    // ---------- Màn Đăng nhập ----------

    @Test
    fun `dang nhap khong ap luat mat khau manh, bam 2 lan chi goi 1 lan`() = runTest(dispatcher) {
        val vm = LoginViewModel()
        vm.onAction(LoginAction.EmailChanged("demo@lokap.vn"))
        vm.onAction(LoginAction.PasswordChanged("abc")) // mật khẩu cũ/ngắn vẫn được gửi đi
        vm.onAction(LoginAction.PasswordFocusLost)
        assertNull(vm.uiState.value.passwordError)

        vm.onAction(LoginAction.SubmitClicked)
        assertTrue(vm.uiState.value.isLoading)
        vm.onAction(LoginAction.SubmitClicked) // bị chặn vì đang gửi
        advanceUntilIdle()
        assertEquals("Email hoặc mật khẩu không đúng", vm.uiState.value.serverError)
    }

    // ---------- Thiết lập hồ sơ ----------

    @Test
    fun `username tu chuyen chu thuong, loi dinh dang chi hien sau khi roi o`() = runTest(dispatcher) {
        val vm = SetupProfileViewModel()
        vm.onAction(SetupProfileAction.UsernameChanged("AB"))
        assertEquals("ab", vm.uiState.value.username)
        assertNull(vm.uiState.value.usernameError)
        vm.onAction(SetupProfileAction.UsernameFocusLost)
        assertNotNull(vm.uiState.value.usernameError)
    }

    @Test
    fun `go tiep thi huy lan kiem tra cu, ket qua cu khong ghi de`() = runTest(dispatcher) {
        val vm = SetupProfileViewModel()
        vm.onAction(SetupProfileAction.UsernameChanged("lokap"))     // đã có người dùng
        advanceTimeBy(200)
        vm.onAction(SetupProfileAction.UsernameChanged("lokap.moi")) // gõ tiếp trước khi hết 400ms
        advanceTimeBy(399)
        assertEquals(UsernameAvailability.CHECKING, vm.uiState.value.usernameAvailability)
        advanceUntilIdle()
        assertEquals(UsernameAvailability.AVAILABLE, vm.uiState.value.usernameAvailability)
        assertNull(vm.uiState.value.usernameError)
    }

    // ---------- Quyền ở Onboarding ----------

    @Test
    fun `tu choi lan dau van xin lai duoc, tu choi han moi mo Cai dat`() {
        val vm = OnboardingViewModel()
        vm.onAction(OnboardingAction.CameraPermissionResult(granted = false, shouldShowRationale = true))
        assertEquals(PermissionStatus.NOT_GRANTED, vm.uiState.value.cameraStatus)
        vm.onAction(OnboardingAction.CameraPermissionResult(granted = false, shouldShowRationale = false))
        assertEquals(PermissionStatus.DENIED, vm.uiState.value.cameraStatus)

        // Đóng hộp thoại lần đầu (không rationale) chưa phải từ chối hẳn
        vm.onAction(OnboardingAction.NotificationPermissionResult(granted = false, shouldShowRationale = false))
        assertEquals(PermissionStatus.NOT_GRANTED, vm.uiState.value.notificationStatus)
    }

    // ---------- Màn Đăng ký Bước 1 ----------

    @Test
    fun `dang ky moi vao khong duoc co loi, roi o khi chua nhap gi van khong bao loi`() = runTest(dispatcher) {
        val vm = RegisterViewModel()
        assertNull(vm.uiState.value.emailError)
        assertNull(vm.uiState.value.passwordError)
        assertNull(vm.uiState.value.confirmPasswordError)
        assertNull(vm.uiState.value.termsError)

        // Rời ô khi chưa nhập gì: không được đỏ loét
        vm.onAction(RegisterAction.EmailFocusLost)
        vm.onAction(RegisterAction.PasswordFocusLost)
        vm.onAction(RegisterAction.ConfirmPasswordFocusLost)
        assertNull(vm.uiState.value.emailError)
        assertNull(vm.uiState.value.passwordError)
        assertNull(vm.uiState.value.confirmPasswordError)

        // Nhập email sai định dạng rồi rời ô: mới báo lỗi
        vm.onAction(RegisterAction.EmailChanged("khongphaiemail"))
        vm.onAction(RegisterAction.EmailFocusLost)
        assertNotNull(vm.uiState.value.emailError)

        // Bấm tiếp tục khi chưa đủ dữ liệu: hiện toàn bộ lỗi bắt buộc
        val freshVm = RegisterViewModel()
        freshVm.onAction(RegisterAction.SubmitClicked)
        assertNotNull(freshVm.uiState.value.emailError)
        assertNotNull(freshVm.uiState.value.passwordError)
        assertNotNull(freshVm.uiState.value.confirmPasswordError)
        assertNotNull(freshVm.uiState.value.termsError)
    }

    // ---------- Điểm bắt đầu ----------

    @Test
    fun `diem bat dau theo trang thai luc mo app`() {
        assertEquals(OnboardingRoute, startDestinationFor())
        FakeAuthStore.onboardingDone.value = true
        assertEquals(LoginRoute, startDestinationFor())
        FakeAuthStore.currentUser.value = SampleData.currentUser.copy(username = "")
        assertEquals(SetupProfileRoute, startDestinationFor())
        FakeAuthStore.currentUser.value = SampleData.currentUser
        assertEquals(Route.Feed, startDestinationFor())
    }
}
