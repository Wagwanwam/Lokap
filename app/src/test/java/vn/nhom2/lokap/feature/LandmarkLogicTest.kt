package vn.nhom2.lokap.feature

import androidx.lifecycle.SavedStateHandle
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
import vn.nhom2.lokap.data.fake.FakeAuthRepository
import vn.nhom2.lokap.data.fake.FakeImageImporter
import vn.nhom2.lokap.data.fake.FakeLandmarkRepository
import vn.nhom2.lokap.data.fake.FakeLocationRepository
import vn.nhom2.lokap.data.fake.FakePostRepository
import vn.nhom2.lokap.data.fake.FakePostStore
import vn.nhom2.lokap.data.fake.FakeSocialRepository
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.data.fake.SampleData2
import vn.nhom2.lokap.data.fake.SampleSocialData
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.Landmark
import vn.nhom2.lokap.domain.model.LandmarkPhotoTab
import vn.nhom2.lokap.domain.model.Post
import vn.nhom2.lokap.domain.model.PostSendState
import vn.nhom2.lokap.domain.model.Reaction
import vn.nhom2.lokap.domain.model.User
import vn.nhom2.lokap.domain.model.arrangeForLandmark
import vn.nhom2.lokap.domain.model.landmarksForMap
import vn.nhom2.lokap.domain.model.pickLandmarkCover
import vn.nhom2.lokap.domain.repository.PostDraft
import vn.nhom2.lokap.domain.repository.PostRepository
import vn.nhom2.lokap.feature.composer.ComposerAction
import vn.nhom2.lokap.feature.composer.ComposerEffect
import vn.nhom2.lokap.feature.composer.ComposerViewModel
import vn.nhom2.lokap.feature.landmark.AddLandmarkAction
import vn.nhom2.lokap.feature.landmark.AddLandmarkEffect
import vn.nhom2.lokap.feature.landmark.AddLandmarkViewModel
import vn.nhom2.lokap.feature.landmark.LandmarkAction
import vn.nhom2.lokap.feature.landmark.LandmarkEffect
import vn.nhom2.lokap.feature.landmark.LandmarkViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class LandmarkLogicTest {

    private lateinit var dispatcher: TestDispatcher

    @Before
    fun setUp() {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        FakePostStore.resetForTest()
        SampleSocialData.resetAccountForDemo()
    }

    @After
    fun tearDown() {
        FakePostStore.resetForTest()
        SampleSocialData.resetAccountForDemo()
        Dispatchers.resetMain()
    }

    private val now: Instant = SampleData.NOW
    private val me: User = SampleData.currentUser
    private val friend: User = SampleData.users[1]
    private val stranger = User(id = "u_la", username = "nguoila", displayName = "Người lạ", avatarUrl = null)

    private val landmark = Landmark(
        id = "vm_cau_rong", name = "Cầu Rồng", provinceCode = "hue", wardCode = null,
        lat = 16.0611, lng = 108.2275, provinceName = "Huế", createdBy = "u_khac"
    )

    private fun photo(
        id: String,
        reactions: Int,
        minutesAgo: Long,
        author: User = stranger,
        visibleAt: Instant? = now.minusSeconds(60),
        landmarkId: String = landmark.id
    ): Post {
        val base = SampleData.posts.first { it.id == "p_01" }
        return base.copy(
            id = id,
            author = author,
            audience = Audience.PUBLIC,
            reactions = if (reactions > 0) listOf(Reaction("❤️", reactions)) else emptyList(),
            takenAt = now.minusSeconds(minutesAgo * 60),
            publicVisibleAt = visibleAt,
            sendState = PostSendState.SENT,
            location = base.location.copy(landmarkId = landmarkId, placeName = landmark.name, provinceCode = "hue")
        )
    }

    private fun repoWith(photos: List<Post>) = object : PostRepository by FakePostRepository() {
        val attached = mutableListOf<Pair<String, String?>>()
        override fun observeLandmarkPosts(landmarkId: String) = flowOf(photos.filter { it.location.landmarkId == landmarkId })
        override suspend fun setPostLandmark(postId: String, landmark: Landmark?): Result<Unit> {
            attached += postId to landmark?.id
            return Result.success(Unit)
        }
    }

    private fun landmarkVm(
        posts: PostRepository,
        catalog: List<Landmark> = listOf(landmark),
        images: FakeImageImporter = FakeImageImporter()
    ) = LandmarkViewModel(
        SavedStateHandle(mapOf("id" to landmark.id)), posts, FakeLandmarkRepository(catalog),
        FakeSocialRepository(), FakeAuthRepository(), images
    )

    // ---------- Ảnh bìa theo số cảm xúc ----------

    @Test
    fun `anh bia la anh public nhieu cam xuc nhat, bang nhau thi anh moi hon`() {
        val photos = listOf(photo("a", 3, 10), photo("b", 9, 300), photo("c", 9, 5), photo("d", 0, 1))
        assertEquals("c", pickLandmarkCover(photos, now)?.id)
    }

    @Test
    fun `anh dang hen gio hoac chua gui xong khong duoc lam anh bia`() {
        val photos = listOf(
            photo("hen_gio", 50, 10, visibleAt = now.plusSeconds(3600)),
            photo("dang_gui", 40, 10).copy(sendState = PostSendState.PENDING),
            photo("ban_be", 30, 10).copy(audience = Audience.FRIENDS),
            photo("cong_khai", 2, 10)
        )
        assertEquals("cong_khai", pickLandmarkCover(photos, now)?.id)
        assertNull(pickLandmarkCover(emptyList(), now))
    }

    @Test
    fun `sap xep va loc anh theo Noi bat, Moi nhat, Ban be, Cua toi`() {
        val photos = listOf(photo("cu_nhieu", 9, 300), photo("moi_it", 1, 1), photo("cua_ban", 5, 50, friend), photo("cua_toi", 0, 20, me))
        val friends = setOf(friend.id)
        assertEquals(listOf("cu_nhieu", "cua_ban", "moi_it", "cua_toi"), photos.arrangeForLandmark(LandmarkPhotoTab.TOP, friends, me.id).map { it.id })
        assertEquals(listOf("moi_it", "cua_toi", "cua_ban", "cu_nhieu"), photos.arrangeForLandmark(LandmarkPhotoTab.NEWEST, friends, me.id).map { it.id })
        assertEquals(listOf("cua_ban"), photos.arrangeForLandmark(LandmarkPhotoTab.FRIENDS, friends, me.id).map { it.id })
        assertEquals(listOf("cua_toi"), photos.arrangeForLandmark(LandmarkPhotoTab.MINE, friends, me.id).map { it.id })
    }

    @Test
    fun `ban do hien ca dia danh chua co anh, so anh va bia tinh tu bai da tai`() {
        val empty = landmark.copy(id = "vm_trong", name = "Chợ Hàn")
        val result = landmarksForMap(listOf(landmark, empty), listOf(photo("a", 4, 10), photo("b", 7, 20)), now)
        val withPhotos = result.first { it.id == landmark.id }
        assertEquals(2, withPhotos.postCount)
        assertEquals("b", withPhotos.coverPostId)
        assertEquals(7, withPhotos.coverReactionCount)
        val noPhotos = result.first { it.id == "vm_trong" }
        assertEquals(0, noPhotos.postCount)
        assertNull(noPhotos.coverUrl)
    }

    // ---------- Màn Địa danh ----------

    @Test
    fun `man dia danh lay bia nhieu cam xuc nhat va doi tab thi sap xep lai`() = runTest(dispatcher) {
        val photos = listOf(photo("it", 1, 1), photo("nhieu", 12, 60), photo("khac_dia_danh", 99, 1, landmarkId = "vm_khac"))
        val vm = landmarkVm(repoWith(photos))
        advanceUntilIdle()
        val state = vm.uiState.value
        assertEquals("nhieu", state.landmark?.coverPostId)
        assertEquals(photos[1].imageUrl, state.landmark?.coverUrl)
        assertEquals(listOf("nhieu", "it"), state.posts.map { it.id })
        assertEquals(2, state.totalPostCount)
        vm.onAction(LandmarkAction.OnSelectTab(LandmarkPhotoTab.NEWEST))
        assertEquals(listOf("it", "nhieu"), vm.uiState.value.posts.map { it.id })
    }

    @Test
    fun `dia danh chua co anh van mo duoc, khong bao loi`() = runTest(dispatcher) {
        val vm = landmarkVm(repoWith(emptyList()))
        advanceUntilIdle()
        assertEquals(landmark.id, vm.uiState.value.landmark?.id)
        assertNull(vm.uiState.value.errorMessage)
        assertTrue(vm.uiState.value.posts.isEmpty())
    }

    @Test
    fun `chon anh tu thu vien thi sang Composer kem dia danh, huy chon thi khong lam gi`() = runTest(dispatcher) {
        val images = FakeImageImporter()
        val vm = landmarkVm(repoWith(emptyList()), images = images)
        vm.onAction(LandmarkAction.OnPickFromGalleryClick)
        assertEquals(LandmarkEffect.LaunchGalleryPicker, vm.effect.first())
        vm.onAction(LandmarkAction.OnGalleryResult(null))
        assertTrue(images.imported.isEmpty())
        vm.onAction(LandmarkAction.OnGalleryResult("content://anh/1"))
        assertEquals(LandmarkEffect.NavigateToComposer("/cache/gallery/content://anh/1.jpg", landmark.id), vm.effect.first())
    }

    @Test
    fun `chup anh thi mo camera cua dia danh`() = runTest(dispatcher) {
        val vm = landmarkVm(repoWith(emptyList()))
        vm.onAction(LandmarkAction.OnAddPhotoClick)
        assertTrue(vm.uiState.value.isAddSheetOpen)
        vm.onAction(LandmarkAction.OnTakePhotoClick)
        assertFalse(vm.uiState.value.isAddSheetOpen)
        assertEquals(LandmarkEffect.NavigateToCamera(landmark.id), vm.effect.first())
    }

    @Test
    fun `gan bai public da dang cua minh vao dia danh`() = runTest(dispatcher) {
        // p_04: bài public của "tôi" đang gắn địa danh khác
        val vm = landmarkVm(FakePostRepository())
        advanceUntilIdle()
        vm.onAction(LandmarkAction.OnAttachExistingClick)
        val attachable = vm.uiState.value.attachablePosts
        assertTrue(attachable.isNotEmpty())
        assertTrue(attachable.all { it.author.id == me.id && it.audience == Audience.PUBLIC && it.location.landmarkId != landmark.id })
        // Hồ sơ mẫu mượn thêm ảnh không có trong kho; chọn bài thật của mình
        val target = attachable.first { p -> FakePostStore.posts.value.any { it.id == p.id } }
        vm.onAction(LandmarkAction.OnAttachPost(target.id))
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isAttachSheetOpen)
        val moved = FakePostStore.posts.value.first { it.id == target.id }
        assertEquals(landmark.id, moved.location.landmarkId)
        assertEquals(landmark.lat, moved.location.lat!!, 0.0)
        assertTrue(vm.uiState.value.posts.any { it.id == target.id })
    }

    @Test
    fun `chi go duoc anh cua minh khoi dia danh`() = runTest(dispatcher) {
        val mine = photo("cua_toi", 0, 5, me)
        val other = photo("nguoi_khac", 0, 5)
        val repo = repoWith(listOf(mine, other))
        val vm = landmarkVm(repo)
        advanceUntilIdle()
        vm.onAction(LandmarkAction.OnPostLongClick(other))
        assertNull(vm.uiState.value.detachCandidate)
        vm.onAction(LandmarkAction.OnPostLongClick(mine))
        assertEquals("cua_toi", vm.uiState.value.detachCandidate?.id)
        vm.onAction(LandmarkAction.OnConfirmDetach)
        advanceUntilIdle()
        assertEquals(listOf("cua_toi" to null), repo.attached)
        assertNull(vm.uiState.value.detachCandidate)
    }

    // ---------- Composer đưa ảnh lên địa danh ----------

    @Test
    fun `composer dia danh khoa cong khai va gui dung vi tri cua dia danh`() = runTest(dispatcher) {
        val drafts = mutableListOf<PostDraft>()
        val repo = object : PostRepository by FakePostRepository() {
            override suspend fun createPost(draft: PostDraft): Result<String> {
                drafts += draft
                return Result.success("moi")
            }
        }
        val vm = ComposerViewModel(repo, FakeLocationRepository(), FakeLandmarkRepository(listOf(landmark)))
        vm.initImagePath("/tmp/anh.jpg")
        vm.initLandmark(landmark.id)
        advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state.isLandmarkMode)
        assertEquals(Audience.PUBLIC, state.audience)
        assertEquals("Cầu Rồng", state.locationLabel)

        vm.onAction(ComposerAction.OnSelectAudience(Audience.FRIENDS))
        assertEquals(Audience.PUBLIC, vm.uiState.value.audience)
        assertTrue(vm.effects.first() is ComposerEffect.ShowMessage)

        vm.onAction(ComposerAction.OnSendClick)
        advanceUntilIdle()
        val location = drafts.single().location
        assertEquals(landmark.id, location.landmarkId)
        // Toạ độ + tỉnh lấy từ địa danh, không phải từ GPS người chụp (rules so khớp)
        assertEquals(landmark.lat, location.lat!!, 0.0)
        assertEquals(landmark.lng, location.lng!!, 0.0)
        assertEquals("hue", location.provinceCode)
        assertNull(drafts.single().placeId)
        assertEquals(ComposerEffect.PostSent, vm.effects.first())
    }

    @Test
    fun `composer chua tai duoc dia danh thi chua cho gui`() = runTest(dispatcher) {
        val vm = ComposerViewModel(FakePostRepository(), FakeLocationRepository(), FakeLandmarkRepository())
        vm.initImagePath("/tmp/anh.jpg")
        vm.initLandmark("vm_khong_co")
        advanceUntilIdle()
        assertFalse(vm.uiState.value.canSend)
    }

    // ---------- Thêm địa danh ----------

    @Test
    fun `them dia danh tu dia diem VietMap roi mo man dia danh`() = runTest(dispatcher) {
        val landmarks = FakeLandmarkRepository()
        val vm = AddLandmarkViewModel(FakeLocationRepository(), landmarks)
        vm.onAction(AddLandmarkAction.OnQueryChange("Trình"))
        advanceUntilIdle()
        val place = vm.uiState.value.results.first()
        vm.onAction(AddLandmarkAction.OnPickPlace(place))
        advanceUntilIdle()
        assertEquals(place.name, vm.uiState.value.name)
        assertEquals(SampleData2.DEFAULT_PROVINCE_CODE, vm.uiState.value.region?.provinceCode)

        vm.onAction(AddLandmarkAction.OnNameChange("A"))
        assertFalse(vm.uiState.value.canSave)
        vm.onAction(AddLandmarkAction.OnNameChange("Trình Cà Phê Hải Châu"))
        vm.onAction(AddLandmarkAction.OnDescriptionChange("Quán cà phê lâu đời"))
        vm.onAction(AddLandmarkAction.OnSave)
        advanceUntilIdle()
        val effect = vm.effect.first() as AddLandmarkEffect.OpenLandmark
        val created = landmarks.store.value.getValue(effect.landmarkId)
        assertEquals("Trình Cà Phê Hải Châu", created.name)
        assertEquals(place.lat, created.lat, 0.0)
        assertEquals("Quán cà phê lâu đời", landmarks.created.single().description)
    }

    @Test
    fun `tu khoa qua ngan thi khong goi tim kiem`() = runTest(dispatcher) {
        val vm = AddLandmarkViewModel(FakeLocationRepository(), FakeLandmarkRepository())
        vm.onAction(AddLandmarkAction.OnQueryChange("T"))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.results.isEmpty())
        assertFalse(vm.uiState.value.isSearching)
    }

    @Test
    fun `bao cao dia danh xong thi quay lai`() = runTest(dispatcher) {
        val catalog = FakeLandmarkRepository(listOf(landmark))
        val vm = LandmarkViewModel(
            SavedStateHandle(mapOf("id" to landmark.id)), repoWith(emptyList()), catalog,
            FakeSocialRepository(), FakeAuthRepository(), FakeImageImporter()
        )
        vm.onAction(LandmarkAction.OnReportClick)
        vm.onAction(LandmarkAction.OnSubmitReport("Spam"))
        advanceUntilIdle()
        assertEquals(listOf(landmark.id to "Spam"), catalog.reported)
        assertEquals(LandmarkEffect.NavigateBack, vm.effect.first())
    }
}
