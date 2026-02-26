package com.example.mineteh.view

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mineteh.view.InboxActivity
import com.example.mineteh.view.ItemAdapter
import com.example.mineteh.view.ProfileActivity
import com.example.mineteh.R
import com.example.mineteh.view.SavedItemsActivity
import com.example.mineteh.view.SellActivity
import com.example.mineteh.model.ItemModel

class HomeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ItemAdapter
    private lateinit var itemList: ArrayList<ItemModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.homepage)

        recyclerView = findViewById(R.id.itemRecyclerView)

        setupRecyclerView()
        loadDummyData()

        // Top Icons Navigation
        findViewById<ImageView>(R.id.btnSavedItems).setOnClickListener {
            startActivity(Intent(this, SavedItemsActivity::class.java))
        }

        findViewById<ImageView>(R.id.btnCart).setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        // Bottom Navigation
        val navBid = findViewById<LinearLayout>(R.id.nav_bid)
        val navSell = findViewById<LinearLayout>(R.id.nav_sell)
        val navInbox = findViewById<LinearLayout>(R.id.nav_inbox)
        val navMe = findViewById<LinearLayout>(R.id.nav_profile)

        navBid.setOnClickListener {
            startActivity(Intent(this, BidActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        navSell.setOnClickListener {
            startActivity(Intent(this, SellActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        navInbox.setOnClickListener {
            startActivity(Intent(this, InboxActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        navMe.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        adapter = ItemAdapter(ArrayList())
        recyclerView.adapter = adapter
    }

    private fun loadDummyData() {
        itemList = arrayListOf(
            ItemModel(
                "Item 1",
                "Brief description",
                "350.00",
                "Manila",
                imageRes = R.drawable.dummyphoto
            ),
            ItemModel(
                "Item 2",
                "Brief description",
                "500.00",
                "Dagupan",
                imageRes = R.drawable.dummyphoto2
            ),
            ItemModel(
                "Item 3",
                "Brief description",
                "750.00",
                "Baguio",
                imageRes = R.drawable.dummyphoto3
            ),
            ItemModel(
                "Item 4",
                "Brief description",
                "1200.00",
                "La Union",
                imageRes = R.drawable.dummyphoto4
            ),
            ItemModel(
                "Item 5",
                "Brief description",
                "899.00",
                "Aguilar",
                imageRes = R.drawable.dummyphoto5
            ),
            ItemModel(
                "Item 6",
                "Brief description",
                "450.00",
                "Lingayen",
                imageRes = R.drawable.dummyphoto6
            )
        )

        adapter.updateList(itemList)
    }
}