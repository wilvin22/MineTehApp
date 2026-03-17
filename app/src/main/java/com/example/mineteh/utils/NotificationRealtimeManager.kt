package com.example.mineteh.utils

import android.content.Context
import android.util.Log
import com.example.mineteh.model.Notification
import com.example.mineteh.model.SupabaseNotificationResponse
import com.example.mineteh.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    private val tokenManager = TokenManager(context)
    
    // Coroutine scope for managing subscriptions
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Flow for new notifications
    private val _newNotifications = MutableSharedFlow<Notification>()
    val newNotifications: SharedFlow<Notification> = _newNotifications.asSharedFlow()
    
    // Flow for notification updates (read status changes)
    private val _notificationUpdates = MutableSharedFlow<Notification>()
    val notificationUpdates: SharedFlow<Notification> = _notificationUpdates.asSharedFlow()
    
    // Flow for unread count changes
    private val _unreadCountUpdates = MutableSharedFlow<Int>()
    val unreadCountUpdates: SharedFlow<Int> = _unreadCountUpdates.asSharedFlow()
    
    // Connection state
    private var isConnected = false
    private var currentUserId: Int? = null
    
    /**
     * Start real-time subscription for the given user
     */
    fun startSubscription(userId: Int) {
        if (currentUserId == userId && isConnected) {
            Log.d(TAG, "Already subscribed for user $userId")
            return
        }
        
        // Stop existing subscription if any
        stopSubscription()
        
        currentUserId = userId
        
        scope.launch {
            try {
                // Realtime subscription is temporarily disabled until the Realtime API is
                // finalized for the currently pinned Supabase SDK version.
                Log.d(TAG, "Realtime disabled; emitting unread count snapshot for user: $userId")
                isConnected = false
                updateUnreadCount()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting real-time subscription", e)
                isConnected = false
            }
        }
    }
    
    /**
     * Stop the current real-time subscription
     */
    fun stopSubscription() {
        scope.launch {
            try {
                Log.d(TAG, "Stopping real-time subscription")

                isConnected = false
                currentUserId = null
                
                Log.d(TAG, "Real-time subscription stopped")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping real-time subscription", e)
            }
        }
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