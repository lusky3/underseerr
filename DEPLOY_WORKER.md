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

3. **Automatic Provisioning:**
    The project uses `wrangler.jsonc` configured for automatic resource provisioning. You do **not** need to manually create KV namespaces or copy IDs.

    When the GitHub Action runs `wrangler deploy`, Cloudflare will automatically:
    * Create the necessary KV Namespaces (`underseerr-notifications-prod-TOKENS`, etc.) if they don't exist.
    * Link them to your worker.

4. Add these to your **GitHub Repository Secrets**:
    * `CLOUDFLARE_API_TOKEN_PROD` / `CLOUDFLARE_API_TOKEN_STAGING`
    * `GOOGLE_APPLICATION_CREDENTIALS_JSON_PROD` / `GOOGLE_APPLICATION_CREDENTIALS_JSON_STAGING`
    * `CLOUDFLARE_ACCOUNT_ID`

### 3. Deploy

1. Login: `npx wrangler login`

2. **Set Secrets (Per Environment):**

    *Production:*

    ```bash
    npx wrangler secret put GOOGLE_APPLICATION_CREDENTIALS_JSON --env production
    ```

    *(Paste Prod JSON)*

    *Staging:*

    ```bash
    npx wrangler secret put GOOGLE_APPLICATION_CREDENTIALS_JSON --env staging
    ```

    *(Paste Staging JSON)*

3. Deploy manually (optional):

    ```bash
    npx wrangler deploy --env staging
    npx wrangler deploy --env production
    ```

## 4. Production Deployment (Secure CI/CD)

For a critical production backend, **do not** deploy from your local machine. Use GitHub Actions to automate deployments, ensure code quality, and manage secrets securely.

### Step 4.1: Configure GitHub Secrets

Go to your **GitHub Repository -> Settings -> Secrets and variables -> Actions** and add:

1. `CLOUDFLARE_API_TOKEN`: Create this in the [Cloudflare Dashboard](https://dash.cloudflare.com/profile/api-tokens) (Template: **Edit Cloudflare Workers**).
2. `CLOUDFLARE_ACCOUNT_ID`: Find this in the Cloudflare Dashboard sidebar (Overview page).
3. `GOOGLE_APPLICATION_CREDENTIALS_JSON_PROD`: Content of your Production Firebase JSON.
4. `GOOGLE_APPLICATION_CREDENTIALS_JSON_STAGING`: Content of your Staging Firebase JSON.

### Step 4.2: Deploy

Simply push changes to the `worker/` directory on the `main` branch.

* **main** branch -> Deploys to `production` environment.
* **develop** branch (create if needed) -> Deploys to `staging`.

The action will:

1. Install dependencies.
2. Run strict type-checking (`tsc`).
3. Securely inject the Google credentials.
4. Deploy to Cloudflare.

### Step 4.3: Rollback

If a bad update is deployed:

1. Run `npx wrangler rollback` locally (requires login).
2. Or re-run a previous successful job in GitHub Actions.
3. Cloudflare keeps the last 10 versions available for instant rollback.

## 5. Connect App

1. Note your worker URL (e.g., `https://underseerr-notifications.your-name.workers.dev`).
2. Open the Android App -> Settings -> Advanced Integration.
3. Tap **Notification Server** and enter your Worker URL (remove any trailing slash).
4. Tap **"Configure Notification Webhook"** to auto-configure Overseerr.

## Troubleshooting

* **Check Logs:** `npx wrangler tail` shows real-time logs.
* **FCM Errors:** Ensure the Firebase Service Account has "Cloud Messaging Service Agent" role (usually default).
