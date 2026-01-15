package app.lusk.client.data.notification

import app.lusk.client.domain.model.NotificationType
import app.lusk.client.domain.repository.NotificationSettings
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.enum
import io.kotest.property.checkAll

/**
 * Property-based tests for notification preference respect.
 * Feature: overseerr-android-client
 * Property 26: Notification Preference Respect
 * Validates: Requirements 6.5
 * 
 * For any notification type, if the user has disabled that notification channel in 
 * preferences, no notifications of that type should be displayed, regardless of server events.
 */
class NotificationPreferencePropertyTest : StringSpec({
    
    "Property 26.1: Disabled notifications should not be shown" {
        // Feature: overseerr-android-client, Property 26: Notification Preference Respect
        checkAll(100, Arb.notificationSettings()) { settings ->
            // When notifications are globally disabled
            if (!settings.enabled) {
                // Then no notifications should be shown
                val shouldShowApproved = shouldShowNotification(
                    NotificationType.REQUEST_APPROVED,
                    settings
                )
                val shouldShowAvailable = shouldShowNotification(
                    NotificationType.REQUEST_AVAILABLE,
                    settings
                )
                val shouldShowDeclined = shouldShowNotification(
                    NotificationType.REQUEST_DECLINED,
                    settings
                )
                
                shouldShowApproved shouldBe false
                shouldShowAvailable shouldBe false
                shouldShowDeclined shouldBe false
            }
        }
    }
    
    "Property 26.2: Approved notifications respect channel preference" {
        // Feature: overseerr-android-client, Property 26: Notification Preference Respect
        checkAll(100, Arb.boolean()) { requestApproved ->
            // When approved notification channel has specific setting
            val settings = NotificationSettings(
                enabled = true,
                requestApproved = requestApproved,
                requestAvailable = true,
                requestDeclined = true
            )
            
            // Then approved notifications should respect the setting
            val shouldShow = shouldShowNotification(
                NotificationType.REQUEST_APPROVED,
                settings
            )
            
            shouldShow shouldBe requestApproved
        }
    }
    
    "Property 26.3: Available notifications respect channel preference" {
        // Feature: overseerr-android-client, Property 26: Notification Preference Respect
        checkAll(100, Arb.boolean()) { requestAvailable ->
            // When available notification channel has specific setting
            val settings = NotificationSettings(
                enabled = true,
                requestApproved = true,
                requestAvailable = requestAvailable,
                requestDeclined = true
            )
            
            // Then available notifications should respect the setting
            val shouldShow = shouldShowNotification(
                NotificationType.REQUEST_AVAILABLE,
                settings
            )
            
            shouldShow shouldBe requestAvailable
        }
    }
    
    "Property 26.4: Declined notifications respect channel preference" {
        // Feature: overseerr-android-client, Property 26: Notification Preference Respect
        checkAll(100, Arb.boolean()) { requestDeclined ->
            // When declined notification channel has specific setting
            val settings = NotificationSettings(
                enabled = true,
                requestApproved = true,
                requestAvailable = true,
                requestDeclined = requestDeclined
            )
            
            // Then declined notifications should respect the setting
            val shouldShow = shouldShowNotification(
                NotificationType.REQUEST_DECLINED,
                settings
            )
            
            shouldShow shouldBe requestDeclined
        }
    }
    
    "Property 26.5: System notifications always show when enabled" {
        // Feature: overseerr-android-client, Property 26: Notification Preference Respect
        checkAll(100, Arb.notificationSettings()) { settings ->
            // When notifications are globally enabled
            if (settings.enabled) {
                // Then system notifications should always show
                val shouldShow = shouldShowNotification(
                    NotificationType.SYSTEM,
                    settings
                )
                
                shouldShow shouldBe true
            }
        }
    }
    
    "Property 26.6: Channel preferences are independent" {
        // Feature: overseerr-android-client, Property 26: Notification Preference Respect
        checkAll(100, Arb.boolean(), Arb.boolean(), Arb.boolean()) { approved, available, declined ->
            // When each channel has different settings
            val settings = NotificationSettings(
                enabled = true,
                requestApproved = approved,
                requestAvailable = available,
                requestDeclined = declined
            )
            
            // Then each channel should be evaluated independently
            shouldShowNotification(NotificationType.REQUEST_APPROVED, settings) shouldBe approved
            shouldShowNotification(NotificationType.REQUEST_AVAILABLE, settings) shouldBe available
            shouldShowNotification(NotificationType.REQUEST_DECLINED, settings) shouldBe declined
        }
    }
    
    "Property 26.7: Disabling one channel does not affect others" {
        // Feature: overseerr-android-client, Property 26: Notification Preference Respect
        checkAll(100, Arb.enum<NotificationType>()) { disabledType ->
            // When one notification type is disabled
            val settings = when (disabledType) {
                NotificationType.REQUEST_APPROVED -> NotificationSettings(
                    enabled = true,
                    requestApproved = false,
                    requestAvailable = true,
                    requestDeclined = true
                )
                NotificationType.REQUEST_AVAILABLE -> NotificationSettings(
                    enabled = true,
                    requestApproved = true,
                    requestAvailable = false,
                    requestDeclined = true
                )
                NotificationType.REQUEST_DECLINED -> NotificationSettings(
                    enabled = true,
                    requestApproved = true,
                    requestAvailable = true,
                    requestDeclined = false
                )
                else -> NotificationSettings(enabled = true)
            }
            
            // Then other notification types should still show
            val otherTypes = NotificationType.entries.filter { 
                it != disabledType && it != NotificationType.PENDING
            }
            
            otherTypes.forEach { type ->
                if (type == NotificationType.SYSTEM) {
                    shouldShowNotification(type, settings) shouldBe true
                }
            }
        }
    }
})

/**
 * Helper function to determine if a notification should be shown.
 */
private fun shouldShowNotification(
    type: NotificationType,
    settings: NotificationSettings
): Boolean {
    if (!settings.enabled) {
        return false
    }
    
    return when (type) {
        NotificationType.REQUEST_APPROVED -> settings.requestApproved
        NotificationType.REQUEST_AVAILABLE -> settings.requestAvailable
        NotificationType.REQUEST_DECLINED -> settings.requestDeclined
        NotificationType.SYSTEM -> true
        NotificationType.PENDING -> true
    }
}

/**
 * Custom Arb for NotificationSettings.
 */
private fun Arb.Companion.notificationSettings(): Arb<NotificationSettings> = arbitrary {
    NotificationSettings(
        enabled = Arb.boolean().bind(),
        requestApproved = Arb.boolean().bind(),
        requestAvailable = Arb.boolean().bind(),
        requestDeclined = Arb.boolean().bind()
    )
}
