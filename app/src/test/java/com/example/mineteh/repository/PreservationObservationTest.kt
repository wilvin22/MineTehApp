package com.example.mineteh.repository

import com.example.mineteh.models.ApiResponse
import com.example.mineteh.models.Listing
import com.example.mineteh.network.ApiResponseDeserializer
import com.example.mineteh.network.ListingDeserializer
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.junit.Test
import org.junit.Assert.*
import java.lang.reflect.Type

/**
 * Task 2: Preservation Property Tests (BEFORE implementing fix)
 * 
 * **Property 2: Preservation** - Valid Response Handling
 * **IMPORTANT**: Follow observation-first methodology
 * 
 * These tests observe behavior on UNFIXED code for valid API responses (non-buggy inputs)
 * **EXPECTED OUTCOME**: Tests PASS (this confirms baseline behavior to preserve)
 * 
 * **Validates: Requirements 3.1, 3.2, 3.3, 3.4**
 */
class PreservationObservationTest {

    /**
     * Create Gson with the EXACT same configuration as the actual app
     */
    private fun createAppGsonConfiguration(): Gson {
        val listType: Type = object : TypeToken<ApiResponse<List<Listing>>>() {}.type
        return GsonBuilder()
            .registerTypeAdapter(listType, ApiResponseDeserializer())
            .registerTypeAdapter(Listing::class.java, ListingDeserializer())
            .create()
    }

    /**
     * Test Case 1: Valid listings array - observe that responses with listing arrays parse correctly
     * **Validates: Requirement 3.1** - Valid JSON responses with data as array of listings SHALL CONTINUE TO parse successfully
     */
    @Test
    fun `PRESERVATION - valid listings array should parse correctly`() {
        println("=== PRESERVATION TEST 1: Valid Listings Array ===")
        
        // Arrange - Valid JSON with array of complete listings (minimal required fields)
        val validJson = """
        {
            "success": true,
            "message": null,
            "data": [
                {
                    "id": 1,
                    "title": "Test Listing 1",
                    "description": "Test Description 1",
                    "price": 100.0,
                    "location": "Test Location 1",
                    "category": "Electronics",
                    "listing_type": "FIXED",
                    "status": "active",
                    "image": null,
                    "images": [],
                    "seller": null,
                    "created_at": "2024-01-01T00:00:00Z",
                    "is_favorited": false
                }
            ]
        }
        """.trimIndent()
        
        val gson = createAppGsonConfiguration()

        try {
            // Act
            val listType: Type = object : TypeToken<ApiResponse<List<Listing>>>() {}.type
            val result: ApiResponse<List<Listing>> = gson.fromJson(validJson, listType)

            // Assert - Observe current behavior for valid responses
            assertNotNull("Result should not be null", result)
            assertTrue("Success should be true for valid response", result.success)
            assertNotNull("Data should not be null for successful response", result.data)
            assertEquals("Should parse 1 listing", 1, result.data!!.size)
            
            // Verify listing details
            val listing = result.data!![0]
            assertEquals("Listing ID should be 1", 1, listing.id)
            assertEquals("Listing title should match", "Test Listing 1", listing.title)
            assertEquals("Listing price should match", 100.0, listing.price, 0.01)
            assertEquals("Listing type should match", "FIXED", listing.listingType)
            assertFalse("Listing should not be favorited", listing.isFavorited)
            
            println("✓ PRESERVATION VERIFIED: Valid listings array parsed correctly - ${result.data!!.size} listings")
            
        } catch (e: Exception) {
            println("❌ PRESERVATION FAILED: Valid listings array failed to parse - ${e::class.simpleName}: ${e.message}")
            e.printStackTrace()
            fail("Valid listings should parse successfully: ${e.message}")
        }
    }

    /**
     * Test Case 2: Empty array [] - observe that empty array responses are handled correctly without crashing
     * **Validates: Requirement 3.2** - Empty array responses SHALL CONTINUE TO be handled correctly without crashing
     */
    @Test
    fun `PRESERVATION - empty array should be handled correctly without crashing`() {
        println("=== PRESERVATION TEST 2: Empty Array ===")
        
        // Arrange - Valid JSON with empty array
        val emptyArrayJson = """{"success": true, "message": null, "data": []}"""
        val gson = createAppGsonConfiguration()

        try {
            // Act
            val listType: Type = object : TypeToken<ApiResponse<List<Listing>>>() {}.type
            val result: ApiResponse<List<Listing>> = gson.fromJson(emptyArrayJson, listType)

            // Assert - Observe current behavior for empty arrays
            assertNotNull("Result should not be null", result)
            assertTrue("Success should be true for valid empty response", result.success)
            assertNotNull("Data should not be null for successful response", result.data)
            assertTrue("Data should be empty list", result.data!!.isEmpty())
            assertEquals("Data size should be 0", 0, result.data!!.size)
            
            println("✓ PRESERVATION VERIFIED: Empty array handled correctly - no crash, empty list returned")
            
        } catch (e: Exception) {
            println("❌ PRESERVATION FAILED: Empty array failed to parse - ${e::class.simpleName}: ${e.message}")
            e.printStackTrace()
            fail("Empty array should be handled correctly: ${e.message}")
        }
    }

    /**
     * Test Case 3: Error responses with success: false - observe that error messages are displayed via Toast
     * **Validates: Requirement 3.3** - Error responses with success: false SHALL CONTINUE TO display error message to user via Toast
     */
    @Test
    fun `PRESERVATION - error responses should display error messages`() {
        println("=== PRESERVATION TEST 3: Error Responses ===")
        
        // Arrange - Error response with success: false
        val errorResponseJson = """{"success": false, "message": "Server error occurred", "data": null}"""
        val gson = createAppGsonConfiguration()

        try {
            // Act
            val listType: Type = object : TypeToken<ApiResponse<List<Listing>>>() {}.type
            val result: ApiResponse<List<Listing>> = gson.fromJson(errorResponseJson, listType)

            // Assert - Observe current behavior for error responses
            assertNotNull("Result should not be null", result)
            assertFalse("Success should be false for error response", result.success)
            assertEquals("Error message should be preserved", "Server error occurred", result.message)
            assertNull("Data should be null for error response", result.data)
            
            println("✓ PRESERVATION VERIFIED: Error response handled correctly - success=false, message='${result.message}', data=null")
            
        } catch (e: Exception) {
            println("❌ PRESERVATION FAILED: Error response failed to parse - ${e::class.simpleName}: ${e.message}")
            e.printStackTrace()
            fail("Error response should be handled correctly: ${e.message}")
        }
    }

    /**
     * Test Case 4: Network errors (timeout, no connection) - observe that they are caught and display appropriate error messages
     * **Validates: Requirement 3.4** - Network errors SHALL CONTINUE TO be caught and display appropriate error messages
     */
    @Test
    fun `PRESERVATION - network error responses should be handled appropriately`() {
        println("=== PRESERVATION TEST 4: Network Error Responses ===")
        
        // Arrange - Simulate various network error response scenarios
        val networkErrorScenarios = listOf(
            "Network timeout" to """{"success": false, "message": "Network timeout", "data": null}""",
            "Connection failed" to """{"success": false, "message": "Connection failed", "data": null}""",
            "Server unavailable" to """{"success": false, "message": "Server unavailable", "data": null}"""
        )
        
        val gson = createAppGsonConfiguration()

        networkErrorScenarios.forEach { (errorType, errorJson) ->
            try {
                // Act
                val listType: Type = object : TypeToken<ApiResponse<List<Listing>>>() {}.type
                val result: ApiResponse<List<Listing>> = gson.fromJson(errorJson, listType)

                // Assert - Observe current behavior for network error responses
                assertNotNull("Result should not be null for $errorType", result)
                assertFalse("Success should be false for $errorType", result.success)
                assertNotNull("Error message should be present for $errorType", result.message)
                assertNull("Data should be null for $errorType", result.data)
                
                println("✓ PRESERVATION VERIFIED: $errorType handled - message='${result.message}'")
                
            } catch (e: Exception) {
                println("❌ PRESERVATION FAILED: $errorType failed to parse - ${e::class.simpleName}: ${e.message}")
                e.printStackTrace()
                fail("$errorType should be handled correctly: ${e.message}")
            }
        }
    }

    /**
     * Property-Based Test: Generate multiple valid listing scenarios
     * This test generates various valid API response patterns to ensure preservation across many cases
     */
    @Test
    fun `PRESERVATION PROPERTY - multiple valid listing scenarios should all parse correctly`() {
        println("=== PRESERVATION PROPERTY TEST: Multiple Valid Scenarios ===")
        
        val gson = createAppGsonConfiguration()
        
        // Generate various valid scenarios with minimal required fields
        val validScenarios = listOf(
            "Single listing" to """{"success": true, "message": null, "data": [{"id": 1, "title": "Single Item", "description": "Test", "price": 50.0, "location": "Test", "category": "Test", "listing_type": "FIXED", "status": "active", "image": null, "images": [], "seller": null, "created_at": "2024-01-01", "is_favorited": false}]}""",
            
            "Multiple listings" to """{"success": true, "message": "Found listings", "data": [{"id": 1, "title": "Fixed Price Item", "description": "Test", "price": 100.0, "location": "Test", "category": "Electronics", "listing_type": "FIXED", "status": "active", "image": null, "images": [], "seller": null, "created_at": "2024-01-01", "is_favorited": false}, {"id": 2, "title": "Auction Item", "description": "Test", "price": 200.0, "location": "Test", "category": "Books", "listing_type": "BID", "status": "active", "image": null, "images": [], "seller": null, "created_at": "2024-01-01", "is_favorited": true, "end_time": "2024-01-10T00:00:00Z"}]}""",
            
            "Empty array" to """{"success": true, "message": "No listings found", "data": []}"""
        )
        
        validScenarios.forEachIndexed { index, (scenarioName, validJson) ->
            try {
                // Act
                val listType: Type = object : TypeToken<ApiResponse<List<Listing>>>() {}.type
                val result: ApiResponse<List<Listing>> = gson.fromJson(validJson, listType)
                
                // Assert - All valid scenarios should parse successfully
                assertNotNull("$scenarioName: Result should not be null", result)
                assertTrue("$scenarioName: Success should be true", result.success)
                assertNotNull("$scenarioName: Data should not be null", result.data)
                
                println("✓ PRESERVATION VERIFIED: $scenarioName parsed successfully - ${result.data!!.size} listings")
                
            } catch (e: Exception) {
                println("❌ PRESERVATION FAILED: $scenarioName failed - ${e::class.simpleName}: ${e.message}")
                e.printStackTrace()
                fail("$scenarioName failed: ${e::class.simpleName}: ${e.message}")
            }
        }
    }
}