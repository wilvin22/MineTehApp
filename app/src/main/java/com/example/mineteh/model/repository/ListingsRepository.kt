package com.example.mineteh.model.repository

import android.content.Context
import android.util.Log
import com.example.mineteh.models.BidData
import com.example.mineteh.models.BidRequest
import com.example.mineteh.models.CreateListingRequest
import com.example.mineteh.models.FavoriteRequest
import com.example.mineteh.models.Listing
import com.example.mineteh.network.ApiClient
import com.example.mineteh.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ListingsRepository(private val context: Context) {
    private val tag = "ListingsRepository"
    private val api = ApiClient.apiService

    suspend fun getListings(
        category: String? = null,
        type: String? = null,
        search: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Resource<List<Listing>> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Fetching listings: category=$category, type=$type, search=$search")
            val response = api.getListings(category, type, search, limit, offset)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    Resource.Success(body.data ?: emptyList())
                } else {
                    Resource.Error(body?.message ?: "Failed to load listings")
                }
            } else {
                Resource.Error("Server error: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching listings", e)
            val msg = when {
                e.message?.contains("Unable to resolve host") == true ->
                    "Cannot connect to server. Please check your internet connection."
                e.message?.contains("timeout") == true ->
                    "Connection timeout. Please try again."
                else -> e.message ?: "Failed to load listings"
            }
            Resource.Error(msg)
        }
    }

    suspend fun getListing(id: Int): Resource<Listing> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Fetching listing id=$id")
            val response = api.getListing(id)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Resource.Success(body.data)
                } else {
                    Resource.Error(body?.message ?: "Listing not found")
                }
            } else {
                Resource.Error("Server error: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching listing $id", e)
            Resource.Error(e.message ?: "Failed to load listing")
        }
    }

    suspend fun createListing(
        title: String,
        description: String,
        price: Double,
        location: String,
        category: String,
        listingType: String,
        endTime: String?,
        minBidIncrement: Double?,
        imageUris: List<android.net.Uri>
    ): Resource<Listing> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Creating listing: $title with ${imageUris.size} images")

            val toBody = { s: String -> s.toRequestBody("text/plain".toMediaTypeOrNull()) }

            val imageParts = imageUris.mapIndexedNotNull { i, uri ->
                try {
                    val stream = context.contentResolver.openInputStream(uri) ?: return@mapIndexedNotNull null
                    val bytes = stream.readBytes()
                    stream.close()
                    val reqBody = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("images[]", "image_$i.jpg", reqBody)
                } catch (e: Exception) {
                    Log.e(tag, "Failed to read image $i", e)
                    null
                }
            }

            val response = api.createListing(
                title = toBody(title),
                description = toBody(description),
                price = toBody(price.toString()),
                location = toBody(location),
                category = toBody(category),
                listingType = toBody(listingType),
                endTime = endTime?.let { toBody(it) },
                minBidIncrement = minBidIncrement?.let { toBody(it.toString()) },
                images = imageParts
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Resource.Success(body.data)
                } else {
                    Resource.Error(body?.message ?: "Failed to create listing")
                }
            } else {
                Resource.Error("Server error: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error creating listing", e)
            Resource.Error(e.message ?: "Failed to create listing")
        }
    }

    suspend fun toggleFavorite(listingId: Int): Resource<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Toggling favorite for listing $listingId")
            val response = api.toggleFavorite(FavoriteRequest(listing_id = listingId))
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Resource.Success(body.data.isFavorited)
                } else {
                    Resource.Error(body?.message ?: "Failed to toggle favorite")
                }
            } else {
                Resource.Error("Server error: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error toggling favorite", e)
            Resource.Error(e.message ?: "Failed to toggle favorite")
        }
    }

    suspend fun getFavorites(): Resource<List<Listing>> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Fetching favorites")
            val response = api.getFavorites()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    Resource.Success(body.data ?: emptyList())
                } else {
                    Resource.Error(body?.message ?: "Failed to load favorites")
                }
            } else {
                Resource.Error("Server error: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching favorites", e)
            Resource.Error(e.message ?: "Failed to load favorites")
        }
    }

    suspend fun placeBid(listingId: Int, bidAmount: Double): Resource<BidData> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Placing bid: listingId=$listingId, amount=$bidAmount")
            val response = api.placeBid(BidRequest(listing_id = listingId, bid_amount = bidAmount))
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    Resource.Success(body.data)
                } else {
                    Resource.Error(body?.message ?: "Failed to place bid")
                }
            } else {
                Resource.Error("Server error: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error placing bid", e)
            Resource.Error(e.message ?: "Failed to place bid")
        }
    }
}
