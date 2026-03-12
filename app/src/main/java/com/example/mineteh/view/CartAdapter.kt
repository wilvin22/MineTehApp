package com.example.mineteh.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.example.mineteh.R
import com.example.mineteh.models.CartItem

class CartAdapter(
    private val onQuantityChanged: (Int, Int) -> Unit,
    private val onRemoveClicked: (Int) -> Unit
) : ListAdapter<CartItem, CartAdapter.ViewHolder>(CartItemDiffCallback()) {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemImage: ImageView = itemView.findViewById(R.id.itemImage)
        val itemName: TextView = itemView.findViewById(R.id.itemName)
        val itemPrice: TextView = itemView.findViewById(R.id.itemPrice)
        val shopName: TextView = itemView.findViewById(R.id.shopName)
        val quantityText: TextView = itemView.findViewById(R.id.quantityText)
        val btnDecrease: ImageButton = itemView.findViewById(R.id.btnDecrease)
        val btnIncrease: ImageButton = itemView.findViewById(R.id.btnIncrease)
        val deleteButton: ImageView = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        
        holder.itemName.text = item.title
        holder.itemPrice.text = "₱ ${String.format("%.2f", item.price * item.quantity)}"
        holder.shopName.text = item.sellerName
        holder.quantityText.text = item.quantity.toString()

        // Load image
        val websiteUrl = "https://mineteh.infinityfree.me/home"
        val imageUrl = item.image?.let { imagePath ->
            when {
                imagePath.startsWith("http") -> imagePath
                else -> "$websiteUrl/$imagePath"
            }
        }

        if (imageUrl != null) {
            val glideUrl = GlideUrl(
                imageUrl,
                LazyHeaders.Builder()
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                    .addHeader("Referer", "https://mineteh.infinityfree.me/")
                    .build()
            )

            Glide.with(holder.itemView.context)
                .load(glideUrl)
                .placeholder(R.drawable.dummyphoto)
                .error(R.drawable.dummyphoto)
                .into(holder.itemImage)
        } else {
            holder.itemImage.setImageResource(R.drawable.dummyphoto)
        }

        // Quantity controls
        holder.btnDecrease.setOnClickListener {
            if (item.quantity > 1) {
                onQuantityChanged(item.listingId, item.quantity - 1)
            }
        }

        holder.btnIncrease.setOnClickListener {
            onQuantityChanged(item.listingId, item.quantity + 1)
        }

        // Delete button
        holder.deleteButton.setOnClickListener {
            onRemoveClicked(item.listingId)
        }
    }

    class CartItemDiffCallback : DiffUtil.ItemCallback<CartItem>() {
        override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem.listingId == newItem.listingId
        }

        override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem == newItem
        }
    }
}
