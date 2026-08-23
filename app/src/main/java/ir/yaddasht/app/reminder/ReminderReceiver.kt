package ir.yaddasht.app.reminder

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ir.yaddasht.app.MainActivity

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ReminderScheduler.EXTRA_NOTE_ID, -1L)
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "یادداشت"
        val isTask = intent.getBooleanExtra(ReminderScheduler.EXTRA_IS_TASK, false)
        ReminderScheduler.ensureChannel(context)

        val open = Intent(context, MainActivity::class.java)
            .putExtra("note_id", id)
            .putExtra("is_task", isTask)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(context, id.toInt(), open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(if (isTask) "✅ یادآور وظیفه: $title" else "⏰ یادآور: $title")
            .setContentText("وقتشه! برای دیدن ضربه بزن.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setVibrate(longArrayOf(0, 800, 400, 800, 400, 800))
            .setDefaults(Notification.DEFAULT_ALL)
            .build()
        notification.flags = notification.flags or Notification.FLAG_INSISTENT or Notification.FLAG_SHOW_LIGHTS

        try { NotificationManagerCompat.from(context).notify(id.toInt(), notification) }
        catch (_: SecurityException) { }
    }
}
