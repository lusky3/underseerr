package app.lusk.underseerr.ui.accessibility

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Accessibility extensions for Compose UI.
 * Feature: underseerr
 * Validates: Requirements 9.1
 */

/**
 * Add content description for accessibility.
 */
fun Modifier.accessibilityDescription(description: String): Modifier {
    return this.semantics {
        contentDescription = description
    }
}

/**
 * Generate content description for media items.
 */
fun generateMediaDescription(
    title: String,
    year: String?,
    rating: Double?,
    status: String?
): String {
    val parts = mutableListOf(title)
    
    year?.let { parts.add("Released in $it") }
    rating?.let { parts.add("Rating: ${"%.1f".format(it)} out of 10") }
    status?.let { parts.add("Status: $it") }
    
    return parts.joinToString(", ")
}

/**
 * Generate content description for request items.
 */
fun generateRequestDescription(
    title: String,
    status: String,
    date: String
): String {
    return "$title, Status: $status, Requested on $date"
}

/**
 * Generate content description for buttons.
 */
fun generateButtonDescription(
    action: String,
    target: String? = null
): String {
    return if (target != null) {
        "$action $target"
    } else {
        action
    }
}

/**
 * Generate content description for images.
 */
fun generateImageDescription(
    type: String,
    title: String
): String {
    return "$type for $title"
}
