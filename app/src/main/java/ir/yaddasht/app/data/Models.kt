package ir.yaddasht.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attachments",
    foreignKeys = [ForeignKey(
        entity = Note::class,
        parentColumns = ["id"],
        childColumns = ["noteId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("noteId")]
)
data class Attachment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val filePath: String,
    val type: String, // "image", "audio", "file"
    val createdAt: Long = System.currentTimeMillis()
)

data class AttachmentCount(
    val noteId: Long,
    val count: Int
)
