package com.example.mineteh.view

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.mineteh.R
import com.example.mineteh.databinding.ItemDetailBinding
import com.example.mineteh.models.CartItem
import com.example.mineteh.models.Listing
import com.example.mineteh.utils.Resource
import com.example.mineteh.viewmodel.ListingDetailViewModel
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class ItemDetailActivity : AppCompatActivity() {

    private lateinit var binding: ItemDetailBinding
    private val viewModel: ListingDetailViewModel by viewModels()
    private var currentListing: Listing? = null
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d("ItemDetailActivity", "=== onCreate START ===")
        
        try {
            binding = ItemDetailBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d("ItemDetailActivity", "Layout inflated successfully")

            setupToolbar()
            Log.d("ItemDetailActivity", "Toolbar setup complete")
            
            setupObservers()
            Log.d("ItemDetailActivity", "Observers setup complete")
            
            // Get listing_id from Intent (regular intent or deep link)
            val listingId = getListingIdFromIntent()
            Log.d("ItemDetailActivity", "Received listing_id: $listingId")
            
            if (listingId == -1) {
                Log.e("ItemDetailActivity", "Invalid listing_id, finishing activity")
                Toast.makeText(this, "Invalid listing", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // Show loading state immediately
            showLoading()
            Log.d("ItemDetailActivity", "Showing loading state")

            // Load listing data
            Log.d("ItemDetailActivity", "Calling viewModel.loadListing($listingId)")
            viewModel.loadListing(listingId)
            Log.d("ItemDetailActivity", "=== onCreate END ===")
            
        } catch (e: Exception) {
            Log.e("ItemDetailActivity", "FATAL ERROR in onCreate", e)
            e.printStackTrace()
            Toast.makeText(this, "Error loading page: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupToolbar() {
        try {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = ""
            binding.toolbar.setNavigationOnClickListener { 
                Log.d("ItemDetailActivity", "Back button clicked")
                finish() 
            }
        } catch (e: Exception) {
            Log.e("ItemDetailActivity", "Error in setupToolbar", e)
            throw e
        }
    }

    private fun setupObservers() {
        try {
            // Observe listing data
            viewModel.listing.observe(this) { resource ->
                Log.d("ItemDetailActivity", "Listing observer triggered: ${resource?.javaClass?.simpleName}")
                when (resource) {
                    is Resource.Loading -> {
                        Log.d("ItemDetailActivity", "Loading state")
                        showLoading()
                    }
                    is Resource.Success -> {
                        Log.d("ItemDetailActivity", "Success state, data: ${resource.data?.title}")
                        resource.data?.let { listing ->
                            currentListing = listing
                            showContent()
                            displayListing(listing)
                        } ?: run {
                            Log.e("ItemDetailActivity", "Success but data is null")
                            showError("No listing data")
                        }
                    }
                    is Resource.Error -> {
                        Log.e("ItemDetailActivity", "Error state: ${resource.message}")
                        showError(resource.message ?: "Failed to load listing")
                    }
                    null -> {
                        Log.d("ItemDetailActivity", "Null resource")
                    }
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
                        val isFavorited = resource.data ?: false
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
            
            // Observe status update result
            viewModel.statusUpdateResult.observe(this) { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        binding.btnToggleStatus.isEnabled = false
                        binding.btnToggleStatus.text = "Updating..."
                    }
                    is Resource.Success -> {
                        binding.btnToggleStatus.isEnabled = true
                        Toast.makeText(this, "Listing status updated successfully", Toast.LENGTH_SHORT).show()
                        viewModel.resetStatusUpdateResult()
                    }
                    is Resource.Error -> {
                        binding.btnToggleStatus.isEnabled = true
                        Toast.makeText(this, resource.message ?: "Failed to update listing status", Toast.LENGTH_SHORT).show()
                        viewModel.resetStatusUpdateResult()
                    }
                    null -> {
                        binding.btnToggleStatus.isEnabled = true
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ItemDetailActivity", "Error in setupObservers", e)
            throw e
        }
    }

    private fun displayListing(listing: Listing) {
        try {
            Log.d("ItemDetailActivity", "displayListing called for: ${listing.title}")
            
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
            
            Log.d("ItemDetailActivity", "displayListing completed successfully")
        } catch (e: Exception) {
            Log.e("ItemDetailActivity", "Error in displayListing", e)
            showError("Error displaying listing: ${e.message}")
        }
    }

    private fun setupImageCarousel(listing: Listing) {
        try {
            val images = listing.images ?: emptyList()
            Log.d("ItemDetailActivity", "Setting up carousel with ${images.size} images")
            
            if (images.isEmpty()) {
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
        } catch (e: Exception) {
            Log.e("ItemDetailActivity", "Error in setupImageCarousel", e)
        }
    }

    private fun displaySellerInfo(listing: Listing) {
        try {
            listing.seller?.let { seller ->
                val fullName = "${seller.firstName} ${seller.lastName}".trim()
                binding.sellerName.text = if (fullName.isNotEmpty()) fullName else seller.username
                binding.sellerUsername.text = "@${seller.username}"
            } ?: run {
                binding.sellerName.text = "Unknown Seller"
                binding.sellerUsername.text = ""
            }
        } catch (e: Exception) {
            Log.e("ItemDetailActivity", "Error in displaySellerInfo", e)
        }
    }

    private fun setupActionButtons(listing: Listing) {
        try {
            Log.d("ItemDetailActivity", "Setting up action buttons for type: ${listing.listingType}")
            
            // Check if current user is the owner
            val tokenManager = com.example.mineteh.utils.TokenManager(this)
            val currentUserId = tokenManager.getUserId()
            val sellerId = listing.seller?.accountId
            val isOwner = currentUserId != -1 && sellerId != null && sellerId == currentUserId
            
            Log.d("ItemDetailActivity", "=== OWNER CHECK ===")
            Log.d("ItemDetailActivity", "Current user ID: $currentUserId")
            Log.d("ItemDetailActivity", "Seller ID: $sellerId")
            Log.d("ItemDetailActivity", "Seller object: ${listing.seller}")
            Log.d("ItemDetailActivity", "Is owner: $isOwner")
            Log.d("ItemDetailActivity", "==================")
            
            if (isOwner) {
                // Show owner management UI
                setupOwnerManagementUI(listing)
                return
            }
            
            // Hide owner badge for non-owners
            binding.ownerBadge.visibility = View.GONE
            binding.ownerManagementCard.visibility = View.GONE
            
            // Show buyer divider
            binding.divider3.visibility = View.VISIBLE
            binding.divider3Owner.visibility = View.GONE
            
            when (listing.listingType) {
                "FIXED" -> {
                    // Show FIXED listing UI
                    binding.detailItemPrice.text = "₱ ${String.format("%.2f", listing.price)}"
                    binding.detailItemPrice.visibility = View.VISIBLE
                    binding.bidInfoCard.visibility = View.GONE
                    binding.auctionStatusBadge.visibility = View.GONE
                    
                    // Update seller info constraint to price
                    val params = binding.sellerAvatarCard.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                    params.topToBottom = binding.detailItemPrice.id
                    binding.sellerAvatarCard.layoutParams = params
                    
                    // Show FIXED buttons
                    binding.btnAddToCart.visibility = View.VISIBLE
                    binding.btnBuyNow.visibility = View.VISIBLE
                    binding.btnPlaceBid.visibility = View.GONE
                    binding.btnContactSeller.visibility = View.VISIBLE

                    binding.btnAddToCart.setOnClickListener {
                        // Add to cart
                        val cartItem = CartItem(
                            listingId = listing.id,
                            title = listing.title,
                            price = listing.price,
                            image = listing.image,
                            sellerId = listing.seller?.accountId,
                            sellerName = listing.seller?.username ?: "Unknown",
                            quantity = 1
                        )
                        
                        val cartRepo = com.example.mineteh.model.repository.CartRepository(this)
                        cartRepo.addToCart(cartItem)
                        
                        Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show()
                    }

                    binding.btnBuyNow.setOnClickListener {
                        // Create temporary cart item for direct purchase
                        val cartItem = CartItem(
                            listingId = listing.id,
                            title = listing.title,
                            price = listing.price,
                            image = listing.image,
                            sellerId = listing.seller?.accountId,
                            sellerName = listing.seller?.username ?: "Unknown",
                            quantity = 1
                        )
                        
                        // Create temporary cart with just this item
                        val cartRepo = com.example.mineteh.model.repository.CartRepository(this)
                        cartRepo.clearCart() // Clear any existing items
                        cartRepo.addToCart(cartItem) // Add only this item
                        
                        // Go directly to checkout
                        val intent = Intent(this, CheckoutActivity::class.java)
                        startActivity(intent)
                    }
                }
                "BID" -> {
                    // Show BID listing UI
                    binding.detailItemPrice.visibility = View.GONE
                    binding.bidInfoCard.visibility = View.VISIBLE
                    binding.auctionStatusBadge.visibility = View.VISIBLE
                    
                    // Update seller info constraint to bid card
                    val params = binding.sellerAvatarCard.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                    params.topToBottom = binding.bidInfoCard.id
                    binding.sellerAvatarCard.layoutParams = params
                    
                    // Check if auction is still active
                    val isActive = listing.status.equals("active", ignoreCase = true)
                    val timeRemaining = com.example.mineteh.utils.TimeUtils.calculateTimeRemaining(listing.endTime ?: "")
                    
                    if (isActive && timeRemaining > 0) {
                        binding.auctionStatusBadge.text = "LIVE"
                        binding.auctionStatusBadge.setBackgroundResource(R.drawable.circle_background_red)
                    } else {
                        binding.auctionStatusBadge.text = "ENDED"
                        binding.auctionStatusBadge.setBackgroundColor(getColor(R.color.text_secondary))
                    }
                    
                    val currentBid = listing.highestBid?.bidAmount ?: listing.price
                    binding.currentBidAmount.text = "₱ ${String.format("%.2f", currentBid)}"
                    
                    // Show BID buttons - hide Add to Cart and Buy Now, show Contact Seller
                    binding.btnAddToCart.visibility = View.GONE
                    binding.btnBuyNow.visibility = View.GONE
                    binding.btnContactSeller.visibility = View.VISIBLE
                    binding.btnPlaceBid.visibility = View.VISIBLE
                    
                    // Hide FIXED divider, show BID divider
                    binding.divider3.visibility = View.GONE

                    // Setup auction countdown
                    setupAuctionTimer(listing.endTime)

                    binding.btnPlaceBid.setOnClickListener {
                        showBidDialog(listing)
                    }
                    
                    binding.btnContactSeller.setOnClickListener {
                        listing.seller?.accountId?.let { sellerId ->
                            val intent = Intent(this, ChatActivity::class.java).apply {
                                putExtra("other_user_id", sellerId)
                                putExtra("other_user_name", listing.seller?.username ?: "Seller")
                                putExtra("listing_id", listing.id)
                            }
                            startActivity(intent)
                        } ?: run {
                            Toast.makeText(this, "Seller information not available", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            // Contact Seller button
            binding.btnContactSeller.setOnClickListener {
                listing.seller?.accountId?.let { sellerId ->
                    val intent = Intent(this, ChatActivity::class.java).apply {
                        putExtra("other_user_id", sellerId)
                        putExtra("other_user_name", listing.seller?.username ?: "Seller")
                        putExtra("listing_id", listing.id)
                    }
                    startActivity(intent)
                } ?: run {
                    Toast.makeText(this, "Seller information not available", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("ItemDetailActivity", "Error in setupActionButtons", e)
        }
    }

    private fun setupOwnerManagementUI(listing: Listing) {
        try {
            Log.d("ItemDetailActivity", "Setting up owner management UI")
            
            // Show owner badge
            binding.ownerBadge.visibility = View.VISIBLE
            
            // Show price/bid info based on type
            when (listing.listingType) {
                "FIXED" -> {
                    binding.detailItemPrice.text = "₱ ${String.format("%.2f", listing.price)}"
                    binding.detailItemPrice.visibility = View.VISIBLE
                    binding.bidInfoCard.visibility = View.GONE
                    binding.auctionStatusBadge.visibility = View.GONE
                    
                    val params = binding.sellerAvatarCard.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                    params.topToBottom = binding.detailItemPrice.id
                    binding.sellerAvatarCard.layoutParams = params
                }
                "BID" -> {
                    binding.detailItemPrice.visibility = View.GONE
                    binding.bidInfoCard.visibility = View.VISIBLE
                    binding.auctionStatusBadge.visibility = View.VISIBLE
                    
                    val params = binding.sellerAvatarCard.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                    params.topToBottom = binding.bidInfoCard.id
                    binding.sellerAvatarCard.layoutParams = params
                    
                    val isActive = listing.status.equals("active", ignoreCase = true)
                    val timeRemaining = com.example.mineteh.utils.TimeUtils.calculateTimeRemaining(listing.endTime ?: "")
                    
                    if (isActive && timeRemaining > 0) {
                        binding.auctionStatusBadge.text = "LIVE"
                        binding.auctionStatusBadge.setBackgroundResource(R.drawable.circle_background_red)
                    } else {
                        binding.auctionStatusBadge.text = "ENDED"
                        binding.auctionStatusBadge.setBackgroundColor(getColor(R.color.text_secondary))
                    }
                    
                    val currentBid = listing.highestBid?.bidAmount ?: listing.price
                    binding.currentBidAmount.text = "₱ ${String.format("%.2f", currentBid)}"
                    
                    setupAuctionTimer(listing.endTime)
                }
            }
            
            // Hide all buyer action buttons and favorite
            binding.btnAddToCart.visibility = View.GONE
            binding.btnBuyNow.visibility = View.GONE
            binding.btnPlaceBid.visibility = View.GONE
            binding.detailHeart.visibility = View.GONE
            binding.btnContactSeller.visibility = View.GONE
            
            // Show owner management card
            binding.ownerManagementCard.visibility = View.VISIBLE
            
            // Toggle dividers
            binding.divider3.visibility = View.GONE
            binding.divider3Owner.visibility = View.VISIBLE
            
            // Show/hide Close Auction button based on listing type
            if (listing.listingType == "BID") {
                binding.btnCloseAuction.visibility = View.VISIBLE
                binding.btnCloseAuction.setOnClickListener {
                    showCloseAuctionDialog(listing)
                }
            } else {
                binding.btnCloseAuction.visibility = View.GONE
            }
            
            // Setup toggle status button
            val isActive = listing.status.equals("active", ignoreCase = true)
            binding.btnToggleStatus.text = if (isActive) "🚫 Disable Listing" else "✅ Enable Listing"
            binding.btnToggleStatus.setBackgroundColor(getColor(if (isActive) R.color.red else R.color.green))
            
            binding.btnToggleStatus.setOnClickListener {
                if (isActive) {
                    showDisableListingDialog(listing)
                } else {
                    showEnableListingDialog(listing)
                }
            }
            
            // Setup edit listing button
            binding.btnEditListing.setOnClickListener {
                Toast.makeText(this, "Edit listing feature coming soon", Toast.LENGTH_SHORT).show()
            }
            
            // Setup your listings button
            binding.btnViewYourListings.setOnClickListener {
                val intent = Intent(this, MyListingsActivity::class.java)
                startActivity(intent)
            }
            
        } catch (e: Exception) {
            Log.e("ItemDetailActivity", "Error in setupOwnerManagementUI", e)
        }
    }
    
    private fun showDisableListingDialog(listing: Listing) {
        AlertDialog.Builder(this)
            .setTitle("Disable Listing")
            .setMessage("Disable this listing? It will be hidden from buyers.")
            .setPositiveButton("Disable") { _, _ ->
                disableListing(listing.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showCloseAuctionDialog(listing: Listing) {
        AlertDialog.Builder(this)
            .setTitle("Close Auction")
            .setMessage("Close this auction? The highest bidder will win.")
            .setPositiveButton("Close") { _, _ ->
                closeAuction(listing.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun closeAuction(listingId: Int) {
        viewModel.updateListingStatus(listingId, "CLOSED")
    }
    
    private fun showEnableListingDialog(listing: Listing) {
        AlertDialog.Builder(this)
            .setTitle("Enable Listing")
            .setMessage("Enable this listing? It will be visible to buyers again.")
            .setPositiveButton("Enable") { _, _ ->
                enableListing(listing.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun disableListing(listingId: Int) {
        viewModel.updateListingStatus(listingId, "inactive")
    }
    
    private fun enableListing(listingId: Int) {
        viewModel.updateListingStatus(listingId, "active")
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
            Log.e("ItemDetailActivity", "Error in setupAuctionTimer", e)
            binding.auctionEndTime.text = "Invalid end time"
        }
    }

    private fun showBidDialog(listing: Listing) {
        try {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_place_bid, null)
            val bidAmountInput = dialogView.findViewById<TextInputEditText>(R.id.bidAmountInput)
            val currentBidInfo = dialogView.findViewById<TextView>(R.id.currentBidInfo)
            val validationError = dialogView.findViewById<TextView>(R.id.bidValidationError)

            val currentBid = listing.highestBid?.bidAmount ?: listing.price
            currentBidInfo.text = "₱ ${String.format("%.2f", currentBid)}"

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
        } catch (e: Exception) {
            Log.e("ItemDetailActivity", "Error in showBidDialog", e)
            Toast.makeText(this, "Error showing bid dialog", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateHeartIcon(isLiked: Boolean) {
        try {
            if (isLiked) {
                binding.detailHeart.setImageResource(R.drawable.heart_red)
            } else {
                binding.detailHeart.setImageResource(R.drawable.heart)
            }
        } catch (e: Exception) {
            Log.e("ItemDetailActivity", "Error in updateHeartIcon", e)
        }
    }

    private fun showLoading() {
        try {
            Log.d("ItemDetailActivity", "showLoading called")
            binding.progressBar.visibility = View.VISIBLE
            binding.errorLayout.visibility = View.GONE
            binding.contentLayout.visibility = View.GONE
        } catch (e: Exception) {
            Log.e("ItemDetailActivity", "Error in showLoading", e)
        }
    }

    private fun showContent() {
        try {
            Log.d("ItemDetailActivity", "showContent called")
            binding.progressBar.visibility = View.GONE
            binding.errorLayout.visibility = View.GONE
            binding.contentLayout.visibility = View.VISIBLE
        } catch (e: Exception) {
            Log.e("ItemDetailActivity", "Error in showContent", e)
        }
    }

    private fun showError(message: String) {
        try {
            Log.d("ItemDetailActivity", "showError called: $message")
            binding.progressBar.visibility = View.GONE
            binding.errorLayout.visibility = View.VISIBLE
            binding.contentLayout.visibility = View.GONE
            binding.errorMessage.text = message
        } catch (e: Exception) {
            Log.e("ItemDetailActivity", "Error in showError", e)
        }
    }

    private fun getListingIdFromIntent(): Int {
        // First check for deep link URI
        intent.data?.let { uri ->
            if (uri.scheme == "mineteh" && uri.host == "listing") {
                val pathSegments = uri.pathSegments
                if (pathSegments.isNotEmpty()) {
                    return pathSegments[0].toIntOrNull() ?: -1
                }
            }
        }
        
        // Fallback to regular intent extra
        return intent.getIntExtra("listing_id", -1)
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        Log.d("ItemDetailActivity", "onDestroy called")
    }
}
