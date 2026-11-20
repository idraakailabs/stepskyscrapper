package com.idraak.stepskyscraper.core.data.di

import com.idraak.stepskyscraper.core.data.notify.ProgressNotifier
import com.idraak.stepskyscraper.core.data.repository.RemoteConfigLadderRepository
import com.idraak.stepskyscraper.core.data.repository.RoomAchievementRepository
import com.idraak.stepskyscraper.core.data.repository.RoomStepsRepository
import com.idraak.stepskyscraper.core.data.repository.RoomTowerRepository
import com.idraak.stepskyscraper.core.data.repository.RoomUnlockRepository
import com.idraak.stepskyscraper.core.data.repository.RoomWalletRepository
import com.idraak.stepskyscraper.core.data.sync.WorkManagerSyncScheduler
import com.idraak.stepskyscraper.core.datastore.PreferencesSettingsRepository
import com.idraak.stepskyscraper.core.datastore.PreferencesSyncStatusRepository
import com.idraak.stepskyscraper.core.domain.AppClock
import com.idraak.stepskyscraper.core.domain.repository.AchievementRepository
import com.idraak.stepskyscraper.core.domain.repository.LadderRepository
import com.idraak.stepskyscraper.core.domain.repository.SettingsRepository
import com.idraak.stepskyscraper.core.domain.repository.StepsRepository
import com.idraak.stepskyscraper.core.domain.repository.SyncListener
import com.idraak.stepskyscraper.core.domain.repository.SyncScheduler
import com.idraak.stepskyscraper.core.domain.repository.SyncStatusRepository
import com.idraak.stepskyscraper.core.domain.repository.TowerRepository
import com.idraak.stepskyscraper.core.domain.repository.UnlockRepository
import com.idraak.stepskyscraper.core.domain.repository.WalletRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemAppClock @Inject constructor() : AppClock {
    override fun now(): Instant = Instant.now()

    override fun zone(): ZoneId = ZoneId.systemDefault()
}

/** Binds every domain interface to its data-layer implementation (features only ever see the interfaces). */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds abstract fun clock(impl: SystemAppClock): AppClock

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
