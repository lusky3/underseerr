package app.lusk.underseerr.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.lusk.underseerr.util.PlatformContext

/**
 * Creates a DataStore instance for the given context.
 */
expect fun createDataStore(context: PlatformContext): DataStore<Preferences>

internal const val DATASTORE_FILE_NAME = "underseerr_preferences.preferences_pb"
