package com.tally.ui.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/** `HH:mm` for the given instant in [tz]. */
fun Instant.timeLabel(tz: TimeZone): String {
    val time = toLocalDateTime(tz).time
    return "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
}

/** "Today" / "Yesterday" / "5 Jan" (or "5 Jan 2025" if not the current year). */
fun dayLabel(
    date: LocalDate,
    today: LocalDate,
): String =
    when (date) {
        today -> "Today"
        today.minus(1, DateTimeUnit.DAY) -> "Yesterday"
        else -> {
            val month = date.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
            if (date.year == today.year) {
                "${date.dayOfMonth} $month"
            } else {
                "${date.dayOfMonth} $month ${date.year}"
            }
        }
    }
