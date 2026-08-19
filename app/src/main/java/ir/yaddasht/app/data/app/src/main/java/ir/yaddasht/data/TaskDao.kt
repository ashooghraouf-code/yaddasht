package ir.yaddasht.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, CASE priority WHEN 'HIGH' THEN 1 WHEN 'NORMAL' THEN 2 WHEN 'LOW' THEN 3 END ASC, dueDate ASC")
    fun getAllTasks(): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, CASE priority WHEN 'HIGH' THEN 1 WHEN 'NORMAL' THEN 2 WHEN 'LOW' THEN 3 END ASC, dueDate ASC")
    fun getAllTasksSync(): List<Task>
    
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): Task?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task)
    
    @Update
    suspend fun update(task: Task)
    
    @Delete
    suspend fun delete(task: Task)
    
    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("SELECT * FROM tasks WHERE hasReminder = 1 AND reminderTime <= :currentTime AND isCompleted = 0")
    suspend fun getTasksWithDueReminders(currentTime: Long): List<Task>
}
