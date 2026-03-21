package com.example.mineteh.view

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.mineteh.R
import com.example.mineteh.databinding.ItemDetailBinding
import com.example.mineteh.models.CartItem
import com.example.mineteh.models.Listing
import com.example.mineteh.model.UserBidData
import com.example.mineteh.utils.Resource
import com.example.mineteh.viewmodel.ListingDetailViewModel
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class BidDetailActivity : AppCompatActivity() {

    private lateinit var binding: ItemDetailBinding
    private val viewModel: ListingDetailViewModel by viewModels()
    private var currentListing: Listing? = null
    private var countDownTimer: CountDownTimer? = null
    private var biddersAdapter: BiddersAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d("BidDetailActivity", "=== onCreate START ===")
        
        try {
            binding = ItemDetailBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d("BidDetailActivity", "Layout inflated successfully")

            setupToolbar()
            Log.d("BidDetailActivity", "Toolbar setup complete")
            
            setupObservers()
            Log.d("BidDetailActivity", "Observers setup complete")
            
            // Get listing_id from Intent
            val listingId = intent.getIntExtra("listing_id", -1)
            Log.d("BidDetailActivity", "Received listing_id: $listingId")
            
            if (listingId == -1) {
                Log.e("BidDetailActivity", "Invalid listing_id, finishing activity")
                Toast.makeText(this, "Invalid listing", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // Show loading state immediately
            showLoading()
            Log.d("BidDetailActivity", "Showing loading state")

            // Load listing data
            Log.d("BidDetailActivity", "Calling viewModel.loadListing($listingId)")
            viewModel.loadListing(listingId)
            Log.d("BidDetailActivity", "=== onCreate END ===")
            
        } catch (e: Exception) {
            Log.e("BidDetailActivity", "FATAL ERROR in onCreate", e)
            e.printStackTrace()
            Toast.makeText(this, "Error loading page: ${e.message}", Toast.LENGTH_LONG).show()
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
                        
                        // Check if we should show bid dialog immediately
                        if (intent.getBooleanExtra("show_bid_dialog", false)) {
                            intent.removeExtra("show_bid_dialog") // Clear the flag
                            // Check if auction is still active
                            val isActive = listing.status.equals("active", ignoreCase = true)
                            val timeRemaining = com.example.mineteh.utils.TimeUtils.calculateTimeRemaining(listing.endTime ?: "")
                            if (isActive && timeRemaining > 0) {
                                showBidDialog(listing)
                            } else {
                                Toast.makeText(this, "Auction has ended", Toast.LENGTH_SHORT).show()
                            }
                        }
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
                    binding.detailHeartBid.isEnabled = false
                }
                is Resource.Success -> {
                    binding.detailHeartBid.isEnabled = true
                    val isFavorited = resource.data ?: false
                    updateHeartIcon(isFavorited)
                    Toast.makeText(
                        this,
                        if (isFavorited) "Saved to favorites" else "Removed from favorites",
                        Toast.LENGTH_SHORT
                    ).show()
                    viewModel.resetFavoriteResult()
                }
                is Resource.Error -> {
                    binding.detailHeartBid.isEnabled = true
                    Toast.makeText(this, resource.message ?: "Failed to update favorite", Toast.LENGTH_SHORT).show()
                    viewModel.resetFavoriteResult()
                }
                null -> {
                    binding.detailHeartBid.isEnabled = true
                }
            }
        }
        
        // Observe status update result
        viewModel.statusUpdateResult.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    // Show loading if needed
                }
                is Resource.Success -> {
                    Toast.makeText(this, "Listing status updated successfully", Toast.LENGTH_SHORT).show()
                    viewModel.resetStatusUpdateResult()
                    // Reload listing
                    currentListing?.let { viewModel.loadListing(it.id) }
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message ?: "Failed to update listing status", Toast.LENGTH_SHORT).show()
                    viewModel.resetStatusUpdateResult()
                }
                null -> {}
            }
        }
    }

    private fun displayListing(listing: Listing) {
        // Display basic info
        binding.detailItemName.text = listing.title
        binding.detailItemDescription.text = listing.description
        binding.detailItemLocation.text = "📍 ${listing.location}"

        // Setup badges
        setupBadges(listing)

        // Setup image carousel
        setupImageCarousel(listing)

        // Display seller info
        displaySellerInfo(listing)

        // Setup action buttons based on listing type
        when (listing.listingType.uppercase()) {
            "BID" -> setupBidActionButtons(listing)
            "FIXED" -> setupFixedActionButtons(listing)
            else -> setupFixedActionButtons(listing) // Default to fixed
        }

        // Update favorite icon
        updateHeartIcon(listing.isFavorited)

        // Setup retry button
        binding.btnRetry.setOnClickListener {
            viewModel.loadListing(listing.id)
        }

        // Setup bidders list (only for BID type)
        if (listing.listingType.uppercase() == "BID") {
            setupBiddersList(listing)
        }
        
        // Setup share button
        binding.btnShare?.setOnClickListener {
            shareListing(listing)
        }
    }
    
    private fun setupBadges(listing: Listing) {
        // Category badge
        binding.badgeCategory?.apply {
            text = listing.category
            visibility = if (listing.category.isNotBlank()) View.VISIBLE else View.GONE
        }
        
        // Listing type badge
        binding.badgeListingType?.apply {
            text = if (listing.listingType.uppercase() == "BID") "Auction" else "Fixed Price"
            visibility = View.VISIBLE
        }
        
        // Condition badge - placeholder for future
        binding.badgeCondition?.visibility = View.GONE
    }
    
    private fun shareListing(listing: Listing) {
        val shareText = buildString {
            append("Check out \"${listing.title}\" on MineTeh!")
            append("\n\n")
            if (listing.listingType.uppercase() == "BID") {
                val currentBid = listing.highestBid?.bidAmount ?: listing.price
                append("Current Bid: ₱${String.format("%.2f", currentBid)}")
            } else {
                append("Price: ₱${String.format("%.2f", listing.price)}")
            }
            append("\n📍 ${listing.location}")
            append("\n\nDownload MineTeh app to bid or buy!")
        }
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, listing.title)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, "Share via"))
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

    private fun setupBidActionButtons(listing: Listing) {
        Log.d("BidDetailActivity", "=== SETUP BID ACTION BUTTONS START ===")
        
        // Check if current user is the owner
        val tokenManager = com.example.mineteh.utils.TokenManager(this)
        val currentUserId = tokenManager.getUserId()
        val sellerId = listing.seller?.accountId
        val isOwner = currentUserId != -1 && sellerId != null && sellerId == currentUserId
        
        Log.d("BidDetailActivity", "Current user ID: $currentUserId, Seller ID: $sellerId, Is owner: $isOwner")
        
        if (isOwner) {
            setupOwnerBidUI(listing)
            return
        }
        
        // BUYER view for BID listing
        binding.ownerBadge.visibility = View.GONE
        binding.ownerManagementCard.visibility = View.GONE
        
        // Show BID info card
        binding.detailItemPrice.visibility = View.GONE
        binding.bidInfoCard.visibility = View.VISIBLE
        binding.auctionStatusBadge.visibility = View.VISIBLE
        
        // Auction status
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
        
        // Show BID buyer UI: bidBuyerRow (heart + place bid) + contact seller below
        binding.fixedBuyerRow.visibility = View.GONE
        binding.btnBuyNow.visibility = View.GONE
        binding.bidBuyerRow.visibility = View.VISIBLE
        binding.btnContactSellerBid.visibility = View.VISIBLE
        binding.divider3.visibility = View.GONE

        // Setup auction countdown
        setupAuctionTimer(listing.endTime)

        binding.btnPlaceBid.setOnClickListener {
            showBidDialog(listing)
        }
        
        binding.detailHeartBid.setOnClickListener {
            viewModel.toggleFavorite(listing.id)
        }
        
        binding.btnContactSellerBid.setOnClickListener {
            listing.seller?.accountId?.let { sid ->
                val intent = Intent(this, ChatActivity::class.java).apply {
                    putExtra("other_user_id", sid)
                    putExtra("other_user_name", listing.seller?.username ?: "Seller")
                    putExtra("listing_id", listing.id)
                }
                startActivity(intent)
            } ?: Toast.makeText(this, "Seller information not available", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupOwnerBidUI(listing: Listing) {
        Log.d("BidDetailActivity", "=== SETUP OWNER BID UI START ===")
        
        // Show owner badge
        binding.ownerBadge.visibility = View.VISIBLE
        
        // Show BID info
        binding.detailItemPrice.visibility = View.GONE
        binding.bidInfoCard.visibility = View.VISIBLE
        binding.auctionStatusBadge.visibility = View.VISIBLE
        
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
        
        // Hide all buyer UI
        binding.fixedBuyerRow.visibility = View.GONE
        binding.btnBuyNow.visibility = View.GONE
        binding.bidBuyerRow.visibility = View.GONE
        binding.btnContactSellerBid.visibility = View.GONE
        binding.divider3.visibility = View.GONE
        
        // Show owner management card
        binding.ownerManagementCard.visibility = View.VISIBLE
        binding.btnCloseAuction.visibility = View.VISIBLE
        
        // Setup buttons
        binding.btnCloseAuction.setOnClickListener {
            showCloseAuctionDialog(listing)
        }
        
        val isActiveStatus = listing.status.equals("active", ignoreCase = true)
        binding.btnToggleStatus.text = if (isActiveStatus) "🚫 Disable Listing" else "✅ Enable Listing"
        binding.btnToggleStatus.setBackgroundColor(getColor(if (isActiveStatus) R.color.red else R.color.green))
        
        binding.btnToggleStatus.setOnClickListener {
            if (isActiveStatus) showDisableListingDialog(listing) else showEnableListingDialog(listing)
        }
        
        binding.btnEditListing.setOnClickListener {
            Toast.makeText(this, "Edit listing feature coming soon", Toast.LENGTH_SHORT).show()
        }
        
        binding.btnViewYourListings.setOnClickListener {
            startActivity(Intent(this, MyListingsActivity::class.java))
        }
        
        Log.d("BidDetailActivity", "=== SETUP OWNER BID UI COMPLETE ===")
    }
    
    private fun setupFixedActionButtons(listing: Listing) {
        Log.d("BidDetailActivity", "=== SETUP FIXED ACTION BUTTONS START ===")
        
        // Check if current user is the owner
        val tokenManager = com.example.mineteh.utils.TokenManager(this)
        val currentUserId = tokenManager.getUserId()
        val sellerId = listing.seller?.accountId
        val isOwner = currentUserId != -1 && sellerId != null && sellerId == currentUserId
        
        Log.d("BidDetailActivity", "Current user ID: $currentUserId, Seller ID: $sellerId, Is owner: $isOwner")
        
        if (isOwner) {
            setupOwnerFixedUI(listing)
            return
        }
        
        // BUYER view for FIXED listing
        binding.ownerBadge.visibility = View.GONE
        binding.ownerManagementCard.visibility = View.GONE
        
        // Hide BID-specific UI
        binding.bidInfoCard.visibility = View.GONE
        binding.auctionStatusBadge.visibility = View.GONE
        binding.bidBuyerRow.visibility = View.GONE
        binding.btnContactSellerBid.visibility = View.GONE
        
        // Show FIXED price card
        binding.fixedPriceCard?.visibility = View.VISIBLE
        binding.detailItemPrice?.text = "₱${String.format("%.2f", listing.price)}"
        binding.detailItemPriceStub?.visibility = View.GONE
        
        // Show FIXED buyer UI
        binding.fixedBuyerRow.visibility = View.VISIBLE
        binding.divider3.visibility = View.GONE

        binding.detailHeart.setOnClickListener {
            viewModel.toggleFavorite(listing.id)
        }
        
        binding.btnImInterested.setOnClickListener {
            Toast.makeText(this, "Seller has been notified of your interest!", Toast.LENGTH_SHORT).show()
        }
        
        binding.btnContactSeller.setOnClickListener {
            listing.seller?.accountId?.let { sid ->
                val intent = Intent(this, ChatActivity::class.java).apply {
                    putExtra("other_user_id", sid)
                    putExtra("other_user_name", listing.seller?.username ?: "Seller")
                    putExtra("listing_id", listing.id)
                }
                startActivity(intent)
            } ?: Toast.makeText(this, "Seller information not available", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupOwnerFixedUI(listing: Listing) {
        Log.d("BidDetailActivity", "=== SETUP OWNER FIXED UI START ===")
        
        // Show owner badge
        binding.ownerBadge.visibility = View.VISIBLE
        
        // Hide BID-specific UI
        binding.bidInfoCard.visibility = View.GONE
        binding.bidBuyerRow.visibility = View.GONE
        binding.btnContactSellerBid.visibility = View.GONE
        
        // Show FIXED price card for owner reference
        binding.fixedPriceCard?.visibility = View.VISIBLE
        binding.detailItemPrice?.text = "₱${String.format("%.2f", listing.price)}"
        
        // Hide buyer UI
        binding.fixedBuyerRow.visibility = View.GONE
        binding.divider3.visibility = View.GONE
        
        // Show owner management card
        binding.ownerManagementCard.visibility = View.VISIBLE
        binding.btnCloseAuction.visibility = View.GONE // Not applicable for fixed
        
        val isActiveStatus = listing.status.equals("active", ignoreCase = true)
        binding.btnToggleStatus.text = if (isActiveStatus) "🚫 Disable Listing" else "✅ Enable Listing"
        binding.btnToggleStatus.setBackgroundColor(getColor(if (isActiveStatus) R.color.red else R.color.green))
        
        binding.btnToggleStatus.setOnClickListener {
            if (isActiveStatus) showDisableListingDialog(listing) else showEnableListingDialog(listing)
        }
        
        binding.btnEditListing.setOnClickListener {
            Toast.makeText(this, "Edit listing feature coming soon", Toast.LENGTH_SHORT).show()
        }
        
        binding.btnViewYourListings.setOnClickListener {
            startActivity(Intent(this, MyListingsActivity::class.java))
        }
        
        Log.d("BidDetailActivity", "=== SETUP OWNER FIXED UI COMPLETE ===")
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
    
    private fun disableListing(listingId: Int) {
        viewModel.updateListingStatus(listingId, "inactive")
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
            binding.auctionEndTime.text = "Invalid end time"
        }
    }

    private fun showBidDialog(listing: Listing) {
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
    }

    private fun updateHeartIcon(isLiked: Boolean) {
        val heartRes = if (isLiked) R.drawable.heart_red else R.drawable.heart
        binding.detailHeartBid.setImageResource(heartRes)
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    /**
     * Setup and display the bidders list
     */
    private fun setupBiddersList(listing: Listing) {
        val recyclerView = binding.root.findViewById<RecyclerView>(R.id.recyclerBidders)
        val emptyState = binding.root.findViewById<View>(R.id.emptyBiddersState)
        val totalBidsBadge = binding.root.findViewById<TextView>(R.id.txtTotalBids)
        
        // Safety check for null views
        if (recyclerView == null || emptyState == null || totalBidsBadge == null) {
            Log.w("BidDetailActivity", "Bidders list views not found, skipping setup")
            return
        }
        
        // Get all bids for this listing from Supabase
        // For now, we'll use an empty list - you can load actual bids from viewModel later
        val allBids = listOf<UserBidData>()
        
        // Update badge count
        totalBidsBadge.text = allBids.size.toString()
        
        // Show/hide empty state
        if (allBids.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
            
            // Setup adapter
            if (biddersAdapter == null) {
                biddersAdapter = BiddersAdapter(this)
                recyclerView.adapter = biddersAdapter
                recyclerView.layoutManager = LinearLayoutManager(this)
            }
            biddersAdapter?.submitList(allBids)
        }
    }

    /**
     * Adapter for displaying bidders in RecyclerView
     */
    inner class BiddersAdapter(private val context: android.content.Context) : 
        RecyclerView.Adapter<BiddersAdapter.BidderViewHolder>() {
        
        private var bidders = listOf<UserBidData>()
        
        inner class BidderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val imgAvatar: android.widget.ImageView = itemView.findViewById(R.id.imgBidderAvatar)
            private val txtName: TextView = itemView.findViewById(R.id.txtBidderName)
            private val txtTime: TextView = itemView.findViewById(R.id.txtBidTime)
            private val txtAmount: TextView = itemView.findViewById(R.id.txtBidAmount)
            private val divider: View = itemView.findViewById(R.id.dividerBidder)
            
            fun bind(bid: UserBidData) {
                // Display bid amount
                txtAmount.text = "₱ ${String.format("%.2f", bid.bidAmount)}"
                
                // Display formatted time
                txtTime.text = formatBidTime(bid.bidTime)
                
                // For now, show user ID as name (you can enhance this to fetch actual username)
                txtName.text = "Bidder #${bid.userId}"
                
                // Show divider for all items except the last one
                divider.visibility = if (adapterPosition < itemCount - 1) View.VISIBLE else View.GONE
            }
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BidderViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_bidder, parent, false)
            return BidderViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: BidderViewHolder, position: Int) {
            holder.bind(bidders[position])
        }
        
        override fun getItemCount(): Int = bidders.size
        
        fun submitList(newBidders: List<UserBidData>) {
            // Sort by bid amount descending (highest bid first)
            bidders = newBidders.sortedByDescending { it.bidAmount }
            notifyDataSetChanged()
        }
        
        private fun formatBidTime(bidTime: String): String {
            // Format "2024-01-15 10:30:00" to "2 hours ago"
            return try {
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val bidDate = format.parse(bidTime)
                val now = Date()
                val diffMs = now.time - bidDate!!.time
                val diffMins = diffMs / (1000 * 60)
                
                when {
                    diffMins < 1 -> "Just now"
                    diffMins < 60 -> "${diffMins}m ago"
                    diffMins < 1440 -> "${diffMins / 60}h ago"
                    else -> "${diffMins / 1440}d ago"
                }
            } catch (e: Exception) {
                Log.e("BiddersAdapter", "Error formatting bid time", e)
                bidTime
            }
        }
    }
}
