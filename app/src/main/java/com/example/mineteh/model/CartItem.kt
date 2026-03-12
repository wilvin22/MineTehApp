package com.example.mineteh.models

data class CartItem(
    val listingId: Int,
    val title: String,
    val price: Double,
    val image: String?,
    val sellerId: Int?,
    val sellerName: String,
    var quantity: Int = 1
)
