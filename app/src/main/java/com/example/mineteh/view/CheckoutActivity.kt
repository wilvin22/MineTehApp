package com.example.mineteh.view

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.example.mineteh.R
import com.example.mineteh.databinding.CheckoutBinding
import com.example.mineteh.viewmodel.CartViewModel
import com.google.android.material.textfield.TextInputEditText

class CheckoutActivity : AppCompatActivity() {

    private lateinit var binding: CheckoutBinding
    private val cartViewModel: CartViewModel by viewModels()
    private val shippingFee = 50.0
    
    private var userFullName = ""
    private var userAddress = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        loadUserInfo()
        observeCart()

        binding.btnPlaceOrder.setOnClickListener {
            placeOrder()
        }
        
        binding.btnEditAddress.setOnClickListener {
            showEditAddressDialog()
        }
    }
    
    private fun loadUserInfo() {
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        userFullName = prefs.getString("full_name", "User") ?: "User"
        userAddress = prefs.getString("address", "No address set") ?: "No address set"
        
        binding.userFullName.text = userFullName
        binding.userAddress.text = userAddress
    }
    
    private fun saveUserInfo() {
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("full_name", userFullName)
            putString("address", userAddress)
            apply()
        }
    }
    
    private fun showEditAddressDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_address, null)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.nameInput)
        val addressInput = dialogView.findViewById<TextInputEditText>(R.id.addressInput)
        
        // Pre-fill with current values
        nameInput.setText(userFullName)
        addressInput.setText(userAddress)
        
        AlertDialog.Builder(this)
            .setTitle("Edit Delivery Information")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newName = nameInput.text.toString().trim()
                val newAddress = addressInput.text.toString().trim()
                
                if (newName.isEmpty() || newAddress.isEmpty()) {
                    Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                userFullName = newName
                userAddress = newAddress
                
                binding.userFullName.text = userFullName
                binding.userAddress.text = userAddress
                
                saveUserInfo()
                Toast.makeText(this, "Delivery information updated", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeCart() {
        cartViewModel.cartItems.observe(this) { items ->
            Log.d("CheckoutActivity", "Cart items: ${items.size}")
            
            if (items.isEmpty()) {
                Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show()
                finish()
                return@observe
            }

            // Display first item (or you can show all items in a RecyclerView)
            val firstItem = items.first()
            
            // Load item image
            val websiteUrl = "https://mineteh.infinityfree.me/home"
            val imageUrl = firstItem.image?.let { imagePath ->
                when {
                    imagePath.startsWith("http") -> imagePath
                    else -> "$websiteUrl/$imagePath"
                }
            }

            if (imageUrl != null) {
                val glideUrl = GlideUrl(
                    imageUrl,
                    LazyHeaders.Builder()
                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                        .addHeader("Referer", "https://mineteh.infinityfree.me/")
                        .build()
                )

                Glide.with(this)
                    .load(glideUrl)
                    .placeholder(R.drawable.dummyphoto)
                    .error(R.drawable.dummyphoto)
                    .into(binding.itemImage)
            }

            // Update item details
            binding.itemTitle.text = firstItem.title
            binding.itemLocation.text = firstItem.sellerName
            binding.itemPrice.text = "₱ ${String.format("%.2f", firstItem.price * firstItem.quantity)}"
            
            // Show item count if multiple items
            if (items.size > 1) {
                binding.itemTitle.text = "${firstItem.title} (+${items.size - 1} more)"
            }
        }

        cartViewModel.cartTotal.observe(this) { total ->
            val grandTotal = total + shippingFee
            
            // Update payment details
            binding.subtotalAmount.text = "₱ ${String.format("%.2f", total)}"
            binding.shippingAmount.text = "₱ ${String.format("%.2f", shippingFee)}"
            binding.totalAmount.text = "₱ ${String.format("%.2f", grandTotal)}"
            
            Log.d("CheckoutActivity", "Subtotal: ₱$total, Shipping: ₱$shippingFee, Total: ₱$grandTotal")
        }
    }

    private fun placeOrder() {
        val items = cartViewModel.cartItems.value
        if (items.isNullOrEmpty()) {
            Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (userAddress == "No address set" || userAddress.isEmpty()) {
            Toast.makeText(this, "Please set your delivery address first", Toast.LENGTH_SHORT).show()
            showEditAddressDialog()
            return
        }

        val total = cartViewModel.cartTotal.value ?: 0.0
        val grandTotal = total + shippingFee

        AlertDialog.Builder(this)
            .setTitle("Confirm Order")
            .setMessage("Place order for ₱${String.format("%.2f", grandTotal)}?\n\n" +
                    "Deliver to: $userFullName\n" +
                    "Address: $userAddress\n\n" +
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