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

    const val CHANNEL_ADVANCE = "course_reminders"
    const val CHANNEL_SCHEDULED = "scheduled_push"
    const val NOTIFICATION_ID = 1001
    private const val TAG = "NotificationHelper"
    private const val XMSF_PACKAGE = "com.xiaomi.xmsf"
    private const val BLIND_WINDOW_MS = 100L
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun createNotificationChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ADVANCE, "提前提醒", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "上课前提前提醒通知"
                enableLights(true)
                enableVibration(true)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_SCHEDULED, "定时推送", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "按设定时间定时推送下节课信息"
            }
        )
    }

    fun postCourseNotification(context: Context, course: CourseEvent, channelId: String = CHANNEL_ADVANCE) {
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

        val notification = NotificationCompat.Builder(context, channelId)
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

        // 1. SYNC BLOCK (Important to prevent race)
        val blocked = ShizukuHelper.blockNetwork(xmsfUid)
        
        // 2. IMMEDIATE NOTIFY
        manager.notify(notificationId, notification)

        // 3. ASYNC RESTORE
        if (blocked) {
            // Use a temporary thread or coroutine to restore network after window
            Thread {
                try {
                    Thread.sleep(BLIND_WINDOW_MS)
                } catch (_: Exception) {}
                ShizukuHelper.unblockNetwork(xmsfUid)
            }.start()
        }
    }

    private fun buildFocusBundle(
        context: Context,
        course: CourseEvent,
        pendingIntent: PendingIntent,
    ): Bundle {
        val bundle = Bundle()
        val courseName = course.summary

        // 1. Build Custom Param JSON (Custom RV mode uses miui.focus.param.custom)
        val customParam = buildCustomParamJson(context, course)
        bundle.putString("miui.focus.param.custom", customParam)

        // 2. Inject Icons Bundle (Required for Island/Ticker icons)
        val pics = Bundle().apply {
            val icon = android.graphics.drawable.Icon.createWithResource(context, R.drawable.ic_notification)
            putParcelable("pic_logo", icon)
        }
        bundle.putBundle("miui.focus.pics", pics)

        // 3. Set RemoteViews
        bundle.putParcelable("miui.focus.rv", buildBaseRemoteViews(context, course, R.layout.layout_focus))
        bundle.putParcelable("miui.focus.rvNight", buildBaseRemoteViews(context, course, R.layout.layout_focus_night))
        bundle.putParcelable("miui.focus.rvAod", buildBaseRemoteViews(context, course, R.layout.layout_focus_aod))
        bundle.putParcelable("miui.focus.rv.fullAod", buildBaseRemoteViews(context, course, R.layout.layout_focus_aod))

        // Island Expand RV
        val rvIslandExpand = buildIslandExpandRemoteViews(context, course, pendingIntent)
        bundle.putParcelable("miui.focus.rv.island.expand", rvIslandExpand)

        // 4. Legacy/Compatibility Fields
        bundle.putString("miui.focus.ticker", "课程提醒：$courseName")
        
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

        // param_island for OS3 Super Island
        val bigIslandArea = JSONObject().apply {
            // Left: Text only (Removed picInfo to hide icon in Big Island)
            put("imageTextInfoLeft", JSONObject().apply {
                put("type", 1)
                put("textInfo", JSONObject().apply {
                    put("title", islandLeftTitle)
                })
            })
            // Right: Bold Text
            put("textInfo", JSONObject().apply {
                put("title", islandRightTitle)
                put("showHighlightColor", true)
            })
        }

        val paramIsland = JSONObject().apply {
            put("islandProperty", 1)
            put("bigIslandArea", bigIslandArea)
            put("smallIslandArea", JSONObject().apply {
                put("picInfo", JSONObject().apply {
                    put("type", 1)
                    put("pic", "pic_logo")
                })
            })
        }

        // Final flat JSON for miui.focus.param.custom
        val customParam = JSONObject().apply {
            put("ticker", "课程提醒：$courseName")
            put("tickerPic", "pic_logo")
            put("aodTitle", courseName)
            put("enableFloat", true)
            put("updatable", true)
            put("isShowNotification", true)
            put("islandFirstFloat", true) // Auto-expand when first shown
            put("timeout", 60)
            put("param_island", paramIsland)
        }

        return customParam.toString()
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
}
