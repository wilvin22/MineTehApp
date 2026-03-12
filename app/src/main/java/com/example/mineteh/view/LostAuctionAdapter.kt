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

class LostAuctionAdapter(
    private val onItemClick: (UserBidWithListing) -> Unit
) : RecyclerView.Adapter<LostAuctionAdapter.ViewHolder>() {
    
    private var bids = listOf<UserBidWithListing>()
    
    fun submitList(newBids: List<UserBidWithListing>) {
        bids = newBids
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_auction_lost, parent, false)
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
        private val lostPrice: TextView = itemView.findViewById(R.id.lostPrice)
        
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
                .into(itemImage)
            
            // Set lost bid amount
            lostPrice.text = "Lost Bid: ${CurrencyUtils.formatCurrency(bid.bidAmount)}"
            
            // Click listener
            itemView.setOnClickListener { onItemClick(bidWithListing) }
        }
    }
}
