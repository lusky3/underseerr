package app.lusk.underseerr.util

import io.kotest.core.spec.style.StringSpec
import io.mockk.*
import io.sentry.Sentry
import io.sentry.IMetricsAggregator

class MetricsTest : StringSpec({
    
    beforeTest {
        mockkStatic(Sentry::class)
        val mockAggregator = mockk<IMetricsAggregator>(relaxed = true)
        every { Sentry.metrics() } returns mockAggregator
    }
    
    afterTest {
        unmockkAll()
    }
    
    "trackServerSetup increments server.setup metric" {
        Metrics.trackServerSetup()
        
        verify { Sentry.metrics().increment("server.setup") }
    }
    
    "trackLogin increments auth.login with method tag" {
        Metrics.trackLogin("plex")
        
        verify { Sentry.metrics().increment("auth.login", tags = mapOf("method" to "plex")) }
    }
    
    "trackMediaRequest increments media.request with type tag" {
        Metrics.trackMediaRequest("movie")
        
        verify { Sentry.metrics().increment("media.request", tags = mapOf("type" to "movie")) }
    }
    
    "trackSearch increments search.performed metric" {
        Metrics.trackSearch()
        
        verify { Sentry.metrics().increment("search.performed") }
    }
    
    "trackIssueReported increments issue.reported metric" {
        Metrics.trackIssueReported()
        
        verify { Sentry.metrics().increment("issue.reported") }
    }
    
    "trackApiResponseTime records distribution with endpoint tag" {
        Metrics.trackApiResponseTime("/api/request", 150L)
        
        verify { 
            Sentry.metrics().distribution(
                "api.response_time", 
                150.0, 
                tags = mapOf("endpoint" to "/api/request")
            ) 
        }
    }
    
    "trackActiveRequests sets gauge value" {
        Metrics.trackActiveRequests(5)
        
        verify { Sentry.metrics().gauge("requests.active", 5.0) }
    }
})
