# Implementation Plan: Supabase Direct Integration Migration

## Overview

This plan migrates the MineTeh Android application from a PHP backend with Retrofit to direct Supabase integration using the Supabase Kotlin SDK. The migration follows a 7-phase strategy that ensures incremental progress, maintains existing functionality, and includes comprehensive testing at each stage.

## Tasks

- [ ] 1. Phase 1: Setup and Configuration
  - [x] 1.1 Add Supabase dependencies to build.gradle.kts
    - Add postgrest-kt, auth-kt, storage-kt dependencies (version 2.0.0)
    - Add ktor-client-android dependency (version 2.3.7)
    - Add kotlinx-serialization plugin and dependencies
    - _Requirements: 1.1_
  
  - [x] 1.2 Create SupabaseClient singleton module
    - Create `app/src/main/java/com/example/mineteh/supabase/SupabaseClient.kt`
    - Implement singleton pattern with lateinit client property
    - Add initialize() method with Supabase URL and anon key configuration
    - Install Auth, Postgrest, and Storage modules
    - Add convenience properties for auth, database, and storage access
    - _Requirements: 1.2, 1.3, 1.4_
  
  - [x] 1.3 Initialize Supabase client in MineTehApplication
    - Update MineTehApplication.onCreate() to call SupabaseClient.initialize()
    - Store Supabase anon key in BuildConfig or local.properties
    - _Requirements: 1.4_
  
  - [ ]* 1.4 Write unit tests for Supabase client initialization
    - Test singleton pattern (same instance returned)
    - Test client configuration with correct URL and key
    - Test module installation (Auth, Postgrest, Storage)
    - **Property 1: Supabase Client Singleton**
    - **Validates: Requirements 1.4**

- [ ] 2. Phase 2: AuthRepository Migration
  - [x] 2.1 Update TokenManager for Supabase session management
    - Add saveSession() and getSession() methods
    - Add getAccessToken() and getRefreshToken() methods
    - Update existing methods to work with Supabase session format
    - Maintain backward compatibility with existing token storage
    - _Requirements: 2.3, 2.5_
  
  - [x] 2.2 Implement AuthRepository login with Supabase Auth
    - Replace apiService.login() with SupabaseClient.auth.signInWith(Email)
    - Extract user data from Supabase session
    - Store session using TokenManager.saveSession()
    - Map Supabase exceptions to Resource.Error with user-friendly messages
    - Return Resource.Success with LoginData on success
    - _Requirements: 2.2, 2.3, 2.7_
  
  - [x] 2.3 Implement AuthRepository register with Supabase Auth
    - Replace apiService.register() with SupabaseClient.auth.signUp()
    - Store user metadata (username, firstName, lastName) in Supabase user profile
    - Create corresponding account record in accounts table
    - Store session using TokenManager.saveSession()
    - Handle duplicate email errors with descriptive messages
    - _Requirements: 2.1, 2.6_
  
  - [x] 2.4 Implement AuthRepository logout with Supabase Auth
    - Replace apiService.logout() with SupabaseClient.auth.signOut()
    - Clear session using TokenManager.clearAll()
    - Return Resource.Success on successful logout
    - _Requirements: 2.4, 2.5_
  
  - [x] 2.5 Implement getCurrentUser method
    - Use SupabaseClient.auth.currentUserOrNull() to get current user
    - Query accounts table to get full user profile data
    - Return Resource.Success with User object
    - Handle unauthenticated state with appropriate error
    - _Requirements: 7.1, 7.3_
  
  - [ ]* 2.6 Write unit tests for AuthRepository
    - Test login with valid credentials
    - Test login with invalid credentials (error handling)
    - Test register with valid data
    - Test register with duplicate email (error handling)
    - Test logout clears session
    - Test getCurrentUser returns correct user data
  
  - [ ]* 2.7 Write property tests for authentication
    - **Property 2: User Registration Creates Account**
    - **Property 3: Authentication State Persistence**
    - **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5**
  
  - [x] 2.8 Test AuthRepository with existing LoginViewModel and SignupViewModel
    - Verify login flow works end-to-end
    - Verify registration flow works end-to-end
    - Verify logout flow works end-to-end
    - Ensure no breaking changes to ViewModel interfaces
    - _Requirements: 8.5, 10.1_

- [ ] 3. Checkpoint - Verify authentication works
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 4. Phase 3: ListingsRepository Migration
  - [x] 4.1 Implement getListings with Supabase Postgrest
    - Replace apiService.getListings() with Postgrest query on listings table
    - Apply filters using .eq(), .like(), .order() for category, type, search
    - Implement pagination with .range() for limit and offset
    - Join with listing_images and accounts tables for complete data
    - Parse response into List<Listing>
    - _Requirements: 4.1, 4.6_
  
  - [ ] 4.2 Implement getUserListings with ownership filtering
    - Query listings table with .eq("account_id", currentUserId)
    - Join with listing_images table
    - Return only listings owned by authenticated user
    - _Requirements: 4.2_
  
  - [ ] 4.3 Implement getListing for single item retrieval
    - Query listings table with .eq("listing_id", id).single()
    - Join with listing_images, accounts, and bids tables
    - Include highest bid information
    - Include is_favorited status for current user
    - _Requirements: 4.1, 4.6_
  
  - [ ] 4.4 Implement createListing with Supabase Storage for images
    - Upload images to Supabase Storage bucket
    - Insert listing record into listings table with current user as owner
    - Insert image records into listing_images table with storage paths
    - Return created listing with all associated data
    - Handle storage errors gracefully
    - _Requirements: 4.3_
  
  - [ ] 4.5 Implement updateListing with authorization check
    - Verify current user owns the listing (RLS policy enforces this)
    - Update listing record with provided fields
    - Handle image updates if new images provided
    - Return updated listing data
    - _Requirements: 4.4, 4.5_
  
  - [ ]* 4.6 Write unit tests for ListingsRepository
    - Test getListings with various filters
    - Test getListings pagination
    - Test getUserListings returns only user's listings
    - Test getListing returns complete data
    - Test createListing stores correct owner
    - Test updateListing modifies data correctly
    - Test updateListing rejects unauthorized updates
  
  - [ ]* 4.7 Write property tests for listings
    - **Property 6: Listing Ownership Filtering**
    - **Property 7: Listing Creation Ownership**
    - **Property 8: Listing Update Persistence**
    - **Property 9: Listing Serialization Round-Trip**
    - **Validates: Requirements 4.2, 4.3, 4.4, 4.6**
  
  - [ ] 4.8 Test ListingsRepository with existing ViewModels
    - Test with HomeViewModel for listing display
    - Test with ListingsViewModel for filtering and search
    - Test with CreateListingViewModel for creation
    - Test with ListingsDetailViewModel for single item view
    - Verify no breaking changes to ViewModel interfaces
    - _Requirements: 8.5, 10.2_

- [ ] 5. Phase 4: BidsRepository and FavoritesRepository Migration
  - [ ] 5.1 Implement placeBid with Supabase Postgrest
    - Insert bid record into bids table with current user and listing IDs
    - Handle duplicate bid constraint violations
    - Return created bid data with bidder information
    - _Requirements: 5.1_
  
  - [ ] 5.2 Implement getBidsForListing
    - Query bids table with .eq("listing_id", listingId)
    - Join with accounts table to get bidder information
    - Order by bid_amount descending
    - _Requirements: 5.2_
  
  - [ ] 5.3 Implement getUserBids
    - Query bids table with .eq("account_id", currentUserId)
    - Join with listings and accounts tables
    - Return all bids placed by current user
    - _Requirements: 5.3_
  
  - [ ] 5.4 Implement toggleFavorite with upsert logic
    - Check if favorite exists for user and listing
    - If exists, delete the favorite record
    - If not exists, insert new favorite record
    - Return favorite status (added or removed)
    - _Requirements: 6.1, 6.2, 6.4_
  
  - [ ] 5.5 Implement getFavorites with join
    - Query favorites table with .eq("account_id", currentUserId)
    - Join with listings table to get full listing data
    - Join with listing_images for images
    - Return list of favorited listings
    - _Requirements: 6.3_
  
  - [ ] 5.6 Implement isFavorited check
    - Query favorites table for specific user and listing combination
    - Return boolean indicating favorite status
    - _Requirements: 6.3_
  
  - [ ]* 5.7 Write unit tests for BidsRepository
    - Test placeBid creates bid with correct associations
    - Test placeBid rejects unauthenticated requests
    - Test getBidsForListing returns only bids for that listing
    - Test getUserBids returns only user's bids
  
  - [ ]* 5.8 Write unit tests for FavoritesRepository
    - Test toggleFavorite adds favorite when not exists
    - Test toggleFavorite removes favorite when exists
    - Test getFavorites returns only user's favorites
    - Test isFavorited returns correct status
  
  - [ ]* 5.9 Write property tests for bids and favorites
    - **Property 10: Bid Creation with Associations**
    - **Property 11: Query Filtering by Association**
    - **Property 12: Favorites Management Idempotence**
    - **Validates: Requirements 5.1, 5.2, 5.3, 6.1, 6.2, 6.3, 6.4**
  
  - [ ] 5.10 Test repositories with existing ViewModels
    - Test BidsRepository with BidActivity and BidDetailActivity
    - Test FavoritesRepository with FavoritesViewModel and SavedItemsActivity
    - Verify no breaking changes to ViewModel interfaces
    - _Requirements: 8.5, 10.3, 10.4_

- [ ] 6. Checkpoint - Verify all repositories work
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 7. Phase 5: Profile Management and Error Handling
  - [ ] 7.1 Implement updateProfile in AuthRepository
    - Update user metadata in Supabase Auth
    - Update corresponding account record in accounts table
    - Handle validation errors with descriptive messages
    - Return updated user profile
    - _Requirements: 7.2, 7.4_
  
  - [ ] 7.2 Create centralized error handling utility
    - Create handleSupabaseError() function to map exceptions
    - Map RestException to user-friendly messages
    - Map HttpRequestException to network error messages
    - Map PostgrestException to database error messages
    - Ensure consistent error types across all repositories
    - _Requirements: 8.6_
  
  - [ ] 7.3 Implement token refresh handling
    - Configure Supabase Auth with autoRefresh = true
    - Add error handling for token refresh failures
    - Clear session and redirect to login on refresh failure
    - _Requirements: 3.2, 3.3_
  
  - [ ] 7.4 Add request authentication interceptor
    - Verify Supabase SDK automatically includes auth token in headers
    - Test authenticated requests include correct Authorization header
    - _Requirements: 3.1_
  
  - [ ]* 7.5 Write unit tests for profile management
    - Test updateProfile modifies user data
    - Test updateProfile validation errors
    - Test getCurrentUser extracts correct user ID
  
  - [ ]* 7.6 Write property tests for profile and error handling
    - **Property 13: Profile Data Round-Trip**
    - **Property 14: User ID Extraction from Token**
    - **Property 15: Repository Error Type Consistency**
    - **Validates: Requirements 7.1, 7.2, 7.3, 8.6**
  
  - [ ]* 7.7 Write unit tests for token management
    - Test token refresh on expiration
    - Test authenticated requests include token
    - Test session cleared on refresh failure
    - **Property 4: Authenticated Requests Include Token**
    - **Property 5: Token Refresh on Expiration**
    - **Validates: Requirements 3.1, 3.2, 3.3**

- [ ] 8. Phase 6: Integration Testing and Validation
  - [ ] 8.1 Run full unit test suite
    - Execute all repository unit tests
    - Execute all ViewModel integration tests
    - Verify 80% minimum code coverage
    - _Requirements: 10.1, 10.2, 10.3, 10.4_
  
  - [ ]* 8.2 Run full property test suite
    - Execute all property tests with 100+ iterations each
    - Verify all 15 properties pass
    - Document any property test failures
  
  - [ ] 8.3 Manual end-to-end testing
    - Test complete registration flow
    - Test complete login flow
    - Test listing creation with image upload
    - Test listing browsing and filtering
    - Test bid placement flow
    - Test favorites add/remove flow
    - Test profile update flow
    - Test logout flow
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6_
  
  - [ ] 8.4 Test error scenarios
    - Test invalid credentials login
    - Test duplicate email registration
    - Test unauthorized listing update
    - Test unauthenticated bid placement
    - Test network timeout handling
    - Test token expiration and refresh
    - _Requirements: 2.6, 2.7, 4.5, 5.4_
  
  - [ ] 8.5 Performance testing
    - Benchmark listing query response times
    - Test with large datasets (100+ listings)
    - Test concurrent operations (multiple users)
    - Compare performance with old PHP backend
    - _Requirements: 10.6_

- [ ] 9. Checkpoint - Verify all functionality works
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 10. Phase 7: Cleanup and Retrofit Removal
  - [ ] 10.1 Remove Retrofit dependencies from build.gradle.kts
    - Remove retrofit library dependency
    - Remove converter-gson dependency
    - Remove okhttp3 logging-interceptor dependency
    - _Requirements: 9.1_
  
  - [ ] 10.2 Delete ApiService interface
    - Delete `app/src/main/java/com/example/mineteh/model/ApiService.kt`
    - _Requirements: 9.2_
  
  - [ ] 10.3 Delete ApiClient configuration
    - Delete `app/src/main/java/com/example/mineteh/ApiClient.kt`
    - _Requirements: 9.4_
  
  - [ ] 10.4 Delete custom deserializers
    - Delete `app/src/main/java/com/example/mineteh/network/ApiResponseDeserializer.kt`
    - Delete `app/src/main/java/com/example/mineteh/network/ListingDeserializer.kt`
    - _Requirements: 9.3_
  
  - [ ] 10.5 Remove unused network configuration
    - Remove any PHP backend URL constants
    - Remove Retrofit-specific annotations from data models
    - Clean up any remaining Retrofit imports
    - _Requirements: 9.4_
  
  - [ ] 10.6 Verify build succeeds after cleanup
    - Run ./gradlew clean build
    - Ensure no compilation errors
    - Ensure no missing dependency errors
    - _Requirements: 9.1, 9.2, 9.3, 9.4_
  
  - [ ] 10.7 Run final test suite
    - Execute all unit tests
    - Execute all integration tests
    - Verify no regressions from cleanup
    - _Requirements: 10.1, 10.2, 10.3, 10.4_

- [ ] 11. Final Checkpoint - Migration Complete
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation at each phase
- Property tests validate universal correctness properties across all inputs
- Unit tests validate specific examples, edge cases, and error conditions
- The migration maintains existing ViewModel and UI layer interfaces to minimize breaking changes
- Supabase SDK handles token refresh automatically, simplifying authentication management
- Row Level Security (RLS) policies in Supabase enforce authorization at the database level
- All repository methods maintain the Resource<T> return type for consistent error handling

## Migration Strategy Summary

1. **Phase 1**: Set up Supabase SDK and configuration
2. **Phase 2**: Migrate authentication (AuthRepository)
3. **Phase 3**: Migrate listings management (ListingsRepository)
4. **Phase 4**: Migrate bids and favorites (BidsRepository, FavoritesRepository)
5. **Phase 5**: Add profile management and centralized error handling
6. **Phase 6**: Comprehensive testing and validation
7. **Phase 7**: Remove Retrofit and clean up old code

Each phase builds on the previous one, ensuring the application remains functional throughout the migration process.
