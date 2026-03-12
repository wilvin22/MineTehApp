package com.example.mineteh.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mineteh.databinding.CartBinding
import com.example.mineteh.viewmodel.CartViewModel

class CartActivity : AppCompatActivity() {

    private lateinit var binding: CartBinding
    private val viewModel: CartViewModel by viewModels()
    private lateinit var adapter: CartAdapter

    private val checkoutLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Order was placed successfully, refresh cart
            viewModel.loadCart()
            // If cart is empty after order, finish this activity
            if (viewModel.cartItems.value.isNullOrEmpty()) {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        observeViewModel()

        binding.btnCheckout.setOnClickListener {
            val intent = Intent(this, CheckoutActivity::class.java)
            checkoutLauncher.launch(intent)
        }
    }

    private fun setupRecyclerView() {
        adapter = CartAdapter(
            onQuantityChanged = { listingId, quantity ->
                viewModel.updateQuantity(listingId, quantity)
            },
            onRemoveClicked = { listingId ->
                viewModel.removeFromCart(listingId)
            }
        )
        
        binding.cartRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.cartRecyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.cartItems.observe(this) { items ->
            adapter.submitList(items)
            
            // Update toolbar title with count
            binding.toolbar.title = "Shopping Cart (${items.size})"
            
            // Show/hide empty state
            if (items.isEmpty()) {
                binding.cartRecyclerView.visibility = View.GONE
                binding.emptyCartLayout.visibility = View.VISIBLE
                binding.btnCheckout.isEnabled = false
            } else {
                binding.cartRecyclerView.visibility = View.VISIBLE
                binding.emptyCartLayout.visibility = View.GONE
                binding.btnCheckout.isEnabled = true
            }
        }
        
        viewModel.cartTotal.observe(this) { total ->
            binding.totalPrice.text = "₱ ${String.format("%.2f", total)}"
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadCart()
    }
}
