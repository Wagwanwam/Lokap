package vn.nhom2.lokap.data.fake

import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import vn.nhom2.lokap.domain.model.*
data class AppSettingsData(
    val defaultLocationLevel: LocationLevel = LocationLevel.PROVINCE,
    val notifyNewPhotos: Boolean = true,
    val notifyReactions: Boolean = true,
    val notifyMessages: Boolean = true,
    val notifyFriendRequests: Boolean = true
)

object SampleSocialData {
    const val MAX_FRIENDS = 20

    private val _currentUserOverride = MutableStateFlow<User?>(null)
    val currentUserOverride: StateFlow<User?> = _currentUserOverride.asStateFlow()

    val directoryUsers: List<User> by lazy {
        val existingIds = SampleData.users.map { it.id }.toSet()
        val extraUsers = listOf(
            User(
                id = "u_genz_01",
                username = "linh.chill",
                displayName = "Linh Đà Lạt",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300"
            ),
            User(
                id = "u_genz_02",
                username = "khoi.langthang",
                displayName = "Khôi Đi Phượt",
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300"
            ),
            User(
                id = "u_genz_03",
                username = "my.boba",
                displayName = "Trà My Tiệm Trà Chanh",
                avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=300"
            ),
            User(
                id = "u_genz_04",
                username = "nam.skateboard",
                displayName = "Nam Lướt Ván",
                avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300"
            ),
            User(
                id = "u_genz_05",
                username = "vy.matcha",
                displayName = "Thảo Vy Matcha",
                avatarUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=300"
            ),
            User(
                id = "u_genz_06",
                username = "hieu.film",
                displayName = "Hiếu Chụp Film",
                avatarUrl = null
            ),
            User(
                id = "u_genz_07",
                username = "an. toxic",
                displayName = "An Hay Quạo",
                avatarUrl = null
            )
        ).filter { it.id !in existingIds }

        (SampleData.users + extraUsers).distinctBy { it.id }
    }

    private val _friendStatuses = MutableStateFlow<Map<String, FriendStatus>>(buildInitialStatuses())
    val friendStatuses: StateFlow<Map<String, FriendStatus>> = _friendStatuses.asStateFlow()

    private val _settings = MutableStateFlow(AppSettingsData())
    val settings: StateFlow<AppSettingsData> = _settings.asStateFlow()

    private fun buildInitialStatuses(): Map<String, FriendStatus> {
        val myId = SampleData.currentUser.id
        val sampleFriendIds = SampleData.users.map { it.id }.toSet() - myId
        val result = mutableMapOf<String, FriendStatus>()

        sampleFriendIds.forEach { result[it] = FriendStatus.FRIENDS }

        directoryUsers.filter { it.id != myId && it.id !in sampleFriendIds }
            .forEachIndexed { index, user ->
                result[user.id] = when (index) {
                    0, 1 -> FriendStatus.REQUEST_RECEIVED
                    2 -> FriendStatus.REQUEST_SENT
                    3 -> FriendStatus.BLOCKED
                    else -> FriendStatus.NONE
                }
            }
        return result
    }

    internal fun setFriendStatusesForTest(statuses: Map<String, FriendStatus>) {
        _friendStatuses.value = statuses
    }

    fun getCurrentUser(): User {
        return _currentUserOverride.value ?: SampleData.currentUser
    }

    fun findUser(userId: String?): User? {
        val current = getCurrentUser()
        if (userId == null || userId == current.id) return current
        return directoryUsers.find { it.id == userId }?.let { found ->
            if (found.id == current.id) current else found
        }
    }

    fun updateMyDisplayName(newDisplayName: String) {
        val current = getCurrentUser()
        _currentUserOverride.value = current.copy(displayName = newDisplayName.trim())
    }

    fun updateMyProfile(displayName: String, username: String, bio: String) {
        val current = getCurrentUser()
        val cleanUsername = username.trim().removePrefix("@")
        _currentUserOverride.value = current.copy(
            displayName = displayName.trim(),
            username = if (cleanUsername.isNotBlank()) cleanUsername else current.username,
            bio = bio.trim()
        )
    }

    fun getFriendStatus(userId: String): FriendStatus {
        if (userId == getCurrentUser().id) return FriendStatus.NONE
        return _friendStatuses.value[userId] ?: FriendStatus.NONE
    }

    fun getFriendsCount(userId: String): Int {
        if (userId == getCurrentUser().id) {
            return _friendStatuses.value.values.count { it == FriendStatus.FRIENDS }
        }
        return ((userId.hashCode() and 0x7FFFFFFF) % 16) + 2
    }

    fun sendFriendRequest(userId: String): Boolean {
        val currentFriends = _friendStatuses.value.values.count { it == FriendStatus.FRIENDS }
        if (currentFriends >= MAX_FRIENDS) return false
        _friendStatuses.update { it + (userId to FriendStatus.REQUEST_SENT) }
        return true
    }

    fun cancelFriendRequest(userId: String) {
        _friendStatuses.update { it + (userId to FriendStatus.NONE) }
    }

    fun acceptFriendRequest(userId: String): Boolean {
        val currentFriends = _friendStatuses.value.values.count { it == FriendStatus.FRIENDS }
        if (currentFriends >= MAX_FRIENDS) return false
        _friendStatuses.update { it + (userId to FriendStatus.FRIENDS) }
        return true
    }

    fun declineFriendRequest(userId: String) {
        _friendStatuses.update { it + (userId to FriendStatus.NONE) }
    }

    fun unfriend(userId: String) {
        _friendStatuses.update { it + (userId to FriendStatus.NONE) }
    }

    fun blockUser(userId: String) {
        _friendStatuses.update { it + (userId to FriendStatus.BLOCKED) }
    }

    fun unblockUser(userId: String) {
        _friendStatuses.update { it + (userId to FriendStatus.NONE) }
    }

    fun updateSettings(transform: (AppSettingsData) -> AppSettingsData) {
        _settings.update(transform)
    }

    fun resetAccountForDemo() {
        _currentUserOverride.value = null
        _friendStatuses.value = buildInitialStatuses()
        _settings.value = AppSettingsData()
    }

    fun conversationIdFor(otherUserId: String): String {
        val myId = getCurrentUser().id
        return if (myId < otherUserId) "conv_${myId}_$otherUserId" else "conv_${otherUserId}_$myId"
    }

    fun isPostVisibleToViewer(
        post: Post,
        isMe: Boolean,
        friendStatus: FriendStatus,
        now: Instant = SampleData.NOW
    ): Boolean {

        if (isMe) return true
        if (friendStatus == FriendStatus.BLOCKED) return false
        val isFriend = friendStatus == FriendStatus.FRIENDS
        return when (post.audience) {
            Audience.FRIENDS -> isFriend
            Audience.PUBLIC -> isFriend || post.publicVisibleAt == null || !post.publicVisibleAt.isAfter(now)
            Audience.DIRECT -> false
        }
    }

    fun buildProvinceMapBubbles(
        posts: List<Post>,
        provinces: List<Province>
    ): List<MapBubble> {
        val postsByProvince = posts.groupBy { it.location.provinceCode }
        return provinces.mapNotNull { province ->
            val provincePosts = postsByProvince[province.code].orEmpty()
            if (provincePosts.isEmpty()) return@mapNotNull null
            MapBubble(
                id = "profile_prov_${province.code}",
                type = MapBubbleType.PROVINCE,
                code = province.code,
                label = province.name,
                lat = province.lat,
                lng = province.lng,
                postCount = provincePosts.size,
                coverImageUrl = provincePosts.maxByOrNull { it.takenAt }?.imageUrl,
                hasFriendPosts = true
            )
        }
    }
}
