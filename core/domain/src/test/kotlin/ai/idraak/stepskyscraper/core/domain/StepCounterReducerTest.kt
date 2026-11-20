package ai.idraak.stepskyscraper.core.domain

import ai.idraak.stepskyscraper.core.domain.usecase.StepCounterReducer
import ai.idraak.stepskyscraper.core.domain.usecase.StepCounterState
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class StepCounterReducerTest {
    private val reducer = StepCounterReducer()
    private val day = LocalDate.of(2026, 11, 20)

    @Test
    fun `first reading only stores the baseline`() {
        val s = reducer.reduce(null, 12_345, day)
        assertThat(s).isEqualTo(StepCounterState(12_345, day, 0))
    }

    @Test
    fun `deltas are added to the day`() {
        var s = reducer.reduce(null, 1_000, day)
        s = reducer.reduce(s, 1_800, day)
        s = reducer.reduce(s, 2_500, day)
        assertThat(s.todaySteps).isEqualTo(1_500)
    }

    @Test
    fun `reboot is detected when the counter decreases and rebases`() {
        var s = reducer.reduce(null, 50_000, day)
        s = reducer.reduce(s, 51_000, day)
        s = reducer.reduce(s, 300, day)
        assertThat(s.todaySteps).isEqualTo(1_300)
        assertThat(s.lastCounter).isEqualTo(300)
        s = reducer.reduce(s, 800, day)
        assertThat(s.todaySteps).isEqualTo(1_800)
    }

    @Test
    fun `a new day starts a fresh total`() {
        var s = reducer.reduce(null, 1_000, day)
        s = reducer.reduce(s, 4_000, day)
        s = reducer.reduce(s, 4_600, day.plusDays(1))
        assertThat(s.date).isEqualTo(day.plusDays(1))
        assertThat(s.todaySteps).isEqualTo(600)
    }

    @Test
    fun `implausible jumps are clamped`() {
        var s = reducer.reduce(null, 0, day)
        s = reducer.reduce(s, 5_000_000, day)
        assertThat(s.todaySteps.toLong()).isEqualTo(StepCounterReducer.MAX_DELTA_PER_READING)
    }
}
