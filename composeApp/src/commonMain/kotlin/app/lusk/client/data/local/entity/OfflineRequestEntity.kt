package app.lusk.client.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey



@Entity(tableName = "offline_requests")
data class OfflineRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mediaType: String,
    val mediaId: Int,
    val seasons: String?, // comma separated
    val qualityProfile: Int?,
    val rootFolder: String?,
    val createdAt: Long = app.lusk.client.util.nowMillis()
)
