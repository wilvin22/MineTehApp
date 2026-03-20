package com.example.mineteh.view

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mineteh.R
import com.example.mineteh.model.MessageModel

class MessageAdapter(private val messageList: List<MessageModel>) :
    RecyclerView.Adapter<MessageAdapter.ViewHolder>() {

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
        val message = messageList[position]
        holder.senderName.text = message.senderName
        holder.messageSnippet.text = message.messageSnippet
        holder.time.text = message.time
        holder.profileImage.setImageResource(message.profileImageRes)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ChatActivity::class.java).apply {
                putExtra("senderName", message.senderName)
                putExtra("profileImage", message.profileImageRes)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = messageList.size
}