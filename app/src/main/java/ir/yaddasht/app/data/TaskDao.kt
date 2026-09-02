package ir.yaddasht.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY dueDate ASC LIMIT 3")
    suspend fun getRecentTasks(): List<Task>

    @Query("SELECT * FROM tasks")
    suspend fun getActiveTasksSync(): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): Task?

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun markCompleted(id: Long, isCompleted: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM attachments WHERE noteId = :taskId")
    fun observeTaskAttachments(taskId: Long): Flow<List<Attachment>>

    @Query("SELECT * FROM attachments WHERE noteId = :taskId")
    suspend fun taskAttachmentsByTask(taskId: Long): List<Attachment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskAttachment(attachment: Attachment): Long

    @Delete
    suspend fun deleteTaskAttachment(attachment: Attachment)
}
