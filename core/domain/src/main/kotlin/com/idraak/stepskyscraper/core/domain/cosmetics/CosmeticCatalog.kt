package com.idraak.stepskyscraper.core.domain.cosmetics

import com.idraak.stepskyscraper.core.domain.model.FacadeKit

enum class CosmeticCategory { Facade, Palette, Crown, Lights, Ground }

/** How a cosmetic is unlocked. Every locked item tells the user exactly this. */
sealed interface UnlockRule {
    data object Free : UnlockRule

    data class FloorsBuilt(val floors: Int) : UnlockRule

    data class TowersFinished(val towers: Int) : UnlockRule

    data class BuildDays(val days: Int) : UnlockRule

    data class ArchitectLevel(val level: Int) : UnlockRule

    data class Blueprints(val cost: Int) : UnlockRule

    data class Achievement(val achievementId: String) : UnlockRule

    data object Premium : UnlockRule
}

data class CosmeticItem(
    val id: String,
    val category: CosmeticCategory,
    val name: String,
    val rule: UnlockRule,
    /** Facade items render with this kit; palettes/crowns/lights/grounds leave it null. */
    val kit: FacadeKit? = null,
    /** Facade items may carry a palette override (e.g. Copper Deco = Deco kit + copper palette). */
    val paletteId: String? = null,
)

/** The full, static list of cosmetics. Premium count = 12 (4 facades, 2 crowns, 6 palettes). */
object CosmeticCatalog {
    const val NATIVE = "native"

    val facades = listOf(
        CosmeticItem("facade_deco", CosmeticCategory.Facade, "Art Deco", UnlockRule.Free, FacadeKit.Deco),
        CosmeticItem("facade_glass", CosmeticCategory.Facade, "Glass Modern", UnlockRule.Free, FacadeKit.Glass),
        CosmeticItem("facade_brick", CosmeticCategory.Facade, "Brick Classic", UnlockRule.Free, FacadeKit.Brick),
        CosmeticItem("facade_brut", CosmeticCategory.Facade, "Brutalist", UnlockRule.Free, FacadeKit.Brut),
        CosmeticItem("facade_eco", CosmeticCategory.Facade, "Eco / Green", UnlockRule.Free, FacadeKit.Eco),
        CosmeticItem(
            "facade_copper_deco",
            CosmeticCategory.Facade,
            "Copper Deco",
            UnlockRule.Achievement("double_century"),
            FacadeKit.Deco,
            "palette_copper",
        ),
        CosmeticItem(
            "facade_warehouse_loft",
            CosmeticCategory.Facade,
            "Warehouse Loft",
            UnlockRule.TowersFinished(10),
            FacadeKit.Brick,
            "palette_soot",
        ),
        CosmeticItem(
            "facade_twilight_glass",
            CosmeticCategory.Facade,
            "Twilight Glass",
            UnlockRule.Premium,
            FacadeKit.Glass,
            "palette_twilight",
        ),
        CosmeticItem(
            "facade_terrace_garden",
            CosmeticCategory.Facade,
            "Terrace Garden",
            UnlockRule.Blueprints(400),
            FacadeKit.Eco,
            "palette_terrace",
        ),
        CosmeticItem(
            "facade_midnight_deco",
            CosmeticCategory.Facade,
            "Midnight Deco",
            UnlockRule.Premium,
            FacadeKit.Deco,
            "palette_midnight",
        ),
        CosmeticItem(
            "facade_marble_brut",
            CosmeticCategory.Facade,
            "Marble Block",
            UnlockRule.Premium,
            FacadeKit.Brut,
            "palette_marble",
        ),
        CosmeticItem(
            "facade_bamboo_eco",
            CosmeticCategory.Facade,
            "Bamboo Eco",
            UnlockRule.Premium,
            FacadeKit.Eco,
            "palette_bamboo",
        ),
    )

    val palettes = listOf(
        CosmeticItem(NATIVE_PALETTE, CosmeticCategory.Palette, "Original", UnlockRule.Free),
        CosmeticItem("palette_sandstone", CosmeticCategory.Palette, "Sandstone", UnlockRule.Free),
        CosmeticItem("palette_slate", CosmeticCategory.Palette, "Slate", UnlockRule.Free),
        CosmeticItem("palette_sage", CosmeticCategory.Palette, "Sage", UnlockRule.Free),
        CosmeticItem("palette_harbour", CosmeticCategory.Palette, "Harbour", UnlockRule.Free),
        CosmeticItem("palette_rose_clay", CosmeticCategory.Palette, "Rose Clay", UnlockRule.FloorsBuilt(200)),
        CosmeticItem("palette_dawn", CosmeticCategory.Palette, "Dawn", UnlockRule.Achievement("early_riser")),
        CosmeticItem("palette_ochre", CosmeticCategory.Palette, "Ochre", UnlockRule.Blueprints(250)),
        CosmeticItem("palette_midnight", CosmeticCategory.Palette, "Midnight", UnlockRule.Premium),
        CosmeticItem("palette_coral", CosmeticCategory.Palette, "Coral", UnlockRule.Premium),
        CosmeticItem("palette_graphite", CosmeticCategory.Palette, "Graphite", UnlockRule.Premium),
        CosmeticItem("palette_mint", CosmeticCategory.Palette, "Mint", UnlockRule.Premium),
        CosmeticItem("palette_pearl", CosmeticCategory.Palette, "Pearl", UnlockRule.Premium),
        CosmeticItem("palette_ivy", CosmeticCategory.Palette, "Ivy", UnlockRule.Premium),
    )

    val crowns = listOf(
        CosmeticItem(NATIVE_CROWN, CosmeticCategory.Crown, "Matching crown", UnlockRule.Free),
        CosmeticItem("crown_ziggurat", CosmeticCategory.Crown, "Ziggurat spire", UnlockRule.Free, FacadeKit.Deco),
        CosmeticItem("crown_mast", CosmeticCategory.Crown, "Mast", UnlockRule.Free, FacadeKit.Glass),
        CosmeticItem("crown_tank", CosmeticCategory.Crown, "Water tank", UnlockRule.Free, FacadeKit.Brick),
        CosmeticItem("crown_cantilever", CosmeticCategory.Crown, "Cantilever", UnlockRule.Free, FacadeKit.Brut),
        CosmeticItem("crown_garden", CosmeticCategory.Crown, "Roof garden", UnlockRule.Free, FacadeKit.Eco),
        CosmeticItem(
            "crown_terrace",
            CosmeticCategory.Crown,
            "Hammock roof terrace",
            UnlockRule.Achievement("rest_is_building"),
            FacadeKit.Eco,
        ),
        CosmeticItem("crown_gilded", CosmeticCategory.Crown, "Gilded spire", UnlockRule.Premium, FacadeKit.Deco),
        CosmeticItem("crown_beacon", CosmeticCategory.Crown, "Beacon mast", UnlockRule.Premium, FacadeKit.Glass),
    )

    val lights = listOf(
        CosmeticItem("lights_warm", CosmeticCategory.Lights, "Warm lamplight", UnlockRule.Free),
        CosmeticItem("lights_office", CosmeticCategory.Lights, "Office white", UnlockRule.Free),
        CosmeticItem("lights_moonlit", CosmeticCategory.Lights, "Moonlit windows", UnlockRule.Achievement("night_shift")),
        CosmeticItem("lights_festival", CosmeticCategory.Lights, "Festival", UnlockRule.ArchitectLevel(10)),
        CosmeticItem("lights_twinkle", CosmeticCategory.Lights, "Slow twinkle", UnlockRule.Premium),
    )

    val grounds = listOf(
        CosmeticItem("ground_plaza", CosmeticCategory.Ground, "Stone plaza", UnlockRule.Free),
        CosmeticItem("ground_park", CosmeticCategory.Ground, "Pocket park", UnlockRule.Free),
        CosmeticItem("ground_plaque", CosmeticCategory.Ground, "Name plaque", UnlockRule.Achievement("five_towers")),
        CosmeticItem("ground_canal", CosmeticCategory.Ground, "Canal", UnlockRule.BuildDays(30)),
        CosmeticItem("ground_tram", CosmeticCategory.Ground, "Tram stop + park", UnlockRule.Achievement("full_district")),
        CosmeticItem("ground_market", CosmeticCategory.Ground, "Market stalls", UnlockRule.Premium),
    )

    val all: List<CosmeticItem> = facades + palettes + crowns + lights + grounds

    private val byId = all.associateBy { it.id }

    fun item(id: String): CosmeticItem? = byId[id]

    fun inCategory(category: CosmeticCategory): List<CosmeticItem> = all.filter { it.category == category }

    fun facadeForKit(kit: FacadeKit): CosmeticItem = facades.first { it.kit == kit && it.rule == UnlockRule.Free }

    fun kitOf(facadeId: String): FacadeKit = item(facadeId)?.kit ?: FacadeKit.Deco

    val premiumCount: Int get() = all.count {
        it.rule == UnlockRule.Premium && it.category != CosmeticCategory.Lights &&
            it.category != CosmeticCategory.Ground
    }
}

const val NATIVE_PALETTE = "palette_native"
const val NATIVE_CROWN = "crown_native"
