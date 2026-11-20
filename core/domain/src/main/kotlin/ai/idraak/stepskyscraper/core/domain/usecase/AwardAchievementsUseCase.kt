package ai.idraak.stepskyscraper.core.domain.usecase

import ai.idraak.stepskyscraper.core.domain.AppClock
import ai.idraak.stepskyscraper.core.domain.achievements.AchievementDefinition
import ai.idraak.stepskyscraper.core.domain.achievements.AchievementReward
import ai.idraak.stepskyscraper.core.domain.model.Outcome
import ai.idraak.stepskyscraper.core.domain.model.WalletEntry
import ai.idraak.stepskyscraper.core.domain.model.WalletReason
import ai.idraak.stepskyscraper.core.domain.repository.AchievementRepository
import ai.idraak.stepskyscraper.core.domain.repository.WalletRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Marks newly reached achievements as earned and pays Blueprint rewards (cosmetic rewards unlock by rule). */
class AwardAchievementsUseCase @Inject constructor(
    private val progress: GetTowerProgressUseCase,
    private val achievements: AchievementRepository,
    private val wallet: WalletRepository,
    private val clock: AppClock,
) {
    suspend operator fun invoke(): Outcome<List<AchievementDefinition>> {
        val snapshot = progress().first()
        val newlyEarned = snapshot.achievements
            .filter { !it.isEarned && it.progress >= it.definition.target }
            .map { it.definition }
        if (newlyEarned.isEmpty()) return Outcome.Success(emptyList())
        val today = clock.today()
        val marked = achievements.markEarned(newlyEarned.associate { it.id to today })
        if (marked is Outcome.Failure) return marked
        newlyEarned.forEach { def ->
            val reward = def.reward
            if (reward is AchievementReward.Blueprints) {
                wallet.addEntry(WalletEntry("ach:${def.id}", reward.amount, WalletReason.Achievement, clock.now()))
            }
        }
        return Outcome.Success(newlyEarned)
    }
}
