package vn.nhom2.lokap

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import vn.nhom2.lokap.core.designsystem.theme.ThemeManager
import vn.nhom2.lokap.core.settings.DevicePreferenceStore
import vn.nhom2.lokap.core.image.DataSaverImageInterceptor
import vn.nhom2.lokap.domain.repository.DeviceSettingsRepository
import vn.vietmap.vietmapsdk.Vietmap

/**
 * Điểm vào của Hilt. WorkManager lấy worker qua HiltWorkerFactory (UploadPostWorker cần Repository),
 * nên initializer mặc định của WorkManager đã bị tắt trong AndroidManifest.
 */
@HiltAndroidApp
class LokapApp : Application(), Configuration.Provider, SingletonImageLoader.Factory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var devicePreferenceStore: DevicePreferenceStore

    @Inject
    lateinit var deviceSettingsRepository: DeviceSettingsRepository

    override fun newImageLoader(context: Context): ImageLoader = ImageLoader.Builder(context)
        .components { add(DataSaverImageInterceptor(deviceSettingsRepository)) }
        .build()

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        ThemeManager.initialize(devicePreferenceStore)
        initFirebase()
        // MapView của VietMap cần khởi tạo trước khi tạo view đầu tiên
        if (BuildConfig.VIETMAP_TILE_KEY.isNotBlank()) Vietmap.getInstance(this)
    }

    /**
     * Có app/google-services.json: plugin google-services tạo resource và Firebase tự khởi tạo trước onCreate.
     * Chỉ có khoá trong .env: khởi tạo bằng tay từ BuildConfig. Sau đó mới nối emulator (nếu có).
     */
    private fun initFirebase() {
        if (FirebaseApp.getApps(this).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setApiKey(BuildConfig.FIREBASE_API_KEY)
                .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                .build()
            FirebaseApp.initializeApp(this, options)
        }

        val emulatorHost = BuildConfig.FIREBASE_EMULATOR_HOST
        if (emulatorHost.isNotBlank()) {
            // Cổng mặc định trong firebase/firebase.json
            FirebaseAuth.getInstance().useEmulator(emulatorHost, 9099)
            FirebaseFirestore.getInstance().useEmulator(emulatorHost, 8080)
        }
    }
}
