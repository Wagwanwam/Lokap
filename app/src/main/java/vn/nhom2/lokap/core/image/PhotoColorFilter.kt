package vn.nhom2.lokap.core.image

/**
 * Bộ lọc màu chọn ở màn chụp (nhấn giữ nút chụp).
 * [matrix] là ma trận màu 4×5 (cột thứ 5 là độ lệch 0..255), cùng định dạng với
 * android.graphics.ColorMatrix và Compose ColorMatrix → dùng chung cho xem trước camera và ảnh đã chụp.
 */
enum class PhotoColorFilter(val labelVi: String) {
    ORIGINAL("Gốc"),
    VIVID("Tươi rực"),
    WARM_GOLDEN("Ấm áp"),
    COOL_BREEZE("Trong trẻo"),
    NOSTALGIA("Hoài niệm"),
    MONO_CHIC("Đen trắng"),
    DREAMY_PASTEL("Mộng mơ"),
    CINEMATIC("Điện ảnh");

    /** null = giữ nguyên màu. Trả mảng mới mỗi lần để nơi dùng không sửa nhầm bản gốc. */
    val matrix: FloatArray?
        get() = when (this) {
            ORIGINAL -> null
            VIVID -> saturation(1.4f)
            WARM_GOLDEN -> floatArrayOf(
                1.12f, 0f, 0f, 0f, 15f,
                0f, 1.02f, 0f, 0f, 8f,
                0f, 0f, 0.88f, 0f, -10f,
                0f, 0f, 0f, 1f, 0f
            )
            COOL_BREEZE -> floatArrayOf(
                0.92f, 0f, 0f, 0f, -5f,
                0f, 1.02f, 0f, 0f, 5f,
                0f, 0f, 1.18f, 0f, 18f,
                0f, 0f, 0f, 1f, 0f
            )
            NOSTALGIA -> floatArrayOf(
                0.95f, 0.05f, 0.05f, 0f, 20f,
                0.05f, 0.90f, 0.05f, 0f, 15f,
                0.05f, 0.05f, 0.75f, 0f, -10f,
                0f, 0f, 0f, 1f, 0f
            )
            MONO_CHIC -> saturation(0f)
            DREAMY_PASTEL -> floatArrayOf(
                1.08f, 0f, 0f, 0f, 16f,
                0f, 0.98f, 0f, 0f, 10f,
                0f, 0f, 1.10f, 0f, 16f,
                0f, 0f, 0f, 1f, 0f
            )
            CINEMATIC -> floatArrayOf(
                1.16f, 0f, 0f, 0f, -8f,
                0f, 1.05f, 0f, 0f, 2f,
                0f, 0f, 1.22f, 0f, 14f,
                0f, 0f, 0f, 1f, 0f
            )
        }

    private companion object {
        /** Giống ColorMatrix.setSaturation của Android (hệ số độ sáng Rec. 709). */
        fun saturation(s: Float): FloatArray {
            val inv = 1f - s
            val r = 0.213f * inv
            val g = 0.715f * inv
            val b = 0.072f * inv
            return floatArrayOf(
                r + s, g, b, 0f, 0f,
                r, g + s, b, 0f, 0f,
                r, g, b + s, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        }
    }
}
