package ir.yaddasht.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import ir.yaddasht.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != "com.htc.intent.action.QUICKBOOT_POWERON") {
            return
        }

        Log.d(TAG, "🔔 Boot completed - rescheduling all reminders")

        val pending = goAsync()
        val db = AppDatabase.get(context.applicationContext)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                var rescheduled = 0

                // ۱. وظایف فعال
                db.taskDao().getActiveTasksSync().forEach { task ->
                    if (task.dueDate > now) {
                        val leads = setOf(LeadTime.NONE, LeadTime.HOUR_1, LeadTime.MIN_10)
                        ReminderScheduler.scheduleMulti(context, task.id, task.title, task.dueDate, true, leads)
                        rescheduled++
                    }
                }

                // ۲. یادداشت‌های فعال
                db.dao().allNotesSync().forEach { note ->
                    if (note.reminderAt > now) {
                        val leads = setOf(LeadTime.NONE, LeadTime.HOUR_1)
                        ReminderScheduler.scheduleMulti(
                            context, note.id,
                            note.title.ifBlank { "یادداشت" },
                            note.reminderAt, false, leads
                        )
                        rescheduled++
                    }
                }

                Log.d(TAG, "✅ $rescheduled reminders rescheduled after boot")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to reschedule reminders", e)
            } finally {
                withContext(Dispatchers.Main) {
                    pending.finish()
                }
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
