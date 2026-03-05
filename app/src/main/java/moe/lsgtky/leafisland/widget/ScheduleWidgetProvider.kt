package moe.lsgtky.leafisland.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.SystemClock
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.util.Log
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
        private const val TAG = "ScheduleWidget"
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
            } catch (e: Exception) {
                Log.e(TAG, "Widget update failed", e)
                writeErrorLog(context, e)
                val views = RemoteViews(context.packageName, R.layout.widget_schedule)
                views.setTextViewText(R.id.widget_info_bottom, "小部件加载失败: ${e.message}")
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
        val timeWeight = SettingsStore.getWidgetTimeWeight(context)
        val infoWeight = SettingsStore.getWidgetInfoWeight(context)
        val textColorStr = SettingsStore.getWidgetTextColor(context)
        val textColor = try {
            Color.parseColor(textColorStr)
        } catch (_: Exception) {
            Color.WHITE
        }
        val infoAbove = SettingsStore.getWidgetInfoAbove(context)
        val infoSpacing = SettingsStore.getWidgetInfoSpacing(context)
        val topPadding = SettingsStore.getWidgetTopPadding(context)

        // Top padding on the clock (only positive via padding)
        views.setViewPadding(R.id.widget_time, 0, dpToPx(context, topPadding.coerceAtLeast(0)), 0, 0)
        if (topPadding < 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            views.setViewLayoutMargin(
                R.id.widget_time,
                RemoteViews.MARGIN_TOP,
                topPadding.toFloat(),
                TypedValue.COMPLEX_UNIT_DIP,
            )
        }

        // Time text with font weight via SpannableString
        val timeStr = LocalTime.now().format(HH_MM)
        views.setTextViewText(R.id.widget_time, styledText(timeStr, timeWeight))
        views.setTextViewTextSize(R.id.widget_time, TypedValue.COMPLEX_UNIT_SP, timeSize.toFloat())
        views.setTextColor(R.id.widget_time, textColor)

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
            views.setTextViewText(R.id.widget_info_top, styledText(infoText, infoWeight))
            views.setTextColor(R.id.widget_info_top, textColor)
            applySpacing(context, views, R.id.widget_info_top, infoSpacing, isBottom = false)
        } else {
            views.setViewVisibility(R.id.widget_info_top, View.GONE)
            views.setViewVisibility(R.id.widget_info_bottom, View.VISIBLE)
            views.setTextViewText(R.id.widget_info_bottom, styledText(infoText, infoWeight))
            views.setTextColor(R.id.widget_info_bottom, textColor)
            applySpacing(context, views, R.id.widget_info_bottom, infoSpacing, isBottom = true)
        }

        return views
    }

    /**
     * Create a SpannableString with the specified font weight.
     * API 28+: uses TypefaceSpan(Typeface) for precise weight control.
     * Older: falls back to StyleSpan(BOLD) for weight >= 600.
     */
    private fun styledText(text: String, weight: Int): CharSequence {
        val spannable = SpannableString(text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val typeface = Typeface.create(null, weight, false)
            spannable.setSpan(
                TypefaceSpan(typeface),
                0, spannable.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        } else {
            val style = if (weight >= 600) Typeface.BOLD else Typeface.NORMAL
            spannable.setSpan(
                StyleSpan(style),
                0, spannable.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        return spannable
    }

    /**
     * Apply spacing between info text and time.
     * Positive: use padding (safe on all APIs).
     * Negative: use layout margin (API 31+) so the view actually moves, avoiding clipping.
     */
    private fun applySpacing(context: Context, views: RemoteViews, viewId: Int, spacingDp: Int, isBottom: Boolean) {
        if (spacingDp >= 0) {
            if (isBottom) {
                views.setViewPadding(viewId, 0, dpToPx(context, spacingDp), 0, 0)
            } else {
                views.setViewPadding(viewId, 0, 0, 0, dpToPx(context, spacingDp))
            }
            // Reset any previously set negative margin
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val marginAttr = if (isBottom) RemoteViews.MARGIN_TOP else RemoteViews.MARGIN_BOTTOM
                views.setViewLayoutMargin(viewId, marginAttr, 0f, TypedValue.COMPLEX_UNIT_DIP)
            }
        } else {
            views.setViewPadding(viewId, 0, 0, 0, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val marginAttr = if (isBottom) RemoteViews.MARGIN_TOP else RemoteViews.MARGIN_BOTTOM
                views.setViewLayoutMargin(
                    viewId,
                    marginAttr,
                    spacingDp.toFloat(),
                    TypedValue.COMPLEX_UNIT_DIP,
                )
            }
        }
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

    private fun writeErrorLog(context: Context, e: Exception) {
        try {
            val logFile = File(context.filesDir, "widget_error.log")
            val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date())
            val entry = "[$timestamp] ${e::class.simpleName}: ${e.message}\n${e.stackTraceToString()}\n\n"
            logFile.appendText(entry)
            if (logFile.length() > 50_000) {
                val lines = logFile.readLines()
                logFile.writeText(lines.takeLast(200).joinToString("\n"))
            }
        } catch (_: Exception) { }
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
