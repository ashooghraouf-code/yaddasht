package ir.yaddasht.app.util

import android.content.Context
import android.net.Uri
import android.util.Base64
import ir.yaddasht.app.data.Attachment
import ir.yaddasht.app.data.Note
import ir.yaddasht.app.data.NoteDao
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    fun buildBackup(notes: List<Note>, attachmentsByNote: Map<Long, List<Attachment>>): JSONObject {
        val root = JSONObject()
        root.put("app", "yaddasht"); root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        val notesArr = JSONArray()
        for (n in notes) {
            val no = JSONObject()
            no.put("title", n.title); no.put("body", n.body)
            no.put("color", n.color); no.put("pinned", n.pinned)
            no.put("reminderAt", n.reminderAt)
            no.put("createdAt", n.createdAt); no.put("updatedAt", n.updatedAt)
            val attArr = JSONArray()
            for (a in attachmentsByNote[n.id].orEmpty()) {
                val f = File(a.filePath)
                if (f.exists() && f.length() < 15L * 1024 * 1024) {
                    val ao = JSONObject()
                    ao.put("fileName", a.fileName); ao.put("mimeType", a.mimeType)
                    ao.put("isImage", a.isImage)
                    ao.put("data", Base64.encodeToString(f.readBytes(), Base64.NO_WRAP))
                    attArr.put(ao)
                }
            }
            no.put("attachments", attArr); notesArr.put(no)
        }
        root.put("notes", notesArr)
        return root
    }

    fun exportBackupFile(context: Context, json: JSONObject): File {
        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "yaddasht_backup_$stamp.json")
        file.writeText(json.toString(2))
        return file
    }

    suspend fun restore(context: Context, dao: NoteDao, uri: Uri): Int {
        val text = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() } ?: return 0
        return try {
            val notesArr = JSONObject(text).getJSONArray("notes")
            var count = 0
            for (i in 0 until notesArr.length()) {
                val no = notesArr.getJSONObject(i)
                val note = Note(
                    title = no.optString("title"), body = no.optString("body"),
                    color = no.optInt("color"), pinned = no.optBoolean("pinned"),
                    reminderAt = no.optLong("reminderAt"),
                    createdAt = no.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = no.optLong("updatedAt", System.currentTimeMillis())
                )
                val newId = dao.insert(note)
                val attArr = no.optJSONArray("attachments")
                if (attArr != null) for (j in 0 until attArr.length()) {
                    val ao = attArr.getJSONObject(j)
                    val data = Base64.decode(ao.optString("data"), Base64.NO_WRAP)
                    val name = ao.optString("fileName", "file")
                    val clean = name.replace(Regex("[^\\p{L}\\p{N}._ -]"), "_")
                    val file = File(AttachmentStore.attachmentsDir(context), "${System.currentTimeMillis()}_$clean")
                    file.writeBytes(data)
                    dao.insertAttachment(Attachment(newId, name, file.absolutePath, ao.optString("mimeType", "*/*"), ao.optBoolean("isImage")))
                }
                count++
            }
            count
        } catch (e: Exception) {
            e.printStackTrace(); 0
        }
    }
}