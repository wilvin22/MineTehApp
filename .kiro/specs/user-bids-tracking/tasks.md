# Implementation Plan: User Bids Tracking

## Overview

This implementation plan converts the user-bids-tracking design into actionable coding tasks. The feature enables users to view and track their auction bids organized by status (Live, Won, Lost) within the existing YourAuctionsActivity. The implementation follows the MVVM architecture pattern with BidsRepository for data access, BidsViewModel for business logic, and updated UI components with real Supabase data.

## Tasks

- [x] 1. Create data models and utility classes
  - [x] 1.1 Create UserBidData and UserBidWithListing data classes
    - Create `UserBidData.kt` with fields: bidId, userId, listingId, bidAmount, bidTime
    - Create `UserBidWithListing.kt` combining bid, listing, and highestBid
    - Create `BidsUiState.kt` sealed class with Loading, Success, and Error states
    - Place in `app/src/main/java/com/example/mineteh/model/` directory
    - _Requirements: 1.1, 1.2, 1.3_

  - [ ]* 1.2 Write property test for data model completeness
    - **Property 2: Bid-Listing Data Completeness**
    - **Validates: Requirements 1.2, 1.3, 1.5**

  - [x] 1.3 Create TimeUtils utility class
    - Implement `formatCountdown(millisRemaining: Long): String` with days/hours/minutes/seconds formatting
    - Implement `formatEndTime(endTime: String?): String` with "MMM dd, yyyy HH:mm" format
    - Implement `parseIsoDate(isoString: String): Date` for ISO date parsing
    - Place in `app/src/main/java/com/example/mineteh/utils/` directory
    - _Requirements: 10.2, 10.3, 10.4, 10.5_

  - [ ]* 1.4 Write property tests for time formatting
    - **Property 21: Countdown Timer Formatting**
    - **Property 22: End Time Formatting**
    - **Validates: Requirements 10.2, 10.5**

  - [x] 1.5 Create CurrencyUtils utility class
    - Implement `formatCurrency(amount: Double): String` with $ symbol and 2 decimal places
    - Place in `app/src/main/java/com/example/mineteh/utils/` directory
    - _Requirements: 10.1_

  - [ ]* 1.6 Write property test for currency formatting
    - **Property 20: Currency Formatting**
    - **Validates: Requirements 10.1**

- [x] 2. Implement BidsRepository for data access
  - [x] 2.1 Create BidsRepository class with Supabase queries
    - Create `BidsRepository.kt` in `app/src/main/java/com/example/mineteh/repository/`
    - Implement `getUserBids(): Resource<List<UserBidWithListing>>` method
    - Query bids table filtered by user_id from TokenManager
    - Implement `parseBidsResponse(jsonData: String): List<UserBidData>` for JSON parsing
    - Implement `fetchListingsForBids(listingIds: List<Int>): Map<Int, Listing>` to get listing details
    - Implement `fetchHighestBids(listingIds: List<Int>): Map<Int, Double>` to get max bids per listing
    - Combine all data into UserBidWithListing objects
    - Handle errors with Resource.Error and descriptive messages
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [ ]* 2.2 Write property test for user bid filtering
    - **Property 1: User Bid Filtering**
    - **Validates: Requirements 1.1**

  - [ ]* 2.3 Write property test for repository error handling
    - **Property 3: Repository Error Handling**
    - **Validates: Requirements 1.4**

  - [x] 2.4 Write unit tests for BidsRepository
    - Test successful bid fetching with mock data
    - Test error handling for network failures
    - Test authentication check before queries
    - Test JSON parsing with malformed data
    - Test empty results handling
    - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [x] 3. Checkpoint - Ensure repository tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Implement BidsViewModel for business logic
  - [x] 4.1 Create BidsViewModel class with categorization logic
    - Create `BidsViewModel.kt` in `app/src/main/java/com/example/mineteh/viewmodel/`
    - Extend AndroidViewModel with BidsRepository instance
    - Create `_bidsState` MutableLiveData and public `bidsState` LiveData
    - Implement `fetchBids()` method to load and categorize bids
    - Implement `categorizeBids(bids: List<UserBidWithListing>)` returning Triple of live/won/lost lists
    - Implement `isAuctionLive(endTime: String): Boolean` checking end_time > now and status == "active"
    - Implement `didUserWin(userBid: Double, highestBid: Double): Boolean` checking userBid >= highestBid
    - Emit BidsUiState.Loading, Success, or Error states
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

  - [ ]* 4.2 Write property tests for bid categorization
    - **Property 4: Bid Categorization Completeness**
    - **Property 5: Live Auction Classification**
    - **Property 6: Won Auction Classification**
    - **Property 7: Lost Auction Classification**
    - **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5**

  - [x] 4.3 Implement auto-refresh mechanism in BidsViewModel
    - Add `autoRefreshJob: Job?` property
    - Implement `startAutoRefresh()` launching coroutine with 30-second delay loop
    - Implement `stopAutoRefresh()` cancelling the job
    - Call `fetchBids()` in each refresh iteration
    - _Requirements: 7.1, 7.5_

  - [ ]* 4.4 Write property test for lifecycle-aware refresh
    - **Property 16: Lifecycle-Aware Refresh**
    - **Validates: Requirements 7.5**

  - [x] 4.5 Write unit tests for BidsViewModel
    - Test categorization with various bid scenarios (live/won/lost)
    - Test edge case: bid amount exactly equals highest bid
    - Test edge case: end time exactly at current time
    - Test auto-refresh starts and stops correctly
    - Test error state propagation from repository
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 7.1, 7.5_

- [x] 5. Checkpoint - Ensure ViewModel tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Create RecyclerView adapters for bid display
  - [x] 6.1 Create LiveAuctionAdapter with countdown timers
    - Create `LiveAuctionAdapter.kt` in `app/src/main/java/com/example/mineteh/view/`
    - Create ViewHolder with `ItemAuctionLiveBinding` (or create layout if needed)
    - Implement `submitList(newBids: List<UserBidWithListing>)` method
    - Bind listing title, description, image, user bid, highest bid
    - Display "Winning" status in green if userBid >= highestBid, else "Outbid" in red
    - Implement `startCountdown(endTime: String)` with coroutine updating every second
    - Use TimeUtils.formatCountdown() for display
    - Cancel countdown job in `onViewRecycled()`
    - Add click listener calling onItemClick callback
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_

  - [ ]* 6.2 Write property tests for live auction display
    - **Property 8: Live Auction Display Completeness**
    - **Property 9: Winning Status Indicator**
    - **Property 10: Outbid Status Indicator**
    - **Validates: Requirements 3.2, 3.3, 3.4, 3.5, 3.6, 3.7**

  - [x] 6.3 Create WonAuctionAdapter
    - Create `WonAuctionAdapter.kt` in `app/src/main/java/com/example/mineteh/view/`
    - Create ViewHolder with `ItemAuctionWonBinding` (or create layout if needed)
    - Implement `submitList(newBids: List<UserBidWithListing>)` method
    - Bind listing title, description, image, winning bid amount
    - Display formatted end time using TimeUtils.formatEndTime()
    - Display "Won" status indicator
    - Add click listener calling onItemClick callback
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

  - [ ]* 6.4 Write property test for won auction display
    - **Property 11: Won Auction Display Completeness**
    - **Validates: Requirements 4.2, 4.3, 4.4, 4.5**

  - [x] 6.5 Create LostAuctionAdapter
    - Create `LostAuctionAdapter.kt` in `app/src/main/java/com/example/mineteh/view/`
    - Create ViewHolder with `ItemAuctionLostBinding` (or create layout if needed)
    - Implement `submitList(newBids: List<UserBidWithListing>)` method
    - Bind listing title, description, image, user bid, winning bid
    - Display formatted end time using TimeUtils.formatEndTime()
    - Add click listener calling onItemClick callback
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

  - [ ]* 6.6 Write property test for lost auction display
    - **Property 12: Lost Auction Display Completeness**
    - **Validates: Requirements 5.2, 5.3, 5.4, 5.5**

  - [x] 6.7 Write unit tests for adapters
    - Test LiveAuctionAdapter binds data correctly
    - Test countdown timer updates and cancellation
    - Test WonAuctionAdapter binds data correctly
    - Test LostAuctionAdapter binds data correctly
    - Test click listeners invoke callbacks with correct data
    - _Requirements: 3.1, 4.1, 5.1, 6.1_

- [x] 7. Update YourAuctionsActivity with real data integration
  - [x] 7.1 Initialize BidsViewModel and setup observers
    - Add `private lateinit var viewModel: BidsViewModel` property
    - Initialize viewModel in onCreate() using ViewModelProvider
    - Create `setupObservers()` method observing bidsState LiveData
    - Handle Loading state: show progress indicator, hide RecyclerViews
    - Handle Success state: hide progress indicator, update adapters with categorized bids
    - Handle Error state: show error message and retry button
    - Call `viewModel.fetchBids()` in onCreate()
    - _Requirements: 9.1, 9.2, 9.3, 9.4_

  - [ ]* 7.2 Write property tests for UI state handling
    - **Property 17: Loading State Display**
    - **Property 18: Error State Display**
    - **Property 19: Retry Mechanism**
    - **Validates: Requirements 9.1, 9.2, 9.3, 9.4, 9.5**

  - [x] 7.3 Replace dummy adapters with real adapters
    - Initialize LiveAuctionAdapter, WonAuctionAdapter, LostAuctionAdapter in onCreate()
    - Pass click listener lambda launching ItemDetailActivity with listing_id
    - Set adapters on respective RecyclerViews for each tab
    - Remove dummy adapter code
    - _Requirements: 3.1, 4.1, 5.1, 6.1, 6.2_

  - [ ]* 7.4 Write property test for navigation data passing
    - **Property 13: Navigation Data Passing**
    - **Validates: Requirements 6.1, 6.2**

  - [x] 7.5 Implement lifecycle-aware auto-refresh
    - Override `onResume()` calling `viewModel.startAutoRefresh()`
    - Override `onPause()` calling `viewModel.stopAutoRefresh()`
    - _Requirements: 7.1, 7.5_

  - [ ]* 7.6 Write property test for auto-refresh interval
    - **Property 14: Auto-Refresh Interval**
    - **Validates: Requirements 7.1**

  - [x] 7.7 Implement empty state views for each tab
    - Add TextView for empty state in each tab's layout (if not exists)
    - Show "No active bids. Start bidding on auctions!" when liveBids is empty
    - Show "No won auctions yet. Keep bidding!" when wonBids is empty
    - Show "No lost auctions" when lostBids is empty
    - Center empty state messages vertically and horizontally
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

  - [x] 7.8 Implement retry button functionality
    - Add retry button in error state layout (if not exists)
    - Set click listener calling `viewModel.fetchBids()`
    - _Requirements: 9.4, 9.5_

  - [x] 7.9 Write unit tests for YourAuctionsActivity
    - Test ViewModel initialization and observer setup
    - Test loading state displays progress indicator
    - Test success state updates adapters
    - Test error state displays error message and retry button
    - Test retry button triggers fetchBids()
    - Test empty states display correct messages
    - Test lifecycle methods call startAutoRefresh/stopAutoRefresh
    - Test item click launches ItemDetailActivity with correct listing_id
    - _Requirements: 6.1, 6.2, 7.1, 7.5, 8.1, 8.2, 8.3, 9.1, 9.2, 9.3, 9.4, 9.5_

- [x] 8. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Handle bid category migration on auction end
  - [x] 9.1 Implement auction end detection in auto-refresh
    - In BidsViewModel auto-refresh loop, detect when live auction end_time is reached
    - Re-categorize bids after each refresh to move ended auctions to won/lost
    - Update UI state with new categorization
    - _Requirements: 7.4_

  - [ ]* 9.2 Write property test for bid category migration
    - **Property 15: Bid Category Migration on Auction End**
    - **Validates: Requirements 7.4**

  - [x] 9.3 Write unit test for auction end detection
    - Test live auction moves to won list when end_time reached and user has highest bid
    - Test live auction moves to lost list when end_time reached and user doesn't have highest bid
    - _Requirements: 7.4_

- [x] 10. Create or update layout files if needed
  - [x] 10.1 Create/update item_auction_live.xml layout
    - Add ImageView for listing image
    - Add TextViews for title, description, your bid, highest bid, countdown, status
    - Style status indicator with appropriate colors
    - _Requirements: 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_

  - [x] 10.2 Create/update item_auction_won.xml layout
    - Add ImageView for listing image
    - Add TextViews for title, description, winning bid, end time, status
    - Style "Won" status indicator
    - _Requirements: 4.2, 4.3, 4.4, 4.5_

  - [x] 10.3 Create/update item_auction_lost.xml layout
    - Add ImageView for listing image
    - Add TextViews for title, description, your bid, winning bid, end time
    - _Requirements: 5.2, 5.3, 5.4, 5.5_

  - [x] 10.4 Update YourAuctionsActivity layout with loading/error states
    - Add ProgressBar for loading state
    - Add error message TextView and retry button
    - Add empty state TextViews for each tab
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 9.1, 9.2, 9.3, 9.4_

- [x] 11. Final integration and testing
  - [x] 11.1 Test end-to-end flow with real Supabase data
    - Verify bids are fetched and categorized correctly
    - Verify countdown timers update in real-time
    - Verify auto-refresh works every 30 seconds
    - Verify navigation to ItemDetailActivity works
    - Verify empty states display correctly
    - Verify error handling and retry works
    - _Requirements: All_

  - [x] 11.2 Test lifecycle transitions
    - Verify auto-refresh stops when activity is paused
    - Verify auto-refresh resumes when activity is resumed
    - Verify countdown timers are cancelled when views are recycled
    - _Requirements: 7.5_

  - [x] 11.3 Test edge cases
    - Test with user having no bids
    - Test with user having only live bids
    - Test with user having only won/lost bids
    - Test with very large bid amounts
    - Test with auctions ending in < 1 minute
    - _Requirements: 8.1, 8.2, 8.3, 10.1, 10.3, 10.4_

- [x] 12. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties using Kotest
- Unit tests validate specific examples and edge cases
- The implementation reuses existing patterns from ListingsRepository and HomeViewModel
- Supabase queries follow the multi-step approach due to Postgrest 2.0.0 limitations
