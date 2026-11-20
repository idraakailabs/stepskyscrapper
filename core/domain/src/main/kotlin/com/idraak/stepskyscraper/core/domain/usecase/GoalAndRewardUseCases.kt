package com.idraak.stepskyscraper.core.domain.usecase

import com.idraak.stepskyscraper.core.domain.AppClock
import com.idraak.stepskyscraper.core.domain.model.AdFailure
import com.idraak.stepskyscraper.core.domain.model.DomainError
import com.idraak.stepskyscraper.core.domain.model.GoalPause
import com.idraak.stepskyscraper.core.domain.model.Outcome
import com.idraak.stepskyscraper.core.domain.model.UiHost
import com.idraak.stepskyscraper.core.domain.model.WalletEntry
import com.idraak.stepskyscraper.core.domain.model.WalletReason
import com.idraak.stepskyscraper.core.domain.repository.AchievementRepository
import com.idraak.stepskyscraper.core.domain.repository.RewardedAdGateway
import com.idraak.stepskyscraper.core.domain.repository.SettingsRepository
import com.idraak.stepskyscraper.core.domain.repository.TowerRepository
import com.idraak.stepskyscraper.core.domain.repository.WalletRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

enum class PauseOption { ThreeDays, OneWeek, UntilResumed }

/** Pause goals for rest, travel or illness. Floors still count; only the weekly goal is set aside. */
class PauseGoalsUseCase @Inject constructor(
    private val settings: SettingsRepository,
    private val achievements: AchievementRepository,
    private val clock: AppClock,
) {
    suspend fun pause(option: PauseOption): Outcome<Unit> {
        val today = clock.today()
        val pause = when (option) {
            PauseOption.ThreeDays -> GoalPause.Until(today.plusDays(2))
            PauseOption.OneWeek -> GoalPause.Until(today.plusDays(6))
            PauseOption.UntilResumed -> GoalPause.Indefinite(today)
        }
        return settings.setPause(pause)
    }

    suspend fun resume(): Outcome<Unit> {
        val wasPaused = settings.settings.first().goals.pause != GoalPause.None
        val result = settings.setPause(GoalPause.None)
        if (result is Outcome.Success && wasPaused) achievements.increment(CalculateCityStatsUseCase.COUNTER_PAUSE_RETURNS)
        return result
    }
}

/**
 * The single opt-in rewarded ad: once per finished tower it doubles that tower's topping-out Blueprints.
 * It never grants floors, and Architect Pass members already get 2× so they never see it.
 */
class ClaimRewardedBonusUseCase @Inject constructor(
    private val progress: GetTowerProgressUseCase,
    private val towers: TowerRepository,
    private val wallet: WalletRepository,
    private val ads: RewardedAdGateway,
    private val clock: AppClock,
) {
    suspend operator fun invoke(towerIndex: Int, host: UiHost): Outcome<Int> {
        val snapshot = progress().first()
        if (snapshot.entitlement.isPremium) return Outcome.Failure(DomainError.AdUnavailable(AdFailure.Premium))
        val tower = snapshot.city.towers.firstOrNull { it.index == towerIndex && it.isComplete }
            ?: return Outcome.Failure(DomainError.ItemLocked("tower:$towerIndex"))
        if (tower.record.rewardedAdClaimed || wallet.hasEntry(CompleteTowerUseCase.adKey(towerIndex))) {
            return Outcome.Failure(DomainError.AlreadyClaimed)
        }
        val shown = ads.show(host)
        if (shown is Outcome.Failure) return shown
        val bonus = BlueprintLadder.blueprintsFor(tower.target)
        val added = wallet.addEntry(WalletEntry(CompleteTowerUseCase.adKey(towerIndex), bonus, WalletReason.RewardedAd, clock.now()))
        if (added is Outcome.Failure) return added
        towers.markRewardedAdClaimed(towerIndex)
        return Outcome.Success(bonus)
    }
}
