package app.lusk.underseerr.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.lusk.underseerr.util.PlatformContext
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask

actual fun getDatabaseBuilder(context: PlatformContext): RoomDatabase.Builder<UnderseerrDatabase> {
    val dbFilePath = NSHomeDirectory() + "/Documents/" + UnderseerrDatabase.DATABASE_NAME
    return Room.databaseBuilder<UnderseerrDatabase>(
        name = dbFilePath
    )
    .setDriver(BundledSQLiteDriver())
}

