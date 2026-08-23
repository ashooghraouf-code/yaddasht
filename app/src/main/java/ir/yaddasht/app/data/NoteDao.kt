package ir.yaddasht.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun observeNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun allNotesSync(): List<Note>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeNote(id: Long): Flow<Note?>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM attachments WHERE noteId = :noteId ORDER BY id ASC")
    fun observeAttachments(noteId: Long): Flow<List<Attachment>>

    @Query("SELECT * FROM attachments")
    fun allAttachments(): List<Attachment>

    @Query("SELECT * FROM attachments WHERE noteId = :noteId")
    suspend fun attachmentsByNote(noteId: Long): List<Attachment>

    @Query("SELECT COUNT(*) FROM attachments WHERE noteId = :noteId")
    suspend fun attachmentCount(noteId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: Attachment): Long

    @Delete
    suspend fun deleteAttachment(attachment: Attachment)

    @Query("SELECT noteId, COUNT(*) as count FROM attachments GROUP BY noteId")
    fun observeAttachmentCounts(): Flow<List<AttachmentCount>>
}
