package vn.nhom2.lokap

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import dagger.hilt.EntryPoints
import java.io.File
import java.util.UUID
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.PostLocation
import vn.nhom2.lokap.domain.model.PostSendState
import vn.nhom2.lokap.domain.model.PublishDelay
import vn.nhom2.lokap.domain.repository.AuthRepository
import vn.nhom2.lokap.domain.repository.PostDraft
import vn.nhom2.lokap.domain.repository.PostRepository
import vn.nhom2.lokap.domain.repository.SettingsRepository

/**
 * Explicitly opt in with instrumentation argument liveFirebase=true.
 * Sign in to a verified, completed TEST account before running. The post test requires no friends.
 * Calls the app's real repositories; all persistence assertions read Source.SERVER, never Room/cache.
 * Deletes its own post and restores profile/settings. The uploaded Cloudinary fixture remains because
 * the app's unsigned upload configuration cannot delete remote images.
 */
@RunWith(AndroidJUnit4::class)
class LiveFirebaseCrudTest {
    private lateinit var db: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var auth: AuthRepository
    private lateinit var posts: PostRepository
    private lateinit var settings: SettingsRepository
    private lateinit var uid: String

    @Before
    fun requireLiveTestSession() = runBlocking {
        assumeTrue(
            "Live Firebase tests require explicit -e liveFirebase true",
            InstrumentationRegistry.getArguments().getString("liveFirebase") == "true"
        )
        assertTrue("Firebase emulator must be disabled", BuildConfig.FIREBASE_EMULATOR_HOST.isBlank())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val graph = EntryPoints.get(context.applicationContext, LiveFirebaseTestEntryPoint::class.java)
        db = graph.firestore()
        firebaseAuth = graph.firebaseAuth()
        auth = graph.authRepository()
        posts = graph.postRepository()
        settings = graph.settingsRepository()
        assertEquals("Unexpected Firebase project", BuildConfig.FIREBASE_PROJECT_ID, db.app.options.projectId)
        assertEquals("Tests must use hosted Firestore", "firestore.googleapis.com", db.firestoreSettings.host)
        val user = firebaseAuth.currentUser
            ?: throw AssertionError("Sign in to a verified test account in the app before running live tests")
        withTimeout(30_000) {
            user.reload().await()
            assertTrue("Test account must have a verified email", firebaseAuth.currentUser?.isEmailVerified == true)
            user.getIdToken(true).await()
            uid = user.uid
            val profile = auth.currentUser.filterNotNull().first { it.id == uid }
            assertTrue("Complete the test account profile first", profile.username.isNotBlank())
            assertTrue("Profile must exist on Firebase", server(db.collection("users").document(uid)).exists())
        }
        checkpoint("Connected to real Firebase project ${db.app.options.projectId}")
    }

    @Test
    fun postCreateReadUpdateReactDeleteOnServer() = runBlocking {
        withTimeout(30_000) {
            val friends = db.collection("friendships").whereArrayContains("users", uid)
                .get(Source.SERVER).await()
            assertTrue(
                "Use an isolated test account without accepted friendships to avoid sharing test content",
                friends.documents.none { it.getString("status") == "accepted" }
            )
        }
        val marker = "LIVE-${UUID.randomUUID().toString().take(8)}"
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val fixture = File(context.cacheDir, "$marker.jpg")
        val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(Color.rgb(41, 89, 128))
            fixture.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it)) }
        } finally {
            bitmap.recycle()
        }
        var postId: String? = null
        var deleted = false
        var failure: Throwable? = null
        try {
            withTimeout(180_000) {
                postId = posts.createPost(
                    PostDraft(
                        imagePath = fixture.absolutePath,
                        caption = marker,
                        audience = Audience.FRIENDS,
                        location = PostLocation(LocationLevel.COUNTRY, null, null, null, null, null, null, null, null),
                        placeId = null,
                        publishDelay = PublishDelay.NOW
                    )
                ).getOrThrow()
                val id = requireNotNull(postId)
                checkpoint("Created local upload request for posts/$id; awaiting server confirmation")
                val sent = posts.observePost(id).filterNotNull().first { it.sendState != PostSendState.PENDING }
                assertEquals("Upload must finish successfully before CRUD assertions", PostSendState.SENT, sent.sendState)
                val ref = db.collection("posts").document(id)
                val created = server(ref)
                assertTrue("Create must persist a real Firestore document", created.exists())
                assertEquals(uid, created.getString("authorId"))
                assertEquals(marker, created.getString("caption"))
                assertEquals("friends", created.getString("audience"))
                assertEquals(listOf(uid), created.get("viewerIds"))
                assertEquals("country", created.getString("locationLevel"))
                assertNull(created.get("displayLat"))
                assertNull(created.get("displayLng"))
                assertTrue(created.getString("imageUrl").orEmpty().startsWith("https://res.cloudinary.com/"))
                assertNotNull(created.getTimestamp("createdAt"))
                checkpoint("PASS create/read: posts/$id confirmed by Source.SERVER")

                posts.refreshFeeds().getOrThrow()
                assertTrue("Refreshed friend feed must contain the created post", posts.friendFeed.first().any { it.id == id })
                posts.refreshUserPosts(uid).getOrThrow()
                assertTrue(
                    "Refreshed profile must contain the created post",
                    posts.observeUserPosts(uid).first().any { it.id == id }
                )
                checkpoint("PASS list queries: feed and profile refreshes succeeded and include posts/$id")

                val edited = "$marker edited"
                posts.updateCaption(id, edited).getOrThrow()
                assertEquals(edited, server(ref).getString("caption"))
                posts.updateCaption(id, "x".repeat(60)).getOrThrow()
                assertEquals("x".repeat(60), server(ref).getString("caption"))
                assertTrue("Caption longer than 60 characters must fail", posts.updateCaption(id, "x".repeat(61)).isFailure)
                assertEquals("Rejected update must not change Firebase", "x".repeat(60), server(ref).getString("caption"))
                posts.updateCaption(id, null).getOrThrow()
                assertNull(server(ref).get("caption"))
                checkpoint("PASS update: edited, 60-character limit, rejected 61 characters, cleared caption")

                posts.toggleReaction(id, "❤️").getOrThrow()
                assertEquals("❤️", reactions(server(ref))[uid])
                posts.toggleReaction(id, "😊").getOrThrow()
                assertEquals("😊", reactions(server(ref))[uid])
                posts.toggleReaction(id, "😊").getOrThrow()
                assertFalse(reactions(server(ref)).containsKey(uid))
                checkpoint("PASS reaction: added, changed, removed on server")

                posts.deletePost(id).getOrThrow()
                assertFalse("Delete must remove the Firestore document", server(ref).exists())
                assertNull(posts.observePost(id).first())
                deleted = true
                checkpoint("PASS delete: posts/$id absent from Source.SERVER and app cache")
            }
        } catch (error: Throwable) {
            failure = error
            throw error
        } finally {
            cleanup(failure) {
                val id = postId
                if (id != null && !deleted) {
                    posts.deletePost(id).getOrThrow()
                    db.waitForPendingWrites().await()
                    val ref = db.collection("posts").document(id)
                    if (server(ref).exists()) posts.deletePost(id).getOrThrow()
                    assertFalse("Test post cleanup failed: posts/$id", server(ref).exists())
                    checkpoint("Cleaned up test post posts/$id")
                }
            }
            fixture.delete()
        }
    }

    @Test
    fun profileUpdateAndRestoreOnServer() = runBlocking {
        val ref = db.collection("users").document(uid)
        val before = withTimeout(30_000) { server(ref) }
        val originalName = requireNotNull(before.getString("displayName"))
        val originalUsername = requireNotNull(before.getString("username"))
        val originalBio = requireNotNull(before.getString("bio"))
        val marker = UUID.randomUUID().toString().take(8)
        var failure: Throwable? = null
        try {
            withTimeout(30_000) {
                auth.updateProfile("Live test $marker", originalUsername, "CRUD test $marker").getOrThrow()
                val changed = server(ref)
                assertEquals("Live test $marker", changed.getString("displayName"))
                assertEquals("CRUD test $marker", changed.getString("bio"))
                assertEquals(originalUsername, changed.getString("username"))
                assertEquals(uid, server(db.collection("usernames").document(originalUsername)).getString("uid"))
                checkpoint("PASS profile update: name and bio confirmed on server; username reservation intact")
            }
        } catch (error: Throwable) {
            failure = error
            throw error
        } finally {
            cleanup(failure) {
                auth.updateProfile(originalName, originalUsername, originalBio).getOrThrow()
                restoreFields(ref, before, listOf("displayName", "username", "bio", "searchName"))
                assertEquals("Profile must be restored exactly", before.data, server(ref).data)
                checkpoint("Restored original profile on server")
            }
        }
    }

    @Test
    fun settingsUpdateAndRestoreOnServer() = runBlocking {
        val ref = db.collection("users").document(uid).collection("private").document("profile")
        val before = withTimeout(30_000) { server(ref) }
        assertTrue("Private profile must exist before testing", before.exists())
        withTimeout(30_000) { settings.refresh().getOrThrow() }
        val original = settings.settings.value
        var failure: Throwable? = null
        try {
            withTimeout(30_000) {
                settings.update { it.copy(notifyReactions = !original.notifyReactions) }.getOrThrow()
                assertEquals(!original.notifyReactions, server(ref).getBoolean("notifyReactions"))
                checkpoint("PASS settings update: notification preference confirmed on server")
            }
        } catch (error: Throwable) {
            failure = error
            throw error
        } finally {
            cleanup(failure) {
                settings.update { original }.getOrThrow()
                restoreFields(ref, before, SETTINGS_FIELDS)
                assertEquals("Settings must be restored, including originally absent fields", before.data, server(ref).data)
                settings.refresh().getOrThrow()
                checkpoint("Restored exact original private profile/settings on server")
            }
        }
    }

    private suspend fun server(ref: DocumentReference): DocumentSnapshot {
        val snapshot = ref.get(Source.SERVER).await()
        assertFalse("Read must come from Firebase server: ${ref.path}", snapshot.metadata.isFromCache)
        assertFalse("Unacknowledged local writes cannot prove persistence: ${ref.path}", snapshot.metadata.hasPendingWrites())
        return snapshot
    }

    private fun reactions(snapshot: DocumentSnapshot): Map<*, *> =
        snapshot.get("reactions") as? Map<*, *> ?: error("Missing server reactions map")

    private suspend fun restoreFields(ref: DocumentReference, before: DocumentSnapshot, fields: List<String>) {
        ref.update(fields.associateWith { field ->
            if (before.contains(field)) before.get(field) else FieldValue.delete()
        }).await()
    }

    private suspend fun cleanup(failure: Throwable?, action: suspend () -> Unit) {
        try {
            withContext(NonCancellable) { withTimeout(45_000) { action() } }
        } catch (cleanupError: Throwable) {
            checkpoint("CLEANUP FAILED: ${cleanupError.message}")
            if (failure != null) failure.addSuppressed(cleanupError) else throw cleanupError
        }
    }

    private fun checkpoint(message: String) {
        InstrumentationRegistry.getInstrumentation().sendStatus(
            0, Bundle().apply { putString("stream", "\nLIVE_FIREBASE: $message\n") }
        )
    }

    private companion object {
        val SETTINGS_FIELDS = listOf(
            "defaultLocationLevel", "notifyNewPhotos", "notifyReactions", "notifyMessages", "notifyFriendRequests"
        )
    }
}
