package com.example.mineteh.model

import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val id: Int,
    val userId: Int,
    val type: NotificationType,
    val title: String,
    val message: String,
    val link: String? = null,
    val isRead: Boolean = false,
    val createdAt: String
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
    PAYMENT_RECEIVED,
    UNKNOWN;

    companion object {
        // Maps DB/website type strings to enum values safely
        fun fromString(value: String): NotificationType = when (value.lowercase()) {
            "bid_placed", "bid_received"    -> BID_PLACED
            "bid_outbid", "outbid"          -> BID_OUTBID
            "auction_ending"                -> AUCTION_ENDING
            "auction_won"                   -> AUCTION_WON
            "auction_lost"                  -> AUCTION_LOST
            "item_sold", "listing_sold"     -> ITEM_SOLD
            "new_message"                   -> NEW_MESSAGE
            "listing_approved"              -> LISTING_APPROVED
            "payment_received", "order_update" -> PAYMENT_RECEIVED
            else                            -> UNKNOWN
        }
    }
}

@Serializable
data class SupabaseNotificationResponse(
    val id: Int,
    val user_id: Int,
    val type: String,
    val title: String,
    val message: String,
    val link: String? = null,
    val is_read: Boolean = false,
    val created_at: String
) {
    fun toNotification(): Notification {
        return Notification(
            id = id,
            userId = user_id,
            type = NotificationType.fromString(type),
            title = title,
            message = message,
            link = link,
            isRead = is_read,
            createdAt = created_at
        )
    }
}

@Serializable
data class NotificationIdOnly(val id: Int)

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
    val quietHoursEnd: String? = null
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
    val quiet_hours_end: String? = null
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
            quietHoursEnd = quiet_hours_end
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