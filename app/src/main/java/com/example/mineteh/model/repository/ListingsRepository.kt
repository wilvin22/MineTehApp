package com.example.mineteh.model.repository

import android.content.Context
import android.util.Log
import com.example.mineteh.models.BidData
import com.example.mineteh.models.BidRequest
import com.example.mineteh.models.FavoriteRequest
import com.example.mineteh.models.ImageUploadData
import com.example.mineteh.models.Listing
import com.example.mineteh.models.Seller
import com.example.mineteh.models.Bid
import com.example.mineteh.network.ApiClient
import com.example.mineteh.supabase.SupabaseClient
import com.example.mineteh.utils.Resource
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

// Supabase DTOs for listings
@Serializable
private data class SupabaseListing(
    val id: Int,
    val title: String,
    val description: String,
    val price: Double,
    val location: String,
    val category: String,
    val listing_type: String,
    val status: String,
    val seller_id: Int,
    val end_time: String? = null,
    val created_at: String
)

@Serializable
private data class SupabaseListingImage(
    val listing_id: Int,
    val image_path: String,
    val image_id: Int? = null
)

@Serializable
private data class SupabaseAccount(
    val account_id: Int,
    val username: String,
    val first_name: String,
    val last_name: String
)

@Serializable
private data class SupabaseBid(
    val listing_id: Int,
    val bid_amount: Double
)

@Serializable
private data class SupabaseFavorite(
    val listing_id: Int
)

@Serializable
private data class SupabaseFavoriteRow(
    val listing_id: Int
)

@Serializable
private data class InsertListing(
    val title: String,
    val description: String,
    val price: Double,
    val location: String,
    val category: String,
    val listing_type: String,
    val seller_id: Int,
    val status: String,
    val end_time: String? = null,
    val min_bid_increment: Double? = null
)

@Serializable
private data class InsertFavorite(
    val listing_id: Int,
    val user_id: Int
)

@Serializable
private data class UpdateListingStatus(
    val status: String
)

@Serializable
private data class UpdateListing(
    val title: String,
    val description: String,
    val price: Double,
    val location: String,
    val category: String,
    val listing_type: String,
    val end_time: String? = null
)

@Serializable
private data class InsertListingImage(
    val listing_id: Int,
    val image_path: String
)

class ListingsRepository(private val context: Context) {
    private val tag = "ListingsRepository"
    private val api = ApiClient.apiService
    private val supabase = SupabaseClient.client

    suspend fun getListings(
        category: String? = null,
        type: String? = null,
        search: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Resource<List<Listing>> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Fetching listings from Supabase: category=$category, type=$type, search=$search, minPrice=$minPrice, maxPrice=$maxPrice")

            val rows = supabase.from("listings")
                .select(columns = Columns.list(
                    "id", "title", "description", "price", "location",
                    "category", "listing_type", "status", "seller_id", "end_time", "created_at"
                )) {
                    filter {
                        eq("status", "active")
                        if (category != null) eq("category", category)
                        if (type != null) eq("listing_type", type)
                        if (search != null) ilike("title", "%$search%")
                        if (minPrice != null) gte("price", minPrice)
                        if (maxPrice != null) lte("price", maxPrice)
                    }
                    order("created_at", order = Order.DESCENDING)
                    limit(limit.toLong())
                    range(offset.toLong(), (offset + limit - 1).toLong())
                }
                .decodeList<SupabaseListing>()

            if (rows.isEmpty()) return@withContext Resource.Success(emptyList())

            val ids = rows.map { it.id }
            val sellerIds = rows.map { it.seller_id }.distinct()
            val bidIds = rows.filter { it.listing_type == "BID" }.map { it.id }

            // Fetch images
            val imageMap = mutableMapOf<Int, String?>()
            if (ids.isNotEmpty()) {
                val images = supabase.from("listing_images")
                    .select(columns = Columns.list("listing_id", "image_path", "image_id")) {
                        filter { isIn("listing_id", ids) }
                        order("image_id", order = Order.ASCENDING)
                    }
                    .decodeList<SupabaseListingImage>()
                images.forEach { img ->
                    if (!imageMap.containsKey(img.listing_id)) {
                        imageMap[img.listing_id] = img.image_path
                    }
                }
            }

            // Fetch sellers
            val sellerMap = mutableMapOf<Int, SupabaseAccount>()
            if (sellerIds.isNotEmpty()) {
                val sellers = supabase.from("accounts")
                    .select(columns = Columns.list("account_id", "username", "first_name", "last_name")) {
                        filter { isIn("account_id", sellerIds) }
                    }
                    .decodeList<SupabaseAccount>()
                sellers.forEach { sellerMap[it.account_id] = it }
            }

            // Fetch highest bids
            val highestBidMap = mutableMapOf<Int, Double>()
            if (bidIds.isNotEmpty()) {
                val bids = supabase.from("bids")
                    .select(columns = Columns.list("listing_id", "bid_amount")) {
                        filter { isIn("listing_id", bidIds) }
                        order("bid_amount", order = Order.DESCENDING)
                    }
                    .decodeList<SupabaseBid>()
                bids.forEach { bid ->
                    if (!highestBidMap.containsKey(bid.listing_id)) {
                        highestBidMap[bid.listing_id] = bid.bid_amount
                    }
                }
            }

            // Fetch current user's favorited listing IDs
            val favoritedIds = mutableSetOf<Int>()
            val userId = com.example.mineteh.utils.TokenManager(context).getUserId()
            if (userId != -1 && ids.isNotEmpty()) {
                val favRows = supabase.from("favorites")
                    .select(columns = Columns.list("listing_id")) {
                        filter {
                            eq("user_id", userId)
                            isIn("listing_id", ids)
                        }
                    }
                    .decodeList<SupabaseFavoriteRow>()
                favRows.forEach { favoritedIds.add(it.listing_id) }
            }

            val listings = rows.map { row ->
                val acc = sellerMap[row.seller_id]
                Listing(
                    id = row.id,
                    title = row.title,
                    description = row.description,
                    price = row.price,
                    location = row.location,
                    category = row.category,
                    listingType = row.listing_type,
                    status = row.status,
                    _image = imageMap[row.id],
                    _images = null,
                    seller = if (acc != null) Seller(
                        accountId = acc.account_id,
                        username = acc.username,
                        firstName = acc.first_name,
                        lastName = acc.last_name
                    ) else null,
                    createdAt = row.created_at,
                    highestBidAmount = highestBidMap[row.id],
                    endTime = row.end_time,
                    isFavorited = row.id in favoritedIds
                )
            }

            Log.d(tag, "Fetched ${listings.size} listings from Supabase")
            Resource.Success(listings)
        } catch (e: Exception) {
            Log.e(tag, "Error fetching listings", e)
            Resource.Error(e.message ?: "Failed to load listings")
        }
    }

    suspend fun getListing(id: Int): Resource<Listing> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Fetching listing id=$id from Supabase")

            val rows = supabase.from("listings")
                .select(columns = Columns.list(
                    "id", "title", "description", "price", "location",
                    "category", "listing_type", "status", "seller_id", "end_time", "created_at"
                )) {
                    filter { eq("id", id) }
                    limit(1)
                }
                .decodeList<SupabaseListing>()

            val row = rows.firstOrNull() ?: return@withContext Resource.Error("Listing not found")

            // Images
            val images = supabase.from("listing_images")
                .select(columns = Columns.list("listing_id", "image_path", "image_id")) {
                    filter { eq("listing_id", id) }
                    order("image_id", order = Order.ASCENDING)
                }
                .decodeList<SupabaseListingImage>()
            val imagePaths = images.map { it.image_path }

            // Seller
            val sellerRows = supabase.from("accounts")
                .select(columns = Columns.list("account_id", "username", "first_name", "last_name")) {
                    filter { eq("account_id", row.seller_id) }
                    limit(1)
                }
                .decodeList<SupabaseAccount>()
            val acc = sellerRows.firstOrNull()

            // Highest bid
            val bidRows = supabase.from("bids")
                .select(columns = Columns.list("listing_id", "bid_amount")) {
                    filter { eq("listing_id", id) }
                    order("bid_amount", order = Order.DESCENDING)
                    limit(1)
                }
                .decodeList<SupabaseBid>()

            // Check if current user has favorited this listing
            val userId = com.example.mineteh.utils.TokenManager(context).getUserId()
            val isFavorited = if (userId != -1) {
                val favRows = supabase.from("favorites")
                    .select(columns = Columns.list("listing_id")) {
                        filter {
                            eq("listing_id", id)
                            eq("user_id", userId)
                        }
                        limit(1)
                    }
                    .decodeList<SupabaseFavoriteRow>()
                favRows.isNotEmpty()
            } else false

            val listing = Listing(
                id = row.id,
                title = row.title,
                description = row.description,
                price = row.price,
                location = row.location,
                category = row.category,
                listingType = row.listing_type,
                status = row.status,
                _image = imagePaths.firstOrNull(),
                _images = imagePaths,
                seller = if (acc != null) Seller(
                    accountId = acc.account_id,
                    username = acc.username,
                    firstName = acc.first_name,
                    lastName = acc.last_name
                ) else null,
                createdAt = row.created_at,
                highestBidAmount = bidRows.firstOrNull()?.bid_amount,
                endTime = row.end_time,
                isFavorited = isFavorited
            )

            Resource.Success(listing)
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
            Log.d(tag, "Creating listing in Supabase: $title with ${imageUris.size} images")

            val tokenManager = com.example.mineteh.utils.TokenManager(context)
            val sellerId = tokenManager.getUserId()
            if (sellerId == -1) return@withContext Resource.Error("Not authenticated")

            // Build insert map
            val insertData = InsertListing(
                title = title,
                description = description,
                price = price,
                location = location,
                category = category,
                listing_type = listingType,
                seller_id = sellerId,
                status = "active",
                end_time = endTime,
                min_bid_increment = minBidIncrement
            )

            // Insert listing
            val listingRow = supabase.from("listings")
                .insert(insertData) {
                    select()
                }
                .decodeSingle<SupabaseListing>()

            Log.d(tag, "Listing inserted with id=${listingRow.id}")

            // Upload images to Supabase Storage, store public URLs in listing_images
            imageUris.forEachIndexed { index, uri ->
                try {
                    val stream = context.contentResolver.openInputStream(uri) ?: return@forEachIndexed
                    val originalBitmap = android.graphics.BitmapFactory.decodeStream(stream)
                    stream.close()
                    if (originalBitmap == null) return@forEachIndexed

                    // Scale down to max 800px
                    val maxSize = 800
                    val scale = minOf(maxSize.toFloat() / originalBitmap.width, maxSize.toFloat() / originalBitmap.height, 1f)
                    val scaledBitmap = if (scale < 1f) {
                        android.graphics.Bitmap.createScaledBitmap(
                            originalBitmap,
                            (originalBitmap.width * scale).toInt(),
                            (originalBitmap.height * scale).toInt(),
                            true
                        )
                    } else originalBitmap

                    val outputStream = java.io.ByteArrayOutputStream()
                    scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
                    val bytes = outputStream.toByteArray()
                    if (scaledBitmap != originalBitmap) scaledBitmap.recycle()
                    originalBitmap.recycle()

                    // Upload to Supabase Storage bucket "images"
                    val fileName = "listing_${listingRow.id}_${index}_${System.currentTimeMillis()}.jpg"
                    supabase.storage.from("images").upload(
                        path = fileName,
                        data = bytes,
                        upsert = true
                    )
                    val publicUrl = supabase.storage.from("images").publicUrl(fileName)

                    Log.d(tag, "Image $index uploaded to storage: $publicUrl")

                    supabase.from("listing_images").insert(InsertListingImage(
                        listing_id = listingRow.id,
                        image_path = publicUrl
                    ))
                } catch (e: Exception) {
                    Log.e(tag, "Failed to upload image $index", e)
                }
            }

            val listing = Listing(
                id = listingRow.id,
                title = listingRow.title,
                description = listingRow.description,
                price = listingRow.price,
                location = listingRow.location,
                category = listingRow.category,
                listingType = listingRow.listing_type,
                status = listingRow.status,
                _image = null,
                _images = null,
                seller = null,
                createdAt = listingRow.created_at,
                endTime = listingRow.end_time
            )

            Resource.Success(listing)
        } catch (e: Exception) {
            Log.e(tag, "Error creating listing", e)
            Resource.Error(e.message ?: "Failed to create listing")
        }
    }

    suspend fun toggleFavorite(listingId: Int): Resource<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Toggling favorite for listing $listingId")
            val tokenManager = com.example.mineteh.utils.TokenManager(context)
            val userId = tokenManager.getUserId()
            if (userId == -1) return@withContext Resource.Error("Not authenticated")

            // Check if already favorited
            val existing = supabase.from("favorites")
                .select(columns = Columns.list("listing_id")) {
                    filter {
                        eq("listing_id", listingId)
                        eq("user_id", userId)
                    }
                    limit(1)
                }
                .decodeList<SupabaseFavorite>()

            return@withContext if (existing.isNotEmpty()) {
                // Remove favorite
                supabase.from("favorites").delete {
                    filter {
                        eq("listing_id", listingId)
                        eq("user_id", userId)
                    }
                }
                Log.d(tag, "Removed favorite for listing $listingId")
                Resource.Success(false)
            } else {
                // Add favorite
                supabase.from("favorites").insert(InsertFavorite(
                    listing_id = listingId,
                    user_id = userId
                ))
                Log.d(tag, "Added favorite for listing $listingId")
                Resource.Success(true)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error toggling favorite", e)
            Resource.Error(e.message ?: "Failed to toggle favorite")
        }
    }

    suspend fun getFavorites(): Resource<List<Listing>> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Fetching favorites from Supabase")
            val tokenManager = com.example.mineteh.utils.TokenManager(context)
            val userId = tokenManager.getUserId()
            if (userId == -1) return@withContext Resource.Error("Not authenticated")

            // Get favorited listing IDs
            val favRows = supabase.from("favorites")
                .select(columns = Columns.list("listing_id")) {
                    filter { eq("user_id", userId) }
                    order("created_at", order = Order.DESCENDING)
                }
                .decodeList<SupabaseFavoriteRow>()

            if (favRows.isEmpty()) return@withContext Resource.Success(emptyList())

            val ids = favRows.map { it.listing_id }

            // Fetch those listings — only active ones
            val rows = supabase.from("listings")
                .select(columns = Columns.list(
                    "id", "title", "description", "price", "location",
                    "category", "listing_type", "status", "seller_id", "end_time", "created_at"
                )) {
                    filter {
                        isIn("id", ids)
                        eq("status", "active")
                    }
                    order("created_at", order = Order.DESCENDING)
                }
                .decodeList<SupabaseListing>()

            if (rows.isEmpty()) return@withContext Resource.Success(emptyList())

            val sellerIds = rows.map { it.seller_id }.distinct()
            val imageMap = mutableMapOf<Int, String?>()
            val images = supabase.from("listing_images")
                .select(columns = Columns.list("listing_id", "image_path", "image_id")) {
                    filter { isIn("listing_id", ids) }
                    order("image_id", order = Order.ASCENDING)
                }
                .decodeList<SupabaseListingImage>()
            images.forEach { img -> if (!imageMap.containsKey(img.listing_id)) imageMap[img.listing_id] = img.image_path }

            val sellerMap = mutableMapOf<Int, SupabaseAccount>()
            if (sellerIds.isNotEmpty()) {
                val sellers = supabase.from("accounts")
                    .select(columns = Columns.list("account_id", "username", "first_name", "last_name")) {
                        filter { isIn("account_id", sellerIds) }
                    }
                    .decodeList<SupabaseAccount>()
                sellers.forEach { sellerMap[it.account_id] = it }
            }

            val listings = rows.map { row ->
                val acc = sellerMap[row.seller_id]
                Listing(
                    id = row.id,
                    title = row.title,
                    description = row.description,
                    price = row.price,
                    location = row.location,
                    category = row.category,
                    listingType = row.listing_type,
                    status = row.status,
                    _image = imageMap[row.id],
                    _images = null,
                    seller = if (acc != null) Seller(
                        accountId = acc.account_id,
                        username = acc.username,
                        firstName = acc.first_name,
                        lastName = acc.last_name
                    ) else null,
                    createdAt = row.created_at,
                    endTime = row.end_time,
                    isFavorited = true
                )
            }

            Log.d(tag, "Fetched ${listings.size} favorites")
            Resource.Success(listings)
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
    
    suspend fun updateListingStatus(listingId: Int, status: String): Resource<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Updating listing status: listingId=$listingId, status=$status")
            
            val tokenManager = com.example.mineteh.utils.TokenManager(context)
            val userId = tokenManager.getUserId()
            if (userId == -1) return@withContext Resource.Error("Not authenticated")
            
            // Update status in Supabase
            supabase.from("listings")
                .update(UpdateListingStatus(status = status)) {
                    filter {
                        eq("id", listingId)
                        eq("seller_id", userId)
                    }
                }
            
            Log.d(tag, "Listing status updated successfully")
            Resource.Success(true)
        } catch (e: Exception) {
            Log.e(tag, "Error updating listing status", e)
            Resource.Error(e.message ?: "Failed to update listing status")
        }
    }
    
    suspend fun getUserListings(): Resource<List<Listing>> = withContext(Dispatchers.IO) {
        try {
            val tokenManager = com.example.mineteh.utils.TokenManager(context)
            val userId = tokenManager.getUserId()
            if (userId == -1) return@withContext Resource.Error("Not authenticated")
            
            Log.d(tag, "Fetching user listings for userId=$userId")
            
            val rows = supabase.from("listings")
                .select(columns = Columns.list(
                    "id", "title", "description", "price", "location",
                    "category", "listing_type", "status", "seller_id", "end_time", "created_at"
                )) {
                    filter {
                        eq("seller_id", userId)
                    }
                    order("created_at", order = Order.DESCENDING)
                }
                .decodeList<SupabaseListing>()
            
            if (rows.isEmpty()) return@withContext Resource.Success(emptyList())
            
            val ids = rows.map { it.id }
            
            // Fetch images
            val imageMap = mutableMapOf<Int, String?>()
            if (ids.isNotEmpty()) {
                val images = supabase.from("listing_images")
                    .select(columns = Columns.list("listing_id", "image_path", "image_id")) {
                        filter { isIn("listing_id", ids) }
                        order("image_id", order = Order.ASCENDING)
                    }
                    .decodeList<SupabaseListingImage>()
                images.forEach { img ->
                    if (!imageMap.containsKey(img.listing_id)) {
                        imageMap[img.listing_id] = img.image_path
                    }
                }
            }
            
            // Fetch seller info (current user)
            val sellerRows = supabase.from("accounts")
                .select(columns = Columns.list("account_id", "username", "first_name", "last_name")) {
                    filter { eq("account_id", userId) }
                    limit(1)
                }
                .decodeList<SupabaseAccount>()
            val seller = sellerRows.firstOrNull()
            
            val listings = rows.map { row ->
                Listing(
                    id = row.id,
                    title = row.title,
                    description = row.description,
                    price = row.price,
                    location = row.location,
                    category = row.category,
                    listingType = row.listing_type,
                    status = row.status,
                    _image = imageMap[row.id],
                    _images = null,
                    seller = if (seller != null) Seller(
                        accountId = seller.account_id,
                        username = seller.username,
                        firstName = seller.first_name,
                        lastName = seller.last_name
                    ) else null,
                    createdAt = row.created_at,
                    highestBidAmount = null,
                    endTime = row.end_time
                )
            }
            
            Log.d(tag, "Fetched ${listings.size} user listings")
            Resource.Success(listings)
        } catch (e: Exception) {
            Log.e(tag, "Error fetching user listings", e)
            Resource.Error(e.message ?: "Failed to load your listings")
        }
    }
    
    suspend fun updateListing(
        listingId: Int,
        title: String,
        description: String,
        price: Double,
        location: String,
        category: String,
        listingType: String,
        endTime: String?,
        newImageUris: List<android.net.Uri>,
        removedImagePaths: List<String>
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Updating listing id=$listingId")

            val tokenManager = com.example.mineteh.utils.TokenManager(context)
            val currentUserId = tokenManager.getUserId()
            if (currentUserId == -1) return@withContext Resource.Error("Not authenticated")

            // Update core listing fields
            supabase.from("listings")
                .update(UpdateListing(
                    title = title,
                    description = description,
                    price = price,
                    location = location,
                    category = category,
                    listing_type = listingType,
                    end_time = endTime
                )) {
                    filter {
                        eq("id", listingId)
                        eq("seller_id", currentUserId)
                    }
                }

            Log.d(tag, "Listing fields updated for id=$listingId")

            // Delete removed images from listing_images
            for (imagePath in removedImagePaths) {
                supabase.from("listing_images").delete {
                    filter {
                        eq("listing_id", listingId)
                        eq("image_path", imagePath)
                    }
                }
            }

            // Upload new images and insert into listing_images
            newImageUris.forEachIndexed { index, uri ->
                try {
                    val stream = context.contentResolver.openInputStream(uri) ?: return@forEachIndexed
                    val originalBitmap = android.graphics.BitmapFactory.decodeStream(stream)
                    stream.close()
                    if (originalBitmap == null) return@forEachIndexed

                    val maxSize = 800
                    val scale = minOf(maxSize.toFloat() / originalBitmap.width, maxSize.toFloat() / originalBitmap.height, 1f)
                    val scaledBitmap = if (scale < 1f) {
                        android.graphics.Bitmap.createScaledBitmap(
                            originalBitmap,
                            (originalBitmap.width * scale).toInt(),
                            (originalBitmap.height * scale).toInt(),
                            true
                        )
                    } else originalBitmap

                    val outputStream = java.io.ByteArrayOutputStream()
                    scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
                    val bytes = outputStream.toByteArray()
                    if (scaledBitmap != originalBitmap) scaledBitmap.recycle()
                    originalBitmap.recycle()

                    val fileName = "listing_${listingId}_edit_${index}_${System.currentTimeMillis()}.jpg"
                    supabase.storage.from("images").upload(
                        path = fileName,
                        data = bytes,
                        upsert = true
                    )
                    val publicUrl = supabase.storage.from("images").publicUrl(fileName)

                    Log.d(tag, "New image $index uploaded for listing $listingId: $publicUrl")

                    supabase.from("listing_images").insert(InsertListingImage(
                        listing_id = listingId,
                        image_path = publicUrl
                    ))
                } catch (e: Exception) {
                    Log.e(tag, "Failed to upload new image $index for listing $listingId", e)
                }
            }

            Log.d(tag, "Listing $listingId updated successfully")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error updating listing $listingId", e)
            Resource.Error(e.message ?: "Failed to update listing")
        }
    }

    suspend fun deleteListing(listingId: Int): Resource<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Deleting listing: listingId=$listingId")
            
            val tokenManager = com.example.mineteh.utils.TokenManager(context)
            val userId = tokenManager.getUserId()
            if (userId == -1) return@withContext Resource.Error("Not authenticated")
            
            // Delete related data first
            supabase.from("listing_images").delete {
                filter { eq("listing_id", listingId) }
            }
            
            supabase.from("bids").delete {
                filter { eq("listing_id", listingId) }
            }
            
            supabase.from("favorites").delete {
                filter { eq("listing_id", listingId) }
            }
            
            // Delete the listing itself
            supabase.from("listings").delete {
                filter {
                    eq("id", listingId)
                    eq("seller_id", userId) // Ensure user owns the listing
                }
            }
            
            Log.d(tag, "Listing deleted successfully")
            Resource.Success(true)
        } catch (e: Exception) {
            Log.e(tag, "Error deleting listing", e)
            Resource.Error(e.message ?: "Failed to delete listing")
        }
    }
}
