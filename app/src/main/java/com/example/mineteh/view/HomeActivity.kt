package com.example.mineteh.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mineteh.R
import com.example.mineteh.utils.Categories
import com.example.mineteh.utils.Resource
import com.example.mineteh.utils.NotificationBadgeManager
import com.example.mineteh.viewmodel.HomeViewModel
import com.example.mineteh.viewmodel.NotificationsViewModel
import com.google.android.material.button.MaterialButton

class HomeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ItemAdapter
    private lateinit var progressBar: ProgressBar
    
    private val viewModel: HomeViewModel by viewModels()
    private val notificationsViewModel: NotificationsViewModel by viewModels()
    
    // Category buttons
    private lateinit var categoryButtons: List<MaterialButton>
    private var selectedCategory: String? = null

    // Type filter buttons
    private lateinit var typeButtons: List<MaterialButton>
    private var selectedListingType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("HomeActivity", "onCreate() called")
        android.util.Log.d("HomeActivity", "Intent extras: ${intent.extras}")
        
        try {
            setContentView(R.layout.homepage)
            android.util.Log.d("HomeActivity", "setContentView completed")

            recyclerView = findViewById(R.id.itemRecyclerView)
            progressBar = findViewById(R.id.progressBar)
            android.util.Log.d("HomeActivity", "Views initialized")

            android.util.Log.d("HomeActivity", "About to initialize ViewModel")
            setupRecyclerView()
            android.util.Log.d("HomeActivity", "RecyclerView setup completed")
            
            observeViewModel()
            android.util.Log.d("HomeActivity", "ViewModel observation setup completed")
            android.util.Log.d("HomeActivity", "ViewModel initialized: $viewModel")

            // Top Icons Navigation
            findViewById<ImageView>(R.id.btnSavedItems).setOnClickListener {
                startActivity(Intent(this, SavedItemsActivity::class.java))
            }

            findViewById<ImageView>(R.id.btnCart).setOnClickListener {
                startActivity(Intent(this, YourAuctionsActivity::class.java))
            }

            // Search functionality
            setupSearchFunctionality()

            // Bottom Navigation
            findViewById<LinearLayout>(R.id.nav_home).setOnClickListener {
                // Already here
            }

            findViewById<LinearLayout>(R.id.nav_notifications).setOnClickListener {
                startActivity(Intent(this, NotificationsActivity::class.java))
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
            
            // Type filters (All / Auctions / Buy Now)
            setupTypeFilters()
            
            // Setup notification badge
            val notificationBadge = findViewById<TextView>(R.id.notificationBadge)
            NotificationBadgeManager.setupBadge(this, this, notificationsViewModel, notificationBadge)
            
            android.util.Log.d("HomeActivity", "onCreate() completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("HomeActivity", "Error in onCreate()", e)
            Toast.makeText(this, "Error loading home: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        adapter = ItemAdapter()
        recyclerView.adapter = adapter

        // Pass current user ID so "Your Listing" badge shows correctly
        val userId = com.example.mineteh.utils.TokenManager(this).getUserId()
        adapter.setCurrentUserId(userId)
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
        val filterBtn = findViewById<MaterialButton>(R.id.btnCategoryAll)
        val categoriesTitle = findViewById<TextView>(R.id.categoriesTitle)

        val categoryList = listOf(
            "All", "Electronics", "Vehicles", "Property", "Fashion",
            "Home & Garden", "Sports", "Books", "Other"
        )

        filterBtn.setOnClickListener { anchor ->
            val popup = PopupMenu(this, anchor)
            categoryList.forEachIndexed { index, name ->
                popup.menu.add(0, index, index, name)
            }
            popup.setOnMenuItemClickListener { item ->
                val selected = categoryList[item.itemId]
                val category = if (selected == "All") null else selected
                selectedCategory = category
                categoriesTitle.text = if (category == null) "All Listings" else selected
                filterBtn.text = "${selected} ▾"
                progressBar.visibility = View.VISIBLE
                viewModel.fetchListings(
                    category = Categories.getCategoryForApi(category),
                    type = selectedListingType
                )
                true
            }
            popup.show()
        }

        // initialise hidden buttons list so selectCategory() doesn't crash
        categoryButtons = emptyList()
        selectCategory(null, filterBtn)
    }

    private fun selectCategory(category: String?, selectedButton: MaterialButton) {
        selectedCategory = category
        progressBar.visibility = View.VISIBLE
        viewModel.fetchListings(category = Categories.getCategoryForApi(category), type = selectedListingType)
    }

    private fun setupTypeFilters() {
        typeButtons = listOf(
            findViewById(R.id.btnTypeAll),
            findViewById(R.id.btnTypeAuctions),
            findViewById(R.id.btnTypeBuyNow)
        )

        findViewById<MaterialButton>(R.id.btnTypeAll).setOnClickListener { button ->
            selectListingType(null, button as MaterialButton)
        }

        findViewById<MaterialButton>(R.id.btnTypeAuctions).setOnClickListener { button ->
            selectListingType("BID", button as MaterialButton)
        }

        findViewById<MaterialButton>(R.id.btnTypeBuyNow).setOnClickListener { button ->
            selectListingType("FIXED", button as MaterialButton)
        }

        // Default to "All" on launch
        selectListingType(null, findViewById(R.id.btnTypeAll))
    }

    private fun selectListingType(type: String?, selectedButton: MaterialButton) {
        selectedListingType = type

        // Update button styles
        typeButtons.forEach { button ->
            if (button == selectedButton) {
                button.setBackgroundColor(getColor(R.color.purple))
                button.setTextColor(getColor(R.color.white))
                button.strokeWidth = 0
            } else {
                button.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                button.setTextColor(getColor(R.color.purple))
                button.strokeWidth = 4
                button.strokeColor = getColorStateList(R.color.purple)
            }
        }

        // Fetch listings with current category and new type
        progressBar.visibility = View.VISIBLE
        viewModel.fetchListings(category = Categories.getCategoryForApi(selectedCategory), type = selectedListingType)
    }

    private fun setupSearchFunctionality() {
        // Find the search EditText in the header
        val searchEditText = findViewById<android.widget.EditText>(R.id.searchEditText) 
            ?: return // If search field doesn't exist in layout, skip

        searchEditText.setOnClickListener {
            // Open search activity when search field is clicked
            startActivity(Intent(this, SearchActivity::class.java))
        }

        searchEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // Open search activity when search field gains focus
                startActivity(Intent(this, SearchActivity::class.java))
                searchEditText.clearFocus()
            }
        }
    }
}
