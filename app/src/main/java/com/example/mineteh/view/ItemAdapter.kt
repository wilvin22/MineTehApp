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
    private val isBidActivity: Boolean = false
) : RecyclerView.Adapter<ItemAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemName: TextView = itemView.findViewById(R.id.itemName)
        val itemPrice: TextView = itemView.findViewById(R.id.itemPrice)
        val itemLocation: TextView? = itemView.findViewById(R.id.itemLocation)
        val itemHeart: ImageView = itemView.findViewById(R.id.itemHeart)
        val itemImage: ImageView = itemView.findViewById(R.id.itemImage)
        val itemTypeBadge: TextView = itemView.findViewById(R.id.itemTypeBadge)
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

        // Use public website URL for images
        val websiteUrl = "https://mineteh.infinityfree.me/home"
        
        val imageUrl = item.image?.let { imagePath ->
            when {
                imagePath.startsWith("http") -> imagePath
                else -> "$websiteUrl/$imagePath"
            }
        }

        android.util.Log.d("ItemAdapter", "  - Final image URL: $imageUrl")

        val glideUrl = imageUrl?.let {
            com.bumptech.glide.load.model.GlideUrl(
                it,
                com.bumptech.glide.load.model.LazyHeaders.Builder()
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36")
                    .addHeader("Referer", "https://mineteh.infinityfree.me/")
                    .build()
            )
        }

        Glide.with(holder.itemView.context)
            .load(glideUrl)
            .placeholder(R.drawable.dummyphoto)
            .error(R.drawable.dummyphoto)
            .transform(com.bumptech.glide.load.resource.bitmap.CenterCrop(), 
                       com.bumptech.glide.load.resource.bitmap.RoundedCorners(48))
            .skipMemoryCache(true)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
            .addListener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                override fun onLoadFailed(
                    e: com.bumptech.glide.load.engine.GlideException?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    android.util.Log.e("ItemAdapter", "Image load FAILED for listing ${item.id}: $imageUrl", e)
                    e?.logRootCauses("ItemAdapter")
                    return false
                }

                override fun onResourceReady(
                    resource: android.graphics.drawable.Drawable,
                    model: Any,
                    target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                    dataSource: com.bumptech.glide.load.DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    android.util.Log.d("ItemAdapter", "Image load SUCCESS for listing ${item.id}: $imageUrl (source: $dataSource)")
                    return false
                }
            })
            .into(holder.itemImage)

        updateHeartIcon(holder.itemHeart, item.isFavorited)

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
}
