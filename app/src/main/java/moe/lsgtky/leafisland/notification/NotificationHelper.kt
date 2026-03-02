package moe.lsgtky.leafisland.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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

    fun cancelAllNotifications(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancelAll()
    }

    private fun buildFocusIslandJson(context: Context, course: CourseEvent): String {
        val courseName = course.summary
        val islandLeftTitle = if (courseName.length > 5) courseName.substring(0, 5) else courseName
        val islandRightTitle = LocationFormatter.toIslandText(course.location)
        val startTime = course.startTime.format(timeFormatter)
        val expandedLocation = LocationFormatter.removeCampusPrefix(course.location)

        val intentUri = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }.toUri(Intent.URI_INTENT_SCHEME)

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

        val baseInfo = JSONObject().apply {
            put("type", 2)
            put("title", courseName)
            put("content", if (course.teacher.isNotBlank()) "任课教师：${course.teacher}" else "")
        }

        val actionInfo = JSONObject().apply {
            put("actionTitle", "详情")
            put("actionIntent", intentUri)
            put("actionIntentType", "2")
            put("actionBgColor", "#30000000")
        }

        val hintInfo = JSONObject().apply {
            put("type", 2)
            put("title", startTime)
            put("content", "时间")
            put("subTitle", expandedLocation)
            put("subContent", "地点")
            put("actionInfo", actionInfo)
        }

        val paramV2 = JSONObject().apply {
            put("protocol", 3)
            put("enableFloat", true)
            put("updatable", true)
            put("ticker", "课程提醒：$courseName")
            put("isShowNotification", true)
            put("baseInfo", baseInfo)
            put("hintInfo", hintInfo)
            put("param_island", paramIsland)
        }

        return JSONObject().apply {
            put("param_v2", paramV2)
        }.toString()
    }
}
