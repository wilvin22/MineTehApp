# Force Android Studio to Rebuild with New Code

The issue is that Android Studio is caching the old compiled code. Follow these steps **IN ORDER**:

## Step 1: Invalidate Caches
1. In Android Studio, go to **File → Invalidate Caches...**
2. Check ALL boxes:
   - ✅ Clear file system cache and Local History
   - ✅ Clear downloaded shared indexes
   - ✅ Clear VCS Log caches and indexes
   - ✅ Wipe IDE system caches
3. Click **Invalidate and Restart**
4. Wait for Android Studio to restart

## Step 2: Clean Project (After Restart)
1. Go to **Build → Clean Project**
2. Wait for it to complete

## Step 3: Delete Build Folders Manually
1. Close Android Studio completely
2. Navigate to your project folder: `C:\Users\EXOUSIA\AndroidStudioProjects\MineTehClone\MineTehApp`
3. Delete these folders if they exist:
   - `app/build`
   - `.gradle`
   - `.idea/caches`
   - `build`

## Step 4: Rebuild
1. Open Android Studio again
2. Go to **Build → Rebuild Project**
3. Wait for the full rebuild to complete

## Step 5: Uninstall Old App
1. On your device/emulator, **completely uninstall** the MineTeh app
2. Or use: **Run → Select Device → Uninstall App**

## Step 6: Fresh Install
1. Click **Run** (green play button)
2. This will install the newly compiled APK with the Supabase code

## Verification
After installation, check the Logcat for:
- ✅ Should see: "Fetching listings from Supabase"
- ✅ Should see: "Successfully fetched X listings from Supabase"
- ❌ Should NOT see: "JSON parsing error - API might be returning HTML"

If you still see the old error message after these steps, there's a deeper issue we need to investigate.
