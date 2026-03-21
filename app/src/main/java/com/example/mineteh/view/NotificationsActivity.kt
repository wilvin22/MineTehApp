package com.example.mineteh.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.mineteh.R
import com.example.mineteh.utils.Resource
import com.example.mineteh.model.Notification
import com.example.mineteh.viewmodel.NotificationsViewModel
import com.example.mineteh.MyOrdersActivity

class NotificationsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationsAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var emptyStateView: LinearLayout
    private lateinit var emptyStateText: TextView
    private lateinit var markAllReadButton: TextView
    private lateinit var settingsButton: ImageView
    
    private val viewModel: NotificationsViewModel by viewModels()
    
    private var isLoadingMore = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        initializeViews()
        setupRecyclerView()
        setupSwipeRefresh()
        setupClickListeners()
        observeViewModel()
        
        // Load initial data
        viewModel.loadNotifications()
        
        // Handle deep link if present
        handleDeepLink()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.notificationsRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        emptyStateView = findViewById(R.id.emptyStateView)
        emptyStateText = findViewById(R.id.emptyStateText)
        markAllReadButton = findViewById(R.id.markAllReadButton)
        settingsButton = findViewById(R.id.settingsButton)
    }

    private fun setupRecyclerView() {
        adapter = NotificationsAdapter(
            onNotificationClick = { notification ->
                handleNotificationClick(notification)
            },
            onNotificationLongClick = { notification ->
                handleNotificationLongClick(notification)
            }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        
        // Add scroll listener for pagination
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                
                if (!isLoadingMore && 
                    (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5 &&
                    firstVisibleItemPosition >= 0) {
                    loadMoreNotifications()
                }
            }
        })
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(R.color.purple)
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshNotifications()
        }
    }

    private fun setupClickListeners() {
        markAllReadButton.setOnClickListener {
            viewModel.markAllAsRead()
        }
        
        settingsButton.setOnClickListener {
            // Notification preferences are disabled for now.
        }

        // Hide settings UI; preferences are not used.
        settingsButton.visibility = View.GONE

        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }
    }

    private fun observeViewModel() {
        viewModel.notifications.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    if (!swipeRefreshLayout.isRefreshing) {
                        progressBar.visibility = View.VISIBLE
                    }
                    emptyStateView.visibility = View.GONE
                }
                is Resource.Success -> {
                    progressBar.visibility = View.GONE
                    swipeRefreshLayout.isRefreshing = false
                    isLoadingMore = false
                    
                    val notifications = resource.data ?: emptyList()
                    adapter.updateNotifications(notifications)
                    
                    if (notifications.isEmpty()) {
                        showEmptyState()
                    } else {
                        hideEmptyState()
                    }
                }
                is Resource.Error -> {
                    progressBar.visibility = View.GONE
                    swipeRefreshLayout.isRefreshing = false
                    isLoadingMore = false
                    
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                    
                    if (adapter.itemCount == 0) {
                        showEmptyState("Error loading notifications")
                    }
                }
                null -> {}
            }
        }

        viewModel.unreadCount.observe(this) { count ->
            updateUnreadCount(count)
        }

        viewModel.isRefreshing.observe(this) { isRefreshing ->
            swipeRefreshLayout.isRefreshing = isRefreshing
        }
    }

    private fun loadMoreNotifications() {
        if (!isLoadingMore) {
            isLoadingMore = true
            viewModel.loadMoreNotifications()
        }
    }

    private fun handleNotificationClick(notification: Notification) {
        // Mark as read if not already read
        if (!notification.isRead) {
            viewModel.markAsRead(notification.id)
        }
        
        // Navigate to relevant screen based on notification type
        navigateToRelevantScreen(notification)
    }

    private fun handleNotificationLongClick(notification: Notification): Boolean {
        // Show context menu for notification actions
        showNotificationContextMenu(notification)
        return true
    }

    private fun navigateToRelevantScreen(notification: Notification) {
        try {
            when (notification.type.name) {
                "BID_PLACED", "BID_OUTBID", "AUCTION_ENDING", "AUCTION_WON", "AUCTION_LOST" -> {
                    // Try to extract listing_id from the link field (e.g. "listing-details.php?id=5")
                    val listingId = notification.link
                        ?.substringAfter("id=")?.toIntOrNull()
                    if (listingId != null) {
                        val intent = Intent(this, ItemDetailActivity::class.java)
                        intent.putExtra("listing_id", listingId)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Unable to find related listing", Toast.LENGTH_SHORT).show()
                    }
                }
                "NEW_MESSAGE" -> {
                    startActivity(Intent(this, InboxActivity::class.java))
                }
                "ITEM_SOLD", "PAYMENT_RECEIVED" -> {
                    startActivity(Intent(this, MyOrdersActivity::class.java))
                }
                "LISTING_APPROVED" -> {
                    val listingId = notification.link
                        ?.substringAfter("id=")?.toIntOrNull()
                    if (listingId != null) {
                        val intent = Intent(this, ItemDetailActivity::class.java)
                        intent.putExtra("listing_id", listingId)
                        startActivity(intent)
                    }
                }
                else -> {
                    Toast.makeText(this, notification.title, Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open related content", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showNotificationContextMenu(notification: Notification) {
        val options = mutableListOf<String>()
        
        if (!notification.isRead) {
            options.add("Mark as read")
        }
        options.add("Delete")
        options.add("View details")
        
        // For now, just show a simple toast. In a real app, you'd show a proper context menu
        Toast.makeText(this, "Long press menu: ${options.joinToString(", ")}", Toast.LENGTH_SHORT).show()
    }

    private fun showNotificationDetails(notification: Notification) {
        // Show a dialog or navigate to a details screen
        Toast.makeText(this, "Notification: ${notification.title}", Toast.LENGTH_SHORT).show()
    }

    private fun showEmptyState(message: String = "No notifications yet") {
        emptyStateView.visibility = View.VISIBLE
        emptyStateText.text = message
        recyclerView.visibility = View.GONE
        markAllReadButton.visibility = View.GONE
    }

    private fun hideEmptyState() {
        emptyStateView.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        markAllReadButton.visibility = View.VISIBLE
    }

    private fun updateUnreadCount(count: Int) {
        // Update mark all read button visibility
        markAllReadButton.visibility = if (count > 0) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        // Refresh unread count when returning to the activity
        viewModel.loadUnreadCount()
        
        // Start real-time updates
        viewModel.startRealtimeUpdates()
    }

    override fun onPause() {
        super.onPause()
        // Stop real-time updates to save battery when not visible
        viewModel.stopRealtimeUpdates()
    }

    private fun handleDeepLink() {
        intent.data?.let { uri ->
            if (uri.scheme == "mineteh" && uri.host == "notifications") {
                // Deep link to notifications - already here, no additional action needed
                android.util.Log.d("NotificationsActivity", "Deep link to notifications received")
            }
        }
    }
}