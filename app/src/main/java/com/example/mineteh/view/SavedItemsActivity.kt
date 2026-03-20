package com.example.mineteh.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
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
    private lateinit var adapter: ItemAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.saved_items)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        recyclerView = findViewById(R.id.savedItemsRecyclerView)
        progressBar = findViewById(R.id.loadingProgress)
        emptyState = findViewById(R.id.emptyState)

        adapter = ItemAdapter(
            onFavoriteToggle = { listing ->
                // Heart tap = remove from saved
                viewModel.toggleFavorite(listing.id)
            }
        )
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter

        observeViewModel()
        viewModel.loadFavorites()
    }

    private fun observeViewModel() {
        viewModel.favorites.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    emptyState.visibility = View.GONE
                }
                is Resource.Success -> {
                    progressBar.visibility = View.GONE
                    val items = resource.data ?: emptyList()
                    if (items.isEmpty()) {
                        recyclerView.visibility = View.GONE
                        emptyState.visibility = View.VISIBLE
                    } else {
                        recyclerView.visibility = View.VISIBLE
                        emptyState.visibility = View.GONE
                        adapter.updateList(items)
                    }
                }
                is Resource.Error -> {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
                null -> {}
            }
        }

        viewModel.toggleResult.observe(this) { resource ->
            if (resource is Resource.Success || resource is Resource.Error) {
                if (resource is Resource.Error) {
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
                viewModel.resetToggleResult()
                viewModel.loadFavorites() // reload after toggle
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadFavorites()
    }
}
