package com.example.mineteh.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mineteh.databinding.CheckoutBinding

class CheckoutActivity : AppCompatActivity() {

    private lateinit var binding: CheckoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnPlaceOrder.setOnClickListener {
            // Handle order placement
        }
    }
}