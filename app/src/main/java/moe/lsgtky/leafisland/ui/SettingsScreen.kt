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
import moe.lsgtky.leafisland.widget.ScheduleWidgetProvider
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

    // --- Widget settings state ---
    var widgetTimeSize by remember { mutableIntStateOf(SettingsStore.getWidgetTimeSize(context)) }
    var widgetTimeWeight by remember { mutableIntStateOf(SettingsStore.getWidgetTimeWeight(context)) }
    var widgetTextColor by remember { mutableStateOf(SettingsStore.getWidgetTextColor(context)) }
    var widgetCourseChars by remember { mutableIntStateOf(SettingsStore.getWidgetCourseChars(context)) }
    var widgetAdvanceMin by remember { mutableIntStateOf(SettingsStore.getWidgetAdvanceMinutes(context)) }
    val showWidgetTimeSizeDialog = remember { mutableStateOf(false) }
    val showWidgetWeightDialog = remember { mutableStateOf(false) }
    val showWidgetColorDialog = remember { mutableStateOf(false) }
    val showWidgetCharsDialog = remember { mutableStateOf(false) }
    val showWidgetAdvanceDialog = remember { mutableStateOf(false) }
    var widgetTimeSizeSlider by remember { mutableFloatStateOf(widgetTimeSize.toFloat()) }
    var widgetCharsSlider by remember { mutableFloatStateOf(widgetCourseChars.toFloat()) }
    var widgetAdvanceSlider by remember { mutableFloatStateOf(widgetAdvanceMin.toFloat()) }
    var widgetColorInput by remember { mutableStateOf(widgetTextColor) }

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
                        title = "时间字号",
                        summary = "${widgetTimeSize}sp",
                        onClick = {
                            widgetTimeSizeSlider = widgetTimeSize.toFloat()
                            showWidgetTimeSizeDialog.value = true
                        },
                    )
                    SuperArrow(
                        title = "时间粗细",
                        summary = weightLabel(widgetTimeWeight),
                        onClick = {
                            showWidgetWeightDialog.value = true
                        },
                    )
                    SuperArrow(
                        title = "文字颜色",
                        summary = widgetTextColor,
                        onClick = {
                            widgetColorInput = widgetTextColor
                            showWidgetColorDialog.value = true
                        },
                    )
                    SuperArrow(
                        title = "课程显示字数",
                        summary = "${widgetCourseChars}个字",
                        onClick = {
                            widgetCharsSlider = widgetCourseChars.toFloat()
                            showWidgetCharsDialog.value = true
                        },
                    )
                    SuperArrow(
                        title = "提前显示时间",
                        summary = "${widgetAdvanceMin} 分钟",
                        onClick = {
                            widgetAdvanceSlider = widgetAdvanceMin.toFloat()
                            showWidgetAdvanceDialog.value = true
                        },
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

        // --- Widget time size dialog ---
        SuperDialog(
            show = showWidgetTimeSizeDialog,
            title = "时间字号",
            onDismissRequest = { showWidgetTimeSizeDialog.value = false },
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "${widgetTimeSizeSlider.toInt()}sp")
                Slider(
                    value = widgetTimeSizeSlider,
                    onValueChange = { widgetTimeSizeSlider = it },
                    valueRange = 30f..80f,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        text = "取消",
                        onClick = { showWidgetTimeSizeDialog.value = false },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        text = "确定",
                        onClick = {
                            val v = widgetTimeSizeSlider.toInt()
                            SettingsStore.setWidgetTimeSize(context, v)
                            widgetTimeSize = v
                            showWidgetTimeSizeDialog.value = false
                            ScheduleWidgetProvider.triggerUpdate(context)
                        },
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // --- Widget weight dialog ---
        SuperDialog(
            show = showWidgetWeightDialog,
            title = "时间粗细",
            onDismissRequest = { showWidgetWeightDialog.value = false },
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                val weights = listOf(400, 500, 600, 700, 800, 900)
                for (w in weights) {
                    TextButton(
                        text = weightLabel(w),
                        onClick = {
                            SettingsStore.setWidgetTimeWeight(context, w)
                            widgetTimeWeight = w
                            showWidgetWeightDialog.value = false
                            ScheduleWidgetProvider.triggerUpdate(context)
                        },
                        colors = if (w == widgetTimeWeight) ButtonDefaults.textButtonColorsPrimary()
                        else ButtonDefaults.textButtonColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // --- Widget color dialog ---
        SuperDialog(
            show = showWidgetColorDialog,
            title = "文字颜色",
            onDismissRequest = { showWidgetColorDialog.value = false },
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = widgetColorInput,
                    onValueChange = { widgetColorInput = it },
                    label = "十六进制颜色 (如 #FFFFFF)",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        text = "取消",
                        onClick = { showWidgetColorDialog.value = false },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        text = "确定",
                        onClick = {
                            val color = widgetColorInput.trim()
                            SettingsStore.setWidgetTextColor(context, color)
                            widgetTextColor = color
                            showWidgetColorDialog.value = false
                            ScheduleWidgetProvider.triggerUpdate(context)
                        },
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // --- Widget course chars dialog ---
        SuperDialog(
            show = showWidgetCharsDialog,
            title = "课程显示字数",
            onDismissRequest = { showWidgetCharsDialog.value = false },
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "${widgetCharsSlider.toInt()}个字")
                Slider(
                    value = widgetCharsSlider,
                    onValueChange = { widgetCharsSlider = it },
                    valueRange = 2f..10f,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        text = "取消",
                        onClick = { showWidgetCharsDialog.value = false },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        text = "确定",
                        onClick = {
                            val v = widgetCharsSlider.toInt()
                            SettingsStore.setWidgetCourseChars(context, v)
                            widgetCourseChars = v
                            showWidgetCharsDialog.value = false
                            ScheduleWidgetProvider.triggerUpdate(context)
                        },
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // --- Widget advance minutes dialog ---
        SuperDialog(
            show = showWidgetAdvanceDialog,
            title = "提前显示时间",
            onDismissRequest = { showWidgetAdvanceDialog.value = false },
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "${widgetAdvanceSlider.toInt()} 分钟")
                Slider(
                    value = widgetAdvanceSlider,
                    onValueChange = { widgetAdvanceSlider = it },
                    valueRange = 5f..120f,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        text = "取消",
                        onClick = { showWidgetAdvanceDialog.value = false },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        text = "确定",
                        onClick = {
                            val v = widgetAdvanceSlider.toInt()
                            SettingsStore.setWidgetAdvanceMinutes(context, v)
                            widgetAdvanceMin = v
                            showWidgetAdvanceDialog.value = false
                            ScheduleWidgetProvider.triggerUpdate(context)
                        },
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private fun weightLabel(weight: Int): String {
    return when (weight) {
        400 -> "Regular (400)"
        500 -> "Medium (500)"
        600 -> "SemiBold (600)"
        700 -> "Bold (700)"
        800 -> "ExtraBold (800)"
        900 -> "Black (900)"
        else -> "$weight"
    }
}
