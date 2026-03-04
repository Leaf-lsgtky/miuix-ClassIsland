package moe.lsgtky.leafisland

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.ui.NavDisplay
import moe.lsgtky.leafisland.data.CourseEvent
import moe.lsgtky.leafisland.data.IcsParser
import moe.lsgtky.leafisland.notification.AlarmScheduler
import moe.lsgtky.leafisland.notification.NotificationHelper
import moe.lsgtky.leafisland.ui.ScheduleScreen
import moe.lsgtky.leafisland.ui.SettingsScreen
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import java.io.File
import java.time.LocalDate

sealed interface Screen : NavKey {
    data object Home : Screen
    data object Settings : Screen
}

class MainActivity : ComponentActivity() {

    private var todayCourses by mutableStateOf<List<CourseEvent>>(emptyList())
    private var tomorrowCourses by mutableStateOf<List<CourseEvent>>(emptyList())
    private var hasData by mutableStateOf(false)

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val content = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (content != null) {
                saveIcsContent(content)
                loadCourses(content)
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        rescheduleAlarms()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.createNotificationChannel(this)

        val saved = loadSavedIcsContent()
        if (saved != null) {
            loadCourses(saved)
        }

        requestNotificationPermissionIfNeeded()

        setContent {
            val controller = remember { ThemeController() }
            val darkMode = isSystemInDarkTheme()

            DisposableEffect(darkMode) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkMode },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkMode },
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
                onDispose {}
            }

            MiuixTheme(controller = controller) {
                val backStack = remember { listOf<Screen>(Screen.Home).toMutableStateList() }

                val entryProviderImpl = entryProvider<Screen> {
                    entry<Screen.Home> {
                        ScheduleScreen(
                            todayCourses = todayCourses,
                            tomorrowCourses = tomorrowCourses,
                            hasData = hasData,
                            onImportClick = { openFilePicker() },
                            onSettingsClick = { backStack.add(Screen.Settings) },
                        )
                    }
                    entry<Screen.Settings> {
                        SettingsScreen(
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }
                }

                val entries = rememberDecoratedNavEntries(
                    backStack = backStack,
                    entryProvider = entryProviderImpl,
                )

                NavDisplay(
                    entries = entries,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val content = loadSavedIcsContent() ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        ) {
            AlarmScheduler.scheduleForCourses(this, content)
            AlarmScheduler.scheduleAllPushAlarms(this)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        rescheduleAlarms()
    }

    private fun rescheduleAlarms() {
        val content = loadSavedIcsContent() ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = android.net.Uri.parse("package:$packageName")
                }
                startActivity(intent)
                return
            }
        }

        AlarmScheduler.scheduleForCourses(this, content)
        AlarmScheduler.scheduleAllPushAlarms(this)
    }

    private fun openFilePicker() {
        filePickerLauncher.launch(arrayOf("text/calendar", "*/*"))
    }

    private fun loadCourses(icsContent: String) {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        todayCourses = IcsParser.parse(icsContent, today)
        tomorrowCourses = IcsParser.parse(icsContent, tomorrow)
        hasData = true
        rescheduleAlarms()
    }

    private fun saveIcsContent(content: String) {
        File(filesDir, "schedule.ics").writeText(content)
    }

    private fun loadSavedIcsContent(): String? {
        val file = File(filesDir, "schedule.ics")
        return if (file.exists()) file.readText() else null
    }
}
