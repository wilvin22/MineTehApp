package com.example.mineteh.models

data class SellerProfileData(
    val accountId: Int,
    val username: String,
    val firstName: String,
    val lastName: String,
    val avatarUrl: String?,
    val averageRating: Double,
    val activeListingCount: Int,
    val soldCount: Int,
    val reviewCount: Int = 0,
    val createdAt: String? = null
)
