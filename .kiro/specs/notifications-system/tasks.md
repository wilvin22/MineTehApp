# Implementation Plan: Notifications System

## Overview

This implementation plan creates a comprehensive notifications system that replaces the existing "Bid" navigation tab with a fully-featured notifications interface. The system follows the existing MVVM architecture patterns, integrates seamlessly with the current Supabase backend, and provides both in-app and push notification capabilities.

The implementation is structured to minimize risk by building incrementally, testing each component thoroughly, and ensuring backward compatibility with existing functionality.

## Tasks

- [x] 1. Database Schema Setup and Migration
  - Create notifications and notification_preferences tables in Supabase
  - Add proper indexes for performance optimization
  - Create database triggers for automatic notification generation
  - Test schema with sample data insertion and retrieval
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [ ] 2. Core Android Models and Data Classes
  - [x] 2.1 Create Notification data models and enums
    - Create Notification, NotificationType, and NotificationPreferences data classes
    - Add Kotlinx serialization annotations for Supabase integration
    - Create SupabaseNotificationResponse for API mapping
    - _Requirements: 2.4, 2.5_
  
  - [ ]* 2.2 Write property test for notification data models
    - **Property 2: Notification Type Support**
    - **Validates: Requirements 2.4**
  
  - [ ]* 2.3 Write property test for JSON data storage
    - **Property 3: JSON Data Storage**
    - **Validates: Requirements 2.5**

- [ ] 3. NotificationsRepository Implementation
  - [x] 3.1 Create NotificationsRepository with core CRUD operations
    - Implement getNotifications, markAsRead, markAllAsRead, getUnreadCount methods
    - Follow existing ListingsRepository patterns for Supabase integration
    - Add proper error handling and Resource wrapper usage
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.6_
  
  - [x] 3.2 Add notification filtering and pagination support
    - Implement filtering by type and read status
    - Add pagination parameters matching existing repository patterns
    - _Requirements: 7.2, 7.5_
  
  - [x] 3.3 Implement notification preferences management
    - Add getPreferences and updatePreferences methods
    - Create default preferences initialization
    - _Requirements: 6.1, 6.4, 6.5, 6.6_
  
  - [ ]* 3.4 Write property tests for repository CRUD operations
    - **Property 27: Repository CRUD Operations**
    - **Validates: Requirements 7.1**
  
  - [ ]* 3.5 Write property test for paginated fetching
    - **Property 28: Paginated Notification Fetching**
    - **Validates: Requirements 7.2**
  
  - [ ]* 3.6 Write unit tests for error handling scenarios
    - Test database connection failures and malformed data
    - Test network timeout and recovery scenarios
    - _Requirements: 7.6, 12.3, 12.4_

- [ ] 4. NotificationsViewModel Implementation
  - [x] 4.1 Create NotificationsViewModel with state management
    - Implement LiveData for notifications list and unread count
    - Add loading, success, and error state handling
    - Follow existing HomeViewModel patterns for consistency
    - _Requirements: 4.1, 4.2, 9.5_
  
  - [x] 4.2 Add notification interaction methods
    - Implement markAsRead, markAllAsRead, and refreshNotifications
    - Add proper coroutine scope management
    - _Requirements: 4.5, 7.3, 7.4_
  
  - [ ]* 4.3 Write property test for chronological ordering
    - **Property 11: Chronological Ordering**
    - **Validates: Requirements 4.1**
  
  - [ ]* 4.4 Write unit tests for ViewModel state management
    - Test loading states and error handling
    - Test LiveData updates and observer patterns
    - _Requirements: 4.1, 4.2_

- [x] 5. Checkpoint - Core Data Layer Complete
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 6. NotificationsActivity and UI Implementation
  - [x] 6.1 Update existing NotificationsActivity with complete functionality
    - Replace sample data with real repository integration
    - Add proper ViewModel binding and lifecycle management
    - Implement pull-to-refresh functionality
    - _Requirements: 4.1, 4.2, 4.7_
  
  - [x] 6.2 Create comprehensive NotificationsAdapter
    - Implement proper ViewHolder pattern with type-based icons
    - Add visual distinction for read/unread states
    - Handle notification tap actions and navigation
    - _Requirements: 4.2, 4.3, 4.4, 4.5_
  
  - [x] 6.3 Create notification item layout with proper styling
    - Design layout matching existing app UI patterns
    - Add notification type icons and timestamp formatting
    - Implement unread state visual indicators
    - _Requirements: 4.2, 4.3, 4.4_
  
  - [ ]* 6.4 Write property test for notification display
    - **Property 12: Notification Display Information**
    - **Validates: Requirements 4.2**
  
  - [ ]* 6.5 Write property test for read/unread visual distinction
    - **Property 13: Read/Unread Visual Distinction**
    - **Validates: Requirements 4.3**

- [ ] 7. Navigation Integration and Tab Replacement
  - [x] 7.1 Update bottom navigation layouts (homepage.xml, bid.xml, etc.)
    - Replace nav_bid with nav_notifications in all layout files
    - Add notification badge support with unread count display
    - Update navigation icons and labels
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 9.1, 9.2_
  
  - [x] 7.2 Update all Activity navigation handlers
    - Replace BidActivity navigation with NotificationsActivity in HomeActivity
    - Update navigation in SellActivity, InboxActivity, ProfileActivity
    - Ensure consistent navigation behavior across all screens
    - _Requirements: 1.2, 1.5_
  
  - [x] 7.3 Implement notification badge management
    - Create badge update logic in navigation controller
    - Add real-time badge count updates
    - Handle badge visibility based on unread count
    - _Requirements: 9.1, 9.2, 9.3, 9.4_
  
  - [ ]* 7.4 Write property test for navigation tab replacement
    - **Property 1: Navigation Tab Replacement**
    - **Validates: Requirements 1.2, 1.5**
  
  - [ ]* 7.5 Write property test for notification badge accuracy
    - **Property 38: Badge Count Accuracy**
    - **Validates: Requirements 9.2**

- [ ] 8. Real-time Updates Implementation
  - [x] 8.1 Create NotificationRealtimeManager for Supabase subscriptions
    - Implement real-time listener for notifications table
    - Add subscription lifecycle management
    - Handle connection errors and reconnection logic
    - _Requirements: 8.2, 8.4, 8.5_
  
  - [x] 8.2 Integrate real-time updates with ViewModel
    - Connect real-time listener to notifications LiveData
    - Update unread count in real-time
    - Handle subscription cleanup on Activity lifecycle
    - _Requirements: 8.2, 8.3, 8.4_
  
  - [ ]* 8.3 Write property test for real-time notification updates
    - **Property 33: Real-time Notification Updates**
    - **Validates: Requirements 8.2**
  
  - [ ]* 8.4 Write property test for real-time read state updates
    - **Property 34: Real-time Read State Updates**
    - **Validates: Requirements 8.3**

- [x] 9. Checkpoint - UI and Real-time Complete
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 10. Database Triggers and Notification Generation
  - [ ] 10.1 Create bid notification triggers
    - Implement create_bid_notification() function for BID_PLACED and BID_OUTBID
    - Add trigger on bids table for automatic notification creation
    - Test with sample bid placements
    - _Requirements: 3.1, 3.2_
  
  - [ ] 10.2 Create auction lifecycle triggers
    - Implement auction ending and completion notification functions
    - Add scheduled job for AUCTION_ENDING notifications (1 hour before)
    - Create AUCTION_WON and AUCTION_LOST notifications on completion
    - _Requirements: 3.3, 3.4_
  
  - [ ] 10.3 Create item sale and message triggers
    - Implement ITEM_SOLD notifications for fixed-price sales
    - Add NEW_MESSAGE notifications for chat messages
    - Include proper context data in all notifications
    - _Requirements: 3.5, 3.6, 3.7_
  
  - [ ]* 10.4 Write property tests for notification generation
    - **Property 4: Bid Notification Generation**
    - **Property 5: Outbid Notification Generation**
    - **Property 6: Auction Ending Notification Generation**
    - **Validates: Requirements 3.1, 3.2, 3.3**
  
  - [ ]* 10.5 Write property test for notification context data
    - **Property 10: Notification Context Data**
    - **Validates: Requirements 3.7**

- [ ] 11. Push Notification Integration
  - [x] 11.1 Set up Firebase Cloud Messaging configuration
    - Add FCM dependencies to build.gradle
    - Configure Firebase project and download google-services.json
    - Add necessary permissions to AndroidManifest.xml
    - _Requirements: 5.1_
  
  - [x] 11.2 Create NotificationService for FCM handling
    - Implement FirebaseMessagingService for push notification reception
    - Add notification channel creation for Android 8+
    - Handle foreground, background, and app-closed states
    - _Requirements: 5.2, 5.5_
  
  - [ ] 11.3 Implement push notification content and navigation
    - Create notifications with proper title, message, and deep links
    - Handle notification tap actions and app navigation
    - Add notification icons and styling
    - _Requirements: 5.3, 5.4_
  
  - [ ] 11.4 Add FCM token management
    - Implement token registration and server synchronization
    - Handle token refresh and update scenarios
    - _Requirements: 5.1_
  
  - [ ]* 11.5 Write property tests for push notification delivery
    - **Property 17: Push Notification Delivery**
    - **Property 18: Push Notification Content**
    - **Validates: Requirements 5.2, 5.3**
  
  - [ ]* 11.6 Write property test for push notification navigation
    - **Property 19: Push Notification Navigation**
    - **Validates: Requirements 5.4**

- [ ] 12. Notification Preferences UI and Management
  - [x] 12.1 Create NotificationPreferencesActivity
    - Design preferences screen with toggle switches for each notification type
    - Add global push notification enable/disable
    - Implement quiet hours time picker functionality
    - _Requirements: 6.1, 6.2, 6.3_
  
  - [x] 12.2 Integrate preferences with notification delivery
    - Check user preferences before creating notifications
    - Respect quiet hours for push notification delivery
    - Handle preference updates and immediate effect
    - _Requirements: 6.5, 5.6_
  
  - [ ]* 12.3 Write property tests for preference management
    - **Property 22: Notification Type Preferences**
    - **Property 23: Global Push Preference**
    - **Property 24: Quiet Hours Functionality**
    - **Validates: Requirements 6.1, 6.2, 6.3**

- [ ] 13. Notification Navigation and Deep Linking
  - [x] 13.1 Implement notification tap navigation logic
    - Create navigation handlers for each notification type
    - Pass relevant IDs and context data to destination screens
    - Handle invalid or non-existent target data gracefully
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7_
  
  - [x] 13.2 Add deep linking support for push notifications
    - Create deep link URL scheme for notification types
    - Handle deep link parsing and navigation
    - Test deep linking from various app states
    - _Requirements: 5.4, 10.6_
  
  - [ ]* 13.3 Write property tests for notification actions
    - **Property 15: Notification Tap Actions**
    - **Property 41: Navigation Context Data**
    - **Property 42: Invalid Navigation Handling**
    - **Validates: Requirements 4.5, 10.6, 10.7**

- [ ] 14. Checkpoint - Push Notifications and Navigation Complete
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 15. Performance Optimization and Cleanup
  - [x] 15.1 Implement automatic notification cleanup
    - Create cleanup job for notifications older than 90 days
    - Preserve 50 most recent notifications per user
    - Schedule cleanup during low-usage periods
    - _Requirements: 11.1, 11.2, 11.3_
  
  - [x] 15.2 Add notification caching and optimization
    - Implement local caching for frequently accessed notifications
    - Add database query optimization and proper indexing
    - Handle large notification lists with pagination
    - _Requirements: 7.2, 11.4_
  
  - [ ]* 15.3 Write property tests for cleanup functionality
    - **Property 43: Automatic Cleanup**
    - **Property 45: Recent Notification Preservation**
    - **Validates: Requirements 11.1, 11.3**

- [ ] 16. Error Handling and Resilience
  - [ ] 16.1 Implement comprehensive error handling
    - Add offline notification queuing for network failures
    - Implement retry logic with exponential backoff
    - Handle database failures gracefully without crashes
    - _Requirements: 12.1, 12.2, 12.3_
  
  - [ ] 16.2 Add fallback mechanisms for critical notifications
    - Implement alternative delivery paths for auction endings
    - Add error logging and monitoring
    - Create user-friendly error messages and recovery suggestions
    - _Requirements: 12.5, 12.6_
  
  - [ ]* 16.3 Write property tests for error resilience
    - **Property 48: Offline Notification Queuing**
    - **Property 49: Push Delivery Retry**
    - **Property 50: Database Failure Resilience**
    - **Validates: Requirements 12.1, 12.2, 12.3**

- [ ] 17. Integration Testing and Validation
  - [ ] 17.1 Create end-to-end notification flow tests
    - Test complete notification lifecycle from generation to delivery
    - Validate real-time updates and push notification integration
    - Test notification preferences and filtering
    - _Requirements: All requirements validation_
  
  - [ ] 17.2 Perform compatibility testing with existing features
    - Ensure no breaking changes to existing navigation
    - Test integration with listings, bids, and messaging
    - Validate performance impact on existing functionality
    - _Requirements: 1.5, integration requirements_
  
  - [ ]* 17.3 Write comprehensive integration property tests
    - **Property 26: Preference Enforcement**
    - **Property 36: Connection Error Recovery**
    - **Property 53: Error Logging**
    - **Validates: Requirements 6.5, 8.5, 12.6**

- [ ] 18. Final Checkpoint and Production Readiness
  - [ ] 18.1 Complete system testing and validation
    - Run all unit tests and property-based tests
    - Perform manual testing of all notification scenarios
    - Validate performance under load conditions
    - _Requirements: All requirements_
  
  - [ ] 18.2 Documentation and deployment preparation
    - Update API documentation for new endpoints
    - Create deployment checklist for database migrations
    - Prepare rollback procedures for production deployment
    - _Requirements: System reliability_

- [ ] 19. Final Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP delivery
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation and early error detection
- Property tests validate universal correctness properties across all notification scenarios
- Unit tests validate specific examples, edge cases, and integration points
- The implementation follows existing codebase patterns to ensure consistency
- Database triggers handle automatic notification generation without app-side complexity
- Real-time updates provide immediate user feedback for better user experience
- Push notifications ensure users stay informed even when the app is closed
- Comprehensive error handling ensures system reliability in production