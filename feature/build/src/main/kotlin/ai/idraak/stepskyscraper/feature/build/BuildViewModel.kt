package ai.idraak.stepskyscraper.feature.build

import androidx.lifecycle.viewModelScope
import ai.idraak.stepskyscraper.core.domain.AppClock
import ai.idraak.stepskyscraper.core.domain.cosmetics.CosmeticCatalog
import ai.idraak.stepskyscraper.core.domain.model.Outcome
import ai.idraak.stepskyscraper.core.domain.model.StepSourceStatus
import ai.idraak.stepskyscraper.core.domain.model.SyncState
import ai.idraak.stepskyscraper.core.domain.model.SyncStatus
import ai.idraak.stepskyscraper.core.domain.repository.SettingsRepository
import ai.idraak.stepskyscraper.core.domain.repository.StepSource
import ai.idraak.stepskyscraper.core.domain.repository.SyncStatusRepository
import ai.idraak.stepskyscraper.core.domain.usecase.CitySnapshot
import ai.idraak.stepskyscraper.core.domain.usecase.GetTowerProgressUseCase
import ai.idraak.stepskyscraper.core.domain.usecase.SkyClock
import ai.idraak.stepskyscraper.core.domain.usecase.SyncStepsUseCase
import ai.idraak.stepskyscraper.core.ui.mvi.Lce
import ai.idraak.stepskyscraper.core.ui.mvi.MviViewModel
import ai.idraak.stepskyscraper.core.ui.mvi.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class BuildViewModel @Inject constructor(
    private val progress: GetTowerProgressUseCase,
    private val sync: SyncStepsUseCase,
    private val syncStatus: SyncStatusRepository,
    private val source: StepSource,
    private val settings: SettingsRepository,
    private val clock: AppClock,
) : MviViewModel<BuildUiState, BuildIntent, BuildEffect>(BuildUiState()) {
    private val accessStatus = MutableStateFlow<StepSourceStatus>(StepSourceStatus.HealthConnectReady)
    private var lastFloors: Int? = null
    private var dropKey = 0
    private var lastSyncRequest: Instant? = null
    private val ceremoniesOpened = mutableSetOf<Int>()

    /** Re-evaluates the sky and "updated X min ago" every minute. */
    private val ticker = flow {
        while (true) {
            emit(clock.now())
            delay(TICK_MS)
        }
    }

    init {
        viewModelScope.launch {
            combine(progress(), syncStatus.status, accessStatus, ticker) { snapshot, status, access, now ->
                Triple(snapshot, status, access) to now
            }
                .catch { e -> reduce { it.copy(content = Lce.Error(ai.idraak.stepskyscraper.core.domain.model.DomainError.Storage(e.message ?: "").toUiText())) } }
                .collect { (triple, now) -> onSnapshot(triple.first, triple.second, triple.third, now) }
        }
        syncNow(userInitiated = false)
    }

    override fun onIntent(intent: BuildIntent) {
        when (intent) {
            BuildIntent.Refresh -> syncNow(userInitiated = true)
            BuildIntent.Resumed -> {
                val last = lastSyncRequest
                if (last == null || Duration.between(last, clock.now()) > RESUME_THROTTLE) syncNow(userInitiated = false)
            }
            BuildIntent.OpenCustomise -> content()?.let { emit(BuildEffect.OpenCustomise(it.towerIndex)) }
            BuildIntent.OpenShare -> content()?.let { emit(BuildEffect.OpenShare(it.towerIndex)) }
            BuildIntent.OpenWeek -> emit(if (content()?.weekMessage == WeekMessage.NeedsHelp) BuildEffect.OpenStepHelp else BuildEffect.OpenWeekDetail)
            BuildIntent.ConnectSteps -> when (content()?.access) {
                StepAccessUi.HcMissing, StepAccessUi.HcNeedsUpdate -> emit(BuildEffect.InstallHealthConnect)
                else -> emit(BuildEffect.RequestStepAccess)
            }
            is BuildIntent.AccessResult -> viewModelScope.launch {
                if (intent.granted) settings.setStepsConnectSkipped(false)
                accessStatus.value = source.status()
                if (intent.granted) syncNow(userInitiated = true)
            }
            BuildIntent.TowerTapped -> reduce { it.copy(zoomed = !it.zoomed) }
            BuildIntent.TowerLongPressed -> reduce { it.copy(mirrored = !it.mirrored) }
            BuildIntent.SnackbarAction -> reduce { it.copy(snackbar = null, zoomed = true) }
            BuildIntent.SnackbarDismissed -> reduce { it.copy(snackbar = null) }
        }
    }

    private fun content(): BuildContent? = (currentState.content as? Lce.Content)?.value

    private fun syncNow(userInitiated: Boolean) {
        lastSyncRequest = clock.now()
        reduce { it.copy(syncing = true) }
        viewModelScope.launch {
            accessStatus.value = source.status()
            val result = sync()
            reduce { state ->
                val added = (result as? Outcome.Success)?.value?.floorsAddedToday ?: 0
                state.copy(
                    syncing = false,
                    snackbar = if (result is Outcome.Success && (added > 0 || userInitiated)) BuildSnackbar(added, clock.now().toEpochMilli()) else state.snackbar,
                )
            }
        }
    }

    private suspend fun onSnapshot(snapshot: CitySnapshot, status: SyncStatus, access: StepSourceStatus, now: Instant) {
        val tower = snapshot.city.activeTower
        val (dropFrom, key) = BuildReducer.dropAfterUpdate(lastFloors, tower.floorsBuilt, dropKey)
        lastFloors = tower.floorsBuilt
        dropKey = key
        val localTime = clock.localNow().toLocalTime()
        val needsHelp = BuildReducer.needsHelp(status.lastDataChangeAt, now, snapshot.todayProgress.steps, status.lastError)
        val week = snapshot.week
        val content = BuildContent(
            towerIndex = tower.index,
            towerName = tower.name,
            kit = CosmeticCatalog.kitOf(tower.record.look.facadeId),
            look = tower.record.look,
            floorsBuilt = tower.floorsBuilt,
            target = tower.target,
            sky = SkyClock.phaseAt(localTime).dominant,
            morning = SkyClock.isMorningGreeting(localTime),
            today = snapshot.todayProgress,
            dailyTarget = snapshot.settings.goals.dailyTargetSteps,
            week = week,
            weekMessage = BuildReducer.weekMessage(
                week, snapshot.todayProgress,
                snapshot.todayProgress.steps >= snapshot.settings.goals.dailyTargetSteps,
                tower.floorsRemaining, needsHelp,
            ),
            stale = BuildReducer.isStale(status.lastSuccessAt, now),
            lastSyncAt = status.lastSuccessAt,
            now = now,
            access = BuildReducer.access(access),
            dropFrom = dropFrom,
            dropKey = key,
        )
        reduce { it.copy(content = Lce.Content(content), syncing = it.syncing || status.state == SyncState.Syncing) }
        snapshot.city.finishedTowers.firstOrNull { !it.record.ceremonySeen && it.index !in ceremoniesOpened }?.let {
            ceremoniesOpened += it.index
            emit(BuildEffect.OpenCeremony(it.index))
        }
    }

    private companion object {
        const val TICK_MS = 60_000L
        val RESUME_THROTTLE: Duration = Duration.ofSeconds(60)
    }
}
