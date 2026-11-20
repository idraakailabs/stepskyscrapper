package ai.idraak.stepskyscraper.core.data.sync

import ai.idraak.stepskyscraper.core.domain.model.DomainError
import ai.idraak.stepskyscraper.core.domain.model.Outcome
import ai.idraak.stepskyscraper.core.domain.repository.LadderRepository
import ai.idraak.stepskyscraper.core.domain.repository.SyncScheduler
import ai.idraak.stepskyscraper.core.domain.usecase.SyncStepsUseCase
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Background sync. No foreground service and no persistent notification: WorkManager decides when to run
 * (every 15–30 minutes, later under power saving, which the UI shows as "Updated X min ago").
 */
@HiltWorker
class StepSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sync: SyncStepsUseCase,
    private val ladder: LadderRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        ladder.refresh()
        return when (val outcome = sync()) {
            is Outcome.Success -> Result.success()
            is Outcome.Failure -> when (outcome.error) {
                // Nothing to retry until the user acts (install HC, grant permission).
                DomainError.StepPermissionDenied, DomainError.HealthConnectNotInstalled,
                DomainError.HealthConnectNeedsUpdate, DomainError.NoStepSource,
                -> Result.success()
                else -> if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.success()
            }
        }
    }

    private companion object {
        const val MAX_RETRIES = 3
    }
}

@Singleton
class WorkManagerSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : SyncScheduler {
    override fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<StepSyncWorker>(PERIOD_MIN, TimeUnit.MINUTES, FLEX_MIN, TimeUnit.MINUTES)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_S, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    override fun requestImmediate() {
        val request = OneTimeWorkRequestBuilder<StepSyncWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(IMMEDIATE, ExistingWorkPolicy.KEEP, request)
    }

    private companion object {
        const val PERIODIC = "step-sync-periodic"
        const val IMMEDIATE = "step-sync-now"
        const val PERIOD_MIN = 30L
        const val FLEX_MIN = 15L
        const val BACKOFF_S = 60L
    }
}
