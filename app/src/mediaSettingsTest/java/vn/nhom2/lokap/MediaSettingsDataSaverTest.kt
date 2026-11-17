package vn.nhom2.lokap

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.decode.DataSource
import coil3.decode.Decoder
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Size
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Opt-in, read-only network check on the signed-in account's existing images. Never uploads/posts. */
@RunWith(AndroidJUnit4::class)
class MediaSettingsDataSaverTest {
    @Test
    fun productionImageLoaderDownloadsSmallerImagesAndSeparatesCache() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveMediaSettings") == "true")
        val context = instrumentation.targetContext
        val app = context.applicationContext as LokapApp
        val settings = app.deviceSettingsRepository
        val before = settings.settings.value.dataSaverEnabled
        lateinit var loader: ImageLoader
        instrumentation.runOnMainSync { loader = SingletonImageLoader.get(context) }
        val user = FirebaseAuth.getInstance().currentUser ?: error("Sign in before this opt-in check")
        val posts = withTimeout(30_000) {
            FirebaseFirestore.getInstance().collection("posts")
                .whereEqualTo("authorId", user.uid).limit(3).get(Source.SERVER).await()
        }
        val urls = posts.mapNotNull { it.getString("imageUrl") }
            .filter { it.startsWith("https://res.cloudinary.com/") && it.contains("/image/upload/v") }
        assertTrue("Need existing Cloudinary post photos to measure", urls.isNotEmpty())
        val measurements = JSONArray()
        val cacheKey = "media-settings-test-${UUID.randomUUID()}"
        try {
            for (url in urls) {
                suspend fun fetch(enabled: Boolean): Pair<SuccessResult, Long> {
                    settings.setDataSaver(enabled)
                    var bytes = 0L
                    var fetchedFrom: DataSource? = null
                    val request = ImageRequest.Builder(context).data(url).size(Size.ORIGINAL)
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .diskCachePolicy(CachePolicy.DISABLED)
                        .decoderFactory(Decoder.Factory { fetched, _, _ ->
                            bytes = fetched.source.source().peek().readByteArray().size.toLong()
                            fetchedFrom = fetched.dataSource
                            null
                        }).build()
                    val result = withTimeout(45_000) { loader.execute(request) }
                    if (result is ErrorResult) throw AssertionError("Image download failed", result.throwable)
                    assertTrue(result is SuccessResult)
                    result as SuccessResult
                    assertEquals(DataSource.NETWORK, fetchedFrom)
                    assertEquals(DataSource.NETWORK, result.dataSource)
                    assertTrue(bytes > 0)
                    return result to bytes
                }
                val (full, fullBytes) = fetch(false)
                val (small, smallBytes) = fetch(true)
                assertEquals(url, full.request.data.toString())
                assertTrue(small.request.data.toString().contains("w_640,h_640"))
                assertTrue(small.image.width <= 640 && small.image.height <= 640)
                assertTrue("Savings must come from actual response bytes", smallBytes < fullBytes)
                measurements.put(JSONObject().apply {
                    put("image_sha256", MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
                        .joinToString("") { "%02x".format(it) })
                    put("full_bytes", fullBytes); put("saver_bytes", smallBytes)
                    put("full_width", full.image.width); put("full_height", full.image.height)
                    put("saver_width", small.image.width); put("saver_height", small.image.height)
                    put("data_source", "NETWORK")
                })
            }

            // A caller-supplied cache key must not let the small variant replace the original.
            val request = ImageRequest.Builder(context).data(urls.first()).size(Size.ORIGINAL)
                .memoryCacheKey(cacheKey).diskCachePolicy(CachePolicy.DISABLED).build()
            settings.setDataSaver(false)
            val full = loader.execute(request) as SuccessResult
            settings.setDataSaver(true)
            val small = loader.execute(request) as SuccessResult
            assertNotEquals(full.memoryCacheKey, small.memoryCacheKey)
            assertTrue(small.image.width <= 640 && small.image.height <= 640)
            settings.setDataSaver(false)
            val restored = loader.execute(request) as SuccessResult
            assertEquals(DataSource.MEMORY_CACHE, restored.dataSource)
            assertEquals(full.image.width, restored.image.width)
            assertEquals(full.image.height, restored.image.height)
            loader.memoryCache?.remove(full.memoryCacheKey!!)
            loader.memoryCache?.remove(small.memoryCacheKey!!)
            instrumentation.sendStatus(0, Bundle().apply {
                putString("stream", "\nMEDIA_DATA_SAVER=" + JSONObject().apply {
                    put("measurements", measurements)
                    put("cache_variants_separated", true)
                }.toString() + "\n")
            })
        } finally {
            settings.setDataSaver(before)
        }
    }
}
