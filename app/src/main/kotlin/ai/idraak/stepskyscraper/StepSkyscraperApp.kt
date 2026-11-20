package ai.idraak.stepskyscraper

import ai.idraak.stepskyscraper.core.domain.repository.SyncScheduler
import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class StepSkyscraperApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var scheduler: SyncScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // Periodic step sync (15–30 min); a no-op until onboarding grants a step source.
        scheduler.schedulePeriodic()
    }
}
