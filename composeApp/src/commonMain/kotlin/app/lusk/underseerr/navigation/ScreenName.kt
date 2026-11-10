package app.lusk.underseerr.navigation

/**
 * Turns a type-safe navigation route into a stable, ID-free analytics screen name.
 *
 * Routes look like `app.lusk.underseerr.navigation.Screen.MediaDetails/{mediaType}/{mediaId}?openRequest={openRequest}`.
 * Only the class name is kept, so no media ID, request ID or token is ever logged.
 */
fun screenNameFor(route: String?): String? {
    val type = route
        ?.substringBefore('?')
        ?.substringBefore('/')
        ?.substringAfterLast('.')
        ?.takeIf { it.isNotBlank() }
        ?: return null
    return type.replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").lowercase()
}

/** Route name of the tab host; the tabs inside it report themselves via [mainTabScreenName]. */
const val MAIN_TABS_SCREEN = "main_tabs"

private val MAIN_TAB_SCREENS = listOf("home", "requests", "issues", "profile")

/** Analytics name for a page of the main-tab pager, or null if out of range. */
fun mainTabScreenName(page: Int): String? = MAIN_TAB_SCREENS.getOrNull(page)
