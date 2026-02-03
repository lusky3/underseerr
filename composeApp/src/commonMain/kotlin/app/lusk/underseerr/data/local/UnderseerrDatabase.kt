package app.lusk.underseerr.data.local

import androidx.room.*
import app.lusk.underseerr.data.local.converter.IntListConverter
import app.lusk.underseerr.data.local.dao.*
import app.lusk.underseerr.data.local.entity.*
import app.lusk.underseerr.util.PlatformContext

// Room KMP constructor - Room KSP generates the actual implementation
@Suppress("NO_ACTUAL_FOR_EXPECT", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object UnderseerrDatabaseConstructor : RoomDatabaseConstructor<UnderseerrDatabase>

/**
 * Room database for the Underseerr.
 * Feature: underseerr
 * Validates: Requirements 7.1, 7.4
 */
@Database(
    entities = [
        MovieEntity::class,
        TvShowEntity::class,
        MediaRequestEntity::class,
        NotificationEntity::class,
        OfflineRequestEntity::class,
        UserEntity::class,
        DiscoveryCacheEntity::class,
        IssueEntity::class
    ],
    version = 7,
    exportSchema = true
)
@TypeConverters(IntListConverter::class)
@ConstructedBy(UnderseerrDatabaseConstructor::class)
abstract class UnderseerrDatabase : RoomDatabase() {
    
    abstract fun movieDao(): MovieDao
    abstract fun tvShowDao(): TvShowDao
    abstract fun mediaRequestDao(): MediaRequestDao
    abstract fun notificationDao(): NotificationDao
    abstract fun offlineRequestDao(): OfflineRequestDao
    abstract fun userDao(): UserDao
    abstract fun discoveryDao(): DiscoveryDao
    abstract fun issueDao(): IssueDao
    
    companion object {
        const val DATABASE_NAME = "underseerr_database"
    }
}

/**
 * Base function to create the Room database builder.
 */
expect fun getDatabaseBuilder(context: PlatformContext): RoomDatabase.Builder<UnderseerrDatabase>
