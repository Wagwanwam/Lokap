package vn.nhom2.lokap.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import vn.nhom2.lokap.core.settings.DevicePreferenceStore
import vn.nhom2.lokap.data.local.PersistentDeviceSettingsRepository
import vn.nhom2.lokap.data.local.SharedPreferencesDevicePreferenceStore
import vn.nhom2.lokap.domain.repository.DeviceSettingsRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class DeviceSettingsModule {
    @Binds
    @Singleton
    abstract fun bindDevicePreferenceStore(impl: SharedPreferencesDevicePreferenceStore): DevicePreferenceStore

    @Binds
    @Singleton
    abstract fun bindDeviceSettingsRepository(impl: PersistentDeviceSettingsRepository): DeviceSettingsRepository
}
