package com.idraak.stepskyscraper.core.domain.usecase

import com.idraak.stepskyscraper.core.domain.AppClock
import com.idraak.stepskyscraper.core.domain.achievements.AchievementCatalog
import com.idraak.stepskyscraper.core.domain.achievements.AchievementProgress
import com.idraak.stepskyscraper.core.domain.achievements.CityStats
import com.idraak.stepskyscraper.core.domain.model.ArchitectLevel
import com.idraak.stepskyscraper.core.domain.model.City
import com.idraak.stepskyscraper.core.domain.model.DayProgress
import com.idraak.stepskyscraper.core.domain.model.DaySteps
import com.idraak.stepskyscraper.core.domain.model.District
import com.idraak.stepskyscraper.core.domain.model.Entitlement
import com.idraak.stepskyscraper.core.domain.model.FloorEvent
import com.idraak.stepskyscraper.core.domain.model.TowerRecord
import com.idraak.stepskyscraper.core.domain.model.UserSettings
import com.idraak.stepskyscraper.core.domain.model.WeekProgress
import com.idraak.stepskyscraper.core.domain.repository.AchievementRepository
import com.idraak.stepskyscraper.core.domain.repository.EntitlementRepository
import com.idraak.stepskyscraper.core.domain.repository.LadderRepository
import com.idraak.stepskyscraper.core.domain.repository.SettingsRepository
import com.idraak.stepskyscraper.core.domain.repository.StepsRepository
import com.idraak.stepskyscraper.core.domain.repository.TowerRepository
import com.idraak.stepskyscraper.core.domain.repository.WalletRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** Everything the screens show about the city, derived in one place from stored facts. */
data class CitySnapshot(
    val today: LocalDate,
    val settings: UserSettings,
    val city: City,
    val todayProgress: DayProgress,
    val week: WeekProgress,
    val stats: CityStats,
    val level: ArchitectLevel,
    val achievements: List<AchievementProgress>,
    val blueprints: Int,
    val entitlement: Entitlement,
    val days: List<DaySteps>,
) {
    val districts: List<District>
        get() = city.finishedTowers.groupBy { (it.index - 1) / District.TOWERS_PER_DISTRICT }
            .map { (i, towers) -> District(i + 1, DistrictNames.forDistrict(i), towers, towers.firstOrNull()?.startedOn) }

    /** District the active tower belongs to (1-based). */
    val currentDistrictIndex: Int get() = (city.activeTower.index - 1) / District.TOWERS_PER_DISTRICT + 1
}

/**
 * Observes the derived city. Emits nothing useful until onboarding has set a build start date; the
 * returned flow then recomputes whenever a day total, tower record, ladder or wallet entry changes.
 */
class GetTowerProgressUseCase @Inject constructor(
    private val steps: StepsRepository,
    private val towers: TowerRepository,
    private val settingsRepository: SettingsRepository,
    private val ladderRepository: LadderRepository,
    private val achievements: AchievementRepository,
    private val wallet: WalletRepository,
    private val entitlements: EntitlementRepository,
    private val clock: AppClock,
    private val cityCalculator: CityCalculator,
    private val floors: CalculateFloorsUseCase,
    private val week: CalculateWeekProgressUseCase,
    private val cityStats: CalculateCityStatsUseCase,
    private val level: CalculateArchitectLevelUseCase,
) {
    operator fun invoke(): Flow<CitySnapshot> {
        val facts = combine(
            steps.observeAllDays(),
            steps.observeFloorEvents(),
            towers.observeTowerRecords(),
            settingsRepository.settings,
            ladderRepository.ladder,
        ) { days, events, records, settings, ladder -> Facts(days, events, records, settings, ladder) }
        val extras = combine(
            achievements.earned,
            achievements.counters,
            wallet.balance,
            entitlements.entitlement,
        ) { earned, counters, balance, entitlement -> Extras(earned, counters, balance, entitlement) }
        return combine(facts, extras) { f, e -> snapshot(f, e) }.distinctUntilChanged()
    }

    private fun snapshot(f: Facts, e: Extras): CitySnapshot {
        val today = clock.today()
        val start = f.settings.buildStartDate ?: today
        val city = cityCalculator.calculate(f.days, start, f.records, f.ladder)
        val todaySteps = f.days.firstOrNull { it.date == today }?.steps ?: 0
        val stats = cityStats(city, f.days, f.events, f.settings.goals, start, clock.zone(), e.counters)
        return CitySnapshot(
            today = today,
            settings = f.settings,
            city = city,
            todayProgress = floors(today, if (today.isBefore(start)) 0 else todaySteps),
            week = week(f.days, f.settings.goals, today, start),
            stats = stats,
            level = level(city.totalFloors),
            achievements = AchievementCatalog.all.map { def ->
                AchievementProgress(def, def.measure(stats).coerceAtMost(def.target), e.earned[def.id])
            },
            blueprints = e.balance,
            entitlement = e.entitlement,
            days = f.days,
        )
    }

    private data class Facts(
        val days: List<DaySteps>,
        val events: List<FloorEvent>,
        val records: List<TowerRecord>,
        val settings: UserSettings,
        val ladder: List<Int>,
    )

    private data class Extras(
        val earned: Map<String, LocalDate>,
        val counters: Map<String, Int>,
        val balance: Int,
        val entitlement: Entitlement,
    )
}
