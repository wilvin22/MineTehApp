package com.example.mineteh.model

import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val id: Int,
    val userId: Int,
    val type: NotificationType,
    val title: String,
    val message: String,
    val data: Map<String, String>? = null,
    val isRead: Boolean = false,
    val createdAt: String,
    val updatedAt: String
)

enum class NotificationType {
    BID_PLACED,
    BID_OUTBID,
    AUCTION_ENDING,
    AUCTION_WON,
    AUCTION_LOST,
    ITEM_SOLD,
    NEW_MESSAGE,
    LISTING_APPROVED,
    PAYMENT_RECEIVED
}

@Serializable
data class SupabaseNotificationResponse(
    val id: Int,
    val user_id: Int,
    val type: String,
    val title: String,
    val message: String,
    val data: Map<String, String>? = null,
    val is_read: Boolean = false,
    val created_at: String,
    val updated_at: String
) {
    fun toNotification(): Notification {
        return Notification(
            id = id,
            userId = user_id,
            type = NotificationType.valueOf(type),
            title = title,
            message = message,
            data = data,
            isRead = is_read,
            createdAt = created_at,
            updatedAt = updated_at
        )
    }
}

@Serializable
data class NotificationPreferences(
    val id: Int = 0,
    val userId: Int,
    val bidPlacedEnabled: Boolean = true,
    val bidOutbidEnabled: Boolean = true,
    val auctionEndingEnabled: Boolean = true,
    val auctionWonEnabled: Boolean = true,
    val auctionLostEnabled: Boolean = true,
    val itemSoldEnabled: Boolean = true,
    val newMessageEnabled: Boolean = true,
    val listingApprovedEnabled: Boolean = true,
    val paymentReceivedEnabled: Boolean = true,
    val pushNotificationsEnabled: Boolean = true,
    val quietHoursStart: String? = null,
    val quietHoursEnd: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class SupabaseNotificationPreferencesResponse(
    val id: Int,
    val user_id: Int,
    val bid_placed_enabled: Boolean = true,
    val bid_outbid_enabled: Boolean = true,
    val auction_ending_enabled: Boolean = true,
    val auction_won_enabled: Boolean = true,
    val auction_lost_enabled: Boolean = true,
    val item_sold_enabled: Boolean = true,
    val new_message_enabled: Boolean = true,
    val listing_approved_enabled: Boolean = true,
    val payment_received_enabled: Boolean = true,
    val push_notifications_enabled: Boolean = true,
    val quiet_hours_start: String? = null,
    val quiet_hours_end: String? = null,
    val created_at: String,
    val updated_at: String
) {
    fun toNotificationPreferences(): NotificationPreferences {
        return NotificationPreferences(
            id = id,
            userId = user_id,
            bidPlacedEnabled = bid_placed_enabled,
            bidOutbidEnabled = bid_outbid_enabled,
            auctionEndingEnabled = auction_ending_enabled,
            auctionWonEnabled = auction_won_enabled,
            auctionLostEnabled = auction_lost_enabled,
            itemSoldEnabled = item_sold_enabled,
            newMessageEnabled = new_message_enabled,
            listingApprovedEnabled = listing_approved_enabled,
            paymentReceivedEnabled = payment_received_enabled,
            pushNotificationsEnabled = push_notifications_enabled,
            quietHoursStart = quiet_hours_start,
            quietHoursEnd = quiet_hours_end,
            createdAt = created_at,
            updatedAt = updated_at
        )
    }
}

data class NotificationContext(
    val userId: Int,
    val type: NotificationType,
    val listingId: Int? = null,
    val bidId: Int? = null,
    val messageId: Int? = null,
    val amount: Double? = null,
    val additionalData: Map<String, String> = emptyMap()
)

data class NotificationTemplate(
    val type: NotificationType,
    val titleTemplate: String,
    val messageTemplate: String
) {
    fun generateNotification(context: NotificationContext, templateData: Map<String, String>): Notification {
        val title = titleTemplate.replace(templateData)
        val message = messageTemplate.replace(templateData)
        
        return Notification(
            id = 0, // Will be set by database
            userId = context.userId,
            type = type,
            title = title,
            message = message,
            data = context.additionalData,
            isRead = false,
            createdAt = "", // Will be set by database
            updatedAt = ""
        )
    }
}

// Extension function to replace template placeholders
private fun String.replace(templateData: Map<String, String>): String {
    var result = this
    templateData.forEach { (key, value) ->
        result = result.replace("{$key}", value)
    }
    return result
}