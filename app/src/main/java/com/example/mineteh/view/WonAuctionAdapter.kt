package com.example.mineteh.view

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

class WonAuctionAdapter(
    private val onItemClick: (UserBidWithListing) -> Unit
) : RecyclerView.Adapter<WonAuctionAdapter.ViewHolder>() {
    
    private var bids = listOf<UserBidWithListing>()
    
    fun submitList(newBids: List<UserBidWithListing>) {
        bids = newBids
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_auction_won, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(bids[position])
    }
    
    override fun getItemCount(): Int = bids.size
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemImage: ImageView = itemView.findViewById(R.id.itemImage)
        private val itemName: TextView = itemView.findViewById(R.id.itemName)
        private val itemLocation: TextView = itemView.findViewById(R.id.itemLocation)
        private val wonPrice: TextView = itemView.findViewById(R.id.wonPrice)
        private val endDate: TextView = itemView.findViewById(R.id.endDate)
        
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
            
            // Set winning bid amount
            wonPrice.text = CurrencyUtils.formatCurrency(bid.bidAmount)
            
            // Set end date
            endDate.text = TimeUtils.formatEndTime(listing.endTime)
            
            // Click listener
            itemView.setOnClickListener { onItemClick(bidWithListing) }
        }
    }
}
