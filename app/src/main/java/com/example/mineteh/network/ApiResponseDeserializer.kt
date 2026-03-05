package com.example.mineteh.network

import com.example.mineteh.models.ApiResponse
import com.example.mineteh.models.Listing
import com.google.gson.*
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class ApiResponseDeserializer : JsonDeserializer<ApiResponse<List<Listing>>> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): ApiResponse<List<Listing>> {
        val jsonObject = json.asJsonObject
        val success = jsonObject.get("success")?.asBoolean ?: false
        val message = jsonObject.get("message")?.asString
        val dataElement = jsonObject.get("data")

        val data: List<Listing>? = if (dataElement != null && dataElement.isJsonArray) {
            val listType = object : com.google.gson.reflect.TypeToken<List<Listing>>() {}.type
            context.deserialize(dataElement, listType)
        } else {
            // If data is not an array (e.g. {}, null, or malformed), return empty list for successful response
            // or null for error response.
            if (success) emptyList() else null
        }

        return ApiResponse(success, message, data)
    }
}
