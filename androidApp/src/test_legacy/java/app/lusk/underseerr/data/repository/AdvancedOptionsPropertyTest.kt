package app.lusk.underseerr.data.repository

import app.lusk.underseerr.data.local.dao.MediaRequestDao
import app.lusk.underseerr.data.remote.SafeApiCall
import app.lusk.underseerr.data.remote.api.ApiQualityProfile
import app.lusk.underseerr.data.remote.api.ApiRootFolder
import app.lusk.underseerr.data.remote.api.RequestApiService
import app.lusk.underseerr.domain.model.Result
import app.lusk.underseerr.domain.repository.QualityProfile
import app.lusk.underseerr.domain.repository.RootFolder
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.mockk

/**
 * Property-based tests for advanced options availability.
 * Feature: underseerr, Property 12: Advanced Options Availability
 * Validates: Requirements 3.6
 */
class AdvancedOptionsPropertyTest : StringSpec({
    
    "Property 12.1: Quality profiles are available when advanced options enabled" {
        checkAll<List<Pair<Int, String>>>(100, Arb.list(Arb.int(1..100) to Arb.string(1..50), 1..10)) { profiles ->
            // Given
            val apiService = mockk<RequestApiService>()
            val dao = mockk<MediaRequestDao>()
            val safeApiCall = SafeApiCall()
            
            val apiProfiles = profiles.map { (id, name) ->
                ApiQualityProfile(id = id, name = name)
            }
            
            coEvery { apiService.getQualityProfiles() } returns apiProfiles
            
            val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
            
            // When
            val result = repository.getQualityProfiles()
            
            // Then
            result.shouldBeInstanceOf<Result.Success<List<QualityProfile>>>()
            val qualityProfiles = (result as Result.Success).data
            qualityProfiles.shouldNotBeEmpty()
            qualityProfiles.size shouldBe profiles.size
        }
    }
    
    "Property 12.2: Each quality profile has unique ID" {
        checkAll<List<Pair<Int, String>>>(100, Arb.list(Arb.int(1..100) to Arb.string(1..50), 2..10)) { profiles ->
            // Given
            val apiService = mockk<RequestApiService>()
            val dao = mockk<MediaRequestDao>()
            val safeApiCall = SafeApiCall()
            
            val uniqueProfiles = profiles.distinctBy { it.first }
            val apiProfiles = uniqueProfiles.map { (id, name) ->
                ApiQualityProfile(id = id, name = name)
            }
            
            coEvery { apiService.getQualityProfiles() } returns apiProfiles
            
            val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
            
            // When
            val result = repository.getQualityProfiles()
            
            // Then
            result.shouldBeInstanceOf<Result.Success<List<QualityProfile>>>()
            val qualityProfiles = (result as Result.Success).data
            
            val ids = qualityProfiles.map { it.id }
            ids.distinct().size shouldBe ids.size
        }
    }
    
    "Property 12.3: Quality profile names are non-empty" {
        checkAll<List<Pair<Int, String>>>(100, Arb.list(Arb.int(1..100) to Arb.string(1..50), 1..10)) { profiles ->
            // Given
            val apiService = mockk<RequestApiService>()
            val dao = mockk<MediaRequestDao>()
            val safeApiCall = SafeApiCall()
            
            val apiProfiles = profiles.map { (id, name) ->
                ApiQualityProfile(id = id, name = name)
            }
            
            coEvery { apiService.getQualityProfiles() } returns apiProfiles
            
            val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
            
            // When
            val result = repository.getQualityProfiles()
            
            // Then
            result.shouldBeInstanceOf<Result.Success<List<QualityProfile>>>()
            val qualityProfiles = (result as Result.Success).data
            
            qualityProfiles.all { it.name.isNotEmpty() } shouldBe true
        }
    }
    
    "Property 12.4: Root folders are available when advanced options enabled" {
        checkAll<List<Pair<String, String>>>(100, Arb.list(Arb.string(1..10) to Arb.string(1..100), 1..10)) { folders ->
            // Given
            val apiService = mockk<RequestApiService>()
            val dao = mockk<MediaRequestDao>()
            val safeApiCall = SafeApiCall()
            
            val apiFolders = folders.map { (id, path) ->
                ApiRootFolder(id = id, path = path)
            }
            
            coEvery { apiService.getRootFolders() } returns apiFolders
            
            val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
            
            // When
            val result = repository.getRootFolders()
            
            // Then
            result.shouldBeInstanceOf<Result.Success<List<RootFolder>>>()
            val rootFolders = (result as Result.Success).data
            rootFolders.shouldNotBeEmpty()
            rootFolders.size shouldBe folders.size
        }
    }
    
    "Property 12.5: Each root folder has unique ID" {
        checkAll<List<Pair<String, String>>>(100, Arb.list(Arb.string(1..10) to Arb.string(1..100), 2..10)) { folders ->
            // Given
            val apiService = mockk<RequestApiService>()
            val dao = mockk<MediaRequestDao>()
            val safeApiCall = SafeApiCall()
            
            val uniqueFolders = folders.distinctBy { it.first }
            val apiFolders = uniqueFolders.map { (id, path) ->
                ApiRootFolder(id = id, path = path)
            }
            
            coEvery { apiService.getRootFolders() } returns apiFolders
            
            val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
            
            // When
            val result = repository.getRootFolders()
            
            // Then
            result.shouldBeInstanceOf<Result.Success<List<RootFolder>>>()
            val rootFolders = (result as Result.Success).data
            
            val ids = rootFolders.map { it.id }
            ids.distinct().size shouldBe ids.size
        }
    }
    
    "Property 12.6: Root folder paths are non-empty" {
        checkAll<List<Pair<String, String>>>(100, Arb.list(Arb.string(1..10) to Arb.string(1..100), 1..10)) { folders ->
            // Given
            val apiService = mockk<RequestApiService>()
            val dao = mockk<MediaRequestDao>()
            val safeApiCall = SafeApiCall()
            
            val apiFolders = folders.map { (id, path) ->
                ApiRootFolder(id = id, path = path)
            }
            
            coEvery { apiService.getRootFolders() } returns apiFolders
            
            val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
            
            // When
            val result = repository.getRootFolders()
            
            // Then
            result.shouldBeInstanceOf<Result.Success<List<RootFolder>>>()
            val rootFolders = (result as Result.Success).data
            
            rootFolders.all { it.path.isNotEmpty() } shouldBe true
        }
    }
})
