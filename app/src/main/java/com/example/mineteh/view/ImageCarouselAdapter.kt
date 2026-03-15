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

class ImageCarouselAdapter(
    private val images: List<String>,
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
        val imageUrl = images[position]
        
        holder.imageLoadingProgress.visibility = View.VISIBLE

        // Handle different image formats
        when {
            imageUrl.startsWith("data:image/") -> {
                // Handle base64 data URI
                try {
                    val base64Data = imageUrl.substring(imageUrl.indexOf(",") + 1)
                    val imageBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                    
                    Glide.with(context)
                        .load(imageBytes)
                        .placeholder(R.drawable.dummyphoto)
                        .error(R.drawable.dummyphoto)
                        .addListener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                            override fun onLoadFailed(
                                e: com.bumptech.glide.load.engine.GlideException?,
                                model: Any?,
                                target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                                isFirstResource: Boolean
                            ): Boolean {
                                android.util.Log.w("ImageCarouselAdapter", "Base64 image load failed, using placeholder")
                                holder.imageLoadingProgress.visibility = View.GONE
                                return false
                            }

                            override fun onResourceReady(
                                resource: android.graphics.drawable.Drawable,
                                model: Any,
                                target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                                dataSource: com.bumptech.glide.load.DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                android.util.Log.d("ImageCarouselAdapter", "Base64 image loaded successfully")
                                holder.imageLoadingProgress.visibility = View.GONE
                                return false
                            }
                        })
                        .into(holder.carouselImage)
                        .clearOnDetach()
                } catch (e: Exception) {
                    android.util.Log.w("ImageCarouselAdapter", "Error decoding base64 image, using placeholder")
                    holder.carouselImage.setImageResource(R.drawable.dummyphoto)
                    holder.imageLoadingProgress.visibility = View.GONE
                }
            }
            
            else -> {
                // Handle regular URLs - but expect many to fail (old listings)
                val glideUrl = GlideUrl(
                    imageUrl,
                    LazyHeaders.Builder()
                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                        .addHeader("Referer", "https://mineteh.infinityfree.me/")
                        .build()
                )

                Glide.with(context)
                    .load(glideUrl)
                    .placeholder(R.drawable.dummyphoto)
                    .error(R.drawable.dummyphoto)
                    .addListener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                        override fun onLoadFailed(
                            e: com.bumptech.glide.load.engine.GlideException?,
                            model: Any?,
                            target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            // Don't log errors for old listings - this is expected
                            android.util.Log.d("ImageCarouselAdapter", "Image not available (likely old listing), using placeholder")
                            holder.imageLoadingProgress.visibility = View.GONE
                            return false
                        }

                        override fun onResourceReady(
                            resource: android.graphics.drawable.Drawable,
                            model: Any,
                            target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                            dataSource: com.bumptech.glide.load.DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            android.util.Log.d("ImageCarouselAdapter", "Image loaded successfully")
                            holder.imageLoadingProgress.visibility = View.GONE
                            return false
                        }
                    })
                    .into(holder.carouselImage)
                    .clearOnDetach()
            }
        }
    }
}
