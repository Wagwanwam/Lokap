package vn.nhom2.lokap.data.local

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import vn.nhom2.lokap.domain.model.User

/**
 * Lưu nhỏ trên máy: đã xem giới thiệu chưa, hồ sơ của người đang đăng nhập (để mở app khi mất mạng
 * vẫn vào thẳng Camera). Phiên đăng nhập (token) do Firebase Auth tự lưu.
 */
@Singleton
class SessionPrefs @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences("lokap_session", Context.MODE_PRIVATE)

    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING, false)
        set(value) = prefs.edit { putBoolean(KEY_ONBOARDING, value) }

    /** Dấu của (bạn bè + hồ sơ) lần cuối đã cập nhật viewerIds trên bài của mình (FirestoreGraph). */
    var reconciledFingerprint: String?
        get() = prefs.getString(KEY_RECONCILED, null)
        set(value) = prefs.edit { putString(KEY_RECONCILED, value) }

    fun cachedProfile(userId: String): User? {
        if (prefs.getString(KEY_ID, null) != userId) return null
        val username = prefs.getString(KEY_USERNAME, null) ?: return null
        return User(
            id = userId,
            username = username,
            displayName = prefs.getString(KEY_DISPLAY_NAME, null).orEmpty(),
            avatarUrl = prefs.getString(KEY_AVATAR, null),
            bio = prefs.getString(KEY_BIO, null).orEmpty()
        )
    }

    fun saveProfile(user: User) = prefs.edit {
        putString(KEY_ID, user.id)
        putString(KEY_USERNAME, user.username)
        putString(KEY_DISPLAY_NAME, user.displayName)
        putString(KEY_AVATAR, user.avatarUrl)
        putString(KEY_BIO, user.bio)
    }

    fun clearProfile() = prefs.edit {
        remove(KEY_ID)
        remove(KEY_USERNAME)
        remove(KEY_DISPLAY_NAME)
        remove(KEY_AVATAR)
        remove(KEY_BIO)
        remove(KEY_RECONCILED)
    }

    private companion object {
        const val KEY_ONBOARDING = "onboarding_done"
        const val KEY_ID = "profile_id"
        const val KEY_USERNAME = "profile_username"
        const val KEY_DISPLAY_NAME = "profile_display_name"
        const val KEY_AVATAR = "profile_avatar"
        const val KEY_BIO = "profile_bio"
        const val KEY_RECONCILED = "reconciled_fingerprint"
    }
}
