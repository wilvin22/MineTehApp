package com.example.mineteh.utils

import android.content.Context
import android.util.Log
import com.example.mineteh.model.Notification
import com.example.mineteh.model.SupabaseNotificationResponse
import com.example.mineteh.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class NotificationRealtimeManager(private val context: Context) {
    
    companion object {
        private const val TAG = "NotificationRealtimeManager"
    }
    
    private val supabase = SupabaseClient.client
    
    // Coroutine scope for managing subscriptions
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var subscriptionJob: Job? = null
    private var hasInitialized = false
    private val knownNotificationIds = mutableSetOf<Int>()
    private val pollIntervalMs = 5000L
    private val fetchLimit = 50
    
    // Flow for new notifications
    private val _newNotifications = MutableSharedFlow<Notification>(extraBufferCapacity = 10)
    val newNotifications: SharedFlow<Notification> = _newNotifications.asSharedFlow()
    
    // Flow for notification updates (read status changes)
    private val _notificationUpdates = MutableSharedFlow<Notification>(extraBufferCapacity = 10)
    val notificationUpdates: SharedFlow<Notification> = _notificationUpdates.asSharedFlow()
    
    // Flow for unread count changes
    private val _unreadCountUpdates = MutableSharedFlow<Int>(extraBufferCapacity = 10)
    val unreadCountUpdates: SharedFlow<Int> = _unreadCountUpdates.asSharedFlow()
    
    // Connection state
    private var isConnected = false
    private var currentUserId: Int? = null
    
    /**
     * Start real-time subscription for the given user
     */
    fun startSubscription(userId: Int) {
        if (currentUserId == userId && subscriptionJob?.isActive == true) {
            Log.d(TAG, "Already subscribed for user $userId")
            return
        }

        // Stop existing subscription if any
        stopSubscription()

        currentUserId = userId
        isConnected = true
        hasInitialized = false
        knownNotificationIds.clear()

        subscriptionJob = scope.launch {
            while (isActive && currentUserId == userId) {
                try {
                    pollForNewNotifications(userId)
                    updateUnreadCount()
                } catch (e: Exception) {
                    Log.e(TAG, "Error polling notifications for user $userId", e)
                }
                delay(pollIntervalMs)
            }
        }
    }
    
    /**
     * Stop the current real-time subscription
     */
    fun stopSubscription() {
        Log.d(TAG, "Stopping notification polling")
        subscriptionJob?.cancel()
        subscriptionJob = null
        isConnected = false
        currentUserId = null
        hasInitialized = false
        knownNotificationIds.clear()
    }
    
    /**
     * Restart subscription (useful for reconnection after network issues)
     */
    fun restartSubscription() {
        currentUserId?.let { userId ->
            Log.d(TAG, "Restarting subscription for user $userId")
            stopSubscription()
            startSubscription(userId)
        }
    }
    
    /**
     * Update unread count and emit to subscribers
     */
    private suspend fun updateUnreadCount() {
        try {
            currentUserId?.let { userId ->
                // Get current unread count from database
                val response = supabase.from("notifications")
                    .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("id")) {
                        filter {
                            eq("user_id", userId)
                            eq("is_read", false)
                        }
                    }
                    .decodeList<Map<String, Any>>()
                
                val unreadCount = response.size
                Log.d(TAG, "Updated unread count: $unreadCount")
                _unreadCountUpdates.emit(unreadCount)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating unread count", e)
        }
    }

    private suspend fun pollForNewNotifications(userId: Int) {
        // Poll for the most recent notifications. On the first poll we only seed known IDs
        // (no alerts), then emit only notifications we haven't seen before.
        val latest = supabase.from("notifications")
            .select(
                columns = Columns.list(
                    "id",
                    "user_id",
                    "type",
                    "title",
                    "message",
                    "link",
                    "is_read",
                    "created_at"
                )
            ) {
                filter {
                    eq("user_id", userId)
                }
                order("created_at", order = Order.DESCENDING)
                limit(fetchLimit.toLong())
            }
            .decodeList<SupabaseNotificationResponse>()

        if (!hasInitialized) {
            latest.forEach { knownNotificationIds.add(it.id) }
            hasInitialized = true
            return
        }

        val newOnes = latest.filter { !knownNotificationIds.contains(it.id) }
        if (newOnes.isEmpty()) return

        // Emit newest first; UI will re-sort by `created_at`.
        newOnes.forEach { response ->
            knownNotificationIds.add(response.id)
            _newNotifications.emit(response.toNotification())
        }
    }
    
    /**
     * Check if currently connected to real-time updates
     */
    fun isConnected(): Boolean = isConnected
    
    /**
     * Get current subscribed user ID
     */
    fun getCurrentUserId(): Int? = currentUserId
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up NotificationRealtimeManager")
        stopSubscription()
        scope.cancel()
    }
    
    /**
     * Handle connection errors and attempt reconnection
     */
    fun handleConnectionError() {
        Log.w(TAG, "Handling connection error, attempting reconnection")
        
        scope.launch {
            try {
                // Wait a bit before reconnecting
                kotlinx.coroutines.delay(2000)
                
                if (!isConnected && currentUserId != null) {
                    Log.d(TAG, "Attempting to reconnect...")
                    restartSubscription()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during reconnection attempt", e)
            }
        }
    }
}