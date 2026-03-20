package com.example.mineteh.view

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mineteh.R
import com.example.mineteh.models.Listing
import com.example.mineteh.utils.ImageUtils

class SellerItemAdapter(
    private val items: List<Listing>
) : RecyclerView.Adapter<SellerItemAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.itemImage)
        val price: TextView = view.findViewById(R.id.itemPrice)
        val name: TextView = view.findViewById(R.id.itemName)
        val location: TextView = view.findViewById(R.id.itemLocation)
        val badge: TextView = view.findViewById(R.id.itemTypeBadge)
        val heartContainer: View = view.findViewById(R.id.heartContainer)
        val yourListingBadge: View = view.findViewById(R.id.yourListingBadge)
        val auctionLayout: View = view.findViewById(R.id.auctionTimerLayout)
        val auctionTimer: TextView = view.findViewById(R.id.itemAuctionTimer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_card_compact, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.price.text = "₱ ${String.format("%.2f", item.price)}"
        holder.name.text = item.title
        holder.location.text = "📍 ${item.location}"
        holder.badge.text = if (item.listingType == "BID") "AUCTION" else "FIXED"
        holder.heartContainer.visibility = View.GONE
        holder.yourListingBadge.visibility = View.GONE

        if (item.listingType == "BID" && item.endTime != null) {
            holder.auctionLayout.visibility = View.VISIBLE
            val ms = com.example.mineteh.utils.TimeUtils.calculateTimeRemaining(item.endTime)
            holder.auctionTimer.text = com.example.mineteh.utils.TimeUtils.formatCountdown(ms)
        } else {
            holder.auctionLayout.visibility = View.GONE
        }

        val imageUrl = ImageUtils.getFullImageUrl(item.image)
        if (imageUrl != null) {
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .placeholder(R.drawable.dummyphoto)
                .error(R.drawable.dummyphoto)
                .centerCrop()
                .into(holder.image)
        } else {
            holder.image.setImageResource(R.drawable.dummyphoto)
        }

        holder.itemView.setOnClickListener {
            val ctx = holder.itemView.context
            val intent = if (item.listingType == "BID") {
                Intent(ctx, BidDetailActivity::class.java)
            } else {
                Intent(ctx, ItemDetailActivity::class.java)
            }
            intent.putExtra("listing_id", item.id)
            ctx.startActivity(intent)
        }
    }
}
