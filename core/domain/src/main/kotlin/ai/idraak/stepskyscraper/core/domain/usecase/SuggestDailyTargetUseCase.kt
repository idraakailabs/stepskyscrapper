package ai.idraak.stepskyscraper.core.domain.usecase

import ai.idraak.stepskyscraper.core.domain.model.DaySteps
import ai.idraak.stepskyscraper.core.domain.model.FloorMath
import ai.idraak.stepskyscraper.core.domain.model.Goals
import ai.idraak.stepskyscraper.core.domain.model.TargetPreset
import ai.idraak.stepskyscraper.core.domain.model.TargetSuggestion
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.ceil

/**
 * Adaptive daily target. Suggestion = average of the last 7 complete days rounded up to the next 1,000
 * (6,400 → 7,000), clamped to 2,000–20,000. Days without data count as zero only when there is some data
 * in the window; with no meaningful data (average under 1,000) the Steady preset is suggested.
 */
class SuggestDailyTargetUseCase @Inject constructor() {
    operator fun invoke(days: List<DaySteps>, today: LocalDate): TargetSuggestion {
        val window = (1L..7L).map { today.minusDays(it) }.toSet()
        val inWindow = days.filter { it.date in window }
        val average = if (inWindow.isEmpty()) 0 else inWindow.sumOf { it.steps } / WINDOW
        // No meaningful history (new phone, source just connected): suggest Steady rather than a tiny target.
        if (average < MIN_MEANINGFUL_AVERAGE) {
            return TargetSuggestion(average, TargetPreset.Steady.dailySteps, TargetPreset.Steady)
        }
        val rounded = (ceil(average / FloorMath.STEPS_PER_FLOOR.toDouble()) * FloorMath.STEPS_PER_FLOOR).toInt()
            .coerceIn(MIN_TARGET, MAX_TARGET)
        val preset = TargetPreset.entries.firstOrNull { it != TargetPreset.Custom && it.dailySteps == rounded }
            ?: TargetPreset.Custom
        return TargetSuggestion(average, rounded, preset)
    }

    /**
     * Weekly re-check used by Me › Goals: if the last 7 days averaged at least 20% above the current target
     * (and goals are not paused), offer the higher suggestion. Never suggests lowering automatically.
     */
    fun shouldOfferRaise(goals: Goals, suggestion: TargetSuggestion, today: LocalDate): Boolean =
        !goals.isPausedOn(today) &&
            suggestion.sevenDayAverage >= goals.dailyTargetSteps * RAISE_THRESHOLD &&
            suggestion.suggestedDailySteps > goals.dailyTargetSteps

    /** Custom targets move in steps of 500. */
    fun normalizeCustom(steps: Int): Int =
        ((steps + CUSTOM_STEP / 2) / CUSTOM_STEP * CUSTOM_STEP).coerceIn(MIN_TARGET, MAX_TARGET)

    companion object {
        const val WINDOW = 7
        const val MIN_TARGET = 2_000
        const val MAX_TARGET = 20_000
        const val CUSTOM_STEP = 500
        private const val MIN_MEANINGFUL_AVERAGE = 1_000
        private const val RAISE_THRESHOLD = 1.2
    }
}
