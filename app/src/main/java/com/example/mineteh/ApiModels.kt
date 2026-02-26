package com.example.mineteh.models

import com.google.gson.annotations.SerializedName

// Generic API Response
data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?
)

// Auth Models
data class LoginRequest(
    val identifier: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String
)

data class LoginData(
    val user: User,
    val token: String,
    @SerializedName("expires_at") val expiresAt: String
)

data class User(
    @SerializedName("account_id") val accountId: Int,
    val username: String,
    val email: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String
)

// Listing Models
data class Listing(
    val id: Int,
    val title: String,
    val description: String,
    val price: Double,
    val location: String,
    val category: String,
    @SerializedName("listing_type") val listingType: String,
    val status: String,
    val image: String?,
    val images: List<ListingImage>?,
    val seller: Seller?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("is_favorited") val isFavorited: Boolean = false,
    @SerializedName("highest_bid") val highestBid: Bid? = null,
    @SerializedName("end_time") val endTime: String? = null  // Add this line if missing
)


data class ListingImage(
    @SerializedName("image_path") val imagePath: String
)

data class Seller(
    @SerializedName("account_id") val accountId: Int? = null,
    val username: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String
)

// Bid Models
data class BidRequest(
    @SerializedName("listing_id") val listingId: Int,
    @SerializedName("bid_amount") val bidAmount: Double
)

data class BidData(
    @SerializedName("bid_amount") val bidAmount: Double,
    @SerializedName("listing_id") val listingId: Int
)

data class Bid(
    @SerializedName("bid_amount") val bidAmount: Double,
    @SerializedName("bid_time") val bidTime: String,
    val bidder: Bidder?
)

data class Bidder(
    val username: String
)

// Favorite Models
data class FavoriteRequest(
    @SerializedName("listing_id") val listingId: Int
)

data class FavoriteData(
    @SerializedName("is_favorited") val isFavorited: Boolean
)
