package com.example.mineteh.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mineteh.R
import com.example.mineteh.model.ItemModel

class CartAdapter(private val cartList: List<ItemModel>) :
    RecyclerView.Adapter<CartAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkbox: CheckBox = itemView.findViewById(R.id.checkbox)
        val itemImage: ImageView = itemView.findViewById(R.id.itemImage)
        val shopName: TextView = itemView.findViewById(R.id.shopName)
        val itemName: TextView = itemView.findViewById(R.id.itemName)
        val itemLocation: TextView = itemView.findViewById(R.id.itemLocation)
        val itemPrice: TextView = itemView.findViewById(R.id.itemPrice)
        val deleteButton: ImageView = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = cartList[position]
        holder.shopName.text = item.shopName
        holder.itemName.text = item.name
        holder.itemLocation.text = item.location
        holder.itemPrice.text = "₱ ${item.price}"

        // Defaulting checkbox to true for demonstration
        holder.checkbox.isChecked = position < 2
    }

    override fun getItemCount(): Int = cartList.size
}