package ir.yaddasht.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY pinned DESC, updatedAt DESC")
    fun observeNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeNote(id: Long): Flow<Note?>

    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM attachments WHERE noteId = :noteId ORDER BY id")
    fun observeAttachments(noteId: Long): Flow<List<Attachment>>

    @Query("SELECT * FROM attachments WHERE noteId = :noteId")
    suspend fun attachmentsByNote(noteId: Long): List<Attachment>

    @Insert
    suspend fun insertAttachment(attachment: Attachment): Long

    @Delete
    suspend fun deleteAttachment(attachment: Attachment)

    @Query("SELECT COUNT(*) FROM attachments WHERE noteId = :noteId")
    suspend fun attachmentCount(noteId: Long): Int

    @Query("SELECT noteId, COUNT(*) AS count FROM attachments GROUP BY noteId")
    fun observeAttachmentCounts(): Flow<List<AttachmentCount>>

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC LIMIT 1")
    suspend fun latestNote(): Note?

    @Query("SELECT * FROM notes")
    suspend fun allNotesSync(): List<Note>

    @Query("SELECT * FROM attachments")
    suspend fun allAttachments(): List<Attachment>
}