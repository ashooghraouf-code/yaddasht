package ir.yaddasht.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val color: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val pinned: Boolean,
    val reminderAt: Long,
    val notebookStyle: String = "blank",
    val fontSize: Float = 16f,
    val textColor: Int = 0xFF2C2C2C.toInt(),
    val isRTL: Boolean = true,
    val textBoxes: String = "[]"
)
