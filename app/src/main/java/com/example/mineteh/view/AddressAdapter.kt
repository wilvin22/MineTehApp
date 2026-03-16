package com.example.mineteh.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mineteh.R
import com.example.mineteh.models.UserAddress

class AddressAdapter(
    private val onAddressClick: (UserAddress) -> Unit,
    private val onEditClick: (UserAddress) -> Unit,
    private val onDeleteClick: (UserAddress) -> Unit,
    private val isSelectionMode: Boolean = false
) : RecyclerView.Adapter<AddressAdapter.AddressViewHolder>() {

    private var addresses = listOf<UserAddress>()
    private var selectedAddressId: Int? = null

    fun submitList(newAddresses: List<UserAddress>) {
        addresses = newAddresses
        notifyDataSetChanged()
    }

    fun setSelectedAddress(addressId: Int?) {
        selectedAddressId = addressId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_address, parent, false)
        return AddressViewHolder(view)
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        holder.bind(addresses[position])
    }

    override fun getItemCount() = addresses.size

    inner class AddressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val addressTypeIcon: TextView = itemView.findViewById(R.id.addressTypeIcon)
        private val addressType: TextView = itemView.findViewById(R.id.addressType)
        private val defaultBadge: TextView = itemView.findViewById(R.id.defaultBadge)
        private val recipientName: TextView = itemView.findViewById(R.id.recipientName)
        private val phoneNumber: TextView = itemView.findViewById(R.id.phoneNumber)
        private val fullAddress: TextView = itemView.findViewById(R.id.fullAddress)
        private val btnEditAddress: ImageView = itemView.findViewById(R.id.btnEditAddress)
        private val btnDeleteAddress: ImageView = itemView.findViewById(R.id.btnDeleteAddress)
        private val selectionIndicator: View = itemView.findViewById(R.id.selectionIndicator)

        fun bind(address: UserAddress) {
            // Set address type icon and text
            when (address.addressType.lowercase()) {
                "home" -> {
                    addressTypeIcon.text = "🏠"
                    addressType.text = "Home"
                }
                "work" -> {
                    addressTypeIcon.text = "🏢"
                    addressType.text = "Work"
                }
                else -> {
                    addressTypeIcon.text = "📍"
                    addressType.text = "Other"
                }
            }

            // Show default badge
            defaultBadge.visibility = if (address.isDefault) View.VISIBLE else View.GONE

            // Set address details
            recipientName.text = address.recipientName
            phoneNumber.text = address.phoneNumber ?: "No phone number"
            fullAddress.text = address.fullAddress

            // Handle selection mode
            if (isSelectionMode) {
                val isSelected = selectedAddressId == address.addressId
                selectionIndicator.visibility = if (isSelected) View.VISIBLE else View.GONE
                
                // Hide action buttons in selection mode
                btnEditAddress.visibility = View.GONE
                btnDeleteAddress.visibility = View.GONE
                
                itemView.setOnClickListener {
                    selectedAddressId = address.addressId
                    onAddressClick(address)
                    notifyDataSetChanged()
                }
            } else {
                selectionIndicator.visibility = View.GONE
                btnEditAddress.visibility = View.VISIBLE
                btnDeleteAddress.visibility = View.VISIBLE
                
                // Set click listeners for action buttons
                btnEditAddress.setOnClickListener { onEditClick(address) }
                btnDeleteAddress.setOnClickListener { onDeleteClick(address) }
                
                itemView.setOnClickListener { onAddressClick(address) }
            }
        }
    }
}