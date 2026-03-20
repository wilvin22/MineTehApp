# Requirements Document

## Introduction

This feature brings the Android app to parity with the existing PHP/Supabase website. Seven gaps have been identified: listing type filtering on the home screen, editing existing listings, wiring up search filters, a public seller profile screen, a selling dashboard with stats, post-purchase seller ratings, and a profile avatar display.

## Glossary

- **App**: The Android application (Kotlin/Supabase).
- **Home_Screen**: The `HomeActivity` displaying the listings grid.
- **Listing**: A marketplace item stored in the `listings` table with `listing_type` of `FIXED` or `BID`.
- **Edit_Screen**: A new activity/fragment that allows a seller to modify an existing listing.
- **Search_Screen**: The `SearchActivity` with `FilterBottomSheetFragment`.
- **Filter**: A set of query parameters (price range, listing type, category) applied to a listings fetch.
- **Seller_Profile**: A read-only public screen showing another user's account info and activity.
- **Selling_Dashboard**: The section of the profile screen showing the authenticated seller's aggregate stats.
- **Review**: A record in the `reviews` table containing a star rating (1–5) and optional comment.
- **Order**: A record in the `orders` table representing a completed purchase.
- **Avatar**: A colored circle displaying the user's initials, or a photo if one has been uploaded.
- **TokenManager**: The app utility that stores the authenticated user's ID and username locally.

---

## Requirements

### Requirement 1: Listing Type Filter on Home Screen

**User Story:** As a buyer, I want to filter home screen listings by All, Auctions, or Buy Now, so that I can quickly find the type of listing I am interested in.

#### Acceptance Criteria

1. THE Home_Screen SHALL display a tab strip or toggle with three options: "All", "Auctions" (BID), and "Buy Now" (FIXED).
2. WHEN the user selects "All", THE Home_Screen SHALL fetch listings without a `listing_type` filter.
3. WHEN the user selects "Auctions", THE Home_Screen SHALL fetch listings where `listing_type = 'BID'`.
4. WHEN the user selects "Buy Now", THE Home_Screen SHALL fetch listings where `listing_type = 'FIXED'`.
5. WHEN a listing type tab is selected, THE Home_Screen SHALL highlight the selected tab and clear the highlight from the others.
6. WHEN the app launches, THE Home_Screen SHALL default to the "All" tab.

---

### Requirement 2: Edit Listing

**User Story:** As a seller, I want to edit my listing's details from the app, so that I can keep my listings accurate without using the website.

#### Acceptance Criteria

1. WHEN the owner views their listing's detail screen and taps "Edit Listing", THE App SHALL open the Edit_Screen pre-populated with the listing's current title, description, price, photos, and auction end time.
2. WHEN the seller submits valid changes on the Edit_Screen, THE App SHALL execute `UPDATE listings SET title=?, description=?, price=?, end_time=? WHERE id=? AND seller_id=?` via Supabase and return to the listing detail screen.
3. WHEN the seller modifies photos on the Edit_Screen, THE App SHALL upload new images to Supabase Storage and update the `listing_images` table accordingly.
4. IF the Supabase update fails, THEN THE Edit_Screen SHALL display an error message and retain the user's unsaved changes.
5. WHILE the update is in progress, THE Edit_Screen SHALL disable the submit button and show a loading indicator.
6. THE Edit_Screen SHALL validate that title is non-empty, price is a positive number, and end time (for BID listings) is a future date before submitting.

---

### Requirement 3: Search Filters

**User Story:** As a buyer, I want to filter search results by price range, listing type, and category, so that I can narrow down results to what I want.

#### Acceptance Criteria

1. WHEN the user taps the filter button on the Search_Screen, THE App SHALL open the `FilterBottomSheetFragment`.
2. WHEN the user applies filters and taps "Apply", THE Search_Screen SHALL pass `minPrice`, `maxPrice`, `listingType`, and `category` as query parameters to the existing listings fetch in `ListingsRepository`.
3. WHEN a price range filter is active, THE Search_Screen SHALL only display listings where `price >= minPrice` and `price <= maxPrice`.
4. WHEN a listing type filter is active, THE Search_Screen SHALL only display listings matching the selected `listing_type`.
5. WHEN a category filter is active, THE Search_Screen SHALL only display listings matching the selected `category`.
6. WHEN the user taps "Clear Filters", THE Search_Screen SHALL reset all filter values and re-fetch unfiltered results.
7. IF no results match the applied filters, THEN THE Search_Screen SHALL display an empty state message.

---

### Requirement 4: Seller Public Profile

**User Story:** As a buyer, I want to view a seller's public profile, so that I can assess their reputation before purchasing.

#### Acceptance Criteria

1. WHEN the user taps a seller's name or avatar on a listing detail screen, THE App SHALL open the Seller_Profile screen for that seller.
2. THE Seller_Profile SHALL display the seller's username, avatar (initials or photo), and average star rating.
3. THE Seller_Profile SHALL display the count of the seller's active listings and the count of completed orders where the seller is the seller.
4. WHEN the Seller_Profile is loading, THE App SHALL show a loading indicator.
5. IF the seller account cannot be found, THEN THE Seller_Profile SHALL display an error message.
6. THE Seller_Profile SHALL query the `accounts`, `listings`, `reviews`, and `orders` tables via Supabase to populate its data.

---

### Requirement 5: Selling Dashboard Stats

**User Story:** As a seller, I want to see my selling stats in the app, so that I can track my performance without visiting the website.

#### Acceptance Criteria

1. THE Selling_Dashboard SHALL display the authenticated seller's active listing count, total sold count, unread message count, and average rating.
2. WHEN the user navigates to the profile screen, THE App SHALL fetch the Selling_Dashboard stats via aggregate queries on the `listings`, `orders`, `conversations`, and `reviews` tables.
3. WHILE the stats are loading, THE Selling_Dashboard SHALL show placeholder values or a loading indicator.
4. IF a stat query fails, THEN THE Selling_Dashboard SHALL display "—" for the affected stat and log the error.
5. THE Selling_Dashboard stats SHALL match the values shown on `selling.php` for the same account.

---

### Requirement 6: Rate Seller After Purchase

**User Story:** As a buyer, I want to rate a seller after my order is completed, so that other buyers can benefit from my experience.

#### Acceptance Criteria

1. WHEN an order's status is marked as completed or sold, THE App SHALL present the buyer with a prompt to rate the seller.
2. THE rating prompt SHALL allow the buyer to select a star rating from 1 to 5 and optionally enter a text comment.
3. WHEN the buyer submits a rating, THE App SHALL insert a record into the `reviews` table with `reviewer_id`, `seller_id`, `listing_id`, `rating`, and `comment`.
4. IF the buyer has already submitted a review for the same order, THEN THE App SHALL not show the rating prompt again for that order.
5. IF the review submission fails, THEN THE App SHALL display an error message and allow the buyer to retry.
6. WHILE the review is being submitted, THE App SHALL disable the submit button and show a loading indicator.

---

### Requirement 7: Profile Avatar

**User Story:** As a user, I want to see my avatar on the profile screen, so that my profile feels personalized and consistent with the website.

#### Acceptance Criteria

1. THE Profile_Screen SHALL display a colored circle containing the user's initials derived from their first and last name stored in the `accounts` table.
2. WHERE the user has uploaded a profile photo, THE Profile_Screen SHALL display the photo instead of the initials circle.
3. THE initials circle background color SHALL be deterministically derived from the user's account ID so that it remains consistent across sessions.
4. THE Seller_Profile screen SHALL also display the seller's avatar using the same initials/photo logic.
