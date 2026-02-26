package com.example.mineteh.view

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mineteh.view.CartAdapter
import com.example.mineteh.view.CheckoutActivity
import com.example.mineteh.databinding.CartBinding
import com.example.mineteh.model.ItemModel

class CartActivity : AppCompatActivity() {

    private lateinit var binding: CartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()

        binding.btnCheckout.setOnClickListener {
            startActivity(Intent(this, CheckoutActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        val cartItems = arrayListOf(
            ItemModel(
                "Pink Lacoste Tote Bag",
                "23 Mabini Street, QC",
                "1,900.00",
                "Quezon City",
                shopName = "Lacoste Official"
            ),
            ItemModel(
                "Hawaiian Sun Sunglasses",
                "11 Mabini St., QC",
                "670.00",
                "Quezon City",
                shopName = "Sun Shop"
            ),
            ItemModel(
                "Glossy Dumbbell Vinyl 5LBS",
                "88 P. Burgos St., Iloilo",
                "599.00",
                "Iloilo City",
                shopName = "Fitness Gear"
            ),
            ItemModel(
                "Venus High Heels Silver",
                "100 Quirino Ave., Manila",
                "1,169.00",
                "Manila",
                shopName = "Venus Footwear"
            )
        )

        binding.cartRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.cartRecyclerView.adapter = CartAdapter(cartItems)
    }
}