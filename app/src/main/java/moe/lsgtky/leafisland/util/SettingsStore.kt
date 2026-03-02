package moe.lsgtky.leafisland.util

import android.content.Context

object SettingsStore {
    private const val PREFS_NAME = "schedule_settings"
    private const val KEY_ADVANCE_MINUTES = "advance_minutes"
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
}
