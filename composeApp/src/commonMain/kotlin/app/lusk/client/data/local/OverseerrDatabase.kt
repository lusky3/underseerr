package app.lusk.client.data.local

import androidx.room.*
import app.lusk.client.data.local.converter.IntListConverter
import app.lusk.client.data.local.dao.*
import app.lusk.client.data.local.entity.*
import app.lusk.client.util.PlatformContext

// Room KMP constructor
expect object OverseerrDatabaseConstructor : RoomDatabaseConstructor<OverseerrDatabase>

/**
 * Room database for the Overseerr Android Client.
 * Feature: overseerr-android-client
 * Validates: Requirements 7.1, 7.4
 */
@Database(
    entities = [
        MovieEntity::class,
        TvShowEntity::class,
        MediaRequestEntity::class,
        NotificationEntity::class,
        OfflineRequestEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(IntListConverter::class)
@ConstructedBy(OverseerrDatabaseConstructor::class)
abstract class OverseerrDatabase : RoomDatabase() {
    
    abstract fun movieDao(): MovieDao
    abstract fun tvShowDao(): TvShowDao
    abstract fun mediaRequestDao(): MediaRequestDao
    abstract fun notificationDao(): NotificationDao
    abstract fun offlineRequestDao(): OfflineRequestDao
    
    companion object {
        const val DATABASE_NAME = "overseerr_database"
    }
}

/**
 * Base function to create the Room database builder.
 */
expect fun getDatabaseBuilder(context: PlatformContext): RoomDatabase.Builder<OverseerrDatabase>
