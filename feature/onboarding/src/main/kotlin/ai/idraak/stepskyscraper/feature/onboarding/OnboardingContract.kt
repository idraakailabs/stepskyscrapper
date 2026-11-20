package ai.idraak.stepskyscraper.feature.onboarding

import ai.idraak.stepskyscraper.core.domain.model.FacadeKit
import ai.idraak.stepskyscraper.core.domain.model.FloorMath
import ai.idraak.stepskyscraper.core.domain.model.StepSourceStatus
import ai.idraak.stepskyscraper.core.domain.model.TargetPreset
import ai.idraak.stepskyscraper.core.domain.usecase.SuggestDailyTargetUseCase
import androidx.compose.runtime.Immutable

enum class OnboardingStep { Promise, Permission, Payoff, Style, Target, Widget }

enum class AccessState { Unknown, Ready, NeedsPermission, NotInstalled, NeedsUpdate, Denied }

@Immutable
data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.Promise,
    val access: AccessState = AccessState.Unknown,
    val usesSensor: Boolean = false,
    val payoffLoading: Boolean = false,
    val todaySteps: Int = 0,
    val kit: FacadeKit = FacadeKit.Glass,
    val name: String = "",
    val restyleKey: Int = 0,
    val sevenDayAverage: Int = 0,
    val suggested: TargetPreset = TargetPreset.Steady,
    val selected: TargetPreset = TargetPreset.Steady,
    val customSteps: Int = TargetPreset.Steady.dailySteps,
    val saving: Boolean = false,
    val widgetUnsupported: Boolean = false,
) {
    val floorsToday: Int get() = todaySteps / FloorMath.STEPS_PER_FLOOR
    val stepsToNext: Int get() = FloorMath.STEPS_PER_FLOOR - todaySteps % FloorMath.STEPS_PER_FLOOR
    val segmentsFilled: Int get() = todaySteps % FloorMath.STEPS_PER_FLOOR / FloorMath.STEPS_PER_SEGMENT

    /** Share of the next floor already walked (0 when no steps into it), drawn as the tower's partial floor. */
    val floorProgress: Float get() = todaySteps % FloorMath.STEPS_PER_FLOOR / FloorMath.STEPS_PER_FLOOR.toFloat()
    val dailyTarget: Int get() = if (selected == TargetPreset.Custom) customSteps else selected.dailySteps
    val weeklyFloors: Int get() = dailyTarget / FloorMath.STEPS_PER_FLOOR * 7
    val pageIndex: Int get() = when (step) {
        OnboardingStep.Promise -> 0
        OnboardingStep.Permission -> 1
        OnboardingStep.Payoff -> 2
        else -> 3
    }
}

sealed interface OnboardingIntent {
    data object Start : OnboardingIntent

    data object AllowAccess : OnboardingIntent

    data class AccessResult(val granted: Boolean) : OnboardingIntent

    data object NotNow : OnboardingIntent

    data object RecheckSource : OnboardingIntent

    data object ContinueFromPayoff : OnboardingIntent

    data class SelectKit(val kit: FacadeKit) : OnboardingIntent

    data class NameChanged(val name: String) : OnboardingIntent

    data object RollName : OnboardingIntent

    data object ConfirmStyle : OnboardingIntent

    data object Back : OnboardingIntent

    data class SelectTarget(val preset: TargetPreset) : OnboardingIntent

    data class AdjustCustom(val delta: Int) : OnboardingIntent

    data object ConfirmTarget : OnboardingIntent

    data object AddWidget : OnboardingIntent

    data class WidgetPinResult(val supported: Boolean) : OnboardingIntent

    data object MaybeLater : OnboardingIntent
}

sealed interface OnboardingEffect {
    data object RequestStepAccess : OnboardingEffect

    data object InstallHealthConnect : OnboardingEffect

    data object RequestPinWidget : OnboardingEffect

    data object Finished : OnboardingEffect
}

/** Pure reducers for the onboarding state machine. */
internal object OnboardingReducer {
    fun accessFrom(status: StepSourceStatus): AccessState = when (status) {
        StepSourceStatus.HealthConnectReady -> AccessState.Ready
        StepSourceStatus.HealthConnectPermissionMissing -> AccessState.NeedsPermission
        StepSourceStatus.HealthConnectNotInstalled -> AccessState.NotInstalled
        StepSourceStatus.HealthConnectNeedsUpdate -> AccessState.NeedsUpdate
        is StepSourceStatus.Sensor -> if (status.permissionGranted) AccessState.Ready else AccessState.NeedsPermission
        StepSourceStatus.Unavailable -> AccessState.Denied
    }

    fun selectKit(state: OnboardingUiState, kit: FacadeKit): OnboardingUiState =
        if (kit == state.kit) state else state.copy(kit = kit, restyleKey = state.restyleKey + 1)

    fun adjustCustom(state: OnboardingUiState, delta: Int, normalize: (Int) -> Int): OnboardingUiState =
        state.copy(selected = TargetPreset.Custom, customSteps = normalize(state.customSteps + delta))

    fun back(state: OnboardingUiState): OnboardingUiState = when (state.step) {
        OnboardingStep.Permission -> state.copy(step = OnboardingStep.Promise)
        OnboardingStep.Payoff -> state.copy(step = OnboardingStep.Permission)
        OnboardingStep.Style -> state.copy(
            step = if (state.access ==
                AccessState.Ready
            ) {
                OnboardingStep.Payoff
            } else {
                OnboardingStep.Permission
            },
        )
        OnboardingStep.Target -> state.copy(step = OnboardingStep.Style)
        OnboardingStep.Widget -> state
        OnboardingStep.Promise -> state
    }

    fun withSuggestion(state: OnboardingUiState, average: Int, suggestedSteps: Int, preset: TargetPreset): OnboardingUiState {
        val normalizedPreset = if (preset == TargetPreset.Custom) TargetPreset.Custom else preset
        return state.copy(
            sevenDayAverage = average,
            suggested = normalizedPreset,
            selected = normalizedPreset,
            customSteps = if (normalizedPreset == TargetPreset.Custom) suggestedSteps else state.customSteps,
        )
    }

    const val CUSTOM_STEP = SuggestDailyTargetUseCase.CUSTOM_STEP
}
