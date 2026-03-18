package com.example.mineteh.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Conversation(
    @SerialName("conversation_id") val conversationId: Int,
    @SerialName("user1_id") val user1Id: Int,
    @SerialName("user2_id") val user2Id: Int,
    @SerialName("listing_id") val listingId: Int? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    
    // Additional fields populated by joins
    var otherUser: ConversationUser? = null,
    var listing: ListingPreview? = null,
    var lastMessage: Message? = null,
    var unreadCount: Int = 0
)

@Serializable
data class Message(
    @SerialName("message_id") val messageId: Int,
    @SerialName("conversation_id") val conversationId: Int,
    @SerialName("sender_id") val senderId: Int,
    @SerialName("message_text") val messageText: String,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("sent_at") val sentAt: String
)

@Serializable
data class ConversationUser(
    @SerialName("account_id") val accountId: Int,
    val username: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String
)

@Serializable
data class ListingPreview(
    val id: Int,
    val title: String,
    val price: Double
)

// For creating new conversations
data class CreateConversationRequest(
    val user1Id: Int,
    val user2Id: Int,
    val listingId: Int?
)

// For sending messages
data class SendMessageRequest(
    val conversationId: Int,
    val senderId: Int,
    val messageText: String
)
