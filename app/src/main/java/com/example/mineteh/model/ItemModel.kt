package com.example.mineteh.model

import com.example.mineteh.R
import java.io.Serializable

data class ItemModel(
    val name: String,
    val description: String,
    val price: String,
    val location: String,
    var isLiked: Boolean = false,
    val shopName: String = "Shop Name",
    val imageRes: Int = R.drawable.ic_launcher_background
) : Serializable