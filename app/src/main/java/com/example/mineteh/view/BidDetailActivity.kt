package com.example.mineteh.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mineteh.databinding.BidItemBinding
import com.example.mineteh.model.ItemModel

class BidDetailActivity : AppCompatActivity() {

    private lateinit var binding: BidItemBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BidItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.title = ""

        // Handle back button click on the toolbar
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Get Item Data
        val item = intent.getSerializableExtra("item") as? ItemModel

        // Populate Views
        item?.let {
            binding.txtItemName.text = it.name
            binding.txtLocation.text = it.location
            binding.valStartingPrice.text = "₱ ${it.price}"

            // Set the same photo from the clicked item
            binding.itemImage.setImageResource(it.imageRes)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}