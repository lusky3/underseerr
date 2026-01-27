package app.lusk.underseerr.mock

import app.lusk.underseerr.data.remote.model.*

/**
 * Provides realistic mock response data for Overseerr API endpoints.
 * All content is fictional and created for promotional screenshots.
 */
object MockResponses {
    
    // ========================================================================
    // FICTIONAL CONTENT DATA
    // These are completely made-up titles for promotional screenshots
    // ========================================================================
    
    private val fakeMovies = listOf(
        FakeMedia(
            id = 1001,
            title = "Neon Horizons",
            overview = "In a futuristic city where technology and humanity collide, a rogue AI gains consciousness and must choose between its programmed directives and its newfound sense of morality. A visually stunning sci-fi epic.",
            year = "2025",
            rating = 8.7,
            status = 5 // Available
        ),
        FakeMedia(
            id = 1002,
            title = "The Last Cartographer",
            overview = "An aging mapmaker discovers that the mysterious islands he's been charting for decades are actually gateways to parallel dimensions. His final expedition becomes the adventure of a lifetime.",
            year = "2024",
            rating = 8.2,
            status = 5 // Available
        ),
        FakeMedia(
            id = 1003,
            title = "Midnight at the Velvet Club",
            overview = "A noir thriller set in 1940s Chicago where a jazz pianist uncovers a citywide conspiracy after witnessing a murder at the legendary Velvet Club. Featuring an original score by Grammy-winning artists.",
            year = "2025",
            rating = 7.9,
            status = 2 // Pending
        ),
        FakeMedia(
            id = 1004,
            title = "Echoes of Tomorrow",
            overview = "After a devastating solar storm erases all digital data on Earth, humanity must rebuild society from memory. A powerful drama about connection, loss, and the resilience of the human spirit.",
            year = "2024",
            rating = 8.5,
            status = 5 // Available
        ),
        FakeMedia(
            id = 1005,
            title = "The Quantum Thief",
            overview = "The world's greatest heist team attempts to steal a revolutionary quantum computer from a floating fortress in the Arctic. But when the device activates, reality itself becomes unstable.",
            year = "2025",
            rating = 8.1,
            status = 2 // Pending
        ),
        FakeMedia(
            id = 1006,
            title = "Garden of Stars",
            overview = "A botanist on a generation ship discovers sentient plants that communicate through bioluminescence. As relationships form across species, she must protect them from those who see only profit.",
            year = "2024",
            rating = 7.8,
            status = 1 // Unknown/Requested
        ),
        FakeMedia(
            id = 1007,
            title = "The Crimson Protocol",
            overview = "When a cyber attack threatens global infrastructure, a disgraced former hacker is recruited for one last mission. The catch: the code was written by her missing sister.",
            year = "2025",
            rating = 8.3,
            status = 5 // Available
        ),
        FakeMedia(
            id = 1008,
            title = "Whispers in the Aurora",
            overview = "In northern Iceland, a deaf scientist develops technology to 'hear' the aurora borealis, accidentally intercepting messages from an ancient civilization living in Earth's magnetosphere.",
            year = "2024",
            rating = 8.6,
            status = 5 // Available
        ),
        FakeMedia(
            id = 1009,
            title = "The Memory Merchant",
            overview = "In a world where memories can be bought and sold, a young woman discovers her most precious memories were stolen at birth. Her journey to reclaim them reveals dark secrets about her true identity.",
            year = "2025",
            rating = 7.7,
            status = 2 // Pending
        ),
        FakeMedia(
            id = 1010,
            title = "Titanfall Legacy",
            overview = "The aging pilot of a legendary mech must train a new generation of warriors when an alien armada threatens Earth. An action-packed tribute to classic sci-fi with groundbreaking visual effects.",
            year = "2024",
            rating = 8.4,
            status = 1 // Unknown/Requested
        ),
        FakeMedia(
            id = 1011,
            title = "Beneath Copper Skies",
            overview = "On a terraformed Mars colony, a detective investigates a series of impossible crimes that seem to violate the laws of physics. The truth challenges everything humanity knows about reality.",
            year = "2025",
            rating = 8.0,
            status = 5 // Available
        ),
        FakeMedia(
            id = 1012,
            title = "The Infinite Library",
            overview = "A librarian discovers a book that rewrites itself based on who reads it. When the book begins predicting disasters, she must race to understand its secrets before time runs out.",
            year = "2024",
            rating = 7.6,
            status = 2 // Pending
        )
    )
    
    private val fakeTvShows = listOf(
        FakeMedia(
            id = 2001,
            title = "Chronicles of the Void",
            overview = "A crew of explorers ventures beyond known space to investigate signals from an impossible source – a star that died millions of years ago. Epic space opera spanning multiple seasons.",
            year = "2023",
            rating = 9.1,
            status = 5,
            seasons = 3
        ),
        FakeMedia(
            id = 2002,
            title = "The Alchemist's Daughter",
            overview = "In a Victorian London where alchemy is real, a young woman inherits her father's forbidden laboratory and discovers she has the power to transmute not just metals, but time itself.",
            year = "2024",
            rating = 8.8,
            status = 5,
            seasons = 2
        ),
        FakeMedia(
            id = 2003,
            title = "Harbor City",
            overview = "A gritty crime drama following three families whose fates intertwine in a corrupt coastal city. Praised for its complex characters and unpredictable plot twists.",
            year = "2022",
            rating = 8.9,
            status = 5,
            seasons = 4
        ),
        FakeMedia(
            id = 2004,
            title = "The Dreamweavers",
            overview = "Therapists with the ability to enter patients' dreams discover a dark entity lurking in the collective unconscious. Psychological thriller meets supernatural horror.",
            year = "2024",
            rating = 8.5,
            status = 2,
            seasons = 1
        ),
        FakeMedia(
            id = 2005,
            title = "Stellar Academy",
            overview = "Young cadets from across the galaxy train at an elite space academy, facing not just academic challenges but an ancient threat awakening at the galaxy's edge.",
            year = "2023",
            rating = 8.2,
            status = 5,
            seasons = 2
        ),
        FakeMedia(
            id = 2006,
            title = "The Cipher",
            overview = "When encrypted messages start appearing in Renaissance paintings, a team of historians and cryptographers uncover a conspiracy that has shaped world events for centuries.",
            year = "2024",
            rating = 8.7,
            status = 5,
            seasons = 1
        ),
        FakeMedia(
            id = 2007,
            title = "Frostbound",
            overview = "After a cataclysmic event freezes the northern hemisphere, survivors in a underground city face dwindling resources and rising tensions. Tense survival drama with supernatural elements.",
            year = "2023",
            rating = 8.4,
            status = 2,
            seasons = 2
        ),
        FakeMedia(
            id = 2008,
            title = "The Syndicate Files",
            overview = "An investigative journalist infiltrates a powerful tech conglomerate and discovers they're developing technology that could either save humanity or end it.",
            year = "2024",
            rating = 8.3,
            status = 1,
            seasons = 1
        ),
        FakeMedia(
            id = 2009,
            title = "Legends of Ashenvale",
            overview = "An epic fantasy series following five kingdoms on the brink of war as an ancient prophecy begins to unfold. Rich world-building and stunning visual effects.",
            year = "2022",
            rating = 9.0,
            status = 5,
            seasons = 3
        ),
        FakeMedia(
            id = 2010,
            title = "Pulse",
            overview = "A medical drama set in the world's most advanced hospital, where cutting-edge technology saves lives while raising ethical questions about the future of medicine.",
            year = "2024",
            rating = 8.1,
            status = 5,
            seasons = 2
        )
    )
    
    private val fakeUsers = listOf(
        FakeUser(id = 1, name = "Admin User", email = "admin@example.com", avatar = "/avatar/admin.jpg", isAdmin = true),
        FakeUser(id = 2, name = "Sarah Chen", email = "sarah.c@example.com", avatar = "/avatar/sarah.jpg"),
        FakeUser(id = 3, name = "Marcus Johnson", email = "marcus.j@example.com", avatar = "/avatar/marcus.jpg"),
        FakeUser(id = 4, name = "Emily Rodriguez", email = "emily.r@example.com", avatar = "/avatar/emily.jpg"),
        FakeUser(id = 5, name = "James Wilson", email = "james.w@example.com", avatar = "/avatar/james.jpg"),
        FakeUser(id = 6, name = "Olivia Park", email = "olivia.p@example.com", avatar = "/avatar/olivia.jpg")
    )
    
    private data class FakeMedia(
        val id: Int,
        val title: String,
        val overview: String,
        val year: String,
        val rating: Double,
        val status: Int,
        val seasons: Int = 0
    )
    
    private data class FakeUser(
        val id: Int,
        val name: String,
        val email: String,
        val avatar: String,
        val isAdmin: Boolean = false
    )
    
    // ========================================================================
    // AUTH RESPONSES
    // ========================================================================
    
    fun authResponse() = ApiAuthResponse(
        apiKey = "test-api-key-12345",
        userId = 1
    )
    
    fun userProfile(userId: Int = 1): ApiUserProfile {
        val user = fakeUsers.find { it.id == userId } ?: fakeUsers[0]
        return ApiUserProfile(
            id = user.id,
            email = user.email,
            displayName = user.name,
            avatar = user.avatar,
            requestCount = 15 + (userId * 3),
            permissions = if (user.isAdmin) 2L else 32L
        )
    }
    
    fun serverInfo() = ApiServerInfo(
        version = "1.33.2",
        initialized = true,
        applicationUrl = "http://localhost:5055"
    )
    
    // ========================================================================
    // DISCOVERY RESPONSES - Using Fake Content
    // ========================================================================
    
    fun searchResults(page: Int = 1, query: String = ""): ApiSearchResults {
        val allMedia = fakeMovies.mapIndexed { index, movie ->
            ApiSearchResult(
                id = movie.id,
                mediaType = "movie",
                title = movie.title,
                name = null,
                overview = movie.overview,
                posterPath = "/poster/movie_${movie.id}.jpg",
                releaseDate = "${movie.year}-06-15",
                firstAirDate = null,
                voteAverage = movie.rating
            )
        } + fakeTvShows.mapIndexed { index, show ->
            ApiSearchResult(
                id = show.id,
                mediaType = "tv",
                title = null,
                name = show.title,
                overview = show.overview,
                posterPath = "/poster/tv_${show.id}.jpg",
                releaseDate = null,
                firstAirDate = "${show.year}-01-15",
                voteAverage = show.rating
            )
        }
        
        val filtered = if (query.isNotEmpty()) {
            allMedia.filter { 
                (it.title ?: it.name ?: "").contains(query, ignoreCase = true) ||
                (it.overview ?: "").contains(query, ignoreCase = true)
            }
        } else allMedia.shuffled()
        
        val pageSize = 20
        val startIndex = (page - 1) * pageSize
        val endIndex = minOf(startIndex + pageSize, filtered.size)
        val pageResults = if (startIndex < filtered.size) filtered.subList(startIndex, endIndex) else emptyList()
        
        return ApiSearchResults(
            page = page,
            totalPages = (filtered.size + pageSize - 1) / pageSize,
            totalResults = filtered.size,
            results = pageResults
        )
    }
    
    fun movieSearchResults(page: Int = 1): ApiSearchResults {
        val pageSize = 20
        val startIndex = (page - 1) * pageSize
        val endIndex = minOf(startIndex + pageSize, fakeMovies.size)
        
        val results = fakeMovies.subList(
            startIndex.coerceIn(0, fakeMovies.size),
            endIndex.coerceIn(0, fakeMovies.size)
        ).map { movie ->
            ApiSearchResult(
                id = movie.id,
                mediaType = "movie",
                title = movie.title,
                name = null,
                overview = movie.overview,
                posterPath = "/poster/movie_${movie.id}.jpg",
                releaseDate = "${movie.year}-06-15",
                firstAirDate = null,
                voteAverage = movie.rating
            )
        }
        
        return ApiSearchResults(
            page = page,
            totalPages = (fakeMovies.size + pageSize - 1) / pageSize,
            totalResults = fakeMovies.size,
            results = results
        )
    }
    
    fun tvShowSearchResults(page: Int = 1): ApiSearchResults {
        val pageSize = 20
        val startIndex = (page - 1) * pageSize
        val endIndex = minOf(startIndex + pageSize, fakeTvShows.size)
        
        val results = fakeTvShows.subList(
            startIndex.coerceIn(0, fakeTvShows.size),
            endIndex.coerceIn(0, fakeTvShows.size)
        ).map { show ->
            ApiSearchResult(
                id = show.id,
                mediaType = "tv",
                title = null,
                name = show.title,
                overview = show.overview,
                posterPath = "/poster/tv_${show.id}.jpg",
                releaseDate = null,
                firstAirDate = "${show.year}-01-15",
                voteAverage = show.rating
            )
        }
        
        return ApiSearchResults(
            page = page,
            totalPages = (fakeTvShows.size + pageSize - 1) / pageSize,
            totalResults = fakeTvShows.size,
            results = results
        )
    }
    
    fun movieDetails(movieId: Int): ApiMovie {
        val movie = fakeMovies.find { it.id == movieId } ?: fakeMovies[0]
        return ApiMovie(
            id = movie.id,
            title = movie.title,
            overview = movie.overview,
            posterPath = "/poster/movie_${movie.id}.jpg",
            backdropPath = "/backdrop/movie_${movie.id}.jpg",
            releaseDate = "${movie.year}-06-15",
            voteAverage = movie.rating,
            mediaInfo = ApiMediaInfo(
                status = movie.status,
                requestId = if (movie.status == 2 || movie.status == 5) movie.id else null,
                available = movie.status == 5
            )
        )
    }
    
    fun tvShowDetails(tvId: Int): ApiTvShow {
        val show = fakeTvShows.find { it.id == tvId } ?: fakeTvShows[0]
        return ApiTvShow(
            id = show.id,
            name = show.title,
            overview = show.overview,
            posterPath = "/poster/tv_${show.id}.jpg",
            backdropPath = "/backdrop/tv_${show.id}.jpg",
            firstAirDate = "${show.year}-01-15",
            voteAverage = show.rating,
            numberOfSeasons = show.seasons,
            mediaInfo = ApiMediaInfo(
                status = show.status,
                requestId = if (show.status == 2 || show.status == 5) show.id else null,
                available = show.status == 5
            )
        )
    }
    
    // ========================================================================
    // REQUEST RESPONSES - Using Fake Content
    // ========================================================================
    
    fun requestResponse(requestId: Int = 1) = ApiRequestResponse(
        id = requestId,
        status = 1,
        media = ApiMediaInfo(
            status = 1,
            requestId = requestId,
            available = false
        )
    )
    
    fun mediaRequest(requestId: Int): ApiMediaRequest {
        val isMovie = requestId % 2 == 0
        val mediaList = if (isMovie) fakeMovies else fakeTvShows
        val mediaIndex = (requestId - 1) % mediaList.size
        val media = mediaList[mediaIndex]
        val user = fakeUsers[(requestId - 1) % fakeUsers.size]
        
        val status = when (requestId % 5) {
            0 -> 1 // Pending Approval
            1 -> 2 // Approved
            2 -> 3 // Declined
            3 -> 4 // Processing
            else -> 5 // Available
        }
        
        return ApiMediaRequest(
            id = requestId,
            type = if (isMovie) "movie" else "tv",
            status = status,
            createdAt = "2025-01-${(requestId % 28) + 1}T${10 + (requestId % 12)}:00:00.000Z",
            media = ApiRequestMedia(
                mediaType = if (isMovie) "movie" else "tv",
                tmdbId = media.id,
                status = status,
                id = media.id,
                title = if (isMovie) media.title else null,
                name = if (!isMovie) media.title else null,
                posterPath = "/poster/${if (isMovie) "movie" else "tv"}_${media.id}.jpg",
                overview = media.overview
            ),
            seasons = if (!isMovie) {
                (1..media.seasons).map { seasonNum ->
                    ApiRequestSeason(id = seasonNum, seasonNumber = seasonNum, status = status)
                }
            } else null
        )
    }
    
    fun requestsList(page: Int = 1): RequestsResponse {
        val totalRequests = 15
        val pageSize = 20
        val startIndex = (page - 1) * pageSize
        val endIndex = minOf(startIndex + pageSize, totalRequests)
        
        val results = (startIndex until endIndex).map { index ->
            mediaRequest(index + 1)
        }
        
        return RequestsResponse(
            pageInfo = PageInfo(
                pages = (totalRequests + pageSize - 1) / pageSize,
                pageSize = pageSize,
                results = totalRequests,
                page = page
            ),
            results = results
        )
    }
    
    fun requestStatus(requestId: Int) = mapOf(
        "status" to when (requestId % 5) {
            0 -> 1
            1 -> 2
            2 -> 3
            3 -> 4
            else -> 5
        }
    )
    
    fun qualityProfiles() = listOf(
        ApiQualityProfile(id = 1, name = "HD-1080p"),
        ApiQualityProfile(id = 2, name = "Ultra-HD"),
        ApiQualityProfile(id = 3, name = "SD"),
        ApiQualityProfile(id = 4, name = "4K HDR")
    )
    
    fun rootFolders() = listOf(
        ApiRootFolder(id = 1, path = "/movies"),
        ApiRootFolder(id = 2, path = "/tv"),
        ApiRootFolder(id = 3, path = "/media/movies"),
        ApiRootFolder(id = 4, path = "/media/tv")
    )
    
    // ========================================================================
    // USER RESPONSES
    // ========================================================================
    
    fun userQuota() = ApiRequestQuota(
        movie = ApiQuotaInfo(
            limit = 10,
            remaining = 7,
            days = 7
        ),
        tv = ApiQuotaInfo(
            limit = 15,
            remaining = 12,
            days = 7
        )
    )
    
    fun userStatistics() = ApiUserStatistics(
        totalRequests = 32,
        approvedRequests = 24,
        declinedRequests = 2,
        pendingRequests = 4,
        availableRequests = 2
    )
    
    // ========================================================================
    // ISSUE RESPONSES - For Issue Reporting Feature
    // ========================================================================
    
    private val fakeIssues = listOf(
        FakeIssue(
            id = 1,
            type = IssueType.VIDEO,
            mediaId = 1001,
            isMovie = true,
            message = "Video quality drops to pixelated mess around the 45 minute mark. Seems to happen in dark scenes.",
            status = 1, // Open
            createdBy = 2,
            comments = listOf(
                "Thanks for reporting! We're looking into this.",
                "This has been escalated to the encoding team."
            )
        ),
        FakeIssue(
            id = 2,
            type = IssueType.AUDIO,
            mediaId = 2001,
            isMovie = false,
            seasonNum = 2,
            episodeNum = 5,
            message = "Audio is out of sync by about 2 seconds throughout the entire episode. Very distracting!",
            status = 2, // Resolved
            createdBy = 3,
            comments = listOf(
                "Confirmed the audio sync issue.",
                "Re-encoded the episode. Should be fixed now!",
                "Perfect, it's working great. Thanks!"
            )
        ),
        FakeIssue(
            id = 3,
            type = IssueType.SUBTITLES,
            mediaId = 1003,
            isMovie = true,
            message = "Spanish subtitles are missing for the last 20 minutes of the film.",
            status = 1,
            createdBy = 4,
            comments = listOf(
                "We'll add the missing subtitles ASAP."
            )
        ),
        FakeIssue(
            id = 4,
            type = IssueType.VIDEO,
            mediaId = 2003,
            isMovie = false,
            seasonNum = 4,
            episodeNum = 1,
            message = "Entire episode seems to be the wrong file - it's showing Season 3 Episode 1 instead.",
            status = 1,
            createdBy = 5,
            comments = emptyList()
        ),
        FakeIssue(
            id = 5,
            type = IssueType.OTHER,
            mediaId = 2009,
            isMovie = false,
            seasonNum = 3,
            episodeNum = 10,
            message = "The episode is listed as 45 minutes but cuts off at 38 minutes. Missing the finale!",
            status = 2,
            createdBy = 6,
            comments = listOf(
                "This is a critical issue - escalating immediately.",
                "Fixed! The complete episode is now available."
            )
        ),
        FakeIssue(
            id = 6,
            type = IssueType.AUDIO,
            mediaId = 1007,
            isMovie = true,
            message = "5.1 surround sound track has the center channel very quiet. Dialog is hard to hear.",
            status = 1,
            createdBy = 2,
            comments = listOf(
                "Can you try the stereo track as a workaround?",
                "Stereo works fine but I'd love the surround mix fixed."
            )
        ),
        FakeIssue(
            id = 7,
            type = IssueType.SUBTITLES,
            mediaId = 2006,
            isMovie = false,
            seasonNum = 1,
            episodeNum = 3,
            message = "French subtitles have lots of typos and some translations don't make sense.",
            status = 1,
            createdBy = 4,
            comments = emptyList()
        ),
        FakeIssue(
            id = 8,
            type = IssueType.VIDEO,
            mediaId = 1008,
            isMovie = true,
            message = "4K HDR version looks washed out compared to the standard HD version.",
            status = 2,
            createdBy = 3,
            comments = listOf(
                "Investigating the HDR metadata.",
                "Found an issue with the color grading. Re-processing now.",
                "All fixed - the HDR version looks amazing now!"
            )
        )
    )
    
    private enum class IssueType(val code: Int) {
        VIDEO(1),
        AUDIO(2),
        SUBTITLES(3),
        OTHER(4)
    }
    
    private data class FakeIssue(
        val id: Int,
        val type: IssueType,
        val mediaId: Int,
        val isMovie: Boolean,
        val seasonNum: Int? = null,
        val episodeNum: Int? = null,
        val message: String,
        val status: Int,
        val createdBy: Int,
        val comments: List<String> = emptyList()
    )
    
    fun issuesList(
        take: Int = 20,
        skip: Int = 0,
        filter: String = "all"
    ): ApiIssueListResponse {
        val filtered = when (filter) {
            "open" -> fakeIssues.filter { it.status == 1 }
            "resolved" -> fakeIssues.filter { it.status == 2 }
            else -> fakeIssues
        }
        
        val paged = filtered.drop(skip).take(take)
        
        return ApiIssueListResponse(
            pageInfo = PageInfo(
                pages = (filtered.size + take - 1) / take,
                pageSize = take,
                results = filtered.size,
                page = (skip / take) + 1
            ),
            results = paged.map { issue ->
                val media = if (issue.isMovie) {
                    fakeMovies.find { it.id == issue.mediaId }
                } else {
                    fakeTvShows.find { it.id == issue.mediaId }
                }
                val user = fakeUsers.find { it.id == issue.createdBy } ?: fakeUsers[0]
                
                ApiIssue(
                    id = issue.id,
                    issueType = issue.type.code,
                    status = issue.status,
                    problemSeason = issue.seasonNum,
                    problemEpisode = issue.episodeNum,
                    media = ApiIssueMedia(
                        id = issue.mediaId,
                        tmdbId = issue.mediaId,
                        mediaType = if (issue.isMovie) "movie" else "tv",
                        status = 5,
                        title = if (issue.isMovie) media?.title else null,
                        name = if (!issue.isMovie) media?.title else null,
                        posterPath = "/poster/${if (issue.isMovie) "movie" else "tv"}_${issue.mediaId}.jpg"
                    ),
                    createdBy = userProfile(user.id),
                    comments = issue.comments.mapIndexed { index, msg ->
                        val commenter = fakeUsers[(issue.createdBy + index) % fakeUsers.size]
                        ApiIssueComment(
                            id = issue.id * 100 + index,
                            user = userProfile(commenter.id),
                            message = msg,
                            createdAt = "2025-01-${(issue.id + index) % 28 + 1}T${10 + index}:30:00.000Z",
                            updatedAt = null
                        )
                    },
                    createdAt = "2025-01-${issue.id % 28}T09:00:00.000Z",
                    updatedAt = "2025-01-${issue.id % 28 + 1}T14:00:00.000Z"
                )
            }
        )
    }
    
    fun issue(issueId: Int): ApiIssue {
        val issue = fakeIssues.find { it.id == issueId } ?: fakeIssues[0]
        val media = if (issue.isMovie) {
            fakeMovies.find { it.id == issue.mediaId }
        } else {
            fakeTvShows.find { it.id == issue.mediaId }
        }
        val user = fakeUsers.find { it.id == issue.createdBy } ?: fakeUsers[0]
        
        return ApiIssue(
            id = issue.id,
            issueType = issue.type.code,
            status = issue.status,
            problemSeason = issue.seasonNum,
            problemEpisode = issue.episodeNum,
            media = ApiIssueMedia(
                id = issue.mediaId,
                tmdbId = issue.mediaId,
                mediaType = if (issue.isMovie) "movie" else "tv",
                status = 5,
                title = if (issue.isMovie) media?.title else null,
                name = if (!issue.isMovie) media?.title else null,
                posterPath = "/poster/${if (issue.isMovie) "movie" else "tv"}_${issue.mediaId}.jpg",
                backdropPath = "/backdrop/${if (issue.isMovie) "movie" else "tv"}_${issue.mediaId}.jpg"
            ),
            createdBy = userProfile(user.id),
            comments = issue.comments.mapIndexed { index, msg ->
                val commenter = fakeUsers[(issue.createdBy + index) % fakeUsers.size]
                ApiIssueComment(
                    id = issue.id * 100 + index,
                    user = userProfile(commenter.id),
                    message = msg,
                    createdAt = "2025-01-${(issue.id + index) % 28 + 1}T${10 + index}:30:00.000Z",
                    updatedAt = null
                )
            },
            createdAt = "2025-01-${issue.id % 28}T09:00:00.000Z",
            updatedAt = "2025-01-${issue.id % 28 + 1}T14:00:00.000Z"
        )
    }
    
    fun issueCounts() = ApiIssueCount(
        total = fakeIssues.size,
        video = fakeIssues.count { it.type == IssueType.VIDEO },
        audio = fakeIssues.count { it.type == IssueType.AUDIO },
        subtitles = fakeIssues.count { it.type == IssueType.SUBTITLES },
        others = fakeIssues.count { it.type == IssueType.OTHER },
        open = fakeIssues.count { it.status == 1 },
        closed = fakeIssues.count { it.status == 2 }
    )
    
    fun createIssue(
        issueType: Int,
        message: String,
        mediaId: Int
    ): ApiIssue {
        val isMovie = mediaId < 2000
        val media = if (isMovie) {
            fakeMovies.find { it.id == mediaId } ?: fakeMovies[0]
        } else {
            fakeTvShows.find { it.id == mediaId } ?: fakeTvShows[0]
        }
        
        return ApiIssue(
            id = 100, // New issue ID
            issueType = issueType,
            status = 1,
            media = ApiIssueMedia(
                id = mediaId,
                tmdbId = mediaId,
                mediaType = if (isMovie) "movie" else "tv",
                status = 5,
                title = if (isMovie) media.title else null,
                name = if (!isMovie) media.title else null,
                posterPath = "/poster/${if (isMovie) "movie" else "tv"}_${mediaId}.jpg"
            ),
            createdBy = userProfile(1),
            comments = listOf(
                ApiIssueComment(
                    id = 10001,
                    user = userProfile(1),
                    message = message,
                    createdAt = "2025-01-25T12:00:00.000Z",
                    updatedAt = null
                )
            ),
            createdAt = "2025-01-25T12:00:00.000Z",
            updatedAt = "2025-01-25T12:00:00.000Z"
        )
    }
}
