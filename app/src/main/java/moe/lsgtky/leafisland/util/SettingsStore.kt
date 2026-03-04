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
}
