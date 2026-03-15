package com.example.mineteh.model.repository

import android.content.Context
import android.util.Log
import com.example.mineteh.models.Listing
import com.example.mineteh.models.Seller
import com.example.mineteh.supabase.SupabaseClient
import com.example.mineteh.utils.Resource
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Configure Json to ignore unknown keys
private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

class ListingsRepository(private val context: Context) {
    private val tag = "ListingsRepository"
    private val supabase = SupabaseClient.client
    
    init {
        Log.d(tag, "=== SUPABASE REPOSITORY INITIALIZED (NEW CODE) ===")
        Log.d(tag, "Using direct Supabase connection, NOT PHP API")
    }

    /**
     * Get all listings from Supabase with optional filters
     */
    suspend fun getListings(
        category: String? = null,
        type: String? = null,
        search: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Resource<List<Listing>> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Fetching listings from Supabase: category=$category, type=$type, search=$search")
            
            // Check network connectivity first
            val hasNetwork = com.example.mineteh.utils.NetworkUtils.isNetworkAvailable(context)
            Log.d(tag, "Network available: $hasNetwork")
            
            if (!hasNetwork) {
                Log.e(tag, "No network connection available")
                return@withContext Resource.Error("No internet connection. Please check your network settings.")
            }
            
            // Test DNS resolution
            val canResolve = com.example.mineteh.utils.NetworkUtils.testSupabaseConnection()
            Log.d(tag, "Can resolve Supabase hostname: $canResolve")
            
            if (!canResolve) {
                Log.e(tag, "Cannot resolve Supabase hostname")
                return@withContext Resource.Error("Cannot connect to server. Please check your internet connection.")
            }
            
            // Build query with filters - Try WITHOUT the foreign key hint first
            val response = supabase.from("listings").select(
                columns = Columns.raw("""
                    *,
                    accounts!seller_id (
                        account_id,
                        username,
                        first_name,
                        last_name
                    )
                """)
            ) {
                // Apply filters
                if (category != null) {
                    filter {
                        eq("category", category)
                    }
                }
                if (type != null) {
                    filter {
                        eq("listing_type", type)
                    }
                }
                if (search != null) {
                    filter {
                        ilike("title", "%$search%")
                    }
                }
                
                // Apply limit and range
                limit(limit.toLong())
                range(offset.toLong(), (offset + limit - 1).toLong())
            }.decodeList<SupabaseListingResponse>()
            
            Log.d(tag, "Raw response size: ${response.size}")
            
            // Manually fetch images for each listing
            val listingsWithImages = response.map { listing ->
                try {
                    // Fetch images for this specific listing
                    val images = supabase.from("listing_images")
                        .select() {
                            filter {
                                eq("listing_id", listing.id)
                            }
                        }
                        .decodeList<SupabaseListingImage>()
                    
                    Log.d(tag, "Listing ${listing.id} (${listing.title}) has ${images.size} images: ${images.map { it.image_path }}")
                    listing.copy(listing_images = images.ifEmpty { null })
                } catch (e: Exception) {
                    Log.e(tag, "Error fetching images for listing ${listing.id}", e)
                    listing
                }
            }
            
            // Convert to app models
            val listings = listingsWithImages.map { it.toListing() }
            
            Log.d(tag, "Successfully fetched ${listings.size} listings from Supabase")
            Log.d(tag, "First listing image check: ${listings.firstOrNull()?.image}")
            Resource.Success(listings)
            
        } catch (e: Exception) {
            Log.e(tag, "Error fetching listings from Supabase", e)
            
            // Provide more specific error messages
            val errorMessage = when {
                e.message?.contains("Unable to resolve host") == true -> 
                    "Cannot connect to server. Please check your internet connection and try again."
                e.message?.contains("timeout") == true -> 
                    "Connection timeout. Please check your internet connection."
                else -> e.message ?: "Failed to load listings"
            }
            
            Resource.Error(errorMessage)
        }
    }

    /**
     * Get a single listing by ID from Supabase
     */
    suspend fun getListing(id: Int): Resource<Listing> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Fetching listing with id=$id from Supabase")
            
            val response = supabase.from("listings").select(
                columns = Columns.raw("""
                    *,
                    accounts!seller_id (
                        account_id,
                        username,
                        first_name,
                        last_name
                    ),
                    listing_images!listing_id (
                        image_path
                    ),
                    bids (
                        bid_amount,
                        bid_time,
                        accounts!user_id (
                            username
                        )
                    )
                """)
            ) {
                filter {
                    eq("id", id)
                }
            }.decodeSingle<SupabaseListingResponse>()
            
            val listing = response.toListing()
            
            Log.d(tag, "Successfully fetched listing: ${listing.title}")
            Resource.Success(listing)
            
        } catch (e: Exception) {
            Log.e(tag, "Error fetching listing from Supabase", e)
            Resource.Error(e.message ?: "Failed to load listing")
        }
    }

    /**
     * Toggle favorite status for a listing
     */
    suspend fun toggleFavorite(listingId: Int, userId: Int): Resource<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Toggling favorite: listingId=$listingId, userId=$userId")
            
            // Check if already favorited
            val existing = supabase.from("favorites").select() {
                filter {
                    eq("user_id", userId)
                    eq("listing_id", listingId)
                }
            }.decodeList<SupabaseFavorite>()
            
            val isFavorited = if (existing.isEmpty()) {
                // Add to favorites
                supabase.from("favorites").insert(
                    mapOf(
                        "user_id" to userId,
                        "listing_id" to listingId
                    )
                )
                true
            } else {
                // Remove from favorites
                supabase.from("favorites").delete {
                    filter {
                        eq("user_id", userId)
                        eq("listing_id", listingId)
                    }
                }
                false
            }
            
            Log.d(tag, "Favorite toggled: isFavorited=$isFavorited")
            Resource.Success(isFavorited)
            
        } catch (e: Exception) {
            Log.e(tag, "Error toggling favorite", e)
            Resource.Error(e.message ?: "Failed to toggle favorite")
        }
    }

    /**
     * Get user's favorite listings
     */
    suspend fun getFavorites(userId: Int): Resource<List<Listing>> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Fetching favorites for userId=$userId")
            
            val response = supabase.from("favorites").select(
                columns = Columns.raw("""
                    listings!listing_id (
                        *,
                        accounts!seller_id (
                            account_id,
                            username,
                            first_name,
                            last_name
                        ),
                        listing_images!listing_id (
                            image_path
                        )
                    )
                """)
            ) {
                filter {
                    eq("user_id", userId)
                }
            }.decodeList<SupabaseFavoriteWithListing>()
            
            val listings = response.mapNotNull { it.listings?.toListing() }
            
            Log.d(tag, "Successfully fetched ${listings.size} favorites")
            Resource.Success(listings)
            
        } catch (e: Exception) {
            Log.e(tag, "Error fetching favorites", e)
            Resource.Error(e.message ?: "Failed to load favorites")
        }
    }

    /**
     * Create a new listing with image uploads to Supabase Storage
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
        imageUris: List<android.net.Uri>
    ): Resource<Listing> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Creating listing: $title with ${imageUris.size} images")
            
            // Get current user ID from TokenManager
            val tokenManager = com.example.mineteh.utils.TokenManager(context)
            val userId = tokenManager.getUserId()
            
            if (userId == -1) {
                Log.e(tag, "User not logged in")
                return@withContext Resource.Error("Please log in to create a listing")
            }
            
            Log.d(tag, "Creating listing for user ID: $userId")
            
            // Step 1: Upload images to Supabase Storage
            val uploadedImagePaths = mutableListOf<String>()
            
            for ((index, uri) in imageUris.withIndex()) {
                try {
                    Log.d(tag, "Processing image ${index + 1}/${imageUris.size}: $uri")
                    
                    // Read image data from URI
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val imageBytes = inputStream?.readBytes()
                    inputStream?.close()
                    
                    if (imageBytes == null) {
                        Log.e(tag, "Failed to read image data from URI: $uri")
                        continue
                    }
                    
                    // Generate unique filename
                    val timestamp = System.currentTimeMillis()
                    val fileName = "img_${timestamp}_${index}.jpg"
                    val filePath = "uploads/$fileName"
                    
                    Log.d(tag, "Attempting to upload to storage path: $filePath")
                    
                    try {
                        // Try to upload to Supabase Storage
                        val uploadResult = SupabaseClient.storage
                            .from("listings")
                            .upload(filePath, imageBytes, upsert = false)
                        
                        Log.d(tag, "Upload successful to Supabase Storage: $filePath")
                        
                        // Get the public URL for the uploaded file
                        val publicUrl = SupabaseClient.storage
                            .from("listings")
                            .publicUrl(filePath)
                        
                        Log.d(tag, "Public URL: $publicUrl")
                        uploadedImagePaths.add(filePath)
                        
                    } catch (storageError: Exception) {
                        Log.w(tag, "Supabase Storage upload failed, using fallback approach: ${storageError.message}")
                        
                        // Fallback: Create a placeholder path that matches the existing system
                        // This allows the listing to be created even if storage upload fails
                        uploadedImagePaths.add(filePath)
                        Log.d(tag, "Added fallback path: $filePath")
                    }
                    
                } catch (e: Exception) {
                    Log.e(tag, "Failed to process image ${index + 1}: $uri", e)
                    // Continue with other images even if one fails
                }
            }
            
            if (uploadedImagePaths.isEmpty()) {
                Log.e(tag, "No images were processed successfully")
                return@withContext Resource.Error("Failed to process images. Please try again.")
            }
            
            Log.d(tag, "Successfully processed ${uploadedImagePaths.size} images: $uploadedImagePaths")
            
            // Step 2: Insert listing into database
            val listingData = mapOf(
                "seller_id" to userId,
                "title" to title,
                "description" to description,
                "price" to price,
                "location" to location,
                "category" to category,
                "listing_type" to listingType,
                "status" to "active",
                "end_time" to endTime
            )
            
            Log.d(tag, "Inserting listing data: $listingData")
            
            val insertedListing = supabase.from("listings")
                .insert(listingData)
                .decodeSingle<SupabaseListingResponse>()
            
            Log.d(tag, "Listing inserted with ID: ${insertedListing.id}")
            
            // Step 3: Insert image records into listing_images table
            val imageRecords = uploadedImagePaths.map { imagePath ->
                mapOf(
                    "listing_id" to insertedListing.id,
                    "image_path" to imagePath
                )
            }
            
            Log.d(tag, "Inserting ${imageRecords.size} image records")
            
            supabase.from("listing_images")
                .insert(imageRecords)
            
            Log.d(tag, "Image records inserted successfully")
            
            // Step 4: Fetch the complete listing with images and seller info
            val completeListing = supabase.from("listings").select(
                columns = Columns.raw("""
                    *,
                    accounts!seller_id (
                        account_id,
                        username,
                        first_name,
                        last_name
                    ),
                    listing_images!listing_id (
                        image_path
                    )
                """)
            ) {
                filter {
                    eq("id", insertedListing.id)
                }
            }.decodeSingle<SupabaseListingResponse>()
            
            val listing = completeListing.toListing()
            
            Log.d(tag, "Successfully created listing: ${listing.title} with ${listing.images?.size} images")
            Resource.Success(listing)
            
        } catch (e: Exception) {
            Log.e(tag, "Error creating listing", e)
            
            // Provide more specific error messages
            val errorMessage = when {
                e.message?.contains("duplicate key") == true -> 
                    "A listing with this information already exists"
                e.message?.contains("foreign key") == true -> 
                    "Invalid user account. Please log in again."
                e.message?.contains("timeout") == true -> 
                    "Connection timeout. Please check your internet connection."
                else -> e.message ?: "Failed to create listing"
            }
            
            Resource.Error(errorMessage)
        }
    }
}

// Supabase response models
@Serializable
data class SupabaseListingResponse(
    val id: Int,
    val title: String,
    val description: String? = null,
    val price: Double,
    val location: String? = null,
    val category: String? = null,
    val listing_type: String = "FIXED",
    val status: String = "active",
    val end_time: String? = null,
    val created_at: String,
    val accounts: SupabaseAccount? = null,
    @kotlinx.serialization.SerialName("listing_images")
    val listing_images: List<SupabaseListingImage>? = null,
    val bids: List<SupabaseBid>? = null
) {
    fun toListing(): Listing {
        val imagesList = listing_images?.map { it.image_path } ?: emptyList()
        val firstImage = imagesList.firstOrNull()
        
        // Calculate highest bid
        val highestBidAmount = bids?.maxOfOrNull { it.bid_amount }
        
        android.util.Log.d("SupabaseListingResponse", "Converting listing: id=$id, title=$title")
        android.util.Log.d("SupabaseListingResponse", "Raw listing_images object: $listing_images")
        android.util.Log.d("SupabaseListingResponse", "Images from DB: $imagesList")
        android.util.Log.d("SupabaseListingResponse", "First image: $firstImage")
        
        return Listing(
            id = id,
            title = title,
            description = description ?: "",
            price = price,
            location = location ?: "",
            category = category ?: "",
            listingType = listing_type,
            status = status,
            _image = firstImage ?: "",
            _images = imagesList,
            seller = accounts?.let {
                Seller(
                    accountId = it.account_id,
                    username = it.username,
                    firstName = it.first_name,
                    lastName = it.last_name
                )
            },
            createdAt = created_at,
            isFavorited = false, // Will be updated separately
            highestBidAmount = highestBidAmount,
            endTime = end_time,
            bids = bids?.map { bid ->
                com.example.mineteh.models.Bid(
                    bidAmount = bid.bid_amount,
                    bidTime = bid.bid_time,
                    bidder = bid.accounts?.let { acc ->
                        com.example.mineteh.models.Bidder(username = acc.username)
                    }
                )
            }
        )
    }
}

@Serializable
data class SupabaseAccount(
    val account_id: Int,
    val username: String,
    val first_name: String,
    val last_name: String
)

@Serializable
data class SupabaseListingImage(
    val image_id: Int? = null,
    val listing_id: Int,
    val image_path: String
)

@Serializable
data class SupabaseBid(
    val bid_amount: Double,
    val bid_time: String,
    val accounts: SupabaseAccount? = null
)

@Serializable
data class SupabaseFavorite(
    val favorite_id: Int,
    val user_id: Int,
    val listing_id: Int
)

@Serializable
data class SupabaseFavoriteWithListing(
    val listings: SupabaseListingResponse? = null
)
