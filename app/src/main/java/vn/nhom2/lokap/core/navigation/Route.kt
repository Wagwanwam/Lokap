package vn.nhom2.lokap.core.navigation

import kotlinx.serialization.Serializable

sealed interface Route {

    @Serializable
    data object Camera : Route

    @Serializable
    data class Composer(val imagePath: String) : Route

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

    @Serializable
    data object Notifications : Route

    @Serializable
    data object Inbox : Route

    @Serializable
    data class Chat(val conversationId: String) : Route

    @Serializable
    data object Settings : Route
}
