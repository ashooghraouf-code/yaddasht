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

enum class LeadTime(val minutes: Int, val label: String, val typeCode: Int) {
    NONE(0, "بدون پیش‌یادآوری", 0),
    MIN_10(10, "۱۰ دقیقه قبل", 1),
    HOUR_1(60, "۱ ساعت قبل", 2),
    HOUR_3(180, "۳ ساعت قبل", 3),
    DAY_1(1440, "۱ روز قبل", 4);
}

object ReminderScheduler {
    const val CHANNEL_ID = "yaddasht_reminders_alarm_v2"
    const val EXTRA_NOTE_ID = "note_id"
    const val EXTRA_TITLE = "title"
    const val EXTRA_IS_TASK = "is_task"
    const val EXTRA_LEAD_TYPE = "lead_type"
    const val EXTRA_LEAD_MINUTES = "lead_minutes"

    private fun alarmSound(context: Context) =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.deleteNotificationChannel("yaddasht_reminders")
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
                val ch = NotificationChannel(CHANNEL_ID, "یادآورهای زنگ‌دار", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "زنگ گوشی و ویبره قوی برای یادآورها"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 800, 400, 800, 400, 800, 400, 800)
                    alarmSound(context)?.let { setSound(it, attrs) }
                    setShowBadge(true); enableLights(true)
                }
                nm.createNotificationChannel(ch)
            }
        }
    }

    fun schedule(context: Context, noteId: Long, title: String, timeMillis: Long) =
        scheduleMulti(context, noteId, title, timeMillis, false, setOf(LeadTime.NONE))

    fun schedule(context: Context, id: Long, title: String, timeMillis: Long, isTask: Boolean) =
        scheduleMulti(context, id, title, timeMillis, isTask, setOf(LeadTime.NONE))

    fun scheduleMulti(
        context: Context,
        id: Long,
        title: String,
        timeMillis: Long,
        isTask: Boolean = false,
        leads: Set<LeadTime>
    ) {
        ensureChannel(context)
        val leadsToSchedule = if (leads.isEmpty()) setOf(LeadTime.NONE) else leads
        val now = System.currentTimeMillis()
        leadsToSchedule.forEach { lead ->
            val triggerTime = if (lead == LeadTime.NONE) timeMillis else timeMillis - lead.minutes * 60_000L
            if (triggerTime <= now) return@forEach
            val intent = Intent(context, ReminderReceiver::class.java)
                .putExtra(EXTRA_NOTE_ID, id)
                .putExtra(EXTRA_TITLE, title.ifBlank { if (isTask) "وظیفه" else "یادداشت" })
                .putExtra(EXTRA_IS_TASK, isTask)
                .putExtra(EXTRA_LEAD_TYPE, lead.typeCode)
                .putExtra(EXTRA_LEAD_MINUTES, lead.minutes)
            val code = computeCode(id, isTask, lead.typeCode)
            val pi = PendingIntent.getBroadcast(context, code, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val am = context.getSystemService(AlarmManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi)
            else am.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pi)
        }
    }

    private fun computeCode(id: Long, isTask: Boolean, leadType: Int): Int {
        val base = if (isTask) id.toInt() + 1_000_000 else id.toInt()
        return base + leadType * 10_000_000
    }

    fun cancel(context: Context, noteId: Long) = cancelAll(context, noteId, false)

    fun cancel(context: Context, id: Long, isTask: Boolean) = cancelAll(context, id, isTask)

    fun cancelAll(context: Context, id: Long, isTask: Boolean = false) {
        val am = context.getSystemService(AlarmManager::class.java)
        LeadTime.values().forEach { lead ->
            val code = computeCode(id, isTask, lead.typeCode)
            val pi = PendingIntent.getBroadcast(context, code,
                Intent(context, ReminderReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            if (pi != null) { am.cancel(pi); pi.cancel() }
        }
    }
}
