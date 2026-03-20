package com.example.mineteh.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.mineteh.R
import com.example.mineteh.models.Listing
import com.example.mineteh.utils.Categories
import com.example.mineteh.utils.Resource
import com.example.mineteh.viewmodel.SearchViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class SearchActivity : AppCompatActivity(), FilterBottomSheetFragment.FilterListener {

    private val viewModel: SearchViewModel by viewModels()
    private lateinit var adapter: ListingsAdapter
    private var currentFilters = FilterBottomSheetFragment.FilterOptions()

    // UI Components
    private lateinit var backButton: ImageView
    private lateinit var searchInput: TextInputEditText
    private lateinit var searchButton: MaterialButton
    private lateinit var filterButton: MaterialButton
    private lateinit var categorySpinner: AutoCompleteTextView
    private lateinit var typeSpinner: AutoCompleteTextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var loadingLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search)

        initViews()
        setupRecyclerView()
        setupSpinners()
        setupListeners()
        observeSearchResults()

        // Handle search query from intent
        val query = intent.getStringExtra("query")
        if (!query.isNullOrEmpty()) {
            searchInput.setText(query)
            performSearch()
        }
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        searchInput = findViewById(R.id.searchInput)
        searchButton = findViewById(R.id.searchButton)
        filterButton = findViewById(R.id.filterButton)
        categorySpinner = findViewById(R.id.categorySpinner)
        typeSpinner = findViewById(R.id.typeSpinner)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        loadingLayout = findViewById(R.id.loadingLayout)
    }

    private fun setupRecyclerView() {
        adapter = ListingsAdapter { listing ->
            openListingDetail(listing)
        }
        
        searchResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = this@SearchActivity.adapter
        }
    }

    private fun setupSpinners() {
        // Category spinner
        categorySpinner.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, Categories.CATEGORIES_WITH_ALL)
        )
        categorySpinner.setText("All", false)

        // Type spinner
        val types = arrayOf("All Types", "Fixed Price", "Auction")
        typeSpinner.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, types)
        )
        typeSpinner.setText("All Types", false)
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }

        searchButton.setOnClickListener {
            performSearch()
        }

        searchInput.setOnEditorActionListener { _, _, _ ->
            performSearch()
            true
        }

        swipeRefreshLayout.setOnRefreshListener {
            performSearch()
        }

        filterButton.setOnClickListener {
            showFilterBottomSheet()
        }
    }

    private fun showFilterBottomSheet() {
        val filterSheet = FilterBottomSheetFragment.newInstance(currentFilters, this)
        filterSheet.show(supportFragmentManager, "FilterBottomSheet")
    }

    override fun onFiltersApplied(filters: FilterBottomSheetFragment.FilterOptions) {
        currentFilters = filters
        performSearchWithFilters()
    }

    private fun performSearchWithFilters() {
        val query = searchInput.text.toString().trim()
        
        viewModel.searchListings(
            query = query.takeIf { it.isNotEmpty() },
            category = currentFilters.category,
            type = currentFilters.listingType,
            minPrice = currentFilters.minPrice,
            maxPrice = currentFilters.maxPrice
        )
    }

    private fun performSearch() {
        val query = searchInput.text.toString().trim()
        val category = categorySpinner.text.toString().let {
            Categories.getCategoryForApi(it)
        }
        val type = typeSpinner.text.toString().let {
            when (it) {
                "Fixed Price" -> "FIXED"
                "Auction" -> "BID"
                else -> null
            }
        }

        viewModel.searchListings(query, category, type)
    }

    private fun observeSearchResults() {
        viewModel.searchResults.observe(this) { resource ->
            swipeRefreshLayout.isRefreshing = false
            
            when (resource) {
                is Resource.Loading -> {
                    showLoading()
                }
                is Resource.Success -> {
                    val listings = resource.data ?: emptyList()
                    if (listings.isEmpty()) {
                        showEmptyState()
                    } else {
                        showResults(listings)
                    }
                }
                is Resource.Error -> {
                    showEmptyState()
                    // You could show a toast or snackbar with the error message
                }
                null -> {
                    showEmptyState()
                }
            }
        }
    }

    private fun showLoading() {
        loadingLayout.visibility = View.VISIBLE
        searchResultsRecyclerView.visibility = View.GONE
        emptyStateLayout.visibility = View.GONE
    }

    private fun showResults(listings: List<Listing>) {
        loadingLayout.visibility = View.GONE
        searchResultsRecyclerView.visibility = View.VISIBLE
        emptyStateLayout.visibility = View.GONE
        adapter.submitList(listings)
    }

    private fun showEmptyState() {
        loadingLayout.visibility = View.GONE
        searchResultsRecyclerView.visibility = View.GONE
        emptyStateLayout.visibility = View.VISIBLE
    }

    private fun openListingDetail(listing: Listing) {
        val intent = if (listing.listingType == "BID") {
            Intent(this, BidDetailActivity::class.java)
        } else {
            Intent(this, ItemDetailActivity::class.java)
        }
        intent.putExtra("listing_id", listing.id)
        startActivity(intent)
    }
}