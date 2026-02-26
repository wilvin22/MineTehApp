package com.example.mineteh.view

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.mineteh.R

class SellActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sell)

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

        navInbox.setOnClickListener {
            startActivity(Intent(this, InboxActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        navMe.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }


        val categoryDropdown = findViewById<AutoCompleteTextView>(R.id.spinnerCategory)

        val categories = arrayOf("Item", "Vehicle", "Property")

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            categories
        )

        categoryDropdown.setAdapter(adapter)

        // Add Photo Button
        val btnAddPhoto = findViewById<Button>(R.id.btnAddPhoto)
        btnAddPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivity(intent)
        }
    }
}