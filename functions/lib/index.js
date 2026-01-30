"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.sendFcmNotification = void 0;
const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();
exports.sendFcmNotification = functions.https.onRequest(async (req, res) => {
    if (req.method !== "POST") {
        res.status(405).send("Method Not Allowed");
        return;
    }
    const payload = req.body;
    functions.logger.info("Received Overseerr payload", payload);
    // Determine the target email
    // The 'email' field should be injected by our specific webhook configuration
    // Fallback to notifyuser_email or requestedBy_email if present
    const targetEmail = payload.email || payload.notifyuser_email || payload.requestedBy_email;
    if (!targetEmail) {
        functions.logger.error("No target email found in payload");
        res.status(400).send("Missing target email");
        return;
    }
    try {
        // 1. Lookup the user's FCM token from Firestore
        // Structure: users/{email}/fcmToken
        // Note: Email might need encoding or we store by UID if we can map it via Overseerr -> App
        // For simplicity, let's assume we store documents with the email as ID (or query by field)
        // Let's query by field 'email' in 'users' collection
        const usersRef = admin.firestore().collection("users");
        const snapshot = await usersRef.where("email", "==", targetEmail).limit(1).get();
        if (snapshot.empty) {
            functions.logger.warn(`No user found with email: ${targetEmail}`);
            res.status(404).send("User not found");
            return;
        }
        const userDoc = snapshot.docs[0];
        const userData = userDoc.data();
        const fcmToken = userData.fcmToken;
        if (!fcmToken) {
            functions.logger.warn(`User ${targetEmail} has no FCM token`);
            res.status(404).send("FCM token not found");
            return;
        }
        // 2. Construct FCM Message
        const message = {
            token: fcmToken,
            data: {
                title: payload.subject,
                message: payload.message || "",
                image: payload.image || "",
                type: payload.notification_type || "unknown",
                url: "underseerr://request", // detailed deep link logic can be expanded
            },
            android: {
                priority: "high",
                notification: {
                    title: payload.subject,
                    body: payload.message,
                    icon: "app_icon_transparent",
                    color: "#FFFFFF",
                    imageUrl: payload.image,
                },
            },
        };
        // 3. Send
        const msgId = await admin.messaging().send(message);
        functions.logger.info(`Successfully sent message: ${msgId}`);
        res.status(200).send({ success: true, messageId: msgId });
    }
    catch (error) {
        functions.logger.error("Error sending notification", error);
        res.status(500).send("Internal Server Error");
    }
});
//# sourceMappingURL=index.js.map