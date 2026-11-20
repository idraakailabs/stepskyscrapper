package com.idraak.stepskyscraper.core.steps

import androidx.health.connect.client.HealthConnectClient
import com.idraak.stepskyscraper.core.domain.model.DaySteps
import com.idraak.stepskyscraper.core.domain.model.Outcome
import com.idraak.stepskyscraper.core.domain.model.StepSourceStatus
import com.idraak.stepskyscraper.core.domain.repository.StepSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Chooses the source: Health Connect whenever the SDK is available (even before permission is granted,
 * so the app asks for it), otherwise the step-counter sensor, otherwise nothing.
 */
@Singleton
class DeviceStepSource @Inject constructor(
    private val healthConnect: HealthConnectStepSource,
    private val sensor: SensorStepSource,
) : StepSource {
    private fun useSensor(): Boolean =
        healthConnect.sdkStatus() == HealthConnectClient.SDK_UNAVAILABLE && sensor.hasSensor()

    override suspend fun status(): StepSourceStatus = when {
        useSensor() -> StepSourceStatus.Sensor(sensor.permissionGranted())
        healthConnect.sdkStatus() == HealthConnectClient.SDK_UNAVAILABLE -> StepSourceStatus.HealthConnectNotInstalled
        else -> healthConnect.status()
    }

    override suspend fun readDailyTotals(from: LocalDate, to: LocalDate): Outcome<List<DaySteps>> =
        if (useSensor()) sensor.readToday() else healthConnect.readDailyTotals(from, to)

    override suspend fun backgroundReadAvailable(): Boolean = useSensor() || healthConnect.backgroundReadGranted()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class StepSourceModule {
    @Binds
    abstract fun stepSource(impl: DeviceStepSource): StepSource
}
