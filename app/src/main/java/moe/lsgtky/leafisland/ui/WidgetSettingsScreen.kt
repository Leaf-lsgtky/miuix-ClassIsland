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
import androidx.compose.ui.unit.dp
import moe.lsgtky.leafisland.util.SettingsStore
import moe.lsgtky.leafisland.widget.ScheduleWidgetProvider
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
import top.yukonga.miuix.kmp.extra.SuperDropdown
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun WidgetSettingsScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())

    // --- State ---
    var timeSize by remember { mutableIntStateOf(SettingsStore.getWidgetTimeSize(context)) }
    var timeWeight by remember { mutableIntStateOf(SettingsStore.getWidgetTimeWeight(context)) }
    var textColor by remember { mutableStateOf(SettingsStore.getWidgetTextColor(context)) }
    var courseChars by remember { mutableIntStateOf(SettingsStore.getWidgetCourseChars(context)) }
    var advanceMin by remember { mutableIntStateOf(SettingsStore.getWidgetAdvanceMinutes(context)) }
    var infoWeight by remember { mutableIntStateOf(SettingsStore.getWidgetInfoWeight(context)) }
    var infoAbove by remember { mutableStateOf(SettingsStore.getWidgetInfoAbove(context)) }
    var infoSpacing by remember { mutableIntStateOf(SettingsStore.getWidgetInfoSpacing(context)) }
    var topPadding by remember { mutableIntStateOf(SettingsStore.getWidgetTopPadding(context)) }

    // --- Dialog visibility ---
    val showTimeSizeDialog = remember { mutableStateOf(false) }
    val showColorDialog = remember { mutableStateOf(false) }
    val showCharsDialog = remember { mutableStateOf(false) }
    val showAdvanceDialog = remember { mutableStateOf(false) }
    val showSpacingDialog = remember { mutableStateOf(false) }
    val showTopPaddingDialog = remember { mutableStateOf(false) }

    // --- Slider temps ---
    var timeSizeSlider by remember { mutableFloatStateOf(timeSize.toFloat()) }
    var charsSlider by remember { mutableFloatStateOf(courseChars.toFloat()) }
    var advanceSlider by remember { mutableFloatStateOf(advanceMin.toFloat()) }
    var spacingSlider by remember { mutableFloatStateOf(infoSpacing.toFloat()) }
    var topPaddingSlider by remember { mutableFloatStateOf(topPadding.toFloat()) }
    var colorInput by remember { mutableStateOf(textColor) }

    // --- Dropdown data ---
    val weightOptions = listOf("Regular (400)", "Medium (500)", "SemiBold (600)", "Bold (700)", "ExtraBold (800)", "Black (900)")
    val weightValues = listOf(400, 500, 600, 700, 800, 900)
    val positionOptions = listOf("时间上方", "时间下方")

    fun weightToIndex(w: Int) = weightValues.indexOf(w).coerceAtLeast(0)

    fun refresh() = ScheduleWidgetProvider.triggerUpdate(context)

    Scaffold(
        topBar = {
            TopAppBar(
                title = "小部件设置",
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
            // --- 时钟 ---
            item { SmallTitle(text = "时钟") }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 6.dp),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    SuperArrow(
                        title = "时间字号",
                        summary = "${timeSize}sp",
                        onClick = {
                            timeSizeSlider = timeSize.toFloat()
                            showTimeSizeDialog.value = true
                        },
                    )
                    SuperDropdown(
                        title = "时间粗细",
                        items = weightOptions,
                        selectedIndex = weightToIndex(timeWeight),
                        onSelectedIndexChange = { index ->
                            val w = weightValues[index]
                            SettingsStore.setWidgetTimeWeight(context, w)
                            timeWeight = w
                            refresh()
                        },
                    )
                    SuperArrow(
                        title = "时钟上间距",
                        summary = "${topPadding}dp",
                        onClick = {
                            topPaddingSlider = topPadding.toFloat()
                            showTopPaddingDialog.value = true
                        },
                    )
                }
            }

            // --- 副文本 ---
            item { SmallTitle(text = "副文本") }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 6.dp),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    SuperDropdown(
                        title = "副文本粗细",
                        items = weightOptions,
                        selectedIndex = weightToIndex(infoWeight),
                        onSelectedIndexChange = { index ->
                            val w = weightValues[index]
                            SettingsStore.setWidgetInfoWeight(context, w)
                            infoWeight = w
                            refresh()
                        },
                    )
                    SuperDropdown(
                        title = "副文本位置",
                        items = positionOptions,
                        selectedIndex = if (infoAbove) 0 else 1,
                        onSelectedIndexChange = { index ->
                            val above = index == 0
                            SettingsStore.setWidgetInfoAbove(context, above)
                            infoAbove = above
                            refresh()
                        },
                    )
                    SuperArrow(
                        title = "副文本与时钟间距",
                        summary = "${infoSpacing}dp",
                        onClick = {
                            spacingSlider = infoSpacing.toFloat()
                            showSpacingDialog.value = true
                        },
                    )
                    SuperArrow(
                        title = "课程显示字数",
                        summary = "${courseChars}个字",
                        onClick = {
                            charsSlider = courseChars.toFloat()
                            showCharsDialog.value = true
                        },
                    )
                    SuperArrow(
                        title = "提前显示时间",
                        summary = "${advanceMin} 分钟",
                        onClick = {
                            advanceSlider = advanceMin.toFloat()
                            showAdvanceDialog.value = true
                        },
                    )
                }
            }

            // --- 外观 ---
            item { SmallTitle(text = "外观") }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 6.dp),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    SuperArrow(
                        title = "文字颜色",
                        summary = textColor,
                        onClick = {
                            colorInput = textColor
                            showColorDialog.value = true
                        },
                    )
                }
            }
        }

        // --- Dialogs ---

        // Time size
        SliderDialog(
            show = showTimeSizeDialog,
            title = "时间字号",
            value = timeSizeSlider,
            onValueChange = { timeSizeSlider = it },
            valueRange = 30f..80f,
            valueLabel = { "${it.toInt()}sp" },
            onConfirm = {
                val v = timeSizeSlider.toInt()
                SettingsStore.setWidgetTimeSize(context, v)
                timeSize = v
                refresh()
            },
        )

        // Top padding
        SliderDialog(
            show = showTopPaddingDialog,
            title = "时钟上间距",
            value = topPaddingSlider,
            onValueChange = { topPaddingSlider = it },
            valueRange = -10f..50f,
            valueLabel = { "${it.toInt()}dp" },
            onConfirm = {
                val v = topPaddingSlider.toInt()
                SettingsStore.setWidgetTopPadding(context, v)
                topPadding = v
                refresh()
            },
        )

        // Spacing
        SliderDialog(
            show = showSpacingDialog,
            title = "副文本与时钟间距",
            value = spacingSlider,
            onValueChange = { spacingSlider = it },
            valueRange = -10f..30f,
            valueLabel = { "${it.toInt()}dp" },
            onConfirm = {
                val v = spacingSlider.toInt()
                SettingsStore.setWidgetInfoSpacing(context, v)
                infoSpacing = v
                refresh()
            },
        )

        // Course chars
        SliderDialog(
            show = showCharsDialog,
            title = "课程显示字数",
            value = charsSlider,
            onValueChange = { charsSlider = it },
            valueRange = 2f..10f,
            valueLabel = { "${it.toInt()}个字" },
            onConfirm = {
                val v = charsSlider.toInt()
                SettingsStore.setWidgetCourseChars(context, v)
                courseChars = v
                refresh()
            },
        )

        // Advance minutes
        SliderDialog(
            show = showAdvanceDialog,
            title = "提前显示时间",
            value = advanceSlider,
            onValueChange = { advanceSlider = it },
            valueRange = 5f..120f,
            valueLabel = { "${it.toInt()} 分钟" },
            onConfirm = {
                val v = advanceSlider.toInt()
                SettingsStore.setWidgetAdvanceMinutes(context, v)
                advanceMin = v
                refresh()
            },
        )

        // Color
        SuperDialog(
            show = showColorDialog,
            title = "文字颜色",
            onDismissRequest = { showColorDialog.value = false },
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = colorInput,
                    onValueChange = { colorInput = it },
                    label = "十六进制颜色 (如 #FFFFFF)",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        text = "取消",
                        onClick = { showColorDialog.value = false },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        text = "确定",
                        onClick = {
                            val c = colorInput.trim()
                            SettingsStore.setWidgetTextColor(context, c)
                            textColor = c
                            showColorDialog.value = false
                            refresh()
                        },
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SliderDialog(
    show: androidx.compose.runtime.MutableState<Boolean>,
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: (Float) -> String,
    onConfirm: () -> Unit,
) {
    SuperDialog(
        show = show,
        title = title,
        onDismissRequest = { show.value = false },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = valueLabel(value))
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    text = "取消",
                    onClick = { show.value = false },
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    text = "确定",
                    onClick = {
                        onConfirm()
                        show.value = false
                    },
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
