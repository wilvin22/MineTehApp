package com.example.mineteh

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mineteh.databinding.MyOrdersBinding
import com.google.android.material.tabs.TabLayoutMediator

class MyOrdersActivity : AppCompatActivity() {

    private lateinit var binding: MyOrdersBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MyOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupViewPager()
    }

    private fun setupViewPager() {
        val adapter = OrdersPagerAdapter()
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "All"
                1 -> "To Pay"
                2 -> "To Ship"
                3 -> "To Receive"
                else -> "Completed"
            }
        }.attach()
    }

    inner class OrdersPagerAdapter : RecyclerView.Adapter<OrdersPagerAdapter.PagerViewHolder>() {
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
            // Reusing a simplified adapter for demonstration. 
            // In a real app, you'd pass the correct list of orders for each status.
            rv.adapter = OrderItemAdapter()
        }

        override fun getItemCount(): Int = 5

        inner class PagerViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view)
    }

    inner class OrderItemAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_order, parent, false)
            return object : RecyclerView.ViewHolder(view) {}
        }
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            // Populate with dummy data if needed
        }
        override fun getItemCount(): Int = 1
    }
}
