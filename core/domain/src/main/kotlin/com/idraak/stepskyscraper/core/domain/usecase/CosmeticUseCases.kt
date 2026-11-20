package com.idraak.stepskyscraper.core.domain.usecase

import com.idraak.stepskyscraper.core.domain.AppClock
import com.idraak.stepskyscraper.core.domain.cosmetics.CosmeticCatalog
import com.idraak.stepskyscraper.core.domain.cosmetics.UnlockRule
import com.idraak.stepskyscraper.core.domain.model.DomainError
import com.idraak.stepskyscraper.core.domain.model.Outcome
import com.idraak.stepskyscraper.core.domain.model.TowerLook
import com.idraak.stepskyscraper.core.domain.model.WalletEntry
import com.idraak.stepskyscraper.core.domain.model.WalletReason
import com.idraak.stepskyscraper.core.domain.repository.AchievementRepository
import com.idraak.stepskyscraper.core.domain.repository.TowerRepository
import com.idraak.stepskyscraper.core.domain.repository.UnlockRepository
import com.idraak.stepskyscraper.core.domain.repository.WalletRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Builds the unlock context (stats, premium, purchases, achievements, balance) for lock checks. */
class ObserveUnlockContextUseCase @Inject constructor(
    private val progress: GetTowerProgressUseCase,
    private val unlocks: UnlockRepository,
) {
    suspend operator fun invoke(): UnlockContext {
        val snapshot = progress().first()
        return UnlockContext(
            stats = snapshot.stats,
            isPremium = snapshot.entitlement.isPremium,
            purchased = unlocks.purchasedItemIds.first(),
            earnedAchievements = snapshot.achievements.filter { it.isEarned }.map { it.definition.id }.toSet(),
            blueprintBalance = snapshot.blueprints,
        )
    }
}

/**
 * Applies a look to any tower (finished towers can be re-skinned, never removed). Every part must be
 * unlocked; the first change counts towards the "Makeover" achievement.
 */
class CustomiseTowerUseCase @Inject constructor(
    private val towers: TowerRepository,
    private val achievements: AchievementRepository,
    private val unlockContext: ObserveUnlockContextUseCase,
    private val resolveLock: ResolveCosmeticLockUseCase,
) {
    suspend operator fun invoke(index: Int, look: TowerLook): Outcome<Unit> {
        val context = unlockContext()
        val ids = listOf(look.facadeId, look.paletteId, look.crownId, look.lightsId, look.groundId)
        ids.forEach { id ->
            val item = CosmeticCatalog.item(id) ?: return Outcome.Failure(DomainError.ItemLocked(id))
            if (resolveLock(item, context) != LockState.Unlocked) return Outcome.Failure(DomainError.ItemLocked(id))
        }
        val current = (towers.towerRecords() as? Outcome.Success)?.value?.firstOrNull { it.index == index }
        if (current?.look == look) return Outcome.Success(Unit)
        val result = towers.updateLook(index, look)
        if (result is Outcome.Success) achievements.increment(CalculateCityStatsUseCase.COUNTER_MAKEOVERS)
        return result
    }
}

/** Buys a Blueprint-priced cosmetic. Blueprints are cosmetic-only currency and can never buy floors. */
class UnlockCosmeticUseCase @Inject constructor(
    private val wallet: WalletRepository,
    private val unlocks: UnlockRepository,
    private val clock: AppClock,
) {
    suspend operator fun invoke(itemId: String): Outcome<Unit> {
        val item = CosmeticCatalog.item(itemId) ?: return Outcome.Failure(DomainError.ItemLocked(itemId))
        val rule = item.rule as? UnlockRule.Blueprints ?: return Outcome.Failure(DomainError.ItemLocked(itemId))
        if (itemId in unlocks.purchasedItemIds.first()) return Outcome.Success(Unit)
        val balance = wallet.balance.first()
        if (balance < rule.cost) return Outcome.Failure(DomainError.InsufficientBlueprints(rule.cost, balance))
        val paid = wallet.addEntry(WalletEntry("spend:$itemId", -rule.cost, WalletReason.Purchase, clock.now()))
        if (paid is Outcome.Failure) return paid
        return unlocks.markPurchased(itemId)
    }
}
