package com.example.mineteh.view

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.mineteh.Login
import com.example.mineteh.MyOrdersActivity
import com.example.mineteh.R
import com.example.mineteh.utils.AvatarUtils
import com.example.mineteh.utils.Resource
import com.example.mineteh.utils.TokenManager
import com.example.mineteh.view.SellActivity
import com.example.mineteh.view.YourAuctionsActivity
import com.example.mineteh.viewmodel.SellingDashboardViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView

class ProfileActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private val sellingDashboardViewModel: SellingDashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile)

        tokenManager = TokenManager(this)

        // Set User info from TokenManager
        findViewById<TextView>(R.id.usernameText)?.text = tokenManager.getUserName() ?: "Username"
        findViewById<TextView>(R.id.emailText)?.text = tokenManager.getUserEmail() ?: "email@example.com"

        // Bind profile avatar using username as fallback for initials
        val username = tokenManager.getUserName() ?: ""
        val accountId = tokenManager.getUserId()
        val ivAvatar = findViewById<ShapeableImageView>(R.id.ivProfileAvatar)
        if (ivAvatar != null) {
            // TokenManager stores username only; use first char as first name, empty last name
            AvatarUtils.bindAvatar(
                view = ivAvatar,
                firstName = username,
                lastName = "",
                accountId = accountId,
                avatarUrl = null,
                context = this
            )
        }

        // Observe selling stats
        val tvActive = findViewById<TextView>(R.id.tvStatActive)
        val tvSold = findViewById<TextView>(R.id.tvStatSold)
        val tvMessages = findViewById<TextView>(R.id.tvStatMessages)
        val tvRating = findViewById<TextView>(R.id.tvStatRating)

        sellingDashboardViewModel.stats.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    tvActive?.text = "..."
                    tvSold?.text = "..."
                    tvMessages?.text = "..."
                    tvRating?.text = "..."
                }
                is Resource.Success -> {
                    val stats = resource.data
                    tvActive?.text = stats?.activeListings?.toString() ?: "—"
                    tvSold?.text = stats?.totalSold?.toString() ?: "—"
                    tvMessages?.text = stats?.unreadMessages?.toString() ?: "—"
                    tvRating?.text = if (stats != null && stats.averageRating > 0)
                        String.format("%.1f", stats.averageRating) else "—"
                }
                is Resource.Error -> {
                    tvActive?.text = "—"
                    tvSold?.text = "—"
                    tvMessages?.text = "—"
                    tvRating?.text = "—"
                }
            }
        }

        sellingDashboardViewModel.loadStats()

        // Profile Action Buttons
        findViewById<LinearLayout>(R.id.btnMyOrders).setOnClickListener {
            startActivity(Intent(this, MyOrdersActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.btnMyBids).setOnClickListener {
            startActivity(Intent(this, YourAuctionsActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.btnMyListings).setOnClickListener {
            startActivity(Intent(this, MyListingsActivity::class.java))
        }

        // Logout Button with Confirmation
        findViewById<MaterialButton>(R.id.logoutBtn).setOnClickListener {
            showLogoutConfirmation()
        }

        // Bottom Navigation
        val navHome = findViewById<LinearLayout>(R.id.nav_home)
        val navNotifications = findViewById<LinearLayout>(R.id.nav_notifications)
        val navSell = findViewById<LinearLayout>(R.id.nav_sell)
        val navInbox = findViewById<LinearLayout>(R.id.nav_inbox)
        val navMe = findViewById<LinearLayout>(R.id.nav_profile)

        navHome.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        navNotifications.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
        navSell.setOnClickListener {
            startActivity(Intent(this, SellActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
        navInbox.setOnClickListener {
            startActivity(Intent(this, InboxActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                performLogout()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun performLogout() {
        // 1. Clear saved credentials
        tokenManager.clearAll()

        // 2. Navigate back to Login
        val intent = Intent(this, Login::class.java)
        
        // 3. Clear the activity stack so user cannot go back to profile with back button
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
