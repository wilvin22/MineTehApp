package com.example.mineteh.models

import com.google.gson.annotations.SerializedName

// Generic API Response
data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?
)

data class LoginResponse(
    val token: String,
    val user: User
)

data class RegisterResponse(
    val token: String,
    val user: User
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
    @SerializedName("image") private val _image: String?,
    @SerializedName("images") private val _images: List<String>?,
    val seller: Seller?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("is_favorited") val isFavorited: Boolean = false,
    @SerializedName("highest_bid") val highestBidAmount: Double? = null,
    @SerializedName("end_time") val endTime: String? = null,
    val bids: List<Bid>? = null
) {
    // Helper properties to get full image URLs
    val image: String?
        get() {
            val fullUrl = com.example.mineteh.utils.ImageUtils.getFullImageUrl(_image)
            android.util.Log.d("Listing", "Getting image URL: _image=$_image, fullUrl=$fullUrl")
            return fullUrl
        }
    
    val images: List<String>
        get() {
            val fullUrls = com.example.mineteh.utils.ImageUtils.getFullImageUrls(_images)
            android.util.Log.d("Listing", "Getting images URLs: _images=$_images, fullUrls=$fullUrls")
            return fullUrls
        }
    
    // Helper property to get highest bid as Bid object for backward compatibility
    val highestBid: Bid?
        get() = if (highestBidAmount != null) {
            Bid(bidAmount = highestBidAmount, bidTime = "", bidder = null)
        } else {
            bids?.firstOrNull()
        }
}


data class Seller(
    @SerializedName("account_id") val accountId: Int? = null,
    val username: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String
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

data class FavoriteData(
    @SerializedName("is_favorited") val isFavorited: Boolean
)

data class CreateListingRequest(
    val title: String,
    val description: String,
    val price: Double,
    val location: String,
    val category: String,
    val listing_type: String, // "FIXED" or "BID"
    val end_time: String? = null, // For BID type only
    val min_bid_increment: Double? = null // For BID type only
)
// Add these to your ApiModels.kt file

data class BidRequest(
    val listing_id: Int,
    val bid_amount: Double
)

data class FavoriteRequest(
    val listing_id: Int
)

