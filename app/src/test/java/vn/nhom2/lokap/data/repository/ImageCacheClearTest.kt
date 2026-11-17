package vn.nhom2.lokap.data.repository

import coil3.ColorImage
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toOkioPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ImageCacheClearTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun `clear image caches preserves camera drafts pending uploads and session files`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val appDir = temporaryFolder.newFolder("app")
        val cameraDraft = appDir.resolve("cache/lokap_draft.jpg").apply {
            parentFile.mkdirs()
            writeText("camera draft")
        }
        val pendingUpload = appDir.resolve("files/pending_posts/post.jpg").apply {
            parentFile.mkdirs()
            writeText("pending upload")
        }
        val sessionFile = appDir.resolve("shared_prefs/session.xml").apply {
            parentFile.mkdirs()
            writeText("session")
        }
        val disk = DiskCache.Builder()
            .directory(appDir.resolve("cache/image_cache").toOkioPath())
            .maxSizeBytes(1024L * 1024)
            .cleanupCoroutineContext(dispatcher)
            .build()
        val memory = MemoryCache.Builder().maxSizeBytes(4096).build()
        val memoryKey = MemoryCache.Key("photo")
        memory[memoryKey] = MemoryCache.Value(ColorImage(size = 64))
        val editor = requireNotNull(disk.openEditor("photo"))
        disk.fileSystem.write(editor.metadata) { writeUtf8("image metadata") }
        disk.fileSystem.write(editor.data) { writeUtf8("downloaded image") }
        editor.commit()
        assertTrue(disk.size > 0)
        assertTrue(memory.keys.contains(memoryKey))

        try {
            clearImageCaches(memory, disk, dispatcher, dispatcher)

            assertNull(disk.openSnapshot("photo"))
            assertEquals(0L, disk.size)
            assertFalse(memory.keys.contains(memoryKey))
            assertEquals("camera draft", cameraDraft.readText())
            assertEquals("pending upload", pendingUpload.readText())
            assertEquals("session", sessionFile.readText())
            // Clearing must not shut down the shared cache: subsequent image requests can reuse it.
            val next = requireNotNull(disk.openEditor("next-photo"))
            disk.fileSystem.write(next.metadata) { writeUtf8("image metadata") }
            disk.fileSystem.write(next.data) { writeUtf8("next image") }
            next.commit()
            requireNotNull(disk.openSnapshot("next-photo")).close()
        } finally {
            disk.shutdown()
        }
    }

    @Test
    fun `clear also supports disabled caches`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        clearImageCaches(null, null, dispatcher, dispatcher)
    }
}
