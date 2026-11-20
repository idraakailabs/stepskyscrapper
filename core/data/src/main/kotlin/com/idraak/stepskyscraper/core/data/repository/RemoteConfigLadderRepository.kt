package com.idraak.stepskyscraper.core.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.idraak.stepskyscraper.core.domain.model.DomainError
import com.idraak.stepskyscraper.core.domain.model.Outcome
import com.idraak.stepskyscraper.core.domain.repository.LadderRepository
import com.idraak.stepskyscraper.core.domain.usecase.BlueprintLadder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Blueprint ladder from Firebase Remote Config key `tower_ladder` ("10,25,50,100,160,250").
 * The bundled default is used offline, on malformed values, and when Firebase is not configured.
 * Only the ladder is fetched; nothing about the user is sent.
 */
@Singleton
class RemoteConfigLadderRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : LadderRepository {
    private val state = MutableStateFlow(BlueprintLadder.DEFAULT)
    override val ladder: Flow<List<Int>> = state.asStateFlow()

    private val config: FirebaseRemoteConfig? by lazy {
        if (FirebaseApp.getApps(context).isEmpty()) {
            null
        } else {
            FirebaseRemoteConfig.getInstance().apply {
                setConfigSettingsAsync(remoteConfigSettings { minimumFetchIntervalInSeconds = FETCH_INTERVAL_S })
                setDefaultsAsync(mapOf(KEY to BlueprintLadder.DEFAULT.joinToString(",")))
            }
        }
    }

    override suspend fun refresh(): Outcome<Unit> {
        val rc = config ?: return Outcome.Success(Unit)
        return try {
            rc.fetchAndActivate().await()
            state.value = parse(rc.getString(KEY))
            Outcome.Success(Unit)
        } catch (e: FirebaseException) {
            Log.w(TAG, "Remote Config fetch failed; keeping ladder", e)
            Outcome.Failure(DomainError.StepSourceFailed("Remote Config unavailable"))
        }
    }

    companion object {
        const val KEY = "tower_ladder"
        private const val TAG = "LadderConfig"
        private const val FETCH_INTERVAL_S = 12L * 60 * 60

        fun parse(raw: String?): List<Int> =
            BlueprintLadder.sanitize(raw?.split(',')?.mapNotNull { it.trim().toIntOrNull() })
    }
}
