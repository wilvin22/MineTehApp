package com.example.mineteh.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mineteh.R
import com.example.mineteh.models.Listing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ListingsAdapter(
    private val onItemClick: (Listing) -> Unit
) : RecyclerView.Adapter<ListingsAdapter.ViewHolder>() {

    private var listings = listOf<Listing>()

    fun submitList(newListings: List<Listing>?) {
        listings = newListings ?: emptyList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listings[position])
    }

    override fun getItemCount() = listings.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemImage: ImageView = itemView.findViewById(R.id.itemImage)
        private val itemHeart: ImageView = itemView.findViewById(R.id.itemHeart)
        private val itemTypeBadge: TextView = itemView.findViewById(R.id.itemTypeBadge)
        private val itemPrice: TextView = itemView.findViewById(R.id.itemPrice)
        private val itemName: TextView = itemView.findViewById(R.id.itemName)
        private val itemLocation: TextView = itemView.findViewById(R.id.itemLocation)
        private val auctionTimerLayout: LinearLayout = itemView.findViewById(R.id.auctionTimerLayout)
        private val itemAuctionTimer: TextView = itemView.findViewById(R.id.itemAuctionTimer)

        fun bind(listing: Listing) {
            // Set price
            itemPrice.text = "₱${listing.price}"

            // Set name
            itemName.text = listing.title

            // Set location
            itemLocation.text = "📍 ${listing.location}"

            // Set type badge
            if (listing.listingType == "BID") {
                itemTypeBadge.text = "BID"
                itemTypeBadge.setBackgroundResource(R.drawable.badge_background_bid)

                // Show auction timer
                auctionTimerLayout.visibility = View.VISIBLE

                // Calculate remaining time
                listing.endTime?.let { endTime ->
                    val remainingTime = calculateRemainingTime(endTime)
                    itemAuctionTimer.text = remainingTime
                }
            } else {
                itemTypeBadge.text = "FIXED"
                itemTypeBadge.setBackgroundResource(R.drawable.badge_background)
                auctionTimerLayout.visibility = View.GONE
            }

            // Load image (use Glide or Picasso)
            listing.image?.let { imageUrl ->
                // TODO: Add Glide dependency and uncomment:
                // Glide.with(itemView.context).load(imageUrl).into(itemImage)
            }

            // Set favorite icon
            itemHeart.setImageResource(
                if (listing.isFavorited) R.drawable.heart_red else R.drawable.heart
            )

            // Click listeners
            itemView.setOnClickListener { onItemClick(listing) }

            itemHeart.setOnClickListener {
                // TODO: Handle favorite toggle
            }
        }

        private fun calculateRemainingTime(endTime: String): String {
            try {
                val endDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(endTime)
                val now = Date()

                if (endDate == null) return "N/A"

                val diff = endDate.time - now.time

                if (diff <= 0) return "Ended"

                val days = TimeUnit.MILLISECONDS.toDays(diff)
                val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60

                return when {
                    days > 0 -> "${days}d ${hours}h"
                    hours > 0 -> "${hours}h ${minutes}m"
                    else -> "${minutes}m"
                }
            } catch (e: Exception) {
                return "N/A"
            }
        }
    }
}