package com.idraak.stepskyscraper.core.domain.usecase

import javax.inject.Inject
import kotlin.random.Random

/**
 * Original tower names from street / tree / material words ("Marlow Spire", "Calder Glass").
 * A seed gives a stable name (used for towers the user has not named yet); no seed rolls the dice.
 * Names never exceed [MAX_LENGTH] characters (design rule: one line, autosize 34 → 26sp).
 */
class GenerateTowerNameUseCase @Inject constructor() {
    operator fun invoke(seed: Long? = null, avoid: String? = null): String {
        val random = if (seed != null) Random(seed * SEED_MIX) else Random.Default
        repeat(MAX_ATTEMPTS) {
            val name = "${FIRST.random(random)} ${SECOND.random(random)}"
            if (name.length <= MAX_LENGTH && name != avoid) return name
        }
        return FALLBACK
    }

    companion object {
        const val MAX_LENGTH = 18
        private const val MAX_ATTEMPTS = 24
        private const val SEED_MIX = 7_919L
        private const val FALLBACK = "Marlow Spire"

        private val FIRST = listOf(
            "Marlow", "Calder", "Harbour", "Juniper", "Alder", "Linden", "Cobalt", "Ashby", "Rowan", "Hazel",
            "Fenwick", "Larch", "Copper", "Granite", "Quarry", "Wren", "Elm", "Bramble", "Slate", "Oakley",
            "Thistle", "Birch", "Cedar", "Kestrel", "Hollis", "Mercer", "Ember", "Sable", "Willow", "Tamsin",
        )
        private val SECOND = listOf(
            "Spire", "Glass", "Row", "Works", "House", "Tower", "Court", "Point", "Hall", "Building",
            "Heights", "Exchange", "Mill", "Lofts", "Yard", "Terrace", "Arcade", "Place",
        )
    }
}

/** District names: "Juniper Quarter", "Linden Yards"… stable per district index. */
object DistrictNames {
    private val FIRST = listOf("Juniper", "Linden", "Alder", "Harbour", "Rowan", "Calder", "Hazel", "Copper", "Wren", "Cedar")
    private val SECOND = listOf("Quarter", "Yards", "Heights", "Commons", "Village", "Wharf", "Gardens", "Crossing")

    fun forDistrict(index: Int): String {
        val i = index.coerceAtLeast(0)
        return "${FIRST[i % FIRST.size]} ${SECOND[(i / FIRST.size + i) % SECOND.size]}"
    }
}
