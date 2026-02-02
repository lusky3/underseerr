---
description: Troubleshooting and Architecture Guide for Underseerr
---

### Common Issues and Solutions

#### 1. Request Pagination Issues

- **Problem**: List stops loading at 10 items or fails to load more data on scroll.
- **Cause**: The Overseerr API may return fewer items than requested (e.g., returning 10 when `take=50` was sent). If the client logic assumes "items < pageSize" means EOF, it will break.
- **Solution**: Always use the `response.pageInfo.results` field from the server to determine the absolute total items availability. Compare `(skip + fetched)` against this total to determine `isLastPage`.

#### 2. Credential Bleeding (Security)

- **Problem**: Requests to external services (Plex.tv, TMDB) fail with "Resource not found" or 401/403 errors.
- **Cause**: The `HttpClientFactory` interceptor might be applying Overseerr-specific headers (`X-Api-Key`, `Cookie`) to ALL outgoing requests.
- **Solution**: The interceptor MUST verify the target host. Only apply Overseerr credentials if the host matches the user-configured `serverUrl`. DO NOT send these headers to `plex.tv` or other external domains.

#### 3. Broken Titles and Box Art

- **Problem**: Requests show "Title Unavailable" or placeholder images.
- **Cause**: Overseerr "request" objects often lack metadata until the server background-processes them.
- **Solution**: Implement "Hydration" in the `RequestRepository`.
  - Fetch details from `/api/v1/movie/{id}` or `/api/v1/tv/{id}`.
  - Run these fetches in **Parallel** using Kotlin Coroutines (`async/awaitAll`).
  - Use a strict **Timeout** (e.g., 3 seconds) for each item to prevent a single slow API call from hanging the entire list refresh.

#### 4. UI Loading Patterns

- **Problem**: Double spinners or no feedback on manual refresh.
- **Cause**: Overlapping loading states (`isLoading` vs `pullRefreshing`).
- **Solution**:
  - **Initial Load**: Show a central `CircularProgressIndicator` if cache is empty.
  - **Manual/Pull Refresh**: Trigger the `PullToRefreshBox` state even from the top-bar button. This provides a consistent "header spinner" experience.
  - **Conditional Logic**: Hide the central spinner if `pullRefreshing` is active.

#### 5. Image URL Construction

- **Problem**: Posters not loading despite having a path.
- **Cause**: Overseerr paths can be absolute, proxy-prefixed (`/api/v1/proxy`), or standard TMDB relative paths.
- **Solution**: Implement a `remember` block that handles all three:
  - `http` -> Use as-is.
  - `/api/v1/proxy` -> Prepend the `serverUrl`.
  - Simple `/path` -> Prepend TMDB base URL.
