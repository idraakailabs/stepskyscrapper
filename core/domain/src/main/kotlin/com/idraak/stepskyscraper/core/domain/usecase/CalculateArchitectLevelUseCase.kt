package com.idraak.stepskyscraper.core.domain.usecase

import com.idraak.stepskyscraper.core.domain.model.ArchitectLevel
import javax.inject.Inject

/** Architect level from lifetime floors. Level 7 "Site Architect" runs 68–119, level 8 starts at 120. */
class CalculateArchitectLevelUseCase @Inject constructor() {
    operator fun invoke(totalFloors: Int): ArchitectLevel {
        val floors = totalFloors.coerceAtLeast(0)
        val levelIndex = THRESHOLDS.indexOfLast { floors >= it }
        val start = THRESHOLDS[levelIndex]
        val next = THRESHOLDS.getOrNull(levelIndex + 1)
        return ArchitectLevel(
            level = levelIndex + 1,
            title = TITLES[levelIndex],
            floorsIntoLevel = floors - start,
            floorsForLevel = next?.minus(start) ?: 0,
            totalFloors = floors,
            nextLevelAtFloors = next,
            nextTitle = TITLES.getOrNull(levelIndex + 1),
        )
    }

    companion object {
        val THRESHOLDS = listOf(0, 5, 12, 22, 35, 50, 68, 120, 180, 260, 360, 480, 620, 800, 1_000)
        val TITLES = listOf(
            "Apprentice", "Bricklayer", "Site Hand", "Foreman", "Surveyor", "Draughtsperson", "Site Architect",
            "Chief Architect", "Master Builder", "City Planner", "Skyline Maker", "Landmark Architect",
            "Metropolis Planner", "Grand Architect", "City Founder",
        )
    }
}
