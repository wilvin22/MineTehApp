package com.example.mineteh.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mineteh.R
import com.example.mineteh.utils.Resource
import com.example.mineteh.viewmodel.FavoritesViewModel

class SavedItemsActivity : AppCompatActivity() {

    private val viewModel: FavoritesViewModel by viewModels()
    private lateinit var adapter: ListingsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.saved_items)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()

        viewModel.loadFavorites()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.savedItemsRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        
        adapter = ListingsAdapter { listing ->
            val intent = if (listing.listingType == "BID") {
                Intent(this, BidDetailActivity::class.java)
            } else {
                Intent(this, ItemDetailActivity::class.java)
            }
            intent.putExtra("listing_id", listing.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.favorites.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    // Optionally show a progress bar if available in layout
                }
                is Resource.Success -> {
                    adapter.submitList(resource.data)
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadFavorites()
    }
}
