package ir.yaddasht.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val emoji: String = "✅",
    val colorIndex: Int = 0,
    val kind: String = "BINARY",
    val targetPerDay: Int = 1,
    val unit: String = "بار",
    val pattern: String = "DAILY",
    val weekdaysMask: Int = 0,
    val everyNDays: Int = 1,
    val cueText: String = "",
    val rewardText: String = "",
    val archived: Boolean = false,
    val createdAt: Long = 0
)

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
    val count: Int = 1,
    val doneAt: Long = 0,
    val rest: Boolean = false
)
