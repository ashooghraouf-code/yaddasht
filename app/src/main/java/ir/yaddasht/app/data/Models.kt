package ir.yaddasht.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val body: String = "",
    val color: Int = 0,
    val pinned: Boolean = false,
    val reminderAt: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "attachments",
    foreignKeys = [ForeignKey(entity = Note::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("noteId")])
data class Attachment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val fileName: String,
    val filePath: String,
    val mimeType: String,
    val isImage: Boolean
)

// ✅ هر دو فیلد count و attachmentCount به‌صورت واقعی
// Room هر دو را از کوئری پر می‌کند (هر دو یک مقدار دارند)
data class AttachmentCount(
    val noteId: Long,
    val count: Int,
    val attachmentCount: Int
)
