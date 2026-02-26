package com.example.mineteh.model

import com.example.mineteh.models.ApiResponse
import com.example.mineteh.models.BidData
import com.example.mineteh.models.BidRequest
import com.example.mineteh.models.FavoriteData
import com.example.mineteh.models.FavoriteRequest
import com.example.mineteh.models.Listing
import com.example.mineteh.models.LoginData
import com.example.mineteh.models.LoginRequest
import com.example.mineteh.models.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("auth/login.php")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginData>>

    @POST("auth/register.php")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<LoginData>>

    @POST("auth/logout.php")
    suspend fun logout(): Response<ApiResponse<Any>>

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

    @POST("bids/place.php")
    suspend fun placeBid(@Body request: BidRequest): Response<ApiResponse<BidData>>

    @POST("favorites/toggle.php")
    suspend fun toggleFavorite(@Body request: FavoriteRequest): Response<ApiResponse<FavoriteData>>

    @GET("favorites/index.php")
    suspend fun getFavorites(): Response<ApiResponse<List<Listing>>>
}