package com.example.mineteh.utils

object ImageUtils {
    private const val BASE_IMAGE_URL = "https://mineteh.infinityfree.me/home/uploads/"
    
    /**
     * Convert image path to full URL
     * Handles both relative paths and full URLs
     */
    fun getFullImageUrl(imagePath: String?): String? {
        if (imagePath.isNullOrBlank()) return null
        
        return when {
            // Already a full URL
            imagePath.startsWith("http://") || imagePath.startsWith("https://") -> imagePath
            // Path already contains "uploads/" - use it directly with base domain
            imagePath.startsWith("uploads/") -> "https://mineteh.infinityfree.me/home/$imagePath"
            // Relative path - prepend base URL
            else -> BASE_IMAGE_URL + imagePath.removePrefix("/")
        }
    }
    
    /**
     * Convert list of image paths to full URLs
     */
    fun getFullImageUrls(imagePaths: List<String>?): List<String> {
        return imagePaths?.mapNotNull { getFullImageUrl(it) } ?: emptyList()
    }
}
