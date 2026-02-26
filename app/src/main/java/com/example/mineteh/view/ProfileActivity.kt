package com.example.mineteh.view

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.mineteh.Login
import com.example.mineteh.MyOrdersActivity
import com.example.mineteh.R
import com.example.mineteh.view.SellActivity
import com.example.mineteh.view.YourAuctionsActivity
import com.google.android.material.button.MaterialButton

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile)

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

        // Logout Button
        findViewById<MaterialButton>(R.id.logoutBtn).setOnClickListener {
            val intent = Intent(this, Login::class.java)
            // Clear the activity stack so user cannot go back to profile with back button
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Bottom Navigation
        val navHome = findViewById<LinearLayout>(R.id.nav_home)
        val navBid = findViewById<LinearLayout>(R.id.nav_bid)
        val navSell = findViewById<LinearLayout>(R.id.nav_sell)
        val navInbox = findViewById<LinearLayout>(R.id.nav_inbox)
        val navMe = findViewById<LinearLayout>(R.id.nav_profile)

        navHome.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        navBid.setOnClickListener {
            startActivity(Intent(this, BidActivity::class.java))
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
}