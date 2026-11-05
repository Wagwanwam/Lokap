package vn.nhom2.lokap.data.fake

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import vn.nhom2.lokap.domain.model.User
import java.io.IOException
import java.util.UUID

sealed class AuthStoreException(override val message: String) : Exception(message) {
    class InvalidCredentials : AuthStoreException("Email hoặc mật khẩu không đúng")
    class EmailAlreadyTaken : AuthStoreException("Email này đã được đăng ký tài khoản khác")
    class UsernameAlreadyTaken : AuthStoreException("Username này đã có người dùng")
    class NetworkError : AuthStoreException("Mất kết nối mạng. Vui lòng kiểm tra và thử lại.")
}

object FakeAuthStore {
    val currentUser = MutableStateFlow<User?>(null)
    val onboardingDone = MutableStateFlow(false)

    var simulateOffline: Boolean = false

    private val registeredCredentials = mutableMapOf(
        "demo@lokap.vn" to "lokap123"
    )

    private val takenEmails = mutableSetOf(
        "demo@lokap.vn",
        "taken@lokap.vn"
    )

    private val takenUsernames = mutableSetOf(
        "lokap",
        SampleData.currentUser.username.lowercase()
    ).apply {
        SampleData.users.forEach { add(it.username.lowercase()) }
    }

    fun markOnboardingDone() {
        onboardingDone.value = true
    }

    suspend fun login(email: String, password: String): Result<User> {
        delay(800)
        if (simulateOffline || email.trim().equals("offline@lokap.vn", ignoreCase = true)) {
            return Result.failure(AuthStoreException.NetworkError())
        }
        val normalizedEmail = email.trim().lowercase()
        val expectedPassword = registeredCredentials[normalizedEmail]
        if (expectedPassword == null || expectedPassword != password) {
            return Result.failure(AuthStoreException.InvalidCredentials())
        }
        val user = if (normalizedEmail == "demo@lokap.vn") {
            SampleData.currentUser
        } else {
            User(
                id = UUID.randomUUID().toString(),
                username = normalizedEmail.substringBefore("@"),
                displayName = normalizedEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                avatarUrl = "https://picsum.photos/seed/lokap_1/200/200"
            )
        }
        currentUser.value = user
        return Result.success(user)
    }

    suspend fun register(email: String, password: String): Result<User> {
        delay(800)
        if (simulateOffline || email.trim().equals("offline@lokap.vn", ignoreCase = true)) {
            return Result.failure(AuthStoreException.NetworkError())
        }
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail in takenEmails) {
            return Result.failure(AuthStoreException.EmailAlreadyTaken())
        }
        registeredCredentials[normalizedEmail] = password
        takenEmails.add(normalizedEmail)
        val incompleteUser = User(
            id = UUID.randomUUID().toString(),
            username = "",
            displayName = "",
            avatarUrl = null
        )
        currentUser.value = incompleteUser
        return Result.success(incompleteUser)
    }

    suspend fun loginWithGoogle(): Result<User> {
        delay(800)
        if (simulateOffline) {
            return Result.failure(AuthStoreException.NetworkError())
        }
        val user = SampleData.currentUser
        currentUser.value = user
        return Result.success(user)
    }

    suspend fun logout() {
        delay(800)
        currentUser.value = null
    }

    suspend fun isUsernameAvailable(username: String): Result<Boolean> {
        delay(250)
        if (simulateOffline) {
            return Result.failure(IOException("Offline"))
        }
        val normalized = username.trim().lowercase()
        return Result.success(normalized !in takenUsernames)
    }

    suspend fun completeProfile(username: String, displayName: String): Result<User> {
        return completeProfile(username = username, displayName = displayName, avatarUrl = null)
    }

    suspend fun completeProfile(
        username: String,
        displayName: String,
        avatarUrl: String?
    ): Result<User> {
        delay(800)
        if (simulateOffline) {
            return Result.failure(AuthStoreException.NetworkError())
        }
        val normalizedUsername = username.trim().lowercase()
        if (normalizedUsername in takenUsernames) {
            return Result.failure(AuthStoreException.UsernameAlreadyTaken())
        }
        takenUsernames.add(normalizedUsername)
        val existing = currentUser.value
        val updated = User(
            id = existing?.id ?: UUID.randomUUID().toString(),
            username = normalizedUsername,
            displayName = displayName.trim(),
            avatarUrl = avatarUrl ?: existing?.avatarUrl
        )
        currentUser.value = updated
        return Result.success(updated)
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        delay(800)
        if (simulateOffline || email.trim().equals("offline@lokap.vn", ignoreCase = true)) {
            return Result.failure(AuthStoreException.NetworkError())
        }
        return Result.success(Unit)
    }
}
