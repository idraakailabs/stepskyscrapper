package com.idraak.stepskyscraper.core.domain

import com.google.common.truth.Truth.assertThat
import com.idraak.stepskyscraper.core.domain.achievements.AchievementCatalog
import com.idraak.stepskyscraper.core.domain.achievements.CityStats
import com.idraak.stepskyscraper.core.domain.cosmetics.CosmeticCatalog
import com.idraak.stepskyscraper.core.domain.cosmetics.UnlockRule
import com.idraak.stepskyscraper.core.domain.usecase.GenerateTowerNameUseCase
import com.idraak.stepskyscraper.core.domain.usecase.LockState
import com.idraak.stepskyscraper.core.domain.usecase.ResolveCosmeticLockUseCase
import com.idraak.stepskyscraper.core.domain.usecase.UnlockContext
import org.junit.Test

class CosmeticsAndAchievementsTest {
    private val resolve = ResolveCosmeticLockUseCase()
    private val base =
        UnlockContext(CityStats.Empty, isPremium = false, purchased = emptySet(), earnedAchievements = emptySet(), blueprintBalance = 0)

    @Test
    fun `there are 24 achievements with unique ids and none depends on consecutive days`() {
        assertThat(AchievementCatalog.all).hasSize(24)
        assertThat(AchievementCatalog.all.map { it.id }.toSet()).hasSize(24)
        assertThat(AchievementCatalog.all.map { it.description.lowercase() }.none { "in a row" in it || "streak" in it }).isTrue()
    }

    @Test
    fun `achievement measures read the right stat`() {
        val stats = CityStats.Empty.copy(totalFloors = 100, nightShiftDays = 6, fullestDistrictTowers = 5)
        assertThat(AchievementCatalog.definition("double_century")!!.measure(stats)).isEqualTo(100)
        assertThat(AchievementCatalog.definition("night_shift")!!.measure(stats)).isEqualTo(6)
        assertThat(AchievementCatalog.definition("full_district")!!.measure(stats)).isEqualTo(5)
    }

    @Test
    fun `the catalogue has twelve premium facades crowns and palettes and five free launch styles`() {
        assertThat(CosmeticCatalog.premiumCount).isEqualTo(12)
        assertThat(CosmeticCatalog.facades.count { it.rule == UnlockRule.Free }).isEqualTo(5)
    }

    @Test
    fun `three lock types resolve to progress price and premium`() {
        val copperPalette = CosmeticCatalog.item("palette_rose_clay")!!
        assertThat(resolve(copperPalette, base)).isEqualTo(LockState.Progress(UnlockRule.FloorsBuilt(200), 0, 200))
        val terrace = CosmeticCatalog.item("facade_terrace_garden")!!
        assertThat(resolve(terrace, base.copy(blueprintBalance = 340))).isEqualTo(LockState.Price(400, affordable = false))
        assertThat(resolve(terrace, base.copy(purchased = setOf(terrace.id)))).isEqualTo(LockState.Unlocked)
        val twilight = CosmeticCatalog.item("facade_twilight_glass")!!
        assertThat(resolve(twilight, base)).isEqualTo(LockState.Premium)
        assertThat(resolve(twilight, base.copy(isPremium = true))).isEqualTo(LockState.Unlocked)
    }

    @Test
    fun `tower names are original and fit on one line`() {
        val gen = GenerateTowerNameUseCase()
        repeat(200) { assertThat(gen(seed = it.toLong()).length).isAtMost(GenerateTowerNameUseCase.MAX_LENGTH) }
        assertThat(gen(seed = 3)).isEqualTo(gen(seed = 3))
        assertThat(gen(avoid = "x")).isNotEmpty()
    }
}
