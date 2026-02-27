package com.example.mineteh.model.repository

import android.content.Context
import com.example.mineteh.models.FavoriteData
import com.example.mineteh.models.FavoriteRequest
import com.example.mineteh.models.Listing
import com.example.mineteh.network.ApiClient
import com.example.mineteh.utils.Resource
import com.example.mineteh.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FavoritesRepository(context: Context) {
    private val apiService = ApiClient.apiService
    private val tokenManager = TokenManager(context)

    suspend fun toggleFavorite(listingId: Int): Resource<FavoriteData> {
        return withContext(Dispatchers.IO) {
            try {
                if (!tokenManager.isLoggedIn()) {
                    return@withContext Resource.Error("Not authenticated")
                }
                
                val request = FavoriteRequest(listing_id = listingId)
                val response = apiService.toggleFavorite(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    Resource.Success(response.body()?.data)
                } else {
                    Resource.Error(response.body()?.message ?: "Failed to toggle favorite")
                }
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Network error")
            }
        }
    }

    suspend fun getFavorites(): Resource<List<Listing>> {
        return withContext(Dispatchers.IO) {
            try {
                if (!tokenManager.isLoggedIn()) {
                    return@withContext Resource.Error("Not authenticated")
                }
                
                val response = apiService.getFavorites()

                if (response.isSuccessful && response.body()?.success == true) {
                    Resource.Success(response.body()?.data)
                } else {
                    Resource.Error(response.body()?.message ?: "Failed to load favorites")
                }
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Network error")
            }
        }
    }
}
