package moe.lsgtky.leafisland.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.SystemClock
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.tyme.solar.SolarDay
import moe.lsgtky.leafisland.R
import moe.lsgtky.leafisland.data.IcsParser
import moe.lsgtky.leafisland.util.SettingsStore
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class ScheduleWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val ACTION_WIDGET_UPDATE = "moe.lsgtky.leafisland.ACTION_WIDGET_UPDATE"
        private const val ALARM_REQUEST_CODE = 0x57_1D_6E_70
        private val HH_MM = DateTimeFormatter.ofPattern("HH:mm")

        fun triggerUpdate(context: Context) {
            val intent = Intent(context, ScheduleWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, ScheduleWidgetProvider::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (id in appWidgetIds) {
            try {
                val views = buildRemoteViews(context)
                appWidgetManager.updateAppWidget(id, views)
            } catch (_: Exception) {
                val views = RemoteViews(context.packageName, R.layout.widget_schedule)
                views.setTextViewText(R.id.widget_info_bottom, "小部件加载失败")
                appWidgetManager.updateAppWidget(id, views)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_UPDATE) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, ScheduleWidgetProvider::class.java))
            onUpdate(context, mgr, ids)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleMinuteAlarm(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelMinuteAlarm(context)
    }

    private fun buildRemoteViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_schedule)

        val timeSize = SettingsStore.getWidgetTimeSize(context)
        val textColorStr = SettingsStore.getWidgetTextColor(context)
        val textColor = try {
            Color.parseColor(textColorStr)
        } catch (_: Exception) {
            Color.WHITE
        }
        val infoAbove = SettingsStore.getWidgetInfoAbove(context)
        val infoSpacing = SettingsStore.getWidgetInfoSpacing(context)
        val topPadding = SettingsStore.getWidgetTopPadding(context)

        // Top padding on the clock
        views.setViewPadding(R.id.widget_time, 0, dpToPx(context, topPadding), 0, 0)

        // Time style
        val timeWeight = SettingsStore.getWidgetTimeWeight(context)
        val infoWeight = SettingsStore.getWidgetInfoWeight(context)
        views.setTextViewTextSize(R.id.widget_time, TypedValue.COMPLEX_UNIT_SP, timeSize.toFloat())
        views.setTextColor(R.id.widget_time, textColor)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                views.setInt(R.id.widget_time, "setTextFontWeight", timeWeight)
            } catch (_: Exception) { }
        }

        // Info line
        val infoText = try {
            buildInfoLine(context)
        } catch (_: Exception) {
            val today = LocalDate.now()
            "${today.monthValue}月${today.dayOfMonth}日"
        }

        // Show info in top or bottom position
        if (infoAbove) {
            views.setViewVisibility(R.id.widget_info_top, View.VISIBLE)
            views.setViewVisibility(R.id.widget_info_bottom, View.GONE)
            views.setTextViewText(R.id.widget_info_top, infoText)
            views.setTextColor(R.id.widget_info_top, textColor)
            views.setViewPadding(R.id.widget_info_top, 0, 0, 0, dpToPx(context, infoSpacing))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    views.setInt(R.id.widget_info_top, "setTextFontWeight", infoWeight)
                } catch (_: Exception) { }
            }
        } else {
            views.setViewVisibility(R.id.widget_info_top, View.GONE)
            views.setViewVisibility(R.id.widget_info_bottom, View.VISIBLE)
            views.setTextViewText(R.id.widget_info_bottom, infoText)
            views.setTextColor(R.id.widget_info_bottom, textColor)
            views.setViewPadding(R.id.widget_info_bottom, 0, dpToPx(context, infoSpacing), 0, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    views.setInt(R.id.widget_info_bottom, "setTextFontWeight", infoWeight)
                } catch (_: Exception) { }
            }
        }

        return views
    }

    private fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    private fun buildInfoLine(context: Context): String {
        val now = LocalTime.now()
        val today = LocalDate.now()
        val dateStr = "${today.monthValue}月${today.dayOfMonth}日"

        val icsFile = File(context.filesDir, "schedule.ics")
        if (!icsFile.exists()) {
            return "$dateStr · ${getLunarStr(today)}"
        }

        val icsContent = icsFile.readText()
        val todayCourses = IcsParser.parse(icsContent, today)
        val courseChars = SettingsStore.getWidgetCourseChars(context)
        val advanceMin = SettingsStore.getWidgetAdvanceMinutes(context)

        // 1. Currently in class?
        val current = todayCourses.firstOrNull { now >= it.startTime && now <= it.endTime }
        if (current != null) {
            val next = todayCourses.firstOrNull { it.startTime > now }
            if (next != null) {
                val minutesUntil = Duration.between(now, next.startTime).toMinutes()
                if (minutesUntil <= advanceMin) {
                    return "$dateStr · ${truncate(next.summary, courseChars)} · ${next.startTime.format(HH_MM)}"
                }
            }
            return "$dateStr · ${truncate(current.summary, courseChars)}"
        }

        // 2. Today has upcoming class?
        val next = todayCourses.firstOrNull { it.startTime > now }
        if (next != null) {
            return "$dateStr · ${truncate(next.summary, courseChars)} · ${next.startTime.format(HH_MM)}"
        }

        // 3. Tomorrow has class?
        val tomorrow = today.plusDays(1)
        val tomorrowCourses = IcsParser.parse(icsContent, tomorrow)
        val tomorrowFirst = tomorrowCourses.firstOrNull()
        if (tomorrowFirst != null) {
            return "$dateStr · ${truncate(tomorrowFirst.summary, courseChars)} · 明天${tomorrowFirst.startTime.format(HH_MM)}"
        }

        // 4. No courses — show lunar
        return "$dateStr · ${getLunarStr(today)}"
    }

    private fun truncate(name: String, maxChars: Int): String {
        return if (name.length > maxChars) {
            name.take(maxChars) + "…"
        } else {
            name
        }
    }

    private fun getLunarStr(date: LocalDate): String {
        return try {
            val solarDay = SolarDay.fromYmd(date.year, date.monthValue, date.dayOfMonth)
            val lunarDay = solarDay.getLunarDay()
            val lunarMonth = lunarDay.getLunarMonth()
            val yearName = lunarMonth.getLunarYear().getSixtyCycle().getName()
            "${yearName}年${lunarMonth.getName()}${lunarDay.getName()}"
        } catch (_: Exception) {
            ""
        }
    }

    private fun scheduleMinuteAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ScheduleWidgetProvider::class.java).apply {
            action = ACTION_WIDGET_UPDATE
        }
        val pi = PendingIntent.getBroadcast(
            context, ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setRepeating(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 60_000,
            60_000,
            pi,
        )
    }

    private fun cancelMinuteAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ScheduleWidgetProvider::class.java).apply {
            action = ACTION_WIDGET_UPDATE
        }
        val pi = PendingIntent.getBroadcast(
            context, ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pi)
    }
}
