package ai.idraak.stepskyscraper.core.ui.format

import ai.idraak.stepskyscraper.core.ui.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Number and date formatting used across screens (tabular digits come from the type styles). */
object Formats {
    fun steps(value: Int): String = NumberFormat.getIntegerInstance().format(value)

    fun steps(value: Long): String = NumberFormat.getIntegerInstance().format(value)

    private fun pattern(p: String) = DateTimeFormatter.ofPattern(p, Locale.getDefault())

    /** "14 Aug" */
    /** "17 Nov", with a no-break space so a wrapped sentence never splits the date. */
    fun dayMonth(date: LocalDate): String = date.format(pattern("d MMM")).replace(' ', ' ')

    /** "2 Sep 2026" */
    fun fullDate(date: LocalDate): String = date.format(pattern("d MMM yyyy"))

    /** "Sat 6 Sep" */
    fun weekdayDayMonth(date: LocalDate): String = date.format(pattern("EEE d MMM"))

    /** "Tue 18 Nov, 21:40" */
    fun weekdayDateTime(at: Instant, zone: ZoneId): String = LocalDateTime.ofInstant(at, zone).format(pattern("EEE d MMM, HH:mm"))

    /** "NOV 2026" */
    fun monthYear(date: LocalDate): String = date.format(pattern("MMM yyyy")).uppercase(Locale.getDefault())

    /** "Sep 2–9" or "Aug 30 – Sep 9" */
    fun dateRange(start: LocalDate, end: LocalDate): String = if (start.month == end.month) {
        "${start.format(pattern("MMM d"))}–${end.dayOfMonth}"
    } else {
        "${start.format(pattern("MMM d"))} – ${end.format(pattern("MMM d"))}"
    }

    /** "27 Nov" style used in paywall legal copy. */
    fun trialEnd(date: LocalDate): String = dayMonth(date)

    /** Single-letter weekday for the bar chart ("M", "T"…). */
    fun weekdayInitial(date: LocalDate): String = date.format(pattern("EEEEE"))
}

/** "Updated just now / 12 min ago / 2 h ago / yesterday" (stale data shows how old it is). */
@Composable
fun updatedAgo(at: Instant?, now: Instant): String {
    if (at == null) return stringResource(R.string.updated_never)
    val minutes = Duration.between(at, now).toMinutes().coerceAtLeast(0)
    return when {
        minutes < 1 -> stringResource(R.string.updated_now)
        minutes < 60 -> pluralStringResource(R.plurals.updated_minutes, minutes.toInt(), minutes.toInt())
        minutes < 24 * 60 -> pluralStringResource(R.plurals.updated_hours, (minutes / 60).toInt(), (minutes / 60).toInt())
        else -> pluralStringResource(R.plurals.updated_days, (minutes / (24 * 60)).toInt(), (minutes / (24 * 60)).toInt())
    }
}

/** Short lower-case variant for list subtitles ("updated 4 min ago"). */
@Composable
fun updatedAgoLower(at: Instant?, now: Instant): String {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    return updatedAgo(at, now).replaceFirstChar { it.lowercase(locale) }
}
