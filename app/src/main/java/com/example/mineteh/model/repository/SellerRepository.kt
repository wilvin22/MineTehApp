package com.example.mineteh.model.repository

import android.content.Context
import android.util.Log
import com.example.mineteh.models.SellerProfileData
import com.example.mineteh.supabase.SupabaseClient
import com.example.mineteh.utils.Resource
import com.example.mineteh.utils.TokenManager
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class SellerStats(
    val activeListings: Int,
    val totalSold: Int,
    val unreadMessages: Int,
    val averageRating: Double
)

@Serializable
private data class AccountRow(
    @SerialName("account_id") val accountId: Int,
    val username: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
private data class RatingRow(
    val rating: Int
)

@Serializable
private data class MessageRow(
    @SerialName("is_read") val isRead: Boolean
)

class SellerRepository(private val context: Context) {
    private val tag = "SellerRepository"
    private val supabase = SupabaseClient.client

    suspend fun getSellerProfile(sellerId: Int): Resource<SellerProfileData> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Fetching seller profile for sellerId=$sellerId")

            // 1. Query accounts
            val accountRows = supabase.from("accounts")
                .select(columns = Columns.list("account_id", "username", "first_name", "last_name", "avatar_url")) {
                    filter { eq("account_id", sellerId) }
                    limit(1)
                }
                .decodeList<AccountRow>()

            val account = accountRows.firstOrNull()
                ?: return@withContext Resource.Error("Seller not found")

            // 2. Count active listings
            val activeListings = supabase.from("listings")
                .select(columns = Columns.list("id")) {
                    filter {
                        eq("seller_id", sellerId)
                        eq("status", "active")
                    }
                }
                .decodeList<kotlinx.serialization.json.JsonObject>()
                .size

            // 3. Count completed orders
            val soldCount = supabase.from("orders")
                .select(columns = Columns.list("order_id")) {
                    filter {
                        eq("seller_id", sellerId)
                        eq("status", "completed")
                    }
                }
                .decodeList<kotlinx.serialization.json.JsonObject>()
                .size

            // 4. Compute average rating
            val ratings = supabase.from("reviews")
                .select(columns = Columns.list("rating")) {
                    filter { eq("seller_id", sellerId) }
                }
                .decodeList<RatingRow>()

            val averageRating = if (ratings.isEmpty()) 0.0
            else ratings.map { it.rating }.average()

            Resource.Success(
                SellerProfileData(
                    accountId = account.accountId,
                    username = account.username,
                    firstName = account.firstName,
                    lastName = account.lastName,
                    avatarUrl = account.avatarUrl,
                    averageRating = averageRating,
                    activeListingCount = activeListings,
                    soldCount = soldCount
                )
            )
        } catch (e: Exception) {
            Log.e(tag, "Error fetching seller profile", e)
            Resource.Error(e.message ?: "Failed to load seller profile")
        }
    }

    suspend fun getMyStats(userId: Int): Resource<SellerStats> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Fetching seller stats for userId=$userId")

            // Active listings count
            val activeListings = supabase.from("listings")
                .select(columns = Columns.list("id")) {
                    filter {
                        eq("seller_id", userId)
                        eq("status", "active")
                    }
                }
                .decodeList<kotlinx.serialization.json.JsonObject>()
                .size

            // Total sold count
            val totalSold = supabase.from("orders")
                .select(columns = Columns.list("order_id")) {
                    filter {
                        eq("seller_id", userId)
                        eq("status", "completed")
                    }
                }
                .decodeList<kotlinx.serialization.json.JsonObject>()
                .size

            // Unread messages count
            val unreadMessages = try {
                supabase.from("messages")
                    .select(columns = Columns.list("is_read")) {
                        filter {
                            eq("receiver_id", userId)
                            eq("is_read", false)
                        }
                    }
                    .decodeList<MessageRow>()
                    .size
            } catch (e: Exception) {
                Log.w(tag, "Could not fetch unread messages count", e)
                0
            }

            // Average rating
            val ratings = supabase.from("reviews")
                .select(columns = Columns.list("rating")) {
                    filter { eq("seller_id", userId) }
                }
                .decodeList<RatingRow>()

            val averageRating = if (ratings.isEmpty()) 0.0
            else ratings.map { it.rating }.average()

            Resource.Success(
                SellerStats(
                    activeListings = activeListings,
                    totalSold = totalSold,
                    unreadMessages = unreadMessages,
                    averageRating = averageRating
                )
            )
        } catch (e: Exception) {
            Log.e(tag, "Error fetching seller stats", e)
            Resource.Error(e.message ?: "Failed to load seller stats")
        }
    }
}
