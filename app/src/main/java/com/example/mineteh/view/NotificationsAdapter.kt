package com.example.mineteh.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mineteh.R

class NotificationsAdapter(
    private val onItemClick: (NotificationItem) -> Unit
) : RecyclerView.Adapter<NotificationsAdapter.ViewHolder>() {

    private var notifications = listOf<NotificationItem>()

    fun submitList(newNotifications: List<NotificationItem>) {
        notifications = newNotifications
        notifyDataSetChanged()
    }

    fun markAsRead(notificationId: Int) {
        val index = notifications.indexOfFirst { it.id == notificationId }
        if (index != -1) {
            val updatedNotifications = notifications.toMutableList()
            updatedNotifications[index] = updatedNotifications[index].copy(isRead = true)
            notifications = updatedNotifications
            notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification_enhanced, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount() = notifications.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val notificationIcon: ImageView = itemView.findViewById(R.id.notificationIcon)
        private val notificationTitle: TextView = itemView.findViewById(R.id.notificationTitle)
        private val notificationMessage: TextView = itemView.findViewById(R.id.notificationMessage)
        private val notificationTime: TextView = itemView.findViewById(R.id.notificationTime)
        private val unreadIndicator: View = itemView.findViewById(R.id.unreadIndicator)

        fun bind(notification: NotificationItem) {
            notificationTitle.text = notification.title
            notificationMessage.text = notification.message
            notificationTime.text = notification.time

            // Set icon based on notification type
            val iconRes = when (notification.type) {
                NotificationType.BID -> R.drawable.bid
                NotificationType.AUCTION -> R.drawable.auction
                NotificationType.SALE -> R.drawable.sell
                NotificationType.MESSAGE -> R.drawable.inbox
                NotificationType.GENERAL -> R.drawable.ic_notifications
            }
            notificationIcon.setImageResource(iconRes)

            // Show/hide unread indicator
            unreadIndicator.visibility = if (notification.isRead) View.GONE else View.VISIBLE

            // Set background alpha for read/unread
            itemView.alpha = if (notification.isRead) 0.7f else 1.0f

            itemView.setOnClickListener {
                onItemClick(notification)
            }
        }
    }
}