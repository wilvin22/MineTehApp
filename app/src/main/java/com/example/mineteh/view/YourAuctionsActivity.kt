package com.example.mineteh.view

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mineteh.R
import com.example.mineteh.databinding.YourAuctionsBinding
import com.google.android.material.tabs.TabLayoutMediator

class YourAuctionsActivity : AppCompatActivity() {

    private lateinit var binding: YourAuctionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = YourAuctionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupViewPager()
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
            return PagerViewHolder(recyclerView)
        }

        override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {
            val rv = holder.itemView as RecyclerView
            when (position) {
                0 -> rv.adapter = AuctionLiveAdapter()
                1 -> rv.adapter = AuctionWonAdapter()
                2 -> rv.adapter = AuctionLostAdapter()
            }
        }

        override fun getItemCount(): Int = 3

        inner class PagerViewHolder(view: View) : RecyclerView.ViewHolder(view)
    }

    // Simplified adapters for demonstration
    inner class AuctionLiveAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_auction_live, parent, false)
            return object : RecyclerView.ViewHolder(view) {}
        }
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
        override fun getItemCount(): Int = 1
    }

    inner class AuctionWonAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_auction_won, parent, false)
            return object : RecyclerView.ViewHolder(view) {}
        }
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
        override fun getItemCount(): Int = 1
    }

    inner class AuctionLostAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_auction_lost, parent, false)
            return object : RecyclerView.ViewHolder(view) {}
        }
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
        override fun getItemCount(): Int = 1
    }
}