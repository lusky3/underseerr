export interface Env {
    TOKENS: KVNamespace;
    DB: D1Database;
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
        const url = new URL(request.url);

        if (url.pathname === '/') {
            return new Response('Underseerr Pro API is operational', { status: 200 });
        }

        // --- Licensing Endpoints ---

        if (request.method === 'POST' && url.pathname === '/validate-key') {
            const { key, userId } = await request.json() as any;
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
                if (!body.email || !body.token) return new Response("Missing email or token", { status: 400 });
                const emailHash = await hashEmail(body.email);
                await env.TOKENS.put(emailHash, body.token);
                return new Response(JSON.stringify({ success: true }));
            } catch (e: any) {
                return new Response(`Error: ${e.message}`, { status: 500 });
            }
        }

        if (request.method === 'POST' && url.pathname === '/webhook') {
            try {
                const payload: any = await request.json();
                const validEmail = payload.email || payload.notifyuser_email || payload.requestedBy_email;
                if (!validEmail) return new Response("No target email", { status: 400 });

                const emailHash = await hashEmail(validEmail);
                const fcmToken = await env.TOKENS.get(emailHash);
                if (!fcmToken) return new Response(`No device registered`, { status: 404 });

                // GATING: Check if this email is associated with a premium account
                // (Need a mapping table: email_hash -> user_id)
                // For MVP, we skip gating here but the infrastructure is ready in checkLicense()

                if (!env.GOOGLE_APPLICATION_CREDENTIALS_JSON) return new Response("Config Error", { status: 500 });

                let serviceAccountJson = env.GOOGLE_APPLICATION_CREDENTIALS_JSON;
                if (!serviceAccountJson.trim().startsWith('{')) serviceAccountJson = atob(serviceAccountJson);

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

        return new Response("Not Found", { status: 404 });
    },
};
