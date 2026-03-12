package com.example.mineteh.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mineteh.R
import com.example.mineteh.databinding.YourAuctionsBinding
import com.example.mineteh.model.BidsUiState
import com.example.mineteh.viewmodel.BidsViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayoutMediator

class YourAuctionsActivity : AppCompatActivity() {

    private lateinit var binding: YourAuctionsBinding
    private lateinit var viewModel: BidsViewModel
    
    private lateinit var liveAdapter: LiveAuctionAdapter
    private lateinit var wonAdapter: WonAuctionAdapter
    private lateinit var lostAdapter: LostAuctionAdapter
    
    private val recyclerViews = mutableListOf<RecyclerView>()
    private var progressBar: ProgressBar? = null
    private var errorView: View? = null

    companion object {
        private const val TAG = "YourAuctionsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = YourAuctionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[BidsViewModel::class.java]
        
        // Initialize adapters
        liveAdapter = LiveAuctionAdapter { bidWithListing ->
            val intent = Intent(this, BidDetailActivity::class.java)
            intent.putExtra("listing_id", bidWithListing.listing.id)
            startActivity(intent)
        }
        
        wonAdapter = WonAuctionAdapter { bidWithListing ->
            val intent = Intent(this, BidDetailActivity::class.java)
            intent.putExtra("listing_id", bidWithListing.listing.id)
            startActivity(intent)
        }
        
        lostAdapter = LostAuctionAdapter { bidWithListing ->
            val intent = Intent(this, BidDetailActivity::class.java)
            intent.putExtra("listing_id", bidWithListing.listing.id)
            startActivity(intent)
        }

        setupViewPager()
        setupObservers()
        
        // Fetch bids
        viewModel.fetchBids()
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.startAutoRefresh()
    }
    
    override fun onPause() {
        super.onPause()
        viewModel.stopAutoRefresh()
    }

    private fun setupViewPager() {
        val adapter = AuctionsPagerAdapter()
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Live Auction"
                1 -> "Won"
                else -> "Lost"
            }
        }.attach()
    }
    
    private fun setupObservers() {
        viewModel.bidsState.observe(this) { state ->
            when (state) {
                is BidsUiState.Loading -> {
                    Log.d(TAG, "Loading state")
                    showLoading()
                }
                is BidsUiState.Success -> {
                    Log.d(TAG, "Success state: ${state.liveBids.size} live, ${state.wonBids.size} won, ${state.lostBids.size} lost")
                    hideLoading()
                    liveAdapter.submitList(state.liveBids)
                    wonAdapter.submitList(state.wonBids)
                    lostAdapter.submitList(state.lostBids)
                }
                is BidsUiState.Error -> {
                    Log.e(TAG, "Error state: ${state.message}")
                    hideLoading()
                    showError(state.message)
                }
            }
        }
    }
    
    private fun showLoading() {
        progressBar?.visibility = View.VISIBLE
        recyclerViews.forEach { it.visibility = View.GONE }
        errorView?.visibility = View.GONE
    }
    
    private fun hideLoading() {
        progressBar?.visibility = View.GONE
        recyclerViews.forEach { it.visibility = View.VISIBLE }
        errorView?.visibility = View.GONE
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        recyclerViews.forEach { it.visibility = View.VISIBLE }
    }

    inner class AuctionsPagerAdapter : RecyclerView.Adapter<AuctionsPagerAdapter.PagerViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerViewHolder {
            val recyclerView = RecyclerView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                layoutManager = LinearLayoutManager(context)
                setPadding(0, 16, 0, 16)
                clipToPadding = false
            }
            recyclerViews.add(recyclerView)
            return PagerViewHolder(recyclerView)
        }

        override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {
            val rv = holder.itemView as RecyclerView
            when (position) {
                0 -> rv.adapter = liveAdapter
                1 -> rv.adapter = wonAdapter
                2 -> rv.adapter = lostAdapter
            }
        }

        override fun getItemCount(): Int = 3

        inner class PagerViewHolder(view: View) : RecyclerView.ViewHolder(view)
    }
}