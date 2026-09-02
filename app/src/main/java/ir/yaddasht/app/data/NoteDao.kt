package ir.yaddasht.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun observeNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun allNotesSync(): List<Note>

    // ✅ تابع جدید برای ویجت: خواندن ۳ یادداشت آخر
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC LIMIT 3")
    suspend fun getRecentNotes(): List<Note>

    @Query("SELECT noteId, COUNT(*) as count FROM attachments GROUP BY noteId")
    fun observeAttachmentCounts(): Flow<List<AttachmentCount>>

    @Query("SELECT * FROM attachments WHERE noteId = :noteId")
    suspend fun attachmentsByNote(noteId: Long): List<Attachment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)
}

// ✅ تعریف صحیح جدول Attachments برای رفع خطای Room
@Entity(tableName = "attachments")
data class Attachment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val filePath: String
)

data class AttachmentCount(val noteId: Long, val count: Int)
