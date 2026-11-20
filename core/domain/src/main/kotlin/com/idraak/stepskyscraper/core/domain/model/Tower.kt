package com.idraak.stepskyscraper.core.domain.model

import java.time.LocalDate

/** The five launch facade kits; each is a lobby + repeatable floor + setbacks + crown (see tower renderer). */
enum class FacadeKit { Deco, Glass, Brick, Brut, Eco }

/** Cosmetic choices for one tower. Ids reference [com.idraak.stepskyscraper.core.domain.cosmetics.CosmeticCatalog]. */
data class TowerLook(
    val facadeId: String,
    val paletteId: String,
    val crownId: String,
    val lightsId: String,
    val groundId: String,
)

/** What the user chose and what was frozen for a tower when it was started. */
data class TowerRecord(
    val index: Int,
    val name: String,
    val look: TowerLook,
    /** Target frozen when the tower was founded so a Remote Config ladder change never rewrites history. */
    val targetFloors: Int,
    val foundedOn: LocalDate,
    val ceremonySeen: Boolean,
    val rewardedAdClaimed: Boolean,
)

/** A tower with its derived (computed, never accumulated) progress. */
data class Tower(
    val record: TowerRecord,
    val floorsBuilt: Int,
    val startedOn: LocalDate?,
    val toppedOutOn: LocalDate?,
    val buildDays: Int,
    val steps: Long,
    val bestDay: BestDay?,
) {
    val index: Int get() = record.index
    val name: String get() = record.name
    val target: Int get() = record.targetFloors
    val isComplete: Boolean get() = floorsBuilt >= target
    val floorsRemaining: Int get() = (target - floorsBuilt).coerceAtLeast(0)
}

data class BestDay(val date: LocalDate, val floors: Int)

/** Every 8 towers found a district. */
data class District(
    val index: Int,
    val name: String,
    val towers: List<Tower>,
    val foundedOn: LocalDate?,
) {
    val floors: Int get() = towers.sumOf { it.floorsBuilt }

    companion object {
        const val TOWERS_PER_DISTRICT = 8
    }
}

/** The whole derived city for the current day totals. */
data class City(
    val towers: List<Tower>,
    val activeTower: Tower,
    val totalFloors: Int,
    val totalSteps: Long,
    val buildDays: Int,
) {
    val finishedTowers: List<Tower> get() = towers.filter { it.isComplete }
}
