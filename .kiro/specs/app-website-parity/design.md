# Design Document

## Overview

This document describes the technical design for bringing the Android app to feature parity with the PHP/Supabase website. All seven features follow the existing MVVM pattern: `Activity/Fragment → ViewModel → Repository → SupabaseClient`. No new backend tables are required except leveraging the existing `reviews` table.

---

## Architecture

The app uses:
- **View layer**: Activities and Fragments (XML layouts, View Binding)
- **ViewModel layer**: `AndroidViewModel` subclasses with `LiveData<Resource<T>>`
- **Repository layer**: Coroutine-based suspend functions calling `SupabaseClient.client`
- **Data layer**: Supabase Kotlin SDK (`postgrest`, `storage`)

All new components follow this same pattern.

---

## Feature Designs

### REQ-1: Listing Type Filter on Home Screen

**Changes to existing files:**

`HomeActivity` — add a `TabLayout` (or three `MaterialButton` toggles) above the category chip row. Wire each tab to call `viewModel.fetchListings(type = ...)`.

`HomeViewModel.fetchListings()` already accepts a `type` parameter — no ViewModel changes needed.

**Layout change (`res/layout/homepage.xml`):**
Add a `com.google.android.material.tabs.TabLayout` (or a `RadioGroup` with three `MaterialButton`s) with items: "All", "Auctions", "Buy Now". Place it between the search bar and the category chip scroll row.

**State management:**
`HomeActivity` tracks `selectedListingType: String?` (null = All, "BID", "FIXED"). On tab selection, call `viewModel.fetchListings(category = selectedCategory, type = selectedListingType)` so category and type filters compose correctly.

---

### REQ-2: Edit Listing

**New files:**
- `view/EditListingActivity.kt`
- `viewmodel/EditListingViewModel.kt`
- `res/layout/activity_edit_listing.xml`

**`EditListingViewModel`:**
```
editListing(id, title, description, price, endTime, newImageUris, removedImagePaths) → LiveData<Resource<Unit>>
loadListing(id) → LiveData<Resource<Listing>>
```

**`ListingsRepository` — new method:**
```kotlin
suspend fun updateListing(
    listingId: Int,
    title: String,
    description: String,
    price: Double,
    endTime: String?,
    newImageUris: List<Uri>,
    removedImagePaths: List<String>
): Resource<Unit>
```
Executes:
```
supabase.from("listings").update({ title=...; description=...; price=...; end_time=... }) {
    filter { eq("id", listingId); eq("seller_id", currentUserId) }
}
```
Then deletes removed images from `listing_images` and uploads/inserts new ones (reusing the existing image upload logic from `createListing`).

**`ItemDetailActivity` change:**
Replace the "coming soon" toast in `setupOwnerManagementUI` with:
```kotlin
binding.btnEditListing.setOnClickListener {
    startActivity(Intent(this, EditListingActivity::class.java).apply {
        putExtra("listing_id", listing.id)
    })
}
```

**Layout (`activity_edit_listing.xml`):**
Mirrors `SellActivity` layout: `TextInputEditText` for title, description, price; `DateTimePicker` for end time (BID only); horizontal `RecyclerView` for photo thumbnails with add/remove; a "Save Changes" `MaterialButton`.

---

### REQ-3: Search Filters

**Changes to existing files only — no new files.**

`SearchActivity.setupListeners()` — uncomment and wire the filter button:
```kotlin
findViewById<ImageView>(R.id.filterButton).setOnClickListener {
    showFilterBottomSheet()
}
```

`SearchActivity.performSearch()` — merge inline spinners with `currentFilters` from `FilterBottomSheetFragment`. After `onFiltersApplied`, call `performSearchWithFilters()` which already exists and passes all four params to `viewModel.searchListings(query, category, type, minPrice, maxPrice)`.

`SearchViewModel.searchListings()` — add `minPrice: Double?` and `maxPrice: Double?` parameters and forward them to `ListingsRepository.getListings()`.

`ListingsRepository.getListings()` — add `minPrice` and `maxPrice` filter clauses:
```kotlin
if (minPrice != null) gte("price", minPrice)
if (maxPrice != null) lte("price", maxPrice)
```

---

### REQ-4: Seller Public Profile

**New files:**
- `view/SellerProfileActivity.kt`
- `viewmodel/SellerProfileViewModel.kt`
- `model/repository/SellerRepository.kt`
- `res/layout/activity_seller_profile.xml`

**Data model (new, in `models/`):**
```kotlin
data class SellerProfileData(
    val accountId: Int,
    val username: String,
    val firstName: String,
    val lastName: String,
    val avatarUrl: String?,
    val averageRating: Double,
    val activeListingCount: Int,
    val soldCount: Int
)
```

**`SellerRepository.getSellerProfile(sellerId)`:**
1. Query `accounts` for username, first_name, last_name, avatar_url.
2. Query `listings` count where `seller_id = sellerId AND status = 'active'`.
3. Query `orders` count where `seller_id = sellerId AND status = 'completed'`.
4. Query `reviews` AVG(rating) where `seller_id = sellerId`.

**`ItemDetailActivity` change:**
Make seller name/avatar clickable:
```kotlin
binding.sellerName.setOnClickListener {
    listing.seller?.accountId?.let { sid ->
        startActivity(Intent(this, SellerProfileActivity::class.java).apply {
            putExtra("seller_id", sid)
        })
    }
}
```

**Layout (`activity_seller_profile.xml`):**
Avatar circle (see REQ-7 component), username, star rating bar, three stat chips (Active Listings, Sold, Rating), a `RecyclerView` of the seller's active listings using the existing `ListingsAdapter`.

---

### REQ-5: Selling Dashboard Stats

**Changes to existing files:**

`ProfileActivity` — add a stats card view and observe a new `SellingDashboardViewModel`.

**New files:**
- `viewmodel/SellingDashboardViewModel.kt`
- `model/repository/SellerRepository.kt` (shared with REQ-4)

**`SellerRepository.getMyStats(userId)`:**
```kotlin
data class SellerStats(
    val activeListings: Int,
    val totalSold: Int,
    val unreadMessages: Int,
    val averageRating: Double
)
```
Queries:
- `listings` count where `seller_id = userId AND status = 'active'`
- `orders` count where `seller_id = userId AND status = 'completed'`
- `messages` count where `receiver_id = userId AND is_read = false` (or conversations unread count)
- `reviews` AVG(rating) where `seller_id = userId`

**Layout change (`res/layout/profile.xml`):**
Add a `MaterialCardView` below the user info header with four `TextView` stat tiles: Active, Sold, Messages, Rating.

---

### REQ-6: Rate Seller After Purchase

**New files:**
- `view/RateSellerDialogFragment.kt`
- `viewmodel/ReviewViewModel.kt`
- `model/repository/ReviewRepository.kt`
- `res/layout/dialog_rate_seller.xml`

**`ReviewRepository`:**
```kotlin
suspend fun submitReview(sellerId: Int, listingId: Int, rating: Int, comment: String?): Resource<Unit>
suspend fun hasReviewed(listingId: Int): Resource<Boolean>
```
`submitReview` inserts into `reviews(reviewer_id, seller_id, listing_id, rating, comment)`.
`hasReviewed` queries `reviews` where `reviewer_id = currentUserId AND listing_id = listingId`.

**Trigger point — `MyOrdersActivity`:**
When an order row has `status = 'completed'` and `hasReviewed` returns false, show a "Rate Seller" button that opens `RateSellerDialogFragment`.

**`RateSellerDialogFragment`:**
Contains a `RatingBar` (1–5) and an optional `TextInputEditText` for comment. On submit, calls `reviewViewModel.submitReview(...)`.

**Data model:**
```kotlin
@Serializable
data class InsertReview(
    val reviewer_id: Int,
    val seller_id: Int,
    val listing_id: Int,
    val rating: Int,
    val comment: String?
)
```

---

### REQ-7: Profile Avatar

**New shared utility:**
`utils/AvatarUtils.kt`

```kotlin
object AvatarUtils {
    fun getInitials(firstName: String, lastName: String): String
    fun getAvatarColor(accountId: Int): Int  // deterministic color from palette
    fun bindAvatar(view: View, firstName: String, lastName: String, accountId: Int, avatarUrl: String?)
}
```

`bindAvatar` uses Glide to load `avatarUrl` if non-null; otherwise draws initials into a `ShapeableImageView` with a colored background.

**Changes:**
- `ProfileActivity` — replace static placeholder with `AvatarUtils.bindAvatar(...)` using data from `TokenManager` (first/last name already stored at login).
- `SellerProfileActivity` — same call with seller's data.
- `ItemDetailActivity.displaySellerInfo()` — bind seller avatar in the existing `sellerAvatarCard`.

**Color palette** (6 colors, index = `accountId % 6`):
Purple, Teal, Orange, Blue, Green, Red — all defined in `res/values/colors.xml`.

---

## Database Summary

No new tables. Existing tables used:

| Table | Used by |
|---|---|
| `listings` | REQ-1, REQ-2, REQ-3, REQ-4, REQ-5 |
| `listing_images` | REQ-2 |
| `accounts` | REQ-4, REQ-7 |
| `orders` | REQ-4, REQ-5, REQ-6 |
| `reviews` | REQ-4, REQ-5, REQ-6 |
| `messages` / `conversations` | REQ-5 |

---

## Correctness Properties

### REQ-2 Edit Listing
- Round-trip: load listing → edit with same values → save → reload → all fields equal original values.
- Ownership invariant: update query always includes `seller_id = currentUserId` filter; a different user's listing must not be modified.

### REQ-3 Search Filters
- Metamorphic: filtered result set is always a subset of the unfiltered result set for the same query.
- Invariant: applying `minPrice=X, maxPrice=Y` returns only listings where `X ≤ price ≤ Y`.

### REQ-6 Rate Seller
- Idempotence: calling `hasReviewed(listingId)` after `submitReview(listingId)` must return true; the prompt must not appear again.
- Error condition: submitting a rating of 0 or 6 must be rejected client-side before reaching the repository.

### REQ-7 Avatar
- Determinism: `getAvatarColor(accountId)` called twice with the same ID must return the same color.
- Round-trip: `getInitials("John", "Doe")` must return "JD".
