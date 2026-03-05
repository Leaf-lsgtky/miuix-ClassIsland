package moe.lsgtky.leafisland.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.SystemClock
import android.util.TypedValue
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
        private const val ALARM_REQUEST_CODE = 0x57_1D_6E_70 // "widget"
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
        val views = buildRemoteViews(context)
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, views)
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
        val timeWeight = SettingsStore.getWidgetTimeWeight(context)
        val textColorStr = SettingsStore.getWidgetTextColor(context)
        val textColor = try {
            Color.parseColor(textColorStr)
        } catch (_: Exception) {
            Color.WHITE
        }

        // Time style
        views.setTextViewTextSize(R.id.widget_time, TypedValue.COMPLEX_UNIT_SP, timeSize.toFloat())
        views.setTextColor(R.id.widget_time, textColor)
        // Bold via textStyle: RemoteViews doesn't support setTypeface directly,
        // but we set bold in XML. For weight control we use the textFontWeight attribute via setInt.
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            views.setInt(R.id.widget_time, "setTextFontWeight", timeWeight)
        }

        // Info line
        val infoText = buildInfoLine(context)
        views.setTextViewText(R.id.widget_info, infoText)
        views.setTextColor(R.id.widget_info, textColor)

        return views
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
            return "$dateStr · ${truncate(current.summary, courseChars)}"
        }

        // 2. Next class within advance time?
        val next = todayCourses.firstOrNull { it.startTime > now }
        if (next != null) {
            val minutesUntil = Duration.between(now, next.startTime).toMinutes()
            if (minutesUntil <= advanceMin) {
                return "$dateStr · ${truncate(next.summary, courseChars)} · ${next.startTime.format(HH_MM)}"
            }
        }

        // 3. Check tomorrow
        val tomorrow = today.plusDays(1)
        val tomorrowCourses = IcsParser.parse(icsContent, tomorrow)
        if (tomorrowCourses.isNotEmpty()) {
            // Today has no more upcoming classes but tomorrow does — still show lunar
            // (per requirement: show lunar only if "找到第二天都没课")
            // Actually, today might still have upcoming classes beyond advance time
            // Show lunar since no imminent class
        }

        if (todayCourses.isEmpty() && tomorrowCourses.isEmpty()) {
            return "$dateStr · ${getLunarStr(today)}"
        }

        // Has courses but none imminent — show lunar
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
        val solarDay = SolarDay.fromYmd(date.year, date.monthValue, date.dayOfMonth)
        val lunarDay = solarDay.getLunarDay()
        return "农历${lunarDay.getLunarMonth().getName()}${lunarDay.getName()}"
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
