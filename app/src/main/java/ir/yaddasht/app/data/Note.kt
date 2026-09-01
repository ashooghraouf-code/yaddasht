package ir.yaddasht.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val body: String = "",
    val color: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val pinned: Boolean = false,
    val reminderAt: Long = 0,
    val notebookStyle: String = "blank",
    val fontSize: Float = 16f,
    val textColor: Int = 0xFF2C2C2C.toInt(),
    val isRTL: Boolean = true,
    val textBoxes: String = "[]"
)
