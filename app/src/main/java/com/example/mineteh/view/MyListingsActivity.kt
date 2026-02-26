package com.example.mineteh.view

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mineteh.R
import com.example.mineteh.databinding.MyListingsBinding

class MyListingsActivity : AppCompatActivity() {

    private lateinit var binding: MyListingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MyListingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val listings = arrayListOf(
            ListingModel("Seventeen 11th Mini Album [Unsealed]", "Active", R.drawable.auction),
            ListingModel("Newjeans Danielle Official PCs", "Active", R.drawable.purchases),
            ListingModel("Im Nayeon (A Ver.) Album [Unsealed]", "Ended", R.drawable.sell),
            ListingModel("Salvatore Ferragamo Incanto Charms", "Ended", R.drawable.orders),
            ListingModel("Gold Watch", "Ended", R.drawable.bid),
            ListingModel("\"Wake Up or Sleep\" Book", "Ended", R.drawable.hourglass),
            ListingModel("\"Stay Awake, Agatha\" Book", "Ended", R.drawable.me),
            ListingModel("Black Bag", "Ended", R.drawable.cart)
        )

        binding.listingsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.listingsRecyclerView.adapter = ListingsAdapter(listings)
        binding.totalProductsLabel.text = "Total Products: ${listings.size}"
    }

    data class ListingModel(val title: String, val status: String, val imageRes: Int)

    inner class ListingsAdapter(private val list: List<ListingModel>) :
        RecyclerView.Adapter<ListingsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.listingTitle)
            val status: TextView = view.findViewById(R.id.listingStatus)
            val image: ImageView = view.findViewById(R.id.listingImage)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_listing, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.title.text = item.title
            holder.status.text = item.status
            holder.image.setImageResource(item.imageRes)

            if (item.status == "Ended") {
                holder.status.setTextColor(Color.GRAY)
            } else {
                holder.status.setTextColor(Color.parseColor("#4CAF50"))
            }
        }

        override fun getItemCount(): Int = list.size
    }
}