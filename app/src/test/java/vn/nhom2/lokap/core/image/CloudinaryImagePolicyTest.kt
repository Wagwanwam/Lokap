package vn.nhom2.lokap.core.image

import org.junit.Assert.assertEquals
import org.junit.Test

class CloudinaryImagePolicyTest {
    private val original = "https://res.cloudinary.com/demo/image/upload/v1763360000/lokap/posts/user-a/photo-1.jpg"
    private val reduced = "https://res.cloudinary.com/demo/image/upload/q_auto:eco,f_auto,c_limit,w_640,h_640/v1763360000/lokap/posts/user-a/photo-1.jpg"

    @Test fun `enabled policy reduces normal uploaded image while disabled preserves original`() {
        assertEquals(reduced, CloudinaryImagePolicy.dataSaverUrl(original, enabled = true))
        assertEquals(original, CloudinaryImagePolicy.dataSaverUrl(original, enabled = false))
    }

    @Test fun `transformation preserves uploaded version and encoded public id`() {
        val url = "https://res.cloudinary.com/cloud-name/image/upload/v123456/lokap/avatars/user/image%20one%2Btwo.PNG"
        assertEquals(
            "https://res.cloudinary.com/cloud-name/image/upload/q_auto:eco,f_auto,c_limit,w_640,h_640/v123456/lokap/avatars/user/image%20one%2Btwo.PNG",
            CloudinaryImagePolicy.dataSaverUrl(url, true)
        )
    }

    @Test fun `policy is idempotent and leaves existing crop and overlay chains untouched`() {
        assertEquals(reduced, CloudinaryImagePolicy.dataSaverUrl(reduced, true))
        listOf(
            "c_fill,w_200,h_200",
            "c_crop,w_800,h_400/c_scale,w_300",
            "l_logo/fl_layer_apply,g_south",
            "t_named_transformation"
        ).forEach { transform ->
            val url = original.replace("upload/", "upload/$transform/")
            assertEquals(url, CloudinaryImagePolicy.dataSaverUrl(url, true))
        }
    }

    @Test fun `signed authenticated private and query protected URLs remain unchanged`() {
        listOf(
            original.replace("upload/", "upload/s--AbCdEf12--/"),
            original.replace("/upload/", "/authenticated/"),
            original.replace("/upload/", "/private/"),
            "$original?__cld_token__=private-token",
            "$original?download=1",
            "$original?",
            "$original#section",
            "$original#"
        ).forEach { assertEquals(it, CloudinaryImagePolicy.dataSaverUrl(it, true)) }
    }

    @Test fun `only exact https cloudinary host and plain authority are allowed`() {
        listOf(
            original.replace("https://", "http://"),
            original.replace("res.cloudinary.com", "res.cloudinary.com.evil.example"),
            original.replace("res.cloudinary.com", "user@res.cloudinary.com"),
            original.replace("res.cloudinary.com", "res.cloudinary.com@evil.example"),
            original.replace("res.cloudinary.com", "res.cloudinary.com:443"),
            original.replace("res.cloudinary.com", "cdn.example.com"),
            "//res.cloudinary.com/demo/image/upload/v123/a.jpg"
        ).forEach { assertEquals(it, CloudinaryImagePolicy.dataSaverUrl(it, true)) }
    }

    @Test fun `local video documents animation and unversioned paths remain unchanged`() {
        listOf(
            "/data/user/0/vn.nhom2.lokap/cache/photo.jpg",
            "content://media/external/images/media/1",
            "file:///tmp/photo.jpg",
            "data:image/png;base64,AAAA",
            original.replace("/image/", "/video/"),
            original.replace(".jpg", ".mp4"),
            original.replace(".jpg", ".pdf"),
            original.replace(".jpg", ".gif"),
            original.replace(".jpg", ".svg"),
            original.replace(".jpg", ""),
            original.replace("v1763360000/", "")
        ).forEach { assertEquals(it, CloudinaryImagePolicy.dataSaverUrl(it, true)) }
    }

    @Test fun `malformed and traversal paths are not rewritten`() {
        listOf(
            "not a URL",
            original.replace("photo-1.jpg", "bad%xx.jpg"),
            original.replace("photo-1.jpg", "../photo-1.jpg"),
            original.replace("photo-1.jpg", "%2e%2e/photo-1.jpg"),
            original.replace("photo-1.jpg", "folder%5Cphoto-1.jpg"),
            original.replace("photo-1.jpg", "/photo-1.jpg"),
            "https://res.cloudinary.com/demo/image/upload/v123/"
        ).forEach { assertEquals(it, CloudinaryImagePolicy.dataSaverUrl(it, true)) }
    }
}
