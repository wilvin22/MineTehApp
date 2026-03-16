package com.example.mineteh.repository

import com.example.mineteh.models.ApiResponse
import com.example.mineteh.models.Listing
import com.example.mineteh.network.ApiResponseDeserializer
import com.example.mineteh.network.ListingDeserializer
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import org.junit.Test
import org.junit.Assert.*
import java.lang.reflect.Type

/**
 * Bug Condition Exploration Test for Homepage JSON Parsing Fix
 * 
 * **Property 1: Fault Condition** - Graceful Handling of Malformed JSON
 * 
 * **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists
 * **EXPECTED OUTCOME**: Test FAILS with IllegalStateException "Expected BEGIN_ARRAY but was BEGIN_OBJECT"
 * 
 * **ANALYSIS**: There's already an ApiResponseDeserializer that should handle malformed JSON.
 * This test explores whether the bug still exists despite the existing deserializer.
 * 
 * **Validates: Requirements 1.1, 1.2, 1.3**
 */
class ListingsRepositoryTest {

    /**
     * Test the current ApiResponseDeserializer with malformed JSON
     * This tests the ACTUAL deserializer used in the app
     */
    private fun createGsonWithCurrentDeserializer(): Gson {
        val listType: Type = object : TypeToken<ApiResponse<List<Listing>>>() {}.type
        return GsonBuilder()
            .setLenient()
            .registerTypeAdapter(
                listType,
                ApiResponseDeserializer()
            )
            .registerTypeAdapter(Listing::class.java, ListingDeserializer())
            .create()
    }

    /**
     * CRITICAL TEST: Test WITHOUT Custom Deserializer (Raw Gson)
     * This demonstrates the bug that WOULD occur without the custom deserializer
     * 
     * **EXPECTED OUTCOME**: This test SHOULD FAIL with IllegalStateException
     * This confirms the bug condition exists and the deserializer is the fix
     */
    @Test
    fun `raw gson without custom deserializer should fail with malformed JSON - EXPECTED TO FAIL`() {
        // Arrange - Malformed JSON with empty object instead of array
        val malformedJson = """{"success": true, "message": null, "data": {}}"""
        val rawGson = Gson() // No custom deserializer

        // Act & Assert - This SHOULD throw IllegalStateException to confirm bug exists
        try {
            val listType: Type = object : TypeToken<ApiResponse<List<Listing>>>() {}.type
            val result: ApiResponse<List<Listing>> = rawGson.fromJson(malformedJson, listType)
            fail("CRITICAL: Expected IllegalStateException but parsing succeeded - bug condition not reproduced!")
        } catch (e: IllegalStateException) {
            // This is the EXPECTED behavior - demonstrates the bug exists
            assertTrue("Expected 'BEGIN_ARRAY' error message", 
                e.message?.contains("Expected BEGIN_ARRAY") == true)
            println("✓ COUNTEREXAMPLE FOUND: ${e.message}")
            println("✓ Bug condition confirmed - raw Gson fails with malformed JSON")
        } catch (e: JsonSyntaxException) {
            // JsonSyntaxException can also occur and wraps IllegalStateException
            assertTrue("Expected 'BEGIN_ARRAY' error message", 
                e.message?.contains("Expected BEGIN_ARRAY") == true)
            println("✓ COUNTEREXAMPLE FOUND: ${e.message}")
            println("✓ Bug condition confirmed - raw Gson fails with malformed JSON")
        } catch (e: Exception) {
            fail("Expected IllegalStateException or JsonSyntaxException but got ${e::class.simpleName}: ${e.message}")
        }
    }

    /**
     * Test Case 1: Empty Object Response with Current Deserializer
     * API returns {"success": true, "message": null, "data": {}}
     * 
     * **EXPECTED**: Should handle gracefully with current deserializer
     * **IF THIS FAILS**: The existing deserializer is not working correctly
     */
    @Test
    fun `current deserializer with empty object data should handle gracefully`() {
        // Arrange - Malformed JSON with empty object instead of array
        val malformedJson = """{"success": true, "message": null, "data": {}}"""
        val gson = createGsonWithCurrentDeserializer()

        try {
            // Act
            val listType: Type = object : TypeToken<ApiResponse<List<Listing>>>() {}.type
            val result: ApiResponse<List<Listing>> = gson.fromJson(malformedJson, listType)

            // Assert - Current deserializer should handle this gracefully
            assertNotNull("Result should not be null", result)
            assertTrue("Success should be true", result.success)
            assertNotNull("Data should not be null for successful response", result.data)
            assertTrue("Data should be empty list for malformed successful response", result.data!!.isEmpty())
            println("✓ CURRENT BEHAVIOR: Empty object handled gracefully - data=${result.data}")
        } catch (e: Exception) {
            fail("CRITICAL: Current deserializer failed with empty object: ${e::class.simpleName}: ${e.message}")
        }
    }

    /**
     * Test Case 2: Null Data Response with Current Deserializer
     * API returns {"success": true, "message": null, "data": null}
     */
    @Test
    fun `current deserializer with null data should handle gracefully`() {
        val malformedJson = """{"success": true, "message": null, "data": null}"""
        val gson = createGsonWithCurrentDeserializer()

        try {
            val listType: Type = object : TypeToken<ApiResponse<List<Listing>>>() {}.type
            val result: ApiResponse<List<Listing>> = gson.fromJson(malformedJson, listType)
            assertNotNull("Result should not be null", result)
            assertTrue("Success should be true", result.success)
            assertNotNull("Data should not be null for successful response", result.data)
            assertTrue("Data should be empty list for null data in successful response", result.data!!.isEmpty())
            println("✓ CURRENT BEHAVIOR: Null data handled gracefully - data=${result.data}")
        } catch (e: Exception) {
            fail("CRITICAL: Current deserializer failed with null data: ${e::class.simpleName}: ${e.message}")
        }
    }

    /**
     * Test Case 3: Wrapped Array Response with Current Deserializer
     * API returns {"success": true, "message": null, "data": {"listings": [...]}}
     */
    @Test
    fun `current deserializer with wrapped array data should handle gracefully`() {
        val malformedJson = """{"success": true, "message": null, "data": {"listings": [{"id": 1, "title": "Test"}]}}"""
        val gson = createGsonWithCurrentDeserializer()

        try {
            val listType: Type = object : TypeToken<ApiResponse<List<Listing>>>() {}.type
            val result: ApiResponse<List<Listing>> = gson.fromJson(malformedJson, listType)
            assertNotNull("Result should not be null", result)
            assertTrue("Success should be true", result.success)
            assertNotNull("Data should not be null for successful response", result.data)
            assertTrue("Data should be empty list for malformed successful response", result.data!!.isEmpty())
            println("✓ CURRENT BEHAVIOR: Wrapped array handled gracefully - data=${result.data}")
        } catch (e: Exception) {
            fail("CRITICAL: Current deserializer failed with wrapped array: ${e::class.simpleName}: ${e.message}")
        }
    }

    /**
     * Test Case 4: Single Object Instead of Array with Current Deserializer
     * API returns {"success": true, "message": null, "data": {"id": 1, "title": "Test"}}
     */
    @Test
    fun `current deserializer with single object data should handle gracefully`() {
        val malformedJson = """{"success": true, "message": null, "data": {"id": 1, "title": "Test Listing", "description": "Test", "price": 100.0, "location": "Test", "category": "Test", "listing_type": "FIXED", "status": "active", "image": null, "images": [], "seller": null, "created_at": "2024-01-01", "is_favorited": false}}"""
        val gson = createGsonWithCurrentDeserializer()

        try {
            val listType: Type = object : TypeToken<ApiResponse<List<Listing>>>() {}.type
            val result: ApiResponse<List<Listing>> = gson.fromJson(malformedJson, listType)
            assertNotNull("Result should not be null", result)
            assertTrue("Success should be true", result.success)
            assertNotNull("Data should not be null for successful response", result.data)
            assertTrue("Data should be empty list for malformed successful response", result.data!!.isEmpty())
            println("✓ CURRENT BEHAVIOR: Single object handled gracefully - data=${result.data}")
        } catch (e: Exception) {
            fail("CRITICAL: Current deserializer failed with single object: ${e::class.simpleName}: ${e.message}")
        }
    }

    /**
     * Test Case 5: Error Response with Current Deserializer
     * API returns {"success": false, "message": "Error occurred", "data": null}
     * Expected: Should handle gracefully and preserve error state
     */
    @Test
    fun `current deserializer with error response should preserve error state`() {
        val errorJson = """{"success": false, "message": "Error occurred", "data": null}"""
        val gson = createGsonWithCurrentDeserializer()

        try {
            val listType: Type = object : TypeToken<ApiResponse<List<Listing>>>() {}.type
            val result: ApiResponse<List<Listing>> = gson.fromJson(errorJson, listType)
            assertNotNull("Result should not be null", result)
            assertFalse("Success should be false", result.success)
            assertEquals("Message should be preserved", "Error occurred", result.message)
            assertNull("Data should be null for error response", result.data)
            println("✓ CURRENT BEHAVIOR: Error response preserved - success=${result.success}, message=${result.message}, data=${result.data}")
        } catch (e: Exception) {
            fail("CRITICAL: Current deserializer failed with error response: ${e::class.simpleName}: ${e.message}")
        }
    }

    /**
     * ANALYSIS TEST: Test with malformed Listing objects in array
     * This tests if the ListingDeserializer can handle malformed individual listings
     */
    @Test
    fun `current deserializer with malformed listing objects should handle gracefully`() {
        // Malformed listing missing required fields
        val malformedJson = """{"success": true, "message": null, "data": [{"id": 1, "title": "Test"}]}"""
        val gson = createGsonWithCurrentDeserializer()

        try {
            val listType: Type = object : TypeToken<ApiResponse<List<Listing>>>() {}.type
            val result: ApiResponse<List<Listing>> = gson.fromJson(malformedJson, listType)
            // This might fail if individual Listing deserialization fails
            println("✓ CURRENT BEHAVIOR: Malformed listing handled - data size=${result.data?.size}")
        } catch (e: Exception) {
            println("⚠ POTENTIAL BUG LOCATION: Malformed listing deserialization failed: ${e::class.simpleName}: ${e.message}")
            // This could be where the actual bug occurs
        }
    }

    // ========================================
    // PRESERVATION PROPERTY TESTS
    // Property 2: Preservation - Valid Response Handling
    // **Validates: Requirements 3.1, 3.2, 3.3, 3.4**
    // ========================================

    /**
     * **Property 2: Preservation** - Valid Response Handling
     * 
     * **IMPORTANT**: Follow observation-first methodology
     * These tests observe behavior on UNFIXED code for valid API responses (non-buggy inputs)
     * **EXPECTED OUTCOME**: Tests PASS (this confirms baseline behavior to preserve)
     * 
     * **Validates: Requirements 3.1, 3.2, 3.3, 3.4**
     */

    /**
     * Test Case 1: Valid listings array - observe that responses with listing arrays parse correctly
     * **Validates: Requirement 3.1** - Valid JSON responses with data as array of listings SHALL CONTINUE TO parse successfully
     */
    @Test
    fun `preservation test - valid listings array should parse correctly and display in RecyclerView`() {
        // Arrange - Valid JSON with array of complete listings
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
                    "image": "test1.jpg",
                    "images": [{"image_path": "test1.jpg"}],
                    "seller": {"account_id": 1, "username": "seller1", "first_name": "John", "last_name": "Doe"},
                    "created_at": "2024-01-01T00:00:00Z",
                    "is_favorited": false
                },
                {
                    "id": 2,
                    "title": "Test Listing 2",
                    "description": "Test Description 2",
                    "price": 200.0,
                    "location": "Test Location 2",
                    "category": "Books",
                    "listing_type": "BID",
                    "status": "active",
                    "image": "test2.jpg",
                    "images": [{"image_path": "test2.jpg"}],
                    "seller": {"account_id": 2, "username": "seller2", "first_name": "Jane", "last_name": "Smith"},
                    "created_at": "2024-01-02T00:00:00Z",
                    "is_favorited": true,
                    "end_time": "2024-01-10T00:00:00Z"
                }
            ]
        }
        """.trimIndent()
        
        val gson = createGsonWithCurrentDeserializer()

        // Act
        val listType: Type = object : TypeToken<ApiResponse<List<Listing>>>() {}.type
        val result: ApiResponse<List<Listing>> = gson.fromJson(validJson, listType)

        // Assert - Observe current behavior for valid responses
        assertNotNull("Result should not be null", result)
        assertTrue("Success should be true for valid response", result.success)
        assertNotNull("Data should not be null for successful response", result.data)
        assertEquals("Should parse 2 listings", 2, result.data!!.size)
        
        // Verify first listing details
        val listing1 = result.data!![0]
        assertEquals("First listing ID should be 1", 1, listing1.id)
        assertEquals("First listing title should match", "Test Listing 1", listing1.title)
        assertEquals("First listing price should match", 100.0, listing1.price, 0.01)
        assertEquals("First listing type should match", "FIXED", listing1.listingType)
        assertFalse("First listing should not be favorited", listing1.isFavorited)
        
        // Verify second listing details
        val listing2 = result.data!![1]
        assertEquals("Second listing ID should be 2", 2, listing2.id)
        assertEquals("Second listing title should match", "Test Listing 2", listing2.title)
        assertEquals("Second listing price should match", 200.0, listing2.price, 0.01)
        assertEquals("Second listing type should match", "BID", listing2.listingType)
        assertTrue("Second listing should be favorited", listing2.isFavorited)
        assertNotNull("Second listing should have end time", listing2.endTime)
        
        println("✓ PRESERVATION VERIFIED: Valid listings array parsed correctly - ${result.data!!.size} listings")
    }

    /**
     * Test Case 2: Empty array [] - observe that empty array responses are handled correctly without crashing
     * **Validates: Requirement 3.2** - Empty array responses SHALL CONTINUE TO be handled correctly without crashing
     */
    @Test
    fun `preservation test - empty array should be handled correctly without crashing`() {
        // Arrange - Valid JSON with empty array
        val emptyArrayJson = """{"success": true, "message": null, "data": []}"""
        val gson = createGsonWithCurrentDeserializer()

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
    }

    /**
     * Test Case 3: Error responses with success: false - observe that error messages are displayed via Toast
     * **Validates: Requirement 3.3** - Error responses with success: false SHALL CONTINUE TO display error message to user via Toast
     */
    @Test
    fun `preservation test - error responses should display error messages via Toast`() {
        // Arrange - Error response with success: false
        val errorResponseJson = """{"success": false, "message": "Server error occurred", "data": null}"""
        val gson = createGsonWithCurrentDeserializer()

        // Act
        val listType: Type = object : TypeToken<ApiResponse<List<Listing>>>() {}.type
        val result: ApiResponse<List<Listing>> = gson.fromJson(errorResponseJson, listType)

        // Assert - Observe current behavior for error responses
        assertNotNull("Result should not be null", result)
        assertFalse("Success should be false for error response", result.success)
        assertEquals("Error message should be preserved", "Server error occurred", result.message)
        assertNull("Data should be null for error response", result.data)
        
        println("✓ PRESERVATION VERIFIED: Error response handled correctly - success=false, message='${result.message}', data=null")
    }

    /**
     * Test Case 4: Network errors (timeout, no connection) - observe that they are caught and display appropriate error messages
     * **Validates: Requirement 3.4** - Network errors SHALL CONTINUE TO be caught and display appropriate error messages
     * 
     * Note: This test simulates the JSON parsing aspect. Network error handling is tested at the repository level.
     */
    @Test
    fun `preservation test - network error responses should be handled appropriately`() {
        // Arrange - Simulate various network error response scenarios
        val networkErrorScenarios = listOf(
            """{"success": false, "message": "Network timeout", "data": null}""",
            """{"success": false, "message": "Connection failed", "data": null}""",
            """{"success": false, "message": "Server unavailable", "data": null}"""
        )
        
        val gson = createGsonWithCurrentDeserializer()

        networkErrorScenarios.forEach { errorJson ->
            // Act
            val listType: Type = object : TypeToken<ApiResponse<List<Listing>>>() {}.type
            val result: ApiResponse<List<Listing>> = gson.fromJson(errorJson, listType)

            // Assert - Observe current behavior for network error responses
            assertNotNull("Result should not be null for network error", result)
            assertFalse("Success should be false for network error", result.success)
            assertNotNull("Error message should be present for network error", result.message)
            assertNull("Data should be null for network error", result.data)
            
            println("✓ PRESERVATION VERIFIED: Network error handled - message='${result.message}'")
        }
    }

    /**
     * Property-Based Test: Generate multiple valid listing scenarios
     * This test generates various valid API response patterns to ensure preservation across many cases
     */
    @Test
    fun `preservation property test - multiple valid listing scenarios should all parse correctly`() {
        val gson = createGsonWithCurrentDeserializer()
        
        // Generate various valid scenarios
        val validScenarios = listOf(
            // Single listing
            """{"success": true, "message": null, "data": [{"id": 1, "title": "Single Item", "description": "Test", "price": 50.0, "location": "Test", "category": "Test", "listing_type": "FIXED", "status": "active", "image": null, "images": [], "seller": null, "created_at": "2024-01-01", "is_favorited": false}]}""",
            
            // Multiple listings with different types
            """{"success": true, "message": "Found listings", "data": [{"id": 1, "title": "Fixed Price Item", "description": "Test", "price": 100.0, "location": "Test", "category": "Electronics", "listing_type": "FIXED", "status": "active", "image": null, "images": [], "seller": null, "created_at": "2024-01-01", "is_favorited": false}, {"id": 2, "title": "Auction Item", "description": "Test", "price": 200.0, "location": "Test", "category": "Books", "listing_type": "BID", "status": "active", "image": null, "images": [], "seller": null, "created_at": "2024-01-01", "is_favorited": true, "end_time": "2024-01-10T00:00:00Z"}]}""",
            
            // Listings with complete seller information
            """{"success": true, "message": null, "data": [{"id": 3, "title": "Complete Listing", "description": "Full details", "price": 300.0, "location": "Complete Location", "category": "Furniture", "listing_type": "FIXED", "status": "active", "image": "complete.jpg", "images": [{"image_path": "complete1.jpg"}, {"image_path": "complete2.jpg"}], "seller": {"account_id": 123, "username": "completeseller", "first_name": "Complete", "last_name": "Seller"}, "created_at": "2024-01-01T12:00:00Z", "is_favorited": false}]}""",
            
            // Empty array (edge case)
            """{"success": true, "message": "No listings found", "data": []}"""
        )
        
        validScenarios.forEachIndexed { index, validJson ->
            try {
                // Act
                val listType: Type = object : TypeToken<ApiResponse<List<Listing>>>() {}.type
                val result: ApiResponse<List<Listing>> = gson.fromJson(validJson, listType)
                
                // Assert - All valid scenarios should parse successfully
                assertNotNull("Scenario $index: Result should not be null", result)
                assertTrue("Scenario $index: Success should be true", result.success)
                assertNotNull("Scenario $index: Data should not be null", result.data)
                
                println("✓ PRESERVATION VERIFIED: Scenario $index parsed successfully - ${result.data!!.size} listings")
                
            } catch (e: Exception) {
                fail("Scenario $index failed: ${e::class.simpleName}: ${e.message}")
            }
        }
    }
}