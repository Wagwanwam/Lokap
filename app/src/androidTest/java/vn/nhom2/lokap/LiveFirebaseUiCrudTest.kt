package vn.nhom2.lokap

import android.Manifest
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import dagger.hilt.EntryPoints
import java.io.File
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

/**
 * Visible end-to-end UI test. Every CRUD mutation comes from clicking the real app screens.
 * Firebase is used ONLY to read Source.SERVER and verify persistence; no repository writes.
 * Opt in: liveFirebaseUi=true, testEmail=<signed-in verified test account>.
 * Resume a named test fixture with both resumeLivePostId and resumeLiveCaption to skip creation.
 * liveUiPauseMs (default 45000, max 60000) keeps created/edited results visible for review.
 * Screenshots: Android/data/vn.nhom2.lokap/files/live-ui-crud/UI-LIVE-<run>/.
 */
@RunWith(AndroidJUnit4::class)
class LiveFirebaseUiCrudTest {
    // Keep resumed CameraX effects on the Compose test UI dispatcher, not the CameraExecutor thread.
    private val compose = createAndroidComposeRule<MainActivity>(effectContext = StandardTestDispatcher())
    private val args get() = InstrumentationRegistry.getArguments()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val marker = "UI-LIVE-${UUID.randomUUID().toString().take(8)}"
    private val pauseMs get() = args.getString("liveUiPauseMs")?.toLongOrNull()?.coerceIn(0, 60_000) ?: 45_000L

    // Grant camera permission before MainActivity starts, so the actual CameraX view can initialize.
    private val prepare = TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                assumeTrue("Explicit live UI test opt-in required", args.getString("liveFirebaseUi") == "true")
                if (Build.VERSION.SDK_INT >= 28) {
                    instrumentation.uiAutomation.grantRuntimePermission(BuildConfig.APPLICATION_ID, Manifest.permission.CAMERA)
                } else {
                    instrumentation.uiAutomation.executeShellCommand(
                        "pm grant ${BuildConfig.APPLICATION_ID} ${Manifest.permission.CAMERA}"
                    ).close()
                }
                base.evaluate()
            }
        }
    }

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(prepare).around(compose)

    @Test
    fun cameraCaptionEditDeleteThroughUi() {
        val resumeId = args.getString("resumeLivePostId")
        val resumeCaption = args.getString("resumeLiveCaption")
        assertTrue("Supply both resumeLivePostId and resumeLiveCaption, or neither", (resumeId == null) == (resumeCaption == null))
        if (resumeId != null) {
            assertTrue("Resume requires a single UUID post ID", resumeId.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
            assertTrue("Only a specifically named UI-LIVE test fixture may be resumed", resumeCaption!!.matches(Regex("UI-LIVE-[0-9a-f]{8}( da sua)?")))
        }
        val marker = resumeCaption ?: this.marker
        val editedCaption = "${marker.removeSuffix(" da sua")} da sua"
        assertTrue("Firebase emulator must be disabled", BuildConfig.FIREBASE_EMULATOR_HOST.isBlank())
        val graph = EntryPoints.get(instrumentation.targetContext.applicationContext, LiveFirebaseTestEntryPoint::class.java)
        val db = graph.firestore()
        val user = graph.firebaseAuth().currentUser ?: throw AssertionError("Sign in to the verified test account first")
        val email = args.getString("testEmail")?.trim().orEmpty()
        assertTrue("Pass the authorized test account's email as testEmail", email.isNotEmpty())
        assertEquals("Refusing to test a different signed-in account", email, user.email)
        assertTrue("Verify the test account email first", user.isEmailVerified)
        assertEquals(BuildConfig.FIREBASE_PROJECT_ID, db.app.options.projectId)
        assertEquals("firestore.googleapis.com", db.firestoreSettings.host)
        val profileBefore = readServer(db.collection("users").document(user.uid)).data
        assertNotNull("Complete the test profile before UI CRUD", profileBefore)
        runBlocking {
            withTimeout(30_000) {
                val friendships = db.collection("friendships").whereArrayContains("users", user.uid).get(Source.SERVER).await()
                assertFalse(friendships.metadata.isFromCache)
                assertTrue("Use a test account without friends to avoid sharing the test photo", friendships.none { it.getString("status") == "accepted" })
            }
        }

        var createdRef: DocumentReference? = null
        var deleted = false
        try {
            val created = if (resumeId != null) {
                readServer(db.collection("posts").document(resumeId))
            } else {
                val cameraTab = hasContentDescription("Chụp ảnh") and hasClickAction()
                compose.waitUntil(30_000) { compose.onAllNodes(cameraTab).fetchSemanticsNodes().isNotEmpty() }
                compose.onNode(cameraTab).performClick()
                compose.waitUntil(45_000) {
                    val cameraError = "Máy ảnh đang bận hoặc không thể khởi tạo. Vui lòng thử lại."
                    if (compose.onAllNodesWithText(cameraError).fetchSemanticsNodes().isNotEmpty()) {
                        throw AssertionError("Camera UI reported: $cameraError")
                    }
                    compose.runOnUiThread {
                        findPreview(compose.activity.window.decorView)?.previewStreamState?.value == PreviewView.StreamState.STREAMING
                    }
                }
                screenshot("01-camera")
                val shutter = hasContentDescription("Chụp ảnh") and
                    SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
                // ShutterButton handles pointer input rather than a semantic OnClick action.
                compose.onNode(shutter).assertIsDisplayed().performTouchInput { click(center) }
                waitForText("Gửi đến…", 45_000)

                val captionField = compose.onNode(hasSetTextAction())
                captionField.performScrollTo().performTextReplacement(marker)
                assertEquals(marker, captionField.fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
                compose.onNodeWithText("Bạn bè").performScrollTo().performClick()
                                screenshot("02-composer")
                compose.onNodeWithContentDescription("Đăng").performScrollTo().assertIsEnabled().performClick()

                awaitCreatedPost(db, user.uid, marker)
            }
            assertTrue("The named UI test post must exist on the server", created.exists())
            assertEquals(marker, created.getString("caption"))
            assertEquals("friends", created.getString("audience"))
            assertEquals(user.uid, created.getString("authorId"))
            assertEquals(listOf(user.uid), created.get("viewerIds"))
            assertTrue(created.getString("imageUrl").orEmpty().startsWith("https://res.cloudinary.com/"))
            assertNotNull(created.getTimestamp("createdAt"))
            createdRef = created.reference
            val creationStatus = if (resumeId == null) "CREATED" else "RESUMED"
            checkpoint("SERVER_$creationStatus post=${created.reference.path} caption=$marker")

            compose.waitUntil(30_000) {
                compose.onAllNodesWithContentDescription("Hồ sơ").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription("Hồ sơ").performClick()
            compose.waitUntil(30_000) {
                compose.onAllNodes(hasScrollToIndexAction()).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasContentDescription(marker))
            compose.onNodeWithContentDescription(marker).assertIsDisplayed().performClick()
            waitForText("Chi tiết khoảnh khắc")
            waitForText(marker)
            compose.onNodeWithText(marker).performScrollTo().assertIsDisplayed()
            visibleCheckpoint("03-created", creationStatus, created.reference.path, marker)

            compose.onNodeWithContentDescription("Tùy chọn").performClick()
            compose.onNodeWithText("Sửa lời nhắn").performClick()
            val editField = compose.onNode(hasSetTextAction() and hasAnyAncestor(isDialog()))
            editField.performTextReplacement(editedCaption)
            assertEquals(editedCaption, editField.fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
            screenshot("04-edit-dialog")
            compose.onNodeWithText("Lưu").assertIsEnabled().performClick()
            awaitServer(created.reference, "updated caption") { it.getString("caption") == editedCaption }
            compose.waitUntil(20_000) { compose.onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty() }
            waitForText(editedCaption)
            compose.onNodeWithText(editedCaption).performScrollTo().assertIsDisplayed()
            visibleCheckpoint("05-edited", "EDITED", created.reference.path, editedCaption)

            compose.onNodeWithContentDescription("Tùy chọn").performClick()
            compose.onNodeWithText("Xoá bài").performClick()
            compose.onNodeWithText("Xoá bài này?").assertIsDisplayed()
            screenshot("06-delete-dialog")
            compose.onNodeWithText("Xoá").assertIsEnabled().performClick()
            awaitServer(created.reference, "deleted document") { !it.exists() }
            deleted = true
            compose.waitUntil(20_000) {
                compose.onAllNodesWithText("Chi tiết khoảnh khắc").fetchSemanticsNodes().isEmpty()
            }
            assertTrue(compose.onAllNodesWithContentDescription(marker).fetchSemanticsNodes().isEmpty())
            assertTrue(compose.onAllNodesWithContentDescription(editedCaption).fetchSemanticsNodes().isEmpty())
            assertEquals("UI post CRUD must not edit the user profile", profileBefore, readServer(db.collection("users").document(user.uid)).data)
            visibleCheckpoint("07-deleted", "DELETED", created.reference.path, "absent from Source.SERVER")
        } catch (error: Throwable) {
            screenshot("failure")
            checkpoint("FAILED marker=$marker post=${createdRef?.path ?: "unknown; look up the unique caption"}: ${error.message}")
            throw error
        } finally {
            if (createdRef != null && !deleted) {
                // Do not bypass a failing UI by deleting through a repository or direct Firestore write.
                checkpoint("UI_CLEANUP_REQUIRED post=${createdRef.path} marker=$marker")
            }
        }
    }

    /** Explicitly requested retained sample, run separately after the full create/edit/delete test. */
    @Test
    fun createVisibleSampleThroughUi() {
        assumeTrue("Keeping a sample requires a separate explicit opt-in", args.getString("keepLiveSample") == "true")
        assertTrue("Firebase emulator must be disabled", BuildConfig.FIREBASE_EMULATOR_HOST.isBlank())
        val graph = EntryPoints.get(instrumentation.targetContext.applicationContext, LiveFirebaseTestEntryPoint::class.java)
        val db = graph.firestore()
        val user = graph.firebaseAuth().currentUser ?: throw AssertionError("Sign in to the verified test account first")
        val email = args.getString("testEmail")?.trim().orEmpty()
        assertTrue("Pass the authorized test account's email as testEmail", email.isNotEmpty())
        assertEquals("Refusing to create a sample for a different account", email, user.email)
        assertTrue("Verify the test account email first", user.isEmailVerified)
        assertEquals(BuildConfig.FIREBASE_PROJECT_ID, db.app.options.projectId)
        assertEquals("firestore.googleapis.com", db.firestoreSettings.host)
        val profileBefore = readServer(db.collection("users").document(user.uid)).data
        assertNotNull("Complete the test profile first", profileBefore)
        runBlocking {
            withTimeout(30_000) {
                val ownPosts = db.collection("posts").whereEqualTo("authorId", user.uid).get(Source.SERVER).await()
                assertFalse(ownPosts.metadata.isFromCache)
                assertFalse(ownPosts.metadata.hasPendingWrites())
                assertTrue("Complete CRUD deletion first; keeping a sample must not create additional posts", ownPosts.isEmpty)
                val friends = db.collection("friendships").whereArrayContains("users", user.uid).get(Source.SERVER).await()
                assertFalse(friends.metadata.isFromCache)
                assertTrue("Use a test account without friends", friends.none { it.getString("status") == "accepted" })
            }
        }
        val sampleCaption = "Bai mau Firebase - giu lai UI-${marker.substringAfter("UI-LIVE-")}"
        var sampleRef: DocumentReference? = null
        try {
            val cameraTab = hasContentDescription("Chụp ảnh") and hasClickAction()
            compose.waitUntil(30_000) { compose.onAllNodes(cameraTab).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(cameraTab).performClick()
            compose.waitUntil(45_000) {
                val error = "Máy ảnh đang bận hoặc không thể khởi tạo. Vui lòng thử lại."
                if (compose.onAllNodesWithText(error).fetchSemanticsNodes().isNotEmpty()) throw AssertionError(error)
                compose.runOnUiThread {
                    findPreview(compose.activity.window.decorView)?.previewStreamState?.value == PreviewView.StreamState.STREAMING
                }
            }
            val shutter = hasContentDescription("Chụp ảnh") and
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
            compose.onNode(shutter).assertIsDisplayed().performTouchInput { click(center) }
            waitForText("Gửi đến…", 45_000)
            val captionField = compose.onNode(hasSetTextAction())
            captionField.performScrollTo().performTextReplacement(sampleCaption)
            assertEquals(sampleCaption, captionField.fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
            compose.onNodeWithText("Bạn bè").performScrollTo().performClick()
                        screenshot("sample-01-composer")
            compose.onNodeWithContentDescription("Đăng").performScrollTo().assertIsEnabled().performClick()

            val sample = awaitCreatedPost(db, user.uid, sampleCaption)
            sampleRef = sample.reference
            assertEquals(sampleCaption, sample.getString("caption"))
            assertEquals(user.uid, sample.getString("authorId"))
            assertEquals("friends", sample.getString("audience"))
            assertEquals(listOf(user.uid), sample.get("viewerIds"))
            assertTrue(sample.getString("imageUrl").orEmpty().startsWith("https://res.cloudinary.com/"))
            assertNotNull(sample.getTimestamp("createdAt"))
            checkpoint("SAMPLE_SERVER_CREATED post=${sample.reference.path} caption=$sampleCaption")

            compose.waitUntil(30_000) { compose.onAllNodesWithContentDescription("Hồ sơ").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Hồ sơ").performClick()
            compose.waitUntil(30_000) { compose.onAllNodes(hasScrollToIndexAction()).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasContentDescription(sampleCaption))
            screenshot("sample-02-profile")
            compose.onNodeWithContentDescription(sampleCaption).assertIsDisplayed().performClick()
            waitForText("Chi tiết khoảnh khắc")
            waitForText(sampleCaption)
            compose.onNodeWithText(sampleCaption).performScrollTo().assertIsDisplayed()
            assertEquals("Sample creation must leave the user profile unchanged", profileBefore, readServer(db.collection("users").document(user.uid)).data)
            visibleCheckpoint("sample-03-kept", "SAMPLE_KEPT", sample.reference.path, sampleCaption)
        } catch (error: Throwable) {
            screenshot("sample-failure")
            checkpoint("SAMPLE_FAILED post=${sampleRef?.path ?: "unknown"} caption=$sampleCaption; no backend cleanup: ${error.message}")
            throw error
        }
    }

    private fun awaitCreatedPost(db: FirebaseFirestore, uid: String, caption: String = marker): DocumentSnapshot = runBlocking {
        withTimeout(150_000) {
            var found: DocumentSnapshot? = null
            while (found == null) {
                val snapshot = db.collection("posts").whereEqualTo("authorId", uid).get(Source.SERVER).await()
                assertFalse("Creation lookup must come from the server", snapshot.metadata.isFromCache)
                if (snapshot.metadata.hasPendingWrites()) {
                    // Source.SERVER may still merge pending local writes; wait for acknowledgement.
                    delay(1_000)
                    continue
                }
                val matches = snapshot.documents.filter { it.getString("caption") == caption }
                assertTrue("The UI must create exactly one post per submission", matches.size <= 1)
                found = matches.singleOrNull()
                if (found == null) delay(1_000)
            }
            found
        }
    }

    private fun awaitServer(ref: DocumentReference, expected: String, predicate: (DocumentSnapshot) -> Boolean) {
        runBlocking {
            withTimeout(45_000) {
                while (true) {
                    val snapshot = ref.get(Source.SERVER).await()
                    assertFalse("Expected $expected from server", snapshot.metadata.isFromCache)
                    if (!snapshot.metadata.hasPendingWrites() && predicate(snapshot)) break
                    delay(750)
                }
            }
        }
    }

    private fun readServer(ref: DocumentReference): DocumentSnapshot = runBlocking {
        withTimeout(30_000) {
            ref.get(Source.SERVER).await().also {
                assertFalse(it.metadata.isFromCache)
                assertFalse(it.metadata.hasPendingWrites())
            }
        }
    }

    private fun waitForText(text: String, timeoutMs: Long = 30_000) {
        compose.waitUntil(timeoutMs) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun findPreview(view: View): PreviewView? {
        if (view is PreviewView) return view
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) findPreview(view.getChildAt(index))?.let { return it }
        }
        return null
    }

    private fun visibleCheckpoint(stage: String, status: String, path: String, value: String) {
        screenshot(stage)
        val holdMs = if (status == "DELETED") 0L else pauseMs
        checkpoint("$status post=$path value=$value pauseMs=$holdMs")
        if (holdMs > 0) Thread.sleep(holdMs)
    }

    private fun screenshot(stage: String) {
        runCatching {
            val context = instrumentation.targetContext
            val directory = File(context.getExternalFilesDir(null) ?: context.filesDir, "live-ui-crud/$marker").apply { mkdirs() }
            val file = File(directory, "$stage.png")
            val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
            try {
                file.outputStream().use { output -> check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) }
            } finally {
                bitmap.recycle()
            }
            checkpoint("SCREENSHOT ${file.absolutePath}")
        }.onFailure { checkpoint("SCREENSHOT_UNAVAILABLE stage=$stage reason=${it.message}") }
    }

    private fun checkpoint(message: String) {
        instrumentation.sendStatus(0, Bundle().apply { putString("stream", "\nLIVE_UI_CRUD: $message\n") })
    }
}
