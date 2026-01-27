package app.lusk.underseerr.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.lusk.underseerr.util.PlatformContext
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*
import okio.Path.Companion.toPath

@OptIn(ExperimentalForeignApi::class)
actual fun createDataStore(context: PlatformContext): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            val fileManager = NSFileManager.defaultManager
            val directory = fileManager.URLForDirectory(
                directory = NSApplicationSupportDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = null
            )
            
            val validDirectory = requireNotNull(directory) { "Application Support directory not found" }
            val path = validDirectory.path + "/$DATASTORE_FILE_NAME"
            println("DataStoreFactory: Saving preferences to $path")
            path.toPath()
        }
    )
}
