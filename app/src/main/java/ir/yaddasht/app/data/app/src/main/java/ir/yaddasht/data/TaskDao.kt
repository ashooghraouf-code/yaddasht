package ir.yaddasht.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): Task?

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): Task?

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeTask(id: Long): Flow<Task?>

    @Query("SELECT * FROM tasks WHERE dueDate > 0 AND isCompleted = 0")
    suspend fun getActiveTasksSync(): List<Task>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY dueDate ASC LIMIT 20")
    suspend fun getUpcomingTasksSync(): List<Task>

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Delete
    suspend fun delete(task: Task)

    @Query("UPDATE tasks SET isCompleted = 1 WHERE id = :id")
    suspend fun markCompleted(id: Long)

    @Insert
    suspend fun insertTaskAttachment(att: TaskAttachment)

    @Delete
    suspend fun deleteTaskAttachment(att: TaskAttachment)

    @Query("SELECT * FROM task_attachments WHERE taskId = :taskId")
    fun observeTaskAttachments(taskId: Long): Flow<List<TaskAttachment>>

    @Query("SELECT * FROM task_attachments WHERE taskId = :taskId")
    suspend fun taskAttachmentsByTask(taskId: Long): List<TaskAttachment>
}
