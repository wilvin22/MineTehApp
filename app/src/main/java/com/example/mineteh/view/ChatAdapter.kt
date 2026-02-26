package com.example.mineteh.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mineteh.R
import com.example.mineteh.model.ChatMessageModel

class ChatAdapter(private val chatMessages: List<ChatMessageModel>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (chatMessages[position].isSentByMe) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_sent, parent, false)
            SentViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_received, parent, false)
            ReceivedViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = chatMessages[position]
        if (holder is SentViewHolder) {
            holder.messageText.text = message.content
        } else if (holder is ReceivedViewHolder) {
            holder.messageText.text = message.content
            // Note: In real app, set the profile image here
        }
    }

    override fun getItemCount(): Int = chatMessages.size

    class SentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.txtSentMessage)
    }

    class ReceivedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.txtReceivedMessage)
        val profileImage: ImageView = view.findViewById(R.id.imgReceiverProfile)
    }
}