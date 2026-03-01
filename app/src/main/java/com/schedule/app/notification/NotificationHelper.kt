package com.schedule.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.schedule.app.MainActivity
import com.schedule.app.R
import com.schedule.app.data.CourseEvent
import com.schedule.app.util.LocationFormatter
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

        val focusIslandJson = buildFocusIslandJson(context, course)
        val timeRange = "${course.startTime.format(timeFormatter)} - ${course.endTime.format(timeFormatter)}"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(course.summary)
            .setContentText("$timeRange  ${course.location}")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
            .also { notif ->
                notif.extras.putString("miui.focus.param", focusIslandJson)
            }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, notification)
    }

    private fun buildFocusIslandJson(context: Context, course: CourseEvent): String {
        val courseName = course.summary
        val islandLeft = if (courseName.length > 5) courseName.substring(0, 5) else courseName
        val islandRight = LocationFormatter.toIslandText(course.location)
        val timeRange = "${course.startTime.format(timeFormatter)} - ${course.endTime.format(timeFormatter)}"
        val expandedLocation = LocationFormatter.removeCampusPrefix(course.location)

        val intentUri = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }.toUri(Intent.URI_INTENT_SCHEME)

        val bigIslandArea = JSONObject().apply {
            put("leftContent", islandLeft)
            put("rightContent", islandRight)
        }

        val baseInfo = JSONObject().apply {
            put("type", 2)
            put("title", courseName)
            put("content", course.teacher)
        }

        val hintInfo = JSONObject().apply {
            put("type", 2)
            put("title", timeRange)
            put("content", "时间")
            put("subTitle", expandedLocation)
            put("subContent", "地点")
        }

        val actionInfo = JSONObject().apply {
            put("text", "详情")
            put("intent", intentUri)
        }

        val paramV2 = JSONObject().apply {
            put("bigIslandArea", bigIslandArea)
            put("baseInfo", baseInfo)
            put("hintInfo", hintInfo)
            put("actionInfo", actionInfo)
        }

        return JSONObject().apply {
            put("protocol", 3)
            put("param_v2", paramV2)
        }.toString()
    }
}
