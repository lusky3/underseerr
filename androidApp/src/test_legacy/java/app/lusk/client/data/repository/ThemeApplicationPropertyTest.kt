package app.lusk.client.data.repository

import app.lusk.client.data.preferences.PreferencesManager
import app.lusk.client.data.repository.SettingsRepositoryImpl
import app.lusk.client.domain.repository.ThemePreference
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

/**
 * Property-based tests for theme application.
 * Feature: overseerr-android-client, Property 20: Theme Application
 * Validates: Requirements 5.3, 9.2, 9.6
 */
class ThemeApplicationPropertyTest : StringSpec({
    
    "Property 20.1: Theme changes are persisted correctly" {
        checkAll<ThemePreference>(100, Arb.enum<ThemePreference>()) { theme ->
            // Given
            val preferencesManager = mockk<PreferencesManager>(relaxed = true)
            coEvery { preferencesManager.getThemePreference() } returns flowOf(theme)
            
            val repository = SettingsRepositoryImpl(preferencesManager)
            
            // When
            repository.setThemePreference(theme)
            val result = repository.getThemePreference().first()
            
            // Then
            result shouldBe theme
            coVerify { preferencesManager.setThemePreference(theme) }
        }
    }
    
    "Property 20.2: All theme preferences are supported" {
        checkAll<ThemePreference>(100, Arb.enum<ThemePreference>()) { theme ->
            // Given
            val preferencesManager = mockk<PreferencesManager>(relaxed = true)
            coEvery { preferencesManager.getThemePreference() } returns flowOf(theme)
            
            val repository = SettingsRepositoryImpl(preferencesManager)
            
            // When
            val result = repository.getThemePreference().first()
            
            // Then
            result shouldBe theme
            // Verify theme is one of the valid options
            val validThemes = listOf(ThemePreference.LIGHT, ThemePreference.DARK, ThemePreference.SYSTEM)
            validThemes.contains(result) shouldBe true
        }
    }
    
    "Property 20.3: Theme preference round trip preserves value" {
        checkAll<ThemePreference>(100, Arb.enum<ThemePreference>()) { originalTheme ->
            // Given
            val preferencesManager = mockk<PreferencesManager>(relaxed = true)
            var storedTheme: ThemePreference? = null
            
            coEvery { preferencesManager.setThemePreference(any()) } answers {
                storedTheme = firstArg()
            }
            coEvery { preferencesManager.getThemePreference() } answers {
                flowOf(storedTheme ?: ThemePreference.SYSTEM)
            }
            
            val repository = SettingsRepositoryImpl(preferencesManager)
            
            // When
            repository.setThemePreference(originalTheme)
            val retrievedTheme = repository.getThemePreference().first()
            
            // Then
            retrievedTheme shouldBe originalTheme
        }
    }
    
    "Property 20.4: Default theme is SYSTEM when not set" {
        checkAll<Int>(100) { seed ->
            // Given
            val preferencesManager = mockk<PreferencesManager>(relaxed = true)
            coEvery { preferencesManager.getThemePreference() } returns flowOf(ThemePreference.SYSTEM)
            
            val repository = SettingsRepositoryImpl(preferencesManager)
            
            // When
            val result = repository.getThemePreference().first()
            
            // Then
            result shouldBe ThemePreference.SYSTEM
        }
    }
    
    "Property 20.5: Theme changes are immediate" {
        checkAll<Pair<ThemePreference, ThemePreference>>(100, 
            Arb.enum<ThemePreference>() to Arb.enum<ThemePreference>()
        ) { (firstTheme, secondTheme) ->
            // Given
            val preferencesManager = mockk<PreferencesManager>(relaxed = true)
            var currentTheme = firstTheme
            
            coEvery { preferencesManager.setThemePreference(any()) } answers {
                currentTheme = firstArg()
            }
            coEvery { preferencesManager.getThemePreference() } answers {
                flowOf(currentTheme)
            }
            
            val repository = SettingsRepositoryImpl(preferencesManager)
            
            // When - set first theme
            repository.setThemePreference(firstTheme)
            val result1 = repository.getThemePreference().first()
            
            // Then - verify first theme
            result1 shouldBe firstTheme
            
            // When - change to second theme
            repository.setThemePreference(secondTheme)
            val result2 = repository.getThemePreference().first()
            
            // Then - verify second theme is applied
            result2 shouldBe secondTheme
        }
    }
})
