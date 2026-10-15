package ir.yaddasht.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "habit_logs",
    primaryKeys = ["habitId", "dateKey"],
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("habitId")]
)
data class HabitLog(
    val habitId: Long,
    val dateKey: String,
    val count: Int = 0,
    val doneAt: Long = 0L,
    val rest: Boolean = false
)
