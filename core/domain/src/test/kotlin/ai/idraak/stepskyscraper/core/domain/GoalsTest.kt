package ai.idraak.stepskyscraper.core.domain

import ai.idraak.stepskyscraper.core.domain.model.DaySteps
import ai.idraak.stepskyscraper.core.domain.model.GoalPause
import ai.idraak.stepskyscraper.core.domain.model.Goals
import ai.idraak.stepskyscraper.core.domain.model.StepSourceKind
import ai.idraak.stepskyscraper.core.domain.model.TargetPreset
import ai.idraak.stepskyscraper.core.domain.usecase.CalculateArchitectLevelUseCase
import ai.idraak.stepskyscraper.core.domain.usecase.CalculateFloorsUseCase
import ai.idraak.stepskyscraper.core.domain.usecase.CalculateWeekProgressUseCase
import ai.idraak.stepskyscraper.core.domain.usecase.SkyClock
import ai.idraak.stepskyscraper.core.domain.usecase.SkyTime
import ai.idraak.stepskyscraper.core.domain.usecase.SuggestDailyTargetUseCase
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Test

class GoalsTest {
    private val week = CalculateWeekProgressUseCase(CalculateFloorsUseCase())
    private val friday = LocalDate.of(2026, 11, 20)
    private val steady = Goals(7_000, TargetPreset.Steady, GoalPause.None)

    private fun d(date: LocalDate, steps: Int) = DaySteps(date, steps, StepSourceKind.HealthConnect)

    @Test
    fun `weekly goal is daily target times seven in floors`() {
        assertThat(steady.weeklyFloors).isEqualTo(49)
        assertThat(Goals(10_000, TargetPreset.Ambitious, GoalPause.None).weeklyFloors).isEqualTo(70)
    }

    @Test
    fun `week sums monday to today and counts days left including today`() {
        val monday = friday.minusDays(4)
        val days = (0L..4L).map { d(monday.plusDays(it), 7_500) }
        val p = week(days, steady, friday, monday.minusDays(10))
        assertThat(p.floors).isEqualTo(35)
        assertThat(p.daysLeftIncludingToday).isEqualTo(3)
        assertThat(p.days.filter { it.isFuture }).hasSize(2)
        assertThat(p.percent).isEqualTo(71)
        assertThat(p.floorsToGo).isEqualTo(14)
    }

    @Test
    fun `paused goals are reported and floors still count`() {
        val paused = steady.copy(pause = GoalPause.Until(friday.plusDays(2)))
        val p = week(listOf(d(friday, 3_000)), paused, friday, friday)
        assertThat(p.paused).isTrue()
        assertThat(p.floors).isEqualTo(3)
        assertThat(paused.isPausedOn(friday.plusDays(3))).isFalse()
    }

    @Test
    fun `adaptive target rounds the 7 day average up to the next thousand`() {
        val days = (1L..7L).map { d(friday.minusDays(it), 6_400) }
        val s = SuggestDailyTargetUseCase()(days, friday)
        assertThat(s.sevenDayAverage).isEqualTo(6_400)
        assertThat(s.suggestedDailySteps).isEqualTo(7_000)
        assertThat(s.suggestedPreset).isEqualTo(TargetPreset.Steady)
    }

    @Test
    fun `adaptive target without data suggests steady and custom moves in 500s`() {
        val use = SuggestDailyTargetUseCase()
        assertThat(use(emptyList(), friday).suggestedDailySteps).isEqualTo(7_000)
        assertThat(use((1L..7L).map { d(friday.minusDays(it), 0) }, friday).suggestedPreset).isEqualTo(TargetPreset.Steady)
        assertThat(use.normalizeCustom(8_260)).isEqualTo(8_500)
        assertThat(use.normalizeCustom(100)).isEqualTo(SuggestDailyTargetUseCase.MIN_TARGET)
    }

    @Test
    fun `raise is offered only when the week clearly beats the target`() {
        val use = SuggestDailyTargetUseCase()
        val high = use((1L..7L).map { d(friday.minusDays(it), 9_000) }, friday)
        assertThat(use.shouldOfferRaise(steady, high, friday)).isTrue()
        val close = use((1L..7L).map { d(friday.minusDays(it), 7_400) }, friday)
        assertThat(use.shouldOfferRaise(steady, close, friday)).isFalse()
    }

    @Test
    fun `architect level 7 is site architect with 100 of 120`() {
        val level = CalculateArchitectLevelUseCase()(100)
        assertThat(level.level).isEqualTo(7)
        assertThat(level.title).isEqualTo("Site Architect")
        assertThat(level.nextLevelAtFloors).isEqualTo(120)
        assertThat(level.nextTitle).isEqualTo("Chief Architect")
        assertThat(level.fraction).isWithin(0.01f).of(0.62f)
    }

    @Test
    fun `sky follows the clock with a 20 minute crossfade`() {
        assertThat(SkyClock.timeAt(LocalTime.of(6, 0))).isEqualTo(SkyTime.Dawn)
        assertThat(SkyClock.timeAt(LocalTime.of(12, 0))).isEqualTo(SkyTime.Day)
        assertThat(SkyClock.timeAt(LocalTime.of(18, 0))).isEqualTo(SkyTime.Dusk)
        assertThat(SkyClock.timeAt(LocalTime.of(23, 0))).isEqualTo(SkyTime.Night)
        val fade = SkyClock.phaseAt(LocalTime.of(17, 30))
        assertThat(fade.from).isEqualTo(SkyTime.Day)
        assertThat(fade.to).isEqualTo(SkyTime.Dusk)
        assertThat(fade.blend).isWithin(0.01f).of(0.5f)
        assertThat(SkyClock.phaseAt(LocalTime.of(12, 0)).blend).isEqualTo(0f)
    }
}
