package com.example.mineteh.model

data class UserBidData(
    val bidId: Int,
    val userId: Int,
    val listingId: Int,
    val bidAmount: Double,
    val bidTime: String
)
