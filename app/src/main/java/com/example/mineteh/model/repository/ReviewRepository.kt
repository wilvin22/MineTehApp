package com.example.mineteh.model.repository

import android.content.Context
import android.util.Log
import com.example.mineteh.supabase.SupabaseClient
import com.example.mineteh.utils.Resource
import com.example.mineteh.utils.TokenManager
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class InsertReview(
    @SerialName("reviewer_id") val reviewerId: Int,
    @SerialName("seller_id") val sellerId: Int,
    @SerialName("listing_id") val listingId: Int,
    val rating: Int,
    val comment: String?
)

class ReviewRepository(private val context: Context) {
    private val tag = "ReviewRepository"
    private val tokenManager = TokenManager(context)
    private val supabase = SupabaseClient.client

    suspend fun submitReview(
        sellerId: Int,
        listingId: Int,
        rating: Int,
        comment: String?
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        if (rating < 1 || rating > 5) {
            return@withContext Resource.Error("Rating must be between 1 and 5")
        }

        val reviewerId = tokenManager.getUserId()
        if (reviewerId == -1) {
            return@withContext Resource.Error("User not logged in")
        }

        try {
            supabase.from("reviews").insert(
                InsertReview(
                    reviewerId = reviewerId,
                    sellerId = sellerId,
                    listingId = listingId,
                    rating = rating,
                    comment = comment?.takeIf { it.isNotBlank() }
                )
            )
            Log.d(tag, "Review submitted for listingId=$listingId")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error submitting review", e)
            Resource.Error(e.message ?: "Failed to submit review")
        }
    }

    suspend fun hasReviewed(listingId: Int): Resource<Boolean> = withContext(Dispatchers.IO) {
        val reviewerId = tokenManager.getUserId()
        if (reviewerId == -1) {
            return@withContext Resource.Error("User not logged in")
        }

        try {
            val rows = supabase.from("reviews")
                .select(columns = Columns.list("listing_id")) {
                    filter {
                        eq("reviewer_id", reviewerId)
                        eq("listing_id", listingId)
                    }
                    limit(1)
                }
                .decodeList<kotlinx.serialization.json.JsonObject>()

            Resource.Success(rows.isNotEmpty())
        } catch (e: Exception) {
            Log.e(tag, "Error checking review status", e)
            Resource.Error(e.message ?: "Failed to check review status")
        }
    }
}
