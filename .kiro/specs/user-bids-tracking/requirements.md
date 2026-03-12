# Requirements Document

## Introduction

The user-bids-tracking feature enables users to view and track all their auction bids organized by status. Users can see live auctions they're bidding on, auctions they've won, and auctions they've lost. The feature integrates with the existing YourAuctionsActivity UI and fetches real-time data from Supabase.

## Glossary

- **Bid_Tracker**: The system component responsible for fetching, organizing, and displaying user bid data
- **Live_Auction**: An auction listing where the end_time has not been reached and status is "active"
- **Won_Auction**: An auction listing where the end_time has passed, the user placed the highest bid, and status is "sold"
- **Lost_Auction**: An auction listing where the end_time has passed and the user did not place the highest bid
- **User_Bid**: A bid record created by the authenticated user containing bid_amount, bid_time, and listing_id
- **Highest_Bid**: The bid with the maximum bid_amount for a specific listing
- **Supabase_Client**: The database client used to query the bids and listings tables
- **Bid_Repository**: The data layer component that handles Supabase queries for bid data
- **Bid_ViewModel**: The presentation layer component that manages bid state and business logic

## Requirements

### Requirement 1: Fetch User Bids from Database

**User Story:** As a user who participates in auctions, I want the app to retrieve all my bids from the database, so that I can see my auction activity.

#### Acceptance Criteria

1. WHEN the YourAuctionsActivity is opened, THE Bid_Repository SHALL query the Supabase bids table for all records where user_id matches the authenticated user
2. THE Bid_Repository SHALL join the bids table with the listings table using listing_id to retrieve complete auction information
3. WHEN the database query succeeds, THE Bid_Repository SHALL return a list of User_Bid objects with associated listing details
4. IF the database query fails, THEN THE Bid_Repository SHALL return an error result with a descriptive error message
5. THE Bid_Repository SHALL include listing fields: id, title, description, image, price, end_time, status, and listing_type in the query results

### Requirement 2: Categorize Bids by Auction Status

**User Story:** As a user, I want my bids organized into Live, Won, and Lost categories, so that I can quickly understand the status of each auction.

#### Acceptance Criteria

1. WHEN bid data is received, THE Bid_ViewModel SHALL categorize each User_Bid based on the associated listing's end_time and status
2. THE Bid_ViewModel SHALL classify a User_Bid as Live_Auction when the listing end_time is in the future and status is "active"
3. THE Bid_ViewModel SHALL classify a User_Bid as Won_Auction when the listing end_time is in the past, the User_Bid bid_amount equals the Highest_Bid amount, and status is "sold"
4. THE Bid_ViewModel SHALL classify a User_Bid as Lost_Auction when the listing end_time is in the past and the User_Bid bid_amount is less than the Highest_Bid amount
5. THE Bid_ViewModel SHALL maintain three separate lists for Live_Auction, Won_Auction, and Lost_Auction categories

### Requirement 3: Display Live Auction Bids

**User Story:** As a user, I want to see my active auction bids with current status information, so that I know if I'm still winning.

#### Acceptance Criteria

1. WHEN the Live Auction tab is selected, THE Bid_Tracker SHALL display all Live_Auction bids in a RecyclerView
2. FOR EACH Live_Auction bid, THE Bid_Tracker SHALL display the listing image, title, and description
3. FOR EACH Live_Auction bid, THE Bid_Tracker SHALL display the user's bid_amount labeled as "Your Bid"
4. FOR EACH Live_Auction bid, THE Bid_Tracker SHALL display the current Highest_Bid amount labeled as "Current Highest Bid"
5. FOR EACH Live_Auction bid, THE Bid_Tracker SHALL display a countdown timer showing time remaining until end_time
6. WHEN the user's bid_amount equals the Highest_Bid amount, THE Bid_Tracker SHALL display a visual indicator showing "Winning"
7. WHEN the user's bid_amount is less than the Highest_Bid amount, THE Bid_Tracker SHALL display a visual indicator showing "Outbid"

### Requirement 4: Display Won Auction Bids

**User Story:** As a user, I want to see auctions I've won, so that I can review my successful bids and take next steps.

#### Acceptance Criteria

1. WHEN the Won tab is selected, THE Bid_Tracker SHALL display all Won_Auction bids in a RecyclerView
2. FOR EACH Won_Auction bid, THE Bid_Tracker SHALL display the listing image, title, and description
3. FOR EACH Won_Auction bid, THE Bid_Tracker SHALL display the winning bid_amount labeled as "Winning Bid"
4. FOR EACH Won_Auction bid, THE Bid_Tracker SHALL display the auction end_time formatted as a readable date and time
5. FOR EACH Won_Auction bid, THE Bid_Tracker SHALL display a visual indicator showing "Won"

### Requirement 5: Display Lost Auction Bids

**User Story:** As a user, I want to see auctions I've lost, so that I can review past bidding activity.

#### Acceptance Criteria

1. WHEN the Lost tab is selected, THE Bid_Tracker SHALL display all Lost_Auction bids in a RecyclerView
2. FOR EACH Lost_Auction bid, THE Bid_Tracker SHALL display the listing image, title, and description
3. FOR EACH Lost_Auction bid, THE Bid_Tracker SHALL display the user's bid_amount labeled as "Your Bid"
4. FOR EACH Lost_Auction bid, THE Bid_Tracker SHALL display the final Highest_Bid amount labeled as "Winning Bid"
5. FOR EACH Lost_Auction bid, THE Bid_Tracker SHALL display the auction end_time formatted as a readable date and time

### Requirement 6: Navigate to Listing Details

**User Story:** As a user, I want to tap on any bid to view the full listing details, so that I can see complete auction information.

#### Acceptance Criteria

1. WHEN a user taps on a bid item in any tab, THE Bid_Tracker SHALL launch the ItemDetailActivity
2. THE Bid_Tracker SHALL pass the listing_id to ItemDetailActivity via Intent extras
3. THE ItemDetailActivity SHALL display the complete listing information for the selected auction

### Requirement 7: Handle Real-Time Updates for Live Auctions

**User Story:** As a user viewing live auctions, I want to see updated bid information automatically, so that I have current auction status without manual refresh.

#### Acceptance Criteria

1. WHILE the Live Auction tab is visible, THE Bid_Tracker SHALL refresh bid data every 30 seconds
2. WHEN new bid data is received, THE Bid_Tracker SHALL update the displayed Highest_Bid amounts without scrolling the list
3. WHEN new bid data is received, THE Bid_Tracker SHALL update countdown timers to reflect current time remaining
4. WHEN a Live_Auction end_time is reached, THE Bid_Tracker SHALL remove the bid from the Live Auction list and add it to either Won_Auction or Lost_Auction based on the final bid status
5. WHEN the YourAuctionsActivity is paused or stopped, THE Bid_Tracker SHALL stop automatic refresh to conserve resources

### Requirement 8: Handle Empty States

**User Story:** As a user, I want to see helpful messages when I have no bids in a category, so that I understand the empty state is intentional.

#### Acceptance Criteria

1. WHEN the Live Auction tab has no Live_Auction bids, THE Bid_Tracker SHALL display a message "No active bids. Start bidding on auctions!"
2. WHEN the Won tab has no Won_Auction bids, THE Bid_Tracker SHALL display a message "No won auctions yet. Keep bidding!"
3. WHEN the Lost tab has no Lost_Auction bids, THE Bid_Tracker SHALL display a message "No lost auctions"
4. THE Bid_Tracker SHALL center empty state messages vertically and horizontally in the tab content area

### Requirement 9: Handle Loading and Error States

**User Story:** As a user, I want to see appropriate feedback when data is loading or if errors occur, so that I understand what's happening.

#### Acceptance Criteria

1. WHEN the YourAuctionsActivity is opened, THE Bid_Tracker SHALL display a loading indicator while fetching bid data
2. WHEN bid data is successfully loaded, THE Bid_Tracker SHALL hide the loading indicator and display the bid lists
3. IF the Bid_Repository returns an error, THEN THE Bid_Tracker SHALL display an error message with the error description
4. IF the Bid_Repository returns an error, THEN THE Bid_Tracker SHALL display a "Retry" button that re-attempts the data fetch when tapped
5. WHEN the user taps the Retry button, THE Bid_Tracker SHALL show the loading indicator and fetch bid data again

### Requirement 10: Format Currency and Time Values

**User Story:** As a user, I want bid amounts and times displayed in readable formats, so that I can easily understand the information.

#### Acceptance Criteria

1. THE Bid_Tracker SHALL format all bid_amount values as currency with two decimal places and the appropriate currency symbol
2. THE Bid_Tracker SHALL format countdown timers in the format "Xd Xh Xm" for days, hours, and minutes remaining
3. WHEN less than 1 hour remains, THE Bid_Tracker SHALL format countdown timers as "Xm Xs" for minutes and seconds
4. WHEN less than 1 minute remains, THE Bid_Tracker SHALL format countdown timers as "Xs" for seconds only
5. THE Bid_Tracker SHALL format end_time values as "MMM dd, yyyy HH:mm" for completed auctions
