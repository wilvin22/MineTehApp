package com.example.mineteh.view

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mineteh.R
import com.example.mineteh.models.Listing

class MyListingsAdapter(
    private val onViewClick: (Listing) -> Unit,
    private val onEditClick: (Listing) -> Unit,
    private val onToggleStatusClick: (Listing) -> Unit,
    private val onDeleteClick: (Listing) -> Unit
) : RecyclerView.Adapter<MyListingsAdapter.ViewHolder>() {

    private var listings = listOf<Listing>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.listingImage)
        val title: TextView = view.findViewById(R.id.listingTitle)
        val price: TextView = view.findViewById(R.id.listingPrice)
        val status: TextView = view.findViewById(R.id.listingStatus)
        val type: TextView = view.findViewById(R.id.listingType)
        val btnView: Button = view.findViewById(R.id.btnViewListing)
        val btnEdit: Button = view.findViewById(R.id.btnEditListing)
        val btnToggleStatus: Button = view.findViewById(R.id.btnToggleStatus)
        val btnDelete: Button = view.findViewById(R.id.btnDeleteListing)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_listing, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val listing = listings[position]
        
        holder.title.text = listing.title
        holder.price.text = "₱ ${String.format("%.2f", listing.price)}"
        holder.type.text = if (listing.listingType == "BID") "Auction" else "Fixed Price"
        
        // Status
        val isActive = listing.status.equals("active", ignoreCase = true)
        holder.status.text = if (isActive) "Active" else "Inactive"
        holder.status.setTextColor(if (isActive) Color.parseColor("#4CAF50") else Color.GRAY)
        
        // Load image
        if (!listing.image.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(listing.image)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(holder.image)
        } else {
            holder.image.setImageResource(R.drawable.ic_launcher_background)
        }
        
        // Toggle status button text
        holder.btnToggleStatus.text = if (isActive) "Disable" else "Enable"
        
        // Click listeners
        holder.btnView.setOnClickListener { onViewClick(listing) }
        holder.btnEdit.setOnClickListener { onEditClick(listing) }
        holder.btnToggleStatus.setOnClickListener { onToggleStatusClick(listing) }
        holder.btnDelete.setOnClickListener { onDeleteClick(listing) }
    }

    override fun getItemCount(): Int = listings.size
    
    fun submitList(newListings: List<Listing>) {
        listings = newListings
        notifyDataSetChanged()
    }
}
