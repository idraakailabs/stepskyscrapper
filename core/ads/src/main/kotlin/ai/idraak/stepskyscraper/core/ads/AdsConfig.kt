package ai.idraak.stepskyscraper.core.ads

/**
 * Build-time ad facts for the running variant, provided by `:app` from its BuildConfig (see
 * `app/build.gradle.kts`). [isProdRelease] is true only for the `release` variant; every other variant (debug,
 * benchmark, non-minified) is a test build. The live ids are empty outside the release variant.
 */
data class AdsEnvironment(
    val isProdRelease: Boolean,
    val liveAppId: String,
    val liveRewardedUnitId: String,
    /** Debug only: hashed device id from logcat, used for UMP debug geography and AdMob test-device config. */
    val testDeviceHashedId: String = "",
    /** Debug only: force the UMP EEA debug geography so the consent form can be verified outside the EEA. */
    val umpDebugGeographyEea: Boolean = false,
)

/** Google's published sample ids and the format of real ones. */
object AdMobIds {
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_REWARDED_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    private const val SAMPLE_PUBLISHER = "ca-app-pub-3940256099942544"
    private val APP_ID = Regex("""ca-app-pub-\d{16}~\d{10}""")
    private val UNIT_ID = Regex("""ca-app-pub-\d{16}/\d{10}""")

    /** A well-formed id that is not one of Google's sample ids. */
    fun isLiveAppId(id: String?): Boolean = id != null && APP_ID.matches(id) && !id.startsWith(SAMPLE_PUBLISHER)

    fun isLiveRewardedUnitId(id: String?): Boolean = id != null && UNIT_ID.matches(id) && !id.startsWith(SAMPLE_PUBLISHER)
}

/** Which ads the running build may serve. */
sealed interface AdSetup {
    /** Every non-release variant: Google test ids, always on, Remote Config ignored. */
    data object Test : AdSetup {
        val rewardedUnitId: String = AdMobIds.TEST_REWARDED_UNIT_ID
    }

    /** Prod Release with valid live ids: served only while the Remote Config switch is on. */
    data class Live(val rewardedUnitId: String) : AdSetup

    /** Prod Release without valid live ids (local `assembleRelease` smoke test): ads off, no requests. */
    data object NotConfigured : AdSetup
}

object AdsConfigResolver {
    fun resolve(env: AdsEnvironment): AdSetup = when {
        !env.isProdRelease -> AdSetup.Test
        AdMobIds.isLiveAppId(env.liveAppId) && AdMobIds.isLiveRewardedUnitId(env.liveRewardedUnitId) ->
            AdSetup.Live(env.liveRewardedUnitId)
        else -> AdSetup.NotConfigured
    }
}
