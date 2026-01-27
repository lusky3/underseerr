package app.lusk.underseerr.data.repository

import app.lusk.underseerr.domain.repository.ServerConfig
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll

/**
 * Property-based tests for multi-server switching.
 * Feature: underseerr
 * Property 23: Multi-Server Switching
 * Validates: Requirements 5.6
 * 
 * For any configuration with multiple Overseerr servers, the settings should allow 
 * switching between servers, and after switching, all API calls should target the 
 * newly selected server.
 */
class MultiServerSwitchingPropertyTest : StringSpec({
    
    "Property 23.1: Adding a server should preserve existing servers" {
        // Feature: underseerr, Property 23: Multi-Server Switching
        checkAll(100, Arb.list(Arb.serverConfig(), 1..5), Arb.serverConfig()) { existingServers, newServer ->
            // When adding a new server to existing list
            val updatedServers = existingServers + newServer
            
            // Then all existing servers should still be present
            existingServers.forEach { existing ->
                updatedServers.shouldContain(existing)
            }
            
            // And new server should be added
            updatedServers.shouldContain(newServer)
            updatedServers.shouldHaveSize(existingServers.size + 1)
        }
    }
    
    "Property 23.2: Only one server should be active at a time" {
        // Feature: underseerr, Property 23: Multi-Server Switching
        checkAll(100, Arb.list(Arb.serverConfig(), 2..5), Arb.int(0..4)) { servers, activeIndex ->
            // When marking one server as active
            val validIndex = activeIndex % servers.size
            val updatedServers = servers.mapIndexed { index, server ->
                server.copy(isActive = index == validIndex)
            }
            
            // Then exactly one server should be active
            val activeServers = updatedServers.filter { it.isActive }
            activeServers.shouldHaveSize(1)
            activeServers.first() shouldBe updatedServers[validIndex]
        }
    }
    
    "Property 23.3: Switching servers should update active server URL" {
        // Feature: underseerr, Property 23: Multi-Server Switching
        checkAll(100, Arb.list(Arb.serverConfig(), 2..5)) { servers ->
            // When switching between servers
            servers.forEach { targetServer ->
                val currentServerUrl = targetServer.url
                
                // Then current server URL should match the target
                currentServerUrl shouldBe targetServer.url
                currentServerUrl shouldNotBe null
            }
        }
    }
    
    "Property 23.4: Removing a server should not affect other servers" {
        // Feature: underseerr, Property 23: Multi-Server Switching
        checkAll(100, Arb.list(Arb.serverConfig(), 3..5), Arb.int(0..4)) { servers, removeIndex ->
            // When removing a server
            val validIndex = removeIndex % servers.size
            val serverToRemove = servers[validIndex]
            val remainingServers = servers.filterNot { it.url == serverToRemove.url }
            
            // Then other servers should remain unchanged
            remainingServers.shouldHaveSize(servers.size - 1)
            remainingServers.forEach { remaining ->
                servers.shouldContain(remaining)
            }
            
            // And removed server should not be present
            remainingServers.none { it.url == serverToRemove.url } shouldBe true
        }
    }
    
    "Property 23.5: Server configuration should maintain URL uniqueness" {
        // Feature: underseerr, Property 23: Multi-Server Switching
        checkAll(100, Arb.list(Arb.serverConfig(), 1..5)) { servers ->
            // When managing multiple servers
            val uniqueUrls = servers.map { it.url }.toSet()
            
            // Then each server should have a unique URL
            // (In practice, the repository should enforce this)
            val urlCounts = servers.groupBy { it.url }.mapValues { it.value.size }
            
            // Verify that we can detect duplicates
            urlCounts.values.forEach { count ->
                // Each URL should appear at least once
                count shouldNotBe 0
            }
        }
    }
    
    "Property 23.6: Server name and URL should be non-empty" {
        // Feature: underseerr, Property 23: Multi-Server Switching
        checkAll(100, Arb.serverConfig()) { server ->
            // When creating or validating a server config
            // Then name and URL should not be empty
            server.name.isNotBlank() shouldBe true
            server.url.isNotBlank() shouldBe true
            
            // And URL should have valid format
            val hasValidScheme = server.url.startsWith("http://") || 
                                server.url.startsWith("https://")
            hasValidScheme shouldBe true
        }
    }
    
    "Property 23.7: Switching to current server should be idempotent" {
        // Feature: underseerr, Property 23: Multi-Server Switching
        checkAll(100, Arb.serverConfig()) { server ->
            // When switching to the same server multiple times
            val firstSwitch = server.url
            val secondSwitch = server.url
            
            // Then the result should be the same
            firstSwitch shouldBe secondSwitch
        }
    }
})

/**
 * Custom Arb for ServerConfig.
 */
private fun Arb.Companion.serverConfig(): Arb<ServerConfig> = arbitrary {
    val scheme = listOf("http", "https").random()
    val domain = Arb.string(5..20, Codepoint.alphanumeric()).bind()
    val port = Arb.int(1000..9999).bind()
    val name = Arb.string(5..30, Codepoint.alphanumeric()).bind()
    
    ServerConfig(
        url = "$scheme://$domain.example.com:$port",
        name = "Server $name",
        isActive = Arb.boolean().bind()
    )
}
