package ai.idraak.stepskyscraper.core.domain

import ai.idraak.stepskyscraper.core.domain.model.DaySteps
import ai.idraak.stepskyscraper.core.domain.model.FacadeKit
import ai.idraak.stepskyscraper.core.domain.model.StepSourceKind
import ai.idraak.stepskyscraper.core.domain.model.TowerRecord
import ai.idraak.stepskyscraper.core.domain.usecase.CalculateFloorsUseCase
import ai.idraak.stepskyscraper.core.domain.usecase.CityCalculator
import ai.idraak.stepskyscraper.core.domain.usecase.GenerateTowerNameUseCase
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class CityCalculatorTest {
    private val calculator = CityCalculator(CalculateFloorsUseCase(), GenerateTowerNameUseCase())
    private val start = LocalDate.of(2026, 11, 1)
    private val ladder = listOf(10, 25, 50)

    private fun day(offset: Long, steps: Int) = DaySteps(start.plusDays(offset), steps, StepSourceKind.HealthConnect)

    private fun record(index: Int, target: Int) = TowerRecord(
        index = index,
        name = "Tower $index",
        look = CityCalculator.defaultLook(FacadeKit.Deco),
        targetFloors = target,
        foundedOn = start,
        ceremonySeen = false,
        rewardedAdClaimed = false,
    )

    @Test
    fun `floors flow into the first tower`() {
        val city = calculator.calculate(listOf(day(0, 6_660), day(1, 2_100)), start, listOf(record(1, 10)), ladder)
        assertThat(city.totalFloors).isEqualTo(8)
        assertThat(city.activeTower.index).isEqualTo(1)
        assertThat(city.activeTower.floorsBuilt).isEqualTo(8)
        assertThat(city.buildDays).isEqualTo(2)
    }

    @Test
    fun `overflow on the topping out day starts the next tower`() {
        val city = calculator.calculate(listOf(day(0, 7_000), day(1, 5_400)), start, listOf(record(1, 10)), ladder)
        val first = city.towers[0]
        assertThat(first.isComplete).isTrue()
        assertThat(first.toppedOutOn).isEqualTo(start.plusDays(1))
        assertThat(city.activeTower.index).isEqualTo(2)
        assertThat(city.activeTower.floorsBuilt).isEqualTo(2)
        assertThat(city.activeTower.target).isEqualTo(25)
        assertThat(city.activeTower.startedOn).isEqualTo(start.plusDays(1))
    }

    @Test
    fun `history before the build start never builds floors`() {
        val days = listOf(DaySteps(start.minusDays(3), 12_000, StepSourceKind.HealthConnect), day(0, 3_000))
        val city = calculator.calculate(days, start, listOf(record(1, 10)), ladder)
        assertThat(city.totalFloors).isEqualTo(3)
    }

    @Test
    fun `calculation is deterministic and idempotent`() {
        val days = (0L..20L).map { day(it, 4_000 + (it * 731 % 5_000).toInt()) }
        val a = calculator.calculate(days, start, listOf(record(1, 10)), ladder)
        val b = calculator.calculate(days.shuffled(), start, listOf(record(1, 10)), ladder)
        assertThat(a).isEqualTo(b)
    }

    @Test
    fun `frozen record target wins over a changed ladder`() {
        val city = calculator.calculate(listOf(day(0, 12_000)), start, listOf(record(1, 24)), listOf(10, 25))
        assertThat(city.towers[0].target).isEqualTo(24)
        assertThat(city.towers[0].isComplete).isFalse()
    }

    @Test
    fun `best day and build days are tracked per tower`() {
        val city = calculator.calculate(listOf(day(0, 3_000), day(1, 5_000), day(2, 0)), start, listOf(record(1, 10)), ladder)
        val tower = city.activeTower
        assertThat(tower.bestDay?.floors).isEqualTo(5)
        assertThat(tower.bestDay?.date).isEqualTo(start.plusDays(1))
        assertThat(tower.buildDays).isEqualTo(2)
    }
}
