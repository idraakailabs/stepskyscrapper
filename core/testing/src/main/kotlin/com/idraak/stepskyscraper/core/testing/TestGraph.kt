package com.idraak.stepskyscraper.core.testing

import com.idraak.stepskyscraper.core.domain.model.FacadeKit
import com.idraak.stepskyscraper.core.domain.model.TowerRecord
import com.idraak.stepskyscraper.core.domain.model.UserSettings
import com.idraak.stepskyscraper.core.domain.usecase.AwardAchievementsUseCase
import com.idraak.stepskyscraper.core.domain.usecase.CalculateArchitectLevelUseCase
import com.idraak.stepskyscraper.core.domain.usecase.CalculateCityStatsUseCase
import com.idraak.stepskyscraper.core.domain.usecase.CalculateFloorsUseCase
import com.idraak.stepskyscraper.core.domain.usecase.CalculateWeekProgressUseCase
import com.idraak.stepskyscraper.core.domain.usecase.CityCalculator
import com.idraak.stepskyscraper.core.domain.usecase.ClaimRewardedBonusUseCase
import com.idraak.stepskyscraper.core.domain.usecase.CompleteTowerUseCase
import com.idraak.stepskyscraper.core.domain.usecase.CustomiseTowerUseCase
import com.idraak.stepskyscraper.core.domain.usecase.EnsureTowerRecordsUseCase
import com.idraak.stepskyscraper.core.domain.usecase.GenerateTowerNameUseCase
import com.idraak.stepskyscraper.core.domain.usecase.GetTowerProgressUseCase
import com.idraak.stepskyscraper.core.domain.usecase.ObserveUnlockContextUseCase
import com.idraak.stepskyscraper.core.domain.usecase.PauseGoalsUseCase
import com.idraak.stepskyscraper.core.domain.usecase.ResolveCosmeticLockUseCase
import com.idraak.stepskyscraper.core.domain.usecase.StartBuildingUseCase
import com.idraak.stepskyscraper.core.domain.usecase.StyleNextTowerUseCase
import com.idraak.stepskyscraper.core.domain.usecase.SuggestDailyTargetUseCase
import com.idraak.stepskyscraper.core.domain.usecase.SyncStepsUseCase
import com.idraak.stepskyscraper.core.domain.usecase.UnlockCosmeticUseCase
import java.time.LocalDate

/** Real use cases wired to in-memory fakes, for ViewModel and use-case tests without Hilt. */
class TestGraph(val clock: TestClock = TestClock()) {
    val source = FakeStepSource()
    val steps = FakeStepsRepository()
    val towers = FakeTowerRepository()
    val settings = FakeSettingsRepository()
    val syncStatus = FakeSyncStatusRepository()
    val wallet = FakeWalletRepository()
    val unlocks = FakeUnlockRepository()
    val achievements = FakeAchievementRepository()
    val ladder = FakeLadderRepository()
    val entitlements = FakeEntitlementRepository()
    val ads = FakeRewardedAdGateway()
    val analytics = FakeAnalyticsTracker()
    val scheduler = FakeSyncScheduler()

    val floors = CalculateFloorsUseCase()
    val names = GenerateTowerNameUseCase()
    val level = CalculateArchitectLevelUseCase()
    val week = CalculateWeekProgressUseCase(floors)
    val cityCalculator = CityCalculator(floors, names)
    val cityStats = CalculateCityStatsUseCase(floors, level)
    val suggest = SuggestDailyTargetUseCase()
    val resolveLock = ResolveCosmeticLockUseCase()
    val progress = GetTowerProgressUseCase(
        steps, towers, settings, ladder, achievements, wallet, entitlements, clock, cityCalculator, floors, week, cityStats, level,
    )
    val completeTower = CompleteTowerUseCase(wallet, entitlements, clock)
    val ensureTowers = EnsureTowerRecordsUseCase(progress, towers, completeTower)
    val award = AwardAchievementsUseCase(progress, achievements, wallet, clock)
    val sync = SyncStepsUseCase(source, steps, settings, syncStatus, achievements, award, ensureTowers, floors, clock, emptySet())
    val unlockContext = ObserveUnlockContextUseCase(progress, unlocks)
    val customise = CustomiseTowerUseCase(towers, achievements, unlockContext, resolveLock)
    val unlock = UnlockCosmeticUseCase(wallet, unlocks, clock)
    val pause = PauseGoalsUseCase(settings, achievements, clock)
    val claimBonus = ClaimRewardedBonusUseCase(progress, towers, wallet, ads, clock)
    val startBuilding = StartBuildingUseCase(settings, towers, ladder, cityCalculator, clock)
    val styleNext = StyleNextTowerUseCase(progress, towers)

    val today: LocalDate get() = clock.today()

    /** Onboarded user whose floors started counting [daysAgo] days ago, first tower targeting [target]. */
    suspend fun onboarded(daysAgo: Long = 7, target: Int = 24, name: String = "Marlow Spire", kit: FacadeKit = FacadeKit.Deco) {
        settings.state.value =
            UserSettings.Default.copy(onboardingComplete = true, buildStartDate = today.minusDays(daysAgo), historyImported = true)
        towers.upsert(
            TowerRecord(
                1,
                name,
                CityCalculator.defaultLook(kit),
                target,
                today.minusDays(daysAgo),
                ceremonySeen = false,
                rewardedAdClaimed = false,
            ),
        )
    }
}
