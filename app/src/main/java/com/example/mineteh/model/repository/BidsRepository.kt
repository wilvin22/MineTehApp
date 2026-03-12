package com.example.mineteh.model.repository

import android.content.Context
import android.util.Log
import com.example.mineteh.model.UserBidData
import com.example.mineteh.model.UserBidWithListing
import com.example.mineteh.models.Listing
import com.example.mineteh.utils.Resource
import com.example.mineteh.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class BidsRepository(private val context: Context) {
    private val tokenManager = TokenManager(context)
    private val listingsRepository = ListingsRepository(context)
    private val apiService = com.example.mineteh.network.ApiClient.apiService
    
    companion object {
        private const val TAG = "BidsRepository"
    }
    
    /**
     * Fetches all bids for the authenticated user with complete listing information
     */
    suspend fun getUserBids(): Resource<List<UserBidWithListing>> = withContext(Dispatchers.IO) {
        try {
            // Check authentication
            if (!tokenManager.isLoggedIn()) {
                Log.w(TAG, "User not authenticated")
                return@withContext Resource.Error("Not authenticated")
            }
            
            val userId = tokenManager.getUserId()
            if (userId == null) {
                Log.w(TAG, "User ID not found")
                return@withContext Resource.Error("User ID not found")
            }
            
            Log.d(TAG, "Fetching bids for user $userId")
            
            // Step 1: Fetch all bids from Supabase
            val bidsResponse = com.example.mineteh.supabase.SupabaseClient.database
                .from("bids")
                .select()
            
            Log.d(TAG, "Bids response: ${bidsResponse.data}")
            
            if (bidsResponse.data.isEmpty() || bidsResponse.data == "[]") {
                Log.d(TAG, "No bids found")
                return@withContext Resource.Success(emptyList())
            }
            
            // Step 2: Parse bids and filter by user_id
            val allBids = parseBidsResponse(bidsResponse.data)
            val userBids = allBids.filter { it.userId == userId }
            
            Log.d(TAG, "Found ${userBids.size} bids for user $userId")
            
            if (userBids.isEmpty()) {
                return@withContext Resource.Success(emptyList())
            }
            
            // Step 3: Get unique listing IDs
            val listingIds = userBids.map { it.listingId }.distinct()
            Log.d(TAG, "Fetching ${listingIds.size} unique listings")
            
            // Step 4: Fetch listings for these IDs
            val listingsMap = fetchListingsForBids(listingIds)
            Log.d(TAG, "Fetched ${listingsMap.size} listings")
            
            // Step 5: Fetch highest bids for each listing
            val highestBidsMap = fetchHighestBids(listingIds)
            Log.d(TAG, "Fetched highest bids for ${highestBidsMap.size} listings")
            
            // Step 6: Combine data into UserBidWithListing objects
            val result = userBids.mapNotNull { bid ->
                val listing = listingsMap[bid.listingId]
                if (listing == null) {
                    Log.w(TAG, "Listing ${bid.listingId} not found for bid ${bid.bidId}")
                    null
                } else {
                    val highestBid = highestBidsMap[bid.listingId] ?: bid.bidAmount
                    UserBidWithListing(
                        bid = bid,
                        listing = listing,
                        highestBid = highestBid
                    )
                }
            }
            
            Log.d(TAG, "Returning ${result.size} bids with listings")
            Resource.Success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user bids", e)
            Resource.Error(e.message ?: "Failed to fetch bids")
        }
    }
    
    /**
     * Parses bids JSON response from Supabase
     */
    private fun parseBidsResponse(jsonData: String): List<UserBidData> {
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val jsonArray = json.parseToJsonElement(jsonData).jsonArray
            
            jsonArray.mapNotNull { element ->
                try {
                    val obj = element.jsonObject
                    UserBidData(
                        bidId = obj["bid_id"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        userId = obj["user_id"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        listingId = obj["listing_id"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        bidAmount = obj["bid_amount"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                        bidTime = obj["bid_time"]?.jsonPrimitive?.content ?: ""
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing bid", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in parseBidsResponse", e)
            emptyList()
        }
    }
    
    /**
     * Fetches listings for the given listing IDs
     */
    private suspend fun fetchListingsForBids(listingIds: List<Int>): Map<Int, Listing> {
        return try {
            // Fetch all listings
            val listingsResult = listingsRepository.getListings(limit = 1000)
            
            if (listingsResult is Resource.Success && listingsResult.data != null) {
                // Filter to only the listings we need
                listingsResult.data
                    .filter { it.id in listingIds }
                    .associateBy { it.id }
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching listings", e)
            emptyMap()
        }
    }
    
    /**
     * Fetches highest bid for each listing
     */
    private suspend fun fetchHighestBids(listingIds: List<Int>): Map<Int, Double> {
        return try {
            // Fetch all bids
            val bidsResponse = com.example.mineteh.supabase.SupabaseClient.database
                .from("bids")
                .select()
            
            if (bidsResponse.data.isEmpty() || bidsResponse.data == "[]") {
                return emptyMap()
            }
            
            val allBids = parseBidsResponse(bidsResponse.data)
            
            // Group by listing_id and find max bid_amount for each
            allBids
                .filter { it.listingId in listingIds }
                .groupBy { it.listingId }
                .mapValues { (_, bids) ->
                    bids.maxOfOrNull { it.bidAmount } ?: 0.0
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching highest bids", e)
            emptyMap()
        }
    }
    
    /**
     * Places a bid on a listing via PHP API
     */
    suspend fun placeBid(listingId: Int, bidAmount: Double): Resource<com.example.mineteh.models.BidData> = withContext(Dispatchers.IO) {
        try {
            if (!tokenManager.isLoggedIn()) {
                return@withContext Resource.Error("Not authenticated")
            }
            
            val token = tokenManager.getToken()
            if (token == null) {
                return@withContext Resource.Error("Authentication token not found")
            }
            
            Log.d(TAG, "Placing bid: listingId=$listingId, amount=$bidAmount")
            
            val request = com.example.mineteh.models.BidRequest(
                listing_id = listingId,
                bid_amount = bidAmount
            )
            
            val response = apiService.placeBid("Bearer $token", request)
            
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.success && apiResponse.data != null) {
                    Log.d(TAG, "Bid placed successfully")
                    Resource.Success(apiResponse.data)
                } else {
                    Log.e(TAG, "Bid placement failed: ${apiResponse.message}")
                    Resource.Error(apiResponse.message ?: "Failed to place bid")
                }
            } else {
                Log.e(TAG, "Bid placement API error: ${response.code()}")
                Resource.Error("Failed to place bid: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error placing bid", e)
            Resource.Error(e.message ?: "Failed to place bid")
        }
    }
}
