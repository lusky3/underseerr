package app.lusk.client.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import app.lusk.client.util.PlatformContext


actual fun getDatabaseBuilder(context: PlatformContext): RoomDatabase.Builder<OverseerrDatabase> {
    val dbFile = context.context.getDatabasePath(OverseerrDatabase.DATABASE_NAME)
    return Room.databaseBuilder<OverseerrDatabase>(
        context = context.context,
        name = dbFile.absolutePath,
        factory = { OverseerrDatabaseConstructor.initialize() }
    )
}
