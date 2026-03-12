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
        private val statusBadge: TextView = itemView.findViewById(R.id.statusBadge)
        private val bidStatus: TextView = itemView.findViewById(R.id.bidStatus)
        private val statusIndicator: View = itemView.findViewById(R.id.statusIndicator)
        private val currentBid: TextView = itemView.findViewById(R.id.currentBid)
        private val yourBid: TextView = itemView.findViewById(R.id.yourBid)
        
        private var countdownJob: Job? = null
        
        fun bind(bidWithListing: UserBidWithListing) {
            val listing = bidWithListing.listing
            val bid = bidWithListing.bid
            
            // Set listing details
            itemName.text = listing.title
            itemLocation.text = "📍 ${listing.location}"
            
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
                .into(itemImage)
            
            // Set prices
            currentBid.text = CurrencyUtils.formatCurrency(bidWithListing.highestBid)
            yourBid.text = CurrencyUtils.formatCurrency(bid.bidAmount)
            
            // Determine if user is winning
            val isWinning = bid.bidAmount >= bidWithListing.highestBid
            
            // Update status indicator
            if (isWinning) {
                bidStatus.text = "Winning"
                bidStatus.setTextColor(Color.parseColor("#4CAF50"))
                statusIndicator.setBackgroundResource(R.drawable.circle_background_red)
            } else {
                bidStatus.text = "Outbid"
                bidStatus.setTextColor(Color.parseColor("#D32F2F"))
                statusIndicator.setBackgroundColor(Color.parseColor("#D32F2F"))
            }
            
            // Start countdown timer
            startCountdown(listing.endTime)
            
            // Click listener
            itemView.setOnClickListener { onItemClick(bidWithListing) }
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
