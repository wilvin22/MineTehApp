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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.mineteh.R
import com.example.mineteh.utils.Categories
import com.example.mineteh.utils.Resource
import com.example.mineteh.viewmodel.SearchViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class SearchActivity : AppCompatActivity(), FilterBottomSheetFragment.FilterListener {

    private val viewModel: SearchViewModel by viewModels()
    private lateinit var adapter: ItemAdapter
    private var currentFilters = FilterBottomSheetFragment.FilterOptions()

    private lateinit var backButton: ImageView
    private lateinit var searchInput: TextInputEditText
    private lateinit var searchButton: MaterialButton
    private lateinit var filterButton: MaterialButton
    private lateinit var btnClearSearch: ImageView
    private lateinit var categorySpinner: AutoCompleteTextView
    private lateinit var typeSpinner: AutoCompleteTextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var loadingLayout: LinearLayout
    private lateinit var initialStateLayout: LinearLayout
    private lateinit var resultsCountBar: LinearLayout
    private lateinit var resultsCountText: TextView
    private lateinit var activeFilterBar: LinearLayout
    private lateinit var activeFilterText: TextView
    private lateinit var btnClearFilter: TextView

    private lateinit var typeButtons: List<MaterialButton>
    private var selectedType: String? = null
    private var selectedCategory: String? = null
    private var hasSearched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search)

        initViews()
        setupRecyclerView()
        setupSpinners()
        setupTypeFilters()
        setupListeners()
        observeSearchResults()

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
        btnClearSearch = findViewById(R.id.btnClearSearch)
        categorySpinner = findViewById(R.id.categorySpinner)
        typeSpinner = findViewById(R.id.typeSpinner)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        loadingLayout = findViewById(R.id.loadingLayout)
        initialStateLayout = findViewById(R.id.initialStateLayout)
        resultsCountBar = findViewById(R.id.resultsCountBar)
        resultsCountText = findViewById(R.id.resultsCountText)
        activeFilterBar = findViewById(R.id.activeFilterBar)
        activeFilterText = findViewById(R.id.activeFilterText)
        btnClearFilter = findViewById(R.id.btnClearFilter)
    }

    private fun setupRecyclerView() {
        adapter = ItemAdapter()
        searchResultsRecyclerView.layoutManager = GridLayoutManager(this, 2)
        searchResultsRecyclerView.adapter = adapter
    }

    private fun setupSpinners() {
        categorySpinner.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, Categories.CATEGORIES_WITH_ALL)
        )
        categorySpinner.setText("All", false)

        val types = arrayOf("All Types", "Fixed Price", "Auction")
        typeSpinner.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, types)
        )
        typeSpinner.setText("All Types", false)
    }

    private fun setupTypeFilters() {
        typeButtons = listOf(
            findViewById(R.id.btnTypeAll),
            findViewById(R.id.btnTypeFixed),
            findViewById(R.id.btnTypeAuction)
        )

        findViewById<MaterialButton>(R.id.btnTypeAll).setOnClickListener {
            selectType(null, it as MaterialButton)
        }
        findViewById<MaterialButton>(R.id.btnTypeFixed).setOnClickListener {
            selectType("FIXED", it as MaterialButton)
        }
        findViewById<MaterialButton>(R.id.btnTypeAuction).setOnClickListener {
            selectType("BID", it as MaterialButton)
        }
    }

    private fun selectType(type: String?, selected: MaterialButton) {
        selectedType = type
        typeButtons.forEach { btn ->
            if (btn == selected) {
                btn.setBackgroundColor(getColor(R.color.purple))
                btn.setTextColor(getColor(R.color.white))
                btn.strokeWidth = 0
            } else {
                btn.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                btn.setTextColor(getColor(R.color.purple))
                btn.strokeWidth = 4
                btn.strokeColor = getColorStateList(R.color.purple)
            }
        }
        if (hasSearched) performSearch()
    }

    private fun setupListeners() {
        backButton.setOnClickListener { finish() }

        searchButton.setOnClickListener { performSearch() }

        searchInput.setOnEditorActionListener { _, _, _ ->
            performSearch()
            true
        }

        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btnClearSearch.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        btnClearSearch.setOnClickListener {
            searchInput.setText("")
            selectedCategory = null
            updateActiveFilterBar()
            showInitialState()
            hasSearched = false
        }

        filterButton.setOnClickListener { showFilterBottomSheet() }

        btnClearFilter.setOnClickListener {
            currentFilters = FilterBottomSheetFragment.FilterOptions()
            selectedCategory = null
            updateActiveFilterBar()
            if (hasSearched) performSearch()
        }

        swipeRefreshLayout.setOnRefreshListener { performSearch() }
    }

    private fun showFilterBottomSheet() {
        val filterSheet = FilterBottomSheetFragment.newInstance(currentFilters, this)
        filterSheet.show(supportFragmentManager, "FilterBottomSheet")
    }

    override fun onFiltersApplied(filters: FilterBottomSheetFragment.FilterOptions) {
        currentFilters = filters
        selectedCategory = filters.category
        updateActiveFilterBar()
        performSearch()
    }

    private fun updateActiveFilterBar() {
        val hasCategory = !selectedCategory.isNullOrEmpty()
        val hasPrice = currentFilters.minPrice != null || currentFilters.maxPrice != null
        if (hasCategory || hasPrice) {
            activeFilterBar.visibility = View.VISIBLE
            val parts = mutableListOf<String>()
            if (hasCategory) parts.add("Category: $selectedCategory")
            if (hasPrice) {
                val min = currentFilters.minPrice?.let { "₱$it" } ?: "Any"
                val max = currentFilters.maxPrice?.let { "₱$it" } ?: "Any"
                parts.add("Price: $min – $max")
            }
            activeFilterText.text = parts.joinToString("  •  ")
        } else {
            activeFilterBar.visibility = View.GONE
        }
    }

    private fun performSearch() {
        hasSearched = true
        val query = searchInput.text.toString().trim()
        viewModel.searchListings(
            query = query.takeIf { it.isNotEmpty() },
            category = Categories.getCategoryForApi(selectedCategory),
            type = selectedType,
            minPrice = currentFilters.minPrice,
            maxPrice = currentFilters.maxPrice
        )
    }

    private fun observeSearchResults() {
        viewModel.searchResults.observe(this) { resource ->
            swipeRefreshLayout.isRefreshing = false
            when (resource) {
                is Resource.Loading -> showLoading()
                is Resource.Success -> {
                    val listings = resource.data ?: emptyList()
                    if (listings.isEmpty()) showEmptyState()
                    else showResults(listings)
                }
                is Resource.Error -> showEmptyState()
                null -> if (!hasSearched) showInitialState()
            }
        }
    }

    private fun showLoading() {
        loadingLayout.visibility = View.VISIBLE
        searchResultsRecyclerView.visibility = View.GONE
        emptyStateLayout.visibility = View.GONE
        initialStateLayout.visibility = View.GONE
        resultsCountBar.visibility = View.GONE
    }

    private fun showResults(listings: List<com.example.mineteh.models.Listing>) {
        loadingLayout.visibility = View.GONE
        searchResultsRecyclerView.visibility = View.VISIBLE
        emptyStateLayout.visibility = View.GONE
        initialStateLayout.visibility = View.GONE
        resultsCountBar.visibility = View.VISIBLE
        resultsCountText.text = "${listings.size} result${if (listings.size != 1) "s" else ""}"
        adapter.updateList(listings)
    }

    private fun showEmptyState() {
        loadingLayout.visibility = View.GONE
        searchResultsRecyclerView.visibility = View.GONE
        emptyStateLayout.visibility = View.VISIBLE
        initialStateLayout.visibility = View.GONE
        resultsCountBar.visibility = View.GONE
    }

    private fun showInitialState() {
        loadingLayout.visibility = View.GONE
        searchResultsRecyclerView.visibility = View.GONE
        emptyStateLayout.visibility = View.GONE
        initialStateLayout.visibility = View.VISIBLE
        resultsCountBar.visibility = View.GONE
    }
}
