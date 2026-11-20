package ai.idraak.stepskyscraper.core.domain.usecase

import ai.idraak.stepskyscraper.core.domain.model.DaySteps
import ai.idraak.stepskyscraper.core.domain.model.Goals
import ai.idraak.stepskyscraper.core.domain.model.PeriodTotal
import ai.idraak.stepskyscraper.core.domain.model.WeekDayBar
import ai.idraak.stepskyscraper.core.domain.model.WeekProgress
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/**
 * Weekly goal: floors built Monday–Sunday versus the weekly goal (daily target × 7 / 1,000).
 * The week, not the day, is what counts; a slow day never "breaks" anything.
 */
class CalculateWeekProgressUseCase @Inject constructor(
    private val floors: CalculateFloorsUseCase,
) {
    operator fun invoke(days: List<DaySteps>, goals: Goals, today: LocalDate, buildStartDate: LocalDate?): WeekProgress {
        val weekStart = weekStartOf(today)
        val byDate = days.associateBy { it.date }
        val bars = (0L until 7L).map { offset ->
            val date = weekStart.plusDays(offset)
            val counts = buildStartDate != null && !date.isBefore(buildStartDate)
            WeekDayBar(
                date = date,
                floors = if (counts) floors.floorsFor(byDate[date]?.steps ?: 0) else 0,
                isToday = date == today,
                isFuture = date.isAfter(today),
            )
        }
        return WeekProgress(
            weekStart = weekStart,
            floors = bars.sumOf { it.floors },
            goalFloors = goals.weeklyFloors,
            daysLeftIncludingToday = DayOfWeek.SUNDAY.value - today.dayOfWeek.value + 1,
            paused = goals.isPausedOn(today),
            days = bars,
        )
    }

    /** Totals for the [count] most recent weeks, oldest first. A week is "paused" if any of its days was. */
    fun recentWeeks(days: List<DaySteps>, goals: Goals, today: LocalDate, buildStartDate: LocalDate?, count: Int): List<PeriodTotal> {
        val currentStart = weekStartOf(today)
        return (count - 1 downTo 0).map { back ->
            val start = currentStart.minusWeeks(back.toLong())
            val end = start.plusDays(6)
            PeriodTotal(
                start = start,
                floors = floorsBetween(days, start, end, buildStartDate),
                goalFloors = goals.weeklyFloors,
                paused = (0L..6L).any { goals.isPausedOn(start.plusDays(it)) && !start.plusDays(it).isAfter(today) },
                isCurrent = back == 0,
            )
        }
    }

    /** Daily floors for the current calendar month (Month toggle on Me). */
    fun monthDays(days: List<DaySteps>, today: LocalDate, buildStartDate: LocalDate?): List<WeekDayBar> {
        val first = today.withDayOfMonth(1)
        val byDate = days.associateBy { it.date }
        return (0 until today.lengthOfMonth()).map { offset ->
            val date = first.plusDays(offset.toLong())
            val counts = buildStartDate != null && !date.isBefore(buildStartDate)
            WeekDayBar(
                date = date,
                floors = if (counts) floors.floorsFor(byDate[date]?.steps ?: 0) else 0,
                isToday = date == today,
                isFuture = date.isAfter(today),
            )
        }
    }

    private fun floorsBetween(days: List<DaySteps>, start: LocalDate, end: LocalDate, buildStartDate: LocalDate?): Int =
        days.filter { !it.date.isBefore(start) && !it.date.isAfter(end) }
            .filter { buildStartDate != null && !it.date.isBefore(buildStartDate) }
            .sumOf { floors.floorsFor(it.steps) }

    companion object {
        fun weekStartOf(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
}
