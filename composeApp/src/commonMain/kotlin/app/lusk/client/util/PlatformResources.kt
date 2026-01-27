package app.lusk.client.util

import androidx.compose.runtime.Composable

/**
 * Platform-specific resource accessors.
 */
@Composable
expect fun getAppIcon(): Any?
