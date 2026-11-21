package ai.idraak.stepskyscraper.core.domain.usecase

import ai.idraak.stepskyscraper.core.domain.AppClock
import ai.idraak.stepskyscraper.core.domain.model.DomainError
import ai.idraak.stepskyscraper.core.domain.model.FacadeKit
import ai.idraak.stepskyscraper.core.domain.model.Outcome
import ai.idraak.stepskyscraper.core.domain.model.TargetPreset
import ai.idraak.stepskyscraper.core.domain.model.Tower
import ai.idraak.stepskyscraper.core.domain.model.WalletEntry
import ai.idraak.stepskyscraper.core.domain.model.WalletReason
import ai.idraak.stepskyscraper.core.domain.repository.EntitlementRepository
import ai.idraak.stepskyscraper.core.domain.repository.LadderRepository
import ai.idraak.stepskyscraper.core.domain.repository.SettingsRepository
import ai.idraak.stepskyscraper.core.domain.repository.TowerRepository
import ai.idraak.stepskyscraper.core.domain.repository.WalletRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Persists a default record for every derived tower that has none yet (e.g. the tower after a topping-out). */
class EnsureTowerRecordsUseCase @Inject constructor(
    private val progress: GetTowerProgressUseCase,
    private val towers: TowerRepository,
    private val completeTower: CompleteTowerUseCase,
) {
    suspend operator fun invoke(): Outcome<Unit> {
        val snapshot = progress().first()
        val known = (towers.towerRecords() as? Outcome.Success)?.value.orEmpty().map { it.index }.toSet()
        snapshot.city.towers.filter { it.index !in known }.forEach { tower ->
            val result = towers.upsert(tower.record)
            if (result is Outcome.Failure) return result
        }
        snapshot.city.finishedTowers.forEach { completeTower(it) }
        return Outcome.Success(Unit)
    }
}

/**
 * Awards the topping-out Blueprints for a finished tower (5 per floor, doubled for towers topped out while an
 * Architect Pass member). Keyed ledger entries make this idempotent; returns the total the tower has earned.
 * A tower earns at most 2× its base: the Pass bonus and the rewarded-ad bonus exclude each other.
 */
class CompleteTowerUseCase @Inject constructor(
    private val wallet: WalletRepository,
    private val entitlements: EntitlementRepository,
    private val clock: AppClock,
) {
    suspend operator fun invoke(tower: Tower): Outcome<Int> {
        if (!tower.isComplete) return Outcome.Failure(DomainError.ItemLocked("tower:${tower.index}"))
        val base = BlueprintLadder.blueprintsFor(tower.target)
        val add = wallet.addEntry(WalletEntry(baseKey(tower.index), base, WalletReason.ToppingOut, clock.now()))
        if (add is Outcome.Failure) return add
        val firstAward = (add as Outcome.Success).value
        // BUG-002: the Pass bonus is decided once, when the topping-out is first recorded, so buying the Pass
        // later never doubles earlier towers; and a tower that already has the ad bonus never gets a second one.
        if (firstAward && !wallet.hasEntry(adKey(tower.index)) && entitlements.entitlement.first().isPremium) {
            wallet.addEntry(WalletEntry(premiumKey(tower.index), base, WalletReason.PremiumBonus, clock.now()))
        }
        var total = base
        if (wallet.hasEntry(premiumKey(tower.index))) total += base
        if (wallet.hasEntry(adKey(tower.index))) total += base
        return Outcome.Success(total)
    }

    companion object {
        fun baseKey(index: Int) = "tower:$index"

        fun premiumKey(index: Int) = "tower_premium:$index"

        fun adKey(index: Int) = "tower_ad:$index"
    }
}

/** Onboarding "Lay the foundation": goal, first tower and the day floors start counting. */
class StartBuildingUseCase @Inject constructor(
    private val settings: SettingsRepository,
    private val towers: TowerRepository,
    private val ladder: LadderRepository,
    private val calculator: CityCalculator,
    private val clock: AppClock,
) {
    suspend operator fun invoke(name: String, kit: FacadeKit, dailyTarget: Int, preset: TargetPreset): Outcome<Unit> {
        val today = clock.today()
        val record = calculator.defaultRecord(1, null, today, ladder.ladder.first()).copy(
            name = name.trim().ifEmpty { calculator.defaultRecord(1, null, today, BlueprintLadder.DEFAULT).name },
            look = CityCalculator.defaultLook(kit),
        )
        val steps = listOf(
            suspend { towers.upsert(record) },
            suspend { settings.setDailyTarget(dailyTarget, preset) },
            suspend { settings.completeOnboarding(today) },
        )
        for (step in steps) {
            val result = step()
            if (result is Outcome.Failure) return result
        }
        return Outcome.Success(Unit)
    }
}

/** "Start next tower": names and styles the tower that is now under construction. */
class StyleNextTowerUseCase @Inject constructor(
    private val progress: GetTowerProgressUseCase,
    private val towers: TowerRepository,
) {
    suspend operator fun invoke(name: String, kit: FacadeKit): Outcome<Unit> {
        val active = progress().first().city.activeTower
        val trimmed = name.trim().take(GenerateTowerNameUseCase.MAX_LENGTH)
        val updated = active.record.copy(
            name = trimmed.ifEmpty { active.name },
            look = CityCalculator.defaultLook(kit).copy(lightsId = active.record.look.lightsId),
        )
        return towers.upsert(updated)
    }
}
