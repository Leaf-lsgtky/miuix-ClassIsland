package moe.lsgtky.leafisland.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import moe.lsgtky.leafisland.MainActivity
import moe.lsgtky.leafisland.R
import moe.lsgtky.leafisland.data.CourseEvent
import moe.lsgtky.leafisland.shizuku.ShizukuHelper
import moe.lsgtky.leafisland.util.LocationFormatter
import moe.lsgtky.leafisland.util.SettingsStore
import org.json.JSONObject
import java.time.format.DateTimeFormatter

object NotificationHelper {

    const val CHANNEL_ID = "course_reminders"
    const val NOTIFICATION_ID = 1001
    private const val CHANNEL_NAME = "课程提醒"
    private const val TAG = "NotificationHelper"
    private const val XMSF_PACKAGE = "com.xiaomi.xmsf"
    private const val BLIND_WINDOW_MS = 100L
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

    fun postCourseNotification(context: Context, course: CourseEvent) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
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
            .addExtras(focusBundle)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        notifyWithBypass(context, manager, NOTIFICATION_ID, notification)
    }

    fun cancelAllNotifications(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancelAll()
    }

    private fun notifyWithBypass(
        context: Context,
        manager: NotificationManager,
        notificationId: Int,
        notification: android.app.Notification,
    ) {
        val useShizuku = SettingsStore.isShizukuEnabled(context)
        if (!useShizuku || !ShizukuHelper.isAvailable() || !ShizukuHelper.hasPermission()) {
            Log.d(TAG, "Shizuku bypass skipped: enabled=$useShizuku, available=${ShizukuHelper.isAvailable()}, permission=${ShizukuHelper.hasPermission()}")
            manager.notify(notificationId, notification)
            return
        }

        val xmsfUid = try {
            context.packageManager.getPackageUid(XMSF_PACKAGE, PackageManager.PackageInfoFlags.of(0))
        } catch (_: PackageManager.NameNotFoundException) {
            Log.w(TAG, "XMSF package not found, skipping bypass")
            manager.notify(notificationId, notification)
            return
        }

        Log.d(TAG, "Starting Shizuku bypass for XMSF UID: $xmsfUid")
        Thread {
            var blocked = false
            try {
                blocked = ShizukuHelper.blockNetwork(xmsfUid)
                Log.d(TAG, "Network block result: $blocked")
            } catch (e: Throwable) {
                Log.w(TAG, "Network block failed: ${e.message}")
            }
            try {
                manager.notify(notificationId, notification)
                if (blocked) Thread.sleep(BLIND_WINDOW_MS)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to post notification", e)
            } finally {
                if (blocked) {
                    try {
                        ShizukuHelper.unblockNetwork(xmsfUid)
                    } catch (e: Throwable) {
                        Log.w(TAG, "Network unblock failed: ${e.message}")
                    }
                }
            }
            Log.d(TAG, "Shizuku bypass completed (blocked=$blocked)")
        }.start()
    }

    private fun buildFocusBundle(
        context: Context,
        course: CourseEvent,
        pendingIntent: PendingIntent,
    ): Bundle {
        val bundle = Bundle()
        val courseName = course.summary
        val timeRange = "${course.startTime.format(timeFormatter)} - ${course.endTime.format(timeFormatter)}"

        // 1. Build param.custom JSON (basic fields + param_island for 摘要态)
        val customParam = buildCustomParamJson(context, course)
        bundle.putString("miui.focus.param.custom", customParam)

        // 2. Focus notification main RemoteViews (light)
        val rvLight = RemoteViews(context.packageName, R.layout.layout_focus).apply {
            setTextViewText(R.id.focus_title, courseName)
            setTextViewText(R.id.focus_time, timeRange)
            setTextViewText(R.id.focus_location, course.location)
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
            setTextViewText(R.id.focus_location, course.location)
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

        // 4. Focus notification AOD RemoteViews
        val rvAod = RemoteViews(context.packageName, R.layout.layout_focus_aod).apply {
            setTextViewText(R.id.focus_title, courseName)
            setTextViewText(R.id.focus_time, timeRange)
            setTextViewText(R.id.focus_location, course.location)
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
        bundle.putParcelable("miui.focus.rvAod", rvAod)

        // 5. Full-screen AOD RemoteViews (same white-on-dark layout)
        val rvFullAod = RemoteViews(context.packageName, R.layout.layout_focus_aod).apply {
            setTextViewText(R.id.focus_title, courseName)
            setTextViewText(R.id.focus_time, timeRange)
            setTextViewText(R.id.focus_location, course.location)
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
        bundle.putParcelable("miui.focus.rv.fullAod", rvFullAod)

        // 6. Island expand RemoteViews
        val rvIslandExpand = buildIslandExpandRemoteViews(context, course, pendingIntent)
        bundle.putParcelable("miui.focus.rv.island.expand", rvIslandExpand)

        // 7. Ticker text
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

        rv.setTextViewText(R.id.island_course_name, course.summary)
        rv.setTextViewText(R.id.island_time, timeRange)
        rv.setTextViewText(R.id.island_location, course.location)

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
            put("aodTitle", courseName)
            put("enableFloat", true)
            put("updatable", true)
            put("isShowNotification", true)
            put("timeout", 60)
            put("param_island", paramIsland)
        }

        return customParam.toString()
    }
}
