# Underseerr Notification Relay

This is a lightweight Cloudflare Worker that acts as a secure relay between your [Overseerr](https://overseerr.dev/) instance and the **Underseerr** mobile app.

It handles:

1. **FCM Delivery**: Safely delivering push notifications via Google/Firebase.
2. **Privacy**: Hashing user emails so PII is never stored in plain text.
3. **Web Push**: Supporting the modern W3C Push API for cross-platform compatibility.

## 🚀 1-Click Deploy

Use the button below to deploy this relay to your own Cloudflare account for free.

[![Deploy to Cloudflare Workers](https://deploy.workers.cloudflare.com/button)](https://deploy.workers.cloudflare.com/?url=https://github.com/lusky3/underseerr/tree/main/notification-relay)

## 📋 Prerequisites

Before deploying, you will need:

1. A **Cloudflare Account** (Free tier is sufficient).
2. A **Firebase Service Account JSON**:
   * Create a project at [console.firebase.google.com](https://console.firebase.google.com).
   * Go to Project Settings > Service Accounts.
   * Click "Generate new private key".
   * You will need the content of this JSON file during deployment.

## ⚙️ Configuration

During deployment, you will be prompted to set the following:

* `GOOGLE_APPLICATION_CREDENTIALS_JSON`: The contents of your Firebase service account JSON.
* `TOKENS`: A Cloudflare KV Namespace (the deployer will help you create this).

Once deployed, copy your Worker's URL (e.g., `https://your-relay.workers.dev`) and enter it in the **Underseerr App Settings** under "Notification Server".
