# Requirements Document

## Introduction

This document specifies the requirements for migrating the MineTeh Android application from a PHP backend hosted on InfinityFree to direct Supabase integration. The migration addresses reliability issues with the current PHP backend (returning HTML instead of JSON) and simplifies the architecture by using Supabase's client SDK for authentication and data operations.

## Glossary

- **MineTeh_App**: The Android application for managing marketplace listings, bids, and favorites
- **Supabase_Client**: The Supabase Kotlin SDK client library integrated into the Android app
- **Supabase_Auth**: Supabase's authentication service for user management
- **Supabase_Database**: Supabase's PostgreSQL database accessed via REST API
- **Auth_Token**: JWT token issued by Supabase Auth for authenticated requests
- **Repository**: Data layer component that handles data operations (AuthRepository, ListingsRepository, BidsRepository, FavoritesRepository)
- **Retrofit_Client**: The existing HTTP client library to be replaced
- **TokenManager**: Component responsible for storing and retrieving authentication tokens
- **Listing**: A marketplace item posted by users
- **Bid**: An offer made on a listing
- **Favorite**: A user-saved listing for later reference

## Requirements

### Requirement 1: Supabase Client Integration

**User Story:** As a developer, I want to integrate the Supabase Kotlin SDK, so that the app can communicate directly with Supabase services.

#### Acceptance Criteria

1. THE MineTeh_App SHALL include the Supabase Kotlin SDK as a dependency
2. THE MineTeh_App SHALL initialize the Supabase_Client with API URL https://didpavzminvohszuuowu.supabase.co
3. THE MineTeh_App SHALL configure the Supabase_Client with the provided anonymous key
4. THE Supabase_Client SHALL be available as a singleton throughout the application lifecycle

### Requirement 2: User Authentication

**User Story:** As a user, I want to register and login to my account, so that I can access personalized features.

#### Acceptance Criteria

1. WHEN a user provides valid email and password for registration, THE Supabase_Auth SHALL create a new user account
2. WHEN a user provides valid credentials for login, THE Supabase_Auth SHALL authenticate the user and return an Auth_Token
3. WHEN authentication succeeds, THE TokenManager SHALL store the Auth_Token securely
4. WHEN a user requests logout, THE Supabase_Auth SHALL invalidate the current session
5. WHEN logout completes, THE TokenManager SHALL clear the stored Auth_Token
6. IF registration fails due to duplicate email, THEN THE MineTeh_App SHALL return a descriptive error message
7. IF login fails due to invalid credentials, THEN THE MineTeh_App SHALL return a descriptive error message

### Requirement 3: Authenticated Request Handling

**User Story:** As a developer, I want all data requests to include authentication tokens, so that users can only access their authorized data.

#### Acceptance Criteria

1. WHEN the Supabase_Client makes a request to Supabase_Database, THE Supabase_Client SHALL include the Auth_Token in the request headers
2. WHEN the Auth_Token expires, THE Supabase_Client SHALL refresh the token automatically
3. IF token refresh fails, THEN THE MineTeh_App SHALL redirect the user to the login screen

### Requirement 4: Listings Management

**User Story:** As a user, I want to view, create, and update marketplace listings, so that I can participate in the marketplace.

#### Acceptance Criteria

1. WHEN a user requests all listings, THE Supabase_Database SHALL return all available Listing records
2. WHEN a user requests their own listings, THE Supabase_Database SHALL return only Listing records owned by that user
3. WHEN an authenticated user creates a new Listing, THE Supabase_Database SHALL store the Listing with the user's ID as owner
4. WHEN an authenticated user updates their own Listing, THE Supabase_Database SHALL modify the existing Listing record
5. IF a user attempts to update a Listing they do not own, THEN THE Supabase_Database SHALL reject the request with an authorization error
6. WHEN a Listing is retrieved, THE MineTeh_App SHALL parse the response into a Listing object

### Requirement 5: Bids Management

**User Story:** As a user, I want to place and view bids on listings, so that I can make offers on items.

#### Acceptance Criteria

1. WHEN an authenticated user places a Bid on a Listing, THE Supabase_Database SHALL store the Bid with the user's ID and Listing ID
2. WHEN a user requests bids for a specific Listing, THE Supabase_Database SHALL return all Bid records associated with that Listing
3. WHEN a user requests their own bids, THE Supabase_Database SHALL return only Bid records created by that user
4. IF a user attempts to place a Bid without authentication, THEN THE Supabase_Database SHALL reject the request with an authentication error

### Requirement 6: Favorites Management

**User Story:** As a user, I want to save and view my favorite listings, so that I can easily find items I'm interested in.

#### Acceptance Criteria

1. WHEN an authenticated user adds a Listing to favorites, THE Supabase_Database SHALL create a Favorite record linking the user and Listing
2. WHEN an authenticated user removes a Listing from favorites, THE Supabase_Database SHALL delete the corresponding Favorite record
3. WHEN a user requests their favorites, THE Supabase_Database SHALL return all Listing records marked as favorites by that user
4. IF a user attempts to add a duplicate favorite, THEN THE Supabase_Database SHALL return the existing Favorite record without error

### Requirement 7: User Profile Management

**User Story:** As a user, I want to view and update my profile information, so that I can manage my account details.

#### Acceptance Criteria

1. WHEN an authenticated user requests their profile, THE Supabase_Auth SHALL return the user's profile data
2. WHEN an authenticated user updates their profile, THE Supabase_Auth SHALL modify the user's profile data
3. THE MineTeh_App SHALL retrieve the current user's ID from the Auth_Token
4. IF profile update fails due to validation errors, THEN THE MineTeh_App SHALL return descriptive error messages

### Requirement 8: Repository Layer Migration

**User Story:** As a developer, I want to update all repository classes to use Supabase, so that the data layer uses the new backend.

#### Acceptance Criteria

1. THE AuthRepository SHALL replace Retrofit_Client calls with Supabase_Auth operations
2. THE ListingsRepository SHALL replace Retrofit_Client calls with Supabase_Database operations
3. THE BidsRepository SHALL replace Retrofit_Client calls with Supabase_Database operations
4. THE FavoritesRepository SHALL replace Retrofit_Client calls with Supabase_Database operations
5. THE Repository classes SHALL maintain their existing public interfaces to minimize ViewModel changes
6. WHEN a Repository operation fails, THE Repository SHALL return appropriate error types consistent with existing error handling

### Requirement 9: Retrofit Removal

**User Story:** As a developer, I want to remove Retrofit dependencies, so that the codebase only uses Supabase for backend communication.

#### Acceptance Criteria

1. THE MineTeh_App SHALL remove Retrofit library dependencies from the build configuration
2. THE MineTeh_App SHALL remove the ApiService interface
3. THE MineTeh_App SHALL remove Retrofit-specific annotations and converters
4. THE MineTeh_App SHALL remove any unused network configuration related to the PHP backend

### Requirement 10: Existing Functionality Preservation

**User Story:** As a user, I want all existing app features to continue working, so that my experience is not disrupted by the migration.

#### Acceptance Criteria

1. WHEN the migration is complete, THE MineTeh_App SHALL support all authentication flows that existed before migration
2. WHEN the migration is complete, THE MineTeh_App SHALL support all listings operations that existed before migration
3. WHEN the migration is complete, THE MineTeh_App SHALL support all bids operations that existed before migration
4. WHEN the migration is complete, THE MineTeh_App SHALL support all favorites operations that existed before migration
5. THE MineTeh_App SHALL maintain the existing UI and ViewModel structure
6. THE MineTeh_App SHALL maintain the existing user experience for all features
