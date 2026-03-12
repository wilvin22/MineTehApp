# Design Document: Modern Listings UI

## Overview

This design modernizes the listings UI in the MineTeh Android marketplace app by implementing Material Design 3 (MD3) principles. The current implementation uses basic CardView components with minimal styling. This upgrade will transform the listings display into a contemporary, polished interface with enhanced visual hierarchy, improved image handling, animated interactions, and full accessibility compliance.

The design maintains backward compatibility with the existing data model and API while enhancing the visual presentation layer. All existing functionality (favoriting, navigation, auction timers) will be preserved and enhanced with better visual feedback and animations.

### Key Design Goals

1. Implement Material Design 3 card system with proper elevation and surface colors
2. Create consistent, attractive image display with aspect ratio control and loading states
3. Establish clear visual hierarchy through enhanced typography
4. Add polished animations for user interactions
5. Ensure WCAG 2.1 Level AA accessibility compliance
6. Maintain performance with efficient image loading and caching

## Architecture

### Component Structure

The modernized listings UI follows a layered architecture:

```
┌─────────────────────────────────────────┐
│         RecyclerView (Homepage)         │
└─────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────┐
│        ListingsAdapter (Enhanced)       │
│  - Material Design 3 card inflation     │
│  - ViewHolder with animation support    │
│  - Efficient view binding               │
└─────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────┐
│      item_card.xml (Redesigned)         │
│  ┌───────────────────────────────────┐  │
│  │  MaterialCardView (MD3)           │  │
│  │  ┌─────────────────────────────┐  │  │
│  │  │  Image Container            │  │  │
│  │  │  - ShapeableImageView       │  │  │
│  │  │  - Shimmer loading effect   │  │  │
│  │  │  - Gradient overlay         │  │  │
│  │  │  - Favorite icon (animated) │  │  │
│  │  │  - Type badge               │  │  │
│  │  └─────────────────────────────┘  │  │
│  │  ┌─────────────────────────────┐  │  │
│  │  │  Content Section            │  │  │
│  │  │  - Price (styled)           │  │  │
│  │  │  - Title (bold)             │  │  │
│  │  │  - Location                 │  │  │
│  │  │  - Auction timer (BID only) │  │  │
│  │  └─────────────────────────────┘  │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────┐
│      Supporting Components              │
│  - ImageLoader (Glide with shimmer)     │
│  - AnimationHelper (scale, fade)        │
│  - ColorManager (MD3 tokens)            │
│  - AccessibilityHelper (descriptions)   │
└─────────────────────────────────────────┘
```

### Design Pattern: Enhanced ViewHolder Pattern

The adapter uses an enhanced ViewHolder pattern with:
- View binding for type-safe access
- Animation state management
- Accessibility content description generation
- Efficient image loading with lifecycle awareness

### Material Design 3 Integration

Material Design 3 will be integrated through:
1. **Material Components Library**: Using `com.google.android.material:material:1.10.0+`
2. **Color System**: MD3 dynamic color tokens (primary, secondary, tertiary, surface)
3. **Typography Scale**: MD3 type scale (displayLarge, headlineSmall, bodyMedium, etc.)
4. **Elevation System**: MD3 elevation levels (0dp, 1dp, 3dp, 6dp)
5. **Shape System**: MD3 shape scale (small: 8dp, medium: 12dp, large: 16dp)

## Components and Interfaces

### 1. Enhanced ListingsAdapter

**Purpose**: Manages the display of listing cards in a RecyclerView with Material Design 3 styling and animations.

**Key Responsibilities**:
- Inflate and bind listing data to card views
- Manage favorite icon animations
- Handle image loading states with shimmer effects
- Apply dynamic colors based on listing type
- Provide accessibility content descriptions

**Interface**:
```kotlin
class ListingsAdapter(
    private val onItemClick: (Listing) -> Unit,
    private val onFavoriteClick: (Listing) -> Unit
) : RecyclerView.Adapter<ListingsAdapter.ViewHolder>() {
    
    fun submitList(newListings: List<Listing>?)
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(listing: Listing)
        private fun animateFavoriteIcon()
        private fun setupAccessibility(listing: Listing)
        private fun loadImage(listing: Listing)
        private fun updateBadgeStyle(listingType: String)
        private fun calculateRemainingTime(endTime: String): String
    }
}
```

### 2. MaterialCardView Layout (item_card.xml)

**Purpose**: Defines the visual structure of each listing card using Material Design 3 components.

**Key Elements**:
- `MaterialCardView`: Root container with MD3 elevation and corner radius
- `ShapeableImageView`: Image display with rounded corners and aspect ratio
- `FrameLayout`: Image overlay container for badge and favorite icon
- `LinearLayout`: Content section with typography hierarchy
- `TextView` elements: Price, title, location, auction timer

**Styling Attributes**:
- `app:cardElevation`: 6dp (MD3 level 2)
- `app:cardCornerRadius`: 16dp (MD3 large shape)
- `app:strokeWidth`: 0dp (no stroke for elevated cards)
- `android:layout_margin`: 16dp (MD3 spacing scale)

### 3. Image Loading System

**Purpose**: Efficiently load and display listing images with proper aspect ratios and loading states.

**Components**:
- **Glide Library**: Primary image loading library
- **Shimmer Effect**: Facebook Shimmer library for loading animation
- **Placeholder Strategy**: Aspect-ratio-preserving placeholder

**Implementation Strategy**:
```kotlin
// Glide configuration with shimmer
Glide.with(context)
    .load(imageUrl)
    .placeholder(shimmerDrawable)
    .error(R.drawable.placeholder_error)
    .transform(CenterCrop(), RoundedCorners(cornerRadius))
    .transition(DrawableTransitionOptions.withCrossFade())
    .into(imageView)
```

**Shimmer Configuration**:
- Base color: Surface color with 10% opacity
- Highlight color: Surface color with 30% opacity
- Duration: 1500ms
- Direction: Left to right

### 4. Typography System

**Purpose**: Establish clear visual hierarchy using Material Design 3 type scale.

**Type Mappings**:
- **Price**: `titleLarge` (22sp, bold) - Most prominent
- **Title**: `bodyLarge` (16sp, medium weight) - Secondary prominence
- **Location**: `bodyMedium` (14sp, regular) - Tertiary information
- **Badge**: `labelSmall` (11sp, bold, uppercase) - Label style
- **Timer**: `bodySmall` (12sp, medium weight) - Supporting information

**Color Mappings**:
- Price (FIXED): `colorTertiary` (green accent)
- Price (BID): `colorPrimary` (blue/purple accent)
- Title: `onSurface` (high emphasis - 87% opacity)
- Location: `onSurfaceVariant` (medium emphasis - 60% opacity)
- Timer: `colorPrimary` (accent color)

### 5. Badge Component

**Purpose**: Visually distinguish listing types (FIXED vs BID) with clear, accessible badges.

**Design Specifications**:
- Shape: Rounded rectangle (8dp corner radius)
- Padding: 10dp horizontal, 4dp vertical
- Typography: 10sp, bold, uppercase, white text
- Background colors:
  - FIXED: `colorTertiary` (green - #4CAF50)
  - BID: `colorPrimary` (blue/purple - #6750A4)
- Position: Bottom-left of image with 12dp margin
- Elevation: 2dp to lift above image

### 6. Favorite Icon Component

**Purpose**: Provide animated, accessible favorite toggle functionality.

**Design Specifications**:
- Icon: Material Icons `favorite` (filled) and `favorite_border` (outline)
- Size: 24dp icon in 48dp touch target
- Position: Top-right of image with 8dp margin
- Background: Semi-transparent surface (60% opacity, 24dp circle)
- Colors:
  - Favorited: `colorError` (#F44336 - red)
  - Not favorited: `onSurface` (white with shadow)

**Animation Specifications**:
- Type: Scale animation with overshoot interpolator
- Duration: 300ms
- Scale: 1.0 → 1.3 → 1.0
- Trigger: On tap

### 7. Auction Timer Component

**Purpose**: Display remaining time for bid-type listings with clear visual emphasis.

**Design Specifications**:
- Visibility: Only shown for BID type listings
- Icon: Material Icons `schedule` (12dp)
- Typography: 12sp, bold, `colorPrimary`
- Format: "Xd Yh Zm" (days, hours, minutes)
- Position: Below location with 6dp top margin
- Alignment: Start-aligned with location text

## Data Models

### Listing Model (Existing - No Changes)

The existing `Listing` data class remains unchanged:

```kotlin
data class Listing(
    val id: Int,
    val title: String,
    val description: String,
    val price: Double,
    val location: String,
    val category: String,
    @SerializedName("listing_type") val listingType: String,
    val status: String,
    val image: String?,
    val images: List<ListingImage>?,
    val seller: Seller?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("is_favorited") val isFavorited: Boolean = false,
    @SerializedName("highest_bid") val highestBid: Bid? = null,
    @SerializedName("end_time") val endTime: String? = null
)
```

### View State Models (New)

To support animations and loading states, we introduce view state models:

```kotlin
// Image loading state
sealed class ImageLoadState {
    object Loading : ImageLoadState()
    object Success : ImageLoadState()
    data class Error(val message: String) : ImageLoadState()
}

// Animation state for favorite icon
data class FavoriteAnimationState(
    val isAnimating: Boolean = false,
    val isFavorited: Boolean = false
)
```

### Material Design 3 Theme Configuration

New theme attributes for MD3 color system:

```xml
<!-- colors.xml additions -->
<color name="md_theme_light_primary">#6750A4</color>
<color name="md_theme_light_onPrimary">#FFFFFF</color>
<color name="md_theme_light_primaryContainer">#EADDFF</color>
<color name="md_theme_light_secondary">#625B71</color>
<color name="md_theme_light_tertiary">#4CAF50</color>
<color name="md_theme_light_surface">#FFFBFE</color>
<color name="md_theme_light_onSurface">#1C1B1F</color>
<color name="md_theme_light_onSurfaceVariant">#49454F</color>
<color name="md_theme_light_error">#F44336</color>

<!-- Dark theme variants -->
<color name="md_theme_dark_primary">#D0BCFF</color>
<color name="md_theme_dark_surface">#1C1B1F</color>
<!-- ... additional dark theme colors -->
```


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Image Aspect Ratio Consistency

*For any* listing image displayed in the card, the rendered image dimensions SHALL maintain a consistent aspect ratio of either 16:9 or 4:3, regardless of the source image dimensions.

**Validates: Requirements 2.1**

### Property 2: Shimmer Effect During Image Loading

*For any* listing image in the loading state, a shimmer animation effect SHALL be visible to the user until the image loads successfully or fails.

**Validates: Requirements 2.4, 9.1**

### Property 3: Error Placeholder Aspect Ratio

*For any* listing image that fails to load, the displayed placeholder SHALL maintain the same aspect ratio (16:9 or 4:3) as successfully loaded images.

**Validates: Requirements 2.6**

### Property 4: Price Color Based on Listing Type

*For any* listing card, the price text color SHALL be green (tertiary color) when listing type is FIXED, and blue/purple (primary color) when listing type is BID.

**Validates: Requirements 3.3, 3.4**

### Property 5: Text Contrast Ratio Compliance

*For any* text element in the listing card, the contrast ratio between text color and background color SHALL be at least 4.5:1 for normal text (< 18sp) and at least 3:1 for large text (≥ 18sp), ensuring WCAG 2.1 Level AA compliance.

**Validates: Requirements 3.6, 7.2, 7.3, 10.4**

### Property 6: Badge Display Matches Listing Type

*For any* listing card, the badge component SHALL display the text matching the listing's type field (either "FIXED" or "BID").

**Validates: Requirements 4.1**

### Property 7: Badge Background Color Distinction

*For any* listing card, the badge background color SHALL be different for FIXED type listings (green/tertiary) versus BID type listings (blue/purple/primary), providing clear visual distinction.

**Validates: Requirements 4.4**

### Property 8: Favorite Icon State Consistency

*For any* listing card, the favorite icon SHALL display as a filled red heart when the listing's `isFavorited` field is true, and as an outline heart when `isFavorited` is false.

**Validates: Requirements 5.4**

### Property 9: Auction Timer Visibility for BID Listings

*For any* listing card where listing type is BID, the auction timer component SHALL be visible; for any listing where type is FIXED, the auction timer SHALL be hidden.

**Validates: Requirements 6.1, 6.6**

### Property 10: Auction Timer Format Compliance

*For any* BID listing with a valid end time, the displayed auction timer text SHALL match the format pattern "Xd Yh Zm" where X represents days, Y represents hours, and Z represents minutes remaining.

**Validates: Requirements 6.2**

### Property 11: Color Consistency Across Cards

*For any* set of listing cards displayed simultaneously, the color values used for equivalent elements (e.g., all FIXED prices, all BID badges) SHALL be identical, ensuring visual consistency.

**Validates: Requirements 7.4**

### Property 12: Responsive Spacing Consistency

*For any* screen size or density configuration, the spacing between card elements (margins, padding) SHALL maintain the same dp values, ensuring consistent visual rhythm across devices.

**Validates: Requirements 8.5**

### Property 13: Image Loading Placeholder Aspect Ratio

*For any* listing image in the loading state, the shimmer placeholder SHALL maintain the target aspect ratio (16:9 or 4:3) before the actual image loads.

**Validates: Requirements 9.2**

### Property 14: Image Cache Efficiency

*For any* listing image that has been successfully loaded once, subsequent displays of the same image SHALL load from cache without triggering a new network request.

**Validates: Requirements 9.3**

### Property 15: Network Timeout Handling

*For any* listing image load operation, if the network request exceeds the timeout threshold (e.g., 10 seconds), the system SHALL gracefully handle the timeout by displaying the error placeholder without crashing.

**Validates: Requirements 9.6**

### Property 16: Interactive Element Content Descriptions

*For any* interactive element in the listing card (favorite icon, card itself), a non-empty content description SHALL be set for accessibility support.

**Validates: Requirements 10.1**

### Property 17: TalkBack Element Descriptions

*For any* listing card, all interactive and informational elements SHALL have appropriate content descriptions that provide meaningful context when accessed via TalkBack screen reader.

**Validates: Requirements 10.3**

### Property 18: Dynamic Text Size Support

*For any* text element in the listing card, when the system text size setting is changed, the text SHALL scale proportionally while maintaining readability and layout integrity.

**Validates: Requirements 10.5**

## Error Handling

### Image Loading Errors

**Error Scenarios**:
1. Network unavailable
2. Invalid image URL
3. Server returns 404/500 error
4. Image format unsupported
5. Timeout exceeded
6. Out of memory during decode

**Handling Strategy**:
- Display aspect-ratio-preserving placeholder image
- Log error details for debugging (non-user-facing)
- Maintain card layout integrity (no broken layouts)
- Provide retry mechanism on user interaction (tap to retry)
- Cache error state to avoid repeated failed requests

**Implementation**:
```kotlin
Glide.with(context)
    .load(imageUrl)
    .error(R.drawable.placeholder_error)
    .listener(object : RequestListener<Drawable> {
        override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: Target<Drawable>,
            isFirstResource: Boolean
        ): Boolean {
            Log.e(TAG, "Image load failed: ${e?.message}", e)
            // Maintain aspect ratio even in error state
            return false // Let Glide handle error drawable
        }
        
        override fun onResourceReady(...): Boolean {
            return false
        }
    })
    .into(imageView)
```

### Animation Errors

**Error Scenarios**:
1. Animation interrupted by rapid user interaction
2. View detached during animation
3. Low-end device performance issues

**Handling Strategy**:
- Cancel in-progress animations before starting new ones
- Check view attachment state before animating
- Use hardware acceleration where available
- Provide graceful degradation (skip animation if performance is poor)

**Implementation**:
```kotlin
fun animateFavoriteIcon(view: View) {
    // Cancel any existing animation
    view.animate().cancel()
    
    // Check if view is attached
    if (!view.isAttachedToWindow) return
    
    view.animate()
        .scaleX(1.3f)
        .scaleY(1.3f)
        .setDuration(150)
        .withEndAction {
            if (view.isAttachedToWindow) {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(150)
                    .start()
            }
        }
        .start()
}
```

### Data Binding Errors

**Error Scenarios**:
1. Null or missing listing data
2. Invalid listing type value
3. Malformed end time string
4. Negative price values

**Handling Strategy**:
- Provide sensible defaults for missing data
- Validate listing type against known values (FIXED, BID)
- Use try-catch for date parsing with fallback display
- Format negative prices as "N/A" or "Contact Seller"

**Implementation**:
```kotlin
fun bind(listing: Listing) {
    // Safe price display
    itemPrice.text = if (listing.price >= 0) {
        "₱${String.format("%.2f", listing.price)}"
    } else {
        "Contact Seller"
    }
    
    // Safe listing type handling
    val type = when (listing.listingType.uppercase()) {
        "FIXED", "BID" -> listing.listingType.uppercase()
        else -> "FIXED" // Default to FIXED for unknown types
    }
    
    // Safe timer calculation with error handling
    val timerText = try {
        calculateRemainingTime(listing.endTime ?: "")
    } catch (e: Exception) {
        Log.e(TAG, "Timer calculation failed", e)
        "N/A"
    }
}
```

### Accessibility Errors

**Error Scenarios**:
1. Missing content descriptions
2. Insufficient contrast ratios
3. Touch targets too small
4. Text not scaling with system settings

**Handling Strategy**:
- Provide default content descriptions for all interactive elements
- Validate color combinations during development
- Ensure minimum 48dp touch targets with padding
- Use `sp` units for all text sizes (not `dp`)
- Test with TalkBack enabled

**Implementation**:
```kotlin
fun setupAccessibility(listing: Listing) {
    // Card content description
    itemView.contentDescription = buildString {
        append("${listing.title}, ")
        append("Price: ₱${listing.price}, ")
        append("Location: ${listing.location}, ")
        append("Type: ${listing.listingType}")
        if (listing.listingType == "BID" && listing.endTime != null) {
            append(", Time remaining: ${calculateRemainingTime(listing.endTime)}")
        }
    }
    
    // Favorite icon content description
    itemHeart.contentDescription = if (listing.isFavorited) {
        "Remove ${listing.title} from favorites"
    } else {
        "Add ${listing.title} to favorites"
    }
    
    // Ensure minimum touch target
    itemHeart.minimumWidth = dpToPx(48)
    itemHeart.minimumHeight = dpToPx(48)
}
```

## Testing Strategy

### Dual Testing Approach

This feature will employ both unit testing and property-based testing to ensure comprehensive coverage:

**Unit Tests**: Focus on specific examples, edge cases, and integration points
- Specific listing data examples (FIXED vs BID)
- Edge cases (null images, expired auctions, zero prices)
- Error conditions (network failures, invalid data)
- Animation trigger verification
- Accessibility attribute checks

**Property Tests**: Verify universal properties across all inputs
- Universal properties that hold for all listings
- Comprehensive input coverage through randomization
- Invariant verification across different data combinations
- Minimum 100 iterations per property test

### Property-Based Testing Configuration

**Framework**: Kotest Property Testing for Kotlin
```kotlin
dependencies {
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-property:5.8.0")
}
```

**Test Structure**:
Each property test will:
1. Reference its design document property number
2. Run minimum 100 iterations with randomized inputs
3. Use appropriate generators for listing data
4. Include descriptive failure messages

**Tag Format**:
```kotlin
@Test
@Tag("Feature: modern-listings-ui, Property 4: Price Color Based on Listing Type")
fun `price color matches listing type for all listings`() = runTest {
    checkAll(100, listingGenerator) { listing ->
        val adapter = ListingsAdapter({}, {})
        val viewHolder = createViewHolder(adapter)
        
        viewHolder.bind(listing)
        
        val expectedColor = if (listing.listingType == "FIXED") {
            R.color.md_theme_light_tertiary
        } else {
            R.color.md_theme_light_primary
        }
        
        val actualColor = viewHolder.itemPrice.currentTextColor
        actualColor shouldBe getColor(expectedColor)
    }
}
```

### Unit Testing Strategy

**Test Categories**:

1. **Layout Inflation Tests**
   - Verify all views are properly inflated
   - Check view IDs are accessible
   - Validate view hierarchy structure

2. **Data Binding Tests**
   - Test binding with valid listing data
   - Test binding with null/missing fields
   - Test binding with edge case values (zero price, empty title)

3. **Conditional Display Tests**
   - Verify auction timer visibility for BID vs FIXED
   - Verify badge text and colors for different types
   - Verify favorite icon states

4. **Image Loading Tests**
   - Test successful image load
   - Test image load failure with placeholder
   - Test loading state with shimmer
   - Test cache behavior (mock Glide)

5. **Animation Tests**
   - Verify animation is triggered on favorite tap
   - Test animation cancellation on rapid taps
   - Test animation with detached view

6. **Accessibility Tests**
   - Verify content descriptions are set
   - Test TalkBack navigation order
   - Verify touch target sizes
   - Test with different text size settings

7. **Time Calculation Tests**
   - Test timer format for various time ranges
   - Test expired auction display
   - Test invalid date string handling

**Example Unit Tests**:
```kotlin
class ListingsAdapterTest {
    
    @Test
    fun `auction timer visible for BID listings`() {
        val bidListing = createListing(listingType = "BID", endTime = "2024-12-31 23:59:59")
        val adapter = ListingsAdapter({}, {})
        val viewHolder = createViewHolder(adapter)
        
        viewHolder.bind(bidListing)
        
        viewHolder.auctionTimerLayout.visibility shouldBe View.VISIBLE
    }
    
    @Test
    fun `auction timer hidden for FIXED listings`() {
        val fixedListing = createListing(listingType = "FIXED")
        val adapter = ListingsAdapter({}, {})
        val viewHolder = createViewHolder(adapter)
        
        viewHolder.bind(fixedListing)
        
        viewHolder.auctionTimerLayout.visibility shouldBe View.GONE
    }
    
    @Test
    fun `favorite icon shows red heart when favorited`() {
        val favoritedListing = createListing(isFavorited = true)
        val adapter = ListingsAdapter({}, {})
        val viewHolder = createViewHolder(adapter)
        
        viewHolder.bind(favoritedListing)
        
        // Verify the drawable resource
        val drawable = viewHolder.itemHeart.drawable
        drawable shouldNotBe null
        // Additional verification that it's the red heart drawable
    }
    
    @Test
    fun `content description includes listing details`() {
        val listing = createListing(
            title = "Test Item",
            price = 100.0,
            location = "Manila",
            listingType = "FIXED"
        )
        val adapter = ListingsAdapter({}, {})
        val viewHolder = createViewHolder(adapter)
        
        viewHolder.bind(listing)
        
        val contentDesc = viewHolder.itemView.contentDescription.toString()
        contentDesc shouldContain "Test Item"
        contentDesc shouldContain "100"
        contentDesc shouldContain "Manila"
        contentDesc shouldContain "FIXED"
    }
    
    @Test
    fun `timer format shows days and hours for long durations`() {
        val endTime = "2024-12-31 23:59:59" // Assume this is 5 days away
        val adapter = ListingsAdapter({}, {})
        val viewHolder = createViewHolder(adapter)
        
        val result = viewHolder.calculateRemainingTime(endTime)
        
        result shouldMatch Regex("\\d+d \\d+h")
    }
}
```

### Integration Testing

**Test Scenarios**:
1. Full RecyclerView with multiple listing types
2. Scroll performance with image loading
3. Favorite toggle with adapter updates
4. Theme switching (light to dark)
5. Configuration changes (rotation)

### Visual Regression Testing

**Approach**: Screenshot testing with different configurations
- Light theme vs dark theme
- Different listing types (FIXED, BID)
- Different states (favorited, not favorited)
- Loading states
- Error states
- Different screen densities

**Tools**: Paparazzi or Shot for screenshot testing

### Accessibility Testing

**Manual Testing Checklist**:
- [ ] Enable TalkBack and navigate through listings
- [ ] Verify all interactive elements are announced
- [ ] Test with large text size (200%)
- [ ] Test with high contrast mode
- [ ] Verify touch targets are easily tappable
- [ ] Test with color blindness simulators

**Automated Accessibility Tests**:
```kotlin
@Test
fun `all interactive elements have content descriptions`() {
    val listing = createListing()
    val adapter = ListingsAdapter({}, {})
    val viewHolder = createViewHolder(adapter)
    
    viewHolder.bind(listing)
    
    // Check card
    viewHolder.itemView.contentDescription shouldNotBe null
    viewHolder.itemView.contentDescription.toString().shouldNotBeBlank()
    
    // Check favorite icon
    viewHolder.itemHeart.contentDescription shouldNotBe null
    viewHolder.itemHeart.contentDescription.toString().shouldNotBeBlank()
}

@Test
fun `touch targets meet minimum size requirements`() {
    val adapter = ListingsAdapter({}, {})
    val viewHolder = createViewHolder(adapter)
    
    // Measure favorite icon touch target
    val minSize = dpToPx(48)
    viewHolder.itemHeart.minimumWidth shouldBeGreaterThanOrEqual minSize
    viewHolder.itemHeart.minimumHeight shouldBeGreaterThanOrEqual minSize
}
```

### Performance Testing

**Metrics to Monitor**:
- RecyclerView scroll frame rate (target: 60fps)
- Image load time (target: < 500ms on 4G)
- Memory usage during scroll
- Cache hit rate for images

**Testing Approach**:
- Use Android Profiler to monitor performance
- Test with large datasets (100+ listings)
- Test on low-end devices (Android 8.0, 2GB RAM)
- Monitor for memory leaks with LeakCanary

### Test Data Generators

**Listing Generator for Property Tests**:
```kotlin
val listingGenerator = Arb.bind(
    Arb.int(1..10000), // id
    Arb.string(5..50), // title
    Arb.string(10..200), // description
    Arb.double(0.0..10000.0), // price
    Arb.string(5..30), // location
    Arb.of("Electronics", "Fashion", "Home", "Sports"), // category
    Arb.of("FIXED", "BID"), // listingType
    Arb.of("ACTIVE", "SOLD", "EXPIRED"), // status
    Arb.string().orNull(), // image
    Arb.list(listingImageGenerator, 0..5).orNull(), // images
    sellerGenerator.orNull(), // seller
    Arb.string(), // createdAt
    Arb.bool(), // isFavorited
    bidGenerator.orNull(), // highestBid
    Arb.string().orNull() // endTime
) { id, title, desc, price, location, category, type, status, 
    image, images, seller, created, fav, bid, endTime ->
    Listing(id, title, desc, price, location, category, type, 
            status, image, images, seller, created, fav, bid, endTime)
}
```

### Continuous Integration

**CI Pipeline Steps**:
1. Run unit tests
2. Run property-based tests (100 iterations each)
3. Run integration tests
4. Generate code coverage report (target: 80%+)
5. Run accessibility checks
6. Generate screenshot tests
7. Run lint checks for accessibility issues

**Quality Gates**:
- All tests must pass
- Code coverage ≥ 80%
- No accessibility lint errors
- No memory leaks detected
