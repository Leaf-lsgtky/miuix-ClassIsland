package com.schedule.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedule.app.data.CourseEvent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ScheduleScreen(
    todayCourses: List<CourseEvent>,
    tomorrowCourses: List<CourseEvent>,
    hasData: Boolean,
    onImportClick: () -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            TopAppBar(
                title = "课程表",
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onImportClick) {
                Icon(
                    imageVector = Icons.Outlined.FileOpen,
                    contentDescription = "导入课程表",
                )
            }
        },
    ) { paddingValues ->
        if (!hasData) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "请点击右下角按钮导入 .ics 课程表文件",
                        fontSize = 15.sp,
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            ) {
                item {
                    val today = LocalDate.now()
                    val dayOfWeek = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE)
                    SmallTitle(
                        text = "今天 · ${today.format(DateTimeFormatter.ofPattern("M月d日"))} $dayOfWeek",
                    )
                }

                if (todayCourses.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 6.dp),
                            insideMargin = PaddingValues(16.dp),
                        ) {
                            Text(text = "今天没有课程", fontSize = 14.sp)
                        }
                    }
                } else {
                    items(todayCourses) { course ->
                        CourseCard(course)
                    }
                }

                item {
                    val tomorrow = LocalDate.now().plusDays(1)
                    val dayOfWeek = tomorrow.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE)
                    SmallTitle(
                        text = "明天 · ${tomorrow.format(DateTimeFormatter.ofPattern("M月d日"))} $dayOfWeek",
                    )
                }

                if (tomorrowCourses.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 6.dp),
                            insideMargin = PaddingValues(16.dp),
                        ) {
                            Text(text = "明天没有课程", fontSize = 14.sp)
                        }
                    }
                } else {
                    items(tomorrowCourses) { course ->
                        CourseCard(course)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun CourseCard(course: CourseEvent) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 6.dp),
        insideMargin = PaddingValues(16.dp),
    ) {
        Text(
            text = course.summary,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = MiuixTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${course.startTime.format(timeFormatter)} - ${course.endTime.format(timeFormatter)}",
                fontSize = 14.sp,
                color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = course.section,
                fontSize = 14.sp,
                color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = course.location,
            fontSize = 14.sp,
            color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        if (course.teacher.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = course.teacher,
                fontSize = 14.sp,
                color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}
