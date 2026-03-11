# Requirements Document

## Introduction

This document specifies the requirements for integrating all XML layouts in the MineTehApp Android application with a local Room database. The application is a marketplace/e-commerce platform with features for browsing listings, bidding on auctions, managing a shopping cart, messaging, and user profiles. Currently, the app uses REST API calls for data retrieval and displays dummy data in many screens. This feature will implement a local Room database to cache API responses, enable offline functionality, and provide seamless data persistence across all screens.

## Glossary

- **Room_Database**: Android's SQLite object mapping library that provides an abstraction layer over SQLite
- **DAO** (Data Access Object): Interface that defines database operations
- **Entity**: Kotlin data class annotated with @Entity that represents a database table
- **Repository**: Component that mediates between data sources (API and local database)
- **Cache**: Local storage of API data for offline access and performance
- **Listing**: An item or auction available for purchase or bidding
- **Cart_Manager**: Component responsible for managing shopping cart operations
- **Favorite_Manager**: Component responsible for managing user favorites
- **Bid_Manager**: Component responsible for managing auction bids
- **Message_Manager**: Component responsible for managing chat messages
- **Sync_Manager**: Component that synchronizes local database with remote API
- **User_Session**: Local storage of authenticated user information
- **Offline_Mode**: Application state when network connectivity is unavailable

## Requirements

### Requirement 1: Database Schema and Entities

**User Story:** As a developer, I want to define database entities for all data models, so that the application can persist data locally.

#### Acceptance Criteria

1. THE Room_Database SHALL define an Entity for Listing with fields: id, title, description, price, location, category, listingType, status, imageUrl, sellerId, sellerUsername, createdAt, isFavorited, highestBid, endTime, and lastSyncedAt
2. THE Room_Database SHALL define an Entity for CartItem with fields: id, listingId, listingTitle, listingPrice, listingImageUrl, sellerName, quantity, addedAt, and isSelected
3. THE Room_Database SHALL define an Entity for Favorite with fields: id, listingId, userId, and favoritedAt
4. THE Room_Database SHALL define an Entity for Bid with fields: id, listingId, userId, bidAmount, bidTime, and status
5. THE Room_Database SHALL define an Entity for Message with fields: id, conversationId, senderId, receiverId, messageText, timestamp, and isRead
6. THE Room_Database SHALL define an Entity for Conversation with fields: id, participantId, participantUsername, lastMessage, lastMessageTime, and unreadCount
7. THE Room_Database SHALL define an Entity for User with fields: accountId, username, email, firstName, lastName, authToken, and tokenExpiresAt
8. THE Room_Database SHALL define an Entity for ListingImage with fields: id, listingId, imagePath, and displayOrder

### Requirement 2: Data Access Objects (DAOs)

**User Story:** As a developer, I want to define DAO interfaces for database operations, so that I can perform CRUD operations on entities.

#### Acceptance Criteria

1. THE Room_Database SHALL provide a ListingDao with methods: insertListing, insertListings, getListingById, getAllListings, getListingsByCategory, getListingsByType, searchListings, updateListing, deleteListing, and deleteAllListings
2. THE Room_Database SHALL provide a CartDao with methods: insertCartItem, getAllCartItems, getSelectedCartItems, updateCartItem, deleteCartItem, deleteAllCartItems, and getTotalCartValue
3. THE Room_Database SHALL provide a FavoriteDao with methods: insertFavorite, deleteFavorite, getAllFavorites, isFavorited, and getFavoriteListings
4. THE Room_Database SHALL provide a BidDao with methods: insertBid, getBidsByListing, getBidsByUser, getHighestBidForListing, and updateBidStatus
5. THE Room_Database SHALL provide a MessageDao with methods: insertMessage, getMessagesByConversation, markMessageAsRead, and deleteMessage
6. THE Room_Database SHALL provide a ConversationDao with methods: insertConversation, getAllConversations, updateConversation, and deleteConversation
7. THE Room_Database SHALL provide a UserDao with methods: insertUser, getCurrentUser, updateUser, and deleteUser
8. THE Room_Database SHALL provide a ListingImageDao with methods: insertImages, getImagesByListing, and deleteImagesByListing

### Requirement 3: Repository Pattern Implementation

**User Story:** As a developer, I want to implement repositories that coordinate between API and database, so that data flows consistently through the application.

#### Acceptance Criteria

1. THE Repository SHALL fetch data from the API and cache it in the Room_Database
2. WHEN network connectivity is unavailable, THE Repository SHALL return cached data from the Room_Database
3. WHEN API data is successfully fetched, THE Repository SHALL update the Room_Database with fresh data
4. THE Repository SHALL provide a single source of truth by exposing LiveData or Flow from the database
5. THE Repository SHALL implement cache expiration logic with a configurable time-to-live (TTL) of 5 minutes for listings
6. WHEN cached data is stale, THE Repository SHALL attempt to refresh from the API while returning cached data immediately

### Requirement 4: Homepage Listings Integration

**User Story:** As a user, I want to see marketplace listings on the homepage, so that I can browse available items.

#### Acceptance Criteria

1. WHEN the homepage loads, THE HomeActivity SHALL display listings from the Room_Database
2. WHEN listings are loaded, THE HomeActivity SHALL show a loading indicator while fetching data
3. IF the API fetch fails, THE HomeActivity SHALL display cached listings with a notification that data may be outdated
4. WHEN a user searches for items, THE HomeActivity SHALL query the Room_Database using the search term
5. WHEN a user filters by category, THE HomeActivity SHALL query the Room_Database for listings matching the selected category
6. THE HomeActivity SHALL update the RecyclerView when database data changes
7. WHEN a user pulls to refresh, THE HomeActivity SHALL force a sync with the API

### Requirement 5: Item Detail Screen Integration

**User Story:** As a user, I want to view detailed information about a listing, so that I can make informed purchase decisions.

#### Acceptance Criteria

1. WHEN the item detail screen loads, THE ItemDetailActivity SHALL retrieve the listing from the Room_Database by ID
2. THE ItemDetailActivity SHALL display all listing fields including title, description, price, location, seller information, and images
3. WHEN listing images exist, THE ItemDetailActivity SHALL load images from cached URLs
4. THE ItemDetailActivity SHALL display the current favorite status from the Room_Database
5. WHEN the listing is not found in the database, THE ItemDetailActivity SHALL fetch it from the API and cache it

### Requirement 6: Shopping Cart Functionality

**User Story:** As a user, I want to add items to my cart and manage them, so that I can purchase multiple items together.

#### Acceptance Criteria

1. WHEN a user clicks "Add to Cart", THE Cart_Manager SHALL insert a CartItem into the Room_Database
2. THE CartActivity SHALL display all cart items from the Room_Database
3. WHEN a user selects/deselects items, THE Cart_Manager SHALL update the isSelected field in the Room_Database
4. WHEN a user removes an item, THE Cart_Manager SHALL delete the CartItem from the Room_Database
5. THE CartActivity SHALL calculate and display the total price of selected items from the Room_Database
6. WHEN the cart is empty, THE CartActivity SHALL display an empty state message
7. THE CartActivity SHALL persist cart state across app restarts

### Requirement 7: Favorites Management

**User Story:** As a user, I want to save items as favorites, so that I can easily find them later.

#### Acceptance Criteria

1. WHEN a user clicks the heart icon, THE Favorite_Manager SHALL toggle the favorite status in the Room_Database
2. WHEN a listing is favorited, THE Favorite_Manager SHALL insert a Favorite entity and update the listing's isFavorited field
3. WHEN a listing is unfavorited, THE Favorite_Manager SHALL delete the Favorite entity and update the listing's isFavorited field
4. THE SavedItemsActivity SHALL display all favorited listings from the Room_Database
5. THE Favorite_Manager SHALL sync favorite status with the API when network is available
6. WHEN viewing a listing, THE ItemDetailActivity SHALL display the correct favorite status from the Room_Database

### Requirement 8: Auction and Bidding Integration

**User Story:** As a user, I want to place bids on auction items, so that I can participate in auctions.

#### Acceptance Criteria

1. THE BidActivity SHALL display only listings where listingType equals "BID" from the Room_Database
2. WHEN a user places a bid, THE Bid_Manager SHALL insert a Bid entity into the Room_Database
3. THE Bid_Manager SHALL validate that the bid amount exceeds the current highest bid before insertion
4. WHEN a bid is placed, THE Bid_Manager SHALL update the listing's highestBid field in the Room_Database
5. THE BidActivity SHALL display the current highest bid and time remaining from the Room_Database
6. THE YourAuctionsActivity SHALL display user's active bids, won auctions, and lost auctions from the Room_Database
7. WHEN an auction ends, THE Sync_Manager SHALL update the auction status in the Room_Database

### Requirement 9: Messaging and Inbox Integration

**User Story:** As a user, I want to send and receive messages with sellers, so that I can communicate about listings.

#### Acceptance Criteria

1. THE InboxActivity SHALL display all conversations from the Room_Database sorted by lastMessageTime
2. WHEN a conversation has unread messages, THE InboxActivity SHALL display the unread count from the Room_Database
3. WHEN a user opens a conversation, THE ChatActivity SHALL display all messages from the Room_Database
4. WHEN a user sends a message, THE Message_Manager SHALL insert the message into the Room_Database immediately
5. THE Message_Manager SHALL sync messages with the API in the background
6. WHEN a new message is received from the API, THE Message_Manager SHALL insert it into the Room_Database and update the conversation
7. THE ChatActivity SHALL mark messages as read when displayed

### Requirement 10: User Profile and Session Management

**User Story:** As a user, I want my login session to persist, so that I don't have to log in every time I open the app.

#### Acceptance Criteria

1. WHEN a user logs in successfully, THE User_Session SHALL store the User entity in the Room_Database
2. THE User_Session SHALL store the authentication token and expiration time in the Room_Database
3. WHEN the app starts, THE User_Session SHALL check for a valid user session in the Room_Database
4. IF the token is expired, THE User_Session SHALL clear the user data and redirect to the login screen
5. THE ProfileActivity SHALL display user information from the Room_Database
6. WHEN a user logs out, THE User_Session SHALL delete all user-specific data from the Room_Database

### Requirement 11: Sell/Create Listing Integration

**User Story:** As a user, I want to create new listings, so that I can sell items on the marketplace.

#### Acceptance Criteria

1. WHEN a user creates a listing, THE SellActivity SHALL validate all required fields before submission
2. WHEN the listing is created successfully via API, THE Repository SHALL insert the new listing into the Room_Database
3. THE MyListingsActivity SHALL display all listings created by the current user from the Room_Database
4. WHEN a listing creation fails due to network issues, THE SellActivity SHALL store the listing as a draft in the Room_Database
5. THE Sync_Manager SHALL retry uploading draft listings when network becomes available

### Requirement 12: Orders and Purchase History

**User Story:** As a user, I want to view my order history, so that I can track my purchases.

#### Acceptance Criteria

1. THE Room_Database SHALL define an Entity for Order with fields: id, userId, orderDate, totalAmount, status, and items
2. THE Room_Database SHALL provide an OrderDao with methods: insertOrder, getOrdersByUser, getOrderById, and updateOrderStatus
3. THE MyOrdersActivity SHALL display all orders from the Room_Database sorted by orderDate
4. WHEN a checkout is completed, THE Cart_Manager SHALL create an Order entity and clear the cart
5. THE Sync_Manager SHALL sync order status updates from the API

### Requirement 13: Data Synchronization

**User Story:** As a developer, I want automatic data synchronization, so that local data stays consistent with the server.

#### Acceptance Criteria

1. THE Sync_Manager SHALL implement a WorkManager periodic sync job that runs every 15 minutes
2. WHEN the app comes to foreground, THE Sync_Manager SHALL trigger an immediate sync
3. THE Sync_Manager SHALL sync listings, bids, messages, favorites, and orders in priority order
4. WHEN a sync conflict occurs, THE Sync_Manager SHALL prioritize server data over local data
5. THE Sync_Manager SHALL track the last sync timestamp for each entity type
6. WHEN network connectivity changes from offline to online, THE Sync_Manager SHALL trigger a sync

### Requirement 14: Offline Mode Support

**User Story:** As a user, I want to use the app offline, so that I can browse cached content without internet.

#### Acceptance Criteria

1. WHEN the device is offline, THE Application SHALL display a banner indicating Offline_Mode
2. WHILE in Offline_Mode, THE Application SHALL allow browsing cached listings, favorites, and cart
3. WHILE in Offline_Mode, THE Application SHALL disable actions that require network: placing bids, sending messages, creating listings, and checkout
4. WHEN a user attempts a network-required action in Offline_Mode, THE Application SHALL display an informative error message
5. WHEN network connectivity is restored, THE Application SHALL hide the offline banner and enable all features

### Requirement 15: Database Migration and Versioning

**User Story:** As a developer, I want database migrations, so that schema changes don't cause data loss.

#### Acceptance Criteria

1. THE Room_Database SHALL define a version number starting at 1
2. WHEN the database schema changes, THE Room_Database SHALL increment the version number
3. THE Room_Database SHALL provide migration strategies for each version upgrade
4. IF a migration is not defined, THE Room_Database SHALL use fallbackToDestructiveMigration for development builds only
5. THE Room_Database SHALL log all migration operations for debugging

### Requirement 16: Performance and Optimization

**User Story:** As a user, I want fast app performance, so that I have a smooth browsing experience.

#### Acceptance Criteria

1. THE Room_Database SHALL use indices on frequently queried fields: Listing.category, Listing.listingType, Listing.status, and Message.conversationId
2. THE Repository SHALL implement pagination for listings with a page size of 20 items
3. THE Repository SHALL load images lazily using Glide with disk caching enabled
4. THE Room_Database SHALL execute all database operations on background threads using coroutines
5. THE Application SHALL limit the cache size to 500 listings maximum, removing oldest entries when exceeded
6. THE Room_Database SHALL use transactions for batch operations to improve performance

### Requirement 17: Data Validation and Integrity

**User Story:** As a developer, I want data validation, so that the database maintains data integrity.

#### Acceptance Criteria

1. THE Room_Database SHALL enforce NOT NULL constraints on all required entity fields
2. THE Room_Database SHALL define foreign key relationships: CartItem.listingId references Listing.id, Favorite.listingId references Listing.id, Bid.listingId references Listing.id
3. WHEN a listing is deleted, THE Room_Database SHALL cascade delete related CartItems, Favorites, and Bids
4. THE Repository SHALL validate data types and ranges before inserting into the Room_Database
5. THE Room_Database SHALL use unique constraints on: User.accountId, Listing.id, and Favorite(userId, listingId)

### Requirement 18: Testing and Quality Assurance

**User Story:** As a developer, I want comprehensive tests, so that database operations are reliable.

#### Acceptance Criteria

1. THE Project SHALL include unit tests for all DAO methods using an in-memory Room_Database
2. THE Project SHALL include integration tests for Repository classes that verify API-to-database flow
3. THE Project SHALL include UI tests that verify data displays correctly from the Room_Database
4. THE Project SHALL achieve minimum 80% code coverage for database and repository classes
5. THE Project SHALL include tests for offline mode scenarios
6. THE Project SHALL include tests for data synchronization and conflict resolution
