package ai.idraak.stepskyscraper.core.datastore

import ai.idraak.stepskyscraper.core.domain.model.Appearance
import ai.idraak.stepskyscraper.core.domain.model.DomainError
import ai.idraak.stepskyscraper.core.domain.model.Entitlement
import ai.idraak.stepskyscraper.core.domain.model.GoalPause
import ai.idraak.stepskyscraper.core.domain.model.Goals
import ai.idraak.stepskyscraper.core.domain.model.Outcome
import ai.idraak.stepskyscraper.core.domain.model.PassPlan
import ai.idraak.stepskyscraper.core.domain.model.PlanOffer
import ai.idraak.stepskyscraper.core.domain.model.SyncState
import ai.idraak.stepskyscraper.core.domain.model.SyncStatus
import ai.idraak.stepskyscraper.core.domain.model.TargetPreset
import ai.idraak.stepskyscraper.core.domain.model.UserSettings
import ai.idraak.stepskyscraper.core.domain.repository.SettingsRepository
import ai.idraak.stepskyscraper.core.domain.repository.SyncStatusRepository
import ai.idraak.stepskyscraper.core.domain.usecase.StepCounterState
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** File name included in Auto Backup (see backup rules in :app). */
const val USER_PREFS_FILE = "user_prefs"

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun preferences(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(USER_PREFS_FILE) }
}

private object Keys {
    val onboarding = booleanPreferencesKey("onboarding_complete")
    val startDate = stringPreferencesKey("build_start_date")
    val target = intPreferencesKey("daily_target")
    val preset = stringPreferencesKey("target_preset")
    val pauseKind = stringPreferencesKey("pause_kind")
    val pauseDate = stringPreferencesKey("pause_date")
    val appearance = stringPreferencesKey("appearance")
    val notifyTop = booleanPreferencesKey("notify_topping_out")
    val notifyWeek = booleanPreferencesKey("notify_weekly")
    val history = booleanPreferencesKey("history_imported")
    val paywallShown = booleanPreferencesKey("paywall_after_first")
    val widgetPrompt = booleanPreferencesKey("widget_prompt_handled")
    val connectSkipped = booleanPreferencesKey("steps_connect_skipped")
    val shareStats = booleanPreferencesKey("share_show_stats")
    val sound = booleanPreferencesKey("ceremony_sound")
    val notifyPrompt = booleanPreferencesKey("notify_prompt_handled")

    val syncing = booleanPreferencesKey("sync_running")
    val lastSuccess = longPreferencesKey("sync_last_success")
    val lastAttempt = longPreferencesKey("sync_last_attempt")
    val lastChange = longPreferencesKey("sync_last_change")
    val lastError = stringPreferencesKey("sync_last_error")

    val sensorCounter = longPreferencesKey("sensor_last_counter")
    val sensorDate = stringPreferencesKey("sensor_date")
    val sensorToday = intPreferencesKey("sensor_today")

    val premium = booleanPreferencesKey("entitlement_premium")
    val plan = stringPreferencesKey("entitlement_plan")
    val verified = longPreferencesKey("entitlement_verified")
    val offers = stringPreferencesKey("pass_offers_cache")
}

private fun DataStore<Preferences>.safeData(): Flow<Preferences> =
    data.catch { e -> if (e is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw e }

private suspend fun DataStore<Preferences>.write(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit): Outcome<Unit> =
    try {
        edit { block(it) }
        Outcome.Success(Unit)
    } catch (e: IOException) {
        Outcome.Failure(DomainError.Storage(e.message ?: "preferences"))
    }

@Singleton
class PreferencesSettingsRepository @Inject constructor(
    private val store: DataStore<Preferences>,
) : SettingsRepository {
    override val settings: Flow<UserSettings> = store.safeData().map { p ->
        val d = UserSettings.Default
        UserSettings(
            onboardingComplete = p[Keys.onboarding] ?: false,
            buildStartDate = p[Keys.startDate]?.let(LocalDate::parse),
            goals = Goals(
                dailyTargetSteps = p[Keys.target] ?: d.goals.dailyTargetSteps,
                preset = p[Keys.preset]?.let { runCatching { TargetPreset.valueOf(it) }.getOrNull() } ?: d.goals.preset,
                pause = readPause(p),
            ),
            appearance = p[Keys.appearance]?.let { runCatching { Appearance.valueOf(it) }.getOrNull() } ?: d.appearance,
            notifyToppingOut = p[Keys.notifyTop] ?: d.notifyToppingOut,
            notifyWeeklySummary = p[Keys.notifyWeek] ?: d.notifyWeeklySummary,
            historyImported = p[Keys.history] ?: false,
            paywallShownAfterFirstTower = p[Keys.paywallShown] ?: false,
            widgetPromptHandled = p[Keys.widgetPrompt] ?: false,
            stepsConnectSkipped = p[Keys.connectSkipped] ?: false,
            shareShowStats = p[Keys.shareStats] ?: true,
            ceremonySound = p[Keys.sound] ?: true,
            notificationPromptHandled = p[Keys.notifyPrompt] ?: false,
        )
    }

    private fun readPause(p: Preferences): GoalPause {
        val date = p[Keys.pauseDate]?.let(LocalDate::parse) ?: return GoalPause.None
        return when (p[Keys.pauseKind]) {
            "until" -> GoalPause.Until(date)
            "indefinite" -> GoalPause.Indefinite(date)
            else -> GoalPause.None
        }
    }

    override suspend fun completeOnboarding(startDate: LocalDate) = store.write {
        it[Keys.onboarding] = true
        if (it[Keys.startDate] == null) it[Keys.startDate] = startDate.toString()
    }

    override suspend fun setDailyTarget(steps: Int, preset: TargetPreset) = store.write {
        it[Keys.target] = steps
        it[Keys.preset] = preset.name
    }

    override suspend fun setPause(pause: GoalPause) = store.write {
        when (pause) {
            GoalPause.None -> {
                it.remove(Keys.pauseKind)
                it.remove(Keys.pauseDate)
            }
            is GoalPause.Until -> {
                it[Keys.pauseKind] = "until"
                it[Keys.pauseDate] = pause.lastPausedDay.toString()
            }
            is GoalPause.Indefinite -> {
                it[Keys.pauseKind] = "indefinite"
                it[Keys.pauseDate] = pause.since.toString()
            }
        }
    }

    override suspend fun setAppearance(appearance: Appearance) = store.write { it[Keys.appearance] = appearance.name }

    override suspend fun setNotifications(toppingOut: Boolean, weeklySummary: Boolean) = store.write {
        it[Keys.notifyTop] = toppingOut
        it[Keys.notifyWeek] = weeklySummary
    }

    override suspend fun setHistoryImported() = store.write { it[Keys.history] = true }

    override suspend fun setPaywallShownAfterFirstTower() = store.write { it[Keys.paywallShown] = true }

    override suspend fun setWidgetPromptHandled() = store.write { it[Keys.widgetPrompt] = true }

    override suspend fun setStepsConnectSkipped(skipped: Boolean) = store.write { it[Keys.connectSkipped] = skipped }

    override suspend fun setShareShowStats(show: Boolean) = store.write { it[Keys.shareStats] = show }

    override suspend fun setCeremonySound(enabled: Boolean) = store.write { it[Keys.sound] = enabled }

    override suspend fun setNotificationPromptHandled() = store.write { it[Keys.notifyPrompt] = true }
}

@Singleton
class PreferencesSyncStatusRepository @Inject constructor(
    private val store: DataStore<Preferences>,
) : SyncStatusRepository {
    /** In-memory "running" flag: a crashed process must never leave a stale "Syncing…" chip. */
    private val running = MutableStateFlow(false)

    override val status: Flow<SyncStatus> = combine(store.safeData(), running) { p, isRunning ->
        SyncStatus(
            state = if (isRunning) SyncState.Syncing else SyncState.Idle,
            lastSuccessAt = p[Keys.lastSuccess]?.let(Instant::ofEpochMilli),
            lastAttemptAt = p[Keys.lastAttempt]?.let(Instant::ofEpochMilli),
            lastError = p[Keys.lastError]?.let(::errorFromKey),
            lastDataChangeAt = p[Keys.lastChange]?.let(Instant::ofEpochMilli),
        )
    }

    override suspend fun markStarted(at: Instant) {
        running.value = true
        store.write { it[Keys.lastAttempt] = at.toEpochMilli() }
    }

    override suspend fun markSucceeded(at: Instant, dataChanged: Boolean) {
        store.write {
            it[Keys.lastSuccess] = at.toEpochMilli()
            it.remove(Keys.lastError)
            if (dataChanged || it[Keys.lastChange] == null) it[Keys.lastChange] = at.toEpochMilli()
            it.remove(Keys.syncing)
        }
        running.value = false
    }

    override suspend fun markFailed(at: Instant, error: DomainError) {
        store.write { it[Keys.lastError] = errorKey(error) }
        running.value = false
    }

    private fun errorKey(error: DomainError): String = when (error) {
        DomainError.HealthConnectNotInstalled -> "hc_missing"
        DomainError.HealthConnectNeedsUpdate -> "hc_update"
        DomainError.StepPermissionDenied -> "permission"
        DomainError.NoStepSource -> "no_source"
        else -> "failed"
    }

    private fun errorFromKey(key: String): DomainError = when (key) {
        "hc_missing" -> DomainError.HealthConnectNotInstalled
        "hc_update" -> DomainError.HealthConnectNeedsUpdate
        "permission" -> DomainError.StepPermissionDenied
        "no_source" -> DomainError.NoStepSource
        else -> DomainError.StepSourceFailed("Last sync did not complete")
    }
}

/** Baseline for the TYPE_STEP_COUNTER fallback. */
@Singleton
class SensorStateStore @Inject constructor(private val store: DataStore<Preferences>) {
    suspend fun read(): StepCounterState? {
        val p = store.safeData().first()
        val date = p[Keys.sensorDate]?.let(LocalDate::parse) ?: return null
        return StepCounterState(p[Keys.sensorCounter], date, p[Keys.sensorToday] ?: 0)
    }

    suspend fun write(state: StepCounterState): Outcome<Unit> = store.write {
        val counter = state.lastCounter
        if (counter != null) it[Keys.sensorCounter] = counter else it.remove(Keys.sensorCounter)
        it[Keys.sensorDate] = state.date.toString()
        it[Keys.sensorToday] = state.todaySteps
    }
}

/** Offline cache of the Architect Pass entitlement (re-verified with Play on every billing refresh). */
@Singleton
class EntitlementCacheStore @Inject constructor(private val store: DataStore<Preferences>) {
    val cached: Flow<Entitlement> = store.safeData().map { p ->
        Entitlement(
            isPremium = p[Keys.premium] ?: false,
            plan = p[Keys.plan]?.let { runCatching { PassPlan.valueOf(it) }.getOrNull() },
            verifiedAt = p[Keys.verified]?.let(Instant::ofEpochMilli),
        )
    }

    suspend fun save(entitlement: Entitlement): Outcome<Unit> = store.write {
        it[Keys.premium] = entitlement.isPremium
        entitlement.plan?.let { plan -> it[Keys.plan] = plan.name } ?: it.remove(Keys.plan)
        entitlement.verifiedAt?.let { v -> it[Keys.verified] = v.toEpochMilli() }
    }

    /** Last plans and store prices, shown instantly on the next paywall while fresh ones load. */
    val cachedOffers: Flow<List<PlanOffer>> = store.safeData().map { p -> decodeOffers(p[Keys.offers]) }

    suspend fun saveOffers(offers: List<PlanOffer>): Outcome<Unit> = store.write { it[Keys.offers] = encodeOffers(offers) }

    companion object {
        private const val FIELD = ''
        private const val RECORD = ''

        fun encodeOffers(offers: List<PlanOffer>): String = offers.joinToString(RECORD.toString()) { o ->
            listOf(o.plan.name, o.formattedPrice, o.priceMicros, o.currencyCode, o.freeTrialDays).joinToString(FIELD.toString())
        }

        /** Unknown or malformed records are dropped, never thrown. */
        fun decodeOffers(raw: String?): List<PlanOffer> = raw.orEmpty().split(RECORD).mapNotNull { record ->
            val f = record.split(FIELD)
            if (f.size != 5) return@mapNotNull null
            runCatching { PlanOffer(PassPlan.valueOf(f[0]), f[1], f[2].toLong(), f[3], f[4].toInt()) }.getOrNull()
        }
    }
}
