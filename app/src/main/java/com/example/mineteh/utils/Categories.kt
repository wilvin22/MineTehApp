package com.example.mineteh.utils

object Categories {
    
    val ALL_CATEGORIES = arrayOf(
        "Electronics", 
        "Vehicles", 
        "Property", 
        "Fashion",
        "Home & Garden", 
        "Sports", 
        "Books", 
        "Other"
    )
    
    val CATEGORIES_WITH_ALL = arrayOf("All") + ALL_CATEGORIES
    
    /**
     * Get category display name for UI
     */
    fun getCategoryDisplayName(category: String?): String {
        return when (category) {
            null, "", "All" -> "All Categories"
            else -> category
        }
    }
    
    /**
     * Get category for API calls (null for "All")
     */
    fun getCategoryForApi(category: String?): String? {
        return when (category) {
            null, "", "All", "All Categories" -> null
            else -> category
        }
    }
    
    /**
     * Check if category is valid
     */
    fun isValidCategory(category: String?): Boolean {
        return category == null || category in CATEGORIES_WITH_ALL
    }
}