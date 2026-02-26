package com.example.mineteh.model.repositories

import com.example.mineteh.models.FavoriteRequest
import com.example.mineteh.network.ApiClient
import com.example.mineteh.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FavoritesRepository {
    private val apiService = ApiClient.apiService

    suspend fun toggleFavorite(listingId: Int) = withContext(Dispatchers.IO) {
        try {
            val response = apiService.toggleFavorite(FavoriteRequest(listingId))
            if (response.isSuccessful && response.body()?.success == true) {
                Resource.Success(response.body()?.data)
            } else {
                Resource.Error(response.body()?.message ?: "Failed to update favorite")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun getFavorites() = withContext(Dispatchers.IO) {
        try {
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