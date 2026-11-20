package ai.idraak.stepskyscraper.feature.build

import androidx.compose.runtime.Immutable
import ai.idraak.stepskyscraper.core.domain.model.DayProgress
import ai.idraak.stepskyscraper.core.domain.model.DomainError
import ai.idraak.stepskyscraper.core.domain.model.FacadeKit
import ai.idraak.stepskyscraper.core.domain.model.StepSourceStatus
import ai.idraak.stepskyscraper.core.domain.model.TowerLook
import ai.idraak.stepskyscraper.core.domain.model.WeekProgress
import ai.idraak.stepskyscraper.core.domain.usecase.SkyTime
import ai.idraak.stepskyscraper.core.ui.mvi.Lce
import java.time.Duration
import java.time.Instant

enum class StepAccessUi { Connected, NotConnected, HcMissing, HcNeedsUpdate }

/** The week card's second line; the nearest, kindest message wins. */
@Immutable
sealed interface WeekMessage {
    data class ToGo(val floors: Int, val daysLeft: Int) : WeekMessage

    data class ToppingOut(val floors: Int) : WeekMessage

    data object Bonus : WeekMessage

    data object Rest : WeekMessage

    data object Paused : WeekMessage

    data object Reached : WeekMessage

    data object NeedsHelp : WeekMessage
}

@Immutable
data class BuildContent(
    val towerIndex: Int,
    val towerName: String,
    val kit: FacadeKit,
    val look: TowerLook,
    val floorsBuilt: Int,
    val target: Int,
    val sky: SkyTime,
    val morning: Boolean,
    val today: DayProgress,
    val dailyTarget: Int,
    val week: WeekProgress,
    val weekMessage: WeekMessage,
    val stale: Boolean,
    val lastSyncAt: Instant?,
    val now: Instant,
    val access: StepAccessUi,
    val dropFrom: Int?,
    val dropKey: Int,
) {
    val targetMet: Boolean get() = today.steps >= dailyTarget && dailyTarget > 0
    val nextFloorNumber: Int get() = floorsBuilt + 1
}

@Immutable
data class BuildSnackbar(val floorsAdded: Int, val id: Long)

@Immutable
data class BuildUiState(
    val content: Lce<BuildContent> = Lce.Loading,
    val syncing: Boolean = false,
    val snackbar: BuildSnackbar? = null,
    val zoomed: Boolean = false,
    val mirrored: Boolean = false,
)

sealed interface BuildIntent {
    data object Refresh : BuildIntent

    data object Resumed : BuildIntent

    data object OpenCustomise : BuildIntent

    data object OpenShare : BuildIntent

    data object OpenWeek : BuildIntent

    data object ConnectSteps : BuildIntent

    data class AccessResult(val granted: Boolean) : BuildIntent

    data object TowerTapped : BuildIntent

    data object TowerLongPressed : BuildIntent

    data object SnackbarAction : BuildIntent

    data object SnackbarDismissed : BuildIntent
}

sealed interface BuildEffect {
    data class OpenCustomise(val towerIndex: Int) : BuildEffect

    data class OpenShare(val towerIndex: Int) : BuildEffect

    data object OpenWeekDetail : BuildEffect

    data object OpenStepHelp : BuildEffect

    data class OpenCeremony(val towerIndex: Int) : BuildEffect

    data object RequestStepAccess : BuildEffect

    data object InstallHealthConnect : BuildEffect
}

/** Pure functions that turn domain facts into Build UI decisions. */
object BuildReducer {
    private val STALE_AFTER: Duration = Duration.ofMinutes(10)
    private val HELP_AFTER: Duration = Duration.ofHours(48)

    fun access(status: StepSourceStatus): StepAccessUi = when (status) {
        StepSourceStatus.HealthConnectReady -> StepAccessUi.Connected
        is StepSourceStatus.Sensor -> if (status.permissionGranted) StepAccessUi.Connected else StepAccessUi.NotConnected
        StepSourceStatus.HealthConnectNotInstalled -> StepAccessUi.HcMissing
        StepSourceStatus.HealthConnectNeedsUpdate -> StepAccessUi.HcNeedsUpdate
        StepSourceStatus.HealthConnectPermissionMissing, StepSourceStatus.Unavailable -> StepAccessUi.NotConnected
    }

    fun isStale(lastSuccess: Instant?, now: Instant): Boolean = lastSuccess == null || Duration.between(lastSuccess, now) > STALE_AFTER

    /** "0 steps for 2 days": no new data for 48h and nothing today. */
    fun needsHelp(lastDataChange: Instant?, now: Instant, todaySteps: Int, lastError: DomainError?): Boolean =
        todaySteps == 0 && (lastError == DomainError.StepPermissionDenied || (lastDataChange != null && Duration.between(lastDataChange, now) > HELP_AFTER))

    @Suppress("LongParameterList")
    fun weekMessage(
        week: WeekProgress,
        today: DayProgress,
        targetMet: Boolean,
        floorsToToppingOut: Int,
        needsHelp: Boolean,
    ): WeekMessage = when {
        week.paused -> WeekMessage.Paused
        needsHelp -> WeekMessage.NeedsHelp
        targetMet -> WeekMessage.Bonus
        today.steps == 0 -> WeekMessage.Rest
        week.goalReached -> WeekMessage.Reached
        floorsToToppingOut in 1 until week.floorsToGo -> WeekMessage.ToppingOut(floorsToToppingOut)
        else -> WeekMessage.ToGo(week.floorsToGo, week.daysLeftIncludingToday)
    }

    /** New floors drop in only when the count grows after the first load. */
    fun dropAfterUpdate(previousFloors: Int?, newFloors: Int, previousKey: Int): Pair<Int?, Int> =
        if (previousFloors != null && newFloors > previousFloors) previousFloors to previousKey + 1 else null to previousKey
}
