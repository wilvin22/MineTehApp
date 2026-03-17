package com.example.mineteh.utils

import android.content.Context
import android.util.Log
import com.example.mineteh.model.Notification
import com.example.mineteh.model.SupabaseNotificationResponse
import com.example.mineteh.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.createChannel
import io.github.jan.supabase.realtime.postgresChanges
import io.github.jan.supabase.realtime.realtime
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
    
    // Realtime channel for notifications
    private var notificationChannel: RealtimeChannel? = null
    
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
                Log.d(TAG, "Starting real-time subscription for user: $userId")
                
                // Create channel for notifications
                notificationChannel = supabase.realtime.createChannel("notifications_$userId") {
                    // Listen for new notifications
                    postgresChanges {
                        event = PostgresAction.INSERT
                        schema = "public"
                        table = "notifications"
                        filter = "user_id=eq.$userId"
                    }
                    
                    // Listen for notification updates (read status changes)
                    postgresChanges {
                        event = PostgresAction.UPDATE
                        schema = "public"
                        table = "notifications"
                        filter = "user_id=eq.$userId"
                    }
                }
                
                // Subscribe to the channel
                notificationChannel?.subscribe { status ->
                    Log.d(TAG, "Real-time subscription status: $status")
                    isConnected = status == io.github.jan.supabase.realtime.RealtimeChannel.Status.SUBSCRIBED
                    
                    if (isConnected) {
                        Log.d(TAG, "Successfully connected to real-time notifications for user $userId")
                    }
                }
                
                // Handle incoming changes
                notificationChannel?.postgresChanges?.collect { change ->
                    handleNotificationChange(change)
                }
                
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
                
                notificationChannel?.unsubscribe()
                notificationChannel = null
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
     * Handle notification changes from real-time subscription
     */
    private suspend fun handleNotificationChange(change: io.github.jan.supabase.realtime.PostgresChangePayload) {
        try {
            Log.d(TAG, "Received notification change: ${change.eventType}")
            
            when (change.eventType) {
                PostgresAction.INSERT -> {
                    // New notification received
                    val notificationData = change.decodeRecord<SupabaseNotificationResponse>()
                    val notification = notificationData.toNotification()
                    
                    Log.d(TAG, "New notification received: ${notification.title}")
                    _newNotifications.emit(notification)
                    
                    // Update unread count
                    updateUnreadCount()
                }
                
                PostgresAction.UPDATE -> {
                    // Notification updated (likely read status change)
                    val notificationData = change.decodeRecord<SupabaseNotificationResponse>()
                    val notification = notificationData.toNotification()
                    
                    Log.d(TAG, "Notification updated: ${notification.id}, isRead: ${notification.isRead}")
                    _notificationUpdates.emit(notification)
                    
                    // Update unread count
                    updateUnreadCount()
                }
                
                else -> {
                    Log.d(TAG, "Unhandled change type: ${change.eventType}")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling notification change", e)
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