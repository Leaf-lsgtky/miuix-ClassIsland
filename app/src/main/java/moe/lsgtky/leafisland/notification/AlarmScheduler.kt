package moe.lsgtky.leafisland.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import moe.lsgtky.leafisland.data.IcsParser
import moe.lsgtky.leafisland.util.SettingsStore
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object AlarmScheduler {

    fun scheduleForCourses(context: Context, icsContent: String) {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val advanceMinutes = SettingsStore.getAdvanceMinutes(context).toLong()
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        val courses = IcsParser.parseWithDates(icsContent, today) +
                IcsParser.parseWithDates(icsContent, tomorrow)

        for (scheduled in courses) {
            val alarmDateTime = LocalDateTime.of(scheduled.date, scheduled.event.startTime)
                .minusMinutes(advanceMinutes)

            val alarmEpochMillis = alarmDateTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            if (alarmEpochMillis <= System.currentTimeMillis()) continue

            val requestCode = generateRequestCode(scheduled.date, scheduled.event.startTime.toString(), scheduled.event.summary)

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(AlarmReceiver.EXTRA_SUMMARY, scheduled.event.summary)
                putExtra(AlarmReceiver.EXTRA_LOCATION, scheduled.event.location)
                putExtra(AlarmReceiver.EXTRA_TEACHER, scheduled.event.teacher)
                putExtra(AlarmReceiver.EXTRA_SECTION, scheduled.event.section)
                putExtra(AlarmReceiver.EXTRA_START_TIME, scheduled.event.startTime.toString())
                putExtra(AlarmReceiver.EXTRA_END_TIME, scheduled.event.endTime.toString())
                putExtra(AlarmReceiver.EXTRA_NOTIFICATION_ID, requestCode)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            scheduleExact(alarmManager, alarmEpochMillis, pendingIntent)

            // Schedule auto-dismiss at startTime + 5 minutes
            val dismissTime = LocalDateTime.of(scheduled.date, scheduled.event.startTime)
                .plusMinutes(5)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            if (dismissTime > System.currentTimeMillis()) {
                val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
                    action = AlarmReceiver.ACTION_DISMISS
                    putExtra(AlarmReceiver.EXTRA_NOTIFICATION_ID, requestCode)
                }
                val dismissPending = PendingIntent.getBroadcast(
                    context,
                    requestCode + 1,
                    dismissIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                scheduleExact(alarmManager, dismissTime, dismissPending)
            }
        }
    }

    fun cancelForCourses(context: Context, icsContent: String) {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        val courses = IcsParser.parseWithDates(icsContent, today) +
                IcsParser.parseWithDates(icsContent, tomorrow)

        for (scheduled in courses) {
            val requestCode = generateRequestCode(scheduled.date, scheduled.event.startTime.toString(), scheduled.event.summary)
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            pendingIntent?.let { alarmManager.cancel(it) }
        }
    }

    private fun generateRequestCode(date: LocalDate, time: String, summary: String): Int {
        return ("$date$time$summary").hashCode()
    }

    private fun scheduleExact(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }
}
