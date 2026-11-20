package com.idraak.stepskyscraper.core.testing

import com.idraak.stepskyscraper.core.domain.AppClock
import com.idraak.stepskyscraper.core.domain.model.AdAvailability
import com.idraak.stepskyscraper.core.domain.model.AnalyticsEvent
import com.idraak.stepskyscraper.core.domain.model.Appearance
import com.idraak.stepskyscraper.core.domain.model.DaySteps
import com.idraak.stepskyscraper.core.domain.model.DomainError
import com.idraak.stepskyscraper.core.domain.model.Entitlement
import com.idraak.stepskyscraper.core.domain.model.FloorEvent
import com.idraak.stepskyscraper.core.domain.model.GoalPause
import com.idraak.stepskyscraper.core.domain.model.Outcome
import com.idraak.stepskyscraper.core.domain.model.PassPlan
import com.idraak.stepskyscraper.core.domain.model.PlanOffer
import com.idraak.stepskyscraper.core.domain.model.StepSourceStatus
import com.idraak.stepskyscraper.core.domain.model.SyncState
import com.idraak.stepskyscraper.core.domain.model.SyncStatus
import com.idraak.stepskyscraper.core.domain.model.TargetPreset
import com.idraak.stepskyscraper.core.domain.model.TowerLook
import com.idraak.stepskyscraper.core.domain.model.TowerRecord
import com.idraak.stepskyscraper.core.domain.model.UiHost
import com.idraak.stepskyscraper.core.domain.model.UserSettings
import com.idraak.stepskyscraper.core.domain.model.WalletEntry
import com.idraak.stepskyscraper.core.domain.repository.AchievementRepository
import com.idraak.stepskyscraper.core.domain.repository.AnalyticsTracker
import com.idraak.stepskyscraper.core.domain.repository.EntitlementRepository
import com.idraak.stepskyscraper.core.domain.repository.LadderRepository
import com.idraak.stepskyscraper.core.domain.repository.RewardedAdGateway
import com.idraak.stepskyscraper.core.domain.repository.SettingsRepository
import com.idraak.stepskyscraper.core.domain.repository.StepSource
import com.idraak.stepskyscraper.core.domain.repository.StepsRepository
import com.idraak.stepskyscraper.core.domain.repository.SyncScheduler
import com.idraak.stepskyscraper.core.domain.repository.SyncStatusRepository
import com.idraak.stepskyscraper.core.domain.repository.TowerRepository
import com.idraak.stepskyscraper.core.domain.repository.UnlockRepository
import com.idraak.stepskyscraper.core.domain.repository.WalletRepository
import com.idraak.stepskyscraper.core.domain.usecase.BlueprintLadder
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class TestClock(var current: LocalDateTime = LocalDateTime.of(2026, 11, 20, 14, 0)) : AppClock {
    override fun now(): Instant = current.toInstant(ZoneOffset.UTC)

    override fun zone(): ZoneId = ZoneOffset.UTC
}

class FakeStepSource(
    var status: StepSourceStatus = StepSourceStatus.HealthConnectReady,
    val totals: MutableMap<LocalDate, Int> = mutableMapOf(),
    var failure: DomainError? = null,
) : StepSource {
    var reads = 0

    override suspend fun status(): StepSourceStatus = status

    override suspend fun readDailyTotals(from: LocalDate, to: LocalDate): Outcome<List<DaySteps>> {
        reads++
        failure?.let { return Outcome.Failure(it) }
        return Outcome.Success(
            generateSequence(from) { d -> d.plusDays(1).takeIf { !it.isAfter(to) } }
                .map { DaySteps(it, totals[it] ?: 0, com.idraak.stepskyscraper.core.domain.model.StepSourceKind.HealthConnect) }
                .toList(),
        )
    }

    override suspend fun backgroundReadAvailable(): Boolean = true
}

class FakeStepsRepository : StepsRepository {
    val days = MutableStateFlow<Map<LocalDate, DaySteps>>(emptyMap())
    val events = MutableStateFlow<List<FloorEvent>>(emptyList())

    fun set(vararg pairs: Pair<LocalDate, Int>) {
        days.value =
            pairs.associate { (d, s) -> d to DaySteps(d, s, com.idraak.stepskyscraper.core.domain.model.StepSourceKind.HealthConnect) }
    }

    override fun observeDays(from: LocalDate, to: LocalDate): Flow<List<DaySteps>> =
        days.map { m -> m.values.filter { !it.date.isBefore(from) && !it.date.isAfter(to) }.sortedBy { it.date } }

    override fun observeAllDays(): Flow<List<DaySteps>> = days.map { m -> m.values.sortedBy { it.date } }

    override suspend fun days(from: LocalDate, to: LocalDate): Outcome<List<DaySteps>> =
        Outcome.Success(days.value.values.filter { !it.date.isBefore(from) && !it.date.isAfter(to) })

    override suspend fun upsertDays(days: List<DaySteps>, frozenBefore: LocalDate): Outcome<Int> {
        var changed = 0
        this.days.update { current ->
            val next = current.toMutableMap()
            days.forEach { d ->
                val old = next[d.date]
                if (old == null || (!d.date.isBefore(frozenBefore) && old.steps != d.steps)) {
                    next[d.date] = d
                    changed++
                }
            }
            next
        }
        return Outcome.Success(changed)
    }

    override fun observeFloorEvents(): Flow<List<FloorEvent>> = events

    override suspend fun recordFloorEvents(events: List<FloorEvent>): Outcome<Unit> {
        this.events.update { it + events }
        return Outcome.Success(Unit)
    }
}

class FakeTowerRepository : TowerRepository {
    val records = MutableStateFlow<Map<Int, TowerRecord>>(emptyMap())

    override fun observeTowerRecords(): Flow<List<TowerRecord>> = records.map { it.values.sortedBy { r -> r.index } }

    override suspend fun towerRecords(): Outcome<List<TowerRecord>> = Outcome.Success(records.value.values.sortedBy { it.index })

    override suspend fun upsert(record: TowerRecord): Outcome<Unit> {
        records.update { it + (record.index to record) }
        return Outcome.Success(Unit)
    }

    private fun modify(index: Int, f: (TowerRecord) -> TowerRecord): Outcome<Unit> {
        records.update { m -> m[index]?.let { m + (index to f(it)) } ?: m }
        return Outcome.Success(Unit)
    }

    override suspend fun rename(index: Int, name: String) = modify(index) { it.copy(name = name) }

    override suspend fun updateLook(index: Int, look: TowerLook) = modify(index) { it.copy(look = look) }

    override suspend fun markCeremonySeen(index: Int) = modify(index) { it.copy(ceremonySeen = true) }

    override suspend fun markRewardedAdClaimed(index: Int) = modify(index) { it.copy(rewardedAdClaimed = true) }
}

class FakeSettingsRepository(initial: UserSettings = UserSettings.Default) : SettingsRepository {
    val state = MutableStateFlow(initial)
    override val settings: Flow<UserSettings> = state

    private fun edit(f: (UserSettings) -> UserSettings): Outcome<Unit> {
        state.update(f)
        return Outcome.Success(Unit)
    }

    override suspend fun completeOnboarding(startDate: LocalDate) =
        edit { it.copy(onboardingComplete = true, buildStartDate = it.buildStartDate ?: startDate) }

    override suspend fun setDailyTarget(steps: Int, preset: TargetPreset) = edit {
        it.copy(goals = it.goals.copy(dailyTargetSteps = steps, preset = preset))
    }

    override suspend fun setPause(pause: GoalPause) = edit { it.copy(goals = it.goals.copy(pause = pause)) }

    override suspend fun setAppearance(appearance: Appearance) = edit { it.copy(appearance = appearance) }

    override suspend fun setNotifications(toppingOut: Boolean, weeklySummary: Boolean) =
        edit { it.copy(notifyToppingOut = toppingOut, notifyWeeklySummary = weeklySummary) }

    override suspend fun setHistoryImported() = edit { it.copy(historyImported = true) }

    override suspend fun setPaywallShownAfterFirstTower() = edit { it.copy(paywallShownAfterFirstTower = true) }

    override suspend fun setWidgetPromptHandled() = edit { it.copy(widgetPromptHandled = true) }

    override suspend fun setStepsConnectSkipped(skipped: Boolean) = edit { it.copy(stepsConnectSkipped = skipped) }

    override suspend fun setShareShowStats(show: Boolean) = edit { it.copy(shareShowStats = show) }

    override suspend fun setCeremonySound(enabled: Boolean) = edit { it.copy(ceremonySound = enabled) }
}

class FakeSyncStatusRepository : SyncStatusRepository {
    val state = MutableStateFlow(SyncStatus(SyncState.Idle, null, null, null, null))
    override val status: Flow<SyncStatus> = state

    override suspend fun markStarted(at: Instant) = state.update { it.copy(state = SyncState.Syncing, lastAttemptAt = at) }

    override suspend fun markSucceeded(at: Instant, dataChanged: Boolean) = state.update {
        it.copy(
            state = SyncState.Idle,
            lastSuccessAt = at,
            lastError = null,
            lastDataChangeAt = if (dataChanged) {
                at
            } else {
                it.lastDataChangeAt
                    ?: at
            },
        )
    }

    override suspend fun markFailed(at: Instant, error: DomainError) = state.update { it.copy(state = SyncState.Idle, lastError = error) }
}

class FakeWalletRepository : WalletRepository {
    val entries = MutableStateFlow<Map<String, WalletEntry>>(emptyMap())
    override val balance: Flow<Int> = entries.map { m -> m.values.sumOf { it.amount } }

    override suspend fun hasEntry(key: String): Boolean = key in entries.value

    override suspend fun addEntry(entry: WalletEntry): Outcome<Boolean> {
        if (entry.key in entries.value) return Outcome.Success(false)
        entries.update { it + (entry.key to entry) }
        return Outcome.Success(true)
    }
}

class FakeUnlockRepository : UnlockRepository {
    val ids = MutableStateFlow<Set<String>>(emptySet())
    override val purchasedItemIds: Flow<Set<String>> = ids

    override suspend fun markPurchased(itemId: String): Outcome<Unit> {
        ids.update { it + itemId }
        return Outcome.Success(Unit)
    }
}

class FakeAchievementRepository : AchievementRepository {
    val earnedState = MutableStateFlow<Map<String, LocalDate>>(emptyMap())
    val counterState = MutableStateFlow<Map<String, Int>>(emptyMap())
    override val earned: Flow<Map<String, LocalDate>> = earnedState
    override val counters: Flow<Map<String, Int>> = counterState

    override suspend fun markEarned(ids: Map<String, LocalDate>): Outcome<Unit> {
        earnedState.update { current -> current + ids.filterKeys { it !in current } }
        return Outcome.Success(Unit)
    }

    override suspend fun increment(counter: String): Outcome<Unit> {
        counterState.update { it + (counter to ((it[counter] ?: 0) + 1)) }
        return Outcome.Success(Unit)
    }
}

class FakeLadderRepository(initial: List<Int> = BlueprintLadder.DEFAULT) : LadderRepository {
    val state = MutableStateFlow(initial)
    override val ladder: Flow<List<Int>> = state

    override suspend fun refresh(): Outcome<Unit> = Outcome.Success(Unit)
}

class FakeEntitlementRepository : EntitlementRepository {
    val state = MutableStateFlow(Entitlement.Free)
    var purchaseResult: Outcome<Unit> = Outcome.Success(Unit)
    val offersState = MutableStateFlow(
        listOf(
            PlanOffer(PassPlan.Annual, "$14.99", 14_990_000, "USD", 7),
            PlanOffer(PassPlan.Monthly, "$2.99", 2_990_000, "USD", 0),
            PlanOffer(PassPlan.Lifetime, "$29.99", 29_990_000, "USD", 0),
        ),
    )
    override val entitlement: Flow<Entitlement> = state
    override val offers: Flow<List<PlanOffer>> = offersState

    override suspend fun refresh(): Outcome<Unit> = Outcome.Success(Unit)

    override suspend fun purchase(plan: PassPlan, host: UiHost): Outcome<Unit> {
        if (purchaseResult is Outcome.Success) state.value = Entitlement(true, plan, null)
        return purchaseResult
    }

    override suspend fun restore(): Outcome<Entitlement> = Outcome.Success(state.value)
}

class FakeRewardedAdGateway : RewardedAdGateway {
    val state = MutableStateFlow(AdAvailability.Ready)
    var result: Outcome<Unit> = Outcome.Success(Unit)
    var shows = 0
    override val availability: Flow<AdAvailability> = state

    override suspend fun prepare(host: UiHost) = Unit

    override suspend fun show(host: UiHost): Outcome<Unit> {
        shows++
        return result
    }
}

class FakeAnalyticsTracker : AnalyticsTracker {
    val events = mutableListOf<AnalyticsEvent>()

    override fun track(event: AnalyticsEvent) {
        events += event
    }
}

class FakeSyncScheduler : SyncScheduler {
    var periodic = 0
    var immediate = 0

    override fun schedulePeriodic() {
        periodic++
    }

    override fun requestImmediate() {
        immediate++
    }
}

object TestHost : UiHost
