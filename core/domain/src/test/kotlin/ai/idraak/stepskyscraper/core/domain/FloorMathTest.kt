package ai.idraak.stepskyscraper.core.domain

import ai.idraak.stepskyscraper.core.domain.usecase.BlueprintLadder
import ai.idraak.stepskyscraper.core.domain.usecase.CalculateFloorsUseCase
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class FloorMathTest {
    private val calc = CalculateFloorsUseCase()
    private val day = LocalDate.of(2026, 11, 20)

    @Test
    fun `every 1000 steps builds one floor and the remainder fills segments`() {
        val p = calc(day, 6_660)
        assertThat(p.floors).isEqualTo(6)
        assertThat(p.stepsIntoFloor).isEqualTo(660)
        assertThat(p.stepsToNextFloor).isEqualTo(340)
        assertThat(p.segmentsFilled).isEqualTo(6)
        assertThat(p.activeSegmentFraction).isWithin(0.001f).of(0.6f)
    }

    @Test
    fun `zero steps means next floor in 1000`() {
        val p = calc(day, 0)
        assertThat(p.floors).isEqualTo(0)
        assertThat(p.stepsToNextFloor).isEqualTo(1_000)
        assertThat(p.segmentsFilled).isEqualTo(0)
    }

    @Test
    fun `onboarding example 4200 steps is 4 floors and floor 5 in 800`() {
        val p = calc(day, 4_200)
        assertThat(p.floors).isEqualTo(4)
        assertThat(p.stepsToNextFloor).isEqualTo(800)
    }

    @Test
    fun `negative and absurd values are clamped`() {
        assertThat(calc(day, -50).floors).isEqualTo(0)
        assertThat(calc(day, 10_000_000).floors).isEqualTo(CalculateFloorsUseCase.MAX_PLAUSIBLE_DAILY_STEPS / 1_000)
    }

    @Test
    fun `exact multiples land on a fresh floor`() {
        val p = calc(day, 7_000)
        assertThat(p.floors).isEqualTo(7)
        assertThat(p.stepsIntoFloor).isEqualTo(0)
    }

    @Test
    fun `ladder follows 10 25 50 100 160 250 then repeats the top`() {
        val targets = (1..8).map { BlueprintLadder.targetFor(it, BlueprintLadder.DEFAULT) }
        assertThat(targets).containsExactly(10, 25, 50, 100, 160, 250, 250, 250).inOrder()
    }

    @Test
    fun `remote ladder is used when valid and rejected when malformed`() {
        assertThat(BlueprintLadder.targetFor(2, listOf(12, 24, 48))).isEqualTo(24)
        assertThat(BlueprintLadder.sanitize(emptyList())).isEqualTo(BlueprintLadder.DEFAULT)
        assertThat(BlueprintLadder.sanitize(listOf(10, 5))).isEqualTo(BlueprintLadder.DEFAULT)
        assertThat(BlueprintLadder.sanitize(listOf(0, 5))).isEqualTo(BlueprintLadder.DEFAULT)
    }

    @Test
    fun `blueprints are five per floor`() {
        assertThat(BlueprintLadder.blueprintsFor(24)).isEqualTo(120)
    }
}
