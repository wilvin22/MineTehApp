package com.example.mineteh.view

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mineteh.R

/**
 * Adapter for the edit listing photo row.
 * Items are either an existing URL string or a new local Uri.
 */
class EditPhotoAdapter(
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<EditPhotoAdapter.ViewHolder>() {

    sealed class PhotoItem {
        data class ExistingUrl(val url: String) : PhotoItem()
        data class NewUri(val uri: Uri) : PhotoItem()
    }

    private val items = mutableListOf<PhotoItem>()

    fun setExistingPhotos(urls: List<String>) {
        items.clear()
        urls.forEach { items.add(PhotoItem.ExistingUrl(it)) }
        notifyDataSetChanged()
    }

    fun addNewUri(uri: Uri) {
        items.add(PhotoItem.NewUri(uri))
        notifyItemInserted(items.size - 1)
    }

    fun removeAt(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    fun getExistingUrls(): List<String> =
        items.filterIsInstance<PhotoItem.ExistingUrl>().map { it.url }

    fun getNewUris(): List<Uri> =
        items.filterIsInstance<PhotoItem.NewUri>().map { it.uri }

    fun getItemCount_() = items.size

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo_preview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imagePreview: ImageView = itemView.findViewById(R.id.imagePreview)
        private val btnRemove: ImageView = itemView.findViewById(R.id.btnRemovePhoto)

        fun bind(item: PhotoItem, position: Int) {
            when (item) {
                is PhotoItem.ExistingUrl -> Glide.with(itemView.context)
                    .load(item.url)
                    .centerCrop()
                    .placeholder(R.drawable.dummyphoto)
                    .into(imagePreview)
                is PhotoItem.NewUri -> Glide.with(itemView.context)
                    .load(item.uri)
                    .centerCrop()
                    .into(imagePreview)
            }
            btnRemove.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onRemoveClick(pos)
            }
        }
    }
}
