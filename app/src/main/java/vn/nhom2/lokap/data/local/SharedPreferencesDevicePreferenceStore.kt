package vn.nhom2.lokap.data.local

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import vn.nhom2.lokap.core.settings.DevicePreferenceStore

@Singleton
class SharedPreferencesDevicePreferenceStore @Inject constructor(
    @ApplicationContext context: Context
) : DevicePreferenceStore {
    private val preferences = context.getSharedPreferences("lokap_device_settings", Context.MODE_PRIVATE)

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        preferences.getBoolean(key, defaultValue)

    override fun putBoolean(key: String, value: Boolean) {
        preferences.edit { putBoolean(key, value) }
    }

    override fun getString(key: String): String? = preferences.getString(key, null)

    override fun putString(key: String, value: String) {
        preferences.edit { putString(key, value) }
    }
}
