package com.example.mineteh.view

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.mineteh.R
import com.example.mineteh.model.Notification
import com.example.mineteh.model.NotificationType
import com.example.mineteh.utils.TimeUtils
import java.text.SimpleDateFormat
import java.util.*

class NotificationsAdapter(
    private val onNotificationClick: (Notification) -> Unit,
    private val onNotificationLongClick: (Notification) -> Boolean
) : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

    private var notifications = listOf<Notification>()

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconImageView: ImageView = itemView.findViewById(R.id.notificationIcon)
        val titleTextView: TextView = itemView.findViewById(R.id.notificationTitle)
        val messageTextView: TextView = itemView.findViewById(R.id.notificationMessage)
        val timestampTextView: TextView = itemView.findViewById(R.id.notificationTimestamp)
        val unreadIndicator: View = itemView.findViewById(R.id.unreadIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        
        // Set notification content
        holder.titleTextView.text = notification.title
        holder.messageTextView.text = notification.message
        
        // Set timestamp
        holder.timestampTextView.text = formatTimestamp(notification.createdAt)
        
        // Set notification type icon
        setNotificationIcon(holder.iconImageView, notification.type)
        
        // Set read/unread state
        setReadUnreadState(holder, notification.isRead)
        
        // Set click listeners
        holder.itemView.setOnClickListener {
            onNotificationClick(notification)
        }
        
        holder.itemView.setOnLongClickListener {
            onNotificationLongClick(notification)
        }
    }

    override fun getItemCount(): Int = notifications.size

    fun updateNotifications(newNotifications: List<Notification>) {
        val diffCallback = NotificationDiffCallback(notifications, newNotifications)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        notifications = newNotifications
        diffResult.dispatchUpdatesTo(this)
    }

    private fun setNotificationIcon(iconView: ImageView, type: NotificationType) {
        val iconRes = when (type) {
            NotificationType.BID_PLACED -> R.drawable.ic_gavel
            NotificationType.BID_OUTBID -> R.drawable.ic_trending_up
            NotificationType.AUCTION_ENDING -> R.drawable.ic_timer
            NotificationType.AUCTION_WON -> R.drawable.ic_trophy
            NotificationType.AUCTION_LOST -> R.drawable.ic_close_circle
            NotificationType.ITEM_SOLD -> R.drawable.ic_check_circle
            NotificationType.NEW_MESSAGE -> R.drawable.ic_message
            NotificationType.LISTING_APPROVED -> R.drawable.ic_verified
            NotificationType.PAYMENT_RECEIVED -> R.drawable.ic_payment
        }
        
        iconView.setImageResource(iconRes)
        
        // Set icon tint based on notification type
        val colorRes = when (type) {
            NotificationType.BID_PLACED -> R.color.blue
            NotificationType.BID_OUTBID -> R.color.orange
            NotificationType.AUCTION_ENDING -> R.color.red
            NotificationType.AUCTION_WON -> R.color.green
            NotificationType.AUCTION_LOST -> R.color.gray
            NotificationType.ITEM_SOLD -> R.color.green
            NotificationType.NEW_MESSAGE -> R.color.purple
            NotificationType.LISTING_APPROVED -> R.color.green
            NotificationType.PAYMENT_RECEIVED -> R.color.green
        }
        
        iconView.setColorFilter(ContextCompat.getColor(iconView.context, colorRes))
    }

    private fun setReadUnreadState(holder: NotificationViewHolder, isRead: Boolean) {
        val context = holder.itemView.context
        
        if (isRead) {
            // Read state - normal appearance
            holder.titleTextView.setTypeface(null, Typeface.NORMAL)
            holder.messageTextView.setTypeface(null, Typeface.NORMAL)
            holder.titleTextView.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            holder.messageTextView.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            holder.unreadIndicator.visibility = View.GONE
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
        } else {
            // Unread state - bold text and indicator
            holder.titleTextView.setTypeface(null, Typeface.BOLD)
            holder.messageTextView.setTypeface(null, Typeface.NORMAL)
            holder.titleTextView.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            holder.messageTextView.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            holder.unreadIndicator.visibility = View.VISIBLE
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.notification_unread_bg))
        }
    }

    private fun formatTimestamp(timestamp: String): String {
        return try {
            // Parse the timestamp from Supabase format
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
            val date = inputFormat.parse(timestamp)
            
            if (date != null) {
                TimeUtils.getRelativeTimeString(date)
            } else {
                "Unknown time"
            }
        } catch (e: Exception) {
            try {
                // Try alternative format without microseconds
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val date = inputFormat.parse(timestamp)
                
                if (date != null) {
                    TimeUtils.getRelativeTimeString(date)
                } else {
                    "Unknown time"
                }
            } catch (e2: Exception) {
                "Unknown time"
            }
        }
    }

    class NotificationDiffCallback(
        private val oldList: List<Notification>,
        private val newList: List<Notification>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            
            return oldItem.id == newItem.id &&
                    oldItem.title == newItem.title &&
                    oldItem.message == newItem.message &&
                    oldItem.isRead == newItem.isRead &&
                    oldItem.type == newItem.type &&
                    oldItem.createdAt == newItem.createdAt
        }
    }
}