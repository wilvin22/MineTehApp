package com.example.mineteh.view

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mineteh.R
import com.example.mineteh.models.Listing
import com.google.android.material.button.MaterialButton

class MyListingsAdapter(
    private val onViewClick: (Listing) -> Unit,
    private val onEditClick: (Listing) -> Unit,
    private val onStatusChange: (Listing, String) -> Unit
) : RecyclerView.Adapter<MyListingsAdapter.ViewHolder>() {

    private var listings = listOf<Listing>()
    private val statusOptions = listOf("Active", "Inactive", "Sold")

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.listingImage)
        val title: TextView = view.findViewById(R.id.listingTitle)
        val price: TextView = view.findViewById(R.id.listingPrice)
        val status: TextView = view.findViewById(R.id.listingStatus)
        val type: TextView = view.findViewById(R.id.listingType)
        val location: TextView = view.findViewById(R.id.listingLocation)
        val btnView: MaterialButton = view.findViewById(R.id.btnViewListing)
        val btnEdit: MaterialButton = view.findViewById(R.id.btnEditListing)
        val spinnerStatus: Spinner = view.findViewById(R.id.spinnerStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_listing, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val listing = listings[position]

        holder.title.text = listing.title
        holder.price.text = "₱${String.format("%.2f", listing.price)}"
        holder.type.text = if (listing.listingType == "BID") "BID" else "FIXED"
        holder.location.text = listing.location

        // Status badge color
        val statusLower = listing.status.lowercase()
        val (badgeColor, badgeText) = when (statusLower) {
            "active"   -> Pair("#4CAF50", "Active")
            "sold"     -> Pair("#F44336", "Sold")
            else       -> Pair("#9E9E9E", "Inactive")
        }
        holder.status.text = badgeText
        holder.status.setBackgroundColor(Color.parseColor(badgeColor))

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

        // Spinner setup — suppress listener during programmatic selection
        val spinnerAdapter = ArrayAdapter(
            holder.itemView.context,
            android.R.layout.simple_spinner_item,
            statusOptions
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        holder.spinnerStatus.adapter = spinnerAdapter

        val currentIndex = statusOptions.indexOfFirst { it.equals(listing.status, ignoreCase = true) }
            .takeIf { it >= 0 } ?: 0

        holder.spinnerStatus.tag = false // suppress listener
        holder.spinnerStatus.setSelection(currentIndex)
        holder.spinnerStatus.tag = true  // enable listener

        holder.spinnerStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                if (holder.spinnerStatus.tag == true) {
                    val selected = statusOptions[pos].lowercase()
                    if (!selected.equals(listing.status, ignoreCase = true)) {
                        onStatusChange(listing, selected)
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        holder.btnView.setOnClickListener { onViewClick(listing) }
        holder.btnEdit.setOnClickListener { onEditClick(listing) }
    }

    override fun getItemCount() = listings.size

    fun submitList(newListings: List<Listing>) {
        listings = newListings
        notifyDataSetChanged()
    }
}
