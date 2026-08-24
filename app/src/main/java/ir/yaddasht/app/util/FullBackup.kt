package ir.yaddasht.app.util

import android.content.Context
import android.net.Uri
import ir.yaddasht.app.data.Note
import ir.yaddasht.app.data.NoteDao
import ir.yaddasht.app.data.Task
import ir.yaddasht.app.data.TaskDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object FullBackup {

    suspend fun exportAll(context: Context, dao: NoteDao, taskDao: TaskDao): File {
        return withContext(Dispatchers.IO) {
            val notes = dao.allNotesSync()
            val tasks = taskDao.getAllTasks().first()

            val json = JSONObject().apply {
                put("app", "yaddasht-full-backup")
                put("version", 1)
                put("timestamp", System.currentTimeMillis())

                put("notes", JSONArray().apply {
                    notes.forEach { note ->
                        put(JSONObject().apply {
                            put("id", note.id)
                            put("title", note.title)
                            put("body", note.body)
                            put("color", note.color)
                            put("pinned", note.pinned)
                            put("reminderAt", note.reminderAt)
                            put("createdAt", note.createdAt)
                            put("updatedAt", note.updatedAt)
                        })
                    }
                })

                put("tasks", JSONArray().apply {
                    tasks.forEach { task ->
                        put(JSONObject().apply {
                            put("id", task.id)
                            put("title", task.title)
                            put("description", task.description)
                            put("dueDate", task.dueDate)
                            put("priority", task.priority.name)
                            put("isCompleted", task.isCompleted)
                            put("reminderTime", task.reminderTime)
                            put("hasReminder", task.hasReminder)
                        })
                    }
                })
            }

            val file = File(context.cacheDir, "yaddasht_backup_${System.currentTimeMillis()}.json")
            file.writeText(json.toString(2))
            file
        }
    }

    suspend fun importAll(context: Context, dao: NoteDao, taskDao: TaskDao, uri: Uri): Pair<Int, Int> {
        return withContext(Dispatchers.IO) {
            var noteCount = 0
            var taskCount = 0

            try {
                val jsonText = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                if (jsonText.isNullOrBlank()) return@withContext Pair(0, 0)

                val json = JSONObject(jsonText)

                json.optJSONArray("notes")?.let { notesArray ->
                    for (i in 0 until notesArray.length()) {
                        try {
                            val noteJson = notesArray.getJSONObject(i)
                            val note = Note(
                                id = 0,
                                title = noteJson.optString("title", ""),
                                body = noteJson.optString("body", ""),
                                color = noteJson.optInt("color", 0),
                                pinned = noteJson.optBoolean("pinned", false),
                                reminderAt = noteJson.optLong("reminderAt", 0L),
                                createdAt = noteJson.optLong("createdAt", System.currentTimeMillis()),
                                updatedAt = noteJson.optLong("updatedAt", System.currentTimeMillis())
                            )
                            dao.insert(note)
                            noteCount++
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                json.optJSONArray("tasks")?.let { tasksArray ->
                    for (i in 0 until tasksArray.length()) {
                        try {
                            val taskJson = tasksArray.getJSONObject(i)
                            val task = Task(
                                id = 0,
                                title = taskJson.optString("title", ""),
                                description = taskJson.optString("description", ""),
                                dueDate = taskJson.optLong("dueDate", 0L),
                                priority = try {
                                    ir.yaddasht.app.data.Priority.valueOf(taskJson.optString("priority", "NORMAL"))
                                } catch (e: Exception) {
                                    ir.yaddasht.app.data.Priority.NORMAL
                                },
                                isCompleted = taskJson.optBoolean("isCompleted", false),
                                reminderTime = taskJson.optLong("reminderTime", 0L),
                                hasReminder = taskJson.optBoolean("hasReminder", false)
                            )
                            taskDao.insert(task)
                            taskCount++
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            Pair(noteCount, taskCount)
        }
    }
}
