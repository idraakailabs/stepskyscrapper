package ai.idraak.stepskyscraper.core.domain.usecase

import java.time.LocalTime

enum class SkyTime { Dawn, Day, Dusk, Night }

/**
 * Sky follows the clock: dawn 05:00–08:00, day 08:00–17:30, dusk 17:30–20:00, night 20:00–05:00.
 * Around each boundary the sky crossfades over 20 minutes (10 before, 10 after), linearly.
 */
data class SkyPhase(val from: SkyTime, val to: SkyTime, val blend: Float) {
    /** The dominant sky (used for tower tint, lit windows and header ink). */
    val dominant: SkyTime get() = if (blend < 0.5f) from else to
}

object SkyClock {
    private const val HALF_FADE_MINUTES = 10
    private val boundaries = listOf(
        LocalTime.of(5, 0) to (SkyTime.Night to SkyTime.Dawn),
        LocalTime.of(8, 0) to (SkyTime.Dawn to SkyTime.Day),
        LocalTime.of(17, 30) to (SkyTime.Day to SkyTime.Dusk),
        LocalTime.of(20, 0) to (SkyTime.Dusk to SkyTime.Night),
    )

    fun timeAt(time: LocalTime): SkyTime = when {
        time < LocalTime.of(5, 0) -> SkyTime.Night
        time < LocalTime.of(8, 0) -> SkyTime.Dawn
        time < LocalTime.of(17, 30) -> SkyTime.Day
        time < LocalTime.of(20, 0) -> SkyTime.Dusk
        else -> SkyTime.Night
    }

    fun phaseAt(time: LocalTime): SkyPhase {
        val minute = time.hour * 60 + time.minute + time.second / 60f
        for ((boundary, pair) in boundaries) {
            val b = boundary.hour * 60 + boundary.minute
            val delta = minute - b
            if (delta >= -HALF_FADE_MINUTES && delta < HALF_FADE_MINUTES) {
                val blend = (delta + HALF_FADE_MINUTES) / (2f * HALF_FADE_MINUTES)
                return SkyPhase(pair.first, pair.second, blend.coerceIn(0f, 1f))
            }
        }
        val t = timeAt(time)
        return SkyPhase(t, t, 0f)
    }

    /** Greeting replaces the tower count before 11:00. */
    fun isMorningGreeting(time: LocalTime): Boolean = time < LocalTime.of(11, 0) && time >= LocalTime.of(4, 0)
}
