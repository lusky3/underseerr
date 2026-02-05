package app.lusk.underseerr.domain.repository

import androidx.paging.PagingData
import app.lusk.underseerr.domain.model.MediaType
import app.lusk.underseerr.domain.model.Result
import app.lusk.underseerr.domain.model.SearchResult
import kotlinx.coroutines.flow.Flow

interface WatchlistRepository {
    fun getWatchlist(): Flow<PagingData<SearchResult>>
    suspend fun getWatchlistIds(): Result<Set<Int>>
    suspend fun addToWatchlist(tmdbId: Int, mediaType: MediaType, ratingKey: String?): Result<Unit>
    suspend fun removeFromWatchlist(tmdbId: Int, mediaType: MediaType, ratingKey: String?): Result<Unit>
}
