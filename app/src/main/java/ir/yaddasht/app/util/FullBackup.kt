package ir.yaddasht.app.util

import android.content.Context
import android.net.Uri
import ir.yaddasht.app.data.Note
import ir.yaddasht.app.data.NoteDao
import ir.yaddasht.app.data.Task
import ir.yaddasht.app.data.TaskDao
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object FullBackup {

    suspend fun exportAll(context: Context, dao: NoteDao, taskDao: TaskDao): File {
        val notes = dao.allNotesSync()
        val tasks = try { taskDao.getAllTasks().first() } catch (e: Exception) { emptyList() }

        val json = JSONObject().apply {
            put("app", "yaddasht-full-backup"); put("version", 2); put("time", System.currentTimeMillis())
            put("notes", JSONArray().apply {
                notes.forEach { n ->
                    put(JSONObject().apply {
                        put("title", n.title); put("body", n.body); put("color", n.color)
                        put("pinned", n.pinned); put("reminderAt", n.reminderAt)
                        put("createdAt", n.createdAt); put("updatedAt", n.updatedAt)
                    })
                }
            })
            put("tasks", JSONArray().apply {
                tasks.forEach { t ->
                    put(JSONObject().apply {
                        put("title", t.title); put("description", t.description); put("dueDate", t.dueDate)
                        put("priority", try { t.priority.name } catch (e: Exception) { "NORMAL" })
                        put("isCompleted", t.isCompleted)
                        put("reminderTime", try { t.reminderTime } catch (e: Exception) { 0L })
                        put("hasReminder", try { t.hasReminder } catch (e: Exception) { false })
                    })
                }
            })
        }
        val dir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val file = File(dir, "yaddasht-backup-${System.currentTimeMillis()}.json")
        file.writeText(json.toString(2), Charsets.UTF_8)
        return file
    }

    suspend fun importAll(context: Context, dao: NoteDao, taskDao: TaskDao, uri: Uri): Pair<Int, Int> {
        return try {
            val text = context.contentResolver.openInputStream(uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            } ?: return Pair(0, 0)
            val json = JSONObject(text)
            var n = 0; var t = 0

            json.optJSONArray("notes")?.let { arr ->
                for (i in 0 until arr.length()) {
                    try {
                        val o = arr.getJSONObject(i)
                        val note = Note(
                            title = o.optString("title", ""), body = o.optString("body", ""),
                            color = o.optInt("color", 0), pinned = o.optBoolean("pinned", false),
                            reminderAt = o.optLong("reminderAt", 0L),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                        )
                        dao.insert(note)
                        n++
                    } catch (e: Exception) { }
                }
            }

            json.optJSONArray("tasks")?.let { arr ->
                for (i in 0 until arr.length()) {
                    try {
                        val o = arr.getJSONObject(i)
                        val priority = try { ir.yaddasht.app.data.Priority.valueOf(o.optString("priority", "NORMAL")) }
                        catch (e: Exception) { ir.yaddasht.app.data.Priority.NORMAL }
                        val task = Task(
                            title = o.optString("title", ""), description = o.optString("description", ""),
                            dueDate = o.optLong("dueDate", 0L), priority = priority,
                            isCompleted = o.optBoolean("isCompleted", false),
                            reminderTime = o.optLong("reminderTime", 0L),
                            hasReminder = o.optBoolean("hasReminder", false)
                        )
                        taskDao.insert(task)
                        t++
                    } catch (e: Exception) { }
                }
            }
            Pair(n, t)
        } catch (e: Exception) { Pair(0, 0) }
    }
}
