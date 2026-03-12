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
            // Query listings table with joins to get images and seller info
            // Note: Supabase Postgrest 2.0.0 has limited API, so we'll fetch all data and filter in Kotlin
            val response = com.example.mineteh.supabase.SupabaseClient.database
                .from("listings")
                .select()
            
            android.util.Log.d("ListingsRepository", "Raw response: ${response.data}")
            
            // Parse the response into List<Listing>
            val listings = parseListingsResponse(response.data)
            
            android.util.Log.d("ListingsRepository", "Parsed ${listings.size} listings")
            
            // Apply filters in Kotlin
            var filteredListings = listings
            
            category?.let { cat ->
                filteredListings = filteredListings.filter { it.category.equals(cat, ignoreCase = true) }
            }
            
            type?.let { t ->
                filteredListings = filteredListings.filter { it.listingType.equals(t, ignoreCase = true) }
            }
            
            search?.let { s ->
                filteredListings = filteredListings.filter { 
                    it.title.contains(s, ignoreCase = true) || 
                    it.description.contains(s, ignoreCase = true)
                }
            }
            
            // Sort by created_at descending (newest first)
            filteredListings = filteredListings.sortedByDescending { it.createdAt }
            
            // Apply pagination
            val paginatedListings = filteredListings.drop(offset).take(limit)
            
            android.util.Log.d("ListingsRepository", "Returning ${paginatedListings.size} listings after filters")
            Resource.Success(paginatedListings)
            
        } catch (e: Exception) {
            android.util.Log.e("ListingsRepository", "Error loading listings", e)
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
     * Also fetches related images and seller information for each listing
     */
    private suspend fun parseListingsResponse(jsonData: String): List<Listing> {
        val json = Json { ignoreUnknownKeys = true }
        val jsonArray = json.parseToJsonElement(jsonData).jsonArray
        
        // Fetch all listing images
        val imagesResponse = com.example.mineteh.supabase.SupabaseClient.database
            .from("listing_images")
            .select()
        val allImages = parseListingImages(imagesResponse.data)
        
        // Fetch all accounts (sellers)
        val accountsResponse = com.example.mineteh.supabase.SupabaseClient.database
            .from("accounts")
            .select()
        val allAccounts = parseAccounts(accountsResponse.data)
        
        return jsonArray.map { element ->
            val obj = element.jsonObject
            
            val listingId = obj["id"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val sellerId = obj["seller_id"]?.jsonPrimitive?.content?.toIntOrNull()
            
            // Find images for this listing
            val images = allImages.filter { it.first == listingId }.map { it.second }
            
            // Find seller for this listing
            val seller = sellerId?.let { id ->
                allAccounts.find { it.accountId == id }
            }
            
            // Create Listing object
            Listing(
                id = listingId,
                title = obj["title"]?.jsonPrimitive?.content ?: "",
                description = obj["description"]?.jsonPrimitive?.content ?: "",
                price = obj["price"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                location = obj["location"]?.jsonPrimitive?.content ?: "",
                category = obj["category"]?.jsonPrimitive?.content ?: "",
                listingType = obj["listing_type"]?.jsonPrimitive?.content ?: "",
                status = obj["status"]?.jsonPrimitive?.content ?: "active",
                image = images.firstOrNull()?.imagePath,
                images = images,
                seller = seller,
                createdAt = obj["created_at"]?.jsonPrimitive?.content ?: "",
                isFavorited = false, // Will be determined separately if needed
                highestBid = null, // Will be fetched separately if needed
                endTime = obj["end_time"]?.jsonPrimitive?.content
            )
        }
    }
    
    /**
     * Parses listing images from JSON response
     * Returns a list of Pair<listingId, ListingImage>
     */
    private fun parseListingImages(jsonData: String): List<Pair<Int, ListingImage>> {
        val json = Json { ignoreUnknownKeys = true }
        val jsonArray = json.parseToJsonElement(jsonData).jsonArray
        
        return jsonArray.mapNotNull { element ->
            val obj = element.jsonObject
            val listingId = obj["listing_id"]?.jsonPrimitive?.content?.toIntOrNull() ?: return@mapNotNull null
            val imagePath = obj["image_path"]?.jsonPrimitive?.content ?: return@mapNotNull null
            
            listingId to ListingImage(imagePath = imagePath)
        }
    }
    
    /**
     * Parses accounts (sellers) from JSON response
     */
    private fun parseAccounts(jsonData: String): List<Seller> {
        val json = Json { ignoreUnknownKeys = true }
        val jsonArray = json.parseToJsonElement(jsonData).jsonArray
        
        return jsonArray.map { element ->
            val obj = element.jsonObject
            Seller(
                accountId = obj["account_id"]?.jsonPrimitive?.content?.toIntOrNull(),
                username = obj["username"]?.jsonPrimitive?.content ?: "",
                firstName = obj["first_name"]?.jsonPrimitive?.content ?: "",
                lastName = obj["last_name"]?.jsonPrimitive?.content ?: ""
            )
        }
    }
}
