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
        val noteId = intent.getLongExtra(ReminderScheduler.EXTRA_NOTE_ID, -1L)
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "یادداشت"
        val isTask = intent.getBooleanExtra(ReminderScheduler.EXTRA_IS_TASK, false)
        val leadType = intent.getIntExtra(ReminderScheduler.EXTRA_LEAD_TYPE, 0)
        ReminderScheduler.ensureChannel(context)
        val open = Intent(context, MainActivity::class.java)
            .putExtra("note_id", noteId).putExtra("is_task", isTask)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val baseCode = if (isTask) noteId.toInt() + 1_000_000 else noteId.toInt()
        val pi = PendingIntent.getActivity(context, baseCode + leadType * 10_000_000, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val kind = if (isTask) "وظیفه" else "یادآور"
        val (emoji, headLine) = when (leadType) {
            6 -> "🗓️" to "یک هفته مونده: $title"
            4 -> "⏳" to "فردا: $title"
            3 -> "🔔" to "۳ ساعت مونده تا $kind"
            2 -> "🔔" to "یک ساعت مونده تا $kind"
            5 -> "⏱️" to "۳۰ دقیقه مونده تا $kind"
            1 -> "⚡" to "۱۰ دقیقه مونده تا $kind"
            else -> (if (isTask) "✅" else "⏰") to "وقتِ $kind: $title"
        }
        val body = when (leadType) {
            0 -> "برای دیدن ضربه بزن."
            6 -> "یک هفته تا $kind مونده: $title"
            4 -> "فردا باید انجام بشه: $title"
            3 -> "فقط ۳ ساعت مونده. آماده‌ش کن!"
            2 -> "فقط یک ساعت مونده. آماده‌ش کن!"
            5 -> "۳۰ دقیقه دیگه وقتشه!"
            1 -> "کم مونده! ۱۰ دقیقه دیگه."
            else -> ""
        }
        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("$emoji $headLine")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true).setContentIntent(pi)
            .setVibrate(longArrayOf(0, 800, 400, 800, 400, 800, 400, 800))
            .setDefaults(Notification.DEFAULT_ALL).build()
        notification.flags = notification.flags or Notification.FLAG_INSISTENT or Notification.FLAG_SHOW_LIGHTS
        val notificationId = baseCode + leadType * 10_000_000
        try { NotificationManagerCompat.from(context).notify(notificationId, notification) } catch (_: SecurityException) { }
    }
}
