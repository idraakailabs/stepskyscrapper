package ai.idraak.stepskyscraper.core.steps

import ai.idraak.stepskyscraper.core.datastore.SensorStateStore
import ai.idraak.stepskyscraper.core.domain.AppClock
import ai.idraak.stepskyscraper.core.domain.model.DaySteps
import ai.idraak.stepskyscraper.core.domain.model.DomainError
import ai.idraak.stepskyscraper.core.domain.model.Outcome
import ai.idraak.stepskyscraper.core.domain.model.StepSourceKind
import ai.idraak.stepskyscraper.core.domain.usecase.StepCounterReducer
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Fallback when Health Connect is unavailable: TYPE_STEP_COUNTER (steps since boot) with
 * ACTIVITY_RECOGNITION. No foreground service: the counter is sampled on each sync (app open, pull to
 * refresh, periodic work) and the delta since the stored baseline is added to today; a lower counter
 * than the baseline means a reboot and is rebased.
 */
@Singleton
class SensorStepSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stateStore: SensorStateStore,
    private val reducer: StepCounterReducer,
    private val clock: AppClock,
) {
    private val sensorManager: SensorManager? get() = context.getSystemService(SensorManager::class.java)

    fun hasSensor(): Boolean = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null

    fun permissionGranted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    suspend fun readToday(): Outcome<List<DaySteps>> {
        if (!hasSensor()) return Outcome.Failure(DomainError.NoStepSource)
        if (!permissionGranted()) return Outcome.Failure(DomainError.StepPermissionDenied)
        val counter = sampleCounter() ?: return Outcome.Failure(DomainError.StepSourceFailed("Step counter did not report"))
        val today: LocalDate = clock.today()
        val next = reducer.reduce(stateStore.read(), counter, today)
        val saved = stateStore.write(next)
        if (saved is Outcome.Failure) return saved
        return Outcome.Success(listOf(DaySteps(today, next.todaySteps, StepSourceKind.Sensor)))
    }

    private suspend fun sampleCounter(): Long? {
        val manager = sensorManager ?: return null
        val sensor = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return null
        return withTimeoutOrNull(SAMPLE_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        manager.unregisterListener(this)
                        if (cont.isActive) cont.resume(event.values.firstOrNull()?.toLong())
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                cont.invokeOnCancellation { manager.unregisterListener(listener) }
            }
        }
    }

    private companion object {
        const val SAMPLE_TIMEOUT_MS = 5_000L
    }
}
