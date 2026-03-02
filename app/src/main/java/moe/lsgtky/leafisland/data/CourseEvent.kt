package moe.lsgtky.leafisland.data

import java.time.LocalTime

data class CourseEvent(
    val summary: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val location: String,
    val teacher: String,
    val section: String,
)
