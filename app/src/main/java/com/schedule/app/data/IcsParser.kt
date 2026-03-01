package com.schedule.app.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object IcsParser {

    private data class RawEvent(
        val summary: String,
        val dtStart: LocalDateTime,
        val dtEnd: LocalDateTime,
        val rruleUntil: LocalDate?,
        val rruleInterval: Int,
        val location: String,
        val description: String,
    )

    fun parse(icsContent: String, targetDate: LocalDate): List<CourseEvent> {
        val events = parseEvents(icsContent)
        return events.filter { isEventOnDate(it, targetDate) }
            .map { toDisplayEvent(it) }
            .sortedBy { it.startTime }
    }

    private fun parseEvents(content: String): List<RawEvent> {
        val events = mutableListOf<RawEvent>()
        val lines = unfoldLines(content.lines())

        var inEvent = false
        var summary = ""
        var dtStart: LocalDateTime? = null
        var dtEnd: LocalDateTime? = null
        var rruleUntil: LocalDate? = null
        var rruleInterval = 1
        var location = ""
        var description = ""

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed == "BEGIN:VEVENT" -> {
                    inEvent = true
                    summary = ""
                    dtStart = null
                    dtEnd = null
                    rruleUntil = null
                    rruleInterval = 1
                    location = ""
                    description = ""
                }
                trimmed == "END:VEVENT" -> {
                    if (inEvent && dtStart != null && dtEnd != null) {
                        events.add(
                            RawEvent(summary, dtStart, dtEnd, rruleUntil, rruleInterval, location, description)
                        )
                    }
                    inEvent = false
                }
                inEvent && trimmed.startsWith("SUMMARY:") -> {
                    summary = trimmed.removePrefix("SUMMARY:")
                }
                inEvent && trimmed.startsWith("DTSTART") -> {
                    dtStart = parseDateTimeLine(trimmed)
                }
                inEvent && trimmed.startsWith("DTEND") -> {
                    dtEnd = parseDateTimeLine(trimmed)
                }
                inEvent && trimmed.startsWith("RRULE:") -> {
                    val rule = trimmed.removePrefix("RRULE:")
                    val parts = rule.split(";").mapNotNull {
                        val kv = it.split("=", limit = 2)
                        if (kv.size == 2) kv[0] to kv[1] else null
                    }.toMap()
                    parts["UNTIL"]?.let {
                        rruleUntil = parseUntilDate(it)
                    }
                    parts["INTERVAL"]?.let {
                        rruleInterval = it.toIntOrNull() ?: 1
                    }
                }
                inEvent && trimmed.startsWith("LOCATION:") -> {
                    location = trimmed.removePrefix("LOCATION:")
                }
                inEvent && trimmed.startsWith("DESCRIPTION:") -> {
                    description = trimmed.removePrefix("DESCRIPTION:")
                }
            }
        }

        return events
    }

    private fun unfoldLines(lines: List<String>): List<String> {
        val result = mutableListOf<String>()
        for (line in lines) {
            if (line.startsWith(" ") || line.startsWith("\t")) {
                if (result.isNotEmpty()) {
                    result[result.lastIndex] = result.last() + line.substring(1)
                }
            } else {
                result.add(line)
            }
        }
        return result
    }

    private fun parseDateTimeLine(line: String): LocalDateTime? {
        val colonIndex = line.lastIndexOf(':')
        if (colonIndex < 0) return null
        val dateStr = line.substring(colonIndex + 1).trim()
        return try {
            LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
        } catch (_: Exception) {
            null
        }
    }

    private fun parseUntilDate(until: String): LocalDate? {
        return try {
            if (until.endsWith("Z")) {
                val zdt = ZonedDateTime.parse(
                    until,
                    DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneId.of("UTC"))
                )
                zdt.withZoneSameInstant(ZoneId.of("Asia/Shanghai")).toLocalDate()
            } else {
                LocalDate.parse(until.substring(0, 8), DateTimeFormatter.ofPattern("yyyyMMdd"))
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun isEventOnDate(event: RawEvent, targetDate: LocalDate): Boolean {
        val eventDate = event.dtStart.toLocalDate()

        if (targetDate.isBefore(eventDate)) return false

        if (event.rruleUntil != null && targetDate.isAfter(event.rruleUntil)) return false

        if (targetDate.dayOfWeek != eventDate.dayOfWeek) return false

        if (event.rruleInterval > 1) {
            val weeksBetween = ChronoUnit.WEEKS.between(eventDate, targetDate)
            if (weeksBetween % event.rruleInterval != 0L) return false
        }

        return true
    }

    private fun toDisplayEvent(event: RawEvent): CourseEvent {
        val descParts = event.description.split("\\n")
        val section = descParts.getOrElse(0) { "" }
        val loc = descParts.getOrElse(1) { event.location }
        val teacher = descParts.getOrElse(2) { "" }

        return CourseEvent(
            summary = event.summary,
            startTime = event.dtStart.toLocalTime(),
            endTime = event.dtEnd.toLocalTime(),
            location = loc,
            teacher = teacher,
            section = section,
        )
    }
}
