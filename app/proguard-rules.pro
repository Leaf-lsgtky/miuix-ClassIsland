# Keep classes referenced by name in AndroidManifest.xml
-keep class moe.lsgtky.leafisland.widget.ScheduleWidgetProvider
-keep class moe.lsgtky.leafisland.notification.AlarmReceiver

# Keep Shizuku UserService (instantiated by Shizuku via reflection)
-keep class moe.lsgtky.leafisland.shizuku.NetworkBlockerService { *; }

# Keep AIDL interface
-keep class moe.lsgtky.leafisland.INetworkBlockerService { *; }
-keep class moe.lsgtky.leafisland.INetworkBlockerService$Stub { *; }
-keep class moe.lsgtky.leafisland.INetworkBlockerService$Stub$Proxy { *; }
