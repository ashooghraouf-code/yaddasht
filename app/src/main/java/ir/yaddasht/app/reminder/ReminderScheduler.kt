package ir.yaddasht.app.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast

object ReminderScheduler {
    const val CHANNEL_ID = "yaddasht_reminders"
    const val EXTRA_NOTE_ID = "note_id"
    const val EXTRA_TITLE = "title"
    private const val TAG = "ReminderScheduler"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID, 
                    "یادآورهای یادداشت", 
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "اعلان یادآور یادداشت‌ها"
                    enableVibration(true)
                }
                nm.createNotificationChannel(channel)
                Log.d(TAG, "Notification channel created")
            }
        }
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun schedule(context: Context, noteId: Long, title: String, timeMillis: Long) {
        ensureChannel(context)
        
        // بررسی permission برای Android 12+
        if (!canScheduleExactAlarms(context)) {
            Log.e(TAG, "Cannot schedule exact alarms - permission denied")
            Toast.makeText(context, "دسترسی یادآور داده نشده است", Toast.LENGTH_LONG).show()
            return
        }
        
        // بررسی زمان
        if (timeMillis <= System.currentTimeMillis()) {
            Log.w(TAG, "Reminder time is in the past: $timeMillis")
            Toast.makeText(context, "زمان یادآور گذشته است!", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(EXTRA_NOTE_ID, noteId)
            .putExtra(EXTRA_TITLE, title.ifBlank { "یادداشت" })
        
        val pi = PendingIntent.getBroadcast(
            context, 
            noteId.toInt(), 
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6+
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, 
                    timeMillis, 
                    pi
                )
            } else {
                // Android 5 و پایین‌تر
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP, 
                    timeMillis, 
                    pi
                )
            }
            
            Log.d(TAG, "Reminder scheduled for note $noteId at $timeMillis")
            Toast.makeText(context, "یادآور تنظیم شد ", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while scheduling alarm", e)
            Toast.makeText(context, "خطا در تنظیم یادآور", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Exception while scheduling alarm", e)
            Toast.makeText(context, "خطا در تنظیم یادآور", Toast.LENGTH_SHORT).show()
        }
    }

    fun cancel(context: Context, noteId: Long) {
        val pi = PendingIntent.getBroadcast(
            context, 
            noteId.toInt(),
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        
        if (pi != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pi)
            pi.cancel()
            Log.d(TAG, "Reminder cancelled for note $noteId")
        }
    }
}
