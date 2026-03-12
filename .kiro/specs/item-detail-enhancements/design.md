# Design Document: Item Detail Enhancements

## Overview

This design specifies the implementation of enhancements to the ItemDetailActivity in the MineTeh Android e-commerce application. The enhancements transform the activity from displaying hardcoded data to a fully functional detail view that loads real listing data from Supabase, displays multiple images in a carousel, implements working favorites, shows seller information, provides bidding functionality for auction listings, and conditionally displays UI elements based on listing type.

The design follows the existing MVVM architecture pattern used throughout the application, leveraging the already-implemented ListingDetailViewModel, ListingsRepository, BidsRepository, and FavoritesRepository. The UI will be updated to support ViewPager2 for image carousels, conditional visibility for listing-type-specific actions, and proper state management for loading, success, and error states.

## Architecture

### Component Overview

The implementation follows the existing MVVM (Model-View-ViewModel) architecture:

```
┌─────────────────────┐
│ ItemDetailActivity  │ (View Layer)
│  - UI State         │
│  - User Interactions│
└──────────┬──────────┘
           │ observes LiveData
           ▼
┌─────────────────────────┐
│ ListingDetailViewModel  │ (ViewModel Layer)
│  - listing: LiveData    │
│  - bidResult: LiveData  │
│  - favoriteResult       │
└──────────┬──────────────┘
           │ calls
           ▼
┌─────────────────────────────────────┐
│ Repositories (Data Layer)           │
│  - ListingsRepository.getListing()  │
│  - BidsRepository.placeBid()        │
│  - FavoritesRepository.toggle()     │
└─────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│ Backend (Supabase/API)              │
│  - listings table                   │
│  - listing_images table             │
│  - accounts table (sellers)         │
│  - bids table                       │
│  - favorites table                  │
└─────────────────────────────────────┘
```

### Data Flow

1. **Initialization**: Activity receives `listing_id` via Intent extra
2. **Loading**: ViewModel fetches listing data from repository
3. **Display**: Activity observes LiveData and updates UI
4. **Interactions**: User actions (favorite, bid, contact) trigger ViewModel methods
5. **Updates**: ViewModel updates LiveData, Activity reacts to changes

## Components and Interfaces

### 1. ItemDetailActivity (View Layer)

**Responsibilities:**
- Receive listing_id from Intent
- Initialize ViewModel and observe LiveData
- Display listing data in UI components
- Handle user interactions (favorite, bid, contact, add to cart)
- Manage UI state (loading, content, error)
- Conditionally show/hide UI elements based on listing type

**Key Methods:**
```kotlin
private fun setupViewModel()
private fun observeListingData()
private fun observeBidResult()
private fun observeFavoriteResult()
private fun displayListing(listing: Listing)
private fun setupImageCarousel(images: List<ListingImage>)
private fun setupActionButtons(listingType: String)
private fun showBidDialog()
private fun handleFavoriteClick()
private fun handleContactSeller()
private fun showLoading()
private fun showError(message: String)
private fun showContent()
```

**Intent Parameters:**
- `listing_id` (Int): The ID of the listing to display

### 2. ListingDetailViewModel (Already Exists)

**Existing Interface:**
```kotlin
val listing: LiveData<Resource<Listing>>
val bidResult: LiveData<Resource<BidData>?>
val favoriteResult: LiveData<Resource<FavoriteData>?>

fun loadListing(listingId: Int)
fun placeBid(listingId: Int, bidAmount: Double)
fun toggleFavorite(listingId: Int)
fun resetBidResult()
fun resetFavoriteResult()
```

**No changes needed** - the ViewModel already provides all required functionality.

### 3. Image Carousel Component

**Implementation:** ViewPager2 with custom adapter

**Adapter Interface:**
```kotlin
class ImageCarouselAdapter(
    private val images: List<ListingImage>,
    private val context: Context
) : RecyclerView.Adapter<ImageCarouselAdapter.ImageViewHolder>()
```

**Features:**
- Swipeable image gallery
- Position indicator (e.g., "1 / 5")
- Image loading with Glide/Coil
- Placeholder for failed images
- Authentication headers for image requests

### 4. Bid Dialog

**Implementation:** MaterialAlertDialog with custom view

**Components:**
- EditText for bid amount input
- TextView showing current highest bid
- TextView showing minimum required bid
- Positive button (Submit)
- Negative button (Cancel)

**Validation:**
- Bid amount must be numeric
- Bid amount must be greater than current highest bid
- Bid amount must be positive

### 5. Conditional UI Manager

**Responsibilities:**
- Show/hide buttons based on listing type
- Enable/disable buttons based on state (e.g., auction ended)

**Logic:**
```kotlin
private fun setupActionButtons(listingType: String) {
    when (listingType) {
        "FIXED" -> {
            binding.btnAddToCart.visibility = View.VISIBLE
            binding.btnBuyNow.visibility = View.VISIBLE
            binding.btnPlaceBid.visibility = View.GONE
        }
        "BID" -> {
            binding.btnAddToCart.visibility = View.GONE
            binding.btnBuyNow.visibility = View.GONE
            binding.btnPlaceBid.visibility = View.VISIBLE
            setupAuctionTimer()
        }
    }
}
```

## Data Models

### Listing (Already Exists)

```kotlin
data class Listing(
    val id: Int,
    val title: String,
    val description: String,
    val price: Double,
    val location: String,
    val category: String,
    val listingType: String,  // "FIXED" or "BID"
    val status: String,
    val image: String?,
    val images: List<ListingImage>?,
    val seller: Seller?,
    val createdAt: String,
    val isFavorited: Boolean,
    val highestBid: Bid?,
    val endTime: String?
)
```

### ListingImage (Already Exists)

```kotlin
data class ListingImage(
    val imagePath: String
)
```

### Seller (Already Exists)

```kotlin
data class Seller(
    val accountId: Int?,
    val username: String,
    val firstName: String,
    val lastName: String
)
```

### Bid (Already Exists)

```kotlin
data class Bid(
    val bidAmount: Double,
    val bidTime: String,
    val bidder: Bidder?
)
```

### Resource<T> (Already Exists)

```kotlin
sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null
) {
    class Success<T>(data: T?) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
    class Loading<T> : Resource<T>()
}
```

## UI Layout Updates

### Layout Structure Changes

The existing `item_detail.xml` needs the following modifications:

1. **Replace single ImageView with ViewPager2:**
```xml
<androidx.viewpager2.widget.ViewPager2
    android:id="@+id/imageCarousel"
    android:layout_width="0dp"
    android:layout_height="320dp" />

<TextView
    android:id="@+id/imagePosition"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="1 / 5"
    android:background="@drawable/rounded_background"
    android:padding="8dp" />
```

2. **Add loading and error states:**
```xml
<ProgressBar
    android:id="@+id/progressBar"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:visibility="gone" />

<LinearLayout
    android:id="@+id/errorLayout"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:visibility="gone">
    
    <TextView
        android:id="@+id/errorMessage"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content" />
    
    <Button
        android:id="@+id/btnRetry"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Retry" />
</LinearLayout>

<ScrollView
    android:id="@+id/contentLayout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:visibility="gone">
    <!-- Existing content -->
</ScrollView>
```

3. **Add bid-specific UI elements:**
```xml
<TextView
    android:id="@+id/currentBidLabel"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Current Bid:"
    android:visibility="gone" />

<TextView
    android:id="@+id/currentBidAmount"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:visibility="gone" />

<TextView
    android:id="@+id/auctionEndTime"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:visibility="gone" />

<Button
    android:id="@+id/btnPlaceBid"
    android:layout_width="0dp"
    android:layout_height="60dp"
    android:text="Place Bid"
    android:visibility="gone" />
```

4. **Update seller information display:**
```xml
<!-- Seller section already exists, just needs data binding -->
<TextView
    android:id="@+id/sellerName"
    android:layout_width="0dp"
    android:layout_height="wrap_content" />

<TextView
    android:id="@+id/sellerUsername"
    android:layout_width="0dp"
    android:layout_height="wrap_content" />
```

### Image Carousel Item Layout

Create new layout file `item_image_carousel.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="320dp"
    app:cardCornerRadius="24dp"
    app:cardElevation="4dp">

    <ImageView
        android:id="@+id/carouselImage"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:scaleType="centerCrop" />
    
    <ProgressBar
        android:id="@+id/imageLoadingProgress"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center" />
</androidx.cardview.widget.CardView>
```

### Bid Dialog Layout

Create new layout file `dialog_place_bid.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="24dp">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Place Your Bid"
        android:textSize="20sp"
        android:textStyle="bold" />

    <TextView
        android:id="@+id/currentBidInfo"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:text="Current highest bid: ₱0.00" />

    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:hint="Your bid amount">

        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/bidAmountInput"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="numberDecimal" />
    </com.google.android.material.textfield.TextInputLayout>

    <TextView
        android:id="@+id/bidValidationError"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:textColor="@android:color/holo_red_dark"
        android:visibility="gone" />
</LinearLayout>
```


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Listing fetch triggered on initialization

*For any* valid listing_id passed via Intent extra, when the ItemDetailActivity is created, the ViewModel's loadListing method should be called with that listing_id.

**Validates: Requirements 1.1, 1.5**

### Property 2: Loading state displays progress indicator

*For any* listing fetch operation, while the Resource state is Loading, the progress indicator should be visible and the content layout should be hidden.

**Validates: Requirements 1.2, 8.1**

### Property 3: Successful load displays all listing fields

*For any* successfully loaded listing, the UI should display the title, description, price, location, category, and creation date in their respective TextViews.

**Validates: Requirements 1.3**

### Property 4: Error state displays error message with retry option

*For any* failed listing fetch operation, the UI should hide the progress indicator, display an error message with the failure reason, and show a retry button.

**Validates: Requirements 1.4, 8.3, 8.4**

### Property 5: Successful load transitions to content state

*For any* successful listing fetch, the progress indicator should be hidden and the content layout should become visible.

**Validates: Requirements 8.2**

### Property 6: Retry button reloads listing data

*For any* retry button tap in error state, the ViewModel's loadListing method should be called again with the same listing_id.

**Validates: Requirements 8.5**

### Property 7: Network operations disable interactive elements

*For any* ongoing network operation (loading, bid placement, favorite toggle), all interactive elements (buttons, favorite icon) should be disabled until the operation completes.

**Validates: Requirements 8.6**

### Property 8: Image carousel displays all listing images

*For any* listing with N images (where N > 0), the ViewPager2 carousel should contain exactly N image views, each displaying the corresponding image from the listing's images list.

**Validates: Requirements 2.1**

### Property 9: Image position indicator shows correct state

*For any* carousel position P in a listing with N total images, the position indicator should display "P / N" where P ranges from 1 to N.

**Validates: Requirements 2.2**

### Property 10: Carousel navigation transitions to adjacent images

*For any* valid swipe gesture (left or right) on the carousel, the ViewPager2 should transition to the previous or next image respectively, unless already at the first or last image.

**Validates: Requirements 2.3**

### Property 11: Image requests include authentication headers

*For any* image load request from the carousel, the HTTP request should include the same authentication headers (Authorization token) used for API requests.

**Validates: Requirements 2.4**

### Property 12: Favorite icon reflects listing state

*For any* loaded listing, the favorite icon should display the filled heart icon if isFavorited is true, and the outline heart icon if isFavorited is false.

**Validates: Requirements 3.1**

### Property 13: Favorite icon tap triggers toggle

*For any* favorite icon tap event, the ViewModel's toggleFavorite method should be called with the current listing_id.

**Validates: Requirements 3.2**

### Property 14: Favorite toggle round-trip preserves state

*For any* listing, if the favorite is toggled twice (favorite → unfavorite → favorite or vice versa), the final state should match the initial state, and reloading the listing should reflect the current favorite status.

**Validates: Requirements 3.3, 3.5**

### Property 15: Failed favorite toggle shows error and reverts state

*For any* failed favorite toggle operation, the UI should display an error message and the favorite icon should remain in its previous state (not change).

**Validates: Requirements 3.4**

### Property 16: Seller username always displayed

*For any* listing with seller data, the seller's username should be displayed in the seller information section.

**Validates: Requirements 4.1**

### Property 17: Full name displayed when available

*For any* seller with non-empty firstName and lastName, the UI should display the full name (firstName + lastName) in addition to or instead of the username.

**Validates: Requirements 4.2**

### Property 18: BID listings display highest bid

*For any* listing with listingType equal to "BID", if a highestBid exists, the current bid amount should be displayed in the UI.

**Validates: Requirements 5.1**

### Property 19: BID listings show auction countdown

*For any* listing with listingType equal to "BID" and a non-null endTime, the UI should display a countdown timer showing the time remaining until the auction ends.

**Validates: Requirements 5.2**

### Property 20: Place Bid button opens dialog

*For any* Place Bid button tap on a BID listing, a bid entry dialog should be displayed with the current highest bid information.

**Validates: Requirements 5.3**

### Property 21: Invalid bids are rejected with validation error

*For any* bid submission where the bid amount is less than or equal to the current highest bid (or less than or equal to zero), the submission should be rejected and a validation error message should be displayed.

**Validates: Requirements 5.4, 5.5**

### Property 22: Valid bid triggers ViewModel method

*For any* bid submission where the bid amount is greater than the current highest bid and greater than zero, the ViewModel's placeBid method should be called with the listing_id and bid amount.

**Validates: Requirements 5.6**

### Property 23: Successful bid updates displayed amount

*For any* successful bid placement, the UI should display a success message and update the displayed highest bid to reflect the new bid amount.

**Validates: Requirements 5.7**

### Property 24: Failed bid displays error message

*For any* failed bid placement operation, the UI should display an error message containing the failure reason from the API response.

**Validates: Requirements 5.8**

### Property 25: Expired auctions disable bidding

*For any* BID listing where the current time is greater than or equal to the endTime, the Place Bid button should be disabled.

**Validates: Requirements 5.9**

### Property 26: Contact Seller button tap triggers communication

*For any* Contact Seller button tap, the application should initiate a communication method (Intent to messaging, email, or in-app chat).

**Validates: Requirements 6.2**

### Property 27: Contact Seller enabled only with seller data

*For any* listing, the Contact Seller button should be enabled if and only if the seller object is non-null and contains valid seller information.

**Validates: Requirements 6.3**

### Property 28: FIXED listings display purchase buttons

*For any* listing with listingType equal to "FIXED", the Add to Cart and Buy Now buttons should be visible, and the Place Bid button should be hidden.

**Validates: Requirements 7.1**

### Property 29: BID listings display bidding buttons

*For any* listing with listingType equal to "BID", the Place Bid button should be visible, and the Add to Cart and Buy Now buttons should be hidden.

**Validates: Requirements 7.2**

### Property 30: Add to Cart adds listing to cart

*For any* Add to Cart button tap on a FIXED listing, the listing should be added to the shopping cart (cart state should include the listing).

**Validates: Requirements 7.4**

### Property 31: Successful cart addition shows confirmation

*For any* successful Add to Cart operation, a confirmation message (Toast or Snackbar) should be displayed to the user.

**Validates: Requirements 7.5**

## Error Handling

### Error Categories

1. **Network Errors**
   - No internet connection
   - Timeout errors
   - Server unavailable (5xx errors)
   - **Handling**: Display user-friendly error message with retry option

2. **Data Errors**
   - Listing not found (404)
   - Invalid listing_id
   - Malformed API response
   - **Handling**: Display specific error message, offer to return to previous screen

3. **Authentication Errors**
   - Token expired
   - Unauthorized access (403)
   - **Handling**: Display error message, redirect to login screen

4. **Validation Errors**
   - Invalid bid amount
   - Bid lower than current highest
   - Auction ended
   - **Handling**: Display inline validation error, keep user on current screen

5. **Image Loading Errors**
   - Image URL invalid
   - Image file not found
   - Network error during image load
   - **Handling**: Display placeholder image, log error silently

### Error Recovery Strategies

1. **Retry Mechanism**
   - Provide retry button for network errors
   - Implement exponential backoff for automatic retries (optional)
   - Clear error state before retry attempt

2. **Graceful Degradation**
   - Display partial data if some fields are missing
   - Show placeholder for missing images
   - Disable features that require missing data

3. **User Feedback**
   - Use Toast for transient errors (e.g., "Added to cart")
   - Use Snackbar for errors with actions (e.g., "Failed to load. Retry?")
   - Use AlertDialog for critical errors requiring user decision

4. **State Preservation**
   - Maintain user input in bid dialog if submission fails
   - Preserve scroll position on error recovery
   - Keep favorite state consistent on toggle failure

### Error Message Guidelines

- **Be specific**: "Failed to load listing" instead of "Error occurred"
- **Be actionable**: Include what the user can do (retry, go back, contact support)
- **Be concise**: Keep messages under 100 characters when possible
- **Be friendly**: Use conversational tone, avoid technical jargon

## Testing Strategy

### Dual Testing Approach

This feature requires both unit testing and property-based testing to ensure comprehensive coverage:

- **Unit tests**: Verify specific examples, edge cases, and error conditions
- **Property tests**: Verify universal properties across all inputs

Both testing approaches are complementary and necessary. Unit tests catch concrete bugs in specific scenarios, while property tests verify general correctness across a wide range of inputs.

### Unit Testing

Unit tests should focus on:

1. **Specific Examples**
   - Loading a specific known listing_id
   - Displaying a listing with exactly 3 images
   - Toggling favorite from false to true
   - Placing a bid of $100 when current bid is $50

2. **Edge Cases**
   - Listing with zero images (should show placeholder)
   - Listing with missing seller information
   - Auction that ended exactly now
   - Bid amount equal to current highest bid (should be rejected)
   - Empty or null listing fields

3. **Error Conditions**
   - Network timeout during listing fetch
   - 404 error for non-existent listing
   - Failed favorite toggle due to authentication error
   - Invalid bid amount (negative, zero, non-numeric)

4. **Integration Points**
   - ViewModel correctly calls repository methods
   - LiveData updates trigger UI changes
   - Intent extras are correctly extracted
   - Image loading library receives correct URLs and headers

### Property-Based Testing

Property-based testing will be implemented using **Kotest Property Testing** library for Kotlin. Each property test must:

- Run a minimum of 100 iterations (due to randomization)
- Reference its corresponding design document property
- Use the tag format: **Feature: item-detail-enhancements, Property {number}: {property_text}**

**Property Test Configuration:**

```kotlin
// In build.gradle.kts
testImplementation("io.kotest:kotest-property:5.8.0")
testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
```

**Example Property Test Structure:**

```kotlin
class ItemDetailPropertyTests : StringSpec({
    "Property 1: Listing fetch triggered on initialization".config(invocations = 100) {
        checkAll(Arb.int(1..10000)) { listingId ->
            // Given: An Intent with listing_id
            val intent = Intent().apply {
                putExtra("listing_id", listingId)
            }
            
            // When: Activity is created
            val viewModel = mockk<ListingDetailViewModel>()
            // ... setup activity with mocked ViewModel
            
            // Then: loadListing should be called with that ID
            verify { viewModel.loadListing(listingId) }
        }
    }
    
    "Property 8: Image carousel displays all listing images".config(invocations = 100) {
        checkAll(Arb.list(Arb.string(), 1..10)) { imagePaths ->
            // Given: A listing with N images
            val images = imagePaths.map { ListingImage(it) }
            val listing = createTestListing(images = images)
            
            // When: Carousel is set up
            val adapter = ImageCarouselAdapter(images, context)
            
            // Then: Adapter should have exactly N items
            adapter.itemCount shouldBe images.size
        }
    }
})
```

**Property Test Coverage:**

Each of the 31 correctness properties should have a corresponding property-based test. Priority properties for property testing:

1. **High Priority** (Core functionality):
   - Property 1: Listing fetch triggered
   - Property 3: All fields displayed
   - Property 8: All images in carousel
   - Property 14: Favorite toggle round-trip
   - Property 21: Invalid bid rejection
   - Property 28: FIXED listing UI
   - Property 29: BID listing UI

2. **Medium Priority** (User interactions):
   - Property 10: Carousel navigation
   - Property 13: Favorite icon tap
   - Property 20: Bid dialog opens
   - Property 22: Valid bid triggers ViewModel
   - Property 30: Add to cart

3. **Lower Priority** (UI state):
   - Property 2: Loading indicator
   - Property 4: Error state
   - Property 12: Favorite icon state
   - Property 27: Contact button enabled state

### Test Data Generation

For property-based tests, use Kotest Arb (Arbitrary) generators:

```kotlin
// Custom generators for domain objects
fun Arb.Companion.listing(
    listingType: String? = null
): Arb<Listing> = arbitrary {
    Listing(
        id = Arb.int(1..10000).bind(),
        title = Arb.string(10..100).bind(),
        description = Arb.string(50..500).bind(),
        price = Arb.double(1.0..10000.0).bind(),
        location = Arb.string(10..50).bind(),
        category = Arb.of("Electronics", "Fashion", "Home", "Sports").bind(),
        listingType = listingType ?: Arb.of("FIXED", "BID").bind(),
        status = "active",
        image = Arb.string().orNull().bind(),
        images = Arb.list(Arb.listingImage(), 0..10).bind(),
        seller = Arb.seller().orNull().bind(),
        createdAt = Arb.instant().toString(),
        isFavorited = Arb.bool().bind(),
        highestBid = Arb.bid().orNull().bind(),
        endTime = Arb.instant().toString().orNull().bind()
    )
}

fun Arb.Companion.listingImage(): Arb<ListingImage> = arbitrary {
    ListingImage(imagePath = Arb.string(20..100).bind())
}

fun Arb.Companion.seller(): Arb<Seller> = arbitrary {
    Seller(
        accountId = Arb.int(1..10000).bind(),
        username = Arb.string(5..20).bind(),
        firstName = Arb.string(3..20).bind(),
        lastName = Arb.string(3..20).bind()
    )
}

fun Arb.Companion.bid(): Arb<Bid> = arbitrary {
    Bid(
        bidAmount = Arb.double(1.0..10000.0).bind(),
        bidTime = Arb.instant().toString(),
        bidder = Arb.bidder().orNull().bind()
    )
}
```

### Testing Tools and Frameworks

1. **JUnit 5**: Test runner
2. **Kotest Property Testing**: Property-based testing framework
3. **MockK**: Mocking framework for Kotlin
4. **Espresso**: UI testing for Android
5. **Robolectric**: Unit testing with Android framework dependencies
6. **Truth**: Assertion library for more readable assertions

### Test Organization

```
app/src/test/kotlin/com/example/mineteh/
├── view/
│   ├── ItemDetailActivityTest.kt          # Unit tests
│   └── ItemDetailPropertyTests.kt         # Property tests
├── viewmodel/
│   ├── ListingDetailViewModelTest.kt      # Unit tests
│   └── ListingDetailViewModelPropertyTests.kt
├── generators/
│   └── DomainGenerators.kt                # Arb generators
└── utils/
    └── TestHelpers.kt                     # Test utilities
```

### Continuous Integration

- Run all unit tests on every commit
- Run property tests (with reduced iterations: 20) on every PR
- Run full property test suite (100 iterations) nightly
- Fail build if any test fails
- Generate code coverage reports (target: >80% coverage)

