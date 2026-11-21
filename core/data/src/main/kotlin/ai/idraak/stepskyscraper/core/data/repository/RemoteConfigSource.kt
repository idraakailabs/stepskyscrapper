package ai.idraak.stepskyscraper.core.data.repository

import ai.idraak.stepskyscraper.core.domain.model.DomainError
import ai.idraak.stepskyscraper.core.domain.model.Outcome
import ai.idraak.stepskyscraper.core.domain.usecase.BlueprintLadder
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/** The Remote Config values the app reads. [DEFAULT] is what applies offline and without Firebase. */
data class RemoteValues(val ladderRaw: String?, val adsEnabled: Boolean) {
    companion object {
        val DEFAULT = RemoteValues(ladderRaw = null, adsEnabled = false)
    }
}

/**
 * The single Firebase Remote Config client. Only app configuration is fetched; nothing about the user is
 * sent. Without `google-services.json` (no FirebaseApp) it never touches Firebase and [values] stays at
 * [RemoteValues.DEFAULT]. After a fetch, success or failure, the last *activated* values are published, so a
 * value fetched in an earlier session still applies offline; a value never fetched falls back to the default.
 */
@Singleton
class RemoteConfigSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val state = MutableStateFlow(RemoteValues.DEFAULT)
    val values: StateFlow<RemoteValues> = state.asStateFlow()
    private val mutex = Mutex()

    private val config: FirebaseRemoteConfig? by lazy {
        if (FirebaseApp.getApps(context).isEmpty()) {
            null
        } else {
            FirebaseRemoteConfig.getInstance().apply {
                setConfigSettingsAsync(remoteConfigSettings { minimumFetchIntervalInSeconds = FETCH_INTERVAL_S })
                setDefaultsAsync(
                    mapOf(
                        KEY_LADDER to BlueprintLadder.DEFAULT.joinToString(","),
                        KEY_ADS_ENABLED to DEFAULT_ADS_ENABLED,
                    ),
                )
            }
        }
    }

    /** Fetches (respecting the 12 h cache) and activates. No-op success when Firebase is not configured. */
    suspend fun refresh(): Outcome<Unit> = mutex.withLock {
        val rc = config ?: return Outcome.Success(Unit)
        val result = try {
            rc.fetchAndActivate().await()
            Outcome.Success(Unit)
        } catch (e: FirebaseException) {
            Log.w(TAG, "Remote Config fetch failed; keeping last activated values", e)
            Outcome.Failure(DomainError.StepSourceFailed("Remote Config unavailable"))
        }
        try {
            rc.ensureInitialized().await()
            state.value = RemoteValues(rc.getString(KEY_LADDER), rc.getBoolean(KEY_ADS_ENABLED))
        } catch (e: FirebaseException) {
            Log.w(TAG, "Remote Config not initialised; using defaults", e)
        }
        result
    }

    companion object {
        const val KEY_LADDER = "tower_ladder"
        const val KEY_ADS_ENABLED = "ads_enabled"
        const val DEFAULT_ADS_ENABLED = false
        private const val TAG = "RemoteConfig"
        private const val FETCH_INTERVAL_S = 12L * 60 * 60
    }
}
