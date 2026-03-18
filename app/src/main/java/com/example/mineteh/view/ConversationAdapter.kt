package com.example.mineteh.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mineteh.R
import com.example.mineteh.models.Conversation
import java.text.SimpleDateFormat
import java.util.*

class ConversationAdapter(
    private val conversations: List<Conversation>,
    private val onConversationClick: (Conversation) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.messageProfileImage)
        val senderName: TextView = itemView.findViewById(R.id.messageSenderName)
        val messageSnippet: TextView = itemView.findViewById(R.id.messageSnippet)
        val time: TextView = itemView.findViewById(R.id.messageTime)
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
        
        // Set default profile image
        holder.profileImage.setImageResource(R.drawable.ic_launcher_background)
        
        // Handle click
        holder.itemView.setOnClickListener {
            onConversationClick(conversation)
        }
    }

    override fun getItemCount(): Int = conversations.size
    
    private fun formatTime(timestamp: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(timestamp) ?: return "Now"
            
            val now = System.currentTimeMillis()
            val diff = now - date.time
            
            when {
                diff < 60000 -> "Now"
                diff < 3600000 -> "${diff / 60000}m"
                diff < 86400000 -> "${diff / 3600000}h"
                diff < 604800000 -> "${diff / 86400000}d"
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
