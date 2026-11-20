package com.idraak.stepskyscraper.core.domain.usecase

import com.idraak.stepskyscraper.core.domain.achievements.CityStats
import com.idraak.stepskyscraper.core.domain.cosmetics.CosmeticCatalog
import com.idraak.stepskyscraper.core.domain.model.City
import com.idraak.stepskyscraper.core.domain.model.DaySteps
import com.idraak.stepskyscraper.core.domain.model.District
import com.idraak.stepskyscraper.core.domain.model.FloorEvent
import com.idraak.stepskyscraper.core.domain.model.Goals
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

/** Folds the derived city, raw days and observed floor events into [CityStats]. Pure. */
class CalculateCityStatsUseCase @Inject constructor(
    private val floors: CalculateFloorsUseCase,
    private val level: CalculateArchitectLevelUseCase,
) {
    @Suppress("LongParameterList")
    operator fun invoke(
        city: City,
        days: List<DaySteps>,
        events: List<FloorEvent>,
        goals: Goals,
        buildStartDate: LocalDate,
        zone: ZoneId,
        counters: Map<String, Int>,
    ): CityStats {
        val counted = days.filter { !it.date.isBefore(buildStartDate) }
        val finished = city.finishedTowers
        val nightDays = events.filter { localTime(it, zone) >= NIGHT_SHIFT_FROM }.map { it.date }.toSet().size
        val earlyDays = events.filter { localTime(it, zone) < EARLY_RISER_BEFORE }.map { it.date }.toSet().size
        val weeksReached = counted.groupBy { CalculateWeekProgressUseCase.weekStartOf(it.date) }
            .count { (_, week) -> week.sumOf { floors.floorsFor(it.steps) } >= goals.weeklyFloors && goals.weeklyFloors > 0 }
        val districtCounts = finished.groupBy { (it.index - 1) / District.TOWERS_PER_DISTRICT }.values.map { it.size }
        return CityStats(
            totalFloors = city.totalFloors,
            towersFinished = finished.size,
            buildDays = city.buildDays,
            architectLevel = level(city.totalFloors).level,
            maxFloorsInOneDay = counted.maxOfOrNull { floors.floorsFor(it.steps) } ?: 0,
            nightShiftDays = nightDays,
            earlyRiserDays = earlyDays,
            weeksGoalReached = weeksReached,
            finishedKits = finished.map { CosmeticCatalog.kitOf(it.record.look.facadeId) }.toSet().size,
            largestFinishedTower = finished.maxOfOrNull { it.target } ?: 0,
            fullestDistrictTowers = districtCounts.maxOrNull() ?: 0,
            districtsFounded = (city.towers.size - 1) / District.TOWERS_PER_DISTRICT + 1,
            shares = counters[COUNTER_SHARES] ?: 0,
            makeovers = counters[COUNTER_MAKEOVERS] ?: 0,
            pauseReturns = counters[COUNTER_PAUSE_RETURNS] ?: 0,
        )
    }

    private fun localTime(event: FloorEvent, zone: ZoneId): LocalTime = event.detectedAt.atZone(zone).toLocalTime()

    companion object {
        const val COUNTER_SHARES = "shares"
        const val COUNTER_MAKEOVERS = "makeovers"
        const val COUNTER_PAUSE_RETURNS = "pause_returns"
        private val NIGHT_SHIFT_FROM: LocalTime = LocalTime.of(21, 0)
        private val EARLY_RISER_BEFORE: LocalTime = LocalTime.of(8, 0)
    }
}
