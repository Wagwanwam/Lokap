package vn.nhom2.lokap.data.local

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.nhom2.lokap.core.settings.DevicePreferenceStore
import vn.nhom2.lokap.domain.model.AccountContactData
import vn.nhom2.lokap.domain.repository.AccountContactRepository

@Singleton
class PersistentAccountContactRepository @Inject constructor(
    private val store: DevicePreferenceStore
) : AccountContactRepository {
    private val json = Json { ignoreUnknownKeys = true }

    override fun read(userId: String): AccountContactData {
        require(userId.isNotBlank())
        val saved = store.getString(key(userId)) ?: return AccountContactData()
        return runCatching { json.decodeFromString<AccountContactData>(saved) }
            .getOrDefault(AccountContactData())
    }

    @Synchronized
    override fun update(
        userId: String,
        transform: (AccountContactData) -> AccountContactData
    ): Result<AccountContactData> = runCatching {
        val updated = transform(read(userId))
        store.putString(key(userId), json.encodeToString(updated))
        updated
    }

    override fun clear(userId: String): Result<Unit> = update(userId) { AccountContactData() }.map { }

    private fun key(userId: String) = "account_contacts:$userId"
}
