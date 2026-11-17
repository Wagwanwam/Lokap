package vn.nhom2.lokap.core.image

import java.net.URI

/**
 * Biến thể tiết kiệm cho secure_url không ký do CloudinaryUploader trả về.
 * Chỉ nhận URL ảnh có version rõ ràng. URL đã có transform khác được giữ nguyên để không
 * thay đổi thứ tự crop/overlay hoặc làm hỏng chữ ký, tên public_id và token truy cập.
 */
object CloudinaryImagePolicy {
    const val DATA_SAVER_TRANSFORMATION = "q_auto:eco,f_auto,c_limit,w_640,h_640"
    private val cloudName = Regex("^[A-Za-z0-9_-]+$")
    private val version = Regex("^v[0-9]+$")
    private val rasterExtensions = setOf("jpg", "jpeg", "png", "webp", "avif", "heic", "heif", "bmp")

    fun dataSaverUrl(url: String, enabled: Boolean): String {
        if (!enabled) return url
        val uri = runCatching { URI(url) }.getOrNull() ?: return url
        if (!uri.scheme.equals("https", ignoreCase = true) ||
            !uri.host.equals("res.cloudinary.com", ignoreCase = true) ||
            uri.rawUserInfo != null || uri.port != -1 || uri.rawQuery != null || uri.rawFragment != null
        ) return url
        val path = uri.rawPath ?: return url
        if (!path.startsWith('/')) return url
        val segments = path.removePrefix("/").split('/')
        if (segments.size < 5 || !cloudName.matches(segments[0]) ||
            segments[1] != "image" || segments[2] != "upload"
        ) return url
        // Bỏ qua signed URLs (s--...--), authenticated/private, và các chuỗi transform có sẵn.
        // Biến thể đã tạo cũng đi nhánh này: không nối thêm transform lần thứ hai.
        if (!version.matches(segments[3])) return url
        val publicId = segments.drop(4)
        if (publicId.any { it.isEmpty() || it == "." || it == ".." }) return url
        val decodedPath = uri.path ?: return url
        if ('\\' in decodedPath || decodedPath.split('/').any { it == "." || it == ".." }) return url
        if (publicId.last().substringAfterLast('.', "").lowercase() !in rasterExtensions) return url
        val transformedPath = "/" + (segments.take(3) + DATA_SAVER_TRANSFORMATION + segments.drop(3)).joinToString("/")
        // Giữ raw path: không decode/re-encode public_id (%20, UTF-8…), không đổi version.
        return "${uri.scheme}://${uri.rawAuthority}$transformedPath"
    }
}
