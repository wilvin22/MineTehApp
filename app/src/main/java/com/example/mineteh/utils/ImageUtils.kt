package com.example.mineteh.utils

object ImageUtils {
    private const val BASE_IMAGE_URL = "https://mineteh.infinityfree.me/home/uploads/"
    
    /**
     * Convert image path to full URL
     * Handles relative paths, full URLs, and base64 data URIs
     */
    fun getFullImageUrl(imagePath: String?): String? {
        if (imagePath.isNullOrBlank()) return null
        
        val result = when {
            // Base64 data URI - return as-is for direct display
            imagePath.startsWith("data:image/") -> {
                android.util.Log.d("ImageUtils", "Handling data URI (${imagePath.length} chars)")
                imagePath
            }
            // Already a full URL
            imagePath.startsWith("http://") || imagePath.startsWith("https://") -> {
                android.util.Log.d("ImageUtils", "Handling full URL: $imagePath")
                imagePath
            }
            // Path already contains "uploads/" - use it directly with base domain
            imagePath.startsWith("uploads/") -> {
                val url = "https://mineteh.infinityfree.me/home/$imagePath"
                android.util.Log.d("ImageUtils", "Handling uploads path: $imagePath -> $url")
                url
            }
            // Relative path - prepend base URL
            else -> {
                val url = BASE_IMAGE_URL + imagePath.removePrefix("/")
                android.util.Log.d("ImageUtils", "Handling relative path: $imagePath -> $url")
                url
            }
        }
        
        return result
    }
    
    /**
     * Convert list of image paths to full URLs
     */
    fun getFullImageUrls(imagePaths: List<String>?): List<String> {
        return imagePaths?.mapNotNull { getFullImageUrl(it) } ?: emptyList()
    }
}
