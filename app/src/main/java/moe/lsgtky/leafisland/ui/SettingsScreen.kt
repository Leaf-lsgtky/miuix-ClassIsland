package moe.lsgtky.leafisland.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import moe.lsgtky.leafisland.data.IcsParser
import moe.lsgtky.leafisland.data.ScheduledPush
import moe.lsgtky.leafisland.notification.AlarmScheduler
import moe.lsgtky.leafisland.notification.NotificationHelper
import moe.lsgtky.leafisland.util.SettingsStore
import java.time.LocalDate
import java.time.LocalTime
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.utils.overScrollVertical
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onWidgetSettings: () -> Unit,
) {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())

    // --- Advance minutes state ---
    var advanceMinutes by remember {
        mutableIntStateOf(SettingsStore.getAdvanceMinutes(context))
    }
    var sliderValue by remember { mutableFloatStateOf(advanceMinutes.toFloat()) }
    val showMinutesDialog = remember { mutableStateOf(false) }
    var textValue by remember { mutableStateOf(advanceMinutes.toString()) }

    // --- Scheduled push state ---
    var scheduledPushes by remember {
        mutableStateOf(SettingsStore.getScheduledPushes(context))
    }
    val showTimePickerDialog = remember { mutableStateOf(false) }
    val showDeleteDialog = remember { mutableStateOf(false) }
    var deleteTargetPush by remember { mutableStateOf<ScheduledPush?>(null) }
    val timePickerState = rememberTimePickerState(
        initialHour = 22,
        initialMinute = 0,
        is24Hour = true,
    )
    var dismissSliderValue by remember { mutableFloatStateOf(30f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "设置",
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(start = 12.dp),
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "返回",
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues,
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        ) {
            // --- 通知 section ---
            item {
                SmallTitle(text = "通知")
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 6.dp),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    SuperArrow(
                        title = "提前提醒时间",
                        summary = "${advanceMinutes} 分钟",
                        onClick = {
                            textValue = advanceMinutes.toString()
                            showMinutesDialog.value = true
                        },
                    )
                    Slider(
                        value = sliderValue,
                        onValueChange = { newVal ->
                            sliderValue = newVal
                        },
                        onValueChangeFinished = {
                            val finalValue = sliderValue.toInt().coerceIn(10, 60)
                            SettingsStore.setAdvanceMinutes(context, finalValue)
                            advanceMinutes = finalValue
                            val icsFile = File(context.filesDir, "schedule.ics")
                            if (icsFile.exists()) {
                                val icsContent = icsFile.readText()
                                AlarmScheduler.scheduleForCourses(context, icsContent)
                            }
                        },
                        valueRange = 10f..60f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 12.dp),
                    )
                    SuperArrow(
                        title = "通知测试",
                        summary = "发送最近一门课程的通知",
                        onClick = {
                            val icsFile = File(context.filesDir, "schedule.ics")
                            if (!icsFile.exists()) return@SuperArrow
                            val icsContent = icsFile.readText()
                            val now = LocalTime.now()
                            val today = LocalDate.now()
                            val tomorrow = today.plusDays(1)
                            val todayCourses = IcsParser.parse(icsContent, today)
                            val tomorrowCourses = IcsParser.parse(icsContent, tomorrow)
                            val nearest = todayCourses.firstOrNull { it.startTime >= now }
                                ?: tomorrowCourses.firstOrNull()
                                ?: todayCourses.firstOrNull()
                            if (nearest != null) {
                                NotificationHelper.postCourseNotification(context, nearest)
                            }
                        },
                    )
                    SuperArrow(
                        title = "清除通知",
                        summary = "清除所有课程提醒通知",
                        onClick = {
                            NotificationHelper.cancelAllNotifications(context)
                        },
                    )
                }
            }

            // --- 定时推送 section ---
            item {
                SmallTitle(text = "定时推送")
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 6.dp),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    for (push in scheduledPushes) {
                        SuperArrow(
                            title = String.format("%02d:%02d", push.hour, push.minute),
                            summary = "推送下一节课程提醒 · ${push.dismissMinutes}分钟后消失",
                            onClick = {
                                deleteTargetPush = push
                                showDeleteDialog.value = true
                            },
                        )
                    }
                    SuperArrow(
                        title = "添加定时推送",
                        summary = "在指定时间推送下一节课程通知",
                        onClick = {
                            dismissSliderValue = 30f
                            showTimePickerDialog.value = true
                        },
                    )
                }
            }

            // --- 小部件 section ---
            item {
                SmallTitle(text = "小部件")
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 6.dp),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    SuperArrow(
                        title = "小部件设置",
                        summary = "自定义桌面小部件外观",
                        onClick = onWidgetSettings,
                    )
                }
            }
        }

        // --- Minutes input dialog ---
        SuperDialog(
            show = showMinutesDialog,
            title = "提前提醒时间",
            onDismissRequest = { showMinutesDialog.value = false },
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = textValue,
                    onValueChange = { input ->
                        textValue = input.filter { it.isDigit() }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = "分钟 (10-60)",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        text = "取消",
                        onClick = { showMinutesDialog.value = false },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        text = "确定",
                        onClick = {
                            val finalValue = (textValue.toIntOrNull() ?: advanceMinutes).coerceIn(10, 60)
                            SettingsStore.setAdvanceMinutes(context, finalValue)
                            advanceMinutes = finalValue
                            sliderValue = finalValue.toFloat()
                            showMinutesDialog.value = false
                            val icsFile = File(context.filesDir, "schedule.ics")
                            if (icsFile.exists()) {
                                val icsContent = icsFile.readText()
                                AlarmScheduler.scheduleForCourses(context, icsContent)
                            }
                        },
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // --- Time picker dialog ---
        SuperDialog(
            show = showTimePickerDialog,
            title = "选择推送时间",
            onDismissRequest = { showTimePickerDialog.value = false },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TimeInput(state = timePickerState)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "通知消失时间：${dismissSliderValue.toInt()} 分钟")
                Slider(
                    value = dismissSliderValue,
                    onValueChange = { dismissSliderValue = it },
                    valueRange = 5f..120f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        text = "取消",
                        onClick = { showTimePickerDialog.value = false },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        text = "确定",
                        onClick = {
                            val newPush = ScheduledPush(
                                id = System.currentTimeMillis(),
                                hour = timePickerState.hour,
                                minute = timePickerState.minute,
                                dismissMinutes = dismissSliderValue.toInt(),
                            )
                            val updated = scheduledPushes + newPush
                            SettingsStore.setScheduledPushes(context, updated)
                            scheduledPushes = updated
                            AlarmScheduler.cancelAllPushAlarms(context)
                            AlarmScheduler.scheduleAllPushAlarms(context)
                            showTimePickerDialog.value = false
                        },
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // --- Delete confirmation dialog ---
        SuperDialog(
            show = showDeleteDialog,
            title = "删除定时推送",
            onDismissRequest = { showDeleteDialog.value = false },
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "确定删除 ${deleteTargetPush?.let { String.format("%02d:%02d", it.hour, it.minute) } ?: ""} 的定时推送？",
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        text = "取消",
                        onClick = { showDeleteDialog.value = false },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        text = "删除",
                        onClick = {
                            deleteTargetPush?.let { target ->
                                AlarmScheduler.cancelAllPushAlarms(context)
                                val updated = scheduledPushes.filter { it.id != target.id }
                                SettingsStore.setScheduledPushes(context, updated)
                                scheduledPushes = updated
                                AlarmScheduler.scheduleAllPushAlarms(context)
                            }
                            showDeleteDialog.value = false
                        },
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
