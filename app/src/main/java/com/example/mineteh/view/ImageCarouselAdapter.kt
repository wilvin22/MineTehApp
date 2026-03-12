package com.example.mineteh.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.example.mineteh.R
import com.example.mineteh.models.ListingImage

class ImageCarouselAdapter(
    private val images: List<ListingImage>,
    private val context: Context
) : RecyclerView.Adapter<ImageCarouselAdapter.ImageViewHolder>() {

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val carouselImage: ImageView = itemView.findViewById(R.id.carouselImage)
        val imageLoadingProgress: ProgressBar = itemView.findViewById(R.id.imageLoadingProgress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_carousel, parent, false)
        return ImageViewHolder(view)
    }

    override fun getItemCount(): Int = images.size

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val image = images[position]
        
        // Use public website URL for images
        val websiteUrl = "https://mineteh.infinityfree.me/home"
        val imageUrl = when {
            image.imagePath.startsWith("http") -> image.imagePath
            else -> "$websiteUrl/${image.imagePath}"
        }

        val glideUrl = GlideUrl(
            imageUrl,
            LazyHeaders.Builder()
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .addHeader("Referer", "https://mineteh.infinityfree.me/")
                .build()
        )

        holder.imageLoadingProgress.visibility = View.VISIBLE

        Glide.with(context)
            .load(glideUrl)
            .placeholder(R.drawable.dummyphoto)
            .error(R.drawable.dummyphoto)
            .into(holder.carouselImage)
            .clearOnDetach()

        holder.imageLoadingProgress.visibility = View.GONE
    }
}
