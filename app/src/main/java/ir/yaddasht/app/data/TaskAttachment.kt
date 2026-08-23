
package ir.yaddasht.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_attachments")
data class TaskAttachment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val fileName: String,
    val filePath: String,
    val mimeType: String,
    val isImage: Boolean
)
