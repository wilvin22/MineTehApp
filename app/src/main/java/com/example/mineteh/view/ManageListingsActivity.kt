package com.example.mineteh.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mineteh.R
import com.example.mineteh.models.Listing
import com.example.mineteh.utils.Resource
import com.example.mineteh.viewmodel.MyListingsViewModel

class ManageListingsActivity : AppCompatActivity() {

    private val viewModel: MyListingsViewModel by viewModels()
    private lateinit var adapter: MyListingsAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var loadingProgress: View
    private lateinit var emptyText: TextView

    private var allListings = listOf<Listing>()
    private var currentFilter = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.my_listings)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        recyclerView = findViewById(R.id.listingsRecyclerView)
        loadingProgress = findViewById(R.id.loadingProgress)
        emptyText = findViewById(R.id.emptyListingsText)

        setupAdapter()
        setupFilterTabs()
        setupObservers()
        viewModel.loadUserListings()
    }

    private fun setupAdapter() {
        adapter = MyListingsAdapter(
            onViewClick = { listing ->
                startActivity(Intent(this, ItemDetailActivity::class.java).apply {
                    putExtra("listing_id", listing.id)
                })
            },
            onEditClick = { listing ->
                Toast.makeText(this, "Edit coming soon", Toast.LENGTH_SHORT).show()
            },
            onStatusChange = { listing, newStatus ->
                viewModel.updateListingStatus(listing.id, newStatus)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupFilterTabs() {
        val tabs = mapOf(
            R.id.tabAll      to "all",
            R.id.tabActive   to "active",
            R.id.tabSold     to "sold",
            R.id.tabInactive to "inactive"
        )
        tabs.forEach { (id, filter) ->
            findViewById<TextView>(id).setOnClickListener {
                currentFilter = filter
                updateTabStyles(id)
                applyFilter()
            }
        }
    }

    private fun updateTabStyles(selectedId: Int) {
        val tabIds = listOf(R.id.tabAll, R.id.tabActive, R.id.tabSold, R.id.tabInactive)
        tabIds.forEach { id ->
            val tab = findViewById<TextView>(id)
            if (id == selectedId) {
                tab.setBackgroundResource(R.drawable.tab_selected_bg)
                tab.setTextColor(android.graphics.Color.WHITE)
            } else {
                tab.setBackgroundResource(R.drawable.tab_unselected_bg)
                tab.setTextColor(android.graphics.Color.parseColor("#888888"))
            }
        }
    }

    private fun applyFilter() {
        val filtered = if (currentFilter == "all") allListings
        else allListings.filter { it.status.equals(currentFilter, ignoreCase = true) }

        // Update tab counts
        updateTabCounts()

        if (filtered.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyText.visibility = View.VISIBLE
            emptyText.text = "No ${if (currentFilter == "all") "" else "$currentFilter "}listings"
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyText.visibility = View.GONE
            adapter.submitList(filtered)
        }
    }

    private fun updateTabCounts() {
        val total    = allListings.size
        val active   = allListings.count { it.status.equals("active", ignoreCase = true) }
        val sold     = allListings.count { it.status.equals("sold", ignoreCase = true) }
        val inactive = allListings.count { it.status.equals("inactive", ignoreCase = true) }

        findViewById<TextView>(R.id.tabAll).text     = "All ($total)"
        findViewById<TextView>(R.id.tabActive).text  = "Active ($active)"
        findViewById<TextView>(R.id.tabSold).text    = "Sold ($sold)"
        findViewById<TextView>(R.id.tabInactive).text = "Inactive ($inactive)"
    }

    private fun setupObservers() {
        viewModel.listings.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    loadingProgress.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    emptyText.visibility = View.GONE
                }
                is Resource.Success -> {
                    loadingProgress.visibility = View.GONE
                    allListings = resource.data ?: emptyList()
                    applyFilter()
                }
                is Resource.Error -> {
                    loadingProgress.visibility = View.GONE
                    emptyText.visibility = View.VISIBLE
                    emptyText.text = resource.message ?: "Failed to load listings"
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
                null -> {}
            }
        }

        viewModel.statusUpdateResult.observe(this) { resource ->
            when (resource) {
                is Resource.Success -> {
                    Toast.makeText(this, "Status updated", Toast.LENGTH_SHORT).show()
                    viewModel.resetStatusUpdateResult()
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message ?: "Failed to update status", Toast.LENGTH_SHORT).show()
                    viewModel.resetStatusUpdateResult()
                    viewModel.loadUserListings() // reload to revert spinner
                }
                else -> {}
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadUserListings()
    }
}
