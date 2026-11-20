package com.idraak.stepskyscraper.core.domain.usecase

import com.idraak.stepskyscraper.core.domain.AppClock
import com.idraak.stepskyscraper.core.domain.model.DomainError
import com.idraak.stepskyscraper.core.domain.model.FloorEvent
import com.idraak.stepskyscraper.core.domain.model.GoalPause
import com.idraak.stepskyscraper.core.domain.model.Outcome
import com.idraak.stepskyscraper.core.domain.model.StepSourceStatus
import com.idraak.stepskyscraper.core.domain.model.SyncResult
import com.idraak.stepskyscraper.core.domain.repository.AchievementRepository
import com.idraak.stepskyscraper.core.domain.repository.SettingsRepository
import com.idraak.stepskyscraper.core.domain.repository.StepSource
import com.idraak.stepskyscraper.core.domain.repository.StepsRepository
import com.idraak.stepskyscraper.core.domain.repository.SyncListener
import com.idraak.stepskyscraper.core.domain.repository.SyncStatusRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * One sync pass, safe to run any number of times (periodic worker, app open, pull-to-refresh):
 *  1. read daily totals from the step source (30 days on first run, otherwise the last 7),
 *  2. store them as totals (days older than 7 days are frozen),
 *  3. record floor events for today's new floors (for time-of-day achievements),
 *  4. expire a finished goal pause, award achievements and notify widgets.
 * Nothing here ever subtracts progress.
 */
@Singleton
class SyncStepsUseCase @Inject constructor(
    private val source: StepSource,
    private val steps: StepsRepository,
    private val settings: SettingsRepository,
    private val syncStatus: SyncStatusRepository,
    private val achievementsRepository: AchievementRepository,
    private val awardAchievements: AwardAchievementsUseCase,
    private val ensureTowers: EnsureTowerRecordsUseCase,
    private val floors: CalculateFloorsUseCase,
    private val clock: AppClock,
    private val listeners: Set<@JvmSuppressWildcards SyncListener>,
) {
    private val mutex = Mutex()

    suspend operator fun invoke(): Outcome<SyncResult> = mutex.withLock {
        val now = clock.now()
        syncStatus.markStarted(now)
        val result = runSync()
        when (result) {
            is Outcome.Success -> {
                syncStatus.markSucceeded(clock.now(), dataChanged = result.value.daysWritten > 0)
                listeners.forEach { it.onSynced() }
            }
            is Outcome.Failure -> syncStatus.markFailed(clock.now(), result.error)
        }
        result
    }

    private suspend fun runSync(): Outcome<SyncResult> {
        val status = source.status()
        sourceError(status)?.let { return Outcome.Failure(it) }
        val current = settings.settings.first()
        val today = clock.today()
        val from = today.minusDays(if (current.historyImported) RECENT_DAYS - 1L else HISTORY_DAYS - 1L)
        val before = (steps.days(today, today) as? Outcome.Success)?.value?.firstOrNull()?.steps ?: 0

        val read = source.readDailyTotals(from, today)
        if (read is Outcome.Failure) return read
        val totals = (read as Outcome.Success).value
        // Never move a day backwards: sources can briefly report less while they deduplicate.
        val stored = (steps.days(from, today) as? Outcome.Success)?.value.orEmpty().associateBy { it.date }
        val merged = totals.map { day -> stored[day.date]?.takeIf { it.steps > day.steps } ?: day }

        val written = steps.upsertDays(merged, frozenBefore = today.minusDays(RECENT_DAYS.toLong()))
        if (written is Outcome.Failure) return written
        if (!current.historyImported) settings.setHistoryImported()

        val after = merged.firstOrNull { it.date == today }?.steps ?: before
        val newFloors = floors.floorsFor(after) - floors.floorsFor(before)
        if (current.buildStartDate != null && !today.isBefore(current.buildStartDate) && newFloors > 0) {
            val firstNew = floors.floorsFor(before) + 1
            steps.recordFloorEvents((firstNew until firstNew + newFloors).map { FloorEvent(today, it, clock.now()) })
        }
        expirePause(current.goals.pause, today)
        if (current.onboardingComplete) {
            ensureTowers()
            awardAchievements()
        }
        return Outcome.Success(SyncResult((written as Outcome.Success).value, newFloors.coerceAtLeast(0), after))
    }

    private suspend fun expirePause(pause: GoalPause, today: java.time.LocalDate) {
        if (pause is GoalPause.Until && today.isAfter(pause.lastPausedDay)) {
            settings.setPause(GoalPause.None)
            achievementsRepository.increment(CalculateCityStatsUseCase.COUNTER_PAUSE_RETURNS)
        }
    }

    private fun sourceError(status: StepSourceStatus): DomainError? = when (status) {
        StepSourceStatus.HealthConnectReady -> null
        StepSourceStatus.HealthConnectPermissionMissing -> DomainError.StepPermissionDenied
        StepSourceStatus.HealthConnectNotInstalled -> DomainError.HealthConnectNotInstalled
        StepSourceStatus.HealthConnectNeedsUpdate -> DomainError.HealthConnectNeedsUpdate
        is StepSourceStatus.Sensor -> if (status.permissionGranted) null else DomainError.StepPermissionDenied
        StepSourceStatus.Unavailable -> DomainError.NoStepSource
    }

    companion object {
        const val HISTORY_DAYS = 30
        const val RECENT_DAYS = 7
    }
}
