package com.idraak.stepskyscraper.core.steps

import android.content.Context
import android.os.RemoteException
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.idraak.stepskyscraper.core.domain.model.DaySteps
import com.idraak.stepskyscraper.core.domain.model.DomainError
import com.idraak.stepskyscraper.core.domain.model.Outcome
import com.idraak.stepskyscraper.core.domain.model.StepSourceKind
import com.idraak.stepskyscraper.core.domain.model.StepSourceStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.time.LocalDate
import java.time.Period
import javax.inject.Inject
import javax.inject.Singleton

/** Permissions requested from Health Connect: Steps read only (+ background read where available). */
object HealthConnectPermissions {
    val READ_STEPS: String = HealthPermission.getReadPermission(StepsRecord::class)
    const val READ_IN_BACKGROUND: String = HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
    const val PROVIDER_PACKAGE = "com.google.android.apps.healthdata"
}

/**
 * Primary source. Uses the aggregate API (StepsRecord.COUNT_TOTAL grouped by day) so Health Connect
 * deduplicates overlapping sources and includes on-device steps. Data never leaves the device.
 */
@Singleton
class HealthConnectStepSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun sdkStatus(): Int = HealthConnectClient.getSdkStatus(context, HealthConnectPermissions.PROVIDER_PACKAGE)

    private fun client(): HealthConnectClient? =
        if (sdkStatus() == HealthConnectClient.SDK_AVAILABLE) HealthConnectClient.getOrCreate(context) else null

    suspend fun status(): StepSourceStatus = when (sdkStatus()) {
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> StepSourceStatus.HealthConnectNeedsUpdate
        HealthConnectClient.SDK_AVAILABLE -> {
            val granted = grantedPermissions()
            if (HealthConnectPermissions.READ_STEPS in
                granted
            ) {
                StepSourceStatus.HealthConnectReady
            } else {
                StepSourceStatus.HealthConnectPermissionMissing
            }
        }
        else -> StepSourceStatus.HealthConnectNotInstalled
    }

    suspend fun grantedPermissions(): Set<String> = try {
        client()?.permissionController?.getGrantedPermissions().orEmpty()
    } catch (e: RemoteException) {
        Log.w(TAG, "Health Connect permission query failed", e)
        emptySet()
    } catch (e: SecurityException) {
        Log.w(TAG, "Health Connect permission query refused", e)
        emptySet()
    }

    /** READ_HEALTH_DATA_IN_BACKGROUND is only requested when the Feature Availability API reports it. */
    fun backgroundFeatureAvailable(): Boolean {
        val c = client() ?: return false
        return c.features.getFeatureStatus(HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND) ==
            HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
    }

    suspend fun backgroundReadGranted(): Boolean =
        backgroundFeatureAvailable() && HealthConnectPermissions.READ_IN_BACKGROUND in grantedPermissions()

    /** The permission set to request right now. */
    fun permissionsToRequest(): Set<String> = buildSet {
        add(HealthConnectPermissions.READ_STEPS)
        if (backgroundFeatureAvailable()) add(HealthConnectPermissions.READ_IN_BACKGROUND)
    }

    suspend fun readDailyTotals(from: LocalDate, to: LocalDate): Outcome<List<DaySteps>> {
        val c = client() ?: return Outcome.Failure(DomainError.HealthConnectNotInstalled)
        return try {
            val groups = c.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(from.atStartOfDay(), to.plusDays(1).atStartOfDay()),
                    timeRangeSlicer = Period.ofDays(1),
                ),
            )
            val byDate = groups.associate { g -> g.startTime.toLocalDate() to (g.result[StepsRecord.COUNT_TOTAL] ?: 0L) }
            val days = generateSequence(from) { d -> d.plusDays(1).takeIf { !it.isAfter(to) } }
                .map { date ->
                    DaySteps(
                        date,
                        (byDate[date] ?: 0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                        StepSourceKind.HealthConnect,
                    )
                }
                .toList()
            Outcome.Success(days)
        } catch (e: SecurityException) {
            Log.w(TAG, "Steps permission missing", e)
            Outcome.Failure(DomainError.StepPermissionDenied)
        } catch (e: RemoteException) {
            Outcome.Failure(DomainError.StepSourceFailed(e.message ?: "Health Connect unavailable"))
        } catch (e: IOException) {
            Outcome.Failure(DomainError.StepSourceFailed(e.message ?: "Health Connect I/O"))
        } catch (e: IllegalStateException) {
            // Thrown for background reads without READ_HEALTH_DATA_IN_BACKGROUND or when rate-limited.
            Outcome.Failure(DomainError.StepSourceFailed(e.message ?: "Health Connect refused the read"))
        }
    }

    private companion object {
        const val TAG = "HealthConnectSource"
    }
}
