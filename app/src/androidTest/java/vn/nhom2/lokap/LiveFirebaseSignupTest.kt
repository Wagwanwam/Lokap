package vn.nhom2.lokap

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.state.ToggleableState
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Source
import dagger.hilt.EntryPoints
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.nhom2.lokap.feature.auth.AuthValidators
import vn.nhom2.lokap.domain.repository.ProfileDraft

/**
 * Opt-in UI registration against the configured real Firebase project.
 * Runtime arguments: liveFirebaseSignup=true, testEmail, testPassword.
 * The supplied email must belong to the consenting tester. Leaves the account signed in for
 * verification/profile setup; never deletes it or prints the password.
 */
@RunWith(AndroidJUnit4::class)
class LiveFirebaseSignupTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun registerTestAccountThroughApp() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("Explicit signup opt-in required", args.getString("liveFirebaseSignup") == "true")
        assertTrue("Firebase emulator must be disabled", BuildConfig.FIREBASE_EMULATOR_HOST.isBlank())
        val email = args.getString("testEmail")?.trim().orEmpty()
        val password = args.getString("testPassword").orEmpty()
        assertNull("A valid testEmail runtime argument is required", AuthValidators.validateEmail(email))
        assertNull("A valid testPassword runtime argument is required", AuthValidators.validatePassword(password))
        val firebase = FirebaseAuth.getInstance()
        assertEquals(BuildConfig.FIREBASE_PROJECT_ID, firebase.app.options.projectId)
        assertNull("Sign out of any existing account before the signup test", firebase.currentUser)

        compose.waitUntil(30_000) {
            visible("Tạo tài khoản Lokap") || visible("Đăng ký ngay")
        }
        if (!visible("Tạo tài khoản Lokap")) {
            compose.onNodeWithText("Đăng ký ngay").performScrollTo().performClick()
        }
        compose.waitUntil(20_000) {
            compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size == 3
        }
        val fields = compose.onAllNodes(hasSetTextAction())
        fields[0].performTextReplacement(email)
        fields[1].performTextReplacement(password)
        fields[2].performTextReplacement(password)
        // Check the actual editable email immediately before submission. A bad address must never
        // trigger creation or a verification email, even on a slow device.
        val enteredEmail = fields[0].fetchSemanticsNode().config[SemanticsProperties.EditableText].text
        assertEquals("The email in the form must match the authorized address", email, enteredEmail)

        val terms = compose.onNode(isToggleable())
        terms.performScrollTo()
        if (terms.fetchSemanticsNode().config[SemanticsProperties.ToggleableState] != ToggleableState.On) {
            terms.performClick()
        }
        terms.assertIsOn()
        compose.onNodeWithText("Tiếp tục thiết lập hồ sơ").performScrollTo().performClick()
        compose.waitUntil(60_000) { visible("Kiểm tra email") || visible("Thử lại") }
        assertTrue("Registration returned a server error; inspect the app's displayed error", visible("Kiểm tra email"))
        compose.onNodeWithText("Kiểm tra email").assertIsDisplayed()
        assertEquals("Firebase must contain the requested signed-in account", email, firebase.currentUser?.email)
    }

    /** Run after email verification; pass testPassword to sign in when the device has no session. */
    @Test
    fun completeVerifiedTestProfile() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("Explicit profile setup opt-in required", args.getString("liveFirebaseProfile") == "true")
        assertTrue("Firebase emulator must be disabled", BuildConfig.FIREBASE_EMULATOR_HOST.isBlank())
        val email = args.getString("testEmail")?.trim().orEmpty()
        assertNull("A valid testEmail runtime argument is required", AuthValidators.validateEmail(email))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val graph = EntryPoints.get(context.applicationContext, LiveFirebaseTestEntryPoint::class.java)
        val firebase = graph.firebaseAuth()
        val db = graph.firestore()
        val auth = graph.authRepository()
        assertEquals(BuildConfig.FIREBASE_PROJECT_ID, db.app.options.projectId)
        assertEquals("firestore.googleapis.com", db.firestoreSettings.host)
        withTimeout(60_000) {
            if (firebase.currentUser == null) {
                val password = args.getString("testPassword").orEmpty()
                assertTrue("testPassword is required when the test account is not signed in", password.isNotEmpty())
                auth.login(email, password).getOrThrow()
            }
            val user = firebase.currentUser ?: throw AssertionError("The verified test account must be signed in")
            assertEquals("Profile setup must use the authorized account", email, user.email)
            auth.confirmEmailVerified().getOrThrow()
            assertTrue("Email must be verified before creating a profile", firebase.currentUser?.isEmailVerified == true)
            val ref = db.collection("users").document(user.uid)
            val existing = ref.get(Source.SERVER).await()
            assertFalse(existing.metadata.isFromCache)
            assertFalse(existing.metadata.hasPendingWrites())
            if (!existing.exists()) {
                auth.completeProfile(
                    ProfileDraft(
                        username = "crud_${UUID.randomUUID().toString().take(8)}",
                        displayName = "Firebase Test",
                        avatarUrl = null,
                        bio = "Kiem thu CRUD Firebase"
                    )
                ).getOrThrow()
            }
            val publicProfile = ref.get(Source.SERVER).await()
            assertTrue("Public profile must exist on Firebase", publicProfile.exists())
            assertFalse(publicProfile.metadata.isFromCache)
            assertFalse(publicProfile.metadata.hasPendingWrites())
            if (existing.exists()) {
                assertEquals("An existing profile must remain unchanged", existing.data, publicProfile.data)
            }
            val username = requireNotNull(publicProfile.getString("username"))
            val reservation = db.collection("usernames").document(username).get(Source.SERVER).await()
            assertEquals(user.uid, reservation.getString("uid"))
            assertFalse(reservation.metadata.isFromCache)
            assertFalse(reservation.metadata.hasPendingWrites())
            val privateProfile = ref.collection("private").document("profile").get(Source.SERVER).await()
            assertEquals(email, privateProfile.getString("email"))
            assertFalse(privateProfile.metadata.isFromCache)
            assertFalse(privateProfile.metadata.hasPendingWrites())
        }
    }

    private fun visible(text: String): Boolean =
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
}
