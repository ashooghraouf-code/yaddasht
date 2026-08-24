package ir.yaddasht.app.util

import android.content.Context
import android.net.Uri
import ir.yaddasht.app.data.Note
import ir.yaddasht.app.data.NoteDao
import ir.yaddasht.app.data.Priority
import ir.yaddasht.app.data.Task
import ir.yaddasht.app.data.TaskDao
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object FullBackup {

    suspend fun exportAll(context: Context, dao: NoteDao, taskDao: TaskDao): File {
        val notes = dao.allNotesSync()
        val tasks = taskDao.getAllTasksSync()
        val json = JSONObject().apply {
            put("app", "yaddasht-full-backup"); put("version", 2); put("time", System.currentTimeMillis())
            put("notes", JSONArray().apply {
                notes.forEach { n -> put(JSONObject().apply {
                    put("title", n.title); put("body", n.body); put("color", n.color)
                    put("pinned", n.pinned); put("reminderAt", n.reminderAt)
                    put("createdAt", n.createdAt); put("updatedAt", n.updatedAt)
                }) }
            })
            put("tasks", JSONArray().apply {
                tasks.forEach { t -> put(JSONObject().apply {
                    put("title", t.title); put("description", t.description); put("dueDate", t.dueDate)
                    put("priority", t.priority.name); put("isCompleted", t.isCompleted)
                    put("reminderTime", t.reminderTime); put("hasReminder", t.hasReminder)
                }) }
            })
        }
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "yaddasht-backup-${System.currentTimeMillis()}.json")
        file.writeText(json.toString(2), Charsets.UTF_8)
        return file
    }

    suspend fun importAll(context: Context, dao: NoteDao, taskDao: TaskDao, uri: Uri): Pair<Int, Int> {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: return Pair(0, 0)
        return try {
            val json = JSONObject(text)
            var n = 0; var t = 0
            json.optJSONArray("notes")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    dao.insert(Note(title = o.optString("title"), body = o.optString("body"), color = o.optInt("color"),
                        pinned = o.optBoolean("pinned"), reminderAt = o.optLong("reminderAt"),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()), updatedAt = o.optLong("updatedAt", System.currentTimeMillis())))
                    n++
                }
            }
            json.optJSONArray("tasks")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    taskDao.insert(Task(title = o.optString("title"), description = o.optString("description"),
                        dueDate = o.optLong("dueDate"),
                        priority = try { Priority.valueOf(o.optString("priority", "NORMAL")) } catch (e: Exception) { Priority.NORMAL },
                        isCompleted = o.optBoolean("isCompleted"), reminderTime = o.optLong("reminderTime"), hasReminder = o.optBoolean("hasReminder")))
                    t++
                }
            }
            Pair(n, t)
        } catch (e: Exception) { Pair(0, 0) }
    }
}
