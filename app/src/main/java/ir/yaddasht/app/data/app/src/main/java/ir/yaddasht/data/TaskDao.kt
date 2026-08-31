package ir.yaddasht.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // ===== خواندن =====
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): Task?

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): Task?

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeTask(id: Long): Flow<Task?>

    // ===== نوشتن =====
    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Delete
    suspend fun delete(task: Task)

    // ✅ متد جدید: فقط ستون isCompleted رو مستقیم آپدیت می‌کنه (برای نوتیفیکیشن)
    @Query("UPDATE tasks SET isCompleted = 1 WHERE id = :id")
    suspend fun markCompleted(id: Long)

    // ===== ضمیمه‌های وظیفه =====
    @Insert
    suspend fun insertTaskAttachment(att: TaskAttachment)

    @Delete
    suspend fun deleteTaskAttachment(att: TaskAttachment)

    @Query("SELECT * FROM task_attachments WHERE taskId = :taskId")
    fun observeTaskAttachments(taskId: Long): Flow<List<TaskAttachment>>

    @Query("SELECT * FROM task_attachments WHERE taskId = :taskId")
    suspend fun taskAttachmentsByTask(taskId: Long): List<TaskAttachment>
}
