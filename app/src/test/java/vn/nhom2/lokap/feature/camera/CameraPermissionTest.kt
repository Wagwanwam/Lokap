package vn.nhom2.lokap.feature.camera

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Chuyển trạng thái quyền camera trong CameraViewModel.
 * Tham số: (isGranted, rationale SAU khi hỏi, rationale TRƯỚC khi hỏi) — đúng như Android trả về.
 */
class CameraPermissionTest {

    private fun CameraViewModel.result(granted: Boolean, after: Boolean, before: Boolean) =
        onAction(CameraAction.OnPermissionResult(granted, after, before))

    private val CameraViewModel.permission get() = uiState.value.permission

    @Test
    fun `cấp quyền thì GRANTED`() {
        val vm = CameraViewModel()
        vm.result(granted = true, after = false, before = false)
        assertEquals(CameraPermissionState.GRANTED, vm.permission)
    }

    @Test
    fun `lần đầu đóng hộp thoại bằng Back thì chưa phải vĩnh viễn`() {
        val vm = CameraViewModel()
        vm.result(granted = false, after = false, before = false)
        assertEquals(CameraPermissionState.DENIED, vm.permission)
    }

    @Test
    fun `từ chối lần 1 thì DENIED, lần 2 thì PERMANENTLY_DENIED`() {
        val vm = CameraViewModel()
        vm.result(granted = false, after = true, before = false)
        assertEquals(CameraPermissionState.DENIED, vm.permission)

        vm.result(granted = false, after = false, before = true)
        assertEquals(CameraPermissionState.PERMANENTLY_DENIED, vm.permission)
    }

    @Test
    fun `Back ở hộp thoại lần 2 vẫn còn DENIED`() {
        val vm = CameraViewModel()
        vm.result(granted = false, after = true, before = false)
        vm.result(granted = false, after = true, before = true)
        assertEquals(CameraPermissionState.DENIED, vm.permission)
    }

    @Test
    fun `mở lại app khi đã bị chặn vĩnh viễn thì bấm Cho phép một lần sẽ ra Mở Cài đặt`() {
        val vm = CameraViewModel() // process mới: NOT_REQUESTED
        vm.result(granted = false, after = false, before = false) // hệ thống từ chối ngay, không hiện hộp thoại
        assertEquals(CameraPermissionState.DENIED, vm.permission)

        vm.result(granted = false, after = false, before = false) // bấm "Cho phép"
        assertEquals(CameraPermissionState.PERMANENTLY_DENIED, vm.permission)
    }
}
