package com.example.mineteh.model.repositories

import com.example.mineteh.models.BidRequest
import com.example.mineteh.network.ApiClient
import com.example.mineteh.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BidsRepository {
    private val apiService = ApiClient.apiService

    suspend fun placeBid(listingId: Int, bidAmount: Double) = withContext(Dispatchers.IO) {
        try {
            val response = apiService.placeBid(BidRequest(listingId, bidAmount))
            if (response.isSuccessful && response.body()?.success == true) {
                Resource.Success(response.body()?.data)
            } else {
                Resource.Error(response.body()?.message ?: "Failed to place bid")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }
}