package app.lusk.underseerr.data.local.dao

import androidx.room.*
import app.lusk.underseerr.data.local.entity.IssueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IssueDao {
    @Query("SELECT * FROM issues ORDER BY cachedAt DESC")
    fun getAllIssues(): Flow<List<IssueEntity>>

    @Query("SELECT * FROM issues ORDER BY cachedAt DESC")
    suspend fun getAllIssuesSync(): List<IssueEntity>

    @Query("SELECT * FROM issues WHERE id = :issueId")
    suspend fun getIssue(issueId: Int): IssueEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIssue(issue: IssueEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIssues(issues: List<IssueEntity>)

    @Query("DELETE FROM issues WHERE id = :issueId")
    suspend fun deleteIssue(issueId: Int)

    @Query("DELETE FROM issues")
    suspend fun clearAll()
}
