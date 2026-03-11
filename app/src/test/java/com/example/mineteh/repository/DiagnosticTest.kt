package com.example.mineteh.repository

import com.example.mineteh.models.ApiResponse
import com.example.mineteh.models.Listing
import com.example.mineteh.network.ApiResponseDeserializer
import com.example.mineteh.network.ListingDeserializer
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.junit.Test
import java.lang.reflect.Type

/**
 * Diagnostic test to understand what's happening with the current implementation
 */
class DiagnosticTest {

    @Test
    fun `diagnostic - test simple empty array parsing`() {
        println("=== DIAGNOSTIC: Simple Empty Array ===")
        
        val gson = Gson() // Plain Gson without custom deserializers
        val emptyArrayJson = """{"success": true, "message": null, "data": []}"""
        
        try {
            val listType: Type = object : TypeToken<ApiResponse<List<Listing>>>() {}.type
            val result: ApiResponse<List<Listing>> = gson.fromJson(emptyArrayJson, listType)
            println("✓ Plain Gson: Empty array parsed successfully")
            println("  - Success: ${result.success}")
            println("  - Data: ${result.data}")
            println("  - Data size: ${result.data?.size}")
        } catch (e: Exception) {
            println("❌ Plain Gson failed: ${e::class.simpleName}: ${e.message}")
            e.printStackTrace()
        }
    }

    @Test
    fun `diagnostic - test with ApiResponseDeserializer only`() {
        println("=== DIAGNOSTIC: ApiResponseDeserializer Only ===")
        
        val listType: Type = object : TypeToken<ApiResponse<List<Listing>>>() {}.type
        val gson = GsonBuilder()
            .registerTypeAdapter(listType, ApiResponseDeserializer())
            .create()
        
        val emptyArrayJson = """{"success": true, "message": null, "data": []}"""
        
        try {
            val result: ApiResponse<List<Listing>> = gson.fromJson(emptyArrayJson, listType)
            println("✓ ApiResponseDeserializer: Empty array parsed successfully")
            println("  - Success: ${result.success}")
            println("  - Data: ${result.data}")
            println("  - Data size: ${result.data?.size}")
        } catch (e: Exception) {
            println("❌ ApiResponseDeserializer failed: ${e::class.simpleName}: ${e.message}")
            e.printStackTrace()
        }
    }

    @Test
    fun `diagnostic - test with both deserializers`() {
        println("=== DIAGNOSTIC: Both Deserializers ===")
        
        val listType: Type = object : TypeToken<ApiResponse<List<Listing>>>() {}.type
        val gson = GsonBuilder()
            .registerTypeAdapter(listType, ApiResponseDeserializer())
            .registerTypeAdapter(Listing::class.java, ListingDeserializer())
            .create()
        
        val emptyArrayJson = """{"success": true, "message": null, "data": []}"""
        
        try {
            val result: ApiResponse<List<Listing>> = gson.fromJson(emptyArrayJson, listType)
            println("✓ Both deserializers: Empty array parsed successfully")
            println("  - Success: ${result.success}")
            println("  - Data: ${result.data}")
            println("  - Data size: ${result.data?.size}")
        } catch (e: Exception) {
            println("❌ Both deserializers failed: ${e::class.simpleName}: ${e.message}")
            e.printStackTrace()
        }
    }

    @Test
    fun `diagnostic - test simple valid listing`() {
        println("=== DIAGNOSTIC: Simple Valid Listing ===")
        
        val listType: Type = object : TypeToken<ApiResponse<List<Listing>>>() {}.type
        val gson = GsonBuilder()
            .registerTypeAdapter(listType, ApiResponseDeserializer())
            .registerTypeAdapter(Listing::class.java, ListingDeserializer())
            .create()
        
        val validJson = """
        {
            "success": true,
            "message": null,
            "data": [
                {
                    "id": 1,
                    "title": "Test",
                    "description": "Test",
                    "price": 100.0,
                    "location": "Test",
                    "category": "Test",
                    "listing_type": "FIXED",
                    "status": "active",
                    "created_at": "2024-01-01",
                    "is_favorited": false
                }
            ]
        }
        """.trimIndent()
        
        try {
            val result: ApiResponse<List<Listing>> = gson.fromJson(validJson, listType)
            println("✓ Valid listing parsed successfully")
            println("  - Success: ${result.success}")
            println("  - Data size: ${result.data?.size}")
            if (result.data?.isNotEmpty() == true) {
                val listing = result.data!![0]
                println("  - First listing ID: ${listing.id}")
                println("  - First listing title: ${listing.title}")
            }
        } catch (e: Exception) {
            println("❌ Valid listing failed: ${e::class.simpleName}: ${e.message}")
            e.printStackTrace()
        }
    }
}