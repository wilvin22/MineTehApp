package com.example.mineteh.model.repository

import android.content.Context
import android.util.Log
import com.example.mineteh.models.BidData
import com.example.mineteh.models.BidRequest
import com.example.mineteh.models.CreateListingRequest
import com.example.mineteh.models.FavoriteRequest
import com.example.mineteh.models.Listing
import com.example.mineteh.models.Seller
import com.example.mineteh.models.Bid
import com.example.mineteh.network.ApiClient
import com.example.mineteh.supabase.SupabaseClient
import com.example.mineteh.utils.Resource
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

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

class ListingsRepository(private val context: Context) {
    private val tag = "ListingsRepository"
    private val api = ApiClient.apiService
    private val supabase = SupabaseClient.client

    suspend fun getListings(
        category: String? = null,
        type: String? = null,
        search: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Resource<List<Listing>> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Fetching listings from Supabase: category=$category, type=$type, search=$search")

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
                    endTime = row.end_time
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
                endTime = row.end_time
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
