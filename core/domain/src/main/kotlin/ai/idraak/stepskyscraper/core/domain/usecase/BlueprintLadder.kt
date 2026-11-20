package ai.idraak.stepskyscraper.core.domain.usecase

/**
 * Tower heights grow along a ladder (10, 25, 50, 100, 160, 250 by default; Remote Config may override).
 * Tower n (1-based) uses step n of the ladder; after the last step every tower uses the final height.
 */
object BlueprintLadder {
    val DEFAULT: List<Int> = listOf(10, 25, 50, 100, 160, 250)

    /** Rejects a malformed remote ladder (empty, non-positive, or not increasing) in favour of the default. */
    fun sanitize(candidate: List<Int>?): List<Int> {
        if (candidate.isNullOrEmpty()) return DEFAULT
        if (candidate.any { it !in 1..MAX_FLOORS }) return DEFAULT
        if (candidate.zipWithNext().any { (a, b) -> b < a }) return DEFAULT
        return candidate
    }

    fun targetFor(towerIndex: Int, ladder: List<Int>): Int {
        require(towerIndex >= 1) { "Tower index is 1-based" }
        val steps = sanitize(ladder)
        return steps[(towerIndex - 1).coerceAtMost(steps.lastIndex)]
    }

    /** Blueprints awarded for topping out: 5 per floor (a 24-floor tower earns 120). */
    fun blueprintsFor(targetFloors: Int): Int = targetFloors * BLUEPRINTS_PER_FLOOR

    const val BLUEPRINTS_PER_FLOOR = 5
    private const val MAX_FLOORS = 1_000
}
