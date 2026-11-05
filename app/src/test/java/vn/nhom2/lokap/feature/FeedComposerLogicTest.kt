package vn.nhom2.lokap.feature

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import vn.nhom2.lokap.data.fake.FakePostStore
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.data.fake.SampleData2
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.PostSendState
import vn.nhom2.lokap.domain.model.PublishDelay
import vn.nhom2.lokap.domain.model.toRegionAlbumTarget
import vn.nhom2.lokap.feature.composer.ComposerAction
import vn.nhom2.lokap.feature.composer.ComposerViewModel
import vn.nhom2.lokap.feature.feed.FeedViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class FeedComposerLogicTest {

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    // ---------- Feed ----------

    @Test
    fun `Khám phá không hiện bài public đang hẹn giờ`() {
        val state = FeedViewModel().uiState.value
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
        val friendIds = FeedViewModel().uiState.value.friendPosts.map { it.id }.toSet()
        SampleData.posts.filter { it.audience == Audience.PUBLIC }.forEach {
            assertTrue("${it.id} (public) phải có trong tab Bạn bè", it.id in friendIds)
        }
    }

    @Test
    fun `chip vị trí mức Việt Nam không mở album`() {
        val country = SampleData.posts.first { it.location.level == LocationLevel.COUNTRY }
        assertNull(country.location.toRegionAlbumTarget())

        val ward = SampleData.posts.first { it.location.level == LocationLevel.WARD }
        assertEquals("WARD" to ward.location.wardCode, ward.location.toRegionAlbumTarget())
    }

    // ---------- Composer ----------

    @Test
    fun `chọn Địa điểm cụ thể mà chưa chọn địa điểm thì không cho Gửi`() {
        val vm = ComposerViewModel()
        vm.onAction(ComposerAction.OnSelectLocationLevel(LocationLevel.PLACE))
        vm.onAction(ComposerAction.OnDismissPlaceSheet)
        assertFalse(vm.uiState.value.canSend)

        vm.onAction(ComposerAction.OnSelectPlace(SampleData2.daNangPlaces.first()))
        assertTrue(vm.uiState.value.canSend)
    }

    @Test
    fun `hẹn giờ chỉ áp dụng cho Public`() {
        val caption = "test-friends-${System.nanoTime()}"
        val vm = ComposerViewModel()
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
        val vm = ComposerViewModel()
        vm.onAction(ComposerAction.OnCaptionChange(caption))
        vm.onAction(ComposerAction.OnSendClick)

        val pending = FakePostStore.posts.value.first { it.caption == caption }
        assertEquals(PostSendState.PENDING, pending.sendState)
        assertEquals(pending.id, FeedViewModel().uiState.value.friendPosts.first().id) // mới nhất ở đầu Feed

        withTimeout(5_000) {
            FakePostStore.posts.first { list ->
                list.first { it.id == pending.id }.sendState == PostSendState.SENT
            }
        }
        Unit
    }
}
