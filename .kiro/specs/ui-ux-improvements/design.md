# Design Document: MineTeh UI/UX Improvements

## Overview

This design document outlines the technical architecture and implementation approach for transforming the MineTeh Android marketplace app into a modern, accessible, and user-friendly application. The design focuses on implementing Material Design 3 principles, enhancing navigation patterns, improving accessibility, and creating a comprehensive design system that can scale with the application's growth.

The current MineTeh app uses basic Android views with a purple-themed design. This redesign will modernize the entire user interface while maintaining the app's core functionality and brand identity. The implementation will be done incrementally, allowing for testing and refinement at each stage.

### Key Design Principles

1. **Material Design 3 Compliance**: Full adoption of Google's latest design system
2. **Accessibility First**: Ensuring the app is usable by all users, including those with disabilities
3. **Performance Optimization**: Smooth animations and efficient resource usage
4. **Scalable Architecture**: Component-based design that supports future feature additions
5. **Consistent User Experience**: Unified patterns across all screens and interactions

## Architecture

### High-Level Architecture

The UI/UX improvements will be implemented using a layered architecture approach:

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │   Activities    │  │   Fragments     │  │   Dialogs    │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                    UI Component Layer                       │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │ Custom Views    │  │  Composables    │  │   Adapters   │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                    Design System Layer                      │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │  Theme Manager  │  │ Animation Engine│  │ Style System │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                    Foundation Layer                         │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │   Resources     │  │   Utilities     │  │ Accessibility│ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Technology Stack

- **UI Framework**: Android Views with Material Design Components (MDC-Android)
- **Animation**: MotionLayout, Property Animations, and Lottie for complex animations
- **Navigation**: Android Navigation Component with Bottom Navigation
- **Theming**: Material Design 3 Dynamic Color and Theme System
- **Accessibility**: Android Accessibility Framework with TalkBack support
- **Image Loading**: Glide with custom transformations and caching
- **Layout**: ConstraintLayout with responsive design patterns

## Components and Interfaces

### Core UI Components

#### 1. Design System Components

**ThemeManager**
```kotlin
interface ThemeManager {
    fun applyTheme(theme: AppTheme)
    fun getCurrentTheme(): AppTheme
    fun isDarkMode(): Boolean
    fun supportsDynamicColor(): Boolean
}

enum class AppTheme {
    LIGHT, DARK, SYSTEM_DEFAULT, DYNAMIC
}
```

**ColorSystem**
- Primary colors: Material Design 3 color roles
- Semantic colors: Success, Warning, Error, Info
- Surface colors: Background, Surface, Surface Variant
- Dynamic color support for Android 12+

#### 2. Navigation Components

**BottomNavigationController**
```xml
<!-- Bottom Navigation with Material Design 3 styling -->
<com.google.android.material.bottomnavigation.BottomNavigationView
    android:id="@+id/bottom_navigation"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:menu="@menu/bottom_navigation_menu"
    app:labelVisibilityMode="labeled"
    app:itemIconTint="@color/bottom_nav_item_color"
    app:itemTextColor="@color/bottom_nav_item_color"
    app:backgroundTint="@color/surface" />
```

**NavigationHost**
- Fragment-based navigation using Navigation Component
- Shared element transitions between screens
- Deep linking support for all major screens

#### 3. Item Display Components

**ItemCard Layout Structure**
```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardCornerRadius="12dp"
    app:cardElevation="2dp"
    app:strokeWidth="0dp">
    
    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="12dp">
        
        <!-- Image with loading state -->
        <ImageView
            android:id="@+id/item_image"
            android:layout_width="0dp"
            android:layout_height="120dp"
            android:scaleType="centerCrop"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toEndOf="parent" />
            
        <!-- Content hierarchy -->
        <!-- Price, Title, Location, Time remaining -->
        
    </androidx.constraintlayout.widget.ConstraintLayout>
</com.google.android.material.card.MaterialCardView>
```

#### 4. Form Components

**Enhanced Text Input Fields**
```xml
<com.google.android.material.textfield.TextInputLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:boxStrokeColor="@color/primary"
    app:hintTextColor="@color/on_surface_variant"
    app:errorTextColor="@color/error"
    app:helperTextTextColor="@color/on_surface_variant"
    style="@style/Widget.Material3.TextInputLayout.OutlinedBox">
    
    <com.google.android.material.textfield.TextInputEditText
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:inputType="text" />
        
</com.google.android.material.textfield.TextInputLayout>
```

#### 5. Search Interface Components

**SearchView with Filters**
- Real-time search suggestions
- Filter chips for categories, price range, location
- Voice search integration
- Recent and popular searches

### Interface Definitions

#### Accessibility Interface
```kotlin
interface AccessibilityHelper {
    fun setContentDescription(view: View, description: String)
    fun announceForAccessibility(view: View, text: String)
    fun setAccessibilityHeading(view: View, isHeading: Boolean)
    fun configureFocusOrder(views: List<View>)
}
```

#### Animation Interface
```kotlin
interface AnimationController {
    fun fadeIn(view: View, duration: Long = 300)
    fun slideUp(view: View, duration: Long = 300)
    fun scaleAnimation(view: View, scale: Float, duration: Long = 200)
    fun sharedElementTransition(fromView: View, toView: View)
}
```

## Data Models

### UI State Models

#### Theme Configuration
```kotlin
data class ThemeConfig(
    val isDarkMode: Boolean,
    val useDynamicColor: Boolean,
    val primaryColor: Int,
    val accentColor: Int,
    val followSystemTheme: Boolean
)
```

#### Navigation State
```kotlin
data class NavigationState(
    val currentDestination: String,
    val backStackCount: Int,
    val bottomNavSelectedItem: Int,
    val badgeCounts: Map<String, Int>
)
```

#### Item Card Data
```kotlin
data class ItemCardData(
    val id: String,
    val title: String,
    val price: String,
    val imageUrl: String,
    val location: String,
    val timeRemaining: String?,
    val isAuction: Boolean,
    val isFavorite: Boolean,
    val sellerRating: Float?,
    val badges: List<ItemBadge>
)

enum class ItemBadge {
    VERIFIED_SELLER, NEW_ITEM, ENDING_SOON, FEATURED
}
```

#### Form Validation State
```kotlin
data class ValidationState(
    val isValid: Boolean,
    val errorMessage: String?,
    val fieldState: FieldState
)

enum class FieldState {
    IDLE, VALIDATING, VALID, ERROR
}
```

### Layout Models

#### Responsive Layout Configuration
```kotlin
data class LayoutConfig(
    val screenSize: ScreenSize,
    val orientation: Orientation,
    val gridColumns: Int,
    val useTabletLayout: Boolean,
    val supportsFoldable: Boolean
)

enum class ScreenSize {
    COMPACT, MEDIUM, EXPANDED
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Design System Compliance

*For any* UI component in the app, it should follow Material Design 3 principles including proper color usage, typography scale, 8dp grid spacing, appropriate elevation levels, consistent corner radii, and unified iconography.

**Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7**

### Property 2: Navigation State Consistency

*For any* navigation action, the system should maintain proper state including active tab highlighting, available back navigation, correct breadcrumbs for deep hierarchies, preserved state during configuration changes, accurate badge counts, and functional deep linking.

**Validates: Requirements 2.1, 2.3, 2.4, 2.5, 2.6, 2.7**

### Property 3: Search Interface Functionality

*For any* search interaction, the system should provide real-time suggestions, apply filters correctly, maintain search history, support voice input, sort results appropriately, show clear filter states, and persist user preferences.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7**

### Property 4: Item Card Information Display

*For any* item card, it should display all required information (images, price, title, location, time remaining), show appropriate badges and seller information when available, respond to interactions with visual feedback, and adapt to different layout modes.

**Validates: Requirements 4.1, 4.2, 4.4, 4.5, 4.6, 4.7**

### Property 5: Accessibility Compliance

*For any* interactive element, it should meet accessibility requirements including content descriptions, screen reader support, minimum touch target sizes (48dp), sufficient color contrast ratios, keyboard navigation support, alternative text for images, visible focus indicators, and text scaling support up to 200%.

**Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8**

### Property 6: Theme System Functionality

*For any* theme configuration, the system should properly implement light and dark themes following Material Design 3 guidelines, detect and respond to system theme changes, allow manual theme selection, ensure all elements work in both themes, support dynamic color theming, maintain proper contrast ratios, and persist user preferences.

**Validates: Requirements 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7**

### Property 7: Interaction Feedback and Animation

*For any* user interaction, the system should provide appropriate visual feedback within 100ms, show loading animations during network requests, implement micro-interactions for state changes, provide pull-to-refresh functionality, and respect system animation preferences for accessibility.

**Validates: Requirements 7.2, 7.3, 7.5, 7.6, 7.7**

### Property 8: Form Validation and Input Handling

*For any* form input, the system should provide real-time validation feedback, display clear error and success states, use Material Design text fields with proper labels, implement appropriate input types and keyboards, indicate required fields clearly, and show progress indicators for multi-step processes.

**Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7**

### Property 9: Profile Dashboard Information Architecture

*For any* profile dashboard element, it should display user statistics with proper hierarchy and icons, use card-based layout, provide quick access buttons to features, show achievements and badges when available, implement loading states for dynamic content, display recent activity and transactions, and provide easy access to settings.

**Validates: Requirements 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7**

### Property 10: Shopping Cart Management

*For any* cart interaction, the system should support swipe-to-delete functionality, provide quantity adjustment controls with feedback, display real-time price calculations, check item availability with status indicators, offer save-for-later functionality, show delivery information when available, and implement smooth checkout flow with progress indicators.

**Validates: Requirements 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7**

### Property 11: Bidding Interface Functionality

*For any* bidding interaction, the system should display accurate real-time auction timers with urgency indicators, provide quick bid buttons with correct increments, show current bid status and user position, display bid history with timestamps, implement confirmation dialogs for bids, provide outbid notifications, and show auction end notifications.

**Validates: Requirements 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7**

### Property 12: Onboarding Flow Management

*For any* onboarding interaction, the system should provide welcome screens explaining features, offer interactive tutorials for complex features, show contextual tooltips for first-time usage, allow skipping while maintaining help access, implement progressive feature disclosure, provide clear account setup CTAs, and remember completed steps to avoid repetition.

**Validates: Requirements 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7**

### Property 13: Performance and Loading State Management

*For any* loading operation, the system should implement skeleton screens for content-heavy pages, show progress indicators for operations longer than 1 second, use image lazy loading with placeholders, cache frequently accessed elements, implement smooth scrolling with proper recycling, provide offline indicators with graceful degradation, and support pull-to-refresh functionality.

**Validates: Requirements 13.1, 13.2, 13.3, 13.4, 13.5, 13.6, 13.7**

### Property 14: Responsive Layout Adaptation

*For any* screen configuration, the system should implement responsive layouts that adapt to different screen sizes, provide appropriate tablet layouts with multi-pane interfaces, support landscape orientation properly, use flexible grid systems, implement appropriate text scaling for different densities, provide proper spacing for different screen sizes, and support foldable devices with layout adjustments.

**Validates: Requirements 14.1, 14.2, 14.3, 14.4, 14.5, 14.6, 14.7**

### Property 15: Error Handling and Empty States

*For any* error or empty state scenario, the system should implement informative empty state screens with illustrations, provide clear error messages with suggested actions, offer retry mechanisms for failed requests, show offline indicators with cached content, provide contextual help in error states, implement graceful degradation when features are unavailable, and log errors appropriately while maintaining privacy.

**Validates: Requirements 15.1, 15.2, 15.3, 15.4, 15.5, 15.6, 15.7**

## Error Handling

### Error Categories and Handling Strategies

#### 1. Network and Connectivity Errors

**Strategy**: Graceful degradation with offline support
- Display cached content when available
- Show clear offline indicators
- Provide retry mechanisms with exponential backoff
- Implement queue for actions that can be performed when connectivity returns

**Implementation**:
```kotlin
sealed class NetworkError {
    object NoConnection : NetworkError()
    object Timeout : NetworkError()
    data class ServerError(val code: Int, val message: String) : NetworkError()
    data class UnknownError(val throwable: Throwable) : NetworkError()
}

class ErrorHandler {
    fun handleNetworkError(error: NetworkError): ErrorState {
        return when (error) {
            is NetworkError.NoConnection -> ErrorState.Offline
            is NetworkError.Timeout -> ErrorState.Retry
            is NetworkError.ServerError -> ErrorState.ServerIssue(error.message)
            is NetworkError.UnknownError -> ErrorState.Unknown
        }
    }
}
```

#### 2. Input Validation Errors

**Strategy**: Real-time validation with helpful guidance
- Show validation errors as users type (with debouncing)
- Provide specific, actionable error messages
- Use visual indicators (color, icons) to show error states
- Prevent form submission until all errors are resolved

#### 3. UI State Errors

**Strategy**: Defensive programming with fallback states
- Implement null safety throughout the UI layer
- Provide default values for missing data
- Use sealed classes for UI states to handle all possible scenarios
- Implement proper loading and error states for all async operations

#### 4. Accessibility Errors

**Strategy**: Comprehensive accessibility testing and fallbacks
- Provide alternative interaction methods when primary methods fail
- Ensure all error messages are accessible to screen readers
- Implement proper focus management during error states
- Test with accessibility services enabled

### Error Recovery Mechanisms

#### Automatic Recovery
- Retry failed network requests with exponential backoff
- Refresh expired authentication tokens automatically
- Reload corrupted cached data from the server
- Restore UI state after configuration changes

#### User-Initiated Recovery
- Pull-to-refresh for manual data refresh
- Clear cache options in settings
- Manual retry buttons for failed operations
- Reset to default settings option

## Testing Strategy

### Dual Testing Approach

The UI/UX improvements will be validated using both unit testing and property-based testing to ensure comprehensive coverage and correctness.

#### Unit Testing Strategy

**Focus Areas**:
- Specific UI component behavior examples
- Edge cases and boundary conditions
- Integration points between UI components
- Error handling scenarios
- Accessibility compliance verification

**Testing Framework**: JUnit 5 with Espresso for UI testing

**Example Unit Tests**:
```kotlin
@Test
fun `item card displays all required information`() {
    val itemData = createTestItemData()
    val itemCard = ItemCardView(context)
    
    itemCard.bindData(itemData)
    
    assertThat(itemCard.findViewById<TextView>(R.id.title).text)
        .isEqualTo(itemData.title)
    assertThat(itemCard.findViewById<TextView>(R.id.price).text)
        .isEqualTo(itemData.price)
    // Additional assertions for all required fields
}

@Test
fun `search filters are applied correctly`() {
    val searchInterface = SearchInterface()
    val filters = SearchFilters(
        priceRange = PriceRange(100, 500),
        category = Category.ELECTRONICS
    )
    
    val results = searchInterface.applyFilters(testItems, filters)
    
    assertThat(results).allMatch { item ->
        item.price in 100..500 && item.category == Category.ELECTRONICS
    }
}
```

#### Property-Based Testing Strategy

**Framework**: Kotest Property Testing with custom generators

**Configuration**: Minimum 100 iterations per property test to ensure comprehensive input coverage

**Property Test Implementation**:

Each correctness property will be implemented as a property-based test with appropriate generators and the required tag format.

```kotlin
class DesignSystemPropertyTests : StringSpec({
    
    "Design System Compliance" {
        checkAll(100, uiComponentGenerator()) { component ->
            // Feature: ui-ux-improvements, Property 1: Design System Compliance
            component.shouldFollowMaterialDesign3Principles()
            component.colors.shouldMatchMaterialDesignSpecs()
            component.spacing.shouldFollowEightDpGrid()
            component.elevation.shouldMatchComponentLevel()
            component.cornerRadius.shouldBeConsistent()
            component.typography.shouldFollowTypeScale()
            component.icons.shouldBeConsistent()
        }
    }
    
    "Navigation State Consistency" {
        checkAll(100, navigationActionGenerator()) { action ->
            // Feature: ui-ux-improvements, Property 2: Navigation State Consistency
            val initialState = createNavigationState()
            val newState = navigationController.handleAction(initialState, action)
            
            newState.shouldMaintainProperState()
            newState.activeTab.shouldBeHighlighted()
            newState.backNavigation.shouldBeAvailable()
            newState.breadcrumbs.shouldBeAccurate()
            newState.badgeCounts.shouldBeCorrect()
        }
    }
    
    "Accessibility Compliance" {
        checkAll(100, interactiveElementGenerator()) { element ->
            // Feature: ui-ux-improvements, Property 5: Accessibility Compliance
            element.contentDescription.shouldNotBeNull()
            element.touchTargetSize.shouldBeAtLeast(48.dp)
            element.colorContrast.shouldMeetWCAGRequirements()
            element.shouldSupportKeyboardNavigation()
            element.focusIndicator.shouldBeVisible()
            element.shouldSupportTextScaling(200)
        }
    }
})
```

**Custom Generators**:
```kotlin
fun uiComponentGenerator() = arbitrary { rs ->
    UIComponent(
        colors = colorPaletteGenerator().bind(),
        typography = typographyGenerator().bind(),
        spacing = spacingGenerator().bind(),
        elevation = elevationGenerator().bind(),
        cornerRadius = cornerRadiusGenerator().bind(),
        icons = iconSetGenerator().bind()
    )
}

fun navigationActionGenerator() = arbitrary { rs ->
    NavigationAction.values().random(rs.random)
}

fun interactiveElementGenerator() = arbitrary { rs ->
    InteractiveElement(
        type = ElementType.values().random(rs.random),
        size = Arb.int(24..96).bind(),
        colors = colorPairGenerator().bind(),
        hasContentDescription = Arb.boolean().bind(),
        supportsFocus = Arb.boolean().bind()
    )
}
```

#### Integration Testing

**Focus**: End-to-end user flows and component interactions
- Complete user journeys (search → view item → add to cart → checkout)
- Theme switching across all screens
- Accessibility service integration
- Performance under various conditions

#### Visual Regression Testing

**Tools**: Screenshot testing with Paparazzi or similar
- Capture screenshots of all major UI components
- Test both light and dark themes
- Verify responsive layouts across different screen sizes
- Ensure consistent visual appearance after changes

### Testing Coverage Goals

- **Unit Tests**: 90% code coverage for UI components
- **Property Tests**: 100% coverage of correctness properties
- **Integration Tests**: All major user flows
- **Accessibility Tests**: 100% compliance with WCAG 2.1 AA
- **Performance Tests**: All animations under 16ms frame time
- **Visual Tests**: All UI components in both themes

### Continuous Testing Strategy

- Run unit tests on every commit
- Execute property tests on pull requests
- Perform integration tests on release candidates
- Conduct accessibility audits weekly
- Monitor performance metrics in production

## Implementation Guidance

### XML Layout Restructuring Strategy

#### Phase 1: Foundation Setup

**1. Create Design System Resources**

Create a comprehensive resource structure to support the new design system:

```xml
<!-- res/values/colors.xml - Material Design 3 Color System -->
<resources>
    <!-- Primary Colors -->
    <color name="md_theme_light_primary">#6750A4</color>
    <color name="md_theme_light_onPrimary">#FFFFFF</color>
    <color name="md_theme_light_primaryContainer">#EADDFF</color>
    <color name="md_theme_light_onPrimaryContainer">#21005D</color>
    
    <!-- Secondary Colors -->
    <color name="md_theme_light_secondary">#625B71</color>
    <color name="md_theme_light_onSecondary">#FFFFFF</color>
    <color name="md_theme_light_secondaryContainer">#E8DEF8</color>
    <color name="md_theme_light_onSecondaryContainer">#1D192B</color>
    
    <!-- Surface Colors -->
    <color name="md_theme_light_surface">#FFFBFE</color>
    <color name="md_theme_light_onSurface">#1C1B1F</color>
    <color name="md_theme_light_surfaceVariant">#E7E0EC</color>
    <color name="md_theme_light_onSurfaceVariant">#49454F</color>
    
    <!-- Dark theme equivalents -->
    <!-- ... -->
</resources>

<!-- res/values/dimens.xml - 8dp Grid System -->
<resources>
    <!-- Spacing Scale -->
    <dimen name="spacing_xs">4dp</dimen>
    <dimen name="spacing_sm">8dp</dimen>
    <dimen name="spacing_md">16dp</dimen>
    <dimen name="spacing_lg">24dp</dimen>
    <dimen name="spacing_xl">32dp</dimen>
    <dimen name="spacing_xxl">48dp</dimen>
    
    <!-- Corner Radius -->
    <dimen name="corner_radius_xs">4dp</dimen>
    <dimen name="corner_radius_sm">8dp</dimen>
    <dimen name="corner_radius_md">12dp</dimen>
    <dimen name="corner_radius_lg">16dp</dimen>
    
    <!-- Elevation -->
    <dimen name="elevation_card">2dp</dimen>
    <dimen name="elevation_fab">6dp</dimen>
    <dimen name="elevation_dialog">8dp</dimen>
    
    <!-- Touch Targets -->
    <dimen name="min_touch_target">48dp</dimen>
</resources>

<!-- res/values/styles.xml - Typography Scale -->
<resources>
    <style name="TextAppearance.MineTeh.DisplayLarge" parent="TextAppearance.Material3.DisplayLarge">
        <item name="fontFamily">@font/roboto_regular</item>
        <item name="android:textSize">57sp</item>
        <item name="android:lineHeight">64sp</item>
    </style>
    
    <style name="TextAppearance.MineTeh.HeadlineMedium" parent="TextAppearance.Material3.HeadlineMedium">
        <item name="fontFamily">@font/roboto_regular</item>
        <item name="android:textSize">28sp</item>
        <item name="android:lineHeight">36sp</item>
    </style>
    
    <style name="TextAppearance.MineTeh.BodyLarge" parent="TextAppearance.Material3.BodyLarge">
        <item name="fontFamily">@font/roboto_regular</item>
        <item name="android:textSize">16sp</item>
        <item name="android:lineHeight">24sp</item>
    </style>
</resources>
```

**2. Create Component Styles**

```xml
<!-- res/values/styles.xml - Component Styles -->
<resources>
    <!-- Item Card Style -->
    <style name="Widget.MineTeh.Card.Item" parent="Widget.Material3.CardView.Elevated">
        <item name="cardCornerRadius">@dimen/corner_radius_md</item>
        <item name="cardElevation">@dimen/elevation_card</item>
        <item name="android:layout_margin">@dimen/spacing_sm</item>
        <item name="contentPadding">@dimen/spacing_md</item>
    </style>
    
    <!-- Button Styles -->
    <style name="Widget.MineTeh.Button.Primary" parent="Widget.Material3.Button">
        <item name="backgroundTint">@color/md_theme_light_primary</item>
        <item name="android:textColor">@color/md_theme_light_onPrimary</item>
        <item name="cornerRadius">@dimen/corner_radius_lg</item>
        <item name="android:minHeight">@dimen/min_touch_target</item>
    </style>
    
    <!-- Text Input Styles -->
    <style name="Widget.MineTeh.TextInputLayout" parent="Widget.Material3.TextInputLayout.OutlinedBox">
        <item name="boxStrokeColor">@color/md_theme_light_primary</item>
        <item name="hintTextColor">@color/md_theme_light_onSurfaceVariant</item>
        <item name="android:layout_marginBottom">@dimen/spacing_md</item>
    </style>
</resources>
```

#### Phase 2: Layout Restructuring

**1. Main Activity Layout with Bottom Navigation**

```xml
<!-- res/layout/activity_main.xml -->
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res/android-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fitsSystemWindows="true">

    <!-- Main Content Container -->
    <androidx.fragment.app.FragmentContainerView
        android:id="@+id/nav_host_fragment"
        android:name="androidx.navigation.fragment.NavHostFragment"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_marginBottom="80dp"
        app:defaultNavHost="true"
        app:navGraph="@navigation/nav_graph" />

    <!-- Bottom Navigation -->
    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottom_navigation"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom"
        app:menu="@menu/bottom_navigation_menu"
        app:labelVisibilityMode="labeled"
        app:itemIconTint="@color/bottom_nav_item_color"
        app:itemTextColor="@color/bottom_nav_item_color"
        app:backgroundTint="@color/md_theme_light_surface"
        app:elevation="@dimen/elevation_card" />

</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

**2. Enhanced Item Card Layout**

```xml
<!-- res/layout/item_card_layout.xml -->
<com.google.android.material.card.MaterialCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res/android-auto"
    xmlns:tools="http://schemas.android.com/tools"
    style="@style/Widget.MineTeh.Card.Item"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:clickable="true"
    android:focusable="true"
    android:contentDescription="@string/item_card_description">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="@dimen/spacing_md">

        <!-- Item Image with Loading State -->
        <ImageView
            android:id="@+id/item_image"
            android:layout_width="0dp"
            android:layout_height="120dp"
            android:scaleType="centerCrop"
            android:background="@color/md_theme_light_surfaceVariant"
            android:contentDescription="@string/item_image_description"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toEndOf="parent"
            tools:src="@drawable/placeholder_image" />

        <!-- Favorite Button -->
        <com.google.android.material.button.MaterialButton
            android:id="@+id/favorite_button"
            style="@style/Widget.Material3.Button.IconButton"
            android:layout_width="@dimen/min_touch_target"
            android:layout_height="@dimen/min_touch_target"
            android:layout_margin="@dimen/spacing_sm"
            app:icon="@drawable/ic_favorite_border"
            app:iconTint="@color/md_theme_light_onSurface"
            app:backgroundTint="@color/md_theme_light_surface"
            app:layout_constraintTop_toTopOf="@id/item_image"
            app:layout_constraintEnd_toEndOf="@id/item_image"
            android:contentDescription="@string/add_to_favorites" />

        <!-- Price -->
        <TextView
            android:id="@+id/item_price"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/spacing_md"
            android:textAppearance="@style/TextAppearance.MineTeh.HeadlineMedium"
            android:textColor="@color/md_theme_light_primary"
            app:layout_constraintTop_toBottomOf="@id/item_image"
            app:layout_constraintStart_toStartOf="parent"
            tools:text="$299.99" />

        <!-- Title -->
        <TextView
            android:id="@+id/item_title"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/spacing_xs"
            android:textAppearance="@style/TextAppearance.MineTeh.BodyLarge"
            android:textColor="@color/md_theme_light_onSurface"
            android:maxLines="2"
            android:ellipsize="end"
            app:layout_constraintTop_toBottomOf="@id/item_price"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toEndOf="parent"
            tools:text="Sample Item Title That Might Be Long" />

        <!-- Location and Time Container -->
        <LinearLayout
            android:id="@+id/metadata_container"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/spacing_sm"
            android:orientation="horizontal"
            app:layout_constraintTop_toBottomOf="@id/item_title"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toEndOf="parent">

            <!-- Location -->
            <TextView
                android:id="@+id/item_location"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:textAppearance="@style/TextAppearance.Material3.BodySmall"
                android:textColor="@color/md_theme_light_onSurfaceVariant"
                android:drawableStart="@drawable/ic_location"
                android:drawablePadding="@dimen/spacing_xs"
                android:gravity="center_vertical"
                tools:text="New York, NY" />

            <!-- Time Remaining (for auctions) -->
            <TextView
                android:id="@+id/time_remaining"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textAppearance="@style/TextAppearance.Material3.BodySmall"
                android:textColor="@color/md_theme_light_error"
                android:drawableStart="@drawable/ic_timer"
                android:drawablePadding="@dimen/spacing_xs"
                android:gravity="center_vertical"
                android:visibility="gone"
                tools:text="2h 30m"
                tools:visibility="visible" />

        </LinearLayout>

        <!-- Badges Container -->
        <com.google.android.flexbox.FlexboxLayout
            android:id="@+id/badges_container"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/spacing_sm"
            app:flexWrap="wrap"
            app:alignItems="flex_start"
            app:layout_constraintTop_toBottomOf="@id/metadata_container"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toEndOf="parent" />

    </androidx.constraintlayout.widget.ConstraintLayout>

</com.google.android.material.card.MaterialCardView>
```

**3. Enhanced Search Interface Layout**

```xml
<!-- res/layout/fragment_search.xml -->
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res/android-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <!-- App Bar with Search -->
    <com.google.android.material.appbar.AppBarLayout
        android:id="@+id/app_bar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        app:elevation="0dp">

        <!-- Search Bar -->
        <com.google.android.material.search.SearchBar
            android:id="@+id/search_bar"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_margin="@dimen/spacing_md"
            android:hint="@string/search_hint"
            app:navigationIcon="@drawable/ic_search" />

    </com.google.android.material.appbar.AppBarLayout>

    <!-- Main Content -->
    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="@dimen/spacing_md">

            <!-- Filter Chips -->
            <com.google.android.material.chip.ChipGroup
                android:id="@+id/filter_chips"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="@dimen/spacing_md"
                app:chipSpacing="@dimen/spacing_sm"
                app:singleSelection="false">

                <com.google.android.material.chip.Chip
                    android:id="@+id/chip_category"
                    style="@style/Widget.Material3.Chip.Filter"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/category"
                    app:chipIcon="@drawable/ic_category" />

                <com.google.android.material.chip.Chip
                    android:id="@+id/chip_price"
                    style="@style/Widget.Material3.Chip.Filter"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/price_range"
                    app:chipIcon="@drawable/ic_price" />

                <com.google.android.material.chip.Chip
                    android:id="@+id/chip_location"
                    style="@style/Widget.Material3.Chip.Filter"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/location"
                    app:chipIcon="@drawable/ic_location" />

            </com.google.android.material.chip.ChipGroup>

            <!-- Results RecyclerView -->
            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/search_results"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:nestedScrollingEnabled="false"
                app:layoutManager="androidx.recyclerview.widget.GridLayoutManager"
                app:spanCount="2" />

        </LinearLayout>

    </androidx.core.widget.NestedScrollView>

    <!-- Search View (overlay) -->
    <com.google.android.material.search.SearchView
        android:id="@+id/search_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:hint="@string/search_hint"
        app:layout_anchor="@id/search_bar">

        <!-- Search suggestions and recent searches -->
        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/search_suggestions"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:padding="@dimen/spacing_md" />

    </com.google.android.material.search.SearchView>

</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

#### Phase 3: Accessibility Implementation

**1. Content Descriptions and Labels**

```xml
<!-- Accessibility attributes for interactive elements -->
<Button
    android:id="@+id/bid_button"
    android:layout_width="wrap_content"
    android:layout_height="@dimen/min_touch_target"
    android:text="@string/place_bid"
    android:contentDescription="@string/place_bid_description"
    android:accessibilityLiveRegion="polite" />

<!-- Semantic markup for headings -->
<TextView
    android:id="@+id/section_title"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="@string/featured_items"
    android:textAppearance="@style/TextAppearance.MineTeh.HeadlineMedium"
    android:accessibilityHeading="true" />
```

**2. Focus Management**

```xml
<!-- Focus order and navigation -->
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:focusable="true"
    android:nextFocusDown="@id/next_element">
    
    <!-- Content -->
    
</LinearLayout>
```

#### Phase 4: Animation and Transitions

**1. Shared Element Transitions**

```xml
<!-- res/transition/shared_element_transition.xml -->
<transitionSet xmlns:android="http://schemas.android.com/apk/res/android">
    <changeImageTransform />
    <changeBounds />
    <changeTransform />
</transitionSet>
```

**2. MotionLayout for Complex Animations**

```xml
<!-- res/layout/item_detail_motion.xml -->
<androidx.constraintlayout.motion.widget.MotionLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res/android-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:layoutDescription="@xml/item_detail_scene">

    <!-- Content that will be animated -->

</androidx.constraintlayout.motion.widget.MotionLayout>
```

### Migration Strategy

#### Step 1: Resource Migration (Week 1)
1. Create new color, dimension, and style resources
2. Update themes to use Material Design 3
3. Add new typography styles
4. Create component-specific styles

#### Step 2: Core Layout Updates (Week 2-3)
1. Update main activity with bottom navigation
2. Restructure item card layouts
3. Implement new search interface
4. Update form layouts with Material Design text fields

#### Step 3: Component Enhancement (Week 4-5)
1. Add accessibility attributes to all interactive elements
2. Implement loading states and error handling
3. Add animation and transition support
4. Create responsive layout variants

#### Step 4: Testing and Refinement (Week 6)
1. Conduct accessibility testing with TalkBack
2. Test responsive layouts on different screen sizes
3. Validate color contrast ratios
4. Performance testing and optimization

### Performance Considerations

#### Image Loading Optimization
- Use Glide with appropriate caching strategies
- Implement lazy loading for RecyclerViews
- Use appropriate image formats (WebP when possible)
- Implement placeholder and error states

#### Layout Performance
- Use ConstraintLayout for complex layouts
- Minimize view hierarchy depth
- Use ViewBinding instead of findViewById
- Implement proper RecyclerView recycling

#### Animation Performance
- Use hardware acceleration for animations
- Implement proper animation lifecycle management
- Respect system animation preferences
- Use appropriate easing curves for natural motion

This comprehensive design provides the technical foundation for transforming your MineTeh app into a modern, accessible, and user-friendly marketplace application. The phased approach allows for incremental implementation while maintaining app functionality throughout the process.