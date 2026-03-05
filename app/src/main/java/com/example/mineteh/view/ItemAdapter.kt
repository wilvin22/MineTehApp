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
        holder.itemName.text = item.title ?: "No Title"
        holder.itemPrice.text = "₱ ${String.format("%.2f", item.price)}"
        holder.itemLocation?.text = "📍 ${item.location ?: "Unknown"}"
        holder.itemTypeBadge.text = item.listingType ?: "FIXED"

        // Construct the full image URL
        val baseUrl = "http://192.168.18.4/MineTeh/"
        val imageUrl = item.image?.let { 
            when {
                it.startsWith("http") -> it
                it.startsWith("../") -> baseUrl + it.removePrefix("../")
                else -> baseUrl + it
            }
        }

        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(R.drawable.dummyphoto)
            .error(R.drawable.dummyphoto)
            .skipMemoryCache(true) // Disable memory cache to ensure fresh images
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
