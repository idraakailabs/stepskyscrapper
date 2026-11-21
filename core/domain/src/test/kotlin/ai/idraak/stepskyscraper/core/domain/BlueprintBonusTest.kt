package ai.idraak.stepskyscraper.core.domain

import ai.idraak.stepskyscraper.core.domain.model.Entitlement
import ai.idraak.stepskyscraper.core.domain.model.FacadeKit
import ai.idraak.stepskyscraper.core.domain.model.Outcome
import ai.idraak.stepskyscraper.core.domain.model.PassPlan
import ai.idraak.stepskyscraper.core.domain.model.PlanOffer
import ai.idraak.stepskyscraper.core.domain.model.Tower
import ai.idraak.stepskyscraper.core.domain.model.TowerRecord
import ai.idraak.stepskyscraper.core.domain.model.UiHost
import ai.idraak.stepskyscraper.core.domain.model.WalletEntry
import ai.idraak.stepskyscraper.core.domain.model.WalletReason
import ai.idraak.stepskyscraper.core.domain.repository.EntitlementRepository
import ai.idraak.stepskyscraper.core.domain.repository.WalletRepository
import ai.idraak.stepskyscraper.core.domain.usecase.BlueprintLadder
import ai.idraak.stepskyscraper.core.domain.usecase.CityCalculator
import ai.idraak.stepskyscraper.core.domain.usecase.CompleteTowerUseCase
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** BUG-002: the Pass bonus is never retroactive and a tower never earns more than 2× its base Blueprints. */
class BlueprintBonusTest {
    private val wallet = MemoryWallet()
    private val entitlements = SwitchableEntitlement()
    private val clock = object : AppClock {
        override fun now(): Instant = Instant.parse("2026-11-21T12:00:00Z")

        override fun zone(): ZoneId = ZoneId.of("UTC")
    }
    private val complete = CompleteTowerUseCase(wallet, entitlements, clock)

    private fun tower(index: Int, target: Int = 10) = Tower(
        record = TowerRecord(
            index,
            "T$index",
            CityCalculator.defaultLook(FacadeKit.Deco),
            target,
            LocalDate.of(2026, 11, 1),
            ceremonySeen = true,
            rewardedAdClaimed = false,
        ),
        floorsBuilt = target,
        startedOn = LocalDate.of(2026, 11, 1),
        toppedOutOn = LocalDate.of(2026, 11, 10),
        buildDays = 5,
        steps = target * 1_000L,
        bestDay = null,
    )

    @Test
    fun `buying the Pass after three towers does not change their Blueprints`() = runTest {
        val towers = (1..3).map { tower(it) }
        towers.forEach { complete(it) }
        val before = wallet.total()

        entitlements.premium = true
        towers.forEach { complete(it) } // every later sync re-runs completion for finished towers

        assertThat(wallet.total()).isEqualTo(before)
        towers.forEach { assertThat(wallet.hasEntry(CompleteTowerUseCase.premiumKey(it.index))).isFalse() }
    }

    @Test
    fun `a tower topped out while premium earns the Pass bonus once`() = runTest {
        entitlements.premium = true
        val t = tower(4)
        val first = complete(t)
        val again = complete(t)

        val base = BlueprintLadder.blueprintsFor(t.target)
        assertThat((first as Outcome.Success).value).isEqualTo(2 * base)
        assertThat((again as Outcome.Success).value).isEqualTo(2 * base)
        assertThat(wallet.total()).isEqualTo(2 * base)
    }

    @Test
    fun `ad bonus then Pass never exceeds 2x`() = runTest {
        val t = tower(1)
        complete(t)
        val base = BlueprintLadder.blueprintsFor(t.target)
        wallet.addEntry(WalletEntry(CompleteTowerUseCase.adKey(1), base, WalletReason.RewardedAd, clock.now()))

        entitlements.premium = true
        val total = complete(t)

        assertThat((total as Outcome.Success).value).isEqualTo(2 * base)
        assertThat(wallet.total()).isEqualTo(2 * base)
    }

    @Test
    fun `ad claimed before the first completion blocks the Pass bonus`() = runTest {
        val t = tower(2)
        val base = BlueprintLadder.blueprintsFor(t.target)
        wallet.addEntry(WalletEntry(CompleteTowerUseCase.adKey(2), base, WalletReason.RewardedAd, clock.now()))
        entitlements.premium = true

        complete(t)

        assertThat(wallet.total()).isEqualTo(2 * base)
    }

    private class MemoryWallet : WalletRepository {
        private val entries = MutableStateFlow<Map<String, WalletEntry>>(emptyMap())
        override val balance: Flow<Int> = entries.map { m -> m.values.sumOf { it.amount } }

        fun total() = entries.value.values.sumOf { it.amount }

        override suspend fun hasEntry(key: String) = key in entries.value

        override suspend fun addEntry(entry: WalletEntry): Outcome<Boolean> {
            if (entry.key in entries.value) return Outcome.Success(false)
            entries.update { it + (entry.key to entry) }
            return Outcome.Success(true)
        }
    }

    private class SwitchableEntitlement : EntitlementRepository {
        var premium = false
        override val entitlement: Flow<Entitlement>
            get() = flowOf(if (premium) Entitlement(true, PassPlan.Annual, null) else Entitlement.Free)
        override val offers: Flow<List<PlanOffer>> = flowOf(emptyList())

        override suspend fun refresh(): Outcome<Unit> = Outcome.Success(Unit)

        override suspend fun purchase(plan: PassPlan, host: UiHost): Outcome<Unit> = Outcome.Success(Unit)

        override suspend fun restore(): Outcome<Entitlement> = Outcome.Success(Entitlement.Free)
    }
}
