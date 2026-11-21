package ai.idraak.stepskyscraper.core.ads

import android.util.Log

internal object AdsLog {
    /** Logcat tag for every ad decision: `adb logcat -s StepSkyAds`. Never logs step or floor values. */
    const val TAG = "StepSkyAds"

    fun i(message: String) {
        // runCatching: android.util.Log is a stub in plain JVM unit tests.
        runCatching { Log.i(TAG, message) }
    }
}
