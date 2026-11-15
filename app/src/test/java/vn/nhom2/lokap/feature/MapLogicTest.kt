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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import vn.nhom2.lokap.data.fake.*
import vn.nhom2.lokap.domain.repository.PostRepository
import kotlinx.coroutines.flow.flowOf
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.MapBubbleType
import vn.nhom2.lokap.domain.model.PROVINCE_WIDE_MAX_ZOOM
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

    private fun mapPosts(): PostRepository = object : PostRepository by FakePostRepository() {
        override val friendFeed = flowOf(SampleData.posts)
        override val publicPosts = flowOf(emptyList<vn.nhom2.lokap.domain.model.Post>())
    }

    private fun map() = MapViewModel(FakeLocationRepository(), mapPosts(), FakeSocialRepository(), FakeLandmarkRepository())

    private fun landmarkVm(id: String, landmarks: FakeLandmarkRepository = FakeLandmarkRepository()) = LandmarkViewModel(
        SavedStateHandle(mapOf("id" to id)), mapPosts(), landmarks, FakeSocialRepository(), FakeAuthRepository(), FakeImageImporter()
    )

    private fun MapViewModel.bubblesAt(zoom: Float) =
        apply { onAction(MapAction.OnCameraChanged(MapCameraState(16.05, 108.2, zoom))) }
            .uiState.value.visibleBubbles

    // ---------- Bản đồ: bong bóng theo mức zoom ----------

    @Test
    fun `zoom duoi 8 chi co bong bong tinh, khong trung`() {
        val bubbles = map().bubblesAt(6f)
        assertTrue(bubbles.isNotEmpty())
        assertTrue(bubbles.all { it.type == MapBubbleType.PROVINCE })
        assertEquals("Trùng tỉnh: ${bubbles.map { it.code }}", bubbles.size, bubbles.distinctBy { it.code }.size)
    }

    @Test
    fun `zoom 8 den 13 la phuong va toi da mot bong bong Khap tinh moi tinh`() {
        for (zoom in listOf(8f, 10f, 12.9f)) {
            val bubbles = map().bubblesAt(zoom)
            assertTrue(bubbles.all { it.type == MapBubbleType.WARD || it.type == MapBubbleType.PROVINCE_WIDE })
            assertTrue(bubbles.any { it.type == MapBubbleType.WARD })
            val wide = bubbles.filter { it.type == MapBubbleType.PROVINCE_WIDE }
            assertEquals("Trùng \"Khắp tỉnh\": ${wide.map { it.label }}", wide.size, wide.distinctBy { it.code }.size)
            assertTrue("Nhãn phải là \"Khắp <tỉnh>\": ${wide.map { it.label }}", wide.all { it.label.startsWith("Khắp ") })
        }
    }

    @Test
    fun `Khap tinh chi hien o zoom 8 den 11, zoom gan hon chi con bai co vi tri that`() {
        val vm = map()
        val far = vm.bubblesAt(10.9f)
        assertTrue("Zoom 10.9 phải còn \"Khắp tỉnh\"", far.any { it.type == MapBubbleType.PROVINCE_WIDE })
        for (zoom in listOf(PROVINCE_WIDE_MAX_ZOOM, 12f, 12.9f, 14f)) {
            val near = vm.bubblesAt(zoom)
            assertTrue("Zoom $zoom không được có \"Khắp tỉnh\": ${near.map { it.label }}", near.none { it.type == MapBubbleType.PROVINCE_WIDE })
        }
        // Bài mức phường / địa điểm vẫn gom theo phường như cũ
        assertTrue(vm.bubblesAt(12f).any { it.type == MapBubbleType.WARD })
    }

    @Test
    fun `bam ve vi tri cua toi thi co cham do dung vi tri may va phong to`() {
        val vm = map()
        assertNull(vm.uiState.value.myLocation)
        vm.onAction(MapAction.OnMyLocationClick)
        val state = vm.uiState.value
        val here = state.myLocation!!
        // FakeLocationRepository: Hải Châu, Đà Nẵng
        assertEquals(16.0678, here.lat, 1e-9)
        assertEquals(108.2208, here.lng, 1e-9)
        assertEquals(here.lat, state.camera.centerLat, 1e-9)
        assertEquals(13f, state.camera.zoom, 0f)
    }

    @Test
    fun `chua cap quyen vi tri thi khong ve cham do va giu nguyen ban do`() {
        val vm = MapViewModel(FakeLocationRepository(available = false), mapPosts(), FakeSocialRepository(), FakeLandmarkRepository())
        val before = vm.uiState.value.camera
        vm.onAction(MapAction.OnMyLocationClick)
        assertNull(vm.uiState.value.myLocation)
        assertEquals(before, vm.uiState.value.camera)
    }

    @Test
    fun `cham Khap tinh mo album tinh ngay, khong phong to lam bong bong bien mat`() {
        val vm = map()
        val wide = vm.bubblesAt(9f).first { it.type == MapBubbleType.PROVINCE_WIDE }
        vm.onAction(MapAction.OnBubbleClick(wide))
        val preview = vm.uiState.value.selectedRegionPreview
        assertEquals("PROVINCE", preview?.level)
        assertEquals(wide.code, preview?.code)
        assertFalse(preview!!.regionName.startsWith("Khắp"))
        assertEquals(9f, vm.uiState.value.camera.zoom, 0f)
    }

    @Test
    fun `so tren bong bong dem tu bai that, khong go cung`() {
        val posts = SampleData.posts.filter {
            it.location.level != LocationLevel.COUNTRY && it.sendState == PostSendState.SENT && it.audience != Audience.DIRECT
        }
        // Mức tỉnh: mọi bài trong tỉnh
        map().bubblesAt(6f).forEach { b ->
            assertEquals(b.label, posts.count { it.location.provinceCode == b.code }, b.postCount)
        }
        // Mức phường: bài mức phường + địa điểm; "Khắp tỉnh": chỉ bài người đăng chọn mức tỉnh
        map().bubblesAt(10f).forEach { b ->
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
        val vm = map()
        vm.onAction(MapAction.OnAudienceFilterChange(MapAudienceFilter.PUBLIC))
        val total = vm.bubblesAt(6f).sumOf { it.postCount }
        val expected = SampleData.posts.count {
            it.location.level != LocationLevel.COUNTRY && it.sendState == PostSendState.SENT && it.audience != Audience.DIRECT &&
                it.audience == Audience.PUBLIC && it.location.provinceCode != null &&
                (it.publicVisibleAt == null || !it.publicVisibleAt!!.isAfter(SampleData.NOW))
        }
        assertEquals(expected, total)
    }

    @Test
    fun `chon mot nguoi ban thi chi dem bai cua nguoi do, doi nhom thi bo chon`() {
        val vm = map()
        val friends = vm.uiState.value.friends
        assertTrue("Dữ liệu mẫu phải có bạn bè", friends.isNotEmpty())
        val friend = friends.first()
        vm.onAction(MapAction.OnFriendFilterChange(friend.id))
        assertEquals(MapAudienceFilter.FRIENDS, vm.uiState.value.audienceFilter)
        assertEquals(friend.id, vm.uiState.value.selectedFriendId)

        val total = vm.bubblesAt(6f).sumOf { it.postCount }
        val expected = SampleData.posts.count {
            it.location.level != LocationLevel.COUNTRY && it.sendState == PostSendState.SENT &&
                it.audience != Audience.DIRECT && it.location.provinceCode != null && it.author.id == friend.id
        }
        assertTrue("Người bạn đứng đầu danh sách phải có ảnh trên bản đồ", expected > 0)
        assertEquals(expected, total)

        // Chọn "Tất cả bạn bè" thì đếm lại bài của mọi bạn
        vm.onAction(MapAction.OnFriendFilterChange(null))
        assertNull(vm.uiState.value.selectedFriendId)
        assertTrue(vm.bubblesAt(6f).sumOf { it.postCount } >= total)

        vm.onAction(MapAction.OnFriendFilterChange(friend.id))
        vm.onAction(MapAction.OnAudienceFilterChange(MapAudienceFilter.PUBLIC))
        assertNull(vm.uiState.value.selectedFriendId)
    }

    @Test
    fun `zoom tu 13 chi co ghim dia diem`() {
        val bubbles = map().bubblesAt(13f)
        assertTrue(bubbles.isNotEmpty())
        assertTrue(bubbles.all { it.type == MapBubbleType.PLACE })
    }

    @Test
    fun `cham bong bong tinh thi zoom sang muc phuong`() {
        val vm = map()
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
            val vm = map()
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

    private fun album(level: String, code: String, posts: PostRepository = mapPosts(), auth: FakeAuthRepository = FakeAuthRepository()) =
        RegionAlbumViewModel(SavedStateHandle(mapOf("level" to level, "code" to code)), posts, FakeSocialRepository(), auth)

    @Test
    fun `album hien bai cua chinh minh giong ban do, ke ca bai che do ban be`() = runTest(dispatcher) {
        val auth = FakeAuthRepository()
        val base = SampleData.posts.first()
        val mine = base.copy(
            id = "mine_da_nang",
            author = auth.currentUser.value!!,
            audience = Audience.FRIENDS,
            sendState = PostSendState.SENT,
            location = base.location.copy(
                level = LocationLevel.PROVINCE, provinceCode = "da-nang", provinceName = "Đà Nẵng",
                wardCode = null, wardName = null, placeName = null, landmarkId = null
            )
        )
        val repo = object : PostRepository by FakePostRepository() {
            override val friendFeed = flowOf(listOf(mine))
            override val publicPosts = flowOf(emptyList<vn.nhom2.lokap.domain.model.Post>())
        }
        val vm = album("PROVINCE", "da-nang", repo, auth)
        advanceUntilIdle()
        // Mặc định "Tất cả" như bản đồ
        assertEquals(RegionAudienceFilter.ALL, vm.uiState.value.filter)
        assertEquals(listOf("mine_da_nang"), vm.uiState.value.displayedPosts.map { it.id })
        assertEquals(1, vm.uiState.value.totalPostCount)
        vm.onAction(RegionAlbumAction.OnFilterSelected(RegionAudienceFilter.FRIENDS))
        assertEquals(listOf("mine_da_nang"), vm.uiState.value.displayedPosts.map { it.id })
        vm.onAction(RegionAlbumAction.OnFilterSelected(RegionAudienceFilter.PUBLIC))
        assertTrue(vm.uiState.value.displayedPosts.isEmpty())
    }

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
        val vm = landmarkVm("lm_cau_rong")
        advanceUntilIdle()
        val posts = vm.uiState.value.posts
        assertTrue(posts.isNotEmpty())
        assertTrue(posts.all { it.location.landmarkId == "lm_cau_rong" })
    }

    @Test
    fun `dia danh khong ton tai thi bao loi, khong mo dia danh khac`() = runTest(dispatcher) {
        val vm = landmarkVm("lm_khong_co")
        advanceUntilIdle()
        assertNull(vm.uiState.value.landmark)
        assertTrue(vm.uiState.value.errorMessage != null)
    }

    @Test
    fun `SampleData co bai muc Viet Nam de test co y nghia`() {
        assertTrue(SampleData.posts.any { it.location.level == LocationLevel.COUNTRY })
    }
}
