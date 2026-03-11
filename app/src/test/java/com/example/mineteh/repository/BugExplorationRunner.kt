package com.example.mineteh.repository

import com.example.mineteh.models.ApiResponse
import com.example.mineteh.models.Listing
import com.example.mineteh.network.ApiResponseDeserializer
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

/**
 * Manual Bug Exploration Runner
 * 
 * This class can be run manually to explore the bug condition
 * without requiring JUnit test execution
 */
object BugExplorationRunner {
    
    @JvmStatic
    fun main(args: Array<String>) {
        println("=== Bug Condition Exploration for Homepage JSON Parsing Fix ===")
        println()
        
        // Test 1: Raw Gson (should fail)
        testRawGsonWithMalformedJson()
        
        // Test 2: Current deserializer (should pass)
        testCurrentDeserializerWithMalformedJson()
        
        // Test 3: Malformed listing objects
        testMalformedListingObjects()
        
        println("\n=== Exploration Complete ===")
    }
    
    private fun testRawGsonWithMalformedJson() {
        println("TEST 1: Raw Gson with malformed JSON (EXPECTED TO FAIL)")
        val malformedJson = """{"success": true, "message": null, "data": {}}"""
        val rawGson = Gson()
        
        try {
            val listType: Type = object : TypeToken<ApiResponse<List<Listing>>>() {}.type
            val result: ApiResponse<List<Listing>> = rawGson.fromJson(malformedJson, listType)
            println("❌ CRITICAL: Raw Gson succeeded - bug condition not reproduced!")
        } catch (e: IllegalStateException) {
            println("✅ COUNTEREXAMPLE FOUND: ${e.message}")
            println("✅ Bug condition confirmed - raw Gson fails with malformed JSON")
        } catch (e: Exception) {
            println("⚠️  Unexpected exception: ${e::class.simpleName}: ${e.message}")
        }
        println()
    }
    
    private fun testCurrentDeserializerWithMalformedJson() {
        println("TEST 2: Current ApiResponseDeserializer with malformed JSON")
        val listType: Type = object : TypeToken<ApiResponse<List<Listing>>>() {}.type
        val gson = GsonBuilder()
            .registerTypeAdapter(
                listType,
                ApiResponseDeserializer()
            )
            .create()
        
        val testCases = listOf(
            "Empty object" to """{"success": true, "message": null, "data": {}}""",
            "Null data" to """{"success": true, "message": null, "data": null}""",
            "Wrapped array" to """{"success": true, "message": null, "data": {"listings": [{"id": 1, "title": "Test"}]}}""",
            "Single object" to """{"success": true, "message": null, "data": {"id": 1, "title": "Test"}}"""
        )
        
        testCases.forEach { (name, json) ->
            try {
                val result: ApiResponse<List<Listing>> = gson.fromJson(json, listType)
                println("✅ $name: Handled gracefully - success=${result.success}, data size=${result.data?.size}")
            } catch (e: Exception) {
                println("❌ $name: Failed - ${e::class.simpleName}: ${e.message}")
            }
        }
        println()
    }
    
    private fun testMalformedListingObjects() {
        println("TEST 3: Malformed Listing objects in array")
        val listType: Type = object : TypeToken<ApiResponse<List<Listing>>>() {}.type
        val gson = GsonBuilder()
            .registerTypeAdapter(
                listType,
                ApiResponseDeserializer()
            )
            .create()
        
        // This might be where the actual bug occurs - malformed individual listings
        val malformedListingJson = """{"success": true, "message": null, "data": [{"id": 1, "title": "Test"}]}"""
        
        try {
            val result: ApiResponse<List<Listing>> = gson.fromJson(malformedListingJson, listType)
            println("✅ Malformed listing: Handled gracefully - data size=${result.data?.size}")
        } catch (e: Exception) {
            println("❌ POTENTIAL BUG LOCATION: Malformed listing failed - ${e::class.simpleName}: ${e.message}")
            println("   This could be where the actual crash occurs!")
        }
    }
}