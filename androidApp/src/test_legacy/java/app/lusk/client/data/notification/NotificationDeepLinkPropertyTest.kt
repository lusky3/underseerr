package app.lusk.client.data.notification

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * Property-based tests for notification deep link navigation.
 * Feature: overseerr-android-client
 * Property 25: Notification Deep Link Navigation
 * Validates: Requirements 6.4
 * 
 * For any notification that is tapped, the system should parse the deep link and 
 * navigate to the relevant screen (media details, request details, etc.) within the app.
 */
class NotificationDeepLinkPropertyTest : StringSpec({
    
    "Property 25.1: Media deep links should parse correctly" {
        // Feature: overseerr-android-client, Property 25: Notification Deep Link Navigation
        checkAll(100, Arb.int(1..100000), Arb.mediaType()) { mediaId, mediaType ->
            // When creating a media deep link
            val deepLink = "overseerr://media/$mediaType/$mediaId"
            
            // Then it should be parseable
            val handler = NotificationHandler(mockContext())
            val info = handler.parseDeepLink(deepLink)
            
            info shouldNotBe null
            info.shouldBeInstanceOf<DeepLinkInfo.MediaDetails>()
            
            val mediaInfo = info as DeepLinkInfo.MediaDetails
            mediaInfo.mediaId shouldBe mediaId
            mediaInfo.mediaType shouldBe mediaType
        }
    }
    
    "Property 25.2: Request deep links should parse correctly" {
        // Feature: overseerr-android-client, Property 25: Notification Deep Link Navigation
        checkAll(100, Arb.int(1..100000)) { requestId ->
            // When creating a request deep link
            val deepLink = "overseerr://request/$requestId"
            
            // Then it should be parseable
            val handler = NotificationHandler(mockContext())
            val info = handler.parseDeepLink(deepLink)
            
            info shouldNotBe null
            info.shouldBeInstanceOf<DeepLinkInfo.RequestDetails>()
            
            val requestInfo = info as DeepLinkInfo.RequestDetails
            requestInfo.requestId shouldBe requestId
        }
    }
    
    "Property 25.3: Deep link creation should be reversible" {
        // Feature: overseerr-android-client, Property 25: Notification Deep Link Navigation
        checkAll(100, Arb.int(1..100000), Arb.mediaType()) { mediaId, mediaType ->
            // When creating and parsing a media deep link
            val handler = NotificationHandler(mockContext())
            val deepLink = handler.createMediaDeepLink(mediaId, mediaType)
            val info = handler.parseDeepLink(deepLink)
            
            // Then the parsed info should match the original
            info shouldNotBe null
            info.shouldBeInstanceOf<DeepLinkInfo.MediaDetails>()
            
            val mediaInfo = info as DeepLinkInfo.MediaDetails
            mediaInfo.mediaId shouldBe mediaId
            mediaInfo.mediaType shouldBe mediaType
        }
    }
    
    "Property 25.4: Request deep link creation should be reversible" {
        // Feature: overseerr-android-client, Property 25: Notification Deep Link Navigation
        checkAll(100, Arb.int(1..100000)) { requestId ->
            // When creating and parsing a request deep link
            val handler = NotificationHandler(mockContext())
            val deepLink = handler.createRequestDeepLink(requestId)
            val info = handler.parseDeepLink(deepLink)
            
            // Then the parsed info should match the original
            info shouldNotBe null
            info.shouldBeInstanceOf<DeepLinkInfo.RequestDetails>()
            
            val requestInfo = info as DeepLinkInfo.RequestDetails
            requestInfo.requestId shouldBe requestId
        }
    }
    
    "Property 25.5: Invalid deep links should return null" {
        // Feature: overseerr-android-client, Property 25: Notification Deep Link Navigation
        checkAll(100, Arb.invalidDeepLink()) { invalidLink ->
            // When parsing an invalid deep link
            val handler = NotificationHandler(mockContext())
            val info = handler.parseDeepLink(invalidLink)
            
            // Then it should return null
            info shouldBe null
        }
    }
    
    "Property 25.6: Deep links should have correct scheme" {
        // Feature: overseerr-android-client, Property 25: Notification Deep Link Navigation
        checkAll(100, Arb.int(1..100000), Arb.mediaType()) { mediaId, mediaType ->
            // When creating a deep link
            val handler = NotificationHandler(mockContext())
            val deepLink = handler.createMediaDeepLink(mediaId, mediaType)
            
            // Then it should start with the correct scheme
            deepLink.startsWith("overseerr://") shouldBe true
        }
    }
    
    "Property 25.7: Deep links should not contain invalid characters" {
        // Feature: overseerr-android-client, Property 25: Notification Deep Link Navigation
        checkAll(100, Arb.int(1..100000), Arb.mediaType()) { mediaId, mediaType ->
            // When creating a deep link
            val handler = NotificationHandler(mockContext())
            val deepLink = handler.createMediaDeepLink(mediaId, mediaType)
            
            // Then it should not contain spaces or invalid characters
            deepLink.contains(" ") shouldBe false
            deepLink.contains("\n") shouldBe false
            deepLink.contains("\t") shouldBe false
        }
    }
})

/**
 * Custom Arb for media types.
 */
private fun Arb.Companion.mediaType(): Arb<String> = arbitrary {
    listOf("movie", "tv").random()
}

/**
 * Custom Arb for invalid deep links.
 */
private fun Arb.Companion.invalidDeepLink(): Arb<String> = arbitrary {
    listOf(
        "http://example.com",
        "overseerr://invalid",
        "overseerr://media",
        "overseerr://media/movie",
        "overseerr://media/movie/abc",
        "overseerr://request",
        "overseerr://request/abc",
        "",
        "invalid",
        Arb.string(1..50).bind()
    ).random()
}

/**
 * Mock context for testing.
 */
private fun mockContext(): android.content.Context {
    return androidx.test.core.app.ApplicationProvider.getApplicationContext()
}
