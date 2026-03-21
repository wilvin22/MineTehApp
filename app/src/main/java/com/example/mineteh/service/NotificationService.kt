package com.example.mineteh.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.mineteh.R
import com.example.mineteh.utils.Resource
import com.example.mineteh.utils.TokenManager
import com.example.mineteh.model.repository.NotificationsRepository
import com.example.mineteh.view.NotificationsActivity
import com.example.mineteh.view.ItemDetailActivity
import com.example.mineteh.view.ChatActivity
import com.example.mineteh.MyOrdersActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * NotificationService - Supabase-only implementation
 * 
 * This service handles local push notifications using Android's notification system
 * combined with Supabase Realtime for real-time updates. No Firebase required.
 */
class NotificationService {

    companion object {
        private const val TAG = "NotificationService"
        private const val CHANNEL_ID = "mineteh_notifications"
        private const val CHANNEL_NAME = "MineTeh Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for bids, messages, and marketplace activity"
    }

    private lateinit var notificationsRepository: NotificationsRepository
    private lateinit var tokenManager: TokenManager

    fun initialize(context: Context) {
        createNotificationChannel(context)
        notificationsRepository = NotificationsRepository(context)
        tokenManager = TokenManager(context)
    }

    /**
     * Show a local notification based on notification data
     * This is called when a new notification is received via Supabase Realtime
     */
    fun showLocalNotification(context: Context, title: String, message: String, data: Map<String, String>) {
        val userId = tokenManager.getUserId()
        if (userId == -1) {
            Log.w(TAG, "No user ID found, cannot check notification preferences")
            return
        }

        // Check user preferences before showing notification
        CoroutineScope(Dispatchers.IO).launch {
            val type = data["type"] ?: ""
            if (shouldShowNotification(userId, type)) {
                showNotification(context, title, message, data)
            } else {
                Log.d(TAG, "Notification blocked by user preferences - Type: $type, UserId: $userId")
            }
        }
    }

    private fun showNotification(context: Context, title: String, message: String, data: Map<String, String>) {
        val intent = createDeepLinkIntent(context, data)
        val pendingIntent = PendingIntent.getActivity(
            context, 
            System.currentTimeMillis().toInt(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notifications)
            .setColor(context.getColor(R.color.purple))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))

        // Add notification type specific styling
        val type = data["type"]
        when (type) {
            "BID_PLACED" -> {
                notificationBuilder.setSmallIcon(R.drawable.ic_gavel)
                notificationBuilder.setColor(context.getColor(R.color.blue))
            }
            "BID_OUTBID" -> {
                notificationBuilder.setSmallIcon(R.drawable.ic_trending_up)
                notificationBuilder.setColor(context.getColor(R.color.orange))
            }
            "AUCTION_ENDING" -> {
                notificationBuilder.setSmallIcon(R.drawable.ic_timer)
                notificationBuilder.setColor(context.getColor(R.color.red))
                notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH)
            }
            "AUCTION_WON" -> {
                notificationBuilder.setSmallIcon(R.drawable.ic_trophy)
                notificationBuilder.setColor(context.getColor(R.color.green))
            }
            "AUCTION_LOST" -> {
                notificationBuilder.setSmallIcon(R.drawable.ic_close_circle)
                notificationBuilder.setColor(context.getColor(R.color.gray))
            }
            "ITEM_SOLD" -> {
                notificationBuilder.setSmallIcon(R.drawable.ic_check_circle)
                notificationBuilder.setColor(context.getColor(R.color.green))
            }
            "NEW_MESSAGE" -> {
                notificationBuilder.setSmallIcon(R.drawable.ic_message)
                notificationBuilder.setColor(context.getColor(R.color.purple))
            }
            "PAYMENT_RECEIVED" -> {
                notificationBuilder.setSmallIcon(R.drawable.ic_payment)
                notificationBuilder.setColor(context.getColor(R.color.green))
            }
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
        
        Log.d(TAG, "Notification shown: $title")
    }

    private fun createDeepLinkIntent(context: Context, data: Map<String, String>): Intent {
        val type = data["type"]
        val listingId = data["listing_id"]?.toIntOrNull()
        val messageId = data["message_id"]?.toIntOrNull()
        val senderId = data["sender_id"]?.toIntOrNull()

        return when (type) {
            "BID_PLACED", "BID_OUTBID", "AUCTION_ENDING", "AUCTION_WON", "AUCTION_LOST", "LISTING_APPROVED" -> {
                if (listingId != null) {
                    // Create deep link intent
                    Intent(Intent.ACTION_VIEW).apply {
                        this.data = android.net.Uri.parse("mineteh://listing/$listingId")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        setClass(context, ItemDetailActivity::class.java)
                        putExtra("listing_id", listingId)
                    }
                } else {
                    createDefaultIntent(context)
                }
            }
            "NEW_MESSAGE" -> {
                if (messageId != null && senderId != null) {
                    // Create deep link intent for chat
                    Intent(Intent.ACTION_VIEW).apply {
                        this.data = android.net.Uri.parse("mineteh://chat/$messageId?sender_id=$senderId")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        setClass(context, ChatActivity::class.java)
                        putExtra("message_id", messageId)
                        putExtra("sender_id", senderId)
                    }
                } else {
                    createDefaultIntent(context)
                }
            }
            "ITEM_SOLD", "PAYMENT_RECEIVED" -> {
                // Create deep link intent for orders
                Intent(Intent.ACTION_VIEW).apply {
                    this.data = android.net.Uri.parse("mineteh://orders" + if (listingId != null) "/$listingId" else "")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    setClass(context, MyOrdersActivity::class.java)
                    if (listingId != null) {
                        putExtra("listing_id", listingId)
                    }
                }
            }
            else -> createDefaultIntent(context)
        }
    }

    private fun createDefaultIntent(context: Context): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("mineteh://notifications")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            setClass(context, NotificationsActivity::class.java)
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                lightColor = context.getColor(R.color.purple)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            
            Log.d(TAG, "Notification channel created: $CHANNEL_ID")
        }
    }

    private fun sendTokenToServer(token: String) {
        // Note: This method is kept for future Firebase integration if needed
        // For Supabase-only approach, we don't need FCM tokens
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = tokenManager.getUserId()
                
                if (userId != -1) {
                    Log.d(TAG, "User $userId logged in - Supabase Realtime handles notifications")
                    // Store locally for potential future use
                    tokenManager.saveFcmToken(token)
                } else {
                    Log.w(TAG, "No user ID found")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling token", e)
            }
        }
    }

    /**
     * Check if notification should be shown based on user preferences
     */
    private suspend fun shouldShowNotification(userId: Int, notificationType: String): Boolean {
        // Preferences are intentionally disabled for now:
        // - prevents dummy/default preference data from blocking notifications
        // - keeps notifications behavior consistent
        return true
    }

    /**
     * Check if current time is within user's quiet hours
     */
    private suspend fun isInQuietHours(userId: Int): Boolean {
        return try {
            val preferencesResult = notificationsRepository.getPreferences(userId)
            if (preferencesResult is Resource.Success) {
                val preferences = preferencesResult.data!!
                val quietStart = preferences.quietHoursStart
                val quietEnd = preferences.quietHoursEnd

                if (quietStart != null && quietEnd != null) {
                    val currentTime = Calendar.getInstance()
                    val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
                    val currentMinute = currentTime.get(Calendar.MINUTE)
                    val currentTimeInMinutes = currentHour * 60 + currentMinute

                    val startParts = quietStart.split(":")
                    val endParts = quietEnd.split(":")
                    
                    if (startParts.size == 2 && endParts.size == 2) {
                        val startTimeInMinutes = startParts[0].toInt() * 60 + startParts[1].toInt()
                        val endTimeInMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()

                        return if (startTimeInMinutes <= endTimeInMinutes) {
                            // Same day quiet hours (e.g., 22:00 to 08:00 next day)
                            currentTimeInMinutes >= startTimeInMinutes && currentTimeInMinutes <= endTimeInMinutes
                        } else {
                            // Overnight quiet hours (e.g., 22:00 to 08:00 next day)
                            currentTimeInMinutes >= startTimeInMinutes || currentTimeInMinutes <= endTimeInMinutes
                        }
                    }
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking quiet hours", e)
            false // Default to not in quiet hours on error
        }
    }
}