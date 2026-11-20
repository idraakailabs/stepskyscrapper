package com.idraak.stepskyscraper.core.domain.usecase

import com.idraak.stepskyscraper.core.domain.achievements.CityStats
import com.idraak.stepskyscraper.core.domain.cosmetics.CosmeticItem
import com.idraak.stepskyscraper.core.domain.cosmetics.UnlockRule
import javax.inject.Inject

/** Whether an item can be used now, and if not, exactly why (always shown as icon + text). */
sealed interface LockState {
    data object Unlocked : LockState

    data class Progress(val rule: UnlockRule, val current: Int, val required: Int) : LockState

    data class Price(val cost: Int, val affordable: Boolean) : LockState

    data object Premium : LockState

    data class Achievement(val achievementId: String) : LockState
}

data class UnlockContext(
    val stats: CityStats,
    val isPremium: Boolean,
    val purchased: Set<String>,
    val earnedAchievements: Set<String>,
    val blueprintBalance: Int,
)

class ResolveCosmeticLockUseCase @Inject constructor() {
    operator fun invoke(item: CosmeticItem, context: UnlockContext): LockState = when (val rule = item.rule) {
        UnlockRule.Free -> LockState.Unlocked
        is UnlockRule.FloorsBuilt -> progress(rule, context.stats.totalFloors, rule.floors)
        is UnlockRule.TowersFinished -> progress(rule, context.stats.towersFinished, rule.towers)
        is UnlockRule.BuildDays -> progress(rule, context.stats.buildDays, rule.days)
        is UnlockRule.ArchitectLevel -> progress(rule, context.stats.architectLevel, rule.level)
        is UnlockRule.Blueprints ->
            if (item.id in context.purchased) LockState.Unlocked else LockState.Price(rule.cost, context.blueprintBalance >= rule.cost)
        is UnlockRule.Achievement ->
            if (rule.achievementId in context.earnedAchievements) LockState.Unlocked else LockState.Achievement(rule.achievementId)
        UnlockRule.Premium -> if (context.isPremium) LockState.Unlocked else LockState.Premium
    }

    private fun progress(rule: UnlockRule, current: Int, required: Int): LockState =
        if (current >= required) LockState.Unlocked else LockState.Progress(rule, current, required)
}
