package app.lusk.underseerr.util

import androidx.compose.runtime.Composable

/**
 * Platform-specific resource accessors.
 */
@Composable
expect fun getAppIcon(): Any?
