package vn.nhom2.lokap.core.navigation

import kotlinx.serialization.Serializable
sealed interface Route {

    @Serializable
    data object Camera : Route

    /** [landmarkId] có giá trị khi đưa ảnh lên một địa danh: Composer khoá Công khai và vị trí = địa danh. */
    @Serializable
    data class Composer(val imagePath: String, val landmarkId: String? = null) : Route

    /** Camera mở từ màn Địa danh: chụp xong sang Composer đã chọn sẵn địa danh. */
    @Serializable
    data class LandmarkCamera(val landmarkId: String) : Route

    @Serializable
    data object Feed : Route

    @Serializable
    data object Map : Route

    @Serializable
    data class RegionAlbum(val level: String, val code: String) : Route

    @Serializable
    data class Landmark(val id: String) : Route

    @Serializable
    data class PostDetail(val id: String) : Route

    @Serializable
    data class Profile(val userId: String? = null) : Route

    @Serializable
    data object Friends : Route

    /** [username] có giá trị khi mở từ link mời `lokap://add/<username>`. */
    @Serializable
    data class AddFriend(val username: String? = null) : Route

    @Serializable
    data object ScanFriendCode : Route

    @Serializable
    data object Notifications : Route

    @Serializable
    data object Inbox : Route

    @Serializable
    data class Chat(val conversationId: String) : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object Search : Route
}
