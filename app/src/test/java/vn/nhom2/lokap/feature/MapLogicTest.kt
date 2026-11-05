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
import vn.nhom2.lokap.domain.model.PostSendState
import vn.nhom2.lokap.feature.map.MapAudienceFilter
import vn.nhom2.lokap.feature.landmark.LandmarkViewModel
import vn.nhom2.lokap.feature.map.MapAction
import vn.nhom2.lokap.feature.map.MapCameraState
import vn.nhom2.lokap.feature.map.MapViewModel
import vn.nhom2.lokap.feature.map.MapPlacementCandidate
import vn.nhom2.lokap.feature.map.MapPlacementKind
import vn.nhom2.lokap.feature.map.projectLatLngToPixel
import vn.nhom2.lokap.feature.map.resolveMapPlacement
import androidx.compose.ui.unit.IntRect
import kotlin.math.roundToInt
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
            assertTrue("Nhãn phải là \"Khắp <tỉnh>\": ${wide.map { it.label }}", wide.all { it.label.startsWith("Khắp ") })
        }
    }

    @Test
    fun `so tren bong bong dem tu bai that, khong go cung`() {
        val posts = SampleData.posts.filter {
            it.location.level != LocationLevel.COUNTRY && it.sendState == PostSendState.SENT
        }
        // Mức tỉnh: mọi bài trong tỉnh
        MapViewModel().bubblesAt(6f).forEach { b ->
            assertEquals(b.label, posts.count { it.location.provinceCode == b.code }, b.postCount)
        }
        // Mức phường: bài mức phường + địa điểm; "Khắp tỉnh": chỉ bài người đăng chọn mức tỉnh
        MapViewModel().bubblesAt(10f).forEach { b ->
            val expected = if (b.type == MapBubbleType.WARD) {
                posts.count {
                    it.location.wardCode == b.code &&
                        it.location.level in setOf(LocationLevel.WARD, LocationLevel.PLACE)
                }
            } else {
                posts.count { it.location.provinceCode == b.code && it.location.level == LocationLevel.PROVINCE }
            }
            assertEquals(b.label, expected, b.postCount)
            assertTrue("Bong bóng 0 bài không được vẽ: ${b.label}", b.postCount > 0)
        }
    }

    @Test
    fun `loc Public khong dem bai public dang hen gio`() {
        val vm = MapViewModel()
        vm.onAction(MapAction.OnAudienceFilterChange(MapAudienceFilter.PUBLIC))
        val total = vm.bubblesAt(6f).sumOf { it.postCount }
        val expected = SampleData.posts.count {
            it.location.level != LocationLevel.COUNTRY && it.sendState == PostSendState.SENT &&
                it.audience == Audience.PUBLIC && it.location.provinceCode != null &&
                (it.publicVisibleAt == null || !it.publicVisibleAt!!.isAfter(SampleData.NOW))
        }
        assertEquals(expected, total)
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

    // ---------- Xếp chỗ: địa danh 0 bài nhường bong bóng phường (quyết định 06/11) ----------

    private fun cand(i: Int, kind: MapPlacementKind, posts: Int, x: Int) =
        MapPlacementCandidate(i, IntRect(x, 100, x + 100, 200), kind, posts)

    @Test
    fun `dia danh 0 bai luon nhuong cho bong bong`() {
        val emptyLm = cand(0, MapPlacementKind.LANDMARK_EMPTY, 0, 0)
        val ward = cand(1, MapPlacementKind.BUBBLE, 3, 50)          // đè lên emptyLm
        for (zoom in listOf(9f, 12f)) {
            assertEquals(listOf(1), resolveMapPlacement(listOf(emptyLm, ward), 1000, 1000, zoom).map { it.index })
        }
        val farEmptyLm = cand(3, MapPlacementKind.LANDMARK_EMPTY, 0, 500) // không đè ai thì vẫn hiện
        assertTrue(3 in resolveMapPlacement(listOf(ward, farEmptyLm), 1000, 1000, 9f).map { it.index })
    }

    @Test
    fun `zoom duoi 11 phuong uu tien, tu 11 dia danh co bai uu tien`() {
        val ward = cand(1, MapPlacementKind.BUBBLE, 3, 50)
        val lmWithPosts = cand(2, MapPlacementKind.LANDMARK_WITH_POSTS, 1, 0)
        assertEquals(listOf(1), resolveMapPlacement(listOf(ward, lmWithPosts), 1000, 1000, 10.9f).map { it.index })
        assertEquals(listOf(2), resolveMapPlacement(listOf(ward, lmWithPosts), 1000, 1000, 11f).map { it.index })
    }

    @Test
    fun `Da Nang zoom 9 den duoi 11 phai thay bong bong phuong`() {
        // Kích thước gần đúng của MapBubbleItem (56dp + nhãn) và LandmarkBubble (73dp + nhãn) ở mật độ 3.0 (480dpi)
        val density = 3f
        val (bw, bh) = (80 * density).roundToInt() to (90 * density).roundToInt()
        val (lw, lh) = (100 * density).roundToInt() to (110 * density).roundToInt()
        val (w, h) = 1080 to 2000
        for (zoom in listOf(9f, 10f, 10.9f)) {
            val vm = MapViewModel()
            val cam = MapCameraState(16.05, 108.2, zoom)
            vm.onAction(MapAction.OnCameraChanged(cam))
            val state = vm.uiState.value
            fun rect(lat: Double, lng: Double, rw: Int, rh: Int): IntRect {
                val p = projectLatLngToPixel(lat, lng, cam, w.toFloat(), h.toFloat(), density)
                val x = (p.x - rw / 2f).roundToInt(); val y = (p.y - rh / 2f).roundToInt()
                return IntRect(x, y, x + rw, y + rh)
            }
            val landmarks = state.landmarks
            val candidates = landmarks.mapIndexed { i, lm ->
                MapPlacementCandidate(i, rect(lm.lat, lm.lng, lw, lh),
                    if (lm.postCount > 0) MapPlacementKind.LANDMARK_WITH_POSTS else MapPlacementKind.LANDMARK_EMPTY, lm.postCount)
            } + state.visibleBubbles.mapIndexed { j, b ->
                MapPlacementCandidate(landmarks.size + j, rect(b.lat, b.lng, bw, bh), MapPlacementKind.BUBBLE, b.postCount)
            }
            val placed = resolveMapPlacement(candidates, w, h, zoom).map { it.index }
            val wardsShown = state.visibleBubbles.filterIndexed { j, b ->
                b.type == MapBubbleType.WARD && (landmarks.size + j) in placed
            }
            assertTrue("Zoom $zoom: không thấy bong bóng phường nào", wardsShown.isNotEmpty())
        }
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
