package com.smartattendance.student.adapters

object ImageUrlHelper {

    fun resolve(path: String?): String? {
        if (path.isNullOrEmpty()) return null
        if (path == "null") return null
        if (path == "/uploads/null") return null
        // Cloudinary or any full URL — use as-is
        return if (path.startsWith("https://") || path.startsWith("http://")) path else null
    }

    fun isValid(path: String?): Boolean = resolve(path) != null
}