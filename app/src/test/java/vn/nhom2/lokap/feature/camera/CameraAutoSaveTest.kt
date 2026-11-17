package vn.nhom2.lokap.feature.camera

import java.io.File
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import vn.nhom2.lokap.core.image.ImageProcessor
import vn.nhom2.lokap.data.fake.FakeAuthRepository
import vn.nhom2.lokap.data.fake.FakeSocialRepository
import vn.nhom2.lokap.feature.ContentTestRepository
import vn.nhom2.lokap.feature.TestDeviceSettingsRepository

@OptIn(ExperimentalCoroutinesApi::class)
class CameraAutoSaveTest {
    @get:Rule val temporaryFolder = TemporaryFolder()
    private val settings = TestDeviceSettingsRepository()
    private val gallery = TestPhotoGalleryRepository()

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun camera() = CameraViewModel(
        FakeAuthRepository(), FakeSocialRepository(), ContentTestRepository(), ImageProcessor(), settings, gallery
    ).apply { onAction(CameraAction.OnSyncPermissionGranted) }

    private fun TestScope.observe(camera: CameraViewModel) = mutableListOf<CameraEffect>().also { effects ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { camera.effects.collect { effects += it } }
    }

    private fun capture(camera: CameraViewModel, path: String) {
        camera.onAction(CameraAction.OnShutterClick)
        camera.onAction(CameraAction.OnPhotoSaved(path))
    }

    @Test fun `off keeps photo for composer without creating gallery copy`() = runTest {
        val camera = camera()
        val effects = observe(camera)
        capture(camera, "/capture/off.jpg")
        assertTrue(gallery.savedPaths.isEmpty())
        assertEquals(CameraEffect.NavigateToComposer("/capture/off.jpg"), effects.last())
        assertFalse(camera.uiState.value.isCapturing)
    }

    @Test fun `on exports each new capture once and reports success before composing`() = runTest {
        settings.setAutoSave(true)
        val camera = camera()
        val effects = observe(camera)
        capture(camera, "/capture/first.jpg")
        camera.onAction(CameraAction.OnPhotoSaved("/capture/first.jpg"))
        capture(camera, "/capture/second.jpg")
        assertEquals(listOf("/capture/first.jpg", "/capture/second.jpg"), gallery.savedPaths)
        assertEquals(2, effects.filterIsInstance<CameraEffect.NavigateToComposer>().size)
        assertEquals(CameraEffect.ShowMessage("Đã lưu ảnh vào thư viện"), effects[effects.lastIndex - 1])
    }

    @Test fun `callback repeated while gallery write is suspended cannot save or navigate twice`() = runTest {
        settings.setAutoSave(true)
        val release = CompletableDeferred<Unit>()
        gallery.beforeSave = { release.await() }
        val camera = camera()
        val effects = observe(camera)
        capture(camera, "/capture/one.jpg")
        camera.onAction(CameraAction.OnPhotoSaved("/capture/one.jpg"))
        camera.onAction(CameraAction.OnShutterClick)
        assertTrue(camera.uiState.value.isCapturing)
        assertEquals(1, gallery.savedPaths.size)
        assertTrue(effects.none { it is CameraEffect.NavigateToComposer })
        release.complete(Unit)
        assertEquals(1, effects.filterIsInstance<CameraEffect.NavigateToComposer>().size)
        assertFalse(camera.uiState.value.isCapturing)
    }

    @Test fun `turning off after shutter still saves only the capture requested while on`() = runTest {
        settings.setAutoSave(true)
        val camera = camera()
        observe(camera)
        camera.onAction(CameraAction.OnShutterClick)
        settings.setAutoSave(false)
        camera.onAction(CameraAction.OnPhotoSaved("/capture/on-at-shutter.jpg"))
        capture(camera, "/capture/off-at-shutter.jpg")
        assertEquals(listOf("/capture/on-at-shutter.jpg"), gallery.savedPaths)
    }

    @Test fun `turning on does not retroactively save a capture started while off`() = runTest {
        val camera = camera()
        observe(camera)
        camera.onAction(CameraAction.OnShutterClick)
        settings.setAutoSave(true)
        camera.onAction(CameraAction.OnPhotoSaved("/capture/off-at-shutter.jpg"))
        assertTrue(gallery.savedPaths.isEmpty())
    }

    @Test fun `setting changes and recreated camera never export old or imported files`() = runTest {
        val camera = camera()
        observe(camera)
        settings.setAutoSave(true)
        camera.onAction(CameraAction.OnPhotoSaved("/gallery/imported.jpg"))
        val recreated = camera()
        observe(recreated)
        recreated.onAction(CameraAction.OnPhotoSaved("/capture/old.jpg"))
        assertTrue(gallery.savedPaths.isEmpty())
    }

    @Test fun `gallery error is visible and original draft remains available for composer`() = runTest {
        settings.setAutoSave(true)
        gallery.result = Result.failure(IOException("No free space"))
        val photo: File = temporaryFolder.newFile("draft.jpg").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        val camera = camera()
        val effects = observe(camera)
        capture(camera, photo.absolutePath)
        assertTrue(photo.exists())
        assertEquals(4L, photo.length())
        assertTrue(effects.filterIsInstance<CameraEffect.ShowMessage>().single().message.startsWith("Không lưu được"))
        assertEquals(CameraEffect.NavigateToComposer(photo.absolutePath), effects.last())
        assertFalse(camera.uiState.value.isCapturing)
    }

    @Test fun `legacy permission denial preserves capture and continues without gallery write`() = runTest {
        settings.setAutoSave(true)
        gallery.permissionRequired = true
        val camera = camera()
        val effects = observe(camera)
        capture(camera, "/capture/permission.jpg")
        assertEquals(CameraEffect.RequestGalleryWritePermission, effects.last())
        assertTrue(camera.uiState.value.isCapturing)
        camera.onAction(CameraAction.OnGalleryPermissionResult(false))
        assertTrue(gallery.savedPaths.isEmpty())
        assertTrue(effects.filterIsInstance<CameraEffect.ShowMessage>().single().message.startsWith("Chưa có quyền"))
        assertEquals(CameraEffect.NavigateToComposer("/capture/permission.jpg"), effects.last())
        assertFalse(camera.uiState.value.isCapturing)
    }

    @Test fun `legacy permission grant resumes capture once despite repeated result`() = runTest {
        settings.setAutoSave(true)
        gallery.permissionRequired = true
        val camera = camera()
        val effects = observe(camera)
        capture(camera, "/capture/permission.jpg")
        gallery.permissionRequired = false
        camera.onAction(CameraAction.OnGalleryPermissionResult(true))
        camera.onAction(CameraAction.OnGalleryPermissionResult(true))
        assertEquals(listOf("/capture/permission.jpg"), gallery.savedPaths)
        assertEquals(1, effects.filterIsInstance<CameraEffect.NavigateToComposer>().size)
    }
}
