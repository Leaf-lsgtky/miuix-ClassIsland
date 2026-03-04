package moe.lsgtky.leafisland.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import moe.lsgtky.leafisland.data.CourseEvent
import moe.lsgtky.leafisland.data.IcsParser
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_SUMMARY = "extra_summary"
        const val EXTRA_LOCATION = "extra_location"
        const val EXTRA_TEACHER = "extra_teacher"
        const val EXTRA_SECTION = "extra_section"
        const val EXTRA_START_TIME = "extra_start_time"
        const val EXTRA_END_TIME = "extra_end_time"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val ACTION_DISMISS = "moe.lsgtky.leafisland.ACTION_DISMISS_NOTIFICATION"
        const val EXTRA_DISMISS_MINUTES = "extra_dismiss_minutes"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                NotificationHelper.createNotificationChannel(context)
                val icsContent = loadSavedIcs(context) ?: return
                AlarmScheduler.scheduleForCourses(context, icsContent)
                AlarmScheduler.scheduleAllPushAlarms(context)
            }
            ACTION_DISMISS -> {
                val manager = context.getSystemService(android.app.NotificationManager::class.java)
                manager.cancel(NotificationHelper.NOTIFICATION_ID)
            }
            AlarmScheduler.ACTION_SCHEDULED_PUSH -> {
                handleScheduledPush(context, intent)
            }
            else -> {
                val summary = intent.getStringExtra(EXTRA_SUMMARY) ?: return
                val location = intent.getStringExtra(EXTRA_LOCATION) ?: ""
                val teacher = intent.getStringExtra(EXTRA_TEACHER) ?: ""
                val section = intent.getStringExtra(EXTRA_SECTION) ?: ""
                val startTime = LocalTime.parse(intent.getStringExtra(EXTRA_START_TIME) ?: return)
                val endTime = LocalTime.parse(intent.getStringExtra(EXTRA_END_TIME) ?: return)

                val course = CourseEvent(
                    summary = summary,
                    startTime = startTime,
                    endTime = endTime,
                    location = location,
                    teacher = teacher,
                    section = section,
                )
                NotificationHelper.postCourseNotification(context, course)
            }
        }
    }

    private fun handleScheduledPush(context: Context, intent: Intent) {
        val icsContent = loadSavedIcs(context) ?: return
        val now = LocalTime.now()
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        val todayCourses = IcsParser.parse(icsContent, today)
        val tomorrowCourses = IcsParser.parse(icsContent, tomorrow)

        val nextCourse = todayCourses.firstOrNull { it.startTime > now }
            ?: tomorrowCourses.firstOrNull()

        if (nextCourse != null) {
            NotificationHelper.postCourseNotification(context, nextCourse)

            // Schedule auto-dismiss
            val dismissMinutes = intent.getIntExtra(EXTRA_DISMISS_MINUTES, 30)
            AlarmScheduler.scheduleDismiss(context, dismissMinutes.toLong())
        }

        // Reschedule push alarms for next cycle
        AlarmScheduler.scheduleAllPushAlarms(context)
    }

    private fun loadSavedIcs(context: Context): String? {
        val file = File(context.filesDir, "schedule.ics")
        return if (file.exists()) file.readText() else null
    }
}
