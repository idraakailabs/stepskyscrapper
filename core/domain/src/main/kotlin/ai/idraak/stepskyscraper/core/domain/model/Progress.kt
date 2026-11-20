package ai.idraak.stepskyscraper.core.domain.model

import java.time.LocalDate

/** Progress for today: floors = steps / 1000, the remainder fills 10 segments of 100 steps. */
data class DayProgress(
    val date: LocalDate,
    val steps: Int,
    val floors: Int,
    val stepsIntoFloor: Int,
) {
    val stepsToNextFloor: Int get() = FloorMath.STEPS_PER_FLOOR - stepsIntoFloor
    val segmentsFilled: Int get() = stepsIntoFloor / FloorMath.STEPS_PER_SEGMENT
    val activeSegmentFraction: Float
        get() = (stepsIntoFloor % FloorMath.STEPS_PER_SEGMENT) / FloorMath.STEPS_PER_SEGMENT.toFloat()
}

object FloorMath {
    const val STEPS_PER_FLOOR = 1_000
    const val SEGMENTS_PER_FLOOR = 10
    const val STEPS_PER_SEGMENT = STEPS_PER_FLOOR / SEGMENTS_PER_FLOOR
}

/** How the user framed their goal during onboarding / in Me › Goals. */
enum class TargetPreset(val dailySteps: Int) {
    EasyGoing(5_000),
    Steady(7_000),
    Ambitious(10_000),
    Custom(0),
}

/** Goal pause: for rest, travel or illness. Floors still count while paused. */
sealed interface GoalPause {
    data object None : GoalPause

    data class Until(val lastPausedDay: LocalDate) : GoalPause

    data class Indefinite(val since: LocalDate) : GoalPause
}

data class Goals(
    val dailyTargetSteps: Int,
    val preset: TargetPreset,
    val pause: GoalPause,
) {
    val weeklyFloors: Int get() = dailyTargetSteps / FloorMath.STEPS_PER_FLOOR * 7

    fun isPausedOn(date: LocalDate): Boolean = when (val p = pause) {
        GoalPause.None -> false
        is GoalPause.Until -> !date.isAfter(p.lastPausedDay)
        is GoalPause.Indefinite -> !date.isBefore(p.since)
    }
}

data class WeekDayBar(
    val date: LocalDate,
    val floors: Int,
    val isToday: Boolean,
    val isFuture: Boolean,
)

data class WeekProgress(
    val weekStart: LocalDate,
    val floors: Int,
    val goalFloors: Int,
    val daysLeftIncludingToday: Int,
    val paused: Boolean,
    val days: List<WeekDayBar>,
) {
    val fraction: Float get() = if (goalFloors == 0) 0f else (floors.toFloat() / goalFloors).coerceIn(0f, 1f)
    val percent: Int get() = (fraction * 100).toInt()
    val floorsToGo: Int get() = (goalFloors - floors).coerceAtLeast(0)
    val goalReached: Boolean get() = floors >= goalFloors && goalFloors > 0
}

/** One row of "Last 4 weeks" / a month bar. */
data class PeriodTotal(
    val start: LocalDate,
    val floors: Int,
    val goalFloors: Int,
    val paused: Boolean,
    val isCurrent: Boolean,
)

/** Architect level derived from total floors. */
data class ArchitectLevel(
    val level: Int,
    val title: String,
    val floorsIntoLevel: Int,
    val floorsForLevel: Int,
    val totalFloors: Int,
    val nextLevelAtFloors: Int?,
    val nextTitle: String?,
) {
    val fraction: Float get() = if (floorsForLevel == 0) 1f else floorsIntoLevel.toFloat() / floorsForLevel
}

/** Suggestion based on the last 7 days. */
data class TargetSuggestion(
    val sevenDayAverage: Int,
    val suggestedDailySteps: Int,
    val suggestedPreset: TargetPreset,
)
