package vn.nhom2.lokap.feature

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import vn.nhom2.lokap.data.fake.FakeAuthRepository
import vn.nhom2.lokap.data.fake.FakeLandmarkRepository
import vn.nhom2.lokap.data.fake.FakePostRepository
import vn.nhom2.lokap.data.fake.FakePostStore
import vn.nhom2.lokap.data.fake.FakeSocialRepository
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.data.fake.SampleData2
import vn.nhom2.lokap.data.fake.SampleSocialData
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.PostSendState
import vn.nhom2.lokap.domain.model.PublishDelay
import vn.nhom2.lokap.domain.model.toRegionAlbumTarget
import vn.nhom2.lokap.feature.composer.ComposerAction
import vn.nhom2.lokap.feature.composer.ComposerEffect
import vn.nhom2.lokap.feature.composer.ComposerViewModel
import vn.nhom2.lokap.data.fake.FakeLocationRepository
import vn.nhom2.lokap.feature.feed.FeedAction
import vn.nhom2.lokap.feature.feed.FeedViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class FeedComposerLogicTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        FakePostStore.resetForTest()
        SampleSocialData.resetAccountForDemo()
    }

    @After
    fun tearDown() {
        FakePostStore.resetForTest()
        SampleSocialData.resetAccountForDemo()
        Dispatchers.resetMain()
    }

    // ViewModel nhận Repository; bản giả bọc đúng các store cũ nên hành vi tuần 2 không đổi
    private fun feed() = FeedViewModel(FakePostRepository(), FakeSocialRepository(), FakeAuthRepository(), ContentTestRepository(), ContentTestRepository(), ContentTestRepository())

    private fun composer() = ComposerViewModel(FakePostRepository(), FakeLocationRepository(), FakeLandmarkRepository())

    // ---------- Feed ----------

    @Test
    fun `Khám phá không hiện bài public đang hẹn giờ`() {
        val state = feed().uiState.value
        val explore = (state.nearbyPosts + state.landmarkPosts).map { it.id }.toSet()

        val scheduled = SampleData.posts.filter {
            it.audience == Audience.PUBLIC && it.publicVisibleAt?.isAfter(SampleData.NOW) == true
        }
        assertTrue("SampleData phải có bài hẹn giờ để test có ý nghĩa", scheduled.isNotEmpty())
        scheduled.forEach { assertFalse("${it.id} đang hẹn giờ mà lọt vào Khám phá", it.id in explore) }

        // Chỉ bài PUBLIC đã gửi xong
        (state.nearbyPosts + state.landmarkPosts).forEach {
            assertEquals(Audience.PUBLIC, it.audience)
            assertEquals(PostSendState.SENT, it.sendState)
        }
    }

    @Test
    fun `tab Bạn bè có bài public của bạn bè, kể cả đang hẹn giờ`() {
        val friendIds = feed().uiState.value.friendPosts.map { it.id }.toSet()
        SampleData.posts.filter { it.audience == Audience.PUBLIC }.forEach {
            assertTrue("${it.id} (public) phải có trong tab Bạn bè", it.id in friendIds)
        }
    }

    @Test
    fun `dropdown tab Bạn bè lọc ảnh theo 1 người, chọn Tất cả thì hiện lại hết`() {
        val vm = feed()
        val all = vm.uiState.value.friendPosts
        val author = all.first().author
        assertTrue(vm.uiState.value.friendFilterOptions.any { it.id == author.id })

        vm.onAction(FeedAction.OnSelectFriendFilter(author.id))
        val filtered = vm.uiState.value.visibleFriendPosts
        assertTrue(filtered.isNotEmpty())
        assertTrue(filtered.all { it.author.id == author.id })

        vm.onAction(FeedAction.OnSelectFriendFilter(null))
        assertEquals(all, vm.uiState.value.visibleFriendPosts)
    }

    @Test
    fun `chip vị trí mức Việt Nam không mở album`() {
        val country = SampleData.posts.first { it.location.level == LocationLevel.COUNTRY }
        assertNull(country.location.toRegionAlbumTarget())

        val ward = SampleData.posts.first { it.location.level == LocationLevel.WARD }
        assertEquals("WARD" to ward.location.wardCode, ward.location.toRegionAlbumTarget())
    }

    @Test
    fun `bấm Chặn chỉ mở hộp xác nhận, Huỷ thì không xoá bài nào`() {
        val vm = feed()
        val before = FakePostStore.posts.value
        val author = SampleData.users[1]

        vm.onAction(FeedAction.OnBlockUser(author.id, author.displayName))
        assertEquals(author.id, vm.uiState.value.pendingBlock?.userId)
        assertEquals(before, FakePostStore.posts.value)

        vm.onAction(FeedAction.OnDismissBlockDialog)
        assertNull(vm.uiState.value.pendingBlock)
        assertEquals(before, FakePostStore.posts.value)
    }

    @Test
    fun `xác nhận Chặn thì ghi trạng thái chặn và bỏ mọi bài của người đó`() {
        val vm = feed()
        val author = SampleData.users[1]
        vm.onAction(FeedAction.OnBlockUser(author.id, author.displayName))
        vm.onAction(FeedAction.OnConfirmBlock)
        assertEquals(FriendStatus.BLOCKED, SampleSocialData.getFriendStatus(author.id))
        assertTrue(FakePostStore.posts.value.none { it.author.id == author.id })
        assertTrue(vm.uiState.value.friendPosts.none { it.author.id == author.id })
    }

    @Test
    fun `thả cảm xúc ở Feed ghi vào kho bài`() {
        val vm = feed()
        vm.onAction(FeedAction.OnReact("p_01", "😂"))
        assertEquals("😂", FakePostStore.posts.value.first { it.id == "p_01" }.myReaction)
        assertEquals("😂", vm.uiState.value.friendPosts.first { it.id == "p_01" }.myReaction)
    }

    @Test
    fun `làm mới khi mất mạng thì hiện offline, vẫn giữ bài đã có`() {
        val vm = FeedViewModel(OfflinePostRepository(), FakeSocialRepository(), FakeAuthRepository(), ContentTestRepository(), ContentTestRepository(), ContentTestRepository())
        vm.onAction(FeedAction.OnRefresh)
        assertTrue(vm.uiState.value.isOffline)
        assertFalse(vm.uiState.value.isRefreshing)
        assertNull("Có bài trong máy thì không che bằng màn lỗi", vm.uiState.value.errorMessage)
        assertTrue(vm.uiState.value.friendPosts.isNotEmpty())
    }

    @Test
    fun `bài trong máy về muộn thì tắt màn chờ ngay, không đợi server`() {
        val repo = SlowCachePostRepository()
        val vm = FeedViewModel(repo, FakeSocialRepository(), FakeAuthRepository(), ContentTestRepository(), ContentTestRepository(), ContentTestRepository())
        assertTrue("Chưa có gì trong máy, đang hỏi server", vm.uiState.value.isLoading)
        repo.cache.value = SampleData.posts.take(3)
        assertFalse("Room đã có bài thì hiện ngay", vm.uiState.value.isLoading)
        assertEquals(3, vm.uiState.value.friendPosts.size)
    }

    @Test
    fun `người đang đăng nhập và trạng thái bạn bè lấy từ Repository`() {
        val state = feed().uiState.value
        assertEquals(SampleData.currentUser.id, state.currentUserId)
        assertEquals(FriendStatus.FRIENDS, state.friendStatuses[SampleData.users[1].id])
    }

    // ---------- Composer ----------

    @Test
    fun `chú thích trên ảnh tối đa 45 ký tự`() {
        val vm = composer()
        vm.onAction(ComposerAction.OnCaptionChange("a".repeat(70)))
        assertEquals(45, vm.uiState.value.caption.length)
    }

    @Test
    fun `nhãn vị trí theo đúng mức sẽ đăng, không còn dấu phẩy trống`() {
        val base = vn.nhom2.lokap.feature.composer.ComposerUiState(
            locationStatus = vn.nhom2.lokap.feature.composer.LocationAcquisitionState.READY,
            currentProvinceName = "Đà Nẵng",
            currentWardName = "Hải Châu"
        )
        assertEquals("Đà Nẵng", base.locationLabel)
        assertEquals("Hải Châu, Đà Nẵng", base.copy(locationLevel = LocationLevel.WARD).locationLabel)
        assertEquals("Việt Nam", base.copy(locationLevel = LocationLevel.COUNTRY).locationLabel)
        assertEquals("Việt Nam", base.copy(currentProvinceName = "", currentWardName = "").locationLabel)
        assertEquals(
            "Đang định vị…",
            base.copy(locationStatus = vn.nhom2.lokap.feature.composer.LocationAcquisitionState.FETCHING).locationLabel
        )
    }

    @Test
    fun `chọn Địa điểm cụ thể mà chưa chọn địa điểm thì không cho Gửi`() {
        val vm = composer()
        vm.onAction(ComposerAction.OnSelectLocationLevel(LocationLevel.PLACE))
        vm.onAction(ComposerAction.OnDismissPlaceSheet)
        assertFalse(vm.uiState.value.canSend)

        vm.onAction(ComposerAction.OnSelectPlace(SampleData2.daNangPlaces.first()))
        assertTrue(vm.uiState.value.canSend)
    }

    @Test
    fun `hẹn giờ chỉ áp dụng cho Public`() {
        val caption = "test-friends-${System.nanoTime()}"
        val vm = composer()
        vm.onAction(ComposerAction.OnSelectAudience(Audience.PUBLIC))
        vm.onAction(ComposerAction.OnSelectPublishDelay(PublishDelay.THREE_DAYS))
        vm.onAction(ComposerAction.OnSelectAudience(Audience.FRIENDS)) // đổi ý sau khi đã chọn hẹn giờ
        vm.onAction(ComposerAction.OnCaptionChange(caption))
        vm.onAction(ComposerAction.OnSendClick)

        val post = FakePostStore.posts.value.first { it.caption == caption }
        assertEquals(Audience.FRIENDS, post.audience)
        assertNull(post.publicVisibleAt)
    }

    @Test
    fun `Gửi thì bài hiện Đang gửi rồi thành đã gửi`() = runBlocking {
        val caption = "test-send-${System.nanoTime()}"
        val vm = composer()
        vm.onAction(ComposerAction.OnCaptionChange(caption))
        vm.onAction(ComposerAction.OnSendClick)

        val pending = FakePostStore.posts.value.first { it.caption == caption }
        assertEquals(PostSendState.PENDING, pending.sendState)
        assertEquals(pending.id, feed().uiState.value.friendPosts.first().id) // mới nhất ở đầu Feed

        withTimeout(5_000) {
            FakePostStore.posts.first { list ->
                list.first { it.id == pending.id }.sendState == PostSendState.SENT
            }
        }
        Unit
    }

    @Test
    fun `gửi thất bại thì báo lỗi, mở lại nút Gửi và không quay về Camera`() = runBlocking {
        val vm = ComposerViewModel(OfflinePostRepository(), FakeLocationRepository(), FakeLandmarkRepository())
        vm.onAction(ComposerAction.OnCaptionChange("mất mạng"))
        vm.onAction(ComposerAction.OnSendClick)
        val effect = withTimeoutOrNull(1000) { vm.effects.first() }
        assertTrue("Phải báo lỗi, nhận: $effect", effect is ComposerEffect.ShowMessage)
        assertFalse(vm.uiState.value.isSubmitting)
        assertTrue(vm.uiState.value.canSend)
        Unit
    }
}
