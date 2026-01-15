package app.lusk.client.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import app.lusk.client.util.PlatformContext
import platform.Foundation.NSHomeDirectory


actual fun getDatabaseBuilder(context: PlatformContext): RoomDatabase.Builder<OverseerrDatabase> {
    val dbFilePath = NSHomeDirectory() + "/" + OverseerrDatabase.DATABASE_NAME
    return Room.databaseBuilder<OverseerrDatabase>(
        name = dbFilePath,
        factory = { OverseerrDatabaseConstructor.initialize() }
    )
}
