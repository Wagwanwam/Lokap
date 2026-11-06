package vn.nhom2.lokap.feature

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
import vn.nhom2.lokap.feature.auth.EmailAvailability
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

    // Báo trùng lúc đang gõ phải cùng câu với lỗi register() trả về khi bấm Tạo tài khoản
    private val emailTakenMessage = AuthStoreException.EmailAlreadyTaken().message

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

    private fun fillValidPasswordAndTerms(vm: RegisterViewModel) {
        vm.onAction(RegisterAction.PasswordChanged("lokap123"))
        vm.onAction(RegisterAction.ConfirmPasswordChanged("lokap123"))
        vm.onAction(RegisterAction.AcceptedTermsChanged(true))
    }

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
    fun `kiem tra email truoc khi dang ky bao trung, email moi dung duoc, mat mang thi loi mang`() = runTest(dispatcher) {
        assertEquals(false, FakeAuthStore.isEmailAvailable("taken@lokap.vn").getOrNull())
        assertEquals(false, FakeAuthStore.isEmailAvailable("demo@lokap.vn").getOrNull())
        val email = uniqueEmail()
        assertEquals(true, FakeAuthStore.isEmailAvailable(email).getOrNull())
        FakeAuthStore.register(email, "lokap123")
        assertEquals(false, FakeAuthStore.isEmailAvailable(email).getOrNull())

        assertTrue(FakeAuthStore.isEmailAvailable(" OFFLINE@lokap.vn").exceptionOrNull() is AuthStoreException.NetworkError)
        FakeAuthStore.simulateOffline = true
        assertTrue(FakeAuthStore.isEmailAvailable(uniqueEmail()).exceptionOrNull() is AuthStoreException.NetworkError)
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

    // ---------- Màn Đăng ký ----------

    @Test
    fun `go email da dang ky thi bao trung ngay, khong can roi o`() = runTest(dispatcher) {
        val vm = RegisterViewModel()
        vm.onAction(RegisterAction.EmailChanged("taken@lokap.vn"))
        assertEquals(EmailAvailability.CHECKING, vm.uiState.value.emailAvailability)
        assertNull(vm.uiState.value.emailError)
        advanceUntilIdle()
        assertEquals(EmailAvailability.TAKEN, vm.uiState.value.emailAvailability)
        assertEquals(emailTakenMessage, vm.uiState.value.emailError)
        assertFalse(vm.uiState.value.emailTouched)
    }

    @Test
    fun `email trung van bi bat khi co khoang trang va chu hoa`() = runTest(dispatcher) {
        val vm = RegisterViewModel()
        vm.onAction(RegisterAction.EmailChanged("  TAKEN@Lokap.vn "))
        advanceUntilIdle()
        assertEquals("  TAKEN@Lokap.vn ", vm.uiState.value.email) // giữ nguyên chữ người dùng gõ
        assertEquals(EmailAvailability.TAKEN, vm.uiState.value.emailAvailability)
        assertEquals(emailTakenMessage, vm.uiState.value.emailError)
    }

    @Test
    fun `email chua ai dung thi bao dung duoc, khong loi`() = runTest(dispatcher) {
        val vm = RegisterViewModel()
        vm.onAction(RegisterAction.EmailChanged(uniqueEmail()))
        advanceUntilIdle()
        assertEquals(EmailAvailability.AVAILABLE, vm.uiState.value.emailAvailability)
        assertNull(vm.uiState.value.emailError)
    }

    @Test
    fun `go tiep email thi huy lan kiem tra cu, ket qua cu khong ghi de`() = runTest(dispatcher) {
        val vm = RegisterViewModel()
        vm.onAction(RegisterAction.EmailChanged("taken@lokap.vn"))
        advanceTimeBy(200)
        vm.onAction(RegisterAction.EmailChanged(uniqueEmail())) // gõ tiếp trước khi hết 400ms
        advanceTimeBy(500) // đã qua lúc kết quả cũ về (650ms), kết quả mới chưa về (850ms)
        assertEquals(EmailAvailability.CHECKING, vm.uiState.value.emailAvailability)
        assertNull(vm.uiState.value.emailError)
        advanceUntilIdle()
        assertEquals(EmailAvailability.AVAILABLE, vm.uiState.value.emailAvailability)
        assertNull(vm.uiState.value.emailError)
    }

    @Test
    fun `email sai dinh dang thi khong kiem tra trung, loi chi hien sau khi roi o`() = runTest(dispatcher) {
        val vm = RegisterViewModel()
        vm.onAction(RegisterAction.EmailChanged("ban@lokap"))
        advanceUntilIdle()
        assertEquals(EmailAvailability.IDLE, vm.uiState.value.emailAvailability)
        assertEquals(0L, currentTime) // không hẹn lần kiểm tra nào
        assertNull(vm.uiState.value.emailError)
        vm.onAction(RegisterAction.EmailFocusLost)
        assertNotNull(vm.uiState.value.emailError)
    }

    @Test
    fun `roi o email khong xoa loi email da dang ky`() = runTest(dispatcher) {
        val vm = RegisterViewModel()
        vm.onAction(RegisterAction.EmailChanged("taken@lokap.vn"))
        advanceUntilIdle()
        vm.onAction(RegisterAction.EmailFocusLost)
        assertTrue(vm.uiState.value.emailTouched)
        assertEquals(EmailAvailability.TAKEN, vm.uiState.value.emailAvailability)
        assertEquals(emailTakenMessage, vm.uiState.value.emailError)
    }

    @Test
    fun `mat mang khi kiem tra email thi bao mat mang, khong hien loi server`() = runTest(dispatcher) {
        FakeAuthStore.simulateOffline = true
        val vm = RegisterViewModel()
        vm.onAction(RegisterAction.EmailChanged(uniqueEmail()))
        advanceUntilIdle()
        assertEquals(EmailAvailability.IDLE, vm.uiState.value.emailAvailability)
        assertTrue(vm.uiState.value.isOffline)
        assertNull(vm.uiState.value.serverError) // nút Thử lại của serverError gửi cả form
        assertNull(vm.uiState.value.emailError)
    }

    @Test
    fun `email da dang ky thi bam Tao tai khoan khong goi dang ky`() = runTest(dispatcher) {
        val vm = RegisterViewModel()
        fillValidPasswordAndTerms(vm)
        vm.onAction(RegisterAction.EmailChanged("taken@lokap.vn"))
        advanceUntilIdle()
        assertEquals(EmailAvailability.TAKEN, vm.uiState.value.emailAvailability)

        vm.onAction(RegisterAction.SubmitClicked)
        assertFalse(vm.uiState.value.isLoading) // register() không được gọi
        assertTrue(vm.uiState.value.emailTouched)
        assertEquals(emailTakenMessage, vm.uiState.value.emailError)
    }

    @Test
    fun `bam Tao tai khoan luc dang kiem tra thi huy kiem tra, dang ky bao trung thi danh dau trung`() = runTest(dispatcher) {
        val vm = RegisterViewModel()
        fillValidPasswordAndTerms(vm)
        vm.onAction(RegisterAction.EmailChanged("taken@lokap.vn"))
        vm.onAction(RegisterAction.SubmitClicked) // bấm trước khi hết 400ms
        advanceTimeBy(700) // qua lúc lần kiểm tra cũ lẽ ra có kết quả (650ms), register() chưa xong (800ms)
        assertTrue(vm.uiState.value.isLoading)
        assertEquals(EmailAvailability.IDLE, vm.uiState.value.emailAvailability)
        assertNull(vm.uiState.value.emailError)

        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
        assertEquals(EmailAvailability.TAKEN, vm.uiState.value.emailAvailability)
        assertEquals(emailTakenMessage, vm.uiState.value.emailError)
        assertNull(vm.uiState.value.serverError)
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

    // ---------- Điểm bắt đầu ----------

    @Test
    fun `diem bat dau theo trang thai luc mo app`() {
        assertEquals(OnboardingRoute, startDestinationFor())
        FakeAuthStore.onboardingDone.value = true
        assertEquals(LoginRoute, startDestinationFor())
        FakeAuthStore.currentUser.value = SampleData.currentUser.copy(username = "")
        assertEquals(SetupProfileRoute, startDestinationFor())
        FakeAuthStore.currentUser.value = SampleData.currentUser
        assertEquals(Route.Camera, startDestinationFor())
    }
}
