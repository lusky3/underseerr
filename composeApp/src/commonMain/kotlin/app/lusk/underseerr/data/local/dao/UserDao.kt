package app.lusk.underseerr.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.lusk.underseerr.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getUser(): Flow<UserEntity?>

    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getUserSync(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Query("DELETE FROM user_profile")
    suspend fun delete()
}
