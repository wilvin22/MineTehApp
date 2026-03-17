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
                startActivity(Intent(this, CartActivity::class.java))
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
        // Initialize category buttons
        categoryButtons = listOf(
            findViewById(R.id.btnCategoryAll),
            findViewById(R.id.btnCategoryElectronics),
            findViewById(R.id.btnCategoryVehicles),
            findViewById(R.id.btnCategoryProperty),
            findViewById(R.id.btnCategoryFashion),
            findViewById(R.id.btnCategoryHomeGarden),
            findViewById(R.id.btnCategorySports),
            findViewById(R.id.btnCategoryBooks),
            findViewById(R.id.btnCategoryOther)
        )

        // Set up click listeners for each category button with visual feedback
        findViewById<MaterialButton>(R.id.btnCategoryAll).setOnClickListener { button ->
            selectCategory(null, button as MaterialButton)
        }
        
        findViewById<MaterialButton>(R.id.btnCategoryElectronics).setOnClickListener { button ->
            selectCategory("Electronics", button as MaterialButton)
        }
        
        findViewById<MaterialButton>(R.id.btnCategoryVehicles).setOnClickListener { button ->
            selectCategory("Vehicles", button as MaterialButton)
        }
        
        findViewById<MaterialButton>(R.id.btnCategoryProperty).setOnClickListener { button ->
            selectCategory("Property", button as MaterialButton)
        }
        
        findViewById<MaterialButton>(R.id.btnCategoryFashion).setOnClickListener { button ->
            selectCategory("Fashion", button as MaterialButton)
        }
        
        findViewById<MaterialButton>(R.id.btnCategoryHomeGarden).setOnClickListener { button ->
            selectCategory("Home & Garden", button as MaterialButton)
        }
        
        findViewById<MaterialButton>(R.id.btnCategorySports).setOnClickListener { button ->
            selectCategory("Sports", button as MaterialButton)
        }
        
        findViewById<MaterialButton>(R.id.btnCategoryBooks).setOnClickListener { button ->
            selectCategory("Books", button as MaterialButton)
        }
        
        findViewById<MaterialButton>(R.id.btnCategoryOther).setOnClickListener { button ->
            selectCategory("Other", button as MaterialButton)
        }
        
        // Set "All" as initially selected
        selectCategory(null, findViewById(R.id.btnCategoryAll))
    }

    private fun selectCategory(category: String?, selectedButton: MaterialButton) {
        selectedCategory = category
        
        // Update button styles
        categoryButtons.forEach { button ->
            if (button == selectedButton) {
                // Selected style - filled button
                button.setBackgroundColor(getColor(R.color.purple))
                button.setTextColor(getColor(R.color.white))
                button.strokeWidth = 0
            } else {
                // Unselected style - outlined button
                button.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                button.setTextColor(getColor(R.color.purple))
                button.strokeWidth = 4 // 2dp in pixels
                button.strokeColor = getColorStateList(R.color.purple)
            }
        }
        
        // Show loading while fetching
        progressBar.visibility = View.VISIBLE
        
        // Fetch listings for selected category
        viewModel.fetchListings(category = Categories.getCategoryForApi(category))
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
