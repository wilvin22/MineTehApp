package com.example.mineteh.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mineteh.databinding.MyListingsBinding
import com.example.mineteh.utils.Resource
import com.example.mineteh.viewmodel.MyListingsViewModel

class MyListingsActivity : AppCompatActivity() {

    private lateinit var binding: MyListingsBinding
    private val viewModel: MyListingsViewModel by viewModels()
    private lateinit var adapter: MyListingsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MyListingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Your Listings"
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        setupObservers()
        
        // Load user's listings
        viewModel.loadUserListings()
    }

    private fun setupRecyclerView() {
        adapter = MyListingsAdapter(
            onViewClick = { listing ->
                val intent = Intent(this, ItemDetailActivity::class.java).apply {
                    putExtra("listing_id", listing.id)
                }
                startActivity(intent)
            },
            onEditClick = { listing ->
                Toast.makeText(this, "Edit feature coming soon", Toast.LENGTH_SHORT).show()
            },
            onToggleStatusClick = { listing ->
                val isActive = listing.status.equals("active", ignoreCase = true)
                if (isActive) {
                    showDisableDialog(listing.id, listing.title)
                } else {
                    showEnableDialog(listing.id, listing.title)
                }
            },
            onDeleteClick = { listing ->
                showDeleteDialog(listing.id, listing.title)
            }
        )
        
        binding.listingsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.listingsRecyclerView.adapter = adapter
    }
    
    private fun setupObservers() {
        viewModel.listings.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.loadingProgress.visibility = View.VISIBLE
                    binding.listingsRecyclerView.visibility = View.GONE
                    binding.emptyListingsText.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.loadingProgress.visibility = View.GONE
                    val listings = resource.data ?: emptyList()
                    
                    if (listings.isEmpty()) {
                        binding.listingsRecyclerView.visibility = View.GONE
                        binding.emptyListingsText.visibility = View.VISIBLE
                        binding.totalProductsLabel.text = "Total Products: 0"
                    } else {
                        binding.listingsRecyclerView.visibility = View.VISIBLE
                        binding.emptyListingsText.visibility = View.GONE
                        binding.totalProductsLabel.text = "Total Products: ${listings.size}"
                        adapter.submitList(listings)
                    }
                }
                is Resource.Error -> {
                    binding.loadingProgress.visibility = View.GONE
                    binding.listingsRecyclerView.visibility = View.GONE
                    binding.emptyListingsText.visibility = View.VISIBLE
                    binding.emptyListingsText.text = resource.message ?: "Failed to load listings"
                    Toast.makeText(this, resource.message ?: "Failed to load listings", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        viewModel.deleteResult.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    // Show loading if needed
                }
                is Resource.Success -> {
                    Toast.makeText(this, "Listing deleted successfully", Toast.LENGTH_SHORT).show()
                    viewModel.resetDeleteResult()
                    viewModel.loadUserListings() // Reload listings
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message ?: "Failed to delete listing", Toast.LENGTH_SHORT).show()
                    viewModel.resetDeleteResult()
                }
                null -> {}
            }
        }
        
        viewModel.statusUpdateResult.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    // Show loading if needed
                }
                is Resource.Success -> {
                    Toast.makeText(this, "Listing status updated", Toast.LENGTH_SHORT).show()
                    viewModel.resetStatusUpdateResult()
                    viewModel.loadUserListings() // Reload listings
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message ?: "Failed to update status", Toast.LENGTH_SHORT).show()
                    viewModel.resetStatusUpdateResult()
                }
                null -> {}
            }
        }
    }
    
    private fun showDisableDialog(listingId: Int, title: String) {
        AlertDialog.Builder(this)
            .setTitle("Disable Listing")
            .setMessage("Disable \"$title\"? It will be hidden from buyers.")
            .setPositiveButton("Disable") { _, _ ->
                viewModel.updateListingStatus(listingId, "inactive")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showEnableDialog(listingId: Int, title: String) {
        AlertDialog.Builder(this)
            .setTitle("Enable Listing")
            .setMessage("Enable \"$title\"? It will be visible to buyers again.")
            .setPositiveButton("Enable") { _, _ ->
                viewModel.updateListingStatus(listingId, "active")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showDeleteDialog(listingId: Int, title: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Listing")
            .setMessage("Delete \"$title\"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteListing(listingId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh listings when returning
        viewModel.loadUserListings()
    }
}