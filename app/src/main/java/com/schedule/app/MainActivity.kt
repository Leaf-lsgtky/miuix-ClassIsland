package com.schedule.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.schedule.app.data.CourseEvent
import com.schedule.app.data.IcsParser
import com.schedule.app.ui.ScheduleScreen
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import java.io.File
import java.time.LocalDate

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val saved = loadSavedIcsContent()
        if (saved != null) {
            loadCourses(saved)
        }

        setContent {
            val controller = remember { ThemeController() }
            MiuixTheme(controller = controller) {
                ScheduleScreen(
                    todayCourses = todayCourses,
                    tomorrowCourses = tomorrowCourses,
                    hasData = hasData,
                    onImportClick = { openFilePicker() },
                )
            }
        }
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
    }

    private fun saveIcsContent(content: String) {
        File(filesDir, "schedule.ics").writeText(content)
    }

    private fun loadSavedIcsContent(): String? {
        val file = File(filesDir, "schedule.ics")
        return if (file.exists()) file.readText() else null
    }
}
