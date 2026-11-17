package vn.nhom2.lokap

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import dagger.Lazy
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import vn.nhom2.lokap.data.local.SessionPrefs
import vn.nhom2.lokap.data.repository.FirestoreContentRepository
import vn.nhom2.lokap.data.repository.FirestoreGraph
import vn.nhom2.lokap.data.repository.FirestoreSocialRepository
import vn.nhom2.lokap.domain.model.User
import vn.nhom2.lokap.domain.repository.AuthRepository
import vn.nhom2.lokap.domain.repository.AuthSession
import vn.nhom2.lokap.domain.repository.PostRepository
import vn.nhom2.lokap.domain.repository.ProfileDraft
import vn.nhom2.lokap.domain.repository.RegisterResult

/** All identities and endpoints below belong only to the disposable local demo project. */
internal class SocialEmulatorFixture private constructor(
    private val context: Context,
    val app: FirebaseApp,
    val firebaseAuth: FirebaseAuth,
    val db: FirebaseFirestore,
    val user: User,
    private val preferencePrefix: String
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val session = EmulatorSession(user)
    private val lazyDb = Lazy { db }
    private val testContext = object : ContextWrapper(context) {
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            context.getSharedPreferences(preferencePrefix + name, mode)
    }
    private val graph = FirestoreGraph(lazyDb, SessionPrefs(testContext))
    private val unusedPosts = Lazy<PostRepository> { error("This test must not mutate posts") }
    val content = FirestoreContentRepository(lazyDb, session)
    val social = anotherSocialRepository()

    /** Separate mutation gate: exercises real transaction races across independent callers. */
    fun anotherSocialRepository() = FirestoreSocialRepository(lazyDb, session, graph, unusedPosts, scope)

    suspend fun close() {
        scope.cancel()
        firebaseAuth.signOut()
        runCatching { db.terminate().await() }
        app.delete()
        context.deleteSharedPreferences(preferencePrefix + "lokap_session")
    }

    companion object {
        const val PROJECT = "demo-lokap-social"
        const val FIRST_UID = "qasociala"
        const val SECOND_UID = "qasocialb"

        suspend fun open(context: Context, first: Boolean): SocialEmulatorFixture {
            require(PROJECT.startsWith("demo-"))
            val uid = if (first) FIRST_UID else SECOND_UID
            val email = if (first) "qa-a@example.test" else "qa-b@example.test"
            val name = "qa-social-$uid-${UUID.randomUUID()}"
            val options = FirebaseOptions.Builder().setProjectId(PROJECT)
                .setApplicationId("1:123:android:abc").setApiKey("fake-api-key").build()
            val app = FirebaseApp.initializeApp(context, options, name)
            check(app.name != FirebaseApp.DEFAULT_APP_NAME)
            val auth = FirebaseAuth.getInstance(app).apply { useEmulator("127.0.0.1", 19099) }
            val db = FirebaseFirestore.getInstance(app).apply {
                firestoreSettings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build()).build()
                useEmulator("127.0.0.1", 18080)
            }
            try {
                // This fixture password is public and valid only inside the local Auth Emulator.
                val signedIn = auth.signInWithEmailAndPassword(email, "TestSocial2026!").await().user
                    ?: error("Missing emulator fixture account")
                check(signedIn.uid == uid && signedIn.isEmailVerified)
                check(signedIn.getIdToken(true).await().claims["email_verified"] == true)
                check(db.app.options.projectId == PROJECT)
                check(db.firestoreSettings.host == "127.0.0.1:18080")
                return SocialEmulatorFixture(context, app, auth, db,
                    User(uid, uid, if (first) "QA Social A" else "QA Social B", null), "$name-")
            } catch (error: Throwable) {
                auth.signOut()
                runCatching { db.terminate().await() }
                app.delete()
                throw error
            }
        }
    }
}

/** Supplies the test identity to production repositories without using the real SessionPrefs. */
private class EmulatorSession(user: User) : AuthRepository {
    override val session = MutableStateFlow<AuthSession>(AuthSession.SignedIn(user))
    override val currentUser = MutableStateFlow<User?>(user)
    override val onboardingDone = MutableStateFlow(true)
    private fun <T> unsupported(): Result<T> = Result.failure(UnsupportedOperationException("Fixture identity is fixed"))
    override fun markOnboardingDone() = Unit
    override suspend fun login(email: String, password: String): Result<User> = unsupported()
    override suspend fun loginWithGoogle(idToken: String): Result<User> = unsupported()
    override suspend fun register(email: String, password: String): Result<RegisterResult> = unsupported()
    override suspend fun confirmEmailVerified(): Result<Unit> = unsupported()
    override suspend fun resendVerificationEmail(email: String): Result<Unit> = unsupported()
    override suspend fun isEmailAvailable(email: String): Result<Boolean> = unsupported()
    override suspend fun isUsernameAvailable(username: String): Result<Boolean> = unsupported()
    override suspend fun completeProfile(draft: ProfileDraft): Result<User> = unsupported()
    override suspend fun updateProfile(displayName: String, username: String, bio: String): Result<User> = unsupported()
    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = unsupported()
    override suspend fun resetPassword(email: String): Result<Unit> = unsupported()
    override suspend fun logout() = error("Do not change identities in a fixture")
    override suspend fun deleteAccount(): Result<Unit> = unsupported()
}
