export interface Env {
    TOKENS: KVNamespace;
    DB: D1Database;
    // Secret: Google Service Account JSON string
    GOOGLE_APPLICATION_CREDENTIALS_JSON: string;
    // Optional: Secret for webhook authorization
    WEBHOOK_SECRET?: string;
    MODE?: string;
}

// Minimal JWT signing for Google Auth (FCM)
async function getAccessToken(serviceAccountJson: string): Promise<string> {
    const serviceAccount = JSON.parse(serviceAccountJson);
    const now = Math.floor(Date.now() / 1000);
    const hour = 3600;

    const header = {
        alg: 'RS256',
        typ: 'JWT',
    };

    const claim = {
        iss: serviceAccount.client_email,
        scope: 'https://www.googleapis.com/auth/firebase.messaging',
        aud: serviceAccount.token_uri,
        exp: now + hour,
        iat: now,
    };

    const encodedHeader = btoa(JSON.stringify(header));
    const encodedClaim = btoa(JSON.stringify(claim));

    const toSign = `${encodedHeader}.${encodedClaim}`;

    const pemHeader = "-----BEGIN PRIVATE KEY-----";
    const pemFooter = "-----END PRIVATE KEY-----";
    const pemContents = serviceAccount.private_key
        .replace(pemHeader, "")
        .replace(pemFooter, "")
        .replace(/\s/g, "");

    const binaryKey = Uint8Array.from(atob(pemContents), c => c.charCodeAt(0));

    const key = await crypto.subtle.importKey(
        "pkcs8",
        binaryKey,
        {
            name: "RSASSA-PKCS1-v1_5",
            hash: "SHA-256",
        },
        false,
        ["sign"]
    );

    const signature = await crypto.subtle.sign(
        "RSASSA-PKCS1-v1_5",
        key,
        new TextEncoder().encode(toSign)
    );

    const encodedSignature = btoa(String.fromCharCode(...new Uint8Array(signature)))
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=+$/, '');

    const jwt = `${encodedHeader}.${encodedClaim}.${encodedSignature}`;

    const response = await fetch(serviceAccount.token_uri, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${jwt}`,
    });

    const data: any = await response.json();
    return data.access_token;
}

async function hashEmail(email: string): Promise<string> {
    const msgBuffer = new TextEncoder().encode(email.trim().toLowerCase());
    const hashBuffer = await crypto.subtle.digest('SHA-256', msgBuffer);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
}

async function checkLicense(userId: string, db: D1Database): Promise<{ isPremium: boolean, expiresAt: number | null }> {
    // Check if user has an active license in D1
    const license = await db.prepare(
        "SELECT expires_at FROM licenses WHERE user_id = ? AND status = 'active' AND expires_at > ?"
    ).bind(userId, Date.now()).first();

    if (license) {
        return { isPremium: true, expiresAt: license.expires_at as number };
    }
    return { isPremium: false, expiresAt: null };
}

export default {
    async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
        try {
            const url = new URL(request.url);

            if (url.pathname === '/') {
                return new Response(`Underseerr Pro API is operational (${env.MODE || 'UNKNOWN'} mode)`, { status: 200 });
            }

            // --- Licensing Endpoints ---

            if (request.method === 'POST' && url.pathname === '/validate-key') {
                const { key, userId } = await request.json() as { key: string, userId: string };
                if (!key || !userId) return new Response("Missing key or userId", { status: 400 });

                const license = await env.DB.prepare(
                    "SELECT * FROM serial_keys WHERE key = ? AND status = 'available'"
                ).bind(key).first();

                if (license) {
                    const expiresAt = Date.now() + (365 * 24 * 60 * 60 * 1000); // 1 year
                    await env.DB.prepare(
                        "UPDATE serial_keys SET status = 'used', used_by = ? WHERE key = ?"
                    ).bind(userId, key).run();

                    await env.DB.prepare(
                        "INSERT INTO licenses (user_id, serial_key, expires_at, status) VALUES (?, ?, ?, 'active')"
                    ).bind(userId, key, expiresAt).run();

                    return new Response(JSON.stringify({ isPremium: true, expiresAt }));
                }
                return new Response(JSON.stringify({ isPremium: false }), { status: 401 });
            }

            if (request.method === 'GET' && url.pathname === '/subscription-status') {
                const userId = url.searchParams.get("userId");
                if (!userId) return new Response("Missing userId", { status: 400 });

                const status = await checkLicense(userId, env.DB);
                return new Response(JSON.stringify(status));
            }

            if (request.method === 'POST' && url.pathname === '/verify-purchase') {
                const { userId, productId, purchaseToken, packageName } = await request.json() as any;
                if (!userId || !productId || !purchaseToken || !packageName) {
                    return new Response("Missing required fields", { status: 400 });
                }

                try {
                    const serviceAccountJson = env.GOOGLE_APPLICATION_CREDENTIALS_JSON;
                    if (!serviceAccountJson) return new Response("Service Account not configured", { status: 500 });

                    let sa = serviceAccountJson;
                    if (typeof sa === 'string' && !sa.trim().startsWith('{')) {
                        try { sa = atob(sa); } catch (e) { }
                    }

                    const accessToken = await getAccessToken(sa);
                    const verifyUrl = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/purchases/subscriptions/${productId}/tokens/${purchaseToken}`;

                    const verifyRes = await fetch(verifyUrl, {
                        headers: { 'Authorization': `Bearer ${accessToken}` }
                    });

                    if (!verifyRes.ok) {
                        const error = await verifyRes.text();
                        console.error("Google Play Verification Failed:", error);
                        return new Response(JSON.stringify({ isPremium: false, error: "Validation failed" }), { status: 401 });
                    }

                    const purchaseData: any = await verifyRes.json();

                    // Check if subscription is active (expiryTimeMillis > now)
                    const expiryTime = parseInt(purchaseData.expiryTimeMillis);
                    const isPremium = expiryTime > Date.now();

                    if (isPremium) {
                        // Update database
                        await env.DB.prepare(
                            "INSERT INTO licenses (user_id, serial_key, expires_at, status) VALUES (?, ?, ?, 'active')"
                        ).bind(userId, `google_play_${purchaseToken.substring(0, 16)}`, expiryTime).run();
                    }

                    return new Response(JSON.stringify({ isPremium, expiresAt: expiryTime }));
                } catch (e: any) {
                    return new Response(`Verification Error: ${e.message}`, { status: 500 });
                }
            }

            // --- Notification Logic (With Gating) ---

            if (request.method === 'POST' && (url.pathname === '/webhook' || url.pathname === '/register')) {
                // Check subscription for hosted users
                // In a real prod environment, you'd extract userId from the request or payload
                // For now, we'll assume gating is done by email lookup if we had a mapping
                // But let's just implement the logic structure
            }

            if (request.method === 'POST' && url.pathname === '/register') {
                try {
                    const body: any = await request.json();
                    if (!body.email || !body.token || !body.userId) {
                        return new Response("Missing email, token, or userId", { status: 400 });
                    }
                    const emailHash = await hashEmail(body.email);

                    // 1. Store FCM token in KV (Email -> Token)
                    await env.TOKENS.put(emailHash, body.token);
                    // 1b. Store Reverse Mapping (Token -> Email) for efficient gating on /push
                    await env.TOKENS.put(`rev:${body.token}`, emailHash);

                    // 2. Map hashed email to userId in D1 (for gating)
                    await env.DB.prepare(
                        "INSERT INTO email_mapping (email_hash, user_id, webhook_secret, updated_at) VALUES (?, ?, ?, ?) " +
                        "ON CONFLICT(email_hash) DO UPDATE SET user_id=excluded.user_id, webhook_secret=COALESCE(excluded.webhook_secret, email_mapping.webhook_secret), updated_at=excluded.updated_at"
                    ).bind(emailHash, body.userId, body.webhookSecret || null, Date.now()).run();

                    return new Response(JSON.stringify({ success: true }));
                } catch (e: any) {
                    return new Response(`Error: ${e.message}`, { status: 500 });
                }
            }

            if (request.method === 'POST' && url.pathname === '/webhook') {
                try {
                    // Verify Webhook Secret if configured
                    if (env.WEBHOOK_SECRET) {
                        const authHeader = request.headers.get("X-Underseerr-Secret");
                        if (authHeader !== env.WEBHOOK_SECRET) {
                            return new Response("Unauthorized Webhook", { status: 401 });
                        }
                    }

                    const payload: any = await request.json();
                    const validEmail = payload.email || payload.notifyuser_email || payload.requestedBy_email;
                    if (!validEmail) return new Response("No target email", { status: 400 });

                    const emailHash = await hashEmail(validEmail);
                    const fcmToken = await env.TOKENS.get(emailHash);
                    if (!fcmToken) return new Response(`No device registered`, { status: 404 });

                    // --- GATING LOGIC ---
                    // 1. Find the User ID and Secret associated with this email hash
                    const mapping = await env.DB.prepare(
                        "SELECT user_id, webhook_secret FROM email_mapping WHERE email_hash = ?"
                    ).bind(emailHash).first();

                    if (!mapping) {
                        return new Response("User context not found", { status: 403 });
                    }

                    // 2. Verify Webhook Secret (Tenant-Specific)
                    if (mapping.webhook_secret) {
                        const clientSecret = request.headers.get("X-Underseerr-Secret");
                        if (clientSecret !== mapping.webhook_secret) {
                            return new Response("Invalid Webhook Secret", { status: 401 });
                        }
                    }

                    // 3. Check if that user has an active premium subscription
                    const status = await checkLicense(mapping.user_id as string, env.DB);
                    if (!status.isPremium) {
                        return new Response("Premium subscription required for hosted notifications", { status: 402 });
                    }
                    // --- END GATING ---

                    if (!env.GOOGLE_APPLICATION_CREDENTIALS_JSON) return new Response("Config Error: GOOGLE_APPLICATION_CREDENTIALS_JSON missing", { status: 500 });

                    let serviceAccountJson = env.GOOGLE_APPLICATION_CREDENTIALS_JSON;
                    if (typeof serviceAccountJson === 'string' && !serviceAccountJson.trim().startsWith('{')) {
                        try { serviceAccountJson = atob(serviceAccountJson); } catch (e) { }
                    }

                    const accessToken = await getAccessToken(serviceAccountJson);
                    const projectId = JSON.parse(serviceAccountJson).project_id;
                    const fcmUrl = `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`;

                    const cleanSubject = String(payload.subject || "").substring(0, 100);
                    const cleanMessage = String(payload.message || "").substring(0, 500);

                    const messageBody = {
                        message: {
                            token: fcmToken,
                            data: {
                                title: cleanSubject,
                                message: cleanMessage,
                                image: payload.image || "",
                                url: "underseerr://request"
                            },
                            android: {
                                priority: "high",
                                notification: {
                                    title: cleanSubject,
                                    body: cleanMessage,
                                    icon: "app_icon_transparent",
                                    color: "#FFFFFF",
                                    image: payload.image
                                }
                            }
                        }
                    };

                    const fcmRes = await fetch(fcmUrl, {
                        method: 'POST',
                        headers: { 'Authorization': `Bearer ${accessToken}`, 'Content-Type': 'application/json' },
                        body: JSON.stringify(messageBody)
                    });

                    if (!fcmRes.ok) return new Response(`Upstream Error`, { status: 502 });
                    const responseData: any = await fcmRes.json();
                    return new Response(JSON.stringify({ success: true, messageId: responseData.name }));
                } catch (e: any) {
                    return new Response(`Processing Error`, { status: 500 });
                }
            }

            // --- Web Push Proxy (Blind) ---
            // POST /push/:token
            if (request.method === 'POST' && url.pathname.startsWith('/push/')) {
                try {
                    const token = url.pathname.split('/').pop();
                    if (!token) return new Response("Missing token in path", { status: 400 });

                    // --- GATING LOGIC ---
                    // For direct push registration, Overseerr doesn't send email in the path.
                    // We use the reverse mapping in KV.
                    const emailHash = await env.TOKENS.get(`rev:${token}`);

                    // In STAGING mode, we can attempt to proceed even without reverse mapping if token looks valid 
                    // OR we just bypass the license check for testability.
                    const isStaging = (env.MODE === 'STAGING');

                    if (!isStaging) {
                        if (!emailHash) return new Response("Device not registered (no reverse mapping)", { status: 403 });

                        const mapping = await env.DB.prepare(
                            "SELECT user_id FROM email_mapping WHERE email_hash = ?"
                        ).bind(emailHash).first();

                        if (!mapping) return new Response("User context not found for token", { status: 403 });

                        const status = await checkLicense(mapping.user_id as string, env.DB);
                        if (!status.isPremium) return new Response("Premium required", { status: 402 });
                    }
                    // --- END GATING ---

                    if (!env.GOOGLE_APPLICATION_CREDENTIALS_JSON) return new Response("Config Error: Service Account Missing", { status: 500 });
                    let sa = env.GOOGLE_APPLICATION_CREDENTIALS_JSON;
                    if (typeof sa === 'string' && !sa.trim().startsWith('{')) {
                        try { sa = atob(sa); } catch (e) { }
                    }
                    const accessToken = await getAccessToken(sa);
                    const projectId = JSON.parse(sa).project_id;
                    const fcmUrl = `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`;

                    const body = await request.arrayBuffer();
                    const uint8 = new Uint8Array(body);
                    let binary = '';
                    for (let i = 0; i < uint8.length; i++) binary += String.fromCharCode(uint8[i]);
                    const bodyBase64 = btoa(binary);

                    const headers: any = {};
                    ['encryption', 'crypto-key', 'content-encoding', 'ttl', 'content-type'].forEach(h => {
                        const val = request.headers.get(h);
                        if (val) headers[h] = val;
                    });

                    const messageBody = {
                        message: {
                            token: token,
                            data: { type: "webpush_encrypted", payload: bodyBase64, headers: JSON.stringify(headers) },
                            android: { priority: "high" }
                        }
                    };

                    const fcmRes = await fetch(fcmUrl, {
                        method: 'POST',
                        headers: { 'Authorization': `Bearer ${accessToken}`, 'Content-Type': 'application/json' },
                        body: JSON.stringify(messageBody)
                    });

                    if (!fcmRes.ok) return new Response(`FCM Error`, { status: 502 });
                    return new Response(JSON.stringify({ success: true }), { status: 201 });
                } catch (e: any) {
                    return new Response(`Error: ${e.message}`, { status: 500 });
                }
            }

            return new Response("Not Found", { status: 404 });
        } catch (e: any) {
            console.error(`[Worker Global Error] ${e.message}`);
            return new Response(`Worker Internal Error: ${e.message}\n${e.stack}`, { status: 500 });
        }
    },
};
