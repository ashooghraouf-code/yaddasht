package ir.yaddasht.app.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build

object ReminderScheduler {
    // ✅ شناسه جدید کانال تا تنظیمات صدا روی همهٔ گوشی‌ها اعمال شود
    const val CHANNEL_ID = "yaddasht_reminder_ring_v2"
    const val EXTRA_NOTE_ID = "note_id"
    const val EXTRA_TITLE = "title"

    fun reminderSoundUri(context: Context) =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID, "زنگ یادآور 🔔", NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "پخش زنگ و لرزش برای یادآورها"
                    enableVibration(true)
                    enableLights(true)
                    val attrs = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                    setSound(reminderSoundUri(context), attrs)
                }
                nm.createNotificationChannel(channel)
            }
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
