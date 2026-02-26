package com.example.mineteh.view

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mineteh.view.ItemDetailActivity
import com.example.mineteh.R
import com.example.mineteh.model.ItemModel

class ItemAdapter(
    private var itemList: ArrayList<ItemModel>,
    private val isBidActivity: Boolean = false
) : RecyclerView.Adapter<ItemAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemName: TextView = itemView.findViewById(R.id.itemName)
        val itemPrice: TextView = itemView.findViewById(R.id.itemPrice)
        val itemLocation: TextView? = itemView.findViewById(R.id.itemLocation)
        val itemHeart: ImageView = itemView.findViewById(R.id.itemHeart)
        val itemImage: ImageView = itemView.findViewById(R.id.itemImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = itemList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemList[position]
        holder.itemName.text = item.name
        holder.itemPrice.text = "₱ ${item.price}"
        holder.itemLocation?.text = item.location

        // Load the image from ItemModel
        holder.itemImage.setImageResource(item.imageRes)

        updateHeartIcon(holder.itemHeart, item.isLiked)

        holder.itemHeart.setOnClickListener {
            item.isLiked = !item.isLiked
            updateHeartIcon(holder.itemHeart, item.isLiked)
        }

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = if (isBidActivity) {
                Intent(context, BidDetailActivity::class.java)
            } else {
                Intent(context, ItemDetailActivity::class.java)
            }
            intent.putExtra("item", item)
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

    fun updateList(newList: ArrayList<ItemModel>) {
        itemList = newList
        notifyDataSetChanged()
    }
}