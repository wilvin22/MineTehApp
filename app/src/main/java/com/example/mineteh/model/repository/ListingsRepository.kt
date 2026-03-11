package com.example.mineteh.model.repository

import android.content.Context
import android.net.Uri
import com.example.mineteh.models.Listing
import com.example.mineteh.models.ListingImage
import com.example.mineteh.models.Seller
import com.example.mineteh.models.Bid
import com.example.mineteh.network.ApiClient
import com.example.mineteh.utils.Resource
import com.example.mineteh.utils.TokenManager
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class ListingsRepository(private val context: Context) {
    private val apiService = ApiClient.apiService
    private val tokenManager = TokenManager(context)

    suspend fun getListings(
        category: String? = null,
        type: String? = null,
        search: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ) = withContext(Dispatchers.IO) {
        try {
            // Query listings table
            val response = com.example.mineteh.supabase.SupabaseClient.database
                .from("listings")
                .select()
            
            // Parse the response into List<Listing>
            val listings = parseListingsResponse(response.data)
            
            // Apply filters in Kotlin (since Supabase Postgrest 2.0.0 API is limited)
            var filteredListings = listings
            
            category?.let { cat ->
                filteredListings = filteredListings.filter { it.category == cat }
            }
            
            type?.let { t ->
                filteredListings = filteredListings.filter { it.listingType == t }
            }
            
            search?.let { s ->
                filteredListings = filteredListings.filter { 
                    it.title.contains(s, ignoreCase = true)
                }
            }
            
            // Sort by created_at descending
            filteredListings = filteredListings.sortedByDescending { it.createdAt }
            
            // Apply pagination
            val paginatedListings = filteredListings.drop(offset).take(limit)
            
            Resource.Success(paginatedListings)
            
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load listings")
        }
    }

    suspend fun getListing(id: Int) = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getListing(id)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {
                    Resource.Success(body.data)
                } else {
                    Resource.Error(body?.message ?: "Failed to load listing")
                }
            } else {
                Resource.Error("Error: ${response.code()} ${response.message()}")
            }
        } catch (e: JsonSyntaxException) {
            Resource.Error("Data format error")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
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
        imageUris: List<Uri>
    ): Resource<Listing> = withContext(Dispatchers.IO) {
        try {
            // Check if logged in
            if (!tokenManager.isLoggedIn()) {
                return@withContext Resource.Error("Not authenticated")
            }

            val titlePart = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val descriptionPart = description.toRequestBody("text/plain".toMediaTypeOrNull())
            val pricePart = price.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val locationPart = location.toRequestBody("text/plain".toMediaTypeOrNull())
            val categoryPart = category.toRequestBody("text/plain".toMediaTypeOrNull())
            val typePart = listingType.toRequestBody("text/plain".toMediaTypeOrNull())
            val endTimePart = endTime?.toRequestBody("text/plain".toMediaTypeOrNull())
            val incrementPart = minBidIncrement?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

            val imageParts = imageUris.mapNotNull { uri ->
                prepareImagePart(uri)
            }

            if (imageParts.isEmpty()) {
                return@withContext Resource.Error("Please select at least one image")
            }

            val response = apiService.createListing(
                titlePart,
                descriptionPart,
                pricePart,
                locationPart,
                categoryPart,
                typePart,
                endTimePart,
                incrementPart,
                imageParts
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {
                    Resource.Success(body.data)
                } else {
                    Resource.Error(body?.message ?: "Failed to create listing")
                }
            } else {
                Resource.Error("Error: ${response.code()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    private fun prepareImagePart(uri: Uri): MultipartBody.Part? {
        val file = getFileFromUri(uri) ?: return null
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("images[]", file.name, requestFile)
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}_${(0..1000).random()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Parses the JSON response from Supabase into a List<Listing>
     */
    private fun parseListingsResponse(jsonData: String): List<Listing> {
        val json = Json { ignoreUnknownKeys = true }
        val jsonArray = json.parseToJsonElement(jsonData).jsonArray
        
        return jsonArray.map { element ->
            val obj = element.jsonObject
            
            // Parse listing images
            val images = obj["listing_images"]?.jsonArray?.map { imgObj ->
                val imgData = imgObj.jsonObject
                ListingImage(
                    imagePath = imgData["image_url"]?.jsonPrimitive?.content ?: ""
                )
            } ?: emptyList()
            
            // Parse seller information
            val sellerObj = obj["accounts"]?.jsonObject
            val seller = sellerObj?.let {
                Seller(
                    accountId = it["account_id"]?.jsonPrimitive?.content?.toIntOrNull(),
                    username = it["username"]?.jsonPrimitive?.content ?: "",
                    firstName = it["first_name"]?.jsonPrimitive?.content ?: "",
                    lastName = it["last_name"]?.jsonPrimitive?.content ?: ""
                )
            }
            
            // Parse highest bid if present
            val highestBidObj = obj["highest_bid"]?.jsonObject
            val highestBid = highestBidObj?.let {
                Bid(
                    bidAmount = it["bid_amount"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                    bidTime = it["bid_time"]?.jsonPrimitive?.content ?: "",
                    bidder = null
                )
            }
            
            // Create Listing object
            Listing(
                id = obj["id"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                title = obj["title"]?.jsonPrimitive?.content ?: "",
                description = obj["description"]?.jsonPrimitive?.content ?: "",
                price = obj["price"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                location = obj["location"]?.jsonPrimitive?.content ?: "",
                category = obj["category"]?.jsonPrimitive?.content ?: "",
                listingType = obj["listing_type"]?.jsonPrimitive?.content ?: "",
                status = obj["status"]?.jsonPrimitive?.content ?: "",
                image = images.firstOrNull()?.imagePath,
                images = images,
                seller = seller,
                createdAt = obj["created_at"]?.jsonPrimitive?.content ?: "",
                isFavorited = obj["is_favorited"]?.jsonPrimitive?.content?.toBoolean() ?: false,
                highestBid = highestBid,
                endTime = obj["end_time"]?.jsonPrimitive?.content
            )
        }
    }
}
