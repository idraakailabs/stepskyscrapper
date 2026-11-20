package com.idraak.stepskyscraper.core.domain.repository

import com.idraak.stepskyscraper.core.domain.model.AdAvailability
import com.idraak.stepskyscraper.core.domain.model.AnalyticsEvent
import com.idraak.stepskyscraper.core.domain.model.Appearance
import com.idraak.stepskyscraper.core.domain.model.DaySteps
import com.idraak.stepskyscraper.core.domain.model.Entitlement
import com.idraak.stepskyscraper.core.domain.model.FloorEvent
import com.idraak.stepskyscraper.core.domain.model.GoalPause
import com.idraak.stepskyscraper.core.domain.model.Outcome
import com.idraak.stepskyscraper.core.domain.model.PassPlan
import com.idraak.stepskyscraper.core.domain.model.PlanOffer
import com.idraak.stepskyscraper.core.domain.model.StepSourceStatus
import com.idraak.stepskyscraper.core.domain.model.SyncStatus
import com.idraak.stepskyscraper.core.domain.model.TargetPreset
import com.idraak.stepskyscraper.core.domain.model.TowerLook
import com.idraak.stepskyscraper.core.domain.model.TowerRecord
import com.idraak.stepskyscraper.core.domain.model.UiHost
import com.idraak.stepskyscraper.core.domain.model.UserSettings
import com.idraak.stepskyscraper.core.domain.model.WalletEntry
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** Daily step totals stored on the device (Room). */
interface StepsRepository {
    fun observeDays(from: LocalDate, to: LocalDate): Flow<List<DaySteps>>

    fun observeAllDays(): Flow<List<DaySteps>>

    suspend fun days(from: LocalDate, to: LocalDate): Outcome<List<DaySteps>>

    /**
     * Writes totals. Days strictly before [frozenBefore] that already exist are never changed (history is
     * frozen after 7 days); missing ones are inserted. Returns the number of rows written.
     */
    suspend fun upsertDays(days: List<DaySteps>, frozenBefore: LocalDate): Outcome<Int>

    fun observeFloorEvents(): Flow<List<FloorEvent>>

    suspend fun recordFloorEvents(events: List<FloorEvent>): Outcome<Unit>
}

/** A device step source (Health Connect primary, sensor fallback). Never sends data off-device. */
interface StepSource {
    suspend fun status(): StepSourceStatus

    /** Daily totals for each date in [from]..[to] (inclusive), local time zone. */
    suspend fun readDailyTotals(from: LocalDate, to: LocalDate): Outcome<List<DaySteps>>

    /** Whether reading in the background is supported and granted (READ_HEALTH_DATA_IN_BACKGROUND). */
    suspend fun backgroundReadAvailable(): Boolean
}

interface TowerRepository {
    fun observeTowerRecords(): Flow<List<TowerRecord>>

    suspend fun towerRecords(): Outcome<List<TowerRecord>>

    suspend fun upsert(record: TowerRecord): Outcome<Unit>

    suspend fun rename(index: Int, name: String): Outcome<Unit>

    suspend fun updateLook(index: Int, look: TowerLook): Outcome<Unit>

    suspend fun markCeremonySeen(index: Int): Outcome<Unit>

    suspend fun markRewardedAdClaimed(index: Int): Outcome<Unit>
}

interface SettingsRepository {
    val settings: Flow<UserSettings>

    suspend fun completeOnboarding(startDate: LocalDate): Outcome<Unit>

    suspend fun setDailyTarget(steps: Int, preset: TargetPreset): Outcome<Unit>

    suspend fun setPause(pause: GoalPause): Outcome<Unit>

    suspend fun setAppearance(appearance: Appearance): Outcome<Unit>

    suspend fun setNotifications(toppingOut: Boolean, weeklySummary: Boolean): Outcome<Unit>

    suspend fun setHistoryImported(): Outcome<Unit>

    suspend fun setPaywallShownAfterFirstTower(): Outcome<Unit>

    suspend fun setWidgetPromptHandled(): Outcome<Unit>

    suspend fun setStepsConnectSkipped(skipped: Boolean): Outcome<Unit>

    suspend fun setShareShowStats(show: Boolean): Outcome<Unit>

    suspend fun setCeremonySound(enabled: Boolean): Outcome<Unit>
}

/** Sync bookkeeping (last success / last data change) persisted locally. */
interface SyncStatusRepository {
    val status: Flow<SyncStatus>

    suspend fun markStarted(at: Instant)

    suspend fun markSucceeded(at: Instant, dataChanged: Boolean)

    suspend fun markFailed(at: Instant, error: com.idraak.stepskyscraper.core.domain.model.DomainError)
}

/** Blueprint ledger. Entries are keyed so awarding the same thing twice is a no-op. */
interface WalletRepository {
    val balance: Flow<Int>

    suspend fun hasEntry(key: String): Boolean

    suspend fun addEntry(entry: WalletEntry): Outcome<Boolean>
}

/** Cosmetics bought with Blueprints. */
interface UnlockRepository {
    val purchasedItemIds: Flow<Set<String>>

    suspend fun markPurchased(itemId: String): Outcome<Unit>
}

interface AchievementRepository {
    /** Achievement id → day it was earned. */
    val earned: Flow<Map<String, LocalDate>>

    suspend fun markEarned(ids: Map<String, LocalDate>): Outcome<Unit>

    /** Achievement-local counters that cannot be derived from day totals (shares, makeovers, pause returns). */
    val counters: Flow<Map<String, Int>>

    suspend fun increment(counter: String): Outcome<Unit>
}

/** Blueprint ladder from Remote Config with a bundled default. */
interface LadderRepository {
    val ladder: Flow<List<Int>>

    suspend fun refresh(): Outcome<Unit>
}

interface EntitlementRepository {
    val entitlement: Flow<Entitlement>

    val offers: Flow<List<PlanOffer>>

    suspend fun refresh(): Outcome<Unit>

    suspend fun purchase(plan: PassPlan, host: UiHost): Outcome<Unit>

    suspend fun restore(): Outcome<Entitlement>
}

interface RewardedAdGateway {
    val availability: Flow<AdAvailability>

    /** Gathers UMP consent (if required) and preloads. Never called on Build, onboarding or the ceremony. */
    suspend fun prepare(host: UiHost)

    /** Shows the ad; Success means the user earned the reward. */
    suspend fun show(host: UiHost): Outcome<Unit>
}

interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
}

/** Schedules background sync and refreshes widgets. */
interface SyncScheduler {
    fun schedulePeriodic()

    fun requestImmediate()
}

/** Called after every successful sync so home-screen widgets can redraw. */
fun interface SyncListener {
    suspend fun onSynced()
}
