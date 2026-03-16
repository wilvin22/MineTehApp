package com.example.mineteh.view

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mineteh.R

class NotificationsActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var notificationsRecyclerView: RecyclerView
    private lateinit var emptyStateLayout: View
    private lateinit var adapter: NotificationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.notifications)

        initViews()
        setupToolbar()
        setupRecyclerView()
        loadNotifications()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Notifications"
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = NotificationsAdapter { notification ->
            // Handle notification click
            markAsRead(notification.id)
        }
        
        notificationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@NotificationsActivity)
            adapter = this@NotificationsActivity.adapter
        }
    }

    private fun loadNotifications() {
        // For now, show sample notifications
        val sampleNotifications = listOf(
            NotificationItem(
                id = 1,
                title = "New bid on your item",
                message = "Someone placed a bid of ₱5,000 on your iPhone 12",
                time = "2 minutes ago",
                isRead = false,
                type = NotificationType.BID
            ),
            NotificationItem(
                id = 2,
                title = "Auction ending soon",
                message = "Your auction for Samsung Galaxy ends in 1 hour",
                time = "1 hour ago",
                isRead = false,
                type = NotificationType.AUCTION
            ),
            NotificationItem(
                id = 3,
                title = "Item sold",
                message = "Your MacBook Pro has been sold for ₱45,000",
                time = "3 hours ago",
                isRead = true,
                type = NotificationType.SALE
            )
        )

        if (sampleNotifications.isEmpty()) {
            showEmptyState()
        } else {
            showNotifications(sampleNotifications)
        }
    }

    private fun showNotifications(notifications: List<NotificationItem>) {
        notificationsRecyclerView.visibility = View.VISIBLE
        emptyStateLayout.visibility = View.GONE
        adapter.submitList(notifications)
    }

    private fun showEmptyState() {
        notificationsRecyclerView.visibility = View.GONE
        emptyStateLayout.visibility = View.VISIBLE
    }

    private fun markAsRead(notificationId: Int) {
        // Implement mark as read functionality
        adapter.markAsRead(notificationId)
    }
}

// Data classes for notifications
data class NotificationItem(
    val id: Int,
    val title: String,
    val message: String,
    val time: String,
    val isRead: Boolean,
    val type: NotificationType
)

enum class NotificationType {
    BID, AUCTION, SALE, MESSAGE, GENERAL
}