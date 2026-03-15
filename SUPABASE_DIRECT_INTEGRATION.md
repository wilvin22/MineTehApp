# Supabase Direct Integration - Bot Protection Bypass

## Problem
InfinityFree hosting was returning JavaScript anti-bot challenges instead of JSON API responses, causing the app to crash with `IllegalStateException: Expected BEGIN_OBJECT but was STRING`.

## Solution
Implemented direct Supabase connections for read operations, bypassing the PHP API entirely.

---

## Features Now Using Direct Supabase

### ✅ Implemented (Bypasses Bot Protection)

1. **Listings (Read)**
   - `getListings()` - Fetch all listings with filters (category, type, search)
   - `getListing(id)` - Fetch single listing with full details
   - Includes seller info, images, and bids
   - Direct Postgrest queries to Supabase

2. **Favorites**
   - `toggleFavorite()` - Add/remove favorites
   - `getFavorites()` - Fetch user's favorite listings
   - Direct database operations

### ❌ Still Using PHP API (Requires Server Logic)

1. **Place Bid** - Needs validation and business logic
2. **Create Listing** - Requires image upload to your server
3. **Authentication** - Uses PHP API (may need to switch to Supabase Auth)

---

## Technical Changes

### Files Modified

1. **ListingsRepository.kt**
   - Completely rewritten to use Supabase Postgrest
   - Added Supabase response models with `@Serializable`
   - Implements complex joins for seller, images, and bids
   - Supports filtering, searching, and pagination

2. **FavoritesRepository.kt**
   - Now delegates to ListingsRepository for Supabase operations
   - Requires user ID from TokenManager

3. **ApiClient.kt**
   - Added cookie jar for potential PHP API calls
   - Added User-Agent header
   - Enabled redirect following

### New Data Models

```kotlin
@Serializable
data class SupabaseListingResponse(...)
@Serializable
data class SupabaseAccount(...)
@Serializable
data class SupabaseListingImage(...)
@Serializable
data class SupabaseBid(...)
@Serializable
data class SupabaseFavorite(...)
```

---

## Database Schema Used

The implementation uses these Supabase tables:
- `listings` - Main listings table
- `accounts` - User accounts (seller info)
- `listing_images` - Multiple images per listing
- `bids` - Bid history
- `favorites` - User favorites

### Key Joins
```sql
listings
  -> accounts (seller_id)
  -> listing_images (listing_id)
  -> bids (listing_id)
    -> accounts (user_id for bidder)
```

---

## How It Works

### Before (PHP API - Blocked by Bot Protection)
```
App → PHP API → Bot Challenge (HTML) → Crash
```

### After (Direct Supabase)
```
App → Supabase Postgrest → JSON Response → Success
```

---

## Testing

### What Should Work Now
1. ✅ Homepage loads listings
2. ✅ View listing details
3. ✅ Search and filter listings
4. ✅ Toggle favorites
5. ✅ View favorites page
6. ✅ Images display correctly (using ImageUtils)

### What Still Needs PHP API
1. ❌ Place bid (requires server validation)
2. ❌ Create listing (requires image upload)
3. ❌ Authentication (if using PHP auth)

---

## Next Steps

### Option 1: Fix PHP API Bot Protection (Recommended)
1. Log into InfinityFree control panel
2. Find "Security" or "Bot Protection" settings
3. Disable or whitelist your app's User-Agent
4. Or add your API endpoints to an allowlist

### Option 2: Move Remaining Features to Supabase
1. **Authentication**: Use Supabase Auth instead of PHP
2. **Place Bid**: Implement as Supabase RPC function
3. **Create Listing**: Use Supabase Storage for images

### Option 3: Hybrid Approach (Current)
- Keep using direct Supabase for reads
- Use PHP API only for complex writes
- Accept that some features may be limited

---

## Configuration

### Supabase Credentials (Already Configured)
```kotlin
supabaseUrl = "https://didpavzminvohszuuowu.supabase.co"
supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### Required User ID
The favorites feature requires `userId` from TokenManager. Make sure to save the user ID during login:
```kotlin
tokenManager.saveUserId(userId)
```

---

## Benefits

1. **No Bot Protection Issues** - Direct database access
2. **Faster Response Times** - No PHP processing overhead
3. **Real-time Capabilities** - Can add Supabase Realtime later
4. **Better Error Handling** - Clear error messages
5. **Type Safety** - Kotlinx Serialization

---

## Limitations

1. **No Server-Side Validation** - Business logic must be in app or RPC functions
2. **Image Upload** - Still needs PHP API or Supabase Storage
3. **Complex Operations** - Bid placement still uses PHP API

---

## Troubleshooting

### If listings still don't load:
1. Check Logcat for "ListingsRepository" logs
2. Verify Supabase credentials are correct
3. Check Supabase dashboard for RLS policies
4. Ensure tables exist and have data

### If favorites don't work:
1. Verify user ID is saved in TokenManager
2. Check Supabase RLS policies allow favorites operations
3. Look for "FavoritesRepository" errors in Logcat

---

## Summary

The app now uses a **hybrid architecture**:
- **Supabase Direct**: Listings, favorites (read operations)
- **PHP API**: Bids, create listing (write operations with validation)

This bypasses the InfinityFree bot protection for the most critical features while maintaining server-side logic where needed.
