package app.lusk.client.data.remote.api

import app.lusk.client.data.remote.model.ApiMovie
import app.lusk.client.data.remote.model.ApiSearchResults
import app.lusk.client.data.remote.model.ApiTvShow
import app.lusk.client.data.remote.model.ApiGenre
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * Ktor implementation of media discovery endpoints.
 */
class DiscoveryKtorService(private val client: HttpClient) {
    
    suspend fun getTrending(page: Int = 1, language: String = "en"): ApiSearchResults {
        return client.get("/api/v1/discover/trending") {
            parameter("page", page)
            parameter("language", language)
        }.body()
    }
    
    suspend fun getTrendingMovies(page: Int = 1, language: String = "en"): ApiSearchResults {
        return client.get("/api/v1/discover/movies") {
            parameter("page", page)
            parameter("language", language)
        }.body()
    }
    
    suspend fun getTrendingTvShows(page: Int = 1, language: String = "en"): ApiSearchResults {
        return client.get("/api/v1/discover/tv") {
            parameter("page", page)
            parameter("language", language)
        }.body()
    }

    suspend fun getUpcomingMovies(page: Int = 1, language: String = "en"): ApiSearchResults {
        return client.get("/api/v1/discover/movies/upcoming") {
            parameter("page", page)
            parameter("language", language)
        }.body()
    }

    suspend fun getUpcomingTvShows(page: Int = 1, language: String = "en"): ApiSearchResults {
        return client.get("/api/v1/discover/tv/upcoming") {
            parameter("page", page)
            parameter("language", language)
        }.body()
    }

    suspend fun getPopularMovies(page: Int = 1, language: String = "en"): ApiSearchResults {
        return client.get("/api/v1/discover/movies") {
            parameter("page", page)
            parameter("language", language)
        }.body()
    }

    suspend fun getPopularTvShows(page: Int = 1, language: String = "en"): ApiSearchResults {
        return client.get("/api/v1/discover/tv") {
            parameter("page", page)
            parameter("language", language)
        }.body()
    }

    suspend fun getWatchlist(page: Int = 1): ApiSearchResults {
        return client.get("/api/v1/discover/watchlist") {
            parameter("page", page)
        }.body()
    }

    suspend fun getMovieGenres(language: String = "en"): List<ApiGenre> {
        return client.get("/api/v1/genres/movie") {
            parameter("language", language)
        }.body()
    }

    suspend fun getTvGenres(language: String = "en"): List<ApiGenre> {
        return client.get("/api/v1/genres/tv") {
            parameter("language", language)
        }.body()
    }

    suspend fun getMoviesByGenre(genreId: Int, page: Int = 1, language: String = "en"): ApiSearchResults {
        return client.get("/api/v1/discover/movies/genre/$genreId") {
            parameter("page", page)
            parameter("language", language)
        }.body()
    }

    suspend fun getTvByGenre(genreId: Int, page: Int = 1, language: String = "en"): ApiSearchResults {
        return client.get("/api/v1/discover/tv/genre/$genreId") {
            parameter("page", page)
            parameter("language", language)
        }.body()
    }

    suspend fun getStudioDetails(studioId: Int, page: Int = 1, language: String = "en"): ApiSearchResults {
        return client.get("/api/v1/discover/movies/studio/$studioId") {
            parameter("page", page)
            parameter("language", language)
        }.body()
    }

    suspend fun getNetworkDetails(networkId: Int, page: Int = 1, language: String = "en"): ApiSearchResults {
        return client.get("/api/v1/discover/tv/network/$networkId") {
            parameter("page", page)
            parameter("language", language)
        }.body()
    }
    
    suspend fun search(query: String, page: Int = 1, language: String = "en"): ApiSearchResults {
        return client.get("/api/v1/search") {
            parameter("query", query)
            parameter("page", page)
            parameter("language", language)
        }.body()
    }
    
    suspend fun getMovieDetails(movieId: Int, language: String = "en"): ApiMovie {
        return client.get("/api/v1/movie/$movieId") {
            parameter("language", language)
        }.body()
    }
    
    suspend fun getTvShowDetails(tvId: Int, language: String = "en"): ApiTvShow {
        return client.get("/api/v1/tv/$tvId") {
            parameter("language", language)
        }.body()
    }
}
