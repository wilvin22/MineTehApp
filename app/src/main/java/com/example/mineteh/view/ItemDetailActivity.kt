package com.example.mineteh.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mineteh.R
import com.example.mineteh.databinding.ItemDetailBinding
import com.example.mineteh.model.ItemModel

class ItemDetailActivity : AppCompatActivity() {

    private lateinit var binding: ItemDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ItemDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Get Item Data using Serializable
        val item = intent.getSerializableExtra("item") as? ItemModel

        // Populate Views
        item?.let { currentItem ->
            binding.detailItemName.text = currentItem.name
            binding.detailItemDescription.text = currentItem.description
            binding.detailItemPrice.text = "₱ ${currentItem.price}"
            binding.detailItemLocation.text = currentItem.location

            // Set the correct photo from the clicked item
            binding.detailItemImage.setImageResource(currentItem.imageRes)

            // Handle Heart Icon State
            updateHeartIcon(currentItem.isLiked)

            binding.detailHeart.setOnClickListener {
                currentItem.isLiked = !currentItem.isLiked
                updateHeartIcon(currentItem.isLiked)
            }

            // Handle Add to Cart
            binding.btnAddToCart.setOnClickListener {
                Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show()
            }

            // Handle Buy Now (Checkout)
            binding.btnBuyNow.setOnClickListener {
                val intent = Intent(this, CheckoutActivity::class.java)
                intent.putExtra("item", currentItem)
                startActivity(intent)
            }
        }
    }

    private fun updateHeartIcon(isLiked: Boolean) {
        if (isLiked) {
            binding.detailHeart.setImageResource(R.drawable.heart_red)
        } else {
            binding.detailHeart.setImageResource(R.drawable.heart)
        }
    }
}
