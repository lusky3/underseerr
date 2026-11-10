# Underseerr Pro Backend

This Cloudflare Worker handles the premium features for the Underseerr application.

## Features

* **License Verification**: Gated access to push notifications for premium users.
* **D1 Database**: Stores license records and user/device mappings.
* **KV Cache**: High-performance storage for token reverse mapping.

## Setup

### Prerequisites

1. **Cloudflare CLI (Wrangler)** installed.
2. **Google Play Service Account** with "Billing" and "Developer" access.

### Environment Variables

Store these as secrets in Cloudflare via `wrangler secret put`:

* `GOOGLE_APPLICATION_CREDENTIALS_JSON`: The JSON for your Google service account.
* `WEBHOOK_SECRET`: (Optional) Global secret for unauthorized request filtering.

Plain variables, set in `wrangler.jsonc`:

* `FREE_ACCESS_UNTIL`: (Optional) A date, `YYYY-MM-DD`. Until the end of that day (UTC) every user gets hosted notifications without a trial or license; a paid license keeps its own expiry. It is currently `2027-12-31`. **To end the free period early, remove the variable (or set it empty) and redeploy production**: the app picks the change up the next time Settings opens. The app labels the period "may end sooner, subject to capacity".

### Deployment

1. Initialize the database:

    ```bash
    npx wrangler d1 execute underseerr-billing --remote --file=./schema.sql
    ```

2. Deploy the worker:

    ```bash
    npx wrangler deploy
    ```
