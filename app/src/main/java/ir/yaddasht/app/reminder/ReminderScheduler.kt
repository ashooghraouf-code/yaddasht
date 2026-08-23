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
    const val CHANNEL_ID = "yaddasht_reminders_alarm_v2"
    const val EXTRA_NOTE_ID = "note_id"
    const val EXTRA_TITLE = "title"
    const val EXTRA_IS_TASK = "is_task"

    private fun alarmSound(context: Context) =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = context.getSystemService(NotificationManager::class.java)
            // همیشه کانال قدیمی را حذف و بازسازی می‌کنیم تا صدا عوض شود
            nm.deleteNotificationChannel("yaddasht_reminders")
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                val ch = NotificationChannel(
                    CHANNEL_ID,
                    "یادآورهای زنگ‌دار",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "زنگ گوشی و ویبره قوی برای یادآورها"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 800, 400, 800, 400, 800, 400, 800)
                    alarmSound(context)?.let { setSound(it, attrs) }
                    setShowBadge(true)
                    enableLights(true)
                }
                nm.createNotificationChannel(ch)
            }
        }
    }

    // ✅ نسخه سازگار با کد قبلی (برای یادداشت)
    fun schedule(context: Context, noteId: Long, title: String, timeMillis: Long) {
        schedule(context, noteId, title, timeMillis, isTask = false)
    }

    // ✅ نسخه جدید با پشتیبانی از وظیفه
    fun schedule(context: Context, id: Long, title: String, timeMillis: Long, isTask: Boolean) {
        ensureChannel(context)
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(EXTRA_NOTE_ID, id)
            .putExtra(EXTRA_TITLE, title.ifBlank { if (isTask) "وظیفه" else "یادداشت" })
            .putExtra(EXTRA_IS_TASK, isTask)
        // کد یکتا برای جلوگیری از تداخل وظیفه و یادداشت
        val code = (if (isTask) id.toInt() + 1_000_000 else id.toInt())
        val pi = PendingIntent.getBroadcast(
            context, code, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pi)
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, timeMillis, pi)
        }
    }

    fun cancel(context: Context, noteId: Long) {
        cancel(context, noteId, isTask = false)
    }

    fun cancel(context: Context, id: Long, isTask: Boolean) {
        val code = (if (isTask) id.toInt() + 1_000_000 else id.toInt())
        val pi = PendingIntent.getBroadcast(
            context, code,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            context.getSystemService(AlarmManager::class.java).cancel(pi)
            pi.cancel()
        }
    }
}
