package com.example.mineteh.model.repositories

import com.example.mineteh.network.ApiClient
import com.example.mineteh.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ListingsRepository {
    private val apiService = ApiClient.apiService

    suspend fun getListings(
        category: String? = null,
        type: String? = null,
        search: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ) = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getListings(category, type, search, limit, offset)
            if (response.isSuccessful && response.body()?.success == true) {
                Resource.Success(response.body()?.data)
            } else {
                Resource.Error(response.body()?.message ?: "Failed to load listings")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun getListing(id: Int) = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getListing(id)
            if (response.isSuccessful && response.body()?.success == true) {
                Resource.Success(response.body()?.data)
            } else {
                Resource.Error(response.body()?.message ?: "Failed to load listing")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }
}