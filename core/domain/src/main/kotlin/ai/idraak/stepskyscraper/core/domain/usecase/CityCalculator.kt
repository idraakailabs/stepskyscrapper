package ai.idraak.stepskyscraper.core.domain.usecase

import ai.idraak.stepskyscraper.core.domain.cosmetics.CosmeticCatalog
import ai.idraak.stepskyscraper.core.domain.cosmetics.NATIVE_CROWN
import ai.idraak.stepskyscraper.core.domain.cosmetics.NATIVE_PALETTE
import ai.idraak.stepskyscraper.core.domain.model.BestDay
import ai.idraak.stepskyscraper.core.domain.model.City
import ai.idraak.stepskyscraper.core.domain.model.DaySteps
import ai.idraak.stepskyscraper.core.domain.model.FacadeKit
import ai.idraak.stepskyscraper.core.domain.model.Tower
import ai.idraak.stepskyscraper.core.domain.model.TowerLook
import ai.idraak.stepskyscraper.core.domain.model.TowerRecord
import java.time.LocalDate
import javax.inject.Inject

/**
 * Derives the whole city from stored day totals. Nothing is accumulated: running this twice on the same input
 * gives the same city, which is what makes syncs idempotent and progress impossible to lose.
 *
 * Floors flow into towers in order; a tower that tops out mid-day passes the overflow to the next tower.
 */
class CityCalculator @Inject constructor(
    private val floors: CalculateFloorsUseCase,
    private val names: GenerateTowerNameUseCase,
) {
    fun calculate(
        days: List<DaySteps>,
        buildStartDate: LocalDate,
        records: List<TowerRecord>,
        ladder: List<Int>,
    ): City {
        val byIndex = records.associateBy { it.index }
        val accumulators = mutableListOf<Acc>()

        fun accFor(index: Int, foundedOn: LocalDate): Acc {
            while (accumulators.size < index) {
                val i = accumulators.size + 1
                val record = byIndex[i] ?: defaultRecord(i, accumulators.lastOrNull()?.record, foundedOn, ladder)
                accumulators += Acc(record)
            }
            return accumulators[index - 1]
        }

        var current = accFor(1, buildStartDate)
        days.asSequence()
            .filter { !it.date.isBefore(buildStartDate) }
            .sortedBy { it.date }
            .forEach { day ->
                var remaining = floors.floorsFor(day.steps)
                val dayFloors = remaining
                if (current.startedOn == null) current.startedOn = day.date
                if (remaining == 0) {
                    current.steps += day.steps
                    return@forEach
                }
                while (remaining > 0) {
                    val take = minOf(remaining, current.record.targetFloors - current.floors)
                    current.floors += take
                    current.buildDays += 1
                    current.steps += day.steps.toLong() * take / dayFloors
                    if (current.startedOn == null) current.startedOn = day.date
                    val best = current.best
                    if (best == null || take > best.floors) current.best = BestDay(day.date, take)
                    remaining -= take
                    if (current.floors >= current.record.targetFloors) {
                        current.toppedOutOn = day.date
                        current = accFor(current.record.index + 1, day.date)
                        current.startedOn = day.date
                    }
                }
            }
        // Make sure every persisted record (e.g. a freshly named next tower) is represented.
        records.maxOfOrNull { it.index }?.let { accFor(it, buildStartDate) }

        val towers = accumulators.map { it.toTower() }
        val active = towers.firstOrNull { !it.isComplete } ?: towers.last()
        return City(
            towers = towers,
            activeTower = active,
            totalFloors = towers.sumOf { it.floorsBuilt },
            totalSteps = days.filter { !it.date.isBefore(buildStartDate) }.sumOf { it.steps.toLong() },
            buildDays = days.count { !it.date.isBefore(buildStartDate) && floors.floorsFor(it.steps) > 0 },
        )
    }

    /** Record used for a tower the user has not styled yet: same look as the previous tower, generated name. */
    fun defaultRecord(index: Int, previous: TowerRecord?, foundedOn: LocalDate, ladder: List<Int>): TowerRecord =
        TowerRecord(
            index = index,
            name = names(seed = index.toLong()),
            look = previous?.look ?: defaultLook(FacadeKit.Deco),
            targetFloors = BlueprintLadder.targetFor(index, ladder),
            foundedOn = foundedOn,
            ceremonySeen = false,
            rewardedAdClaimed = false,
        )

    private class Acc(val record: TowerRecord) {
        var floors = 0
        var buildDays = 0
        var steps = 0L
        var startedOn: LocalDate? = null
        var toppedOutOn: LocalDate? = null
        var best: BestDay? = null

        fun toTower() = Tower(
            record = record,
            floorsBuilt = floors,
            startedOn = startedOn,
            toppedOutOn = toppedOutOn,
            buildDays = buildDays,
            steps = steps,
            bestDay = best,
        )
    }

    companion object {
        fun defaultLook(kit: FacadeKit) = TowerLook(
            facadeId = CosmeticCatalog.facadeForKit(kit).id,
            paletteId = NATIVE_PALETTE,
            crownId = NATIVE_CROWN,
            lightsId = "lights_warm",
            groundId = if (kit == FacadeKit.Eco) "ground_park" else "ground_plaza",
        )
    }
}
