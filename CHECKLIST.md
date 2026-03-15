# Android Studio Cache Fix - Checklist

Print this or keep it open while you work through the steps.

---

## Pre-Flight Check
- [ ] All files saved in Android Studio
- [ ] No builds currently running
- [ ] Device/emulator connected and visible

---

## Step 1: Invalidate Caches
- [ ] Open Android Studio
- [ ] Go to **File → Invalidate Caches...**
- [ ] Check box: **Clear file system cache and Local History**
- [ ] Check box: **Clear downloaded shared indexes**
- [ ] Check box: **Clear VCS Log caches and indexes**
- [ ] Check box: **Wipe IDE system caches**
- [ ] Click **"Invalidate and Restart"**
- [ ] Wait for Android Studio to fully restart (2-3 minutes)

---

## Step 2: Clean Project
- [ ] After restart, go to **Build → Clean Project**
- [ ] Wait for "BUILD SUCCESSFUL" in Build Output panel
- [ ] Verify completion (bottom status bar shows "Idle")

---

## Step 3: Manual Folder Deletion
- [ ] **Close Android Studio completely** (File → Exit)
- [ ] Open File Explorer
- [ ] Navigate to: `C:\Users\EXOUSIA\AndroidStudioProjects\MineTehClone\MineTehApp`
- [ ] Delete folder: `app\build` (if exists)
- [ ] Delete folder: `.gradle` (if exists)
- [ ] Delete folder: `.idea\caches` (if exists)
- [ ] Delete folder: `.kotlin\sessions` (if exists)
- [ ] If "folder in use" error: Open Task Manager → End all "java.exe" processes → Try again

---

## Step 4: Rebuild Project
- [ ] Open Android Studio
- [ ] Wait for project to fully load
- [ ] Go to **Build → Rebuild Project** (NOT just "Build Project")
- [ ] Wait for FULL rebuild (2-5 minutes)
- [ ] Verify "BUILD SUCCESSFUL" in Build Output
- [ ] Check for errors - there should be NONE

---

## Step 5: Uninstall Old App
- [ ] On your device: Long-press MineTeh app icon
- [ ] Select "Uninstall" or "App info" → "Uninstall"
- [ ] Confirm uninstallation
- [ ] Verify app is completely removed from device

---

## Step 6: Fresh Install
- [ ] In Android Studio: **Run → Run 'app'** (or click green play button)
- [ ] Select your device
- [ ] Wait for "Installing APK" message
- [ ] Wait for app to launch
- [ ] App should open to login screen

---

## Step 7: Verification (CRITICAL)
- [ ] Open **Logcat** panel in Android Studio
- [ ] Clear Logcat (trash icon)
- [ ] Filter by: **MineTehApp**
- [ ] Look for this log message:
  ```
  D/MineTehApp: MineTeh App Starting - VERSION 2.0 (SUPABASE)
  ```
- [ ] If you see it: **SUCCESS! New code is running** ✅
- [ ] If you DON'T see it: **FAILURE - Old code still running** ❌

---

## Step 8: Additional Verification
- [ ] Change Logcat filter to: **SupabaseClient**
- [ ] Look for:
  ```
  D/SupabaseClient: === INITIALIZING SUPABASE CLIENT (NEW CODE) ===
  ```
- [ ] Change Logcat filter to: **ListingsRepository**
- [ ] Look for:
  ```
  D/ListingsRepository: === SUPABASE REPOSITORY INITIALIZED (NEW CODE) ===
  ```

---

## Step 9: Test Login and Homepage
- [ ] Enter login credentials
- [ ] Click "Login"
- [ ] Wait for homepage to load
- [ ] Check Logcat for:
  ```
  D/ListingsRepository: Fetching listings from Supabase
  D/ListingsRepository: Successfully fetched X listings from Supabase
  ```
- [ ] Verify listings appear on homepage
- [ ] Verify NO error: "JSON parsing error - API might be returning HTML"

---

## Success Criteria

### ✅ All Good If:
- [x] You see "VERSION 2.0 (SUPABASE)" in logs
- [x] You see "SUPABASE REPOSITORY INITIALIZED (NEW CODE)" in logs
- [x] Listings load on homepage
- [x] No "JSON parsing error" in Logcat
- [x] Images display correctly

### ❌ Still Broken If:
- [ ] You see "JSON parsing error - API might be returning HTML"
- [ ] You DON'T see "VERSION 2.0 (SUPABASE)" in logs
- [ ] No listings appear on homepage
- [ ] Old error messages in Logcat

---

## If Still Broken After All Steps

See: **CRITICAL_FIX_ANDROID_STUDIO_CACHE.md** for nuclear options:
- Delete Android Studio configuration folders
- Kill Gradle daemon manually
- Check for duplicate source files
- Verify Git status

---

## Notes Section (Write Here)

Time started: ___________

Step completed / Issues encountered:
- Step 1: ___________________________________________
- Step 2: ___________________________________________
- Step 3: ___________________________________________
- Step 4: ___________________________________________
- Step 5: ___________________________________________
- Step 6: ___________________________________________
- Step 7: ___________________________________________

Final result: ___________________________________________
