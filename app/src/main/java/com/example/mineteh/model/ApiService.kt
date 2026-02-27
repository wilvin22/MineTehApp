package com.example.mineteh.network

import com.example.mineteh.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // Auth endpoints
    @POST("auth/login.php")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginResponse>>

    @POST("auth/register.php")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<RegisterResponse>>

    @POST("auth/logout.php")
    suspend fun logout(): Response<ApiResponse<Any>>

    // Listings endpoints
    @GET("listings/index.php")
    suspend fun getListings(
        @Query("category") category: String? = null,
        @Query("type") type: String? = null,
        @Query("search") search: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<ApiResponse<List<Listing>>>

    @GET("listings/show.php")
    suspend fun getListing(@Query("id") id: Int): Response<ApiResponse<Listing>>

    @Multipart
    @POST("listings/create.php")
    suspend fun createListing(
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("price") price: RequestBody,
        @Part("location") location: RequestBody,
        @Part("category") category: RequestBody,
        @Part("listing_type") listingType: RequestBody,
        @Part("end_time") endTime: RequestBody?,
        @Part("min_bid_increment") minBidIncrement: RequestBody?,
        @Part images: List<MultipartBody.Part>
    ): Response<ApiResponse<Listing>>

    // Bids endpoints
    @POST("bids/place.php")
    suspend fun placeBid(
        @Body request: BidRequest
    ): Response<ApiResponse<BidData>>

    // Favorites endpoints
    @POST("favorites/toggle.php")
    suspend fun toggleFavorite(
        @Body request: FavoriteRequest
    ): Response<ApiResponse<FavoriteData>>

    @GET("favorites/index.php")
    suspend fun getFavorites(): Response<ApiResponse<List<Listing>>>
}
