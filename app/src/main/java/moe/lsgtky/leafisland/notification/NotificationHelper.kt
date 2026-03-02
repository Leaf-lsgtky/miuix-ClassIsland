package moe.lsgtky.leafisland.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import moe.lsgtky.leafisland.MainActivity
import moe.lsgtky.leafisland.R
import moe.lsgtky.leafisland.data.CourseEvent
import moe.lsgtky.leafisland.util.LocationFormatter
import org.json.JSONObject
import java.time.format.DateTimeFormatter

object NotificationHelper {

    const val CHANNEL_ID = "course_reminders"
    private const val CHANNEL_NAME = "课程提醒"
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "上课前提醒通知"
            enableLights(true)
            enableVibration(true)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun postCourseNotification(context: Context, course: CourseEvent, notificationId: Int) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val timeRange = "${course.startTime.format(timeFormatter)} - ${course.endTime.format(timeFormatter)}"
        val focusBundle = buildFocusBundle(context, course, pendingIntent)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(course.summary)
            .setContentText("$timeRange  ${course.location}")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
            .also { notif ->
                notif.extras.putAll(focusBundle)
            }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, notification)
    }

    fun cancelAllNotifications(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancelAll()
    }

    private fun buildFocusBundle(
        context: Context,
        course: CourseEvent,
        pendingIntent: PendingIntent,
    ): Bundle {
        val bundle = Bundle()
        val courseName = course.summary
        val timeRange = "${course.startTime.format(timeFormatter)} - ${course.endTime.format(timeFormatter)}"
        val expandedLocation = LocationFormatter.removeCampusPrefix(course.location)

        // 1. Build param.custom JSON (basic fields + param_island for 摘要态)
        val customParam = buildCustomParamJson(context, course)
        bundle.putString("miui.focus.param.custom", customParam)

        // 2. Focus notification main RemoteViews (light)
        val rvLight = RemoteViews(context.packageName, R.layout.layout_focus).apply {
            setTextViewText(R.id.focus_title, courseName)
            setTextViewText(R.id.focus_time, timeRange)
            setTextViewText(R.id.focus_location, expandedLocation)
            if (course.section.isNotBlank()) {
                setTextViewText(R.id.focus_section, course.section)
                setViewVisibility(R.id.focus_section_row, View.VISIBLE)
            } else {
                setViewVisibility(R.id.focus_section_row, View.GONE)
            }
            if (course.teacher.isNotBlank()) {
                setTextViewText(R.id.focus_teacher, course.teacher)
                setViewVisibility(R.id.focus_teacher_row, View.VISIBLE)
            } else {
                setViewVisibility(R.id.focus_teacher_row, View.GONE)
            }
        }
        bundle.putParcelable("miui.focus.rv", rvLight)

        // 3. Focus notification main RemoteViews (dark)
        val rvNight = RemoteViews(context.packageName, R.layout.layout_focus_night).apply {
            setTextViewText(R.id.focus_title, courseName)
            setTextViewText(R.id.focus_time, timeRange)
            setTextViewText(R.id.focus_location, expandedLocation)
            if (course.section.isNotBlank()) {
                setTextViewText(R.id.focus_section, course.section)
                setViewVisibility(R.id.focus_section_row, View.VISIBLE)
            } else {
                setViewVisibility(R.id.focus_section_row, View.GONE)
            }
            if (course.teacher.isNotBlank()) {
                setTextViewText(R.id.focus_teacher, course.teacher)
                setViewVisibility(R.id.focus_teacher_row, View.VISIBLE)
            } else {
                setViewVisibility(R.id.focus_teacher_row, View.GONE)
            }
        }
        bundle.putParcelable("miui.focus.rvNight", rvNight)

        // 4. Island expand RemoteViews
        val rvIslandExpand = buildIslandExpandRemoteViews(context, course, pendingIntent)
        bundle.putParcelable("miui.focus.rv.island.expand", rvIslandExpand)

        // 5. Ticker text
        bundle.putString("miui.focus.ticker", "课程提醒：$courseName")

        return bundle
    }

    private fun buildIslandExpandRemoteViews(
        context: Context,
        course: CourseEvent,
        pendingIntent: PendingIntent,
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.layout_island_expand)
        val timeRange = "${course.startTime.format(timeFormatter)} - ${course.endTime.format(timeFormatter)}"
        val expandedLocation = LocationFormatter.removeCampusPrefix(course.location)

        rv.setTextViewText(R.id.island_course_name, course.summary)
        rv.setTextViewText(R.id.island_time, timeRange)
        rv.setTextViewText(R.id.island_location, expandedLocation)

        if (course.section.isNotBlank()) {
            rv.setTextViewText(R.id.island_section, course.section)
            rv.setViewVisibility(R.id.island_section_row, View.VISIBLE)
        } else {
            rv.setViewVisibility(R.id.island_section_row, View.GONE)
        }

        if (course.teacher.isNotBlank()) {
            rv.setTextViewText(R.id.island_teacher, course.teacher)
            rv.setViewVisibility(R.id.island_teacher_row, View.VISIBLE)
        } else {
            rv.setViewVisibility(R.id.island_teacher_row, View.GONE)
        }

        rv.setOnClickPendingIntent(R.id.island_expand_root, pendingIntent)

        return rv
    }

    private fun buildCustomParamJson(context: Context, course: CourseEvent): String {
        val courseName = course.summary
        val islandLeftTitle = if (courseName.length > 5) courseName.substring(0, 5) else courseName
        val islandRightTitle = LocationFormatter.toIslandText(course.location)

        // param_island for 摘要态
        val imageTextInfoLeft = JSONObject().apply {
            put("type", 1)
            put("textInfo", JSONObject().apply {
                put("title", islandLeftTitle)
            })
        }

        val bigIslandTextInfo = JSONObject().apply {
            put("title", islandRightTitle)
            put("showHighlightColor", true)
        }

        val bigIslandArea = JSONObject().apply {
            put("imageTextInfoLeft", imageTextInfoLeft)
            put("textInfo", bigIslandTextInfo)
        }

        val paramIsland = JSONObject().apply {
            put("islandProperty", 1)
            put("bigIslandArea", bigIslandArea)
        }

        // Custom param JSON
        val customParam = JSONObject().apply {
            put("ticker", "课程提醒：$courseName")
            put("enableFloat", true)
            put("updatable", true)
            put("isShowNotification", true)
            put("timeout", 60)
            put("param_island", paramIsland)
        }

        return customParam.toString()
    }
}
