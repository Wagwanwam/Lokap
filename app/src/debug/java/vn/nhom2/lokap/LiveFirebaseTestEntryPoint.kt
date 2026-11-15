package vn.nhom2.lokap

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import vn.nhom2.lokap.domain.repository.AuthRepository
import vn.nhom2.lokap.domain.repository.PostRepository
import vn.nhom2.lokap.domain.repository.SettingsRepository

/** Lets opt-in device tests exercise the same repositories used by the debug app. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface LiveFirebaseTestEntryPoint {
    fun authRepository(): AuthRepository
    fun postRepository(): PostRepository
    fun settingsRepository(): SettingsRepository
    fun firestore(): FirebaseFirestore
    fun firebaseAuth(): FirebaseAuth
}
