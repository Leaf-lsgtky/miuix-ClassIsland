@file:OptIn(ExperimentalGlanceApi::class)

package moe.lsgtky.leafisland.notification

import android.content.Context
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.runBlocking
import moe.lsgtky.leafisland.data.CourseEvent
import java.time.format.DateTimeFormatter

object AodRemoteViewsBuilder {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private val titleColor = ColorProvider(Color.White)
    private val labelColor = ColorProvider(Color.White.copy(alpha = 0.6f))
    private val valueColor = ColorProvider(Color.White.copy(alpha = 0.9f))

    fun build(context: Context, course: CourseEvent): RemoteViews = runBlocking {
        val timeRange = "${course.startTime.format(timeFormatter)} - ${course.endTime.format(timeFormatter)}"

        GlanceRemoteViews().compose(
            context = context,
            size = DpSize(300.dp, 200.dp),
        ) {
            AodContent(course, timeRange)
        }.remoteViews
    }

    @Composable
    private fun AodContent(course: CourseEvent, timeRange: String) {
        Column(modifier = GlanceModifier.padding(16.dp)) {
            Text(
                text = course.summary,
                style = TextStyle(
                    color = titleColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            InfoRow("时间", timeRange)
            if (course.section.isNotBlank()) {
                InfoRow("节次", course.section)
            }
            InfoRow("地点", course.location)
            if (course.teacher.isNotBlank()) {
                InfoRow("教师", course.teacher)
            }
        }
    }

    @Composable
    private fun InfoRow(label: String, value: String) {
        Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 1.dp)) {
            Text(
                text = label,
                style = TextStyle(color = labelColor, fontSize = 13.sp),
                modifier = GlanceModifier.width(40.dp),
            )
            Text(
                text = value,
                style = TextStyle(color = valueColor, fontSize = 14.sp),
            )
        }
    }
}
