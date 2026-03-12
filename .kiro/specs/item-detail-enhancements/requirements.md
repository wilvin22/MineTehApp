# Requirements Document

## Introduction

This document specifies requirements for enhancing the ItemDetailActivity in the MineTeh Android e-commerce application. The enhancements will replace hardcoded data with real Supabase data, add support for multiple images, implement working favorites, display seller information, add bidding functionality for auction listings, and provide conditional UI based on listing type (FIXED vs BID).

## Glossary

- **Item_Detail_Screen**: The Android Activity that displays detailed information about a single listing
- **Listing**: A product or item available for purchase or bidding in the e-commerce system
- **Listing_Detail_ViewModel**: The ViewModel component responsible for fetching and managing listing data from Supabase
- **FIXED_Listing**: A listing with a set price that can be purchased immediately
- **BID_Listing**: An auction-style listing where users place bids within a time window
- **Favorite**: A user action to save a listing for later viewing
- **Seller**: The user who created and owns the listing
- **Bid**: An offer to purchase a BID_Listing at a specified price
- **Image_Gallery**: A carousel component displaying multiple images for a listing
- **Supabase**: The backend database service storing listing and user data

## Requirements

### Requirement 1: Load Real Listing Data

**User Story:** As a user, I want to view accurate listing information from the database, so that I can make informed purchasing decisions.

#### Acceptance Criteria

1. WHEN the Item_Detail_Screen is opened with a listing_id, THE Item_Detail_Screen SHALL fetch the listing data using Listing_Detail_ViewModel
2. WHILE the listing data is loading, THE Item_Detail_Screen SHALL display a progress indicator
3. WHEN the listing data is successfully loaded, THE Item_Detail_Screen SHALL display the title, description, price, location, category, and creation date
4. IF the listing data fails to load, THEN THE Item_Detail_Screen SHALL display an error message with retry option
5. THE Item_Detail_Screen SHALL receive the listing_id via Intent extra parameter

### Requirement 2: Display Multiple Images

**User Story:** As a user, I want to view all available photos of a listing, so that I can better evaluate the item before purchasing.

#### Acceptance Criteria

1. WHEN a listing has multiple images, THE Image_Gallery SHALL display all images in a swipeable carousel
2. THE Image_Gallery SHALL display a position indicator showing current image number and total count
3. WHEN a user swipes left or right, THE Image_Gallery SHALL transition to the previous or next image
4. WHEN loading images from the remote server, THE Image_Gallery SHALL use the same authentication headers as the listings page
5. IF an image fails to load, THEN THE Image_Gallery SHALL display a placeholder image

### Requirement 3: Implement Working Favorites

**User Story:** As a user, I want to save listings as favorites, so that I can easily find them later.

#### Acceptance Criteria

1. WHEN the listing data is loaded, THE Item_Detail_Screen SHALL display the favorite icon in the correct state based on isFavorited property
2. WHEN a user taps the favorite icon, THE Item_Detail_Screen SHALL call the toggleFavorite method on Listing_Detail_ViewModel
3. WHEN the favorite toggle succeeds, THE Item_Detail_Screen SHALL update the icon visual state to reflect the new favorite status
4. IF the favorite toggle fails, THEN THE Item_Detail_Screen SHALL display an error message and revert the icon state
5. THE Item_Detail_Screen SHALL persist the favorite state to the Supabase database

### Requirement 4: Display Seller Information

**User Story:** As a user, I want to see who is selling the item, so that I can assess the seller's credibility.

#### Acceptance Criteria

1. WHEN the listing data is loaded, THE Item_Detail_Screen SHALL display the seller username
2. WHEN the seller has a first name and last name, THE Item_Detail_Screen SHALL display the full name
3. THE Item_Detail_Screen SHALL display seller information in a dedicated section of the layout
4. WHEN the seller profile data is incomplete, THE Item_Detail_Screen SHALL display only the available information

### Requirement 5: Bidding Interface for Auction Listings

**User Story:** As a user, I want to place bids on auction listings, so that I can compete to purchase items at favorable prices.

#### Acceptance Criteria

1. WHEN a listing has listingType equal to BID, THE Item_Detail_Screen SHALL display the current highest bid amount
2. WHEN a BID_Listing is displayed, THE Item_Detail_Screen SHALL show the auction end time with a countdown timer
3. WHEN a user taps the Place Bid button on a BID_Listing, THE Item_Detail_Screen SHALL open a bid entry dialog
4. WHEN a user submits a bid, THE Item_Detail_Screen SHALL validate that the bid amount is greater than the current highest bid
5. IF the bid amount is invalid, THEN THE Item_Detail_Screen SHALL display a validation error message
6. WHEN a valid bid is submitted, THE Item_Detail_Screen SHALL call the placeBid method on Listing_Detail_ViewModel
7. WHEN the bid placement succeeds, THE Item_Detail_Screen SHALL display a success message and update the displayed highest bid
8. IF the bid placement fails, THEN THE Item_Detail_Screen SHALL display an error message with the failure reason
9. WHEN the auction end time is reached, THE Item_Detail_Screen SHALL disable the Place Bid button

### Requirement 6: Contact Seller Functionality

**User Story:** As a user, I want to contact the seller directly, so that I can ask questions about the listing.

#### Acceptance Criteria

1. THE Item_Detail_Screen SHALL display a Contact Seller button
2. WHEN a user taps the Contact Seller button, THE Item_Detail_Screen SHALL provide a method to communicate with the seller
3. THE Item_Detail_Screen SHALL enable the Contact Seller button only when seller information is available

### Requirement 7: Conditional UI Based on Listing Type

**User Story:** As a user, I want to see appropriate action buttons based on whether an item is for direct purchase or auction, so that I can interact with listings correctly.

#### Acceptance Criteria

1. WHEN a listing has listingType equal to FIXED, THE Item_Detail_Screen SHALL display Add to Cart and Buy Now buttons
2. WHEN a listing has listingType equal to BID, THE Item_Detail_Screen SHALL display Place Bid and Contact Seller buttons
3. THE Item_Detail_Screen SHALL hide buttons that are not applicable to the current listing type
4. WHEN a user taps Add to Cart on a FIXED_Listing, THE Item_Detail_Screen SHALL add the listing to the shopping cart
5. WHEN the Add to Cart action succeeds, THE Item_Detail_Screen SHALL display a confirmation message

### Requirement 8: Loading and Error State Management

**User Story:** As a user, I want clear feedback when data is loading or when errors occur, so that I understand the application state.

#### Acceptance Criteria

1. WHEN the Item_Detail_Screen begins loading listing data, THE Item_Detail_Screen SHALL display a progress indicator
2. WHEN the listing data is successfully loaded, THE Item_Detail_Screen SHALL hide the progress indicator and display the content
3. IF the listing data fails to load, THEN THE Item_Detail_Screen SHALL hide the progress indicator and display an error message
4. WHEN an error message is displayed, THE Item_Detail_Screen SHALL provide a retry button
5. WHEN a user taps the retry button, THE Item_Detail_Screen SHALL attempt to reload the listing data
6. WHILE any network operation is in progress, THE Item_Detail_Screen SHALL disable interactive elements to prevent duplicate requests
