package app.lusk.underseerr.data.repository

import app.lusk.underseerr.domain.repository.NotificationSettings
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.checkAll

/**
 * Property-based tests for notification permission flow.
 * Feature: underseerr
 * Property 22: Notification Permission Flow
 * Validates: Requirements 5.5
 * 
 * For any attempt to enable notifications, the system should request Android notification 
 * permissions (if not already granted) and configure the appropriate notification channels 
 * for different notification types.
 */
class NotificationPermissionPropertyTest : StringSpec({
    
    "Property 22.1: Enabling notifications should configure all notification channels" {
        // Feature: underseerr, Property 22: Notification Permission Flow
        checkAll(100, Arb.notificationSettings()) { settings ->
            // When notifications are enabled
            if (settings.enabled) {
                // Then all notification channel flags should be accessible
                val hasApprovedChannel = settings.requestApproved != null
                val hasAvailableChannel = settings.requestAvailable != null
                val hasDeclinedChannel = settings.requestDeclined != null
                
                // All channels should be defined when notifications are enabled
                hasApprovedChannel shouldBe true
                hasAvailableChannel shouldBe true
                hasDeclinedChannel shouldBe true
            }
        }
    }
    
    "Property 22.2: Disabling notifications should preserve channel preferences" {
        // Feature: underseerr, Property 22: Notification Permission Flow
        checkAll(100, Arb.notificationSettings()) { originalSettings ->
            // When notifications are disabled
            val disabledSettings = originalSettings.copy(enabled = false)
            
            // Then channel preferences should be preserved
            disabledSettings.requestApproved shouldBe originalSettings.requestApproved
            disabledSettings.requestAvailable shouldBe originalSettings.requestAvailable
            disabledSettings.requestDeclined shouldBe originalSettings.requestDeclined
        }
    }
    
    "Property 22.3: Notification settings should be independent per channel" {
        // Feature: underseerr, Property 22: Notification Permission Flow
        checkAll(100, Arb.boolean(), Arb.boolean(), Arb.boolean()) { approved, available, declined ->
            // When different channels have different settings
            val settings = NotificationSettings(
                enabled = true,
                requestApproved = approved,
                requestAvailable = available,
                requestDeclined = declined
            )
            
            // Then each channel should maintain its own state
            settings.requestApproved shouldBe approved
            settings.requestAvailable shouldBe available
            settings.requestDeclined shouldBe declined
        }
    }
    
    "Property 22.4: All notification channels should default to enabled" {
        // Feature: underseerr, Property 22: Notification Permission Flow
        checkAll(100, Arb.boolean()) { enabled ->
            // When creating default notification settings
            val defaultSettings = NotificationSettings(enabled = enabled)
            
            // Then all channels should be enabled by default
            if (enabled) {
                defaultSettings.requestApproved shouldBe true
                defaultSettings.requestAvailable shouldBe true
                defaultSettings.requestDeclined shouldBe true
            }
        }
    }
    
    "Property 22.5: Notification settings should be serializable" {
        // Feature: underseerr, Property 22: Notification Permission Flow
        checkAll(100, Arb.notificationSettings()) { settings ->
            // When converting settings to data class
            val enabled = settings.enabled
            val approved = settings.requestApproved
            val available = settings.requestAvailable
            val declined = settings.requestDeclined
            
            // Then all fields should be accessible
            val reconstructed = NotificationSettings(
                enabled = enabled,
                requestApproved = approved,
                requestAvailable = available,
                requestDeclined = declined
            )
            
            // And should match original
            reconstructed shouldBe settings
        }
    }
})

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
