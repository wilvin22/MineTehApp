package com.example.mineteh.view

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.mineteh.R
import com.example.mineteh.databinding.ItemDetailBinding
import com.example.mineteh.models.Listing
import com.example.mineteh.utils.Resource
import com.example.mineteh.viewmodel.ListingDetailViewModel
import com.google.android.material.textfield.TextInputEditText
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class ItemDetailActivity : AppCompatActivity() {

    private lateinit var binding: ItemDetailBinding
    private val viewModel: ListingDetailViewModel by viewModels()
    private var currentListing: Listing? = null
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            binding = ItemDetailBinding.inflate(layoutInflater)
            setContentView(binding.root)

            android.util.Log.d("ItemDetailActivity", "onCreate started")
            
            setupToolbar()
            setupObservers()
            
            // Get listing_id from Intent
            val listingId = intent.getIntExtra("listing_id", -1)
            android.util.Log.d("ItemDetailActivity", "Received listing_id: $listingId")
            
            if (listingId == -1) {
                android.util.Log.e("ItemDetailActivity", "Invalid listing_id")
                Toast.makeText(this, "Invalid listing", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // Load listing data
            android.util.Log.d("ItemDetailActivity", "Loading listing $listingId")
            viewModel.loadListing(listingId)
        } catch (e: Exception) {
            android.util.Log.e("ItemDetailActivity", "Error in onCreate", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupObservers() {
        // Observe listing data
        viewModel.listing.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> showLoading()
                is Resource.Success -> {
                    resource.data?.let { listing ->
                        currentListing = listing
                        showContent()
                        displayListing(listing)
                    }
                }
                is Resource.Error -> showError(resource.message ?: "Failed to load listing")
                null -> {}
            }
        }

        // Observe bid result
        viewModel.bidResult.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.btnPlaceBid.isEnabled = false
                    binding.btnPlaceBid.text = "Placing bid..."
                }
                is Resource.Success -> {
                    binding.btnPlaceBid.isEnabled = true
                    binding.btnPlaceBid.text = "Place Bid"
                    Toast.makeText(this, "Bid placed successfully!", Toast.LENGTH_SHORT).show()
                    viewModel.resetBidResult()
                    // Reload listing to get updated bid
                    currentListing?.let { viewModel.loadListing(it.id) }
                }
                is Resource.Error -> {
                    binding.btnPlaceBid.isEnabled = true
                    binding.btnPlaceBid.text = "Place Bid"
                    Toast.makeText(this, resource.message ?: "Failed to place bid", Toast.LENGTH_LONG).show()
                    viewModel.resetBidResult()
                }
                null -> {
                    binding.btnPlaceBid.isEnabled = true
                    binding.btnPlaceBid.text = "Place Bid"
                }
            }
        }

        // Observe favorite result
        viewModel.favoriteResult.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.detailHeart.isEnabled = false
                }
                is Resource.Success -> {
                    binding.detailHeart.isEnabled = true
                    val isFavorited = resource.data?.isFavorited ?: false
                    updateHeartIcon(isFavorited)
                    Toast.makeText(
                        this,
                        if (isFavorited) "Added to favorites" else "Removed from favorites",
                        Toast.LENGTH_SHORT
                    ).show()
                    viewModel.resetFavoriteResult()
                }
                is Resource.Error -> {
                    binding.detailHeart.isEnabled = true
                    Toast.makeText(this, resource.message ?: "Failed to update favorite", Toast.LENGTH_SHORT).show()
                    viewModel.resetFavoriteResult()
                }
                null -> {
                    binding.detailHeart.isEnabled = true
                }
            }
        }
    }

    private fun displayListing(listing: Listing) {
        // Display basic info
        binding.detailItemName.text = listing.title
        binding.detailItemDescription.text = listing.description
        binding.detailItemLocation.text = "📍 ${listing.location}"

        // Setup image carousel
        setupImageCarousel(listing)

        // Display seller info
        displaySellerInfo(listing)

        // Setup action buttons based on listing type
        setupActionButtons(listing)

        // Update favorite icon
        updateHeartIcon(listing.isFavorited)

        // Setup favorite click listener
        binding.detailHeart.setOnClickListener {
            viewModel.toggleFavorite(listing.id)
        }

        // Setup retry button
        binding.btnRetry.setOnClickListener {
            viewModel.loadListing(listing.id)
        }
    }

    private fun setupImageCarousel(listing: Listing) {
        val images = listing.images ?: emptyList()
        
        if (images.isEmpty()) {
            // Show placeholder if no images
            binding.imagePosition.text = "1 / 1"
            return
        }

        val adapter = ImageCarouselAdapter(images, this)
        binding.imageCarousel.adapter = adapter
        binding.imagePosition.text = "1 / ${images.size}"

        binding.imageCarousel.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.imagePosition.text = "${position + 1} / ${images.size}"
            }
        })
    }

    private fun displaySellerInfo(listing: Listing) {
        listing.seller?.let { seller ->
            val fullName = "${seller.firstName} ${seller.lastName}".trim()
            binding.sellerName.text = if (fullName.isNotEmpty()) fullName else seller.username
            binding.sellerUsername.text = "@${seller.username}"
        } ?: run {
            binding.sellerName.text = "Unknown Seller"
            binding.sellerUsername.text = ""
        }
    }

    private fun setupActionButtons(listing: Listing) {
        when (listing.listingType) {
            "FIXED" -> {
                // Show FIXED listing buttons
                binding.detailItemPrice.text = "₱ ${String.format("%.2f", listing.price)}"
                binding.detailItemPrice.visibility = View.VISIBLE
                binding.currentBidLabel.visibility = View.GONE
                binding.currentBidAmount.visibility = View.GONE
                binding.auctionEndTime.visibility = View.GONE
                
                binding.btnAddToCart.visibility = View.VISIBLE
                binding.btnBuyNow.visibility = View.VISIBLE
                binding.btnPlaceBid.visibility = View.GONE

                binding.btnAddToCart.setOnClickListener {
                    Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show()
                }

                binding.btnBuyNow.setOnClickListener {
                    val intent = Intent(this, CheckoutActivity::class.java)
                    intent.putExtra("listing_id", listing.id)
                    startActivity(intent)
                }
            }
            "BID" -> {
                // Show BID listing buttons
                binding.detailItemPrice.visibility = View.GONE
                binding.currentBidLabel.visibility = View.VISIBLE
                binding.currentBidAmount.visibility = View.VISIBLE
                binding.auctionEndTime.visibility = View.VISIBLE
                
                val currentBid = listing.highestBid?.bidAmount ?: listing.price
                binding.currentBidAmount.text = "₱ ${String.format("%.2f", currentBid)}"
                
                binding.btnAddToCart.visibility = View.GONE
                binding.btnBuyNow.visibility = View.GONE
                binding.btnPlaceBid.visibility = View.VISIBLE

                // Setup auction countdown
                setupAuctionTimer(listing.endTime)

                binding.btnPlaceBid.setOnClickListener {
                    showBidDialog(listing)
                }
            }
        }

        // Contact Seller button (always visible)
        binding.btnContactSeller.setOnClickListener {
            Toast.makeText(this, "Contact seller feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupAuctionTimer(endTime: String?) {
        if (endTime == null) {
            binding.auctionEndTime.text = "No end time"
            return
        }

        try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC")
            val endDate = format.parse(endTime)
            
            if (endDate == null) {
                binding.auctionEndTime.text = "Invalid end time"
                return
            }

            val currentTime = System.currentTimeMillis()
            val endTimeMillis = endDate.time
            val timeRemaining = endTimeMillis - currentTime

            if (timeRemaining <= 0) {
                binding.auctionEndTime.text = "Auction ended"
                binding.btnPlaceBid.isEnabled = false
                return
            }

            countDownTimer?.cancel()
            countDownTimer = object : CountDownTimer(timeRemaining, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val days = millisUntilFinished / (1000 * 60 * 60 * 24)
                    val hours = (millisUntilFinished / (1000 * 60 * 60)) % 24
                    val minutes = (millisUntilFinished / (1000 * 60)) % 60
                    val seconds = (millisUntilFinished / 1000) % 60

                    binding.auctionEndTime.text = when {
                        days > 0 -> "Ends in: ${days}d ${hours}h ${minutes}m"
                        hours > 0 -> "Ends in: ${hours}h ${minutes}m ${seconds}s"
                        else -> "Ends in: ${minutes}m ${seconds}s"
                    }
                }

                override fun onFinish() {
                    binding.auctionEndTime.text = "Auction ended"
                    binding.btnPlaceBid.isEnabled = false
                }
            }.start()
        } catch (e: Exception) {
            binding.auctionEndTime.text = "Invalid end time"
        }
    }

    private fun showBidDialog(listing: Listing) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_place_bid, null)
        val bidAmountInput = dialogView.findViewById<TextInputEditText>(R.id.bidAmountInput)
        val currentBidInfo = dialogView.findViewById<TextView>(R.id.currentBidInfo)
        val validationError = dialogView.findViewById<TextView>(R.id.bidValidationError)

        val currentBid = listing.highestBid?.bidAmount ?: listing.price
        currentBidInfo.text = "Current highest bid: ₱ ${String.format("%.2f", currentBid)}"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Place Bid", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val bidAmountStr = bidAmountInput.text.toString()
                val bidAmount = bidAmountStr.toDoubleOrNull()

                when {
                    bidAmount == null -> {
                        validationError.text = "Please enter a valid amount"
                        validationError.visibility = View.VISIBLE
                    }
                    bidAmount <= currentBid -> {
                        validationError.text = "Bid must be higher than ₱ ${String.format("%.2f", currentBid)}"
                        validationError.visibility = View.VISIBLE
                    }
                    else -> {
                        validationError.visibility = View.GONE
                        viewModel.placeBid(listing.id, bidAmount)
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun updateHeartIcon(isLiked: Boolean) {
        if (isLiked) {
            binding.detailHeart.setImageResource(R.drawable.heart_red)
        } else {
            binding.detailHeart.setImageResource(R.drawable.heart)
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.errorLayout.visibility = View.GONE
        binding.contentLayout.visibility = View.GONE
    }

    private fun showContent() {
        binding.progressBar.visibility = View.GONE
        binding.errorLayout.visibility = View.GONE
        binding.contentLayout.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.errorLayout.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE
        binding.errorMessage.text = message
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
