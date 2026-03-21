package com.example.mineteh.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mineteh.R
import com.example.mineteh.models.Conversation
import com.example.mineteh.utils.AvatarUtils
import java.text.SimpleDateFormat
import java.util.*

class ConversationAdapter(
    private val conversations: List<Conversation>,
    private val onConversationClick: (Conversation) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: TextView = itemView.findViewById(R.id.messageProfileImage)
        val senderName: TextView = itemView.findViewById(R.id.messageSenderName)
        val messageSnippet: TextView = itemView.findViewById(R.id.messageSnippet)
        val time: TextView = itemView.findViewById(R.id.messageTime)
        val unreadBadge: TextView = itemView.findViewById(R.id.unreadBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val conversation = conversations[position]
        
        // Display other user's name
        val otherUser = conversation.otherUser
        val displayName = if (otherUser != null) {
            val fullName = "${otherUser.firstName} ${otherUser.lastName}".trim()
            if (fullName.isNotEmpty()) fullName else otherUser.username
        } else {
            "Unknown User"
        }
        holder.senderName.text = displayName
        
        // Display listing title if available
        val snippet = conversation.listing?.let { "Re: ${it.title}" } ?: "Start a conversation"
        holder.messageSnippet.text = snippet
        
        // Format time
        holder.time.text = formatTime(conversation.updatedAt)

        // Show initials instead of an avatar image
        val initials = otherUser?.let { user ->
            val first = user.firstName.trim()
            val last = user.lastName.trim()
            if (first.isNotEmpty() || last.isNotEmpty()) {
                AvatarUtils.getInitials(first, last).ifEmpty { user.username.take(1).uppercase() }
            } else {
                user.username.take(1).uppercase()
            }
        } ?: "U"
        holder.profileImage.text = initials

        // New message indicator
        val unreadCount = conversation.unreadCount
        holder.unreadBadge.visibility = if (unreadCount > 0) View.VISIBLE else View.GONE
        holder.unreadBadge.text = if (unreadCount > 99) "99+" else unreadCount.toString()
        
        // Handle click
        holder.itemView.setOnClickListener {
            onConversationClick(conversation)
        }
    }

    override fun getItemCount(): Int = conversations.size
    
    private fun formatTime(timestamp: String): String {
        return try {
            // Supabase typically returns ISO timestamps.
            // We treat the parsed instant as UTC and display relative time in the user's locale.
            val date = try {
                java.time.Instant.parse(timestamp)
                    .toEpochMilli()
                    .let { Date(it) }
            } catch (_: Exception) {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                inputFormat.parse(timestamp) ?: return "Now"
            }

            val diffMs = System.currentTimeMillis() - date.time
            val minutes = diffMs / 60000
            val hours = diffMs / 3600000

            return when {
                diffMs < 60000 -> "Now"
                minutes < 60 -> "${minutes}m ago"
                hours < 24 -> "${hours}h ago"
                else -> {
                    val outputFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                    outputFormat.format(date)
                }
            }
        } catch (e: Exception) {
            "Now"
        }
    }
}
