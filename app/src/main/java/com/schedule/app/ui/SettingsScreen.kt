package com.schedule.app.ui

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.schedule.app.notification.AlarmScheduler
import com.schedule.app.util.SettingsStore
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

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())

    var advanceMinutes by remember {
        mutableIntStateOf(SettingsStore.getAdvanceMinutes(context))
    }
    var sliderValue by remember { mutableFloatStateOf(advanceMinutes.toFloat()) }

    val showDialog = remember { mutableStateOf(false) }
    var textValue by remember { mutableStateOf(advanceMinutes.toString()) }

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
                            showDialog.value = true
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
                                AlarmScheduler.cancelForCourses(context, icsContent)
                                AlarmScheduler.scheduleForCourses(context, icsContent)
                            }
                        },
                        valueRange = 10f..60f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 12.dp),
                    )
                }
            }
        }

        SuperDialog(
            show = showDialog,
            title = "提前提醒时间",
            onDismissRequest = { showDialog.value = false },
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
                        onClick = { showDialog.value = false },
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
                            showDialog.value = false
                            val icsFile = File(context.filesDir, "schedule.ics")
                            if (icsFile.exists()) {
                                val icsContent = icsFile.readText()
                                AlarmScheduler.cancelForCourses(context, icsContent)
                                AlarmScheduler.scheduleForCourses(context, icsContent)
                            }
                        },
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
