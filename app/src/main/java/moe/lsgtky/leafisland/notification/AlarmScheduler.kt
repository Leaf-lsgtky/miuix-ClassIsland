package moe.lsgtky.leafisland.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import moe.lsgtky.leafisland.data.IcsParser
import moe.lsgtky.leafisland.data.ScheduledPush
import moe.lsgtky.leafisland.util.SettingsStore
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object AlarmScheduler {

    const val ACTION_SCHEDULED_PUSH = "moe.lsgtky.leafisland.ACTION_SCHEDULED_PUSH"

    // --- Course alarms ---

    fun scheduleForCourses(context: Context, icsContent: String) {
        cancelAllTrackedAlarms(context)

        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val advanceMinutes = SettingsStore.getAdvanceMinutes(context).toLong()
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        val courses = IcsParser.parseWithDates(icsContent, today) +
                IcsParser.parseWithDates(icsContent, tomorrow)

        val newCodes = mutableSetOf<Int>()

        for (scheduled in courses) {
            val alarmDateTime = LocalDateTime.of(scheduled.date, scheduled.event.startTime)
                .minusMinutes(advanceMinutes)

            val alarmEpochMillis = alarmDateTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            if (alarmEpochMillis <= System.currentTimeMillis()) continue

            val requestCode = generateRequestCode(scheduled.date, scheduled.event.startTime.toString(), scheduled.event.summary)
            newCodes.add(requestCode)

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

        SettingsStore.setScheduledRequestCodes(context, newCodes)
    }

    private fun cancelAllTrackedAlarms(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val savedCodes = SettingsStore.getScheduledRequestCodes(context)
        for (code in savedCodes) {
            val intent = Intent(context, AlarmReceiver::class.java)
            val mainPending = PendingIntent.getBroadcast(
                context, code, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            mainPending?.let { alarmManager.cancel(it) }

            val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_DISMISS
            }
            val dismissPending = PendingIntent.getBroadcast(
                context, code + 1, dismissIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            dismissPending?.let { alarmManager.cancel(it) }
        }
        SettingsStore.clearScheduledRequestCodes(context)
    }

    // --- Scheduled push alarms ---

    fun scheduleAllPushAlarms(context: Context) {
        val pushes = SettingsStore.getScheduledPushes(context)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val tomorrow = today.plusDays(1)

        for (push in pushes) {
            val todayAlarmTime = LocalDateTime.of(today, LocalTime.of(push.hour, push.minute))
            if (todayAlarmTime.isAfter(now)) {
                scheduleSinglePush(context, alarmManager, push, todayAlarmTime)
            }
            val tomorrowAlarmTime = LocalDateTime.of(tomorrow, LocalTime.of(push.hour, push.minute))
            scheduleSinglePush(context, alarmManager, push, tomorrowAlarmTime)
        }
    }

    fun cancelAllPushAlarms(context: Context) {
        val pushes = SettingsStore.getScheduledPushes(context)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        for (push in pushes) {
            for (date in listOf(today, tomorrow)) {
                val requestCode = generatePushRequestCode(push.id, date)
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_SCHEDULED_PUSH
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
                )
                pendingIntent?.let { alarmManager.cancel(it) }
            }
        }
    }

    private fun scheduleSinglePush(
        context: Context,
        alarmManager: AlarmManager,
        push: ScheduledPush,
        alarmDateTime: LocalDateTime,
    ) {
        val epochMillis = alarmDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val requestCode = generatePushRequestCode(push.id, alarmDateTime.toLocalDate())

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SCHEDULED_PUSH
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        scheduleExact(alarmManager, epochMillis, pendingIntent)
    }

    private fun generatePushRequestCode(pushId: Long, date: LocalDate): Int {
        return "push_${pushId}_$date".hashCode()
    }

    // --- Common ---

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
