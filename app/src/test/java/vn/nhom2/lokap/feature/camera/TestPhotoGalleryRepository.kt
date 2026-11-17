package vn.nhom2.lokap.feature.camera

import vn.nhom2.lokap.domain.repository.PhotoGalleryRepository

class TestPhotoGalleryRepository : PhotoGalleryRepository {
    var permissionRequired = false
    var result = Result.success("content://media/external/images/media/123")
    val savedPaths = mutableListOf<String>()
    var beforeSave: suspend () -> Unit = {}

    override fun requiresWritePermission() = permissionRequired

    override suspend fun savePhoto(imagePath: String): Result<String> {
        savedPaths += imagePath
        beforeSave()
        return result
    }
}
