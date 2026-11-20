package ai.idraak.stepskyscraper.debug

import ai.idraak.stepskyscraper.core.data.di.DebugClockShift
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.edit
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Singleton

/**
 * Debug builds only: shifts the app clock so the time-of-day skies can be checked on a real device without
 * touching the phone's system time.
 *
 * ```
 * adb shell am broadcast -n ai.idraak.stepskyscraper.debug/ai.idraak.stepskyscraper.debug.DebugClockReceiver --es time 18:30
 * adb shell am broadcast -n ai.idraak.stepskyscraper.debug/ai.idraak.stepskyscraper.debug.DebugClockReceiver --es time 2026-11-22T10:00
 * adb shell am broadcast -n ai.idraak.stepskyscraper.debug/ai.idraak.stepskyscraper.debug.DebugClockReceiver --es time off
 * ```
 * The shift is fixed when set (the clock keeps running from the chosen time). Screens pick it up on their
 * next minute tick or when reopened.
 */
class DebugClockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val time = intent.getStringExtra("time") ?: return
        val shiftMs = when {
            time == "off" -> 0L
            // Full local date-time, e.g. 2026-11-22T10:00 (reach other days, such as a day with no steps yet).
            'T' in time -> Duration.between(LocalDateTime.now(), LocalDateTime.parse(time)).toMillis()
            else -> Duration.between(LocalTime.now(), LocalTime.parse(time)).toMillis()
        }
        prefs(context).edit { putLong(KEY_SHIFT, shiftMs) }
        Log.i("DebugClock", "clock shift set to ${Duration.ofMillis(shiftMs)}")
    }
}

private const val KEY_SHIFT = "shift_ms"

private fun prefs(context: Context) = context.getSharedPreferences("debug_clock", Context.MODE_PRIVATE)

@Module
@InstallIn(SingletonComponent::class)
object DebugClockModule {
    @Provides
    @Singleton
    fun debugClockShift(@ApplicationContext context: Context): DebugClockShift {
        val prefs = prefs(context)
        return DebugClockShift { Duration.ofMillis(prefs.getLong(KEY_SHIFT, 0L)) }
    }
}
