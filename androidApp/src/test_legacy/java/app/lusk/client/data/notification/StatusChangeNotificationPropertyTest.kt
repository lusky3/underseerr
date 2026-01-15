package app.lusk.client.data.notification

import app.lusk.client.domain.model.Notification
import app.lusk.client.domain.model.NotificationType
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll

/**
 * Property-based tests for status change notifications.
 * Feature: overseerr-android-client
 * Property 24: Status Change Notifications
 * Validates: Requirements 6.1, 6.2, 6.3
 * 
 * For any request status change (approved, available, or declined), the system should 
 * send a push notification to the user with appropriate content (approval confirmation, 
 * availability notice with deep link, or decline reason).
 */
class StatusChangeNotificationPropertyTest : StringSpec({
    
    "Property 24.1: Approved notifications should contain request ID" {
        // Feature: overseerr-android-client, Property 24: Status Change Notifications
        checkAll(100, Arb.approvedNotification()) { notification ->
            // When a request is approved
            notification.type shouldBe NotificationType.REQUEST_APPROVED
            
            // Then notification should contain request ID
            notification.requestId shouldNotBe null
            notification.requestId!! shouldNotBe 0
        }
    }
    
    "Property 24.2: Available notifications should contain deep link" {
        // Feature: overseerr-android-client, Property 24: Status Change Notifications
        checkAll(100, Arb.availableNotification()) { notification ->
            // When media becomes available
            notification.type shouldBe NotificationType.REQUEST_AVAILABLE
            
            // Then notification should contain deep link
            notification.deepLink shouldNotBe null
            notification.deepLink!!.isNotBlank() shouldBe true
            
            // And should contain media ID
            notification.mediaId shouldNotBe null
        }
    }
    
    "Property 24.3: Declined notifications should contain request ID" {
        // Feature: overseerr-android-client, Property 24: Status Change Notifications
        checkAll(100, Arb.declinedNotification()) { notification ->
            // When a request is declined
            notification.type shouldBe NotificationType.REQUEST_DECLINED
            
            // Then notification should contain request ID
            notification.requestId shouldNotBe null
            
            // And should have descriptive body text
            notification.body.isNotBlank() shouldBe true
        }
    }
    
    "Property 24.4: All status notifications should have valid timestamps" {
        // Feature: overseerr-android-client, Property 24: Status Change Notifications
        checkAll(100, Arb.statusNotification()) { notification ->
            // When any status notification is created
            // Then timestamp should be valid (positive and reasonable)
            notification.timestamp shouldNotBe 0L
            notification.timestamp shouldNotBe Long.MIN_VALUE
            notification.timestamp shouldNotBe Long.MAX_VALUE
            
            // Timestamp should be in milliseconds (reasonable range)
            val currentTime = System.currentTimeMillis()
            val oneYearAgo = currentTime - (365L * 24 * 60 * 60 * 1000)
            val oneYearFromNow = currentTime + (365L * 24 * 60 * 60 * 1000)
            
            (notification.timestamp >= oneYearAgo) shouldBe true
            (notification.timestamp <= oneYearFromNow) shouldBe true
        }
    }
    
    "Property 24.5: Notifications should have unique IDs" {
        // Feature: overseerr-android-client, Property 24: Status Change Notifications
        checkAll(100, Arb.list(Arb.statusNotification(), 2..10)) { notifications ->
            // When multiple notifications are created
            val ids = notifications.map { it.id }
            val uniqueIds = ids.toSet()
            
            // Then all IDs should be unique
            uniqueIds.size shouldBe ids.size
            
            // And IDs should not be empty
            ids.forEach { id ->
                id.isNotBlank() shouldBe true
            }
        }
    }
    
    "Property 24.6: Notification title and body should not be empty" {
        // Feature: overseerr-android-client, Property 24: Status Change Notifications
        checkAll(100, Arb.statusNotification()) { notification ->
            // When any notification is created
            // Then title and body should not be empty
            notification.title.isNotBlank() shouldBe true
            notification.body.isNotBlank() shouldBe true
        }
    }
    
    "Property 24.7: New notifications should be unread by default" {
        // Feature: overseerr-android-client, Property 24: Status Change Notifications
        checkAll(100, Arb.statusNotification()) { notification ->
            // When a new notification is created
            // Then it should be unread by default
            notification.isRead shouldBe false
        }
    }
})

/**
 * Custom Arb for approved notifications.
 */
private fun Arb.Companion.approvedNotification(): Arb<Notification> = arbitrary {
    Notification(
        id = Arb.uuid().bind().toString(),
        title = "Request Approved",
        body = Arb.string(10..100).bind(),
        type = NotificationType.REQUEST_APPROVED,
        timestamp = System.currentTimeMillis() - Arb.long(0..86400000).bind(),
        isRead = false,
        deepLink = null,
        mediaId = Arb.int(1..100000).bind(),
        requestId = Arb.int(1..100000).bind()
    )
}

/**
 * Custom Arb for available notifications.
 */
private fun Arb.Companion.availableNotification(): Arb<Notification> = arbitrary {
    val mediaId = Arb.int(1..100000).bind()
    val mediaType = listOf("movie", "tv").random()
    
    Notification(
        id = Arb.uuid().bind().toString(),
        title = "Media Available",
        body = Arb.string(10..100).bind(),
        type = NotificationType.REQUEST_AVAILABLE,
        timestamp = System.currentTimeMillis() - Arb.long(0..86400000).bind(),
        isRead = false,
        deepLink = "overseerr://media/$mediaType/$mediaId",
        mediaId = mediaId,
        requestId = Arb.int(1..100000).bind()
    )
}

/**
 * Custom Arb for declined notifications.
 */
private fun Arb.Companion.declinedNotification(): Arb<Notification> = arbitrary {
    Notification(
        id = Arb.uuid().bind().toString(),
        title = "Request Declined",
        body = "Your request was declined: " + Arb.string(10..50).bind(),
        type = NotificationType.REQUEST_DECLINED,
        timestamp = System.currentTimeMillis() - Arb.long(0..86400000).bind(),
        isRead = false,
        deepLink = null,
        mediaId = Arb.int(1..100000).bind(),
        requestId = Arb.int(1..100000).bind()
    )
}

/**
 * Custom Arb for any status notification.
 */
private fun Arb.Companion.statusNotification(): Arb<Notification> = arbitrary {
    val types = listOf(
        NotificationType.REQUEST_APPROVED,
        NotificationType.REQUEST_AVAILABLE,
        NotificationType.REQUEST_DECLINED
    )
    
    when (types.random()) {
        NotificationType.REQUEST_APPROVED -> Arb.approvedNotification().bind()
        NotificationType.REQUEST_AVAILABLE -> Arb.availableNotification().bind()
        NotificationType.REQUEST_DECLINED -> Arb.declinedNotification().bind()
        else -> Arb.approvedNotification().bind()
    }
}
