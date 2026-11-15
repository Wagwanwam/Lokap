package vn.nhom2.lokap.feature

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import vn.nhom2.lokap.data.fake.FakeAuthRepository
import vn.nhom2.lokap.data.fake.FakeLandmarkRepository
import vn.nhom2.lokap.data.fake.FakeImageImporter
import vn.nhom2.lokap.data.fake.FakeLocationRepository
import vn.nhom2.lokap.data.fake.FakePostRepository
import vn.nhom2.lokap.data.fake.FakeSocialRepository
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.*
import vn.nhom2.lokap.domain.repository.*
import vn.nhom2.lokap.feature.auth.setup.SetupProfileUiState
import vn.nhom2.lokap.feature.composer.ComposerUiState
import vn.nhom2.lokap.feature.landmark.LandmarkViewModel
import vn.nhom2.lokap.feature.map.*
import vn.nhom2.lokap.feature.region.RegionAlbumViewModel
import vn.nhom2.lokap.feature.search.SearchUiState
import vn.nhom2.lokap.feature.settings.SettingsUiState

@OptIn(ExperimentalCoroutinesApi::class)
class NoBundledDataTest {
    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun repository(cache: MutableStateFlow<List<Post>>) = object : PostRepository by FakePostRepository() {
        override val friendFeed = cache
        override val publicPosts = flowOf(emptyList<Post>())
        override fun observeLandmarkPosts(landmarkId: String) =
            cache.map { list -> list.filter { it.location.landmarkId == landmarkId && it.audience == Audience.PUBLIC } }
    }

    private fun landmarkVm(id: String, repo: PostRepository) = LandmarkViewModel(
        SavedStateHandle(mapOf("id" to id)), repo, FakeLandmarkRepository(), FakeSocialRepository(),
        FakeAuthRepository(), FakeImageImporter()
    )

    @Test fun `empty repositories do not populate map or albums`() {
        val repo = repository(MutableStateFlow(emptyList()))
        val social = FakeSocialRepository()
        val map = MapViewModel(FakeLocationRepository(), repo, social, FakeLandmarkRepository())
        val album = RegionAlbumViewModel(SavedStateHandle(mapOf("level" to "PROVINCE", "code" to "da-nang")), repo, social, FakeAuthRepository())
        val landmark = landmarkVm("lm_cau_rong", repo)
        assertTrue(map.uiState.value.visibleBubbles.isEmpty())
        assertTrue(map.uiState.value.landmarks.isEmpty())
        assertTrue(map.uiState.value.isViewportEmpty)
        assertTrue(album.uiState.value.displayedPosts.isEmpty())
        assertEquals(0, album.uiState.value.totalPostCount)
        assertNull(landmark.uiState.value.landmark)
        assertTrue(landmark.uiState.value.posts.isEmpty())
    }

    @Test fun `removing last real post clears map without sample fallback`() {
        val post = SampleData.posts.first { it.location.level == LocationLevel.PLACE && it.sendState == PostSendState.SENT }
            .copy(audience = Audience.PUBLIC, publicVisibleAt = null)
        val cache = MutableStateFlow(listOf(post))
        val map = MapViewModel(FakeLocationRepository(), repository(cache), FakeSocialRepository(), FakeLandmarkRepository())
        assertTrue(map.uiState.value.visibleBubbles.isNotEmpty())
        cache.value = emptyList()
        assertTrue(map.uiState.value.visibleBubbles.isEmpty())
        assertTrue(map.uiState.value.landmarks.isEmpty())
    }

    @Test fun `map excludes direct pending and country posts and deduplicates feeds`() = runTest {
        val post = SampleData.posts.first { it.location.level == LocationLevel.PLACE }.copy(
            audience = Audience.PUBLIC, sendState = PostSendState.SENT, publicVisibleAt = null
        )
        val cache = MutableStateFlow(listOf(post, post.copy(id = "direct", audience = Audience.DIRECT),
            post.copy(id = "pending", sendState = PostSendState.PENDING),
            post.copy(id = "country", location = post.location.copy(level = LocationLevel.COUNTRY))))
        val repo = object : PostRepository by repository(cache) { override val publicPosts = flowOf(listOf(post)) }
        assertEquals(listOf(post), repo.observeMapPosts().first())
    }

    @Test fun `friend filter uses actual relationship instead of any other author`() {
        val post = SampleData.posts.first { it.location.level == LocationLevel.PLACE }.copy(
            audience = Audience.PUBLIC, sendState = PostSendState.SENT, publicVisibleAt = null
        )
        val graphState = MutableStateFlow(SocialGraph())
        val social = object : SocialRepository by FakeSocialRepository() { override val graph = graphState }
        val map = MapViewModel(FakeLocationRepository(), repository(MutableStateFlow(listOf(post))), social, FakeLandmarkRepository())
        map.onAction(MapAction.OnAudienceFilterChange(MapAudienceFilter.FRIENDS))
        assertTrue(map.uiState.value.visibleBubbles.isEmpty())
        graphState.value = SocialGraph(statuses = mapOf(post.author.id to FriendStatus.FRIENDS))
        assertEquals(1, map.uiState.value.visibleBubbles.sumOf { it.postCount })
    }

    @Test fun `forms do not prefill invented personal data`() {
        val profile = SetupProfileUiState()
        assertNull(profile.selectedAvatarUrl)
        assertTrue(profile.presetAvatars.isEmpty())
        assertNull(profile.birthYear)
        assertTrue(profile.homeProvince.isEmpty())
        assertTrue(ComposerUiState().currentProvinceCode.isEmpty())
        assertTrue(ComposerUiState().filteredPlaces.isEmpty())
        assertTrue(SearchUiState().recentSearches.isEmpty())
        assertTrue(SettingsUiState().linkedEmails.isEmpty())
    }
}
