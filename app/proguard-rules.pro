# Keep widget provider (referenced by class name in AndroidManifest.xml)
-keep class moe.lsgtky.leafisland.widget.ScheduleWidgetProvider { *; }

# Keep SettingsStore (used by widget provider via SharedPreferences keys)
-keep class moe.lsgtky.leafisland.util.SettingsStore { *; }
