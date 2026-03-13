package com.example.mineteh.network

import com.example.mineteh.models.Listing
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
        
        // Parse images field - can be array of strings or array of objects with image_path
        val images = try {
            val imagesElement = jsonObject.get("images")
            when {
                imagesElement == null || imagesElement.isJsonNull -> null
                imagesElement.isJsonArray -> {
                    imagesElement.asJsonArray.mapNotNull { element ->
                        when {
                            element.isJsonPrimitive -> element.asString
                            element.isJsonObject -> element.asJsonObject.get("image_path")?.asString
                            else -> null
                        }
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
        
        // Parse seller - handle missing account_id
        val sellerElement = jsonObject.get("seller")
        val seller = if (sellerElement != null && !sellerElement.isJsonNull) {
            val sellerObj = sellerElement.asJsonObject
            Seller(
                accountId = sellerObj.get("account_id")?.takeIf { !it.isJsonNull }?.asInt,
                username = sellerObj.get("username").asString,
                firstName = sellerObj.get("first_name").asString,
                lastName = sellerObj.get("last_name").asString
            )
        } else {
            null
        }
        
        // Parse highest_bid - it's just a number (Double) from the API
        val highestBidAmount = try {
            val bidElement = jsonObject.get("highest_bid")
            when {
                bidElement == null || bidElement.isJsonNull -> null
                bidElement.isJsonPrimitive && bidElement.asJsonPrimitive.isNumber -> bidElement.asDouble
                else -> null
            }
        } catch (e: Exception) {
            null
        }
        
        // Parse bids array (for detail view)
        val bids = try {
            val bidsElement = jsonObject.get("bids")
            if (bidsElement != null && !bidsElement.isJsonNull && bidsElement.isJsonArray) {
                val listType = object : com.google.gson.reflect.TypeToken<List<com.example.mineteh.models.Bid>>() {}.type
                context.deserialize(bidsElement, listType)
            } else {
                null
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
            _image = jsonObject.get("image")?.takeIf { !it.isJsonNull }?.asString,
            _images = images,
            seller = seller,
            createdAt = jsonObject.get("created_at").asString,
            isFavorited = jsonObject.get("is_favorited")?.asBoolean ?: false,
            highestBidAmount = highestBidAmount,
            endTime = jsonObject.get("end_time")?.takeIf { !it.isJsonNull }?.asString,
            bids = bids
        )
    }
}
