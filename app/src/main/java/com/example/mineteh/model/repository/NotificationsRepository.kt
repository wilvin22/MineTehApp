package com.example.mineteh.model.repository

import android.content.Context
import android.util.Log
import com.example.mineteh.utils.Resource
import com.example.mineteh.utils.TokenManager
import com.example.mineteh.model.Notification
import com.example.mineteh.model.NotificationPreferences
import com.example.mineteh.model.SupabaseNotificationResponse
import com.example.mineteh.model.SupabaseNotificationPreferencesResponse
import com.example.mineteh.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class NotificationsRepository(private val context: Context) {
    private val supabase = SupabaseClient.client
    private val tokenManager = TokenManager(context)
    
    companion object {
        private const val TAG = "NotificationsRepository"
    }

    suspend fun getNotifications(userId: Int, limit: Int = 50, offset: Int = 0): Resource<List<Notification>> {
        return try {
            Log.d(TAG, "Fetching notifications for user: $userId, limit: $limit, offset: $offset")
            
            val response = supabase.from("notifications")
                .select(columns = Columns.list(
                    "id", "user_id", "type", "title", "message", "data", "is_read", "created_at", "updated_at"
                )) {
                    filter {
                        eq("user_id", userId)
                    }
                    order("created_at", order = Order.DESCENDING)
                    limit(limit.toLong())
                    range(offset.toLong(), (offset + limit - 1).toLong())
                }
                .decodeList<SupabaseNotificationResponse>()

            val notifications = response.map { it.toNotification() }
            Log.d(TAG, "Successfully fetched ${notifications.size} notifications")
            Resource.Success(notifications)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching notifications", e)
            Resource.Error("Failed to load notifications: ${e.message}")
        }
    }

    suspend fun markAsRead(notificationId: Int): Resource<Boolean> {
        return try {
            Log.d(TAG, "Marking notification as read: $notificationId")
            
            supabase.from("notifications")
                .update({
                    set("is_read", true)
                }) {
                    filter {
                        eq("id", notificationId)
                    }
                }

            Log.d(TAG, "Successfully marked notification $notificationId as read")
            Resource.Success(true)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error marking notification as read", e)
            Resource.Error("Failed to mark notification as read: ${e.message}")
        }
    }

    suspend fun markAllAsRead(userId: Int): Resource<Boolean> {
        return try {
            Log.d(TAG, "Marking all notifications as read for user: $userId")
            
            supabase.from("notifications")
                .update({
                    set("is_read", true)
                }) {
                    filter {
                        eq("user_id", userId)
                        eq("is_read", false)
                    }
                }

            Log.d(TAG, "Successfully marked all notifications as read for user $userId")
            Resource.Success(true)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error marking all notifications as read", e)
            Resource.Error("Failed to mark all notifications as read: ${e.message}")
        }
    }

    suspend fun getUnreadCount(userId: Int): Resource<Int> {
        return try {
            Log.d(TAG, "Getting unread count for user: $userId")
            
            val response = supabase.from("notifications")
                .select(columns = Columns.list("id")) {
                    filter {
                        eq("user_id", userId)
                        eq("is_read", false)
                    }
                }
                .decodeList<Map<String, Any>>()

            val count = response.size
            Log.d(TAG, "Unread count for user $userId: $count")
            Resource.Success(count)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting unread count", e)
            Resource.Error("Failed to get unread count: ${e.message}")
        }
    }

    suspend fun getNotificationsByType(userId: Int, type: String, limit: Int = 50): Resource<List<Notification>> {
        return try {
            Log.d(TAG, "Fetching notifications by type for user: $userId, type: $type")
            
            val response = supabase.from("notifications")
                .select(columns = Columns.list(
                    "id", "user_id", "type", "title", "message", "is_read", "created_at", "updated_at"
                )) {
                    filter {
                        eq("user_id", userId)
                        eq("type", type)
                    }
                    order("created_at", order = Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<SupabaseNotificationResponse>()

            val notifications = response.map { it.toNotification() }
            Log.d(TAG, "Successfully fetched ${notifications.size} notifications of type $type")
            Resource.Success(notifications)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching notifications by type", e)
            Resource.Error("Failed to load notifications by type: ${e.message}")
        }
    }

    suspend fun getNotificationsByReadStatus(userId: Int, isRead: Boolean, limit: Int = 50): Resource<List<Notification>> {
        return try {
            Log.d(TAG, "Fetching notifications by read status for user: $userId, isRead: $isRead")
            
            val response = supabase.from("notifications")
                .select(columns = Columns.list(
                    "id", "user_id", "type", "title", "message", "is_read", "created_at", "updated_at"
                )) {
                    filter {
                        eq("user_id", userId)
                        eq("is_read", isRead)
                    }
                    order("created_at", order = Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<SupabaseNotificationResponse>()

            val notifications = response.map { it.toNotification() }
            Log.d(TAG, "Successfully fetched ${notifications.size} notifications with read status $isRead")
            Resource.Success(notifications)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching notifications by read status", e)
            Resource.Error("Failed to load notifications by read status: ${e.message}")
        }
    }

    suspend fun deleteNotification(notificationId: Int): Resource<Boolean> {
        return try {
            Log.d(TAG, "Deleting notification: $notificationId")
            
            supabase.from("notifications")
                .delete {
                    filter {
                        eq("id", notificationId)
                    }
                }

            Log.d(TAG, "Successfully deleted notification $notificationId")
            Resource.Success(true)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting notification", e)
            Resource.Error("Failed to delete notification: ${e.message}")
        }
    }

    suspend fun createNotification(
        userId: Int,
        type: String,
        title: String,
        message: String,
        data: Map<String, String>? = null
    ): Resource<Notification> {
        return try {
            Log.d(TAG, "Creating notification for user: $userId, type: $type")
            
            // Check if this notification type is enabled for the user
            val typeEnabledResult = isNotificationTypeEnabled(userId, type)
            if (typeEnabledResult is Resource.Success && !typeEnabledResult.data!!) {
                Log.d(TAG, "Notification type $type is disabled for user $userId, skipping creation")
                return Resource.Error("Notification type disabled by user preferences")
            }
            
            val notificationData = mutableMapOf<String, Any>(
                "user_id" to userId,
                "type" to type,
                "title" to title,
                "message" to message
            )
            
            if (data != null) {
                // Optional payload: only include if your DB has a `data` json/jsonb column.
                // (Some deployments may not have this column.)
            }
            
            val response = supabase.from("notifications")
                .insert(notificationData)
                .decodeSingle<SupabaseNotificationResponse>()

            val notification = response.toNotification()
            Log.d(TAG, "Successfully created notification with ID: ${notification.id}")
            Resource.Success(notification)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating notification", e)
            Resource.Error("Failed to create notification: ${e.message}")
        }
    }

    suspend fun getFilteredNotifications(
        userId: Int,
        type: String? = null,
        isRead: Boolean? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Resource<List<Notification>> {
        return try {
            Log.d(TAG, "Fetching filtered notifications for user: $userId, type: $type, isRead: $isRead")
            
            val response = supabase.from("notifications")
                .select(columns = Columns.list(
                    "id", "user_id", "type", "title", "message", "is_read", "created_at", "updated_at"
                )) {
                    filter {
                        eq("user_id", userId)
                        if (type != null) {
                            eq("type", type)
                        }
                        if (isRead != null) {
                            eq("is_read", isRead)
                        }
                    }
                    order("created_at", order = Order.DESCENDING)
                    limit(limit.toLong())
                    range(offset.toLong(), (offset + limit - 1).toLong())
                }
                .decodeList<SupabaseNotificationResponse>()

            val notifications = response.map { it.toNotification() }
            Log.d(TAG, "Successfully fetched ${notifications.size} filtered notifications")
            Resource.Success(notifications)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching filtered notifications", e)
            Resource.Error("Failed to load filtered notifications: ${e.message}")
        }
    }

    suspend fun searchNotifications(
        userId: Int,
        searchQuery: String,
        limit: Int = 50,
        offset: Int = 0
    ): Resource<List<Notification>> {
        return try {
            Log.d(TAG, "Searching notifications for user: $userId, query: $searchQuery")
            
            val response = supabase.from("notifications")
                .select(columns = Columns.list(
                    "id", "user_id", "type", "title", "message", "is_read", "created_at", "updated_at"
                )) {
                    filter {
                        eq("user_id", userId)
                        or {
                            ilike("title", "%$searchQuery%")
                            ilike("message", "%$searchQuery%")
                        }
                    }
                    order("created_at", order = Order.DESCENDING)
                    limit(limit.toLong())
                    range(offset.toLong(), (offset + limit - 1).toLong())
                }
                .decodeList<SupabaseNotificationResponse>()

            val notifications = response.map { it.toNotification() }
            Log.d(TAG, "Successfully found ${notifications.size} notifications matching query")
            Resource.Success(notifications)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error searching notifications", e)
            Resource.Error("Failed to search notifications: ${e.message}")
        }
    }

    suspend fun getNotificationsPaginated(
        userId: Int,
        page: Int,
        pageSize: Int = 20
    ): Resource<Pair<List<Notification>, Boolean>> {
        return try {
            val offset = page * pageSize
            Log.d(TAG, "Fetching paginated notifications for user: $userId, page: $page, pageSize: $pageSize")
            
            // Fetch one extra item to check if there are more pages
            val response = supabase.from("notifications")
                .select(columns = Columns.list(
                    "id", "user_id", "type", "title", "message", "is_read", "created_at", "updated_at"
                )) {
                    filter {
                        eq("user_id", userId)
                    }
                    order("created_at", order = Order.DESCENDING)
                    limit((pageSize + 1).toLong())
                    range(offset.toLong(), (offset + pageSize).toLong())
                }
                .decodeList<SupabaseNotificationResponse>()

            val hasMorePages = response.size > pageSize
            val notifications = response.take(pageSize).map { it.toNotification() }
            
            Log.d(TAG, "Successfully fetched ${notifications.size} notifications, hasMorePages: $hasMorePages")
            Resource.Success(Pair(notifications, hasMorePages))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching paginated notifications", e)
            Resource.Error("Failed to load paginated notifications: ${e.message}")
        }
    }

    fun subscribeToNotifications(userId: Int): Flow<List<Notification>> = flow {
        try {
            Log.d(TAG, "Realtime subscription disabled; doing initial load only for user: $userId")
            
            // Initial load
            val initialNotifications = getNotifications(userId)
            if (initialNotifications is Resource.Success) {
                emit(initialNotifications.data ?: emptyList())
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in real-time subscription", e)
            emit(emptyList())
        }
    }

    // Notification Preferences Management
    suspend fun getPreferences(userId: Int): Resource<NotificationPreferences> {
        return try {
            Log.d(TAG, "Fetching notification preferences for user: $userId")
            
            val response = supabase.from("notification_preferences")
                .select(columns = Columns.list(
                    "id", "user_id", "bid_placed_enabled", "bid_outbid_enabled", 
                    "auction_ending_enabled", "auction_won_enabled", "auction_lost_enabled",
                    "item_sold_enabled", "new_message_enabled", "listing_approved_enabled",
                    "payment_received_enabled", "push_notifications_enabled",
                    "quiet_hours_start", "quiet_hours_end", "created_at", "updated_at"
                )) {
                    filter {
                        eq("user_id", userId)
                    }
                    limit(1)
                }
                .decodeList<SupabaseNotificationPreferencesResponse>()

            if (response.isNotEmpty()) {
                val preferences = response.first().toNotificationPreferences()
                Log.d(TAG, "Successfully fetched preferences for user $userId")
                Resource.Success(preferences)
            } else {
                // Create default preferences if none exist
                Log.d(TAG, "No preferences found for user $userId, creating defaults")
                createDefaultPreferences(userId)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching notification preferences", e)
            Resource.Error("Failed to load notification preferences: ${e.message}")
        }
    }

    suspend fun updatePreferences(userId: Int, preferences: NotificationPreferences): Resource<Boolean> {
        return try {
            Log.d(TAG, "Updating notification preferences for user: $userId")
            
            val updateData = mapOf(
                "bid_placed_enabled" to preferences.bidPlacedEnabled,
                "bid_outbid_enabled" to preferences.bidOutbidEnabled,
                "auction_ending_enabled" to preferences.auctionEndingEnabled,
                "auction_won_enabled" to preferences.auctionWonEnabled,
                "auction_lost_enabled" to preferences.auctionLostEnabled,
                "item_sold_enabled" to preferences.itemSoldEnabled,
                "new_message_enabled" to preferences.newMessageEnabled,
                "listing_approved_enabled" to preferences.listingApprovedEnabled,
                "payment_received_enabled" to preferences.paymentReceivedEnabled,
                "push_notifications_enabled" to preferences.pushNotificationsEnabled,
                "quiet_hours_start" to preferences.quietHoursStart,
                "quiet_hours_end" to preferences.quietHoursEnd
            )
            
            supabase.from("notification_preferences")
                .update({
                    updateData.forEach { (key, value) -> set(key, value) }
                }) {
                    filter {
                        eq("user_id", userId)
                    }
                }

            Log.d(TAG, "Successfully updated preferences for user $userId")
            Resource.Success(true)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notification preferences", e)
            Resource.Error("Failed to update notification preferences: ${e.message}")
        }
    }

    suspend fun createDefaultPreferences(userId: Int): Resource<NotificationPreferences> {
        return try {
            Log.d(TAG, "Creating default notification preferences for user: $userId")
            
            val defaultData = mapOf(
                "user_id" to userId,
                "bid_placed_enabled" to true,
                "bid_outbid_enabled" to true,
                "auction_ending_enabled" to true,
                "auction_won_enabled" to true,
                "auction_lost_enabled" to true,
                "item_sold_enabled" to true,
                "new_message_enabled" to true,
                "listing_approved_enabled" to true,
                "payment_received_enabled" to true,
                "push_notifications_enabled" to true
            )
            
            val response = supabase.from("notification_preferences")
                .insert(defaultData)
                .decodeSingle<SupabaseNotificationPreferencesResponse>()

            val preferences = response.toNotificationPreferences()
            Log.d(TAG, "Successfully created default preferences for user $userId")
            Resource.Success(preferences)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating default notification preferences", e)
            Resource.Error("Failed to create default notification preferences: ${e.message}")
        }
    }

    suspend fun isNotificationTypeEnabled(userId: Int, notificationType: String): Resource<Boolean> {
        return try {
            Log.d(TAG, "Checking if notification type $notificationType is enabled for user: $userId")
            
            val preferencesResult = getPreferences(userId)
            if (preferencesResult is Resource.Success) {
                val preferences = preferencesResult.data!!
                val isEnabled = when (notificationType) {
                    "BID_PLACED" -> preferences.bidPlacedEnabled
                    "BID_OUTBID" -> preferences.bidOutbidEnabled
                    "AUCTION_ENDING" -> preferences.auctionEndingEnabled
                    "AUCTION_WON" -> preferences.auctionWonEnabled
                    "AUCTION_LOST" -> preferences.auctionLostEnabled
                    "ITEM_SOLD" -> preferences.itemSoldEnabled
                    "NEW_MESSAGE" -> preferences.newMessageEnabled
                    "LISTING_APPROVED" -> preferences.listingApprovedEnabled
                    "PAYMENT_RECEIVED" -> preferences.paymentReceivedEnabled
                    else -> true // Default to enabled for unknown types
                }
                
                Log.d(TAG, "Notification type $notificationType is ${if (isEnabled) "enabled" else "disabled"} for user $userId")
                Resource.Success(isEnabled)
            } else {
                Log.w(TAG, "Could not fetch preferences for user $userId, defaulting to enabled")
                Resource.Success(true) // Default to enabled if preferences can't be fetched
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking notification type enabled status", e)
            Resource.Success(true) // Default to enabled on error
        }
    }

    suspend fun isPushNotificationEnabled(userId: Int): Resource<Boolean> {
        return try {
            Log.d(TAG, "Checking if push notifications are enabled for user: $userId")
            
            val preferencesResult = getPreferences(userId)
            if (preferencesResult is Resource.Success) {
                val preferences = preferencesResult.data!!
                Log.d(TAG, "Push notifications are ${if (preferences.pushNotificationsEnabled) "enabled" else "disabled"} for user $userId")
                Resource.Success(preferences.pushNotificationsEnabled)
            } else {
                Log.w(TAG, "Could not fetch preferences for user $userId, defaulting to enabled")
                Resource.Success(true) // Default to enabled if preferences can't be fetched
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking push notification enabled status", e)
            Resource.Success(true) // Default to enabled on error
        }
    }

    // Notification Cleanup Methods
    suspend fun cleanupOldNotifications(userId: Int, daysToKeep: Int = 90): Resource<Int> {
        return try {
            Log.d(TAG, "Cleaning up notifications older than $daysToKeep days for user: $userId")
            
            // Calculate the cutoff date
            val cutoffDate = java.time.LocalDateTime.now().minusDays(daysToKeep.toLong())
            val cutoffDateString = cutoffDate.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            
            // Get count of notifications to be deleted
            val countResponse = supabase.from("notifications")
                .select(columns = Columns.list("id")) {
                    filter {
                        eq("user_id", userId)
                        lt("created_at", cutoffDateString)
                    }
                }
                .decodeList<Map<String, Any>>()
            
            val deleteCount = countResponse.size
            
            if (deleteCount > 0) {
                // Delete old notifications
                supabase.from("notifications")
                    .delete {
                        filter {
                            eq("user_id", userId)
                            lt("created_at", cutoffDateString)
                        }
                    }
                
                Log.d(TAG, "Successfully deleted $deleteCount old notifications for user $userId")
            } else {
                Log.d(TAG, "No old notifications to delete for user $userId")
            }
            
            Resource.Success(deleteCount)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old notifications", e)
            Resource.Error("Failed to cleanup old notifications: ${e.message}")
        }
    }

    suspend fun cleanupOldNotificationsKeepRecent(userId: Int, maxNotificationsToKeep: Int = 50): Resource<Int> {
        return try {
            Log.d(TAG, "Cleaning up notifications, keeping $maxNotificationsToKeep most recent for user: $userId")
            
            // Get all notification IDs ordered by creation date (newest first)
            val allNotifications = supabase.from("notifications")
                .select(columns = Columns.list("id")) {
                    filter {
                        eq("user_id", userId)
                    }
                    order("created_at", order = Order.DESCENDING)
                }
                .decodeList<Map<String, Any>>()
            
            val totalCount = allNotifications.size
            
            if (totalCount > maxNotificationsToKeep) {
                // Get IDs of notifications to delete (everything after the first maxNotificationsToKeep)
                val notificationsToDelete = allNotifications.drop(maxNotificationsToKeep)
                val idsToDelete = notificationsToDelete.mapNotNull { it["id"] as? Int }
                
                if (idsToDelete.isNotEmpty()) {
                    // Delete notifications in batches to avoid query size limits
                    val batchSize = 100
                    var deletedCount = 0
                    
                    idsToDelete.chunked(batchSize).forEach { batch ->
                        supabase.from("notifications")
                            .delete {
                                filter {
                                    isIn("id", batch)
                                }
                            }
                        deletedCount += batch.size
                    }
                    
                    Log.d(TAG, "Successfully deleted $deletedCount excess notifications for user $userId")
                    Resource.Success(deletedCount)
                } else {
                    Log.d(TAG, "No excess notifications to delete for user $userId")
                    Resource.Success(0)
                }
            } else {
                Log.d(TAG, "User $userId has $totalCount notifications, no cleanup needed")
                Resource.Success(0)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up excess notifications", e)
            Resource.Error("Failed to cleanup excess notifications: ${e.message}")
        }
    }

    suspend fun performAutomaticCleanup(userId: Int): Resource<Pair<Int, Int>> {
        return try {
            Log.d(TAG, "Performing automatic cleanup for user: $userId")
            
            // First cleanup by age (90 days)
            val ageCleanupResult = cleanupOldNotifications(userId, 90)
            val ageDeletedCount = if (ageCleanupResult is Resource.Success) ageCleanupResult.data!! else 0
            
            // Then cleanup by count (keep 50 most recent)
            val countCleanupResult = cleanupOldNotificationsKeepRecent(userId, 50)
            val countDeletedCount = if (countCleanupResult is Resource.Success) countCleanupResult.data!! else 0
            
            val totalDeleted = ageDeletedCount + countDeletedCount
            Log.d(TAG, "Automatic cleanup completed for user $userId: $totalDeleted notifications deleted")
            
            Resource.Success(Pair(ageDeletedCount, countDeletedCount))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error performing automatic cleanup", e)
            Resource.Error("Failed to perform automatic cleanup: ${e.message}")
        }
    }
}