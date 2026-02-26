package com.example.mineteh.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mineteh.R
import com.example.mineteh.model.ItemModel

class SavedItemsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.saved_items)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val recyclerView = findViewById<RecyclerView>(R.id.savedItemsRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        // Dummy data for saved items
        val favItems = arrayListOf(
            ItemModel("Pink Lacoste Bag", "Large concept tote", "1,900.00", "Quezon City", true),
            ItemModel("iPhone 15 Pro", "Brand new sealed", "27,000.00", "Manila", true)
        )

        // Note: For now, we reuse ItemAdapter. You might want to create a specific FavAdapter later.
        val adapter = ItemAdapter(favItems)
        recyclerView.adapter = adapter
    }
}