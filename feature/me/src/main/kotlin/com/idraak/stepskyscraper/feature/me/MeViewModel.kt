package com.idraak.stepskyscraper.feature.me

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.idraak.stepskyscraper.core.domain.AppClock
import com.idraak.stepskyscraper.core.domain.achievements.AchievementProgress
import com.idraak.stepskyscraper.core.domain.model.AnalyticsEvent
import com.idraak.stepskyscraper.core.domain.model.ArchitectLevel
import com.idraak.stepskyscraper.core.domain.model.DomainError
import com.idraak.stepskyscraper.core.domain.model.GoalPause
import com.idraak.stepskyscraper.core.domain.model.Goals
import com.idraak.stepskyscraper.core.domain.model.PassPlan
import com.idraak.stepskyscraper.core.domain.model.PeriodTotal
import com.idraak.stepskyscraper.core.domain.model.StepSourceStatus
import com.idraak.stepskyscraper.core.domain.model.UserSettings
import com.idraak.stepskyscraper.core.domain.model.WeekDayBar
import com.idraak.stepskyscraper.core.domain.repository.AnalyticsTracker
import com.idraak.stepskyscraper.core.domain.repository.EntitlementRepository
import com.idraak.stepskyscraper.core.domain.repository.StepSource
import com.idraak.stepskyscraper.core.domain.repository.SyncStatusRepository
import com.idraak.stepskyscraper.core.domain.usecase.CalculateWeekProgressUseCase
import com.idraak.stepskyscraper.core.domain.usecase.GetTowerProgressUseCase
import com.idraak.stepskyscraper.core.domain.usecase.PauseGoalsUseCase
import com.idraak.stepskyscraper.core.domain.usecase.PauseOption
import com.idraak.stepskyscraper.core.ui.mvi.Lce
import com.idraak.stepskyscraper.core.ui.mvi.MviViewModel
import com.idraak.stepskyscraper.core.ui.mvi.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object MeRoute

enum class SourceKind { HealthConnect, Sensor, Off }

@Immutable
data class MeContent(
    val stepsToNext: Int,
    val segmentsFilled: Int,
    val level: ArchitectLevel,
    val buildDays: Int,
    val floors: Int,
    val towers: Int,
    val week: List<WeekDayBar>,
    val weekFloors: Int,
    val weekGoal: Int,
    val month: List<WeekDayBar>,
    val lastWeeks: List<PeriodTotal>,
    val achievements: List<AchievementProgress>,
    val premium: Boolean,
    val monthlyPrice: String,
    val goals: Goals,
    val settings: UserSettings,
    val source: SourceKind,
    val lastSync: Instant?,
    val now: Instant,
)

@Immutable
data class MeUiState(
    val content: Lce<MeContent> = Lce.Loading,
    val showMonth: Boolean = false,
    val pauseSheet: Boolean = false,
)

sealed interface MeIntent {
    data class ShowMonth(val month: Boolean) : MeIntent

    data object TogglePause : MeIntent

    data object DismissPause : MeIntent

    data class Pause(val option: PauseOption) : MeIntent

    data object Resume : MeIntent
}

object MeReducer {
    fun source(status: StepSourceStatus): SourceKind = when (status) {
        StepSourceStatus.HealthConnectReady -> SourceKind.HealthConnect
        is StepSourceStatus.Sensor -> if (status.permissionGranted) SourceKind.Sensor else SourceKind.Off
        else -> SourceKind.Off
    }

    /** Earned first (most recent), then nearest to completion — the grid on Me shows six. */
    fun highlight(achievements: List<AchievementProgress>): List<AchievementProgress> =
        achievements.filter { it.isEarned }.sortedByDescending { it.earnedOn } +
            achievements.filter { !it.isEarned }.sortedByDescending { it.fraction }
}

@HiltViewModel
class MeViewModel @Inject constructor(
    progress: GetTowerProgressUseCase,
    syncStatus: SyncStatusRepository,
    entitlements: EntitlementRepository,
    private val source: StepSource,
    private val week: CalculateWeekProgressUseCase,
    private val pause: PauseGoalsUseCase,
    private val analytics: AnalyticsTracker,
    private val clock: AppClock,
) : MviViewModel<MeUiState, MeIntent, Nothing>(MeUiState()) {
    private val status = MutableStateFlow<StepSourceStatus>(StepSourceStatus.HealthConnectReady)

    init {
        viewModelScope.launch { status.value = source.status() }
        viewModelScope.launch {
            combine(progress(), syncStatus.status, entitlements.offers, status) { s, sync, offers, st ->
                val start = s.settings.buildStartDate
                MeContent(
                    stepsToNext = s.todayProgress.stepsToNextFloor,
                    segmentsFilled = s.todayProgress.segmentsFilled,
                    level = s.level,
                    buildDays = s.city.buildDays,
                    floors = s.city.totalFloors,
                    towers = s.city.finishedTowers.size,
                    week = s.week.days,
                    weekFloors = s.week.floors,
                    weekGoal = s.week.goalFloors,
                    month = week.monthDays(s.days, s.today, start),
                    lastWeeks = week.recentWeeks(s.days, s.settings.goals, s.today, start, 4),
                    achievements = MeReducer.highlight(s.achievements),
                    premium = s.entitlement.isPremium,
                    monthlyPrice = offers.firstOrNull { it.plan == PassPlan.Monthly }?.formattedPrice ?: DEFAULT_MONTHLY,
                    goals = s.settings.goals,
                    settings = s.settings,
                    source = MeReducer.source(st),
                    lastSync = sync.lastSuccessAt,
                    now = clock.now(),
                )
            }
                .catch { e -> reduce { it.copy(content = Lce.Error(DomainError.Storage(e.message ?: "").toUiText())) } }
                .collect { content -> reduce { it.copy(content = Lce.Content(content)) } }
        }
    }

    override fun onIntent(intent: MeIntent) {
        when (intent) {
            is MeIntent.ShowMonth -> reduce { it.copy(showMonth = intent.month) }
            MeIntent.TogglePause -> {
                val paused = (currentState.content as? Lce.Content)?.value?.goals?.pause != GoalPause.None
                if (paused) onIntent(MeIntent.Resume) else reduce { it.copy(pauseSheet = true) }
            }
            MeIntent.DismissPause -> reduce { it.copy(pauseSheet = false) }
            is MeIntent.Pause -> viewModelScope.launch {
                pause.pause(intent.option)
                analytics.track(AnalyticsEvent.GoalsPaused)
                reduce { it.copy(pauseSheet = false) }
            }
            MeIntent.Resume -> viewModelScope.launch {
                pause.resume()
                reduce { it.copy(pauseSheet = false) }
            }
        }
    }

    fun refreshSource() = viewModelScope.launch { status.value = source.status() }

    private companion object {
        const val DEFAULT_MONTHLY = "$2.99"
    }
}
