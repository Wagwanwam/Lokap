package vn.nhom2.lokap.core.settings

/** Các tuỳ chọn của thiết bị, tách khỏi hồ sơ và phiên đăng nhập. */
interface DevicePreferenceStore {
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getString(key: String): String?
    fun putString(key: String, value: String)
}
