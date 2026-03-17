package com.example.mineteh.model.repository

import android.content.Context
import android.util.Log
import com.example.mineteh.models.Listing
import com.example.mineteh.utils.Resource
import com.example.mineteh.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FavoritesRepository(private val context: Context) {
    private val tokenManager = TokenManager(context)
    private val listingsRepository = ListingsRepository(context)
    private val tag = "FavoritesRepository"

    suspend fun toggleFavorite(listingId: Int): Resource<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                if (!tokenManager.isLoggedIn()) {
                    return@withContext Resource.Error("Not authenticated")
                }
                
                val userId = tokenManager.getUserId()
                if (userId == -1) {
                    return@withContext Resource.Error("User ID not found")
                }
                
                listingsRepository.toggleFavorite(listingId)
            } catch (e: Exception) {
                Log.e(tag, "Error toggling favorite", e)
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
                
                val userId = tokenManager.getUserId()
                if (userId == -1) {
                    return@withContext Resource.Error("User ID not found")
                }
                
                listingsRepository.getFavorites()
            } catch (e: Exception) {
                Log.e(tag, "Error fetching favorites", e)
                Resource.Error(e.message ?: "Network error")
            }
        }
    }
}