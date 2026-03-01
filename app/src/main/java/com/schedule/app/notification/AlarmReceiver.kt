package com.schedule.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.schedule.app.data.CourseEvent
import java.io.File
import java.time.LocalTime

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_SUMMARY = "extra_summary"
        const val EXTRA_LOCATION = "extra_location"
        const val EXTRA_TEACHER = "extra_teacher"
        const val EXTRA_START_TIME = "extra_start_time"
        const val EXTRA_END_TIME = "extra_end_time"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                NotificationHelper.createNotificationChannel(context)
                val icsContent = loadSavedIcs(context) ?: return
                AlarmScheduler.scheduleForCourses(context, icsContent)
            }
            else -> {
                val summary = intent.getStringExtra(EXTRA_SUMMARY) ?: return
                val location = intent.getStringExtra(EXTRA_LOCATION) ?: ""
                val teacher = intent.getStringExtra(EXTRA_TEACHER) ?: ""
                val startTime = LocalTime.parse(intent.getStringExtra(EXTRA_START_TIME) ?: return)
                val endTime = LocalTime.parse(intent.getStringExtra(EXTRA_END_TIME) ?: return)
                val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, summary.hashCode())

                val course = CourseEvent(
                    summary = summary,
                    startTime = startTime,
                    endTime = endTime,
                    location = location,
                    teacher = teacher,
                    section = "",
                )
                NotificationHelper.postCourseNotification(context, course, notifId)
            }
        }
    }

    private fun loadSavedIcs(context: Context): String? {
        val file = File(context.filesDir, "schedule.ics")
        return if (file.exists()) file.readText() else null
    }
}
