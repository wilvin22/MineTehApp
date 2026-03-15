package com.example.mineteh.model.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.mineteh.models.Listing
import com.example.mineteh.network.ApiClient
import com.example.mineteh.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class ListingsRepository(private val context: Context) {
    private val apiService = ApiClient.apiService
    private val tag = "ListingsRepository"

    /**
     * Get all listings from the API
     */
    suspend fun getListings(
        category: String? = null,
        type: String? = null,
        search: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Resource<List<Listing>> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Fetching listings: category=$category, type=$type, search=$search")
            
            val response = apiService.getListings(category, type, search, limit, offset)
            
            Log.d(tag, "Response code: ${response.code()}")
            Log.d(tag, "Response message: ${response.message()}")
            
            if (response.isSuccessful) {
                val body = response.body()
                Log.d(tag, "Response body: $body")
                
                if (body?.success == true && body.data != null) {
                    Log.d(tag, "Successfully fetched ${body.data.size} listings")
                    Resource.Success(body.data)
                } else {
                    val errorMsg = body?.message ?: "Unknown error"
                    Log.e(tag, "API returned error: $errorMsg")
                    Resource.Error(errorMsg)
                }
            } else {
                // Log the error body
                val errorBody = response.errorBody()?.string()
                Log.e(tag, "Error response body: $errorBody")
                
                val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                Log.e(tag, "API request failed: $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: com.google.gson.JsonSyntaxException) {
            Log.e(tag, "JSON parsing error - API might be returning HTML or plain text", e)
            Resource.Error("Server error: Invalid response format. Please check your internet connection.")
        } catch (e: Exception) {
            Log.e(tag, "Error fetching listings", e)
            Resource.Error(e.message ?: "Failed to load listings")
        }
    }

    /**
     * Get a single listing by ID from the API
     */
    suspend fun getListing(id: Int): Resource<Listing> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Fetching listing with id=$id")
            
            val response = apiService.getListing(id)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Log.d(tag, "Successfully fetched listing: ${body.data.title}")
                    Resource.Success(body.data)
                } else {
                    val errorMsg = body?.message ?: "Listing not found"
                    Log.e(tag, "API returned error: $errorMsg")
                    Resource.Error(errorMsg)
                }
            } else {
                val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                Log.e(tag, "API request failed: $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching listing", e)
            Resource.Error(e.message ?: "Failed to load listing")
        }
    }

    /**
     * Create a new listing via the API
     */
    suspend fun createListing(
        title: String,
        description: String,
        price: Double,
        location: String,
        category: String,
        listingType: String,
        endTime: String?,
        minBidIncrement: Double?,
        imageUris: List<Uri>
    ): Resource<Listing> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Creating listing: $title")
            
            // Prepare form data
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val descriptionBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
            val priceBody = price.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val locationBody = location.toRequestBody("text/plain".toMediaTypeOrNull())
            val categoryBody = category.toRequestBody("text/plain".toMediaTypeOrNull())
            val listingTypeBody = listingType.toRequestBody("text/plain".toMediaTypeOrNull())
            val endTimeBody = endTime?.toRequestBody("text/plain".toMediaTypeOrNull())
            val minBidIncrementBody = minBidIncrement?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            
            // Prepare image parts
            val imageParts = imageUris.mapNotNull { uri -> prepareImagePart(uri) }
            
            val response = apiService.createListing(
                title = titleBody,
                description = descriptionBody,
                price = priceBody,
                location = locationBody,
                category = categoryBody,
                listingType = listingTypeBody,
                endTime = endTimeBody,
                minBidIncrement = minBidIncrementBody,
                images = imageParts
            )
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Log.d(tag, "Successfully created listing")
                    Resource.Success(body.data)
                } else {
                    val errorMsg = body?.message ?: "Failed to create listing"
                    Log.e(tag, "API returned error: $errorMsg")
                    Resource.Error(errorMsg)
                }
            } else {
                val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                Log.e(tag, "API request failed: $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error creating listing", e)
            Resource.Error(e.message ?: "Failed to create listing")
        }
    }

    /**
     * Place a bid on a listing
     */
    suspend fun placeBid(listingId: Int, bidAmount: Double): Resource<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Placing bid: listingId=$listingId, amount=$bidAmount")
            
            val request = com.example.mineteh.models.BidRequest(listingId, bidAmount)
            val response = apiService.placeBid(request)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    Log.d(tag, "Bid placed successfully")
                    Resource.Success(true)
                } else {
                    val errorMsg = body?.message ?: "Failed to place bid"
                    Log.e(tag, "API returned error: $errorMsg")
                    Resource.Error(errorMsg)
                }
            } else {
                val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                Log.e(tag, "API request failed: $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error placing bid", e)
            Resource.Error(e.message ?: "Failed to place bid")
        }
    }

    /**
     * Toggle favorite status for a listing
     */
    suspend fun toggleFavorite(listingId: Int): Resource<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Toggling favorite: listingId=$listingId")
            
            val request = com.example.mineteh.models.FavoriteRequest(listingId)
            val response = apiService.toggleFavorite(request)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Log.d(tag, "Favorite toggled: isFavorited=${body.data.isFavorited}")
                    Resource.Success(body.data.isFavorited)
                } else {
                    val errorMsg = body?.message ?: "Failed to toggle favorite"
                    Log.e(tag, "API returned error: $errorMsg")
                    Resource.Error(errorMsg)
                }
            } else {
                val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                Log.e(tag, "API request failed: $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error toggling favorite", e)
            Resource.Error(e.message ?: "Failed to toggle favorite")
        }
    }

    /**
     * Get user's favorite listings
     */
    suspend fun getFavorites(): Resource<List<Listing>> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Fetching favorites")
            
            val response = apiService.getFavorites()
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Log.d(tag, "Successfully fetched ${body.data.size} favorites")
                    Resource.Success(body.data)
                } else {
                    val errorMsg = body?.message ?: "Failed to load favorites"
                    Log.e(tag, "API returned error: $errorMsg")
                    Resource.Error(errorMsg)
                }
            } else {
                val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                Log.e(tag, "API request failed: $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching favorites", e)
            Resource.Error(e.message ?: "Failed to load favorites")
        }
    }

    /**
     * Prepare an image part for multipart upload
     */
    private fun prepareImagePart(uri: Uri): MultipartBody.Part? {
        return try {
            val file = getFileFromUri(uri) ?: return null
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("images[]", file.name, requestFile)
        } catch (e: Exception) {
            Log.e(tag, "Error preparing image part", e)
            null
        }
    }

    /**
     * Convert URI to File
     */
    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            file
        } catch (e: Exception) {
            Log.e(tag, "Error converting URI to file", e)
            null
        }
    }
}
