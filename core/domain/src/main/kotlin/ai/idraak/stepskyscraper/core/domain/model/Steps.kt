package ai.idraak.stepskyscraper.core.domain.model

import java.time.Instant
import java.time.LocalDate

/** Total steps for one local calendar day. Stored as a total so re-syncing the same day is idempotent. */
data class DaySteps(
    val date: LocalDate,
    val steps: Int,
    val source: StepSourceKind,
)

enum class StepSourceKind { HealthConnect, Sensor, Demo }

/** A floor whose completion was observed during a sync, with the local time it was seen. */
data class FloorEvent(
    val date: LocalDate,
    val floorNumberOfDay: Int,
    val detectedAt: Instant,
)

/** Where steps come from right now, as seen by the step-source layer. */
sealed interface StepSourceStatus {
    data object HealthConnectReady : StepSourceStatus

    data object HealthConnectPermissionMissing : StepSourceStatus

    data object HealthConnectNotInstalled : StepSourceStatus

    data object HealthConnectNeedsUpdate : StepSourceStatus

    /** Health Connect unavailable; using TYPE_STEP_COUNTER with ACTIVITY_RECOGNITION. */
    data class Sensor(val permissionGranted: Boolean) : StepSourceStatus

    data object Unavailable : StepSourceStatus
}

val StepSourceStatus.isReady: Boolean
    get() = this == StepSourceStatus.HealthConnectReady || (this is StepSourceStatus.Sensor && permissionGranted)

enum class SyncState { Idle, Syncing }

data class SyncStatus(
    val state: SyncState,
    val lastSuccessAt: Instant?,
    val lastAttemptAt: Instant?,
    val lastError: DomainError?,
    /** Last moment new step data (an increase) arrived; drives the "0 steps for 2 days" help entry. */
    val lastDataChangeAt: Instant?,
)

/** Outcome of one sync pass. */
data class SyncResult(
    val daysWritten: Int,
    val floorsAddedToday: Int,
    val todaySteps: Int,
    /** A day's total went up. Inserting a new day with 0 steps is not new data (drives the 48h step-help link). */
    val stepsArrived: Boolean = false,
)
