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

class HomeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ItemAdapter
    private lateinit var progressBar: ProgressBar
    
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.homepage)

        recyclerView = findViewById(R.id.itemRecyclerView)
        progressBar = findViewById(R.id.progressBar)

        setupRecyclerView()
        observeViewModel()

        // Top Icons Navigation
        findViewById<ImageView>(R.id.btnSavedItems).setOnClickListener {
            startActivity(Intent(this, SavedItemsActivity::class.java))
        }

        findViewById<ImageView>(R.id.btnCart).setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        // Bottom Navigation
        findViewById<LinearLayout>(R.id.nav_home).setOnClickListener {
            // Already here
        }

        findViewById<LinearLayout>(R.id.nav_bid).setOnClickListener {
            startActivity(Intent(this, BidActivity::class.java))
            overridePendingTransition(0, 0)
        }

        findViewById<LinearLayout>(R.id.nav_sell).setOnClickListener {
            startActivity(Intent(this, SellActivity::class.java))
            overridePendingTransition(0, 0)
        }

        findViewById<LinearLayout>(R.id.nav_inbox).setOnClickListener {
            startActivity(Intent(this, InboxActivity::class.java))
            overridePendingTransition(0, 0)
        }

        findViewById<LinearLayout>(R.id.nav_profile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            overridePendingTransition(0, 0)
        }
        
        // Category filters
        setupCategoryFilters()
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        adapter = ItemAdapter()
        recyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.listings.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    progressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    progressBar.visibility = View.GONE
                    resource.data?.let { listings ->
                        adapter.updateList(listings)
                    }
                }
                is Resource.Error -> {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
                null -> {}
            }
        }
    }

    private fun setupCategoryFilters() {
        // Simple filter implementation - can be expanded
        // Assuming the buttons in homepage.xml have IDs or we can find them in the layout
        // For now, it just demonstrates how to trigger the ViewModel
        
        // Example: If you had IDs for category buttons
        // findViewById<Button>(R.id.btn_all).setOnClickListener { viewModel.fetchListings() }
        // findViewById<Button>(R.id.btn_items).setOnClickListener { viewModel.fetchListings(category = "Items") }
    }
}
