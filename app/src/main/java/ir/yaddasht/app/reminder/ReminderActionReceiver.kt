package ir.yaddasht.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import ir.yaddasht.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getStringExtra(EXTRA_ACTION) ?: return
        val id = intent.getLongExtra(ReminderScheduler.EXTRA_NOTE_ID, -1L)
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: ""
        val isTask = intent.getBooleanExtra(ReminderScheduler.EXTRA_IS_TASK, false)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, id.toInt())
        val originalTime = intent.getLongExtra(EXTRA_ORIGINAL_TIME, System.currentTimeMillis())

        if (id < 0) return

        // پاک کردن نوتیفیکیشن
        try {
            NotificationManagerCompat.from(context).cancel(notificationId)
        } catch (_: SecurityException) {}

        when (action) {
            ACTION_DONE -> {
                if (!isTask) return
                val db = AppDatabase.get(context.applicationContext)
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        db.taskDao().markCompleted(id)
                        ReminderScheduler.cancelAll(context, id, true)
                    } catch (_: Exception) {}
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "✅ «$title» انجام شد", Toast.LENGTH_SHORT).show()
                        pending.finish()
                    }
                }
            }
            ACTION_SNOOZE -> {
                val newTime = System.currentTimeMillis() + 5 * 60_000L
                ReminderScheduler.scheduleMulti(context, id, title, newTime, isTask, setOf(LeadTime.NONE))
                Toast.makeText(context, "⏰ ۵ دقیقهٔ دیگه یادآوری می‌شه", Toast.LENGTH_SHORT).show()
            }
            ACTION_TOMORROW -> {
                val newTime = originalTime + 86_400_000L
                ReminderScheduler.scheduleMulti(context, id, title, newTime, isTask, setOf(LeadTime.NONE))
                Toast.makeText(context, "📅 فردا همین موقع", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val EXTRA_ACTION = "action"
        const val EXTRA_NOTIFICATION_ID = "notif_id"
        const val EXTRA_ORIGINAL_TIME = "orig_time"
        const val ACTION_DONE = "done"
        const val ACTION_SNOOZE = "snooze"
        const val ACTION_TOMORROW = "tomorrow"
    }
}
