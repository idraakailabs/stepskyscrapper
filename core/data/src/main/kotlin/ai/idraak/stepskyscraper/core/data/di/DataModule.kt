package ai.idraak.stepskyscraper.core.data.di

import ai.idraak.stepskyscraper.core.data.notify.ProgressNotifier
import ai.idraak.stepskyscraper.core.data.repository.RemoteConfigLadderRepository
import ai.idraak.stepskyscraper.core.data.repository.RoomAchievementRepository
import ai.idraak.stepskyscraper.core.data.repository.RoomStepsRepository
import ai.idraak.stepskyscraper.core.data.repository.RoomTowerRepository
import ai.idraak.stepskyscraper.core.data.repository.RoomUnlockRepository
import ai.idraak.stepskyscraper.core.data.repository.RoomWalletRepository
import ai.idraak.stepskyscraper.core.data.sync.WorkManagerSyncScheduler
import ai.idraak.stepskyscraper.core.datastore.PreferencesSettingsRepository
import ai.idraak.stepskyscraper.core.datastore.PreferencesSyncStatusRepository
import ai.idraak.stepskyscraper.core.domain.AppClock
import ai.idraak.stepskyscraper.core.domain.repository.AchievementRepository
import ai.idraak.stepskyscraper.core.domain.repository.LadderRepository
import ai.idraak.stepskyscraper.core.domain.repository.SettingsRepository
import ai.idraak.stepskyscraper.core.domain.repository.StepsRepository
import ai.idraak.stepskyscraper.core.domain.repository.SyncListener
import ai.idraak.stepskyscraper.core.domain.repository.SyncScheduler
import ai.idraak.stepskyscraper.core.domain.repository.SyncStatusRepository
import ai.idraak.stepskyscraper.core.domain.repository.TowerRepository
import ai.idraak.stepskyscraper.core.domain.repository.UnlockRepository
import ai.idraak.stepskyscraper.core.domain.repository.WalletRepository
import dagger.Binds
import dagger.BindsOptionalOf
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Optional
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug-only clock shift, used to check the dawn / dusk / night skies on a device without changing its
 * system time. Bound only in `app/src/debug`; release builds bind nothing, so the clock is the system clock.
 */
fun interface DebugClockShift {
    fun shift(): Duration
}

@Singleton
class SystemAppClock @Inject constructor(private val debugShift: Optional<DebugClockShift>) : AppClock {
    override fun now(): Instant {
        val now = Instant.now()
        return if (debugShift.isPresent) now.plus(debugShift.get().shift()) else now
    }

    override fun zone(): ZoneId = ZoneId.systemDefault()
}

/** Binds every domain interface to its data-layer implementation (features only ever see the interfaces). */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds abstract fun clock(impl: SystemAppClock): AppClock

    @BindsOptionalOf abstract fun debugClockShift(): DebugClockShift

    @Binds abstract fun steps(impl: RoomStepsRepository): StepsRepository

    @Binds abstract fun towers(impl: RoomTowerRepository): TowerRepository

    @Binds abstract fun wallet(impl: RoomWalletRepository): WalletRepository

    @Binds abstract fun unlocks(impl: RoomUnlockRepository): UnlockRepository

    @Binds abstract fun achievements(impl: RoomAchievementRepository): AchievementRepository

    @Binds abstract fun ladder(impl: RemoteConfigLadderRepository): LadderRepository

    @Binds abstract fun settings(impl: PreferencesSettingsRepository): SettingsRepository

    @Binds abstract fun syncStatus(impl: PreferencesSyncStatusRepository): SyncStatusRepository

    @Binds abstract fun scheduler(impl: WorkManagerSyncScheduler): SyncScheduler

    @Multibinds abstract fun syncListeners(): Set<SyncListener>

    @Binds @IntoSet
    abstract fun notifier(impl: ProgressNotifier): SyncListener
}
