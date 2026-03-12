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

    init {
        android.util.Log.d("ListingsRepository", "Constructor called with context: $context")
        android.util.Log.d("ListingsRepository", "ApiService initialized: $apiService")
        android.util.Log.d("ListingsRepository", "TokenManager initialized: $tokenManager")
    }

    suspend fun getListings(
        category: String? = null,
        type: String? = null,
        search: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ) = withContext(Dispatchers.IO) {
        android.util.Log.d("ListingsRepository", "getListings() called with category=$category, type=$type, search=$search, limit=$limit, offset=$offset")
        try {
            android.util.Log.d("ListingsRepository", "Fetching listings from Supabase...")
            
            // Query listings table with joins to get images and seller info
            // Note: Supabase Postgrest 2.0.0 has limited API, so we'll fetch all data and filter in Kotlin
            val response = com.example.mineteh.supabase.SupabaseClient.database
                .from("listings")
                .select()
            
            android.util.Log.d("ListingsRepository", "Raw response: ${response.data}")
            android.util.Log.d("ListingsRepository", "Response length: ${response.data.length}")
            
            // Check if response is empty
            if (response.data.isEmpty() || response.data == "[]") {
                android.util.Log.w("ListingsRepository", "Empty response from Supabase")
                return@withContext Resource.Success(emptyList())
            }
            
            // Parse the response into List<Listing>
            val listings = parseListingsResponse(response.data)
            
            android.util.Log.d("ListingsRepository", "Parsed ${listings.size} listings")
            
            // Apply filters in Kotlin
            var filteredListings = listings
            
            category?.let { cat ->
                android.util.Log.d("ListingsRepository", "Filtering by category: $cat")
                filteredListings = filteredListings.filter { it.category.equals(cat, ignoreCase = true) }
            }
            
            type?.let { t ->
                android.util.Log.d("ListingsRepository", "Filtering by type: $t")
                filteredListings = filteredListings.filter { it.listingType.equals(t, ignoreCase = true) }
            }
            
            search?.let { s ->
                android.util.Log.d("ListingsRepository", "Filtering by search: $s")
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
        android.util.Log.d("ListingsRepository", "getListing() called with id=$id")
        try {
            android.util.Log.d("ListingsRepository", "Fetching listing from Supabase...")
            
            // Query single listing
            val response = com.example.mineteh.supabase.SupabaseClient.database
                .from("listings")
                .select()
                .eq("id", id.toString())
                .single()
            
            android.util.Log.d("ListingsRepository", "Raw response: ${response.data}")
            
            if (response.data.isEmpty()) {
                android.util.Log.w("ListingsRepository", "Listing not found")
                return@withContext Resource.Error<Listing>("Listing not found")
            }
            
            // Parse the response
            val listings = parseListingsResponse("[${response.data}]")
            
            if (listings.isEmpty()) {
                return@withContext Resource.Error<Listing>("Failed to parse listing")
            }
            
            android.util.Log.d("ListingsRepository", "Successfully loaded listing ${listings[0].id}")
            Resource.Success(listings[0])
            
        } catch (e: Exception) {
            android.util.Log.e("ListingsRepository", "Error loading listing", e)
            Resource.Error<Listing>(e.message ?: "Failed to load listing")
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
        try {
            android.util.Log.d("ListingsRepository", "Parsing listings response...")
            val json = Json { ignoreUnknownKeys = true }
            val jsonArray = json.parseToJsonElement(jsonData).jsonArray
            
            android.util.Log.d("ListingsRepository", "Found ${jsonArray.size} listings in response")
            
            // Fetch all listing images
            val imagesResponse = com.example.mineteh.supabase.SupabaseClient.database
                .from("listing_images")
                .select()
            android.util.Log.d("ListingsRepository", "Images response: ${imagesResponse.data}")
            val allImages = parseListingImages(imagesResponse.data)
            android.util.Log.d("ListingsRepository", "Parsed ${allImages.size} images")
            
            // Fetch all accounts (sellers)
            val accountsResponse = com.example.mineteh.supabase.SupabaseClient.database
                .from("accounts")
                .select()
            android.util.Log.d("ListingsRepository", "Accounts response: ${accountsResponse.data}")
            val allAccounts = parseAccounts(accountsResponse.data)
            android.util.Log.d("ListingsRepository", "Parsed ${allAccounts.size} accounts")
            
            return jsonArray.mapIndexedNotNull { index, element ->
                try {
                    val obj = element.jsonObject
                    
                    val listingId = obj["id"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    val sellerId = obj["seller_id"]?.jsonPrimitive?.content?.toIntOrNull()
                    
                    android.util.Log.d("ListingsRepository", "Parsing listing $index: id=$listingId, sellerId=$sellerId")
                    
                    // Find images for this listing
                    val images = allImages.filter { it.first == listingId }.map { it.second }
                    android.util.Log.d("ListingsRepository", "  - Found ${images.size} images for listing $listingId")
                    images.forEachIndexed { imgIdx, img ->
                        android.util.Log.d("ListingsRepository", "    - Image $imgIdx: ${img.imagePath}")
                    }
                    
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
                } catch (e: Exception) {
                    android.util.Log.e("ListingsRepository", "Error parsing listing at index $index", e)
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ListingsRepository", "Error in parseListingsResponse", e)
            return emptyList()
        }
    }
    
    /**
     * Parses listing images from JSON response
     * Returns a list of Pair<listingId, ListingImage>
     */
    private fun parseListingImages(jsonData: String): List<Pair<Int, ListingImage>> {
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val jsonArray = json.parseToJsonElement(jsonData).jsonArray
            
            jsonArray.mapNotNull { element ->
                try {
                    val obj = element.jsonObject
                    val listingId = obj["listing_id"]?.jsonPrimitive?.content?.toIntOrNull() ?: return@mapNotNull null
                    val imagePath = obj["image_path"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    
                    android.util.Log.d("ListingsRepository", "Parsed image: listingId=$listingId, path=$imagePath")
                    listingId to ListingImage(imagePath = imagePath)
                } catch (e: Exception) {
                    android.util.Log.e("ListingsRepository", "Error parsing image", e)
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ListingsRepository", "Error in parseListingImages", e)
            emptyList()
        }
    }
    
    /**
     * Parses accounts (sellers) from JSON response
     */
    private fun parseAccounts(jsonData: String): List<Seller> {
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val jsonArray = json.parseToJsonElement(jsonData).jsonArray
            
            jsonArray.mapNotNull { element ->
                try {
                    val obj = element.jsonObject
                    Seller(
                        accountId = obj["account_id"]?.jsonPrimitive?.content?.toIntOrNull(),
                        username = obj["username"]?.jsonPrimitive?.content ?: "",
                        firstName = obj["first_name"]?.jsonPrimitive?.content ?: "",
                        lastName = obj["last_name"]?.jsonPrimitive?.content ?: ""
                    )
                } catch (e: Exception) {
                    android.util.Log.e("ListingsRepository", "Error parsing account", e)
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ListingsRepository", "Error in parseAccounts", e)
            emptyList()
        }
    }
}
