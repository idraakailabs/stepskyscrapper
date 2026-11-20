package ai.idraak.stepskyscraper.core.domain.usecase

import java.time.LocalDate
import javax.inject.Inject

/**
 * State kept for the TYPE_STEP_COUNTER fallback. The sensor reports steps since boot, so we store the last
 * raw counter as a baseline and add the deltas to the current day's total.
 */
data class StepCounterState(
    val lastCounter: Long?,
    val date: LocalDate,
    val todaySteps: Int,
)

/**
 * Pure reducer for sensor readings:
 *  - first reading only sets the baseline;
 *  - a counter lower than the baseline means the phone rebooted: the new counter is the delta (rebase);
 *  - a new local date starts a fresh total; steps since the previous reading land on the new day
 *    (the sensor has no timestamps per step, see docs/design-deviations.md);
 *  - implausible jumps are clamped so a sensor glitch can never build a tower at once.
 */
class StepCounterReducer @Inject constructor() {
    fun reduce(state: StepCounterState?, counter: Long, date: LocalDate): StepCounterState {
        if (counter < 0) return state ?: StepCounterState(null, date, 0)
        if (state?.lastCounter == null) {
            val keep = if (state != null && state.date == date) state.todaySteps else 0
            return StepCounterState(counter, date, keep)
        }
        val raw = counter - state.lastCounter
        val delta = (if (raw < 0) counter else raw).coerceIn(0, MAX_DELTA_PER_READING).toInt()
        val base = if (state.date == date) state.todaySteps else 0
        return StepCounterState(counter, date, base + delta)
    }

    companion object {
        const val MAX_DELTA_PER_READING = 60_000L
    }
}
