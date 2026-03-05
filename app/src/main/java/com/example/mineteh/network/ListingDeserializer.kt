package com.example.mineteh.network

import com.example.mineteh.models.Bid
import com.example.mineteh.models.Listing
import com.example.mineteh.models.ListingImage
import com.example.mineteh.models.Seller
import com.google.gson.*
import java.lang.reflect.Type

class ListingDeserializer : JsonDeserializer<Listing> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Listing {
        val jsonObject = json.asJsonObject
        
        // Parse images field - handle if it's not an array
        val images = try {
            val imagesElement = jsonObject.get("images")
            when {
                imagesElement == null || imagesElement.isJsonNull -> null
                imagesElement.isJsonArray -> {
                    imagesElement.asJsonArray.map { 
                        context.deserialize<ListingImage>(it, ListingImage::class.java)
                    }
                }
                else -> null // If it's an object or string, treat as null
            }
        } catch (e: Exception) {
            null
        }
        
        return Listing(
            id = jsonObject.get("id").asInt,
            title = jsonObject.get("title").asString,
            description = jsonObject.get("description").asString,
            price = jsonObject.get("price").asDouble,
            location = jsonObject.get("location").asString,
            category = jsonObject.get("category").asString,
            listingType = jsonObject.get("listing_type").asString,
            status = jsonObject.get("status").asString,
            image = jsonObject.get("image")?.takeIf { !it.isJsonNull }?.asString,
            images = images,
            seller = context.deserialize(jsonObject.get("seller"), Seller::class.java),
            createdAt = jsonObject.get("created_at").asString,
            isFavorited = jsonObject.get("is_favorited")?.asBoolean ?: false,
            highestBid = context.deserialize(jsonObject.get("highest_bid"), Bid::class.java),
            endTime = jsonObject.get("end_time")?.takeIf { !it.isJsonNull }?.asString
        )
    }
}
