package ir.yaddasht.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Priority {
    LOW, NORMAL, HIGH
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueDate: Long = 0,
    val priority: Priority = Priority.NORMAL,
    val isCompleted: Boolean = false,
    val reminderTime: Long = 0,
    val hasReminder: Boolean = false
)
