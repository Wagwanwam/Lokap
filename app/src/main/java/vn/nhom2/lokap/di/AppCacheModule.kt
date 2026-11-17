package vn.nhom2.lokap.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import vn.nhom2.lokap.data.repository.CoilAppCacheRepository
import vn.nhom2.lokap.domain.repository.AppCacheRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class AppCacheModule {
    @Binds
    @Singleton
    abstract fun bindAppCacheRepository(repository: CoilAppCacheRepository): AppCacheRepository
}
