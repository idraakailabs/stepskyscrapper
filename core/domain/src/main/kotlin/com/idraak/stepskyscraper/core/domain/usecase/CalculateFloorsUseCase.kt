package com.idraak.stepskyscraper.core.domain.usecase

import com.idraak.stepskyscraper.core.domain.model.DayProgress
import com.idraak.stepskyscraper.core.domain.model.FloorMath
import java.time.LocalDate
import javax.inject.Inject

/**
 * Floors are computed per local day: every full 1,000 steps builds a floor, the remainder fills the ten
 * 100-step segments of the next floor and starts fresh tomorrow. Negative or absurd totals are clamped.
 */
class CalculateFloorsUseCase @Inject constructor() {
    operator fun invoke(date: LocalDate, steps: Int): DayProgress {
        val safe = steps.coerceIn(0, MAX_PLAUSIBLE_DAILY_STEPS)
        return DayProgress(
            date = date,
            steps = safe,
            floors = safe / FloorMath.STEPS_PER_FLOOR,
            stepsIntoFloor = safe % FloorMath.STEPS_PER_FLOOR,
        )
    }

    fun floorsFor(steps: Int): Int = steps.coerceIn(0, MAX_PLAUSIBLE_DAILY_STEPS) / FloorMath.STEPS_PER_FLOOR

    companion object {
        /** Anything above this in a single day is treated as a data error from the source. */
        const val MAX_PLAUSIBLE_DAILY_STEPS = 150_000
    }
}
