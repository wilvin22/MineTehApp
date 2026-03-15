# Quick Fix Steps - Android Studio Cache Issue

## Do These Steps IN ORDER:

### 1. Invalidate Caches
**File → Invalidate Caches... → Check ALL boxes → Invalidate and Restart**

### 2. After Restart: Clean
**Build → Clean Project**

### 3. Close Android Studio & Delete Folders
Close Android Studio, then delete:
- `app\build`
- `.gradle`
- `.idea\caches`

### 4. Rebuild
Open Android Studio → **Build → Rebuild Project**

### 5. Uninstall Old App
On your device: Uninstall MineTeh app completely

### 6. Fresh Install
**Run → Run 'app'**

---

## Check Logcat After Launch

### ✅ SUCCESS - Look for:
```
D/MineTehApp: MineTeh App Starting - VERSION 2.0 (SUPABASE)
D/SupabaseClient: === INITIALIZING SUPABASE CLIENT (NEW CODE) ===
D/ListingsRepository: === SUPABASE REPOSITORY INITIALIZED (NEW CODE) ===
D/ListingsRepository: Fetching listings from Supabase
```

### ❌ FAILURE - If you see:
```
E/ListingsRepository: JSON parsing error - API might be returning HTML
```
**This means old code is still running. See CRITICAL_FIX_ANDROID_STUDIO_CACHE.md for nuclear options.**

---

## Expected Behavior After Fix

1. App launches to login screen
2. After login, homepage loads
3. Listings appear from Supabase
4. No "JSON parsing error" in Logcat
5. Images load correctly
6. Favorites work
7. Item details work

---

## If Listings Still Don't Show

After confirming new code is running (you see the VERSION 2.0 log), if listings still don't appear:

1. Check Logcat for Supabase errors
2. Verify internet connection
3. Check if Supabase database has data
4. Look for any error messages with tag "ListingsRepository"
