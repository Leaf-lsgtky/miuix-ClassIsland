package moe.lsgtky.leafisland.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import moe.lsgtky.leafisland.util.SettingsStore
import moe.lsgtky.leafisland.widget.ScheduleWidgetProvider
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.ColorPalette
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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
    var infoSize by remember { mutableIntStateOf(SettingsStore.getWidgetInfoSize(context)) }
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
    val showInfoSizeDialog = remember { mutableStateOf(false) }

    // --- Slider/input temps ---
    var timeSizeInput by remember { mutableStateOf(timeSize.toString()) }
    var infoSizeInput by remember { mutableStateOf(infoSize.toString()) }
    var charsSlider by remember { mutableFloatStateOf(courseChars.toFloat()) }
    var advanceSlider by remember { mutableFloatStateOf(advanceMin.toFloat()) }
    var spacingInput by remember { mutableStateOf(infoSpacing.toString()) }
    var topPaddingInput by remember { mutableStateOf(topPadding.toString()) }

    // --- Dropdown data ---
    val weightOptions = listOf("Regular (400)", "Medium (500)", "SemiBold (600)", "Bold (700)", "ExtraBold (800)", "Black (900)")
    val weightValues = listOf(400, 500, 600, 700, 800, 900)
    val positionOptions = listOf("时间上方", "时间下方")

    fun weightToIndex(w: Int) = weightValues.indexOf(w).coerceAtLeast(0)

    fun refresh() = ScheduleWidgetProvider.triggerUpdate(context)

    // --- Preview bitmap ---
    val dm = context.resources.displayMetrics
    val previewColor = try { AndroidColor.parseColor(textColor) } catch (_: Exception) { AndroidColor.WHITE }
    val previewBitmap = remember(timeSize, timeWeight, infoSize, infoWeight, textColor, infoAbove, infoSpacing, topPadding) {
        val w = (300 * dm.density).toInt()
        val h = (120 * dm.density).toInt()
        val now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        val today = LocalDate.now()
        val info = "${today.monthValue}月${today.dayOfMonth}日 · 预览文本"
        ScheduleWidgetProvider.renderBitmap(
            w, h, dm.density, dm.scaledDensity,
            timeSize, timeWeight, infoSize, infoWeight, previewColor,
            infoAbove, infoSpacing, topPadding, now, info,
        )
    }

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
            // --- 预览 ---
            item { SmallTitle(text = "预览") }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 6.dp),
                ) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "小部件预览",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(300f / 120f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                    )
                }
            }

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
                            timeSizeInput = timeSize.toString()
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
                            topPaddingInput = topPadding.toString()
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
                    SuperArrow(
                        title = "副文本字号",
                        summary = "${infoSize}sp",
                        onClick = {
                            infoSizeInput = infoSize.toString()
                            showInfoSizeDialog.value = true
                        },
                    )
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
                            spacingInput = infoSpacing.toString()
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
                        onClick = { showColorDialog.value = true },
                    )
                }
            }
        }

        // --- Dialogs ---

        // Time size
        InputDialog(
            show = showTimeSizeDialog,
            title = "时间字号",
            value = timeSizeInput,
            onValueChange = { timeSizeInput = it },
            suffix = "sp（默认 56）",
            onConfirm = {
                val v = timeSizeInput.toIntOrNull() ?: timeSize
                SettingsStore.setWidgetTimeSize(context, v)
                timeSize = v
                refresh()
            },
        )

        // Info size
        InputDialog(
            show = showInfoSizeDialog,
            title = "副文本字号",
            value = infoSizeInput,
            onValueChange = { infoSizeInput = it },
            suffix = "sp（默认 14）",
            onConfirm = {
                val v = infoSizeInput.toIntOrNull() ?: infoSize
                SettingsStore.setWidgetInfoSize(context, v)
                infoSize = v
                refresh()
            },
        )

        // Top padding (text input, no bounds)
        InputDialog(
            show = showTopPaddingDialog,
            title = "时钟上间距",
            value = topPaddingInput,
            onValueChange = { topPaddingInput = it },
            suffix = "dp",
            onConfirm = {
                val v = topPaddingInput.toIntOrNull() ?: topPadding
                SettingsStore.setWidgetTopPadding(context, v)
                topPadding = v
                refresh()
            },
        )

        // Spacing (text input, no bounds)
        InputDialog(
            show = showSpacingDialog,
            title = "副文本与时钟间距",
            value = spacingInput,
            onValueChange = { spacingInput = it },
            suffix = "dp",
            onConfirm = {
                val v = spacingInput.toIntOrNull() ?: infoSpacing
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

        // Color palette
        SuperDialog(
            show = showColorDialog,
            title = "文字颜色",
            onDismissRequest = { showColorDialog.value = false },
        ) {
            var selectedColor by remember(textColor) {
                mutableStateOf(
                    try { Color(AndroidColor.parseColor(textColor)) } catch (_: Exception) { Color.White }
                )
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                ColorPalette(
                    color = selectedColor,
                    onColorChanged = { selectedColor = it },
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
                            val hex = String.format("#%08X", selectedColor.toArgb())
                            SettingsStore.setWidgetTextColor(context, hex)
                            textColor = hex
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

@Composable
private fun InputDialog(
    show: androidx.compose.runtime.MutableState<Boolean>,
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    suffix: String,
    onConfirm: () -> Unit,
) {
    SuperDialog(
        show = show,
        title = title,
        onDismissRequest = { show.value = false },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = suffix)
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = value,
                onValueChange = onValueChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
