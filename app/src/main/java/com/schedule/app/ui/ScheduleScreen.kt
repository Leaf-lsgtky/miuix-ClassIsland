package com.schedule.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedule.app.data.CourseEvent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
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
    onSettingsClick: () -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            TopAppBar(
                title = "课程表",
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = MiuixIcons.Settings,
                            contentDescription = "设置",
                        )
                    }
                    IconButton(
                        onClick = onImportClick,
                        modifier = Modifier.padding(end = 12.dp),
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Add,
                            contentDescription = "导入课程表",
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        if (!hasData) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "请点击右上角 + 按钮导入 .ics 课程表文件",
                    fontSize = 15.sp,
                )
            }
        } else {
            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier
                    .fillMaxSize()
                    .overScrollVertical()
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
                                .fillMaxWidth()
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
                                .fillMaxWidth()
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
            }
        }
    }
}

@Composable
private fun CourseCard(course: CourseEvent) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val secondaryColor = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
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
        InfoRow(
            label = "时间",
            value = "${course.startTime.format(timeFormatter)} - ${course.endTime.format(timeFormatter)}",
            color = secondaryColor,
        )
        InfoRow(label = "节次", value = course.section, color = secondaryColor)
        InfoRow(label = "地点", value = course.location, color = secondaryColor)
        if (course.teacher.isNotBlank()) {
            InfoRow(label = "教师", value = course.teacher, color = secondaryColor)
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = color.copy(alpha = 0.5f),
            modifier = Modifier.width(36.dp),
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = color,
        )
    }
}
