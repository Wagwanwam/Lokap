package vn.nhom2.lokap.di

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.room.Room
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import vn.nhom2.lokap.data.local.LokapDatabase
import vn.nhom2.lokap.data.local.PostDao
import vn.nhom2.lokap.data.location.FusedLocationRepository
import vn.nhom2.lokap.data.repository.FirebaseAuthRepository
import vn.nhom2.lokap.core.image.ContentImageImporter
import vn.nhom2.lokap.data.repository.FirestoreLandmarkRepository
import vn.nhom2.lokap.data.repository.FirestorePostRepository
import vn.nhom2.lokap.data.repository.FirestoreSettingsRepository
import vn.nhom2.lokap.data.repository.FirestoreSocialRepository
import vn.nhom2.lokap.data.repository.PostUploader
import vn.nhom2.lokap.domain.repository.AuthRepository
import vn.nhom2.lokap.domain.repository.ImageImporter
import vn.nhom2.lokap.domain.repository.LandmarkRepository
import vn.nhom2.lokap.domain.repository.LocationRepository
import vn.nhom2.lokap.domain.repository.PostRepository
import vn.nhom2.lokap.domain.repository.SettingsRepository
import vn.nhom2.lokap.domain.repository.SocialRepository

/** Scope sống cùng app, cho việc chạy nền của Repository (theo dõi phiên, làm mới dữ liệu). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** FirebaseApp đã khởi tạo trong LokapApp (chỉ khi có cấu hình). Các repository dùng cùng phiên Firebase. */
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance(FirebaseApp.getInstance())

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance(FirebaseApp.getInstance())

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LokapDatabase =
        Room.databaseBuilder(context, LokapDatabase::class.java, "lokap.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun providePostDao(database: LokapDatabase): PostDao = database.postDao()

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)

    /** Repository chỉ dùng để xoá phiên Google lúc đăng xuất; hộp chọn tài khoản cần Activity nên màn Đăng nhập tự tạo. */
    @Provides
    @Singleton
    fun provideCredentialManager(@ApplicationContext context: Context): CredentialManager = CredentialManager.create(context)
}

/**
 * Các repository luôn dùng Firebase; không tự thay bằng dữ liệu mẫu.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebase: FirebaseAuthRepository
    ): AuthRepository = firebase

    @Provides
    @Singleton
    fun providePostRepository(
        firestore: FirestorePostRepository
    ): PostRepository = firestore

    @Provides
    @Singleton
    fun provideSocialRepository(
        firestore: FirestoreSocialRepository
    ): SocialRepository = firestore

    @Provides
    @Singleton
    fun provideSettingsRepository(
        firestore: FirestoreSettingsRepository
    ): SettingsRepository = firestore

    @Provides
    fun providePostUploader(firestore: FirestorePostRepository): PostUploader = firestore
}

/** Vị trí thật (Fused Location + VietMap). */
@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {
    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: FusedLocationRepository): LocationRepository
}

/** Địa danh (Firestore `landmarks`) và nhập ảnh từ thư viện máy để đưa lên địa danh. */
@Module
@InstallIn(SingletonComponent::class)
abstract class LandmarkModule {
    @Binds
    @Singleton
    abstract fun bindLandmarkRepository(impl: FirestoreLandmarkRepository): LandmarkRepository

    @Binds
    @Singleton
    abstract fun bindImageImporter(impl: ContentImageImporter): ImageImporter
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ContentModule {
    @Binds abstract fun messaging(impl: vn.nhom2.lokap.data.repository.FirestoreContentRepository): vn.nhom2.lokap.domain.repository.MessagingRepository
    @Binds abstract fun comments(impl: vn.nhom2.lokap.data.repository.FirestoreContentRepository): vn.nhom2.lokap.domain.repository.CommentRepository
    @Binds abstract fun notifications(impl: vn.nhom2.lokap.data.repository.FirestoreContentRepository): vn.nhom2.lokap.domain.repository.NotificationRepository
}
