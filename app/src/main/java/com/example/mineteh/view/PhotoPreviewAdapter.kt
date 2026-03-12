package com.example.mineteh.view

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mineteh.R

class PhotoPreviewAdapter(
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<PhotoPreviewAdapter.ViewHolder>() {

    private val photos = mutableListOf<Uri>()

    fun submitList(newPhotos: List<Uri>) {
        photos.clear()
        photos.addAll(newPhotos)
        notifyDataSetChanged()
    }

    fun addPhoto(uri: Uri) {
        photos.add(uri)
        notifyItemInserted(photos.size - 1)
    }

    fun removePhoto(position: Int) {
        photos.removeAt(position)
        notifyItemRemoved(position)
    }

    fun getPhotos(): List<Uri> = photos.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo_preview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(photos[position], position)
    }

    override fun getItemCount() = photos.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imagePreview: ImageView = itemView.findViewById(R.id.imagePreview)
        private val btnRemove: ImageView = itemView.findViewById(R.id.btnRemovePhoto)

        fun bind(uri: Uri, position: Int) {
            Glide.with(itemView.context)
                .load(uri)
                .centerCrop()
                .into(imagePreview)

            btnRemove.setOnClickListener {
                onRemoveClick(position)
            }
        }
    }
}
