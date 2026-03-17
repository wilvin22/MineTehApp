# Owner Auction Management Fix

## Issue
When an auction listing owner viewed their own item, they were seeing the buyer UI with "Place Bid" button instead of owner management controls.

## Root Cause
The owner detection logic was working, but needed better null-safety checks for the `Seller.accountId` field which is nullable (`Int?`).

## Changes Made

### 1. Improved Owner Detection Logic
**File:** `ItemDetailActivity.kt`

```kotlin
// Before
val isOwner = currentUserId != -1 && listing.seller?.accountId == currentUserId

// After
val sellerId = listing.seller?.accountId
val isOwner = currentUserId != -1 && sellerId != null && sellerId == currentUserId
```

Added explicit null check for `sellerId` to ensure proper comparison.

### 2. Enhanced Logging
Added detailed logging to debug owner detection:
```kotlin
Log.d("ItemDetailActivity", "=== OWNER CHECK ===")
Log.d("ItemDetailActivity", "Current user ID: $currentUserId")
Log.d("ItemDetailActivity", "Seller ID: $sellerId")
Log.d("ItemDetailActivity", "Seller object: ${listing.seller}")
Log.d("ItemDetailActivity", "Is owner: $isOwner")
Log.d("ItemDetailActivity", "==================")
```

### 3. Added "Close Auction" Button
**File:** `item_detail.xml`

Added new button in owner management card:
```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btnCloseAuction"
    android:text="🔨 Close Auction"
    android:visibility="gone"
    app:backgroundTint="#ffc107" />
```

### 4. Conditional Close Auction Button
**File:** `ItemDetailActivity.kt`

Button only shows for BID listings:
```kotlin
if (listing.listingType == "BID") {
    binding.btnCloseAuction.visibility = View.VISIBLE
    binding.btnCloseAuction.setOnClickListener {
        showCloseAuctionDialog(listing)
    }
} else {
    binding.btnCloseAuction.visibility = View.GONE
}
```

### 5. Close Auction Functionality
Added dialog and method to close auctions:
```kotlin
private fun showCloseAuctionDialog(listing: Listing) {
    AlertDialog.Builder(this)
        .setTitle("Close Auction")
        .setMessage("Close this auction? The highest bidder will win.")
        .setPositiveButton("Close") { _, _ ->
            closeAuction(listing.id)
        }
        .setNegativeButton("Cancel", null)
        .show()
}

private fun closeAuction(listingId: Int) {
    viewModel.updateListingStatus(listingId, "CLOSED")
}
```

## Owner Management UI (Based on Website)

### For FIXED Listings:
- ✏️ Edit Listing
- 📦 Your Listings
- 🚫 Disable Listing / ✅ Enable Listing

### For BID/Auction Listings:
- ✏️ Edit Listing
- 📦 Your Listings
- 🔨 Close Auction (yellow button)
- 🚫 Disable Listing / ✅ Enable Listing

## What Owners See

### FIXED Listing Owner View:
1. "Your Listing" badge
2. Price (read-only)
3. Seller info (their own profile)
4. Management card with 3 buttons
5. Description

### BID Listing Owner View:
1. "Your Listing" badge
2. Bid info card with current highest bid (read-only)
3. Auction timer (read-only)
4. LIVE/ENDED badge
5. Seller info (their own profile)
6. Management card with 4 buttons (includes Close Auction)
7. Description

## What Owners DON'T See
- ❌ Place Bid button
- ❌ Add to Cart button
- ❌ Buy Now button
- ❌ Favorite button
- ❌ Contact Seller button

## Files Modified
1. `app/src/main/java/com/example/mineteh/view/ItemDetailActivity.kt`
2. `app/src/main/res/layout/item_detail.xml`

## Testing Checklist
- [ ] Owner sees management card for FIXED listings
- [ ] Owner sees management card for BID listings
- [ ] Close Auction button only shows for BID listings
- [ ] Close Auction button is yellow/gold color
- [ ] Close Auction dialog appears with confirmation
- [ ] Closing auction sets status to "CLOSED"
- [ ] Owner never sees Place Bid button
- [ ] Buyer sees Place Bid button (not owner controls)
- [ ] Logging shows correct owner detection
