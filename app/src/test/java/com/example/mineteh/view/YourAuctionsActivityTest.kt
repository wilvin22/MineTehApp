package com.example.mineteh.view

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.mineteh.model.UserBidData
import com.example.mineteh.model.UserBidWithListing
import com.example.mineteh.models.Listing
import com.example.mineteh.models.Seller
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class YourAuctionsActivityTest {

    @Test
    fun `intent creation for BidDetailActivity works correctly`() {
        // Given
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val testListingId = 123

        // When
        val intent = Intent(context, BidDetailActivity::class.java)
        intent.putExtra("listing_id", testListingId)
        
        // Then
        assertEquals(testListingId, intent.getIntExtra("listing_id", -1))
        assertEquals(BidDetailActivity::class.java.name, intent.component?.className)
    }

    @Test
    fun `UserBidWithListing data model works correctly`() {
        // Given
        val testBid = createTestBidWithListing(123, "Test Auction", "active", futureEndTime())

        // When & Then
        assertEquals(123, testBid.listing.id)
        assertEquals("Test Auction", testBid.listing.title)
        assertEquals(100.0, testBid.bid.bidAmount, 0.01)
        assertEquals(100.0, testBid.highestBid, 0.01)
    }

    @Test
    fun `bid data creation works correctly`() {
        // Given
        val bidId = 1
        val userId = 1
        val listingId = 123
        val bidAmount = 150.0
        val bidTime = "2024-01-01T10:00:00"

        // When
        val bid = UserBidData(bidId, userId, listingId, bidAmount, bidTime)

        // Then
        assertEquals(bidId, bid.bidId)
        assertEquals(userId, bid.userId)
        assertEquals(listingId, bid.listingId)
        assertEquals(bidAmount, bid.bidAmount, 0.01)
        assertEquals(bidTime, bid.bidTime)
    }

    @Test
    fun `listing data creation works correctly`() {
        // Given
        val listingId = 123
        val title = "Test Auction"
        val status = "active"
        val endTime = futureEndTime()

        // When
        val listing = createTestListing(listingId, title, status, endTime)

        // Then
        assertEquals(listingId, listing.id)
        assertEquals(title, listing.title)
        assertEquals(status, listing.status)
        assertEquals(endTime, listing.endTime)
    }

    @Test
    fun `bid with listing combination works correctly`() {
        // Given
        val bid = UserBidData(1, 1, 123, 100.0, "2024-01-01T10:00:00")
        val listing = createTestListing(123, "Test Auction", "active", futureEndTime())
        val highestBid = 120.0

        // When
        val bidWithListing = UserBidWithListing(bid, listing, highestBid)

        // Then
        assertEquals(bid, bidWithListing.bid)
        assertEquals(listing, bidWithListing.listing)
        assertEquals(highestBid, bidWithListing.highestBid, 0.01)
    }

    @Test
    fun `intent extras work correctly for navigation`() {
        // Given
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val listingId = 456
        val intent = Intent(context, BidDetailActivity::class.java)

        // When
        intent.putExtra("listing_id", listingId)

        // Then
        assertTrue(intent.hasExtra("listing_id"))
        assertEquals(listingId, intent.getIntExtra("listing_id", -1))
    }

    @Test
    fun `multiple bid data objects work correctly`() {
        // Given
        val bid1 = createTestBidWithListing(1, "Auction 1", "active", futureEndTime())
        val bid2 = createTestBidWithListing(2, "Auction 2", "sold", pastEndTime())
        val bid3 = createTestBidWithListing(3, "Auction 3", "expired", pastEndTime())

        // When
        val bids = listOf(bid1, bid2, bid3)

        // Then
        assertEquals(3, bids.size)
        assertEquals("Auction 1", bids[0].listing.title)
        assertEquals("Auction 2", bids[1].listing.title)
        assertEquals("Auction 3", bids[2].listing.title)
    }

    @Test
    fun `bid categorization data is correct`() {
        // Given
        val liveBid = createTestBidWithListing(1, "Live Auction", "active", futureEndTime())
        val wonBid = createTestBidWithListing(2, "Won Auction", "sold", pastEndTime())
        val lostBid = createTestBidWithListing(3, "Lost Auction", "expired", pastEndTime())

        // When & Then
        assertEquals("active", liveBid.listing.status)
        assertEquals("sold", wonBid.listing.status)
        assertEquals("expired", lostBid.listing.status)
        
        // Verify end times
        assertTrue(liveBid.listing.endTime!!.contains("2025")) // Future date
        assertTrue(wonBid.listing.endTime!!.contains("2023")) // Past date
        assertTrue(lostBid.listing.endTime!!.contains("2023")) // Past date
    }

    @Test
    fun `empty bid list handling works`() {
        // Given
        val emptyBids = emptyList<UserBidWithListing>()

        // When & Then
        assertEquals(0, emptyBids.size)
        assertTrue(emptyBids.isEmpty())
    }

    @Test
    fun `bid amount formatting data is correct`() {
        // Given
        val bid1 = createTestBidWithListing(1, "Test", "active", futureEndTime())
        val bid2 = UserBidWithListing(
            bid = UserBidData(2, 1, 2, 1234.56, "2024-01-01T10:00:00"),
            listing = createTestListing(2, "Test 2", "active", futureEndTime()),
            highestBid = 1500.75
        )

        // When & Then
        assertEquals(100.0, bid1.bid.bidAmount, 0.01)
        assertEquals(1234.56, bid2.bid.bidAmount, 0.01)
        assertEquals(1500.75, bid2.highestBid, 0.01)
    }

    @Test
    fun `seller data is included correctly`() {
        // Given
        val bid = createTestBidWithListing(1, "Test Auction", "active", futureEndTime())

        // When & Then
        assertNotNull(bid.listing.seller)
        assertEquals("testuser", bid.listing.seller?.username)
        assertEquals("Test", bid.listing.seller?.firstName)
        assertEquals("User", bid.listing.seller?.lastName)
    }

    @Test
    fun `listing categories are handled correctly`() {
        // Given
        val electronicsBid = createTestBidWithListing(1, "Electronics Item", "active", futureEndTime())
        
        // When & Then
        assertEquals("Electronics", electronicsBid.listing.category)
        assertEquals("BID", electronicsBid.listing.listingType)
    }

    @Test
    fun `time handling works correctly`() {
        // Given
        val futureTime = futureEndTime()
        val pastTime = pastEndTime()

        // When & Then
        assertTrue(futureTime.contains("2025")) // Future year
        assertTrue(pastTime.contains("2023")) // Past year
        assertNotEquals(futureTime, pastTime)
    }

    @Test
    fun `bid comparison works correctly`() {
        // Given
        val userBid = 100.0
        val highestBid1 = 120.0 // User is outbid
        val highestBid2 = 100.0 // User is winning (tie)
        val highestBid3 = 80.0  // User is winning

        // When & Then
        assertTrue(userBid < highestBid1) // Outbid
        assertTrue(userBid >= highestBid2) // Winning (tie)
        assertTrue(userBid >= highestBid3) // Winning
    }

    @Test
    fun `activity class exists and is accessible`() {
        // Given & When
        val activityClass = YourAuctionsActivity::class.java

        // Then
        assertNotNull(activityClass)
        assertEquals("YourAuctionsActivity", activityClass.simpleName)
    }

    @Test
    fun `BidDetailActivity class exists and is accessible`() {
        // Given & When
        val activityClass = BidDetailActivity::class.java

        // Then
        assertNotNull(activityClass)
        assertEquals("BidDetailActivity", activityClass.simpleName)
    }

    // Helper methods for creating test data
    private fun createTestBidWithListing(
        listingId: Int,
        title: String,
        status: String,
        endTime: String
    ): UserBidWithListing {
        val bid = UserBidData(
            bidId = listingId,
            userId = 1,
            listingId = listingId,
            bidAmount = 100.0,
            bidTime = "2024-01-01T10:00:00"
        )

        val listing = createTestListing(listingId, title, status, endTime)

        return UserBidWithListing(
            bid = bid,
            listing = listing,
            highestBid = 100.0
        )
    }

    private fun createTestListing(
        listingId: Int,
        title: String,
        status: String,
        endTime: String
    ): Listing {
        return Listing(
            id = listingId,
            title = title,
            description = "Test description",
            price = 100.0,
            location = "Test location",
            category = "Electronics",
            listingType = "BID",
            status = status,
            _image = null,
            _images = null,
            seller = Seller(1, "testuser", "Test", "User"),
            createdAt = "2024-01-01T10:00:00",
            isFavorited = false,
            highestBidAmount = 100.0,
            endTime = endTime
        )
    }

    private fun futureEndTime(): String {
        return "2025-12-31T23:59:59"
    }

    private fun pastEndTime(): String {
        return "2023-01-01T00:00:00"
    }
}