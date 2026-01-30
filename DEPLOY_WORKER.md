# Deploying Notification Worker (Cloudflare)

This guide explains how to deploy the notification backend to Cloudflare Workers. This is a lightweight, low-cost (often free) alternative to Firebase Cloud Functions.

## Prerequisites

1. **Cloudflare Account**: [Sign up here](https://dash.cloudflare.com/sign-up).
2. **Node.js & NPM**: Installed on your machine.
3. **Wrangler CLI**: `npm install -g wrangler`
4. **Firebase Project (for FCM)**: You still need a Firebase project to use Cloud Messaging (FCM), but you don't need the paid "Blaze" plan for hosting.

## Steps

### 1. Get Firebase Service Account

1. Go to [Firebase Console](https://console.firebase.google.com/).
2. Select your project -> Project Settings -> Service accounts.
3. Click "Generate new private key".
4. Save the JSON file. You will need its content.

### 2. Configure Worker

1. Navigate to the `worker` directory:

    ```bash
    cd worker
    ```

2. Install dependencies:

    ```bash
    npm install
    ```

3. Create a KV Namespace for storing tokens:

    ```bash
    npx wrangler kv:namespace create "TOKENS"
    ```

    *Output Example:*

    ```toml
    [[kv_namespaces]]
    binding = "TOKENS"
    id = "e57c..."
    ```

4. Copy the `id` from the output and update `wrangler.toml`:

    ```toml
    [[kv_namespaces]]
    binding = "TOKENS"
    id = "YOUR_KV_ID_HERE"
    ```

### 3. Deploy

1. Login to Cloudflare:

    ```bash
    npx wrangler login
    ```

2. Deploy the worker:

    ```bash
    npx wrangler deploy
    ```

3. **Set the Secret (Crucial):**
    Open the JSON file you downloaded from Firebase in Step 1. Copy the entire content.
    Run this command to save it securely (do not commit it):

    ```bash
    npx wrangler secret put GOOGLE_APPLICATION_CREDENTIALS_JSON
    ```

    Paste the JSON content when prompted.

### 4. Connect App

1. Note your worker URL (e.g., `https://underseerr-notifications.your-name.workers.dev`).
2. Open the Android App -> Settings -> Advanced Integration.
3. Tap **Notification Server** and enter your Worker URL (remove any trailing slash).
4. Tap **"Configure Notification Webhook"** to auto-configure Overseerr.

## Troubleshooting

- **Check Logs:** `npx wrangler tail` shows real-time logs.
- **FCM Errors:** Ensure the Firebase Service Account has "Cloud Messaging Service Agent" role (usually default).
