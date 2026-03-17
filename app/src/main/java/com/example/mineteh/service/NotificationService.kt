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
import com.example.mineteh.Resource
import com.example.mineteh.utils.TokenManager
import com.example.mineteh.model.repository.NotificationsRepository
import com.example.mineteh.view.NotificationsActivity
import com.example.mineteh.view.ItemDetailActivity
import com.example.mineteh.view.ChatActivity
import com.example.mineteh.MyOrdersActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

class NotificationService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "NotificationService"
        private const val CHANNEL_ID = "mineteh_notifications"
        private const val CHANNEL_NAME = "MineTeh Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for bids, messages, and marketplace activity"
    }

    private lateinit var notificationsRepository: NotificationsRepository
    private lateinit var tokenManager: TokenManager

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        notificationsRepository = NotificationsRepository(this)
        tokenManager = TokenManager(this)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d(TAG, "From: ${remoteMessage.from}")
        
        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Check if message contains a notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            showNotification(
                title = it.title ?: "MineTeh",
                message = it.body ?: "",
                data = remoteMessage.data
            )
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        
        // Send token to server
        sendTokenToServer(token)
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val title = data["title"] ?: "MineTeh"
        val message = data["message"] ?: ""
        val type = data["type"] ?: ""
        val userId = data["user_id"]?.toIntOrNull()
        
        Log.d(TAG, "Handling data message - Type: $type, Title: $title, UserId: $userId")
        
        // Check user preferences before showing notification
        if (userId != null) {
            CoroutineScope(Dispatchers.IO).launch {
                if (shouldShowNotification(userId, type)) {
                    showNotification(title, message, data)
                } else {
                    Log.d(TAG, "Notification blocked by user preferences - Type: $type, UserId: $userId")
                }
            }
        } else {
            // If no user ID, show notification (fallback)
            showNotification(title, message, data)
        }
    }

    private fun showNotification(title: String, message: String, data: Map<String, String>) {
        val intent = createDeepLinkIntent(data)
        val pendingIntent = PendingIntent.getActivity(
            this, 
            System.currentTimeMillis().toInt(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notifications)
            .setColor(getColor(R.color.purple))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))

        // Add notification type specific styling
        val type = data["type"]
        when (type) {
            "BID_PLACED" -> {
                notificationBuilder.setSmallIcon(R.drawable.ic_gavel)
                notificationBuilder.setColor(getColor(R.color.blue))
            }
            "BID_OUTBID" -> {
                notificationBuilder.setSmallIcon(R.drawable.ic_trending_up)
                notificationBuilder.setColor(getColor(R.color.orange))
            }
            "AUCTION_ENDING" -> {
                notificationBuilder.setSmallIcon(R.drawable.ic_timer)
                notificationBuilder.setColor(getColor(R.color.red))
                notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH)
            }
            "AUCTION_WON" -> {
                notificationBuilder.setSmallIcon(R.drawable.ic_trophy)
                notificationBuilder.setColor(getColor(R.color.green))
            }
            "AUCTION_LOST" -> {
                notificationBuilder.setSmallIcon(R.drawable.ic_close_circle)
                notificationBuilder.setColor(getColor(R.color.gray))
            }
            "ITEM_SOLD" -> {
                notificationBuilder.setSmallIcon(R.drawable.ic_check_circle)
                notificationBuilder.setColor(getColor(R.color.green))
            }
            "NEW_MESSAGE" -> {
                notificationBuilder.setSmallIcon(R.drawable.ic_message)
                notificationBuilder.setColor(getColor(R.color.purple))
            }
            "PAYMENT_RECEIVED" -> {
                notificationBuilder.setSmallIcon(R.drawable.ic_payment)
                notificationBuilder.setColor(getColor(R.color.green))
            }
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
        
        Log.d(TAG, "Notification shown: $title")
    }

    private fun createDeepLinkIntent(data: Map<String, String>): Intent {
        val type = data["type"]
        val listingId = data["listing_id"]?.toIntOrNull()
        val messageId = data["message_id"]?.toIntOrNull()
        val senderId = data["sender_id"]?.toIntOrNull()

        return when (type) {
            "BID_PLACED", "BID_OUTBID", "AUCTION_ENDING", "AUCTION_WON", "AUCTION_LOST", "LISTING_APPROVED" -> {
                if (listingId != null) {
                    // Create deep link intent
                    Intent(Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse("mineteh://listing/$listingId")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        setClass(this@NotificationService, ItemDetailActivity::class.java)
                        putExtra("listing_id", listingId)
                    }
                } else {
                    createDefaultIntent()
                }
            }
            "NEW_MESSAGE" -> {
                if (messageId != null && senderId != null) {
                    // Create deep link intent for chat
                    Intent(Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse("mineteh://chat/$messageId?sender_id=$senderId")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        setClass(this@NotificationService, ChatActivity::class.java)
                        putExtra("message_id", messageId)
                        putExtra("sender_id", senderId)
                    }
                } else {
                    createDefaultIntent()
                }
            }
            "ITEM_SOLD", "PAYMENT_RECEIVED" -> {
                // Create deep link intent for orders
                Intent(Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse("mineteh://orders" + if (listingId != null) "/$listingId" else "")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    setClass(this@NotificationService, MyOrdersActivity::class.java)
                    if (listingId != null) {
                        putExtra("listing_id", listingId)
                    }
                }
            }
            else -> createDefaultIntent()
        }
    }

    private fun createDefaultIntent(): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("mineteh://notifications")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            setClass(this@NotificationService, NotificationsActivity::class.java)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                lightColor = getColor(R.color.purple)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            
            Log.d(TAG, "Notification channel created: $CHANNEL_ID")
        }
    }

    private fun sendTokenToServer(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = tokenManager.getUserId()
                
                if (userId != -1) {
                    // TODO: Send token to Supabase or your backend
                    // This would typically involve calling an API endpoint to store the FCM token
                    // associated with the user ID for sending targeted notifications
                    
                    Log.d(TAG, "FCM token for user $userId: $token")
                    
                    // For now, just store it locally
                    tokenManager.saveFcmToken(token)
                } else {
                    Log.w(TAG, "No user ID found, cannot associate FCM token")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending token to server", e)
            }
        }
    }

    /**
     * Check if notification should be shown based on user preferences
     */
    private suspend fun shouldShowNotification(userId: Int, notificationType: String): Boolean {
        return try {
            // Check if push notifications are globally enabled
            val pushEnabledResult = notificationsRepository.isPushNotificationEnabled(userId)
            if (pushEnabledResult is Resource.Success && !pushEnabledResult.data!!) {
                Log.d(TAG, "Push notifications disabled globally for user $userId")
                return false
            }

            // Check if this specific notification type is enabled
            val typeEnabledResult = notificationsRepository.isNotificationTypeEnabled(userId, notificationType)
            if (typeEnabledResult is Resource.Success && !typeEnabledResult.data!!) {
                Log.d(TAG, "Notification type $notificationType disabled for user $userId")
                return false
            }

            // Check quiet hours
            if (isInQuietHours(userId)) {
                Log.d(TAG, "Notification blocked by quiet hours for user $userId")
                return false
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking notification preferences, defaulting to show", e)
            true // Default to showing notification on error
        }
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