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

class ItemAdapter(
    private var itemList: List<Listing> = emptyList(),
    private val isBidActivity: Boolean = false,
    private var currentUserId: Int = -1,
    private val onFavoriteToggle: ((Listing) -> Unit)? = null
) : RecyclerView.Adapter<ItemAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemName: TextView = itemView.findViewById(R.id.itemName)
        val itemPrice: TextView = itemView.findViewById(R.id.itemPrice)
        val itemLocation: TextView? = itemView.findViewById(R.id.itemLocation)
        val itemHeart: ImageView = itemView.findViewById(R.id.itemHeart)
        val heartContainer: android.widget.FrameLayout = itemView.findViewById(R.id.heartContainer)
        val itemImage: ImageView = itemView.findViewById(R.id.itemImage)
        val itemTypeBadge: TextView = itemView.findViewById(R.id.itemTypeBadge)
        val yourListingBadge: TextView = itemView.findViewById(R.id.yourListingBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = itemList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemList[position]
        val context = holder.itemView.context
        
        holder.itemName.text = item.title ?: "No Title"
        holder.itemPrice.text = "₱ ${String.format("%.2f", item.price)}"
        holder.itemLocation?.text = "📍 ${item.location ?: "Unknown"}"
        holder.itemTypeBadge.text = item.listingType ?: "FIXED"

        // Show "Your Listing" badge and hide heart if this item belongs to the current user
        val isOwner = currentUserId != -1 && item.seller?.accountId == currentUserId
        holder.yourListingBadge.visibility = if (isOwner) View.VISIBLE else View.GONE
        holder.heartContainer.visibility = if (isOwner) View.GONE else View.VISIBLE

        // Set price color based on listing type
        val priceColor = if (item.listingType == "BID") {
            context.getColor(R.color.purple)
        } else {
            context.getColor(R.color.md_tertiary)
        }
        holder.itemPrice.setTextColor(priceColor)

        android.util.Log.d("ItemAdapter", "Binding item ${item.id}: ${item.title}")
        android.util.Log.d("ItemAdapter", "  - image field: ${item.image}")
        android.util.Log.d("ItemAdapter", "  - images list size: ${item.images?.size}")
        android.util.Log.d("ItemAdapter", "  - images list: ${item.images}")

        // Use ImageUtils for proper URL construction
        val imageUrl = com.example.mineteh.utils.ImageUtils.getFullImageUrl(item.image)

        android.util.Log.d("ItemAdapter", "  - Final image URL: $imageUrl")

        // Clear the ImageView first to prevent showing old images
        holder.itemImage.setImageDrawable(null)
        
        Glide.with(holder.itemView.context)
            .clear(holder.itemImage)

        // Handle different image formats
        when {
            imageUrl != null && imageUrl.startsWith("data:image/") -> {
                // Handle base64 data URI
                try {
                    val base64Data = imageUrl.substring(imageUrl.indexOf(",") + 1)
                    val imageBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                    
                    Glide.with(holder.itemView.context)
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
                                android.util.Log.w("ItemAdapter", "Base64 image load failed for listing ${item.id}, using placeholder")
                                return false
                            }

                            override fun onResourceReady(
                                resource: android.graphics.drawable.Drawable,
                                model: Any,
                                target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                                dataSource: com.bumptech.glide.load.DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                android.util.Log.d("ItemAdapter", "Base64 image loaded successfully for listing ${item.id}")
                                return false
                            }
                        })
                        .into(holder.itemImage)
                } catch (e: Exception) {
                    android.util.Log.w("ItemAdapter", "Error decoding base64 image for listing ${item.id}, using placeholder")
                    holder.itemImage.setImageResource(R.drawable.dummyphoto)
                }
            }
            
            imageUrl != null -> {
                // Handle regular URLs - but expect many to fail (old listings)
                val glideUrl = com.bumptech.glide.load.model.GlideUrl(
                    imageUrl,
                    com.bumptech.glide.load.model.LazyHeaders.Builder()
                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36")
                        .addHeader("Referer", "https://mineteh.infinityfree.me/")
                        .build()
                )

                Glide.with(holder.itemView.context)
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
                            // Don't log errors for old listings - this is expected
                            android.util.Log.d("ItemAdapter", "Image not available for listing ${item.id} (likely old listing), using placeholder")
                            return false
                        }

                        override fun onResourceReady(
                            resource: android.graphics.drawable.Drawable,
                            model: Any,
                            target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                            dataSource: com.bumptech.glide.load.DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            android.util.Log.d("ItemAdapter", "Image loaded successfully for listing ${item.id}")
                            return false
                        }
                    })
                    .into(holder.itemImage)
            }
            
            else -> {
                // No image URL available
                android.util.Log.d("ItemAdapter", "No image URL for listing ${item.id}, using placeholder")
                holder.itemImage.setImageResource(R.drawable.dummyphoto)
            }
        }

        updateHeartIcon(holder.itemHeart, item.isFavorited)

        holder.itemHeart.setOnClickListener {
            val newState = !item.isFavorited
            // Update the list copy so rebinds stay in sync
            itemList = itemList.toMutableList().also { it[holder.adapterPosition] = item.copy(isFavorited = newState) }
            updateHeartIcon(holder.itemHeart, newState)
            onFavoriteToggle?.invoke(item)
        }

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = if (isBidActivity || item.listingType == "BID") {
                Intent(context, BidDetailActivity::class.java)
            } else {
                Intent(context, ItemDetailActivity::class.java)
            }
            intent.putExtra("listing_id", item.id)
            context.startActivity(intent)
        }
    }

    private fun updateHeartIcon(imageView: ImageView, isLiked: Boolean) {
        if (isLiked) {
            imageView.setImageResource(R.drawable.heart_red)
        } else {
            imageView.setImageResource(R.drawable.heart)
        }
    }

    fun updateList(newList: List<Listing>) {
        itemList = newList
        notifyDataSetChanged()
    }

    fun setCurrentUserId(userId: Int) {
        currentUserId = userId
        notifyDataSetChanged()
    }
}
