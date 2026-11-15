package vn.nhom2.lokap.feature.camera

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import vn.nhom2.lokap.data.fake.FakeAuthRepository
import vn.nhom2.lokap.data.fake.FakeSocialRepository
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.data.fake.SampleSocialData

/**
 * Chuyển trạng thái quyền camera trong CameraViewModel.
 * Tham số: (isGranted, rationale SAU khi hỏi, rationale TRƯỚC khi hỏi) — đúng như Android trả về.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CameraPermissionTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        SampleSocialData.resetAccountForDemo()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun camera() = CameraViewModel(FakeAuthRepository(), FakeSocialRepository(), vn.nhom2.lokap.feature.ContentTestRepository(), vn.nhom2.lokap.core.image.ImageProcessor())

    private fun CameraViewModel.result(granted: Boolean, after: Boolean, before: Boolean) =
        onAction(CameraAction.OnPermissionResult(granted, after, before))

    private val CameraViewModel.permission get() = uiState.value.permission

    @Test
    fun `cấp quyền thì GRANTED`() {
        val vm = camera()
        vm.result(granted = true, after = false, before = false)
        assertEquals(CameraPermissionState.GRANTED, vm.permission)
    }

    @Test
    fun `lần đầu đóng hộp thoại bằng Back thì chưa phải vĩnh viễn`() {
        val vm = camera()
        vm.result(granted = false, after = false, before = false)
        assertEquals(CameraPermissionState.DENIED, vm.permission)
    }

    @Test
    fun `từ chối lần 1 thì DENIED, lần 2 thì PERMANENTLY_DENIED`() {
        val vm = camera()
        vm.result(granted = false, after = true, before = false)
        assertEquals(CameraPermissionState.DENIED, vm.permission)

        vm.result(granted = false, after = false, before = true)
        assertEquals(CameraPermissionState.PERMANENTLY_DENIED, vm.permission)
    }

    @Test
    fun `Back ở hộp thoại lần 2 vẫn còn DENIED`() {
        val vm = camera()
        vm.result(granted = false, after = true, before = false)
        vm.result(granted = false, after = true, before = true)
        assertEquals(CameraPermissionState.DENIED, vm.permission)
    }

    @Test
    fun `mở lại app khi đã bị chặn vĩnh viễn thì bấm Cho phép một lần sẽ ra Mở Cài đặt`() {
        val vm = camera() // process mới: NOT_REQUESTED
        vm.result(granted = false, after = false, before = false) // hệ thống từ chối ngay, không hiện hộp thoại
        assertEquals(CameraPermissionState.DENIED, vm.permission)

        vm.result(granted = false, after = false, before = false) // bấm "Cho phép"
        assertEquals(CameraPermissionState.PERMANENTLY_DENIED, vm.permission)
    }

    @Test
    fun `avatar và số bạn trên màn Camera lấy từ Repository`() {
        val vm = camera()
        assertEquals(SampleData.currentUser, vm.uiState.value.me)
        assertEquals(SampleData.users.size - 1, vm.uiState.value.friendCount)
    }
}
