package com.idraak.stepskyscraper.feature.onboarding

import androidx.lifecycle.viewModelScope
import com.idraak.stepskyscraper.core.domain.AppClock
import com.idraak.stepskyscraper.core.domain.model.AnalyticsEvent
import com.idraak.stepskyscraper.core.domain.model.Outcome
import com.idraak.stepskyscraper.core.domain.model.StepSourceStatus
import com.idraak.stepskyscraper.core.domain.repository.AnalyticsTracker
import com.idraak.stepskyscraper.core.domain.repository.SettingsRepository
import com.idraak.stepskyscraper.core.domain.repository.StepSource
import com.idraak.stepskyscraper.core.domain.repository.StepsRepository
import com.idraak.stepskyscraper.core.domain.repository.SyncScheduler
import com.idraak.stepskyscraper.core.domain.usecase.GenerateTowerNameUseCase
import com.idraak.stepskyscraper.core.domain.usecase.StartBuildingUseCase
import com.idraak.stepskyscraper.core.domain.usecase.SuggestDailyTargetUseCase
import com.idraak.stepskyscraper.core.domain.usecase.SyncStepsUseCase
import com.idraak.stepskyscraper.core.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val source: StepSource,
    private val sync: SyncStepsUseCase,
    private val steps: StepsRepository,
    private val settings: SettingsRepository,
    private val startBuilding: StartBuildingUseCase,
    private val suggest: SuggestDailyTargetUseCase,
    private val names: GenerateTowerNameUseCase,
    private val scheduler: SyncScheduler,
    private val analytics: AnalyticsTracker,
    private val clock: AppClock,
) : MviViewModel<OnboardingUiState, OnboardingIntent, OnboardingEffect>(OnboardingUiState(name = names(seed = 1L))) {

    init {
        analytics.track(AnalyticsEvent.OnboardingStarted)
        refreshAccess()
    }

    override fun onIntent(intent: OnboardingIntent) {
        when (intent) {
            OnboardingIntent.Start -> reduce { it.copy(step = OnboardingStep.Permission) }
            OnboardingIntent.AllowAccess -> when (currentState.access) {
                AccessState.NotInstalled, AccessState.NeedsUpdate -> emit(OnboardingEffect.InstallHealthConnect)
                AccessState.Ready -> onAccessGranted()
                else -> emit(OnboardingEffect.RequestStepAccess)
            }
            is OnboardingIntent.AccessResult -> {
                analytics.track(AnalyticsEvent.StepAccess(intent.granted))
                if (intent.granted) onAccessGranted() else reduce { it.copy(access = AccessState.Denied) }
            }
            OnboardingIntent.NotNow -> viewModelScope.launch {
                settings.setStepsConnectSkipped(true)
                reduce { it.copy(step = OnboardingStep.Style) }
            }
            OnboardingIntent.RecheckSource -> refreshAccess()
            OnboardingIntent.ContinueFromPayoff -> reduce { it.copy(step = OnboardingStep.Style) }
            is OnboardingIntent.SelectKit -> reduce { OnboardingReducer.selectKit(it, intent.kit) }
            is OnboardingIntent.NameChanged -> reduce { it.copy(name = intent.name) }
            OnboardingIntent.RollName -> reduce { it.copy(name = names(avoid = it.name)) }
            OnboardingIntent.ConfirmStyle -> {
                analytics.track(AnalyticsEvent.FacadeChosen(currentState.kit))
                loadSuggestion()
                reduce { it.copy(step = OnboardingStep.Target) }
            }
            OnboardingIntent.Back -> reduce(OnboardingReducer::back)
            is OnboardingIntent.SelectTarget -> reduce { it.copy(selected = intent.preset) }
            is OnboardingIntent.AdjustCustom -> reduce { OnboardingReducer.adjustCustom(it, intent.delta, suggest::normalizeCustom) }
            OnboardingIntent.ConfirmTarget -> confirmTarget()
            OnboardingIntent.AddWidget -> {
                analytics.track(AnalyticsEvent.WidgetPinRequested)
                emit(OnboardingEffect.RequestPinWidget)
            }
            is OnboardingIntent.WidgetPinResult -> if (intent.supported) finish() else reduce { it.copy(widgetUnsupported = true) }
            OnboardingIntent.MaybeLater -> finish()
        }
    }

    private fun refreshAccess() = viewModelScope.launch {
        val status = source.status()
        reduce { it.copy(access = OnboardingReducer.accessFrom(status), usesSensor = status is StepSourceStatus.Sensor) }
    }

    private fun onAccessGranted() {
        reduce { it.copy(access = AccessState.Ready, step = OnboardingStep.Payoff, payoffLoading = true) }
        viewModelScope.launch {
            settings.setStepsConnectSkipped(false)
            sync()
            val today = clock.today()
            val todaySteps = (steps.days(today, today) as? Outcome.Success)?.value?.firstOrNull()?.steps ?: 0
            reduce { it.copy(todaySteps = todaySteps, payoffLoading = false) }
        }
    }

    private fun loadSuggestion() = viewModelScope.launch {
        val today = clock.today()
        val history = (steps.days(today.minusDays(7), today.minusDays(1)) as? Outcome.Success)?.value.orEmpty()
        val suggestion = suggest(history, today)
        reduce {
            OnboardingReducer.withSuggestion(
                it,
                suggestion.sevenDayAverage,
                suggestion.suggestedDailySteps,
                suggestion.suggestedPreset,
            )
        }
    }

    private fun confirmTarget() {
        if (currentState.saving) return
        reduce { it.copy(saving = true) }
        viewModelScope.launch {
            val s = currentState
            val result = startBuilding(s.name, s.kit, s.dailyTarget, s.selected)
            analytics.track(AnalyticsEvent.TargetChosen(s.selected))
            reduce { it.copy(saving = false, step = if (result is Outcome.Success) OnboardingStep.Widget else it.step) }
            if (result is Outcome.Success) {
                scheduler.schedulePeriodic()
                analytics.track(AnalyticsEvent.OnboardingCompleted)
            }
        }
    }

    private fun finish() = viewModelScope.launch {
        settings.setWidgetPromptHandled()
        emit(OnboardingEffect.Finished)
    }
}
