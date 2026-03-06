package moe.lsgtky.leafisland.util

import android.content.Context
import moe.lsgtky.leafisland.data.ScheduledPush
import org.json.JSONArray
import org.json.JSONObject

object SettingsStore {
    private const val PREFS_NAME = "schedule_settings"
    private const val KEY_ADVANCE_MINUTES = "advance_minutes"
    private const val KEY_SCHEDULED_REQUEST_CODES = "scheduled_request_codes"
    private const val KEY_SCHEDULED_PUSHES = "scheduled_pushes"
    private const val KEY_WIDGET_TIME_SIZE = "widget_time_size"
    private const val KEY_WIDGET_TIME_WEIGHT = "widget_time_weight"
    private const val KEY_WIDGET_TEXT_COLOR = "widget_text_color"
    private const val KEY_WIDGET_COURSE_CHARS = "widget_course_chars"
    private const val KEY_WIDGET_ADVANCE_MINUTES = "widget_advance_minutes"
    private const val KEY_WIDGET_INFO_WEIGHT = "widget_info_weight"
    private const val KEY_WIDGET_INFO_ABOVE = "widget_info_above"
    private const val KEY_WIDGET_INFO_SPACING = "widget_info_spacing"
    private const val KEY_WIDGET_TOP_PADDING = "widget_top_padding"
    private const val KEY_WIDGET_INFO_SIZE = "widget_info_size"
    const val DEFAULT_ADVANCE_MINUTES = 15

    fun getAdvanceMinutes(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_ADVANCE_MINUTES, DEFAULT_ADVANCE_MINUTES)
    }

    fun setAdvanceMinutes(context: Context, minutes: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_ADVANCE_MINUTES, minutes.coerceIn(10, 60))
            .apply()
    }

    // --- Request code tracking (for cancelling stale alarms) ---

    fun getScheduledRequestCodes(context: Context): Set<Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_SCHEDULED_REQUEST_CODES, emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            ?: emptySet()
    }

    fun setScheduledRequestCodes(context: Context, codes: Set<Int>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_SCHEDULED_REQUEST_CODES, codes.map { it.toString() }.toSet())
            .apply()
    }

    fun clearScheduledRequestCodes(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SCHEDULED_REQUEST_CODES)
            .apply()
    }

    // --- Scheduled push rules ---

    fun getScheduledPushes(context: Context): List<ScheduledPush> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_SCHEDULED_PUSHES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                ScheduledPush(
                    id = obj.getLong("id"),
                    hour = obj.getInt("hour"),
                    minute = obj.getInt("minute"),
                    dismissMinutes = obj.optInt("dismissMinutes", 30),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun setScheduledPushes(context: Context, pushes: List<ScheduledPush>) {
        val arr = JSONArray()
        for (push in pushes) {
            arr.put(JSONObject().apply {
                put("id", push.id)
                put("hour", push.hour)
                put("minute", push.minute)
                put("dismissMinutes", push.dismissMinutes)
            })
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SCHEDULED_PUSHES, arr.toString())
            .apply()
    }

    // --- Widget settings ---

    fun getWidgetTimeSize(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_WIDGET_TIME_SIZE, 56)
    }

    fun setWidgetTimeSize(context: Context, size: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_WIDGET_TIME_SIZE, size).apply()
    }

    fun getWidgetTimeWeight(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_WIDGET_TIME_WEIGHT, 700)
    }

    fun setWidgetTimeWeight(context: Context, weight: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_WIDGET_TIME_WEIGHT, weight).apply()
    }

    fun getWidgetTextColor(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_WIDGET_TEXT_COLOR, "#FFFFFFFF") ?: "#FFFFFFFF"
    }

    fun setWidgetTextColor(context: Context, color: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_WIDGET_TEXT_COLOR, color).apply()
    }

    fun getWidgetCourseChars(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_WIDGET_COURSE_CHARS, 4)
    }

    fun setWidgetCourseChars(context: Context, chars: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_WIDGET_COURSE_CHARS, chars.coerceIn(2, 10)).apply()
    }

    fun getWidgetAdvanceMinutes(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_WIDGET_ADVANCE_MINUTES, 30)
    }

    fun setWidgetAdvanceMinutes(context: Context, minutes: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_WIDGET_ADVANCE_MINUTES, minutes.coerceIn(5, 120)).apply()
    }

    fun getWidgetInfoWeight(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_WIDGET_INFO_WEIGHT, 400)
    }

    fun setWidgetInfoWeight(context: Context, weight: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_WIDGET_INFO_WEIGHT, weight).apply()
    }

    fun getWidgetInfoAbove(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_WIDGET_INFO_ABOVE, false)
    }

    fun setWidgetInfoAbove(context: Context, above: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_WIDGET_INFO_ABOVE, above).apply()
    }

    fun getWidgetInfoSpacing(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_WIDGET_INFO_SPACING, 2)
    }

    fun setWidgetInfoSpacing(context: Context, dp: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_WIDGET_INFO_SPACING, dp).apply()
    }

    fun getWidgetTopPadding(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_WIDGET_TOP_PADDING, 8)
    }

    fun setWidgetTopPadding(context: Context, dp: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_WIDGET_TOP_PADDING, dp).apply()
    }

    fun getWidgetInfoSize(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_WIDGET_INFO_SIZE, 14)
    }

    fun setWidgetInfoSize(context: Context, size: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_WIDGET_INFO_SIZE, size).apply()
    }
}
