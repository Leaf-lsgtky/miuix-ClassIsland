# Keep all app classes (widget uses reflection and cross-package references)
-keep class moe.lsgtky.leafisland.** { *; }

# Keep tyme library (used by widget for lunar calendar)
-keep class com.tyme.** { *; }
