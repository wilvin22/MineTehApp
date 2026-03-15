package com.example.mineteh.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
            val context = itemView.context
            
            itemPrice.text = "₱${listing.price}"
            itemName.text = listing.title
            itemLocation.text = "📍 ${listing.location}"

            // Set price color based on listing type
            val priceColor = if (listing.listingType == "BID") {
                context.getColor(R.color.purple)
            } else {
                context.getColor(R.color.md_tertiary)
            }
            itemPrice.setTextColor(priceColor)

            if (listing.listingType == "BID") {
                itemTypeBadge.text = "BID"
                itemTypeBadge.setBackgroundResource(R.drawable.badge_background_bid)
                auctionTimerLayout.visibility = View.VISIBLE
                listing.endTime?.let { endTime ->
                    itemAuctionTimer.text = calculateRemainingTime(endTime)
                }
            } else {
                itemTypeBadge.text = "FIXED"
                itemTypeBadge.setBackgroundResource(R.drawable.badge_background)
                auctionTimerLayout.visibility = View.GONE
            }

            // Load image using Glide with ImageUtils
            android.util.Log.d("ListingsAdapter", "Binding listing ${listing.id}: ${listing.title}")
            android.util.Log.d("ListingsAdapter", "  - image field: ${listing.image}")
            
            val imageUrl = com.example.mineteh.utils.ImageUtils.getFullImageUrl(listing.image)
            
            android.util.Log.d("ListingsAdapter", "  - Final image URL: $imageUrl")
            
            val glideUrl = imageUrl?.let {
                com.bumptech.glide.load.model.GlideUrl(
                    it,
                    com.bumptech.glide.load.model.LazyHeaders.Builder()
                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36")
                        .addHeader("Referer", "https://mineteh.infinityfree.me/")
                        .build()
                )
            }
            
            // Clear the ImageView first to prevent showing old images
            itemImage.setImageDrawable(null)
            
            Glide.with(itemView.context)
                .clear(itemImage)
            
            // Handle data URIs differently than regular URLs
            if (imageUrl != null && imageUrl.startsWith("data:image/")) {
                // Handle base64 data URI
                try {
                    val base64Data = imageUrl.substring(imageUrl.indexOf(",") + 1)
                    val imageBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                    
                    Glide.with(itemView.context)
                        .load(imageBytes)
                        .placeholder(R.drawable.dummyphoto)
                        .error(R.drawable.dummyphoto)
                        .transform(com.bumptech.glide.load.resource.bitmap.CenterCrop(), 
                                   com.bumptech.glide.load.resource.bitmap.RoundedCorners(48))
                        .addListener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                            override fun onLoadFailed(
                                e: com.bumptech.glide.load.engine.GlideException?,
                                model: Any?,
                                target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                                isFirstResource: Boolean
                            ): Boolean {
                                android.util.Log.e("ListingsAdapter", "Base64 image load FAILED for listing ${listing.id}", e)
                                return false
                            }

                            override fun onResourceReady(
                                resource: android.graphics.drawable.Drawable,
                                model: Any,
                                target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                                dataSource: com.bumptech.glide.load.DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                android.util.Log.d("ListingsAdapter", "Base64 image load SUCCESS for listing ${listing.id}")
                                return false
                            }
                        })
                        .into(itemImage)
                } catch (e: Exception) {
                    android.util.Log.e("ListingsAdapter", "Error decoding base64 image for listing ${listing.id}", e)
                    itemImage.setImageResource(R.drawable.dummyphoto)
                }
            } else {
                // Handle regular URLs
                Glide.with(itemView.context)
                    .load(glideUrl)
                    .placeholder(R.drawable.dummyphoto)
                    .error(R.drawable.dummyphoto)
                    .transform(com.bumptech.glide.load.resource.bitmap.CenterCrop(), 
                               com.bumptech.glide.load.resource.bitmap.RoundedCorners(48))
                    .addListener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                        override fun onLoadFailed(
                            e: com.bumptech.glide.load.engine.GlideException?,
                            model: Any?,
                            target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            android.util.Log.e("ListingsAdapter", "Image load FAILED for listing ${listing.id}: $imageUrl", e)
                            e?.logRootCauses("ListingsAdapter")
                            return false
                        }

                        override fun onResourceReady(
                            resource: android.graphics.drawable.Drawable,
                            model: Any,
                            target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                            dataSource: com.bumptech.glide.load.DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            android.util.Log.d("ListingsAdapter", "Image load SUCCESS for listing ${listing.id}: $imageUrl (source: $dataSource)")
                            return false
                        }
                    })
                    .into(itemImage)
            }

            itemHeart.setImageResource(
                if (listing.isFavorited) R.drawable.heart_red else R.drawable.heart
            )

            itemView.setOnClickListener { onItemClick(listing) }
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
