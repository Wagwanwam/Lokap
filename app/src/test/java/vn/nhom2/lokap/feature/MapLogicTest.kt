package vn.nhom2.lokap.feature

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.MapBubbleType
import vn.nhom2.lokap.feature.landmark.LandmarkViewModel
import vn.nhom2.lokap.feature.map.MapAction
import vn.nhom2.lokap.feature.map.MapCameraState
import vn.nhom2.lokap.feature.map.MapViewModel
import vn.nhom2.lokap.feature.region.RegionAlbumAction
import vn.nhom2.lokap.feature.region.RegionAlbumViewModel
import vn.nhom2.lokap.feature.region.RegionAudienceFilter

@OptIn(ExperimentalCoroutinesApi::class)
class MapLogicTest {

    private lateinit var dispatcher: TestDispatcher

    @Before
    fun setUp() {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun MapViewModel.bubblesAt(zoom: Float) =
        apply { onAction(MapAction.OnCameraChanged(MapCameraState(16.05, 108.2, zoom))) }
            .uiState.value.visibleBubbles

    // ---------- Bản đồ: bong bóng theo mức zoom ----------

    @Test
    fun `zoom duoi 8 chi co bong bong tinh, khong trung`() {
        val bubbles = MapViewModel().bubblesAt(6f)
        assertTrue(bubbles.isNotEmpty())
        assertTrue(bubbles.all { it.type == MapBubbleType.PROVINCE })
        assertEquals("Trùng tỉnh: ${bubbles.map { it.code }}", bubbles.size, bubbles.distinctBy { it.code }.size)
    }

    @Test
    fun `zoom 8 den 12 la phuong va dung mot bong bong Khap tinh moi tinh`() {
        for (zoom in listOf(8f, 10f, 12.9f)) {
            val bubbles = MapViewModel().bubblesAt(zoom)
            assertTrue(bubbles.all { it.type == MapBubbleType.WARD || it.type == MapBubbleType.PROVINCE_WIDE })
            assertTrue(bubbles.any { it.type == MapBubbleType.WARD })
            val wide = bubbles.filter { it.type == MapBubbleType.PROVINCE_WIDE }
            assertEquals("Trùng \"Khắp tỉnh\": ${wide.map { it.label }}", wide.size, wide.distinctBy { it.code }.size)
            assertEquals(1, wide.count { it.code == "da-nang" })
            assertTrue("Nhãn phải là \"Khắp <tỉnh>\": ${wide.map { it.label }}", wide.all { it.label.startsWith("Khắp ") })
        }
    }

    @Test
    fun `zoom tu 13 chi co ghim dia diem`() {
        val bubbles = MapViewModel().bubblesAt(13f)
        assertTrue(bubbles.isNotEmpty())
        assertTrue(bubbles.all { it.type == MapBubbleType.PLACE })
    }

    @Test
    fun `cham bong bong tinh thi zoom sang muc phuong`() {
        val vm = MapViewModel()
        val province = vm.bubblesAt(6f).first { it.code == "da-nang" }
        vm.onAction(MapAction.OnBubbleClick(province))
        assertTrue(vm.uiState.value.camera.zoom >= 8f)
        assertTrue(vm.uiState.value.visibleBubbles.all { it.type != MapBubbleType.PROVINCE })
    }

    // ---------- Album khu vực ----------

    private fun album(level: String, code: String) =
        RegionAlbumViewModel(SavedStateHandle(mapOf("level" to level, "code" to code)))

    @Test
    fun `album tinh chi co bai dung tinh, khong co bai muc Viet Nam, khong trung id`() = runTest(dispatcher) {
        val vm = album("PROVINCE", "da-nang")
        advanceUntilIdle()
        // Tab mặc định + tab Public
        for (filter in RegionAudienceFilter.entries) {
            vm.onAction(RegionAlbumAction.OnFilterSelected(filter))
            val posts = vm.uiState.value.displayedPosts
            assertTrue(posts.all { it.location.provinceCode == "da-nang" })
            assertTrue(posts.none { it.location.level == LocationLevel.COUNTRY })
            assertEquals(posts.size, posts.distinctBy { it.id }.size)
            if (filter == RegionAudienceFilter.PUBLIC) assertTrue(posts.all { it.audience == Audience.PUBLIC })
        }
    }

    @Test
    fun `album khu vuc khong co bai thi trong, khong lay bai noi khac`() = runTest(dispatcher) {
        val vm = album("WARD", "hn-hang-bac")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.displayedPosts.isEmpty())
    }

    // ---------- Địa danh ----------

    @Test
    fun `dia danh chi hien bai gan dung dia danh`() = runTest(dispatcher) {
        val vm = LandmarkViewModel(SavedStateHandle(mapOf("id" to "lm_cau_rong")))
        advanceUntilIdle()
        val posts = vm.uiState.value.posts
        assertTrue(posts.isNotEmpty())
        assertTrue(posts.all { it.location.landmarkId == "lm_cau_rong" })
    }

    @Test
    fun `dia danh khong ton tai thi bao loi, khong mo dia danh khac`() = runTest(dispatcher) {
        val vm = LandmarkViewModel(SavedStateHandle(mapOf("id" to "lm_khong_co")))
        advanceUntilIdle()
        assertNull(vm.uiState.value.landmark)
        assertTrue(vm.uiState.value.errorMessage != null)
    }

    @Test
    fun `SampleData co bai muc Viet Nam de test co y nghia`() {
        assertTrue(SampleData.posts.any { it.location.level == LocationLevel.COUNTRY })
    }
}
