package com.example.mineteh.view

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.mineteh.R
import com.example.mineteh.model.UserBidWithListing
import com.example.mineteh.utils.CurrencyUtils
import com.example.mineteh.utils.TimeUtils
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LiveAuctionAdapter(
    private val onItemClick: (UserBidWithListing) -> Unit
) : RecyclerView.Adapter<LiveAuctionAdapter.ViewHolder>() {
    
    private var bids = listOf<UserBidWithListing>()
    
    fun submitList(newBids: List<UserBidWithListing>) {
        bids = newBids
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_auction_live, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(bids[position])
    }
    
    override fun getItemCount(): Int = bids.size
    
    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.cancelCountdown()
    }
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemImage: ImageView = itemView.findViewById(R.id.itemImage)
        private val itemName: TextView = itemView.findViewById(R.id.itemName)
        private val itemLocation: TextView = itemView.findViewById(R.id.itemLocation)
        private val txtTimer: TextView = itemView.findViewById(R.id.txtTimer)
        private val startingPrice: TextView = itemView.findViewById(R.id.startingPrice)
        private val currentBid: TextView = itemView.findViewById(R.id.currentBid)
        private val yourBid: TextView = itemView.findViewById(R.id.yourBid)
        private val btnRaiseBid: MaterialButton = itemView.findViewById(R.id.btnRaiseBid)
        
        private var countdownJob: Job? = null
        
        fun bind(bidWithListing: UserBidWithListing) {
            val listing = bidWithListing.listing
            val bid = bidWithListing.bid
            
            // Set listing details
            itemName.text = listing.title
            itemLocation.text = listing.location
            
            // Load image
            val imageUrl = listing.image?.let {
                "https://mineteh.infinityfree.me/home/$it"
            }
            
            Glide.with(itemView.context)
                .load(imageUrl)
                .apply(
                    RequestOptions()
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                )
                .addListener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }
                    
                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable,
                        model: Any,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }
                })
                .into(itemImage)
            
            // Set prices
            startingPrice.text = CurrencyUtils.formatCurrency(listing.price)
            currentBid.text = CurrencyUtils.formatCurrency(bidWithListing.highestBid)
            yourBid.text = CurrencyUtils.formatCurrency(bid.bidAmount)
            
            // Determine if user is winning
            val isWinning = bid.bidAmount >= bidWithListing.highestBid
            
            // Update button text and color based on status
            if (isWinning) {
                btnRaiseBid.text = "Winning"
                btnRaiseBid.setBackgroundColor(Color.parseColor("#4CAF50"))
            } else {
                btnRaiseBid.text = "Outbid - Raise Bid"
                btnRaiseBid.setBackgroundColor(Color.parseColor("#D32F2F"))
            }
            
            // Start countdown timer
            startCountdown(listing.endTime)
            
            // Click listeners
            itemView.setOnClickListener { onItemClick(bidWithListing) }
            btnRaiseBid.setOnClickListener { onItemClick(bidWithListing) }
        }
        
        private fun startCountdown(endTime: String?) {
            if (endTime == null) {
                txtTimer.text = "No end time"
                return
            }
            
            countdownJob?.cancel()
            countdownJob = CoroutineScope(Dispatchers.Main).launch {
                while (isActive) {
                    val remaining = TimeUtils.calculateTimeRemaining(endTime)
                    txtTimer.text = TimeUtils.formatCountdown(remaining)
                    
                    if (remaining <= 0) {
                        break
                    }
                    
                    delay(1000)
                }
            }
        }
        
        fun cancelCountdown() {
            countdownJob?.cancel()
        }
    }
}
