export interface Env {
    TOKENS: KVNamespace;
    // Secret: Google Service Account JSON string
    GOOGLE_APPLICATION_CREDENTIALS_JSON: string;
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

    // Sign with RSA-SHA256 using the private key
    // We need to import the PEM key
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

    // Exchanging JWT for Access Token
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

// Helper to hash email for privacy (SHA-256)
async function hashEmail(email: string): Promise<string> {
    const msgBuffer = new TextEncoder().encode(email.trim().toLowerCase());
    const hashBuffer = await crypto.subtle.digest('SHA-256', msgBuffer);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
}

export default {
    async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
        const url = new URL(request.url);

        // Endpoint 1: Register Device Token
        // POST /register
        // Body: { "email": "user@email.com", "token": "FCM_TOKEN" }
        if (request.method === 'POST' && url.pathname === '/register') {
            try {
                const body: any = await request.json();
                if (!body.email || !body.token) {
                    return new Response("Missing email or token", { status: 400 });
                }

                // Privacy: Hash email before storage
                const emailHash = await hashEmail(body.email);

                // Store in KV: Key = SHA256(Email), Value = Token
                // In real app, might want a list of tokens per email, but 1:1 is fine for MVP
                await env.TOKENS.put(emailHash, body.token);

                return new Response(JSON.stringify({ success: true }), {
                    headers: { "Content-Type": "application/json" }
                });
            } catch (e: any) {
                return new Response(`Error: ${e.message}`, { status: 500 });
            }
        }

        // Endpoint 2: Overseerr Webhook
        // POST /webhook
        if (request.method === 'POST' && url.pathname === '/webhook') {
            try {
                const payload: any = await request.json();

                // Determine Target Email
                const validEmail = payload.email || payload.notifyuser_email || payload.requestedBy_email;
                if (!validEmail) {
                    return new Response("No target email in payload", { status: 400 });
                }

                // Privacy: Hash the email before lookup
                // We do NOT log the email or payload content to avoid PII leakage in logs
                const emailHash = await hashEmail(validEmail);

                // Lookup FCM Token using Hash
                const fcmToken = await env.TOKENS.get(emailHash);
                if (!fcmToken) {
                    // Do not reveal which email was missing, generic error
                    return new Response(`No device registered for user`, { status: 404 });
                }

                // Get Google Access Token
                // Get Google Access Token
                if (!env.GOOGLE_APPLICATION_CREDENTIALS_JSON) {
                    console.error("GOOGLE_APPLICATION_CREDENTIALS_JSON env var is missing");
                    return new Response("Service Account not configured", { status: 500 });
                }

                let serviceAccountJson = env.GOOGLE_APPLICATION_CREDENTIALS_JSON;
                // If it doesn't look like JSON, assume Base64 and try to decode
                if (!serviceAccountJson.trim().startsWith('{')) {
                    try {
                        serviceAccountJson = atob(serviceAccountJson);
                    } catch (err) {
                        console.error("Failed to decode Base64 secret", err);
                        return new Response("Invalid Service Account credentials", { status: 500 });
                    }
                }

                const accessToken = await getAccessToken(serviceAccountJson);

                // Construct FCM Message (HTTP v1 API)
                const projectId = JSON.parse(serviceAccountJson).project_id;
                const fcmUrl = `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`;

                // Sanitize Payload: Only pass necessary strings
                const cleanSubject = String(payload.subject || "").substring(0, 100); // Truncate
                const cleanMessage = String(payload.message || "").substring(0, 500);

                const messageBody = {
                    message: {
                        token: fcmToken,
                        data: {
                            title: cleanSubject,
                            message: cleanMessage,
                            image: payload.image || "",
                            type: payload.notification_type || "unknown",
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
                    headers: {
                        'Authorization': `Bearer ${accessToken}`,
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(messageBody)
                });

                if (!fcmRes.ok) {
                    // Return generic error, log detail internally if needed (but disabled here)
                    return new Response(`Upstream Error`, { status: 502 });
                }

                const responseData: any = await fcmRes.json();
                // Return success but minimize output
                return new Response(JSON.stringify({ success: true, messageId: responseData.name }), {
                    headers: { "Content-Type": "application/json" }
                });

            } catch (e: any) {
                // Generic error to client
                return new Response(`Processing Error`, { status: 500 });
            }
        }

        // Endpoint 3: Web Push Proxy (Blind)
        // POST /push/:token
        if (request.method === 'POST' && url.pathname.startsWith('/push/')) {
            try {
                const token = url.pathname.split('/').pop();
                if (!token) return new Response("Missing token in path", { status: 400 });

                // Get Google Access Token
                if (!env.GOOGLE_APPLICATION_CREDENTIALS_JSON) {
                    return new Response("Service Account not configured", { status: 500 });
                }

                let serviceAccountJson = env.GOOGLE_APPLICATION_CREDENTIALS_JSON;
                if (!serviceAccountJson.trim().startsWith('{')) {
                    try { serviceAccountJson = atob(serviceAccountJson); } catch (e) { }
                }
                const accessToken = await getAccessToken(serviceAccountJson);
                const projectId = JSON.parse(serviceAccountJson).project_id;
                const fcmUrl = `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`;

                // Forward the RAW payload (encrypted) and headers
                const body = await request.arrayBuffer();
                const uint8 = new Uint8Array(body);
                let binary = '';
                for (let i = 0; i < uint8.length; i++) {
                    binary += String.fromCharCode(uint8[i]);
                }
                const bodyBase64 = btoa(binary);

                // Collect relevant headers for Web Push decryption
                const headers: any = {};
                ['encryption', 'crypto-key', 'content-encoding', 'ttl', 'content-type'].forEach(h => {
                    const val = request.headers.get(h);
                    if (val) headers[h] = val;
                });

                console.log(`Forwarding push for token: ${token.substring(0, 10)}... Size: ${body.byteLength} Encoding: ${headers['content-encoding']}`);

                const messageBody = {
                    message: {
                        token: token,
                        data: {
                            type: "webpush_encrypted",
                            payload: bodyBase64,
                            headers: JSON.stringify(headers)
                        },
                        android: {
                            priority: "high"
                        }
                    }
                };

                const fcmRes = await fetch(fcmUrl, {
                    method: 'POST',
                    headers: {
                        'Authorization': `Bearer ${accessToken}`,
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(messageBody)
                });

                const fcmText = await fcmRes.text();
                if (!fcmRes.ok) {
                    console.error(`FCM Error: ${fcmRes.status} - ${fcmText}`);
                    return new Response(`FCM Error: ${fcmText}`, { status: 502 });
                }

                console.log(`FCM Success: ${fcmText}`);
                return new Response(JSON.stringify({ success: true, fcm: JSON.parse(fcmText) }), { status: 201 });

            } catch (e: any) {
                return new Response(`Error: ${e.message}`, { status: 500 });
            }
        }

        return new Response("Underseerr Notification Worker", { status: 200 });
    },
};
