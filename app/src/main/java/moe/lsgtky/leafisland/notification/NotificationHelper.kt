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
            .build()
            .also { it.extras.putAll(focusBundle) }

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
            Log.d(TAG, "Shizuku bypass skipped: enabled=$useShizuku")
            manager.notify(notificationId, notification)
            return
        }

        val xmsfUid = try {
            context.packageManager.getPackageUid(XMSF_PACKAGE, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            manager.notify(notificationId, notification)
            return
        }

        // Must run in background to avoid blocking main thread during delay()
        Thread {
            var blocked = false
            try {
                // 1. Block network synchronously before notify
                blocked = ShizukuHelper.blockNetwork(xmsfUid, XMSF_PACKAGE)
                
                // 2. Dispatch notification
                manager.notify(notificationId, notification)

                // 3. Keep blocked for a window to bypass async scan
                if (blocked) {
                    Thread.sleep(BLIND_WINDOW_MS)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Bypass execution failed", e)
                // Fallback notify if something crashed before notify
                manager.notify(notificationId, notification)
            } finally {
                if (blocked) {
                    ShizukuHelper.unblockNetwork(xmsfUid, XMSF_PACKAGE)
                }
            }
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

        // Build V3-compatible custom param JSON
        val customParam = buildCustomParamJson(context, course)
        bundle.putString("miui.focus.param.custom", customParam)

        // Standard Focus RVs
        bundle.putParcelable("miui.focus.rv", buildBaseRemoteViews(context, course, R.layout.layout_focus))
        bundle.putParcelable("miui.focus.rvNight", buildBaseRemoteViews(context, course, R.layout.layout_focus_night))
        bundle.putParcelable("miui.focus.rvAod", buildBaseRemoteViews(context, course, R.layout.layout_focus_aod))
        bundle.putParcelable("miui.focus.rv.fullAod", buildBaseRemoteViews(context, course, R.layout.layout_focus_aod))

        // Island Expand RV - Crucial for "Super Island"
        val rvIslandExpand = buildIslandExpandRemoteViews(context, course, pendingIntent)
        bundle.putParcelable("miui.focus.rv.island.expand", rvIslandExpand)

        bundle.putString("miui.focus.ticker", "课程提醒：$courseName")
        
        // V3 Flags
        bundle.putBoolean("miui.focus.updatable", true)
        bundle.putBoolean("miui.focus.enableFloat", true)

        return bundle
    }

    private fun buildBaseRemoteViews(context: Context, course: CourseEvent, layoutId: Int): RemoteViews {
        val timeRange = "${course.startTime.format(timeFormatter)} - ${course.endTime.format(timeFormatter)}"
        return RemoteViews(context.packageName, layoutId).apply {
            setTextViewText(R.id.focus_title, course.summary)
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
