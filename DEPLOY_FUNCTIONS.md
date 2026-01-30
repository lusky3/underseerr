# Deploying Firebase Cloud Functions for Overseerr Notifications

## Prerequisites

- Node.js (v18+) and npm installed
- Firebase CLI installed (`npm install -g firebase-tools`)
- Login to Firebase (`firebase login`)
- **Important:** Your Firebase Project must be on the **Blaze (Pay as you go)** plan.
  - This is required for deploying Cloud Functions.
  - The free usage limits are generous, so it often remains cost-free for personal use.

## Steps

1. **Navigate to the functions directory:**

   ```bash
   cd functions
   ```

2. **Check your Project ID:**
   Run `firebase projects:list` to see your available projects.
   The app is currently configured to use `overseerr-client-app`.

   If your project ID is different, update the URL in `SettingsViewModel.kt`:

   ```kotlin
   val webhookUrl = "https://us-central1-YOUR_PROJECT_ID.cloudfunctions.net/sendFcmNotification"
   ```

3. **Install Dependencies:**

   ```bash
   npm install
   ```

4. **Deploy:**
   Run the following command in the root directory:

   ```bash
   firebase deploy --only functions --project overseerr-client-app
   ```

   *(Replace `overseerr-client-app` with your actual project ID if different)*

5. **Verify Deployment:**
   Go to the Firebase Console -> Functions Dashboard.
   You should see `sendFcmNotification` listed.

## App Configuration

1. Build and install the Android App.
2. Log in to Overseerr within the app.
3. Go to **Settings -> Advanced Integration**.
4. Tap **"Configure Notification Webhook"**.
   - This will automatically set up the Notification Agent in your Overseerr server to point to the Cloud Function.
5. Ensure "Enable Notifications" is switched ON.

## Troubleshooting

- **Permission Errors:** Ensure your Google Account has `Editor` or `Owner` role on the Firebase project.
- **Subscription Errors:** Check the Android Logcat for `PushNotificationService` tags.
- **Overseerr Logs:** Check the Overseerr system logs if notifications aren't triggering.
