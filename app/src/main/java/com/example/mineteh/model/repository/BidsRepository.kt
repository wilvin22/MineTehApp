package com.example.mineteh.model.repository

import android.content.Context
import com.example.mineteh.models.BidData
import com.example.mineteh.models.BidRequest
import com.example.mineteh.network.ApiClient
import com.example.mineteh.utils.Resource
import com.example.mineteh.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BidsRepository(context: Context) {
    private val apiService = ApiClient.apiService
    private val tokenManager = TokenManager(context)

    suspend fun placeBid(listingId: Int, bidAmount: Double): Resource<BidData> {
        return withContext(Dispatchers.IO) {
            try {
                if (!tokenManager.isLoggedIn()) {
                    return@withContext Resource.Error("Not authenticated")
                }
                
                val request = BidRequest(listing_id = listingId, bid_amount = bidAmount)
                val response = apiService.placeBid(request)

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
}
