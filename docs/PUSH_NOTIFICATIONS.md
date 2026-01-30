# Setting Up Push Notifications

Underseerr supports native push notifications for Android. To enable this functionality without relying on a central server managed by the app developers, you can deploy your own lightweight notification backend.

This "Bring Your Own Backend" approach allows you to:

- Be in full control of your data.
- Utilize generous free tiers from cloud providers (often $0/month).
- Avoid relying on a 3rd party subscription service.

We recommend **Cloudflare Workers** as the easiest and most cost-effective method.

## Option 1: Cloudflare Workers (Recommended)

Cloudflare Workers allow you to host the notification bridge for free (up to 100,000 requests/day).

### Prerequisites

1. **Cloudflare Account**: [Sign up here](https://dash.cloudflare.com/sign-up).
2. **Firebase Project**: You need a Firebase project to use Google's Cloud Messaging (FCM) service.
3. **Wrangler CLI**: Install on your computer via `npm install -g wrangler`.

### Step 1: Get Firebase Service Account

1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Select your project (or create one).
3. Go to **Project Settings -> Service accounts**.
4. Click **Generate new private key**.
5. Save the JSON file. You will need its content shortly.

### Step 2: Prepare the Worker

The worker code is provided in the repository under the `worker/` directory.

1. Open your terminal and navigate to the `worker` folder of the repo:

    ```bash
    cd worker
    ```

2. Install dependencies:

    ```bash
    npm install
    ```

3. Create a storage namespace (KV) for device tokens:

    ```bash
    npx wrangler kv:namespace create "TOKENS"
    ```

    *Copy the `id` from the output.*
4. Open `worker/wrangler.toml` and paste the `id` into the `[[kv_namespaces]]` section.

### Step 3: Deploy

1. Login to Cloudflare:

    ```bash
    npx wrangler login
    ```

2. Deploy the worker:

    ```bash
    npx wrangler deploy
    ```

3. **Set the Secret (Crucial):**
    Open the Firebase JSON file you downloaded in Step 1. Copy the **entire content**.
    Run this command to save it securely:

    ```bash
    npx wrangler secret put GOOGLE_APPLICATION_CREDENTIALS_JSON
    ```

    Paste the JSON content when prompted.

### Step 4: Configure the App

1. Note your deployed worker URL (e.g., `https://underseerr-notifications.your-name.workers.dev`).
2. Open **Underseerr** on your Android device.
3. Go to **Settings -> Advanced Integration**.
4. Tap **Notification Server**.
5. Enter your Worker URL (e.g., `https://underseerr-notifications.your-name.workers.dev`).
6. Tap **Save**.
7. Tap **Configure Notification Webhook**.
    - This will automatically log in to your Overseerr server and configure the notification settings to point to your new worker.

---

## Option 2: Firebase Cloud Functions

If you prefer to stay entirely within the Google ecosystem, you can use Firebase Cloud Functions.

**Note:** This requires the **Blaze (Pay as you go)** plan on Firebase, even if your usage falls within the free tier.

1. Navigate to the `functions` directory in the repo.
2. Run `npm install`.
3. Deploy using the Firebase CLI:

    ```bash
    firebase deploy --only functions
    ```

4. In the App Settings, enter your Firebase Functions URL as the Notification Server.
