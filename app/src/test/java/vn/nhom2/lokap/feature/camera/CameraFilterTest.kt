package vn.nhom2.lokap.feature.camera

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import vn.nhom2.lokap.core.image.ImageProcessor
import vn.nhom2.lokap.core.image.PhotoColorFilter
import vn.nhom2.lokap.data.fake.FakeAuthRepository
import vn.nhom2.lokap.data.fake.FakeSocialRepository
import vn.nhom2.lokap.data.fake.SampleSocialData
import vn.nhom2.lokap.feature.ContentTestRepository

/** Vòng bộ lọc khi nhấn giữ nút chụp: hình học chọn nút + luồng trạng thái trong CameraViewModel. */
@OptIn(ExperimentalCoroutinesApi::class)
class CameraFilterTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        SampleSocialData.resetAccountForDemo()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun camera() = CameraViewModel(
        FakeAuthRepository(), FakeSocialRepository(), ContentTestRepository(), ImageProcessor()
    ).apply { onAction(CameraAction.OnPermissionResult(isGranted = true, shouldShowRationale = false, shouldShowRationaleBefore = false)) }

    // ---------- Hình học: 8 nút trên nửa vòng trên, trái (Gốc) → phải (Điện ảnh) ----------

    private val activation = 100f
    private val last = FilterRing.filters.lastIndex

    @Test
    fun `ngón tay còn trong nút chụp thì chưa chọn gì`() {
        assertNull(FilterRing.indexAt(dx = 30f, dy = -40f, activationPx = activation))
    }

    @Test
    fun `kéo sang trái là Gốc, sang phải là nút cuối, thẳng lên là giữa vòng`() {
        assertEquals(0, FilterRing.indexAt(dx = -300f, dy = 0f, activationPx = activation))
        assertEquals(last, FilterRing.indexAt(dx = 300f, dy = 0f, activationPx = activation))
        val up = FilterRing.indexAt(dx = 0f, dy = -300f, activationPx = activation)!!
        assertEquals(true, up == last / 2 || up == (last + 1) / 2)
    }

    @Test
    fun `lệch xuống dưới một chút ở hai đầu vẫn chọn nút đầu cuối, kéo hẳn xuống thì bỏ`() {
        assertEquals(0, FilterRing.indexAt(dx = -300f, dy = 100f, activationPx = activation))
        assertEquals(last, FilterRing.indexAt(dx = 300f, dy = 100f, activationPx = activation))
        assertNull(FilterRing.indexAt(dx = 0f, dy = 300f, activationPx = activation))
    }

    @Test
    fun `mỗi nút trên vòng chọn đúng chính nó`() {
        FilterRing.filters.indices.forEach { i ->
            val p = FilterRing.itemOffset(i, radiusPx = 400f)
            assertEquals(i, FilterRing.indexAt(p.x, p.y, activationPx = activation))
        }
    }

    // ---------- Trạng thái ----------

    @Test
    fun `nhấn giữ, lướt rồi thả thì chọn bộ lọc, chưa chụp`() {
        val vm = camera()
        vm.onAction(CameraAction.OnFilterPickerOpen)
        vm.onAction(CameraAction.OnFilterHover(PhotoColorFilter.WARM_GOLDEN))
        assertEquals("Đang lướt: xem thử trên camera", PhotoColorFilter.WARM_GOLDEN, vm.uiState.value.previewFilter)

        vm.onAction(CameraAction.OnFilterPickerRelease)
        val state = vm.uiState.value
        assertEquals(PhotoColorFilter.WARM_GOLDEN, state.selectedFilter)
        assertFalse(state.isFilterPickerOpen)
        assertFalse(state.isCapturing)
    }

    @Test
    fun `thả tay khi ngón tay về giữa thì giữ bộ lọc cũ`() {
        val vm = camera()
        vm.onAction(CameraAction.OnFilterPickerOpen)
        vm.onAction(CameraAction.OnFilterHover(PhotoColorFilter.MONO_CHIC))
        vm.onAction(CameraAction.OnFilterPickerRelease)

        vm.onAction(CameraAction.OnFilterPickerOpen)
        vm.onAction(CameraAction.OnFilterHover(PhotoColorFilter.VIVID))
        vm.onAction(CameraAction.OnFilterHover(null))
        assertEquals(PhotoColorFilter.MONO_CHIC, vm.uiState.value.previewFilter)
        vm.onAction(CameraAction.OnFilterPickerRelease)
        assertEquals(PhotoColorFilter.MONO_CHIC, vm.uiState.value.selectedFilter)
    }

    @Test
    fun `chưa có quyền camera thì không mở vòng`() {
        val vm = CameraViewModel(FakeAuthRepository(), FakeSocialRepository(), ContentTestRepository(), ImageProcessor())
        vm.onAction(CameraAction.OnFilterPickerOpen)
        assertFalse(vm.uiState.value.isFilterPickerOpen)
    }

    @Test
    fun `chụp với ảnh gốc thì sang màn soạn bài với đúng file`() {
        val vm = camera()
        vm.onAction(CameraAction.OnShutterClick)
        vm.onAction(CameraAction.OnPhotoSaved("/tmp/a.jpg"))
        assertFalse(vm.uiState.value.isCapturing)
    }

    @Test
    fun `ma trận bộ lọc đủ 20 số, Gốc là null`() {
        assertNull(PhotoColorFilter.ORIGINAL.matrix)
        PhotoColorFilter.entries.filter { it != PhotoColorFilter.ORIGINAL }.forEach {
            assertEquals(it.name, 20, it.matrix!!.size)
        }
    }
}
