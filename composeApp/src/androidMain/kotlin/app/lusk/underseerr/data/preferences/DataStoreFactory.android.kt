package app.lusk.underseerr.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import app.lusk.underseerr.util.PlatformContext
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import okio.Path.Companion.toPath

actual fun createDataStore(context: PlatformContext): DataStore<Preferences> {
    return PreferenceDataStoreFactory.create(
        produceFile = { context.context.preferencesDataStoreFile("underseerr_preferences") }
    )
}
