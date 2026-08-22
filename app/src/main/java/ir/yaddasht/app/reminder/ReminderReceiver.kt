package ir.yaddasht.app.reminder

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
        ReminderScheduler.ensureChannel(context)

        val open = Intent(context, MainActivity::class.java)
            .putExtra("note_id", noteId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(context, noteId.toInt(), open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // ✅ زنگ آلارم گوشی + لرزش پله‌ای + اولویت بیشینه
        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ یادآور: $title")
            .setContentText("وقتشه! برای دیدن یادداشتت ضربه بزن.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("وقتشه! برای دیدن یادداشتت ضربه بزن."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setSound(ReminderScheduler.reminderSoundUri(context))
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(noteId.toInt(), notification)
        } catch (_: SecurityException) { }
    }
}
