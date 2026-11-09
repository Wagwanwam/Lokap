package vn.nhom2.lokap.core.network

import kotlinx.coroutines.flow.Flow
interface ConnectivityObserver {
    fun observe(): Flow<Status>

    enum class Status {
        AVAILABLE,
        UNAVAILABLE,
        LOSING,
        LOST
    }
}
