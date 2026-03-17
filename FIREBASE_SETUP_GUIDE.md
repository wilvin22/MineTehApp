# Firebase Setup Guide for MineTeh Notifications

The notifications system is currently configured to work without Firebase for development purposes. To enable full push notification functionality, follow these steps:

## Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Click "Create a project" or "Add project"
3. Enter project name: `MineTeh` (or your preferred name)
4. Enable Google Analytics (optional)
5. Click "Create project"

## Step 2: Add Android App to Firebase

1. In your Firebase project, click "Add app" and select Android
2. Enter your package name: `com.example.mineteh`
3. Enter app nickname: `MineTeh Android`
4. Enter SHA-1 certificate fingerprint (optional for development)
5. Click "Register app"

## Step 3: Download Configuration File

1. Download the `google-services.json` file
2. Place it in your `app/` directory (same level as `build.gradle.kts`)
3. The file structure should look like:
   ```
   MineTeh2/
   ├── app/
   │   ├── google-services.json  ← Place here
   │   ├── build.gradle.kts
   │   └── src/
   ```

## Step 4: Enable Firebase in Your App

1. **Uncomment Firebase plugin in `app/build.gradle.kts`:**
   ```kotlin
   plugins {
       // ... other plugins
       id("com.google.gms.google-services") version "4.4.0"  // Uncomment this line
   }
   ```

2. **Uncomment Firebase dependencies in `app/build.gradle.kts`:**
   ```kotlin
   dependencies {
       // ... other dependencies
       
       // Firebase Cloud Messaging - Uncomment these lines
       implementation("com.google.firebase:firebase-messaging:23.4.0")
       implementation("com.google.firebase:firebase-analytics:21.5.0")
   }
   ```

3. **Uncomment Firebase service in `app/src/main/AndroidManifest.xml`:**
   ```xml
   <!-- Uncomment this entire block -->
   <service
       android:name=".service.NotificationService"
       android:exported="false">
       <intent-filter>
           <action android:name="com.google.firebase.MESSAGING_EVENT" />
       </intent-filter>
   </service>

   <meta-data
       android:name="com.google.firebase.messaging.default_notification_icon"
       android:resource="@drawable/ic_notifications" />
   <meta-data
       android:name="com.google.firebase.messaging.default_notification_color"
       android:resource="@color/purple" />
   <meta-data
       android:name="com.google.firebase.messaging.default_notification_channel_id"
       android:value="@string/default_notification_channel_id" />
   ```

4. **Update NotificationService to extend FirebaseMessagingService:**
   - Uncomment the Firebase imports
   - Change class declaration to extend `FirebaseMessagingService`
   - Uncomment the `onMessageReceived` and `onNewToken` methods
   - Remove the `initialize` method and restore `onCreate`

## Step 5: Test Push Notifications

1. Build and run your app
2. In Firebase Console, go to "Cloud Messaging"
3. Click "Send your first message"
4. Enter a test message and select your app
5. Send the notification to test

## Step 6: Server Integration (Optional)

To send notifications from your backend:

1. In Firebase Console, go to Project Settings → Service Accounts
2. Generate a new private key for your service account
3. Use the Firebase Admin SDK in your backend to send notifications
4. Store FCM tokens in your Supabase database when users register

## Current Status

✅ **Working without Firebase:**
- In-app notifications via Supabase real-time
- Notification preferences and quiet hours
- Deep linking and navigation
- All UI components and data management

🔄 **Requires Firebase setup:**
- Push notifications when app is closed
- Background notification delivery
- FCM token management

## Troubleshooting

**Build Error: "google-services.json is missing"**
- Make sure the file is in the correct location (`app/google-services.json`)
- Verify the package name matches exactly: `com.example.mineteh`

**Notifications not received:**
- Check that the app has notification permissions
- Verify Firebase project configuration
- Test with Firebase Console first before implementing server-side sending

**Deep links not working:**
- Ensure the app is installed and the URL scheme is registered
- Test deep links using ADB: `adb shell am start -W -a android.intent.action.VIEW -d "mineteh://notifications"`