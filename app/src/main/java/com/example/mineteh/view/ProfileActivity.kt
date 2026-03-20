package com.example.mineteh.view

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.RatingBar
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
import com.example.mineteh.viewmodel.SellingDashboardViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private val sellingDashboardViewModel: SellingDashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile)

        tokenManager = TokenManager(this)

        // Toolbar back button
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // User info
        val username = tokenManager.getUserName() ?: "Username"
        val accountId = tokenManager.getUserId()
        findViewById<TextView>(R.id.usernameText).text = username
        findViewById<TextView>(R.id.emailText).text = tokenManager.getUserEmail() ?: "email@example.com"

        // Avatar
        AvatarUtils.bindAvatar(
            view = findViewById<ShapeableImageView>(R.id.ivProfileAvatar),
            firstName = username,
            lastName = "",
            accountId = accountId,
            avatarUrl = null,
            context = this
        )

        // Member since — TokenManager doesn't store this yet, show placeholder
        findViewById<TextView>(R.id.tvMemberSince).text = "🗓 Member since —"

        // Observe stats
        sellingDashboardViewModel.stats.observe(this) { resource ->
            val tvActive    = findViewById<TextView>(R.id.tvStatActive)
            val tvSold      = findViewById<TextView>(R.id.tvStatSold)
            val tvMessages  = findViewById<TextView>(R.id.tvStatMessages)
            val tvRating    = findViewById<TextView>(R.id.tvStatRating)
            val tvSellerRating = findViewById<TextView>(R.id.tvStatSellerRating)
            val tvInactive  = findViewById<TextView>(R.id.tvStatInactive)
            val tvLive      = findViewById<TextView>(R.id.tvStatLiveAuctions)
            val ratingBar   = findViewById<RatingBar>(R.id.profileRatingBar)

            when (resource) {
                is Resource.Loading -> {
                    listOf(tvActive, tvSold, tvMessages, tvRating, tvSellerRating, tvInactive, tvLive)
                        .forEach { it.text = "..." }
                }
                is Resource.Success -> {
                    val s = resource.data
                    tvActive.text   = s?.activeListings?.toString() ?: "0"
                    tvSold.text     = s?.totalSold?.toString() ?: "0"
                    tvMessages.text = s?.unreadMessages?.toString() ?: "0"
                    tvInactive.text = s?.inactiveListings?.toString() ?: "0"
                    tvLive.text     = s?.liveAuctions?.toString() ?: "0"
                    val rating = s?.averageRating ?: 0.0
                    val ratingStr = if (rating > 0) String.format("%.1f", rating) else "—"
                    tvRating.text = ratingStr
                    tvSellerRating.text = ratingStr
                    ratingBar.rating = rating.toFloat()
                }
                is Resource.Error -> {
                    listOf(tvActive, tvSold, tvMessages, tvRating, tvSellerRating, tvInactive, tvLive)
                        .forEach { it.text = "—" }
                }
                null -> {}
            }
        }

        sellingDashboardViewModel.loadStats()

        // Create new listing
        findViewById<MaterialButton>(R.id.btnCreateListing).setOnClickListener {
            startActivity(Intent(this, SellActivity::class.java))
        }

        // See all listings (now a TextView acting as link)
        findViewById<TextView>(R.id.btnMyListings).setOnClickListener {
            startActivity(Intent(this, MyListingsActivity::class.java))
        }

        // Quick actions
        findViewById<LinearLayout>(R.id.btnMyOrders).setOnClickListener {
            startActivity(Intent(this, MyOrdersActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.btnMyBids).setOnClickListener {
            startActivity(Intent(this, YourAuctionsActivity::class.java))
        }

        // Logout
        findViewById<MaterialButton>(R.id.logoutBtn).setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ -> performLogout() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun performLogout() {
        tokenManager.clearAll()
        startActivity(Intent(this, Login::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
