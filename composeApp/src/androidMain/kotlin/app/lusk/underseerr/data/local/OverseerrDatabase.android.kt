package app.lusk.underseerr.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import app.lusk.underseerr.util.PlatformContext


actual fun getDatabaseBuilder(context: PlatformContext): RoomDatabase.Builder<UnderseerrDatabase> {
    val dbFile = context.context.getDatabasePath(UnderseerrDatabase.DATABASE_NAME)
    return Room.databaseBuilder<UnderseerrDatabase>(
        context = context.context,
        name = dbFile.absolutePath,
        factory = { UnderseerrDatabaseConstructor.initialize() }
    )
}
