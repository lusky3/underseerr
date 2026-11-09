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
