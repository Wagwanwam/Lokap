package vn.nhom2.lokap.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import vn.nhom2.lokap.data.local.PersistentAccountContactRepository
import vn.nhom2.lokap.domain.repository.AccountContactRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class AccountContactModule {
    @Binds
    @Singleton
    abstract fun bindAccountContactRepository(impl: PersistentAccountContactRepository): AccountContactRepository
}
