package vn.nhom2.lokap.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import vn.nhom2.lokap.core.image.MediaStorePhotoGallery
import vn.nhom2.lokap.domain.repository.PhotoGalleryRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class PhotoGalleryModule {
    @Binds
    @Singleton
    abstract fun bindPhotoGalleryRepository(impl: MediaStorePhotoGallery): PhotoGalleryRepository
}
