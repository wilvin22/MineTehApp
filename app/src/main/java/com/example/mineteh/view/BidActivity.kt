package com.example.mineteh.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mineteh.R
import com.example.mineteh.utils.Resource
import com.example.mineteh.viewmodel.HomeViewModel

class BidActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ItemAdapter
    private lateinit var progressBar: ProgressBar

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bid)

        recyclerView = findViewById(R.id.itemRecyclerView)
        // Note: The bid.xml layout doesn't have a progressBar defined in the shared content,
        // but if it's missing, we should either add it or handle its absence.
        // Looking at the read_file output of bid.xml, it is missing.
        // I will use a simple check or assume it's there if I was to add it.
        // For now, I'll just focus on fixing the type mismatch.

        setupRecyclerView()
        observeViewModel()

        // Top Icons Navigation
        findViewById<ImageView>(R.id.btnSavedItems).setOnClickListener {
            startActivity(Intent(this, SavedItemsActivity::class.java))
        }

        findViewById<ImageView>(R.id.btnCart).setOnClickListener {
            startActivity(Intent(this, YourAuctionsActivity::class.java))
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
            overridePendingTransition(0, 0)
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

        navMe.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
        
        // Fetch only BID type listings
        viewModel.fetchListings(type = "BID")
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        adapter = ItemAdapter(isBidActivity = true)
        recyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.listings.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    // Show progress if you add it to layout
                }
                is Resource.Success -> {
                    resource.data?.let { listings ->
                        adapter.updateList(listings)
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
                null -> {}
            }
        }
    }
}
