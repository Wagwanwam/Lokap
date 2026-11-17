package vn.nhom2.lokap.domain.repository

/** Writes a copy of a newly captured photo; never moves or deletes its draft file. */
interface PhotoGalleryRepository {
    /** Only Android 8–9 need a runtime storage permission to add a photo. */
    fun requiresWritePermission(): Boolean

    /** Returns the published MediaStore URI, or a failure while preserving the source. */
    suspend fun savePhoto(imagePath: String): Result<String>
}
