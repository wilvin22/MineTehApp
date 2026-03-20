package com.example.mineteh.models

data class SellerProfileData(
    val accountId: Int,
    val username: String,
    val firstName: String,
    val lastName: String,
    val avatarUrl: String?,
    val averageRating: Double,
    val activeListingCount: Int,
    val soldCount: Int
)
