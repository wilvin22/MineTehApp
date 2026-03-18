package com.example.mineteh.model.repository

import android.content.Context
import android.util.Log
import com.example.mineteh.models.*
import com.example.mineteh.supabase.SupabaseClient
import com.example.mineteh.utils.Resource
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
private data class SupabaseConversation(
    val conversation_id: Int,
    val user1_id: Int,
    val user2_id: Int,
    val listing_id: Int? = null,
    val created_at: String,
    val updated_at: String
)

@Serializable
private data class SupabaseMessage(
    val message_id: Int,
    val conversation_id: Int,
    val sender_id: Int,
    val message_text: String,
    val is_read: Boolean,
    val sent_at: String
)

@Serializable
private data class SupabaseUser(
    val account_id: Int,
    val username: String,
    val first_name: String,
    val last_name: String
)

@Serializable
private data class SupabaseListingPreview(
    val id: Int,
    val title: String,
    val price: Double
)

@Serializable
private data class InsertMessageData(
    val conversation_id: Int,
    val sender_id: Int,
    val message_text: String
)

@Serializable
private data class UpdateConversationData(
    val updated_at: String
)

@Serializable
private data class UpdateMessageReadData(
    val is_read: Boolean
)

@Serializable
private data class InsertConversationData(
    val user1_id: Int,
    val user2_id: Int,
    val listing_id: Int? = null
)

class MessagingRepository(private val context: Context) {
    private val tag = "MessagingRepository"
    private val supabase = SupabaseClient.client
    private val tokenManager = com.example.mineteh.utils.TokenManager(context)

    suspend fun getUserConversations(): Resource<List<Conversation>> = withContext(Dispatchers.IO) {
        try {
            val userId = tokenManager.getUserId()
            if (userId == -1) return@withContext Resource.Error("Not authenticated")
            
            Log.d(tag, "Fetching conversations for userId=$userId")
            
            // Fetch conversations where user is either user1 or user2
            val rows = supabase.from("conversations")
                .select(columns = Columns.list(
                    "conversation_id", "user1_id", "user2_id", "listing_id", "created_at", "updated_at"
                )) {
                    filter {
                        or {
                            eq("user1_id", userId)
                            eq("user2_id", userId)
                        }
                    }
                    order("updated_at", order = Order.DESCENDING)
                }
                .decodeList<SupabaseConversation>()
            
            if (rows.isEmpty()) return@withContext Resource.Success(emptyList())
            
            // Get other user IDs and listing IDs
            val otherUserIds = rows.map { conv ->
                if (conv.user1_id == userId) conv.user2_id else conv.user1_id
            }.distinct()
            
            val listingIds = rows.mapNotNull { it.listing_id }.distinct()
            
            // Fetch other users
            val userMap = mutableMapOf<Int, ConversationUser>()
            if (otherUserIds.isNotEmpty()) {
                val users = supabase.from("accounts")
                    .select(columns = Columns.list("account_id", "username", "first_name", "last_name")) {
                        filter { isIn("account_id", otherUserIds) }
                    }
                    .decodeList<SupabaseUser>()
                users.forEach {
                    userMap[it.account_id] = ConversationUser(it.account_id, it.username, it.first_name, it.last_name)
                }
            }
            
            // Fetch listings
            val listingMap = mutableMapOf<Int, ListingPreview>()
            if (listingIds.isNotEmpty()) {
                val listings = supabase.from("listings")
                    .select(columns = Columns.list("id", "title", "price")) {
                        filter { isIn("id", listingIds) }
                    }
                    .decodeList<SupabaseListingPreview>()
                listings.forEach {
                    listingMap[it.id] = ListingPreview(it.id, it.title, it.price)
                }
            }
            
            // Build conversations with additional data
            val conversations = rows.map { row ->
                val otherId = if (row.user1_id == userId) row.user2_id else row.user1_id
                Conversation(
                    conversationId = row.conversation_id,
                    user1Id = row.user1_id,
                    user2Id = row.user2_id,
                    listingId = row.listing_id,
                    createdAt = row.created_at,
                    updatedAt = row.updated_at,
                    otherUser = userMap[otherId],
                    listing = row.listing_id?.let { listingMap[it] }
                )
            }
            
            Log.d(tag, "Fetched ${conversations.size} conversations")
            Resource.Success(conversations)
        } catch (e: Exception) {
            Log.e(tag, "Error fetching conversations", e)
            Resource.Error(e.message ?: "Failed to load conversations")
        }
    }

    suspend fun getMessages(conversationId: Int): Resource<List<Message>> = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Fetching messages for conversationId=$conversationId")
            
            val rows = supabase.from("messages")
                .select(columns = Columns.list(
                    "message_id", "conversation_id", "sender_id", "message_text", "is_read", "sent_at"
                )) {
                    filter { eq("conversation_id", conversationId) }
                    order("sent_at", order = Order.ASCENDING)
                }
                .decodeList<SupabaseMessage>()
            
            val messages = rows.map {
                Message(it.message_id, it.conversation_id, it.sender_id, it.message_text, it.is_read, it.sent_at)
            }
            
            Log.d(tag, "Fetched ${messages.size} messages")
            Resource.Success(messages)
        } catch (e: Exception) {
            Log.e(tag, "Error fetching messages", e)
            Resource.Error(e.message ?: "Failed to load messages")
        }
    }
    
    suspend fun sendMessage(conversationId: Int, messageText: String): Resource<Message> = withContext(Dispatchers.IO) {
        try {
            val userId = tokenManager.getUserId()
            if (userId == -1) return@withContext Resource.Error("Not authenticated")
            
            Log.d(tag, "Sending message to conversationId=$conversationId")
            
            val insertData = InsertMessageData(
                conversation_id = conversationId,
                sender_id = userId,
                message_text = messageText
            )
            
            val result = supabase.from("messages")
                .insert(insertData) {
                    select()
                }
                .decodeSingle<SupabaseMessage>()
            
            // Update conversation updated_at
            val updateData = UpdateConversationData(updated_at = result.sent_at)
            supabase.from("conversations")
                .update(updateData) {
                    filter { eq("conversation_id", conversationId) }
                }
            
            val message = Message(
                result.message_id,
                result.conversation_id,
                result.sender_id,
                result.message_text,
                result.is_read,
                result.sent_at
            )
            
            Log.d(tag, "Message sent successfully")
            Resource.Success(message)
        } catch (e: Exception) {
            Log.e(tag, "Error sending message", e)
            Resource.Error(e.message ?: "Failed to send message")
        }
    }

    suspend fun markMessagesAsRead(conversationId: Int): Resource<Boolean> = withContext(Dispatchers.IO) {
        try {
            val userId = tokenManager.getUserId()
            if (userId == -1) return@withContext Resource.Error("Not authenticated")
            
            Log.d(tag, "Marking messages as read for conversationId=$conversationId")
            
            // Mark all unread messages from other user as read
            val updateData = UpdateMessageReadData(is_read = true)
            supabase.from("messages")
                .update(updateData) {
                    filter {
                        eq("conversation_id", conversationId)
                        neq("sender_id", userId)
                        eq("is_read", false)
                    }
                }
            
            Resource.Success(true)
        } catch (e: Exception) {
            Log.e(tag, "Error marking messages as read", e)
            Resource.Error(e.message ?: "Failed to mark messages as read")
        }
    }
    
    suspend fun getOrCreateConversation(otherUserId: Int, listingId: Int?): Resource<Conversation> = withContext(Dispatchers.IO) {
        try {
            val userId = tokenManager.getUserId()
            if (userId == -1) return@withContext Resource.Error("Not authenticated")
            
            Log.d(tag, "Getting or creating conversation with userId=$otherUserId, listingId=$listingId")
            
            // Check if conversation exists (check both directions)
            val existing = supabase.from("conversations")
                .select(columns = Columns.list(
                    "conversation_id", "user1_id", "user2_id", "listing_id", "created_at", "updated_at"
                )) {
                    filter {
                        if (listingId != null) {
                            eq("listing_id", listingId)
                        }
                        or {
                            and {
                                eq("user1_id", userId)
                                eq("user2_id", otherUserId)
                            }
                            and {
                                eq("user1_id", otherUserId)
                                eq("user2_id", userId)
                            }
                        }
                    }
                    limit(1)
                }
                .decodeList<SupabaseConversation>()
            
            if (existing.isNotEmpty()) {
                val conv = existing[0]
                return@withContext Resource.Success(Conversation(
                    conv.conversation_id, conv.user1_id, conv.user2_id,
                    conv.listing_id, conv.created_at, conv.updated_at
                ))
            }
            
            // Create new conversation
            val insertData = InsertConversationData(
                user1_id = userId,
                user2_id = otherUserId,
                listing_id = listingId
            )
            
            val result = supabase.from("conversations")
                .insert(insertData) {
                    select()
                }
                .decodeSingle<SupabaseConversation>()
            
            val conversation = Conversation(
                result.conversation_id, result.user1_id, result.user2_id,
                result.listing_id, result.created_at, result.updated_at
            )
            
            Log.d(tag, "Conversation created with id=${conversation.conversationId}")
            Resource.Success(conversation)
        } catch (e: Exception) {
            Log.e(tag, "Error getting/creating conversation", e)
            Resource.Error(e.message ?: "Failed to create conversation")
        }
    }
}
