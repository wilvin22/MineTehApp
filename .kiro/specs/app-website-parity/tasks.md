# Implementation Tasks

## Task List

- [x] 1. REQ-1: Listing Type Filter on Home Screen
  - [x] 1.1 Add listing type tab strip to `res/layout/homepage.xml` (three MaterialButton toggles: All / Auctions / Buy Now)
  - [x] 1.2 Add `selectedListingType: String?` state to `HomeActivity` and wire each tab to call `viewModel.fetchListings(category = selectedCategory, type = selectedListingType)`
  - [x] 1.3 Apply selected/unselected visual styles to the type tabs (matching existing category button style)
  - [x] 1.4 Default to "All" tab on launch

- [x] 2. REQ-2: Edit Listing
  - [x] 2.1 Add `updateListing(listingId, title, description, price, endTime, newImageUris, removedImagePaths)` to `ListingsRepository`
  - [x] 2.2 Create `EditListingViewModel` with `loadListing(id)` and `editListing(...)` LiveData
  - [x] 2.3 Create `res/layout/activity_edit_listing.xml` (mirrors SellActivity layout with pre-populated fields)
  - [x] 2.4 Create `EditListingActivity` — load listing on start, validate inputs, call ViewModel on save
  - [x] 2.5 Replace "coming soon" toast in `ItemDetailActivity.setupOwnerManagementUI` with intent to `EditListingActivity`

- [x] 3. REQ-3: Search Filters
  - [x] 3.1 Add `minPrice: Double?` and `maxPrice: Double?` params to `ListingsRepository.getListings()` with `gte`/`lte` filter clauses
  - [x] 3.2 Add `minPrice` and `maxPrice` params to `SearchViewModel.searchListings()` and forward to repository
  - [x] 3.3 Wire the filter button in `SearchActivity.setupListeners()` to call `showFilterBottomSheet()`
  - [x] 3.4 Update `SearchActivity.performSearchWithFilters()` to pass `currentFilters.minPrice` and `currentFilters.maxPrice` to the ViewModel

- [x] 4. REQ-4: Seller Public Profile
  - [x] 4.1 Add `SellerProfileData` data class to `models/`
  - [x] 4.2 Create `SellerRepository` with `getSellerProfile(sellerId)` querying accounts, listings, orders, reviews
  - [x] 4.3 Create `SellerProfileViewModel` with `loadProfile(sellerId)` LiveData
  - [x] 4.4 Create `res/layout/activity_seller_profile.xml` (avatar, username, rating, stats, listings RecyclerView)
  - [x] 4.5 Create `SellerProfileActivity` observing the ViewModel and binding data
  - [x] 4.6 Make seller name/avatar clickable in `ItemDetailActivity.displaySellerInfo()` to open `SellerProfileActivity`

- [x] 5. REQ-5: Selling Dashboard Stats
  - [x] 5.1 Add `SellerStats` data class and `getMyStats(userId)` to `SellerRepository`
  - [x] 5.2 Create `SellingDashboardViewModel` with `loadStats()` LiveData
  - [x] 5.3 Add stats card to `res/layout/profile.xml` (Active, Sold, Messages, Rating tiles)
  - [x] 5.4 Observe `SellingDashboardViewModel` in `ProfileActivity` and bind stats to the card

- [x] 6. REQ-6: Rate Seller After Purchase
  - [x] 6.1 Create `ReviewRepository` with `submitReview(sellerId, listingId, rating, comment)` and `hasReviewed(listingId)`
  - [x] 6.2 Create `ReviewViewModel` wrapping the repository methods
  - [x] 6.3 Create `res/layout/dialog_rate_seller.xml` (RatingBar + optional comment TextInputEditText + Submit button)
  - [x] 6.4 Create `RateSellerDialogFragment` calling `ReviewViewModel.submitReview` on submit
  - [x] 6.5 In `MyOrdersActivity`, show "Rate Seller" button on completed orders where `hasReviewed` returns false; open `RateSellerDialogFragment` on tap

- [x] 7. REQ-7: Profile Avatar
  - [x] 7.1 Create `utils/AvatarUtils.kt` with `getInitials`, `getAvatarColor`, and `bindAvatar` functions
  - [x] 7.2 Add avatar color palette (6 colors) to `res/values/colors.xml`
  - [x] 7.3 Update `ProfileActivity` to call `AvatarUtils.bindAvatar` using TokenManager data
  - [x] 7.4 Update `ItemDetailActivity.displaySellerInfo()` to call `AvatarUtils.bindAvatar` for the seller avatar card
  - [x] 7.5 Apply `AvatarUtils.bindAvatar` in `SellerProfileActivity` (created in task 4.5)
