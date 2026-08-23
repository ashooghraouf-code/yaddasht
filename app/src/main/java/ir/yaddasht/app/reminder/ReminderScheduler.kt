package ir.yaddasht.app.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager

object ReminderScheduler {
    const val CHANNEL_ID = "yaddasht_reminders"
    const val EXTRA_NOTE_ID = "note_id"
    const val EXTRA_TITLE = "title"

    private fun reminderSound(context: Context) =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val prefs = context.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)
        // یک‌بار کانال قدیمی را حذف و با صدای زنگ بازسازی می‌کنیم
        if (!prefs.getBoolean("channel_v2", false)) {
            nm.deleteNotificationChannel(CHANNEL_ID)
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val ch = NotificationChannel(CHANNEL_ID, "یادآورهای یادداشت", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "زنگ و ویبره برای یادآورها"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 250, 600)
                reminderSound(context)?.let { setSound(it, attrs) }
                setShowBadge(true)
            }
            nm.createNotificationChannel(ch)
            prefs.edit().putBoolean("channel_v2", true).apply()
        }
    }

    fun schedule(context: Context, noteId: Long, title: String, timeMillis: Long) {
        ensureChannel(context)
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(EXTRA_NOTE_ID, noteId)
            .putExtra(EXTRA_TITLE, title.ifBlank { "یادداشت" })
        val pi = PendingIntent.getBroadcast(context, noteId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        context.getSystemService(AlarmManager::class.java)
            .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pi)
    }

    fun cancel(context: Context, noteId: Long) {
        val pi = PendingIntent.getBroadcast(context, noteId.toInt(),
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pi != null) {
            context.getSystemService(AlarmManager::class.java).cancel(pi)
            pi.cancel()
        }
    }
}
