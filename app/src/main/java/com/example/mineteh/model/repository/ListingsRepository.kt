package com.example.mineteh.model.repository

import android.content.Context
import android.util.Log
import com.example.mineteh.models.Listing
import com.example.mineteh.models.Seller
import com.example.mineteh.supabase.SupabaseClient
import com.example.mineteh.utils.Resource
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class ListingsRepository(private val context: Context) {
    private val tag = "ListingsRepository"
    private val supabase = SupabaseClient.client

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
            
            // Build query
            var query = supabase.from("listings")
                .select(Columns.raw("""
                    *,
                    accounts!seller_id (
                        account_id,
                        username,
                        first_name,
                        last_name
                    ),
                    listing_images (
                        image_path
                    )
                """))
            
            // Apply filters
            if (category != null) {
                query = query.eq("category", category)
            }
            if (type != null) {
                query = query.eq("listing_type", type)
            }
            if (search != null) {
                query = query.ilike("title", "%$search%")
            }
            
            // Execute query with limit and offset
            val response = query
                .limit(limit.toLong())
                .range(offset.toLong(), (offset + limit - 1).toLong())
                .decodeList<SupabaseListingResponse>()
            
            // Convert to app models
            val listings = response.map { it.toListing() }
            
            Log.d(tag, "Successfully fetched ${listings.size} listings from Supabase")
            Resource.Success(listings)
            
        } catch (e: Exception) {
            Log.e(tag, "Error fetching listings from Supabase", e)
            Resource.Error(e.message ?: "Failed to load listings")
        }
    }

    /**
     * Get a single listing by ID from Supabase
     */
    suspend fun getListing(id: Int): Resource<Listing> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Fetching listing with id=$id from Supabase")
            
            val response = supabase.from("listings")
                .select(Columns.raw("""
                    *,
                    accounts!seller_id (
                        account_id,
                        username,
                        first_name,
                        last_name
                    ),
                    listing_images (
                        image_path
                    ),
                    bids (
                        bid_amount,
                        bid_time,
                        accounts!user_id (
                            username
                        )
                    )
                """))
                .eq("id", id)
                .decodeSingle<SupabaseListingResponse>()
            
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
            val existing = supabase.from("favorites")
                .select()
                .eq("user_id", userId)
                .eq("listing_id", listingId)
                .decodeList<SupabaseFavorite>()
            
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
                supabase.from("favorites")
                    .delete {
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
            
            val response = supabase.from("favorites")
                .select(Columns.raw("""
                    listings!listing_id (
                        *,
                        accounts!seller_id (
                            account_id,
                            username,
                            first_name,
                            last_name
                        ),
                        listing_images (
                            image_path
                        )
                    )
                """))
                .eq("user_id", userId)
                .decodeList<SupabaseFavoriteWithListing>()
            
            val listings = response.mapNotNull { it.listings?.toListing() }
            
            Log.d(tag, "Successfully fetched ${listings.size} favorites")
            Resource.Success(listings)
            
        } catch (e: Exception) {
            Log.e(tag, "Error fetching favorites", e)
            Resource.Error(e.message ?: "Failed to load favorites")
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
    val listing_images: List<SupabaseListingImage>? = null,
    val bids: List<SupabaseBid>? = null
) {
    fun toListing(): Listing {
        val imagesList = listing_images?.map { it.image_path } ?: emptyList()
        val firstImage = imagesList.firstOrNull()
        
        // Calculate highest bid
        val highestBidAmount = bids?.maxOfOrNull { it.bid_amount }
        
        return Listing(
            id = id,
            title = title,
            description = description ?: "",
            price = price,
            location = location ?: "",
            category = category ?: "",
            listingType = listing_type,
            status = status,
            _image = firstImage,
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
