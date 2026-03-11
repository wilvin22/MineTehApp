# Design Document: Supabase Direct Integration Migration

## Overview

This design outlines the migration of the MineTeh Android application from a PHP backend with Retrofit HTTP client to direct Supabase integration using the Supabase Kotlin SDK. The migration addresses reliability issues with the current PHP backend (returning HTML instead of JSON) and simplifies the architecture by eliminating the intermediary PHP layer.

### Current Architecture

The application currently uses:
- Retrofit 2.11.0 for HTTP communication
- PHP backend hosted on InfinityFree (https://mineteh.infinityfree.me/)
- Custom JSON deserializers to handle malformed responses
- Token-based authentication with manual header injection
- Repository pattern with ApiService interface

### Target Architecture

The new architecture will use:
- Supabase Kotlin SDK for direct database and auth operations
- Supabase Auth for user authentication and session management
- Supabase Database (PostgreSQL) accessed via REST API
- Automatic token management and refresh via SDK
- Maintained repository pattern with updated implementations

### Migration Benefits

1. **Reliability**: Eliminates PHP backend issues (HTML responses, parsing errors)
2. **Simplicity**: Direct SDK integration removes HTTP layer complexity
3. **Type Safety**: Supabase SDK provides type-safe database operations
4. **Automatic Token Management**: SDK handles token refresh automatically
5. **Real-time Capabilities**: Future support for real-time subscriptions
6. **Reduced Maintenance**: No PHP backend to maintain

## Architecture

### Component Overview

```
┌─────────────────────────────────────────────────────────────┐
│                         UI Layer                             │
│  (Activities, Fragments, Adapters - No Changes)             │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│                      ViewModel Layer                         │
│  (ViewModels - Minimal Changes)                             │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│                    Repository Layer                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │    Auth      │  │   Listings   │  │     Bids     │     │
│  │  Repository  │  │  Repository  │  │  Repository  │     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
│         │                  │                  │              │
│  ┌──────▼──────────────────▼──────────────────▼───────┐    │
│  │           Supabase Client (Singleton)              │    │
│  └──────┬──────────────────┬──────────────────┬───────┘    │
└─────────┼──────────────────┼──────────────────┼────────────┘
          │                  │                  │
┌─────────▼──────┐  ┌────────▼────────┐  ┌─────▼──────────┐
│  Supabase Auth │  │ Supabase        │  │  Supabase      │
│                │  │ Database        │  │  Storage       │
└────────────────┘  └─────────────────┘  └────────────────┘
```

### Supabase Client Configuration

The Supabase client will be initialized as a singleton in the Application class:

```kotlin
object SupabaseClient {
    lateinit var client: SupabaseClient
        private set
    
    fun initialize(context: Context) {
        client = createSupabaseClient(
            supabaseUrl = "https://didpavzminvohszuuowu.supabase.co",
            supabaseKey = "[ANON_KEY]"
        ) {
            install(Auth)
            install(Postgrest)
            install(Storage)
        }
    }
}
```

### Authentication Flow

```
┌──────────┐         ┌──────────────┐         ┌──────────────┐
│   User   │         │ AuthRepository│         │ Supabase Auth│
└────┬─────┘         └──────┬───────┘         └──────┬───────┘
     │                      │                         │
     │  login(email, pwd)   │                         │
     ├─────────────────────>│                         │
     │                      │  signInWithEmail()      │
     │                      ├────────────────────────>│
     │                      │                         │
     │                      │  <Session + JWT>        │
     │                      │<────────────────────────┤
     │                      │                         │
     │                      │  saveToken()            │
     │                      ├──────────┐              │
     │                      │          │              │
     │                      │<─────────┘              │
     │  Resource.Success    │                         │
     │<─────────────────────┤                         │
     │                      │                         │
```

### Data Flow Pattern

All repository operations follow this pattern:

1. Check authentication status (if required)
2. Execute Supabase operation (query, insert, update, delete)
3. Handle response or error
4. Return Resource<T> (Success or Error)

## Components and Interfaces

### 1. Supabase Client Module

**File**: `app/src/main/java/com/example/mineteh/supabase/SupabaseClient.kt`

```kotlin
object SupabaseClient {
    lateinit var client: SupabaseClient
        private set
    
    fun initialize(context: Context) {
        client = createSupabaseClient(
            supabaseUrl = "https://didpavzminvohszuuowu.supabase.co",
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth) {
                // Auto-refresh tokens
                autoRefresh = true
            }
            install(Postgrest)
            install(Storage)
        }
    }
    
    val auth: Auth get() = client.auth
    val database: Postgrest get() = client.postgrest
    val storage: Storage get() = client.storage
}
```

### 2. AuthRepository (Updated)

**File**: `app/src/main/java/com/example/mineteh/model/repository/AuthRepository.kt`

**Public Interface** (maintained for compatibility):
```kotlin
class AuthRepository(context: Context) {
    suspend fun login(identifier: String, password: String): Resource<LoginData>
    suspend fun register(username: String, email: String, password: String, 
                        firstName: String, lastName: String): Resource<RegisterData>
    suspend fun logout(): Resource<Unit>
    suspend fun getCurrentUser(): Resource<User>
}
```

**Implementation Changes**:
- Replace `apiService.login()` with `SupabaseClient.auth.signInWith(Email)`
- Replace `apiService.register()` with `SupabaseClient.auth.signUp()`
- Use `SupabaseClient.auth.currentSessionOrNull()` for session management
- Store user metadata in Supabase user profile

### 3. ListingsRepository (Updated)

**File**: `app/src/main/java/com/example/mineteh/model/repository/ListingsRepository.kt`

**Public Interface** (maintained):
```kotlin
class ListingsRepository(context: Context) {
    suspend fun getListings(category: String?, type: String?, search: String?, 
                           limit: Int, offset: Int): Resource<List<Listing>>
    suspend fun getListing(id: Int): Resource<Listing>
    suspend fun createListing(title: String, description: String, price: Double,
                             location: String, category: String, listingType: String,
                             endTime: String?, minBidIncrement: Double?, 
                             imageUris: List<Uri>): Resource<Listing>
    suspend fun updateListing(id: Int, updates: Map<String, Any>): Resource<Listing>
    suspend fun getUserListings(): Resource<List<Listing>>
}
```

**Implementation Changes**:
- Replace Retrofit calls with Supabase database queries
- Use `.select()`, `.insert()`, `.update()` operations
- Apply filters using `.eq()`, `.like()`, `.order()` methods
- Handle image uploads via Supabase Storage

### 4. BidsRepository (Updated)

**File**: `app/src/main/java/com/example/mineteh/model/repository/BidsRepository.kt`

**Public Interface** (maintained):
```kotlin
class BidsRepository(context: Context) {
    suspend fun placeBid(listingId: Int, bidAmount: Double): Resource<BidData>
    suspend fun getBidsForListing(listingId: Int): Resource<List<Bid>>
    suspend fun getUserBids(): Resource<List<Bid>>
}
```

**Implementation Changes**:
- Replace Retrofit calls with Supabase database inserts/queries
- Use RPC calls for complex bid validation logic

### 5. FavoritesRepository (Updated)

**File**: `app/src/main/java/com/example/mineteh/model/repository/FavoritesRepository.kt`

**Public Interface** (maintained):
```kotlin
class FavoritesRepository(context: Context) {
    suspend fun toggleFavorite(listingId: Int): Resource<FavoriteData>
    suspend fun getFavorites(): Resource<List<Listing>>
    suspend fun isFavorited(listingId: Int): Resource<Boolean>
}
```

**Implementation Changes**:
- Replace Retrofit calls with Supabase database operations
- Use `.upsert()` for toggle functionality
- Join with listings table to get full listing data

### 6. TokenManager (Updated)

**File**: `app/src/main/java/com/example/mineteh/TokenManager.kt`

**Changes**:
- Store Supabase session instead of just token
- Add methods to retrieve access token and refresh token
- Integrate with Supabase session management
- Keep existing SharedPreferences for user metadata

```kotlin
class TokenManager(context: Context) {
    // Existing methods maintained
    fun saveToken(token: String)
    fun getToken(): String?
    fun saveUserId(userId: Int)
    fun getUserId(): Int
    fun clearAll()
    fun isLoggedIn(): Boolean
    
    // New methods for Supabase
    fun saveSession(session: Session)
    fun getSession(): Session?
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
}
```

## Data Models

### Database Schema

The Supabase PostgreSQL database will have the following tables:

#### accounts table
```sql
CREATE TABLE accounts (
    account_id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    auth_user_id UUID REFERENCES auth.users(id),
    created_at TIMESTAMP DEFAULT NOW()
);
```

#### listings table
```sql
CREATE TABLE listings (
    listing_id SERIAL PRIMARY KEY,
    account_id INTEGER REFERENCES accounts(account_id),
    title VARCHAR(200) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    location VARCHAR(100),
    category VARCHAR(50),
    listing_type VARCHAR(10) CHECK (listing_type IN ('FIXED', 'BID')),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    end_time TIMESTAMP,
    min_bid_increment DECIMAL(10, 2),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
```

#### listing_images table
```sql
CREATE TABLE listing_images (
    image_id SERIAL PRIMARY KEY,
    listing_id INTEGER REFERENCES listings(listing_id) ON DELETE CASCADE,
    image_path TEXT NOT NULL,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);
```

#### bids table
```sql
CREATE TABLE bids (
    bid_id SERIAL PRIMARY KEY,
    listing_id INTEGER REFERENCES listings(listing_id) ON DELETE CASCADE,
    account_id INTEGER REFERENCES accounts(account_id),
    bid_amount DECIMAL(10, 2) NOT NULL,
    bid_time TIMESTAMP DEFAULT NOW(),
    UNIQUE(listing_id, account_id, bid_amount)
);
```

#### favorites table
```sql
CREATE TABLE favorites (
    favorite_id SERIAL PRIMARY KEY,
    account_id INTEGER REFERENCES accounts(account_id),
    listing_id INTEGER REFERENCES listings(listing_id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(account_id, listing_id)
);
```

### Kotlin Data Models

The existing data models will be maintained with minor adjustments:

```kotlin
@Serializable
data class Listing(
    @SerialName("listing_id") val id: Int,
    val title: String,
    val description: String,
    val price: Double,
    val location: String,
    val category: String,
    @SerialName("listing_type") val listingType: String,
    val status: String,
    val images: List<ListingImage>? = null,
    val seller: Seller? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("is_favorited") val isFavorited: Boolean = false,
    @SerialName("highest_bid") val highestBid: Bid? = null,
    @SerialName("end_time") val endTime: String? = null
)

@Serializable
data class User(
    @SerialName("account_id") val accountId: Int,
    val username: String,
    val email: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    @SerialName("auth_user_id") val authUserId: String? = null
)

@Serializable
data class Bid(
    @SerialName("bid_id") val bidId: Int? = null,
    @SerialName("listing_id") val listingId: Int,
    @SerialName("account_id") val accountId: Int,
    @SerialName("bid_amount") val bidAmount: Double,
    @SerialName("bid_time") val bidTime: String,
    val bidder: Bidder? = null
)

@Serializable
data class Favorite(
    @SerialName("favorite_id") val favoriteId: Int? = null,
    @SerialName("account_id") val accountId: Int,
    @SerialName("listing_id") val listingId: Int,
    @SerialName("created_at") val createdAt: String
)
```

### Row Level Security (RLS) Policies

Supabase RLS policies will enforce data access rules:

```sql
-- Listings: Anyone can read, only owner can update/delete
ALTER TABLE listings ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Listings are viewable by everyone"
ON listings FOR SELECT
USING (true);

CREATE POLICY "Users can insert their own listings"
ON listings FOR INSERT
WITH CHECK (auth.uid() = (SELECT auth_user_id FROM accounts WHERE account_id = listings.account_id));

CREATE POLICY "Users can update their own listings"
ON listings FOR UPDATE
USING (auth.uid() = (SELECT auth_user_id FROM accounts WHERE account_id = listings.account_id));

-- Bids: Anyone can read, authenticated users can insert
ALTER TABLE bids ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Bids are viewable by everyone"
ON bids FOR SELECT
USING (true);

CREATE POLICY "Authenticated users can place bids"
ON bids FOR INSERT
WITH CHECK (auth.uid() IS NOT NULL);

-- Favorites: Users can only see and modify their own
ALTER TABLE favorites ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their own favorites"
ON favorites FOR SELECT
USING (auth.uid() = (SELECT auth_user_id FROM accounts WHERE account_id = favorites.account_id));

CREATE POLICY "Users can insert their own favorites"
ON favorites FOR INSERT
WITH CHECK (auth.uid() = (SELECT auth_user_id FROM accounts WHERE account_id = favorites.account_id));

CREATE POLICY "Users can delete their own favorites"
ON favorites FOR DELETE
USING (auth.uid() = (SELECT auth_user_id FROM accounts WHERE account_id = favorites.account_id));
```


## Correctness Properties

A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.

### Property 1: Supabase Client Singleton

For any number of calls to access the Supabase client throughout the application lifecycle, all calls should return the same instance reference.

**Validates: Requirements 1.4**

### Property 2: User Registration Creates Account

For any valid email and password combination, when a user registers, the Supabase Auth system should create a new user account and return a valid authentication token.

**Validates: Requirements 2.1, 2.2**

### Property 3: Authentication State Persistence

For any successful authentication (login or register), the TokenManager should store the authentication token, and for any logout operation, the TokenManager should clear the stored token.

**Validates: Requirements 2.3, 2.4, 2.5**

### Property 4: Authenticated Requests Include Token

For any database request made by an authenticated user, the Supabase client should automatically include the authentication token in the request headers.

**Validates: Requirements 3.1**

### Property 5: Token Refresh on Expiration

For any expired authentication token, when a request is made, the Supabase client should automatically refresh the token before proceeding with the request.

**Validates: Requirements 3.2**

### Property 6: Listing Ownership Filtering

For any user and any set of listings in the database, when the user requests their own listings, the returned set should contain only listings where the owner ID matches the user's ID.

**Validates: Requirements 4.2**

### Property 7: Listing Creation Ownership

For any authenticated user and any valid listing data, when the user creates a listing, the stored listing should have the user's ID as the owner field.

**Validates: Requirements 4.3**

### Property 8: Listing Update Persistence

For any authenticated user and any of their own listings, when the user updates the listing with new data, retrieving the listing should return the updated data.

**Validates: Requirements 4.4**

### Property 9: Listing Serialization Round-Trip

For any valid Listing object, serializing it to JSON and then deserializing back should produce an equivalent Listing object.

**Validates: Requirements 4.6**

### Property 10: Bid Creation with Associations

For any authenticated user and any listing, when the user places a bid, the stored bid should contain both the user's ID and the listing's ID.

**Validates: Requirements 5.1**

### Property 11: Query Filtering by Association

For any listing with associated bids, when querying bids for that listing, all returned bids should have a listing_id matching the queried listing. Similarly, for any user with bids, when querying that user's bids, all returned bids should have an account_id matching the user.

**Validates: Requirements 5.2, 5.3, 6.3**

### Property 12: Favorites Management Idempotence

For any authenticated user and any listing, adding the listing to favorites multiple times should result in exactly one favorite record, and removing it should delete the record. Adding then removing should return to the original state (no favorite record).

**Validates: Requirements 6.1, 6.2, 6.4**

### Property 13: Profile Data Round-Trip

For any authenticated user, updating their profile with new data and then retrieving their profile should return the updated data.

**Validates: Requirements 7.1, 7.2**

### Property 14: User ID Extraction from Token

For any valid authentication token, the application should be able to extract the user's ID from the token.

**Validates: Requirements 7.3**

### Property 15: Repository Error Type Consistency

For any repository operation that fails, the returned error should be of type Resource.Error with a descriptive message, consistent with the existing error handling pattern.

**Validates: Requirements 8.6**

## Error Handling

### Error Categories

The application will handle four categories of errors:

1. **Authentication Errors**
   - Invalid credentials
   - Expired session
   - Missing authentication
   - Token refresh failure

2. **Database Errors**
   - Query failures
   - Constraint violations (duplicate email, unique constraints)
   - Authorization failures (RLS policy violations)
   - Network timeouts

3. **Validation Errors**
   - Invalid input data
   - Missing required fields
   - Format errors

4. **Storage Errors**
   - Image upload failures
   - File size limits
   - Unsupported formats

### Error Handling Strategy

All repository methods return `Resource<T>` sealed class:

```kotlin
sealed class Resource<out T> {
    data class Success<T>(val data: T?) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}
```

### Error Mapping

Supabase exceptions will be mapped to user-friendly error messages:

```kotlin
private fun handleSupabaseError(e: Exception): Resource.Error {
    return when (e) {
        is RestException -> {
            when (e.error) {
                "invalid_credentials" -> Resource.Error("Invalid email or password")
                "email_exists" -> Resource.Error("An account with this email already exists")
                "weak_password" -> Resource.Error("Password must be at least 6 characters")
                else -> Resource.Error(e.message ?: "An error occurred")
            }
        }
        is HttpRequestException -> {
            Resource.Error("Network error. Please check your connection.")
        }
        is PostgrestException -> {
            when {
                e.message?.contains("violates foreign key") == true -> 
                    Resource.Error("Referenced item not found")
                e.message?.contains("duplicate key") == true -> 
                    Resource.Error("This item already exists")
                else -> Resource.Error("Database error: ${e.message}")
            }
        }
        else -> Resource.Error(e.message ?: "An unexpected error occurred")
    }
}
```

### Token Expiration Handling

The Supabase SDK automatically handles token refresh, but if refresh fails:

1. Clear stored session from TokenManager
2. Return `Resource.Error("Session expired. Please login again.")`
3. ViewModel observes error and navigates to login screen

### Network Error Handling

For network failures:
- Implement retry logic with exponential backoff (3 attempts)
- Show user-friendly messages
- Cache data locally when possible (future enhancement)

### Validation Error Handling

Client-side validation before API calls:
- Email format validation
- Password strength requirements
- Required field checks
- Price/bid amount validation

## Testing Strategy

### Dual Testing Approach

The testing strategy employs both unit tests and property-based tests to ensure comprehensive coverage:

- **Unit Tests**: Verify specific examples, edge cases, error conditions, and integration points
- **Property-Based Tests**: Verify universal properties across all inputs through randomization

### Unit Testing

Unit tests will focus on:

1. **Configuration Tests**
   - Supabase client initialization with correct URL and key
   - Singleton pattern verification
   - Module installation verification

2. **Edge Cases**
   - Duplicate email registration
   - Invalid credentials login
   - Unauthorized update attempts
   - Unauthenticated bid placement
   - Token refresh failure
   - Profile update validation errors

3. **Integration Tests**
   - Repository interface compatibility (method signatures unchanged)
   - ViewModel integration (existing ViewModels work with new repositories)
   - Error type consistency

4. **Migration Verification**
   - All authentication flows work (login, register, logout)
   - All listing operations work (get, create, update, filter)
   - All bid operations work (place, query)
   - All favorite operations work (add, remove, list)

### Property-Based Testing

Property-based tests will use **Kotest Property Testing** library (recommended for Kotlin):

```kotlin
dependencies {
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-property:5.8.0")
}
```

Each property test will:
- Run minimum 100 iterations with randomized inputs
- Reference the design document property in a comment tag
- Use appropriate generators for domain objects

**Example Property Test Structure**:

```kotlin
class ListingRepositoryPropertyTest : StringSpec({
    "Property 6: Listing Ownership Filtering" {
        // Feature: supabase-direct-integration, Property 6: For any user and any set of listings, 
        // querying user's own listings returns only listings owned by that user
        
        checkAll(100, Arb.int(1..1000), Arb.list(Arb.listing(), 1..50)) { userId, allListings ->
            // Setup: Insert listings into test database
            val userListings = allListings.filter { it.ownerId == userId }
            
            // Execute: Query user's listings
            val result = repository.getUserListings(userId)
            
            // Verify: All returned listings belong to user
            result.data?.forEach { listing ->
                listing.seller?.accountId shouldBe userId
            }
            result.data?.size shouldBe userListings.size
        }
    }
})
```

### Test Configuration

**Test Database**: Use Supabase test project or local Supabase instance
- Separate test database from production
- Reset database state between test runs
- Use test-specific API keys

**Mock Strategy**: 
- Mock Supabase client for unit tests
- Use real Supabase instance for integration tests
- Mock external dependencies (image upload, network)

### Coverage Goals

- Unit test coverage: 80% minimum
- Property test coverage: All properties from design document
- Integration test coverage: All repository public methods
- Edge case coverage: All error conditions from requirements

### Test Execution

```bash
# Run all tests
./gradlew test

# Run unit tests only
./gradlew testDebugUnitTest

# Run property tests only
./gradlew test --tests "*PropertyTest"

# Run with coverage
./gradlew testDebugUnitTestCoverage
```

### Migration Testing Checklist

Before considering migration complete:

- [ ] All unit tests pass
- [ ] All property tests pass (100+ iterations each)
- [ ] All edge case tests pass
- [ ] Integration tests verify ViewModel compatibility
- [ ] Manual testing of all user flows
- [ ] Performance testing (response times comparable to old backend)
- [ ] Error handling tested for all failure scenarios
- [ ] Token refresh tested with expired tokens
- [ ] RLS policies tested for authorization

## Migration Strategy

### Phase 1: Setup and Configuration (Day 1)

1. **Add Supabase Dependencies**
   ```kotlin
   // build.gradle.kts
   implementation("io.github.jan-tennert.supabase:postgrest-kt:2.0.0")
   implementation("io.github.jan-tennert.supabase:auth-kt:2.0.0")
   implementation("io.github.jan-tennert.supabase:storage-kt:2.0.0")
   implementation("io.ktor:ktor-client-android:2.3.7")
   ```

2. **Create Supabase Client Module**
   - Create `SupabaseClient.kt` singleton
   - Initialize in `MineTehApplication.onCreate()`
   - Configure Auth, Postgrest, and Storage modules

3. **Database Schema Setup**
   - Create tables in Supabase dashboard
   - Set up RLS policies
   - Create indexes for performance
   - Migrate existing data from PHP backend

### Phase 2: Repository Migration (Days 2-4)

**Day 2: AuthRepository**
1. Create new `AuthRepository` implementation using Supabase Auth
2. Update `login()` method to use `auth.signInWith(Email)`
3. Update `register()` method to use `auth.signUp()`
4. Update `logout()` method to use `auth.signOut()`
5. Update TokenManager to store Supabase session
6. Write unit tests and property tests
7. Test with existing LoginViewModel and SignupViewModel

**Day 3: ListingsRepository**
1. Update `getListings()` to use Postgrest queries
2. Update `getListing()` for single item retrieval
3. Update `createListing()` with Storage for images
4. Add `updateListing()` method
5. Add `getUserListings()` method
6. Write unit tests and property tests
7. Test with existing ViewModels

**Day 4: BidsRepository and FavoritesRepository**
1. Update BidsRepository methods
2. Update FavoritesRepository methods
3. Write unit tests and property tests
4. Test with existing ViewModels

### Phase 3: Testing and Validation (Day 5)

1. **Run Full Test Suite**
   - Execute all unit tests
   - Execute all property tests (100+ iterations)
   - Execute integration tests

2. **Manual Testing**
   - Test all user flows end-to-end
   - Test error scenarios
   - Test token expiration and refresh
   - Test offline behavior

3. **Performance Testing**
   - Compare response times with old backend
   - Test with large datasets
   - Test concurrent operations

### Phase 4: Cleanup (Day 6)

1. **Remove Retrofit Dependencies**
   ```kotlin
   // Remove from build.gradle.kts
   // implementation("com.squareup.retrofit2:retrofit:2.11.0")
   // implementation("com.squareup.retrofit2:converter-gson:2.11.0")
   // implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
   ```

2. **Delete Old Files**
   - Delete `ApiService.kt`
   - Delete `ApiClient.kt`
   - Delete `ApiResponseDeserializer.kt`
   - Delete `ListingDeserializer.kt`
   - Remove Retrofit-specific code from repositories

3. **Update Documentation**
   - Update README with new architecture
   - Document Supabase setup process
   - Document environment variables

### Phase 5: Deployment (Day 7)

1. **Staging Deployment**
   - Deploy to staging environment
   - Run smoke tests
   - Monitor for errors

2. **Production Deployment**
   - Deploy to production
   - Monitor error rates
   - Monitor performance metrics
   - Have rollback plan ready

### Rollback Plan

If critical issues are discovered:

1. Revert to previous commit (before Retrofit removal)
2. Redeploy old version
3. Investigate issues in staging
4. Fix and redeploy

### Data Migration

**Existing Data Transfer**:
1. Export data from PHP/MySQL backend
2. Transform data to match new schema
3. Import into Supabase PostgreSQL
4. Verify data integrity
5. Update foreign key relationships

**User Account Migration**:
- Create Supabase Auth users for existing accounts
- Link `auth.users.id` to `accounts.auth_user_id`
- Send password reset emails to all users
- Maintain backward compatibility during transition

### Risk Mitigation

**Risk 1: Data Loss**
- Mitigation: Backup all data before migration
- Mitigation: Run parallel systems during transition
- Mitigation: Verify data integrity after migration

**Risk 2: Authentication Issues**
- Mitigation: Test auth flows extensively
- Mitigation: Implement graceful fallback
- Mitigation: Monitor auth error rates

**Risk 3: Performance Degradation**
- Mitigation: Benchmark before and after
- Mitigation: Optimize queries with indexes
- Mitigation: Use Supabase caching

**Risk 4: Breaking Changes**
- Mitigation: Maintain repository interfaces
- Mitigation: Comprehensive integration tests
- Mitigation: Staged rollout

### Success Criteria

Migration is successful when:
- [ ] All tests pass (unit, property, integration)
- [ ] All user flows work correctly
- [ ] No increase in error rates
- [ ] Response times are comparable or better
- [ ] No data loss or corruption
- [ ] Token management works correctly
- [ ] RLS policies enforce correct authorization
- [ ] Image uploads work correctly
- [ ] All edge cases handled properly
- [ ] Documentation is updated

