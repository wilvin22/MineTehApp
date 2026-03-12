package com.example.mineteh.view

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.mineteh.databinding.CheckoutBinding
import com.example.mineteh.viewmodel.CartViewModel

class CheckoutActivity : AppCompatActivity() {

    private lateinit var binding: CheckoutBinding
    private val cartViewModel: CartViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        observeCart()

        binding.btnPlaceOrder.setOnClickListener {
            placeOrder()
        }
    }

    private fun observeCart() {
        cartViewModel.cartItems.observe(this) { items ->
            Log.d("CheckoutActivity", "Cart items: ${items.size}")
            
            if (items.isEmpty()) {
                Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show()
                finish()
                return@observe
            }
        }

        cartViewModel.cartTotal.observe(this) { total ->
            val shippingFee = 50.0
            val grandTotal = total + shippingFee
            
            // Update UI (Note: The layout has hardcoded values, but we can show toast or dialog with real values)
            Log.d("CheckoutActivity", "Subtotal: ₱$total, Shipping: ₱$shippingFee, Total: ₱$grandTotal")
        }
    }

    private fun placeOrder() {
        val items = cartViewModel.cartItems.value
        if (items.isNullOrEmpty()) {
            Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show()
            return
        }

        val total = cartViewModel.cartTotal.value ?: 0.0
        val shippingFee = 50.0
        val grandTotal = total + shippingFee

        AlertDialog.Builder(this)
            .setTitle("Confirm Order")
            .setMessage("Place order for ₱${String.format("%.2f", grandTotal)}?\n\n" +
                    "Items: ${items.size}\n" +
                    "Subtotal: ₱${String.format("%.2f", total)}\n" +
                    "Shipping: ₱${String.format("%.2f", shippingFee)}\n" +
                    "Total: ₱${String.format("%.2f", grandTotal)}\n\n" +
                    "Payment Method: Cash on Delivery")
            .setPositiveButton("Confirm") { _, _ ->
                // Clear cart after successful order
                cartViewModel.clearCart()
                
                Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_LONG).show()
                
                // Close checkout and cart activities
                setResult(RESULT_OK)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}