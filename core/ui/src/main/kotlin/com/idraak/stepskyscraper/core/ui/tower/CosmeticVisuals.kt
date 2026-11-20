package com.idraak.stepskyscraper.core.ui.tower

import androidx.annotation.StringRes
import com.idraak.stepskyscraper.core.designsystem.theme.SkyTone
import com.idraak.stepskyscraper.core.designsystem.tower.CrownMode
import com.idraak.stepskyscraper.core.designsystem.tower.CrownStyle
import com.idraak.stepskyscraper.core.designsystem.tower.Facade
import com.idraak.stepskyscraper.core.designsystem.tower.GroundStyle
import com.idraak.stepskyscraper.core.designsystem.tower.LightsStyle
import com.idraak.stepskyscraper.core.designsystem.tower.SkylineTower
import com.idraak.stepskyscraper.core.designsystem.tower.TowerPalette
import com.idraak.stepskyscraper.core.designsystem.tower.TowerSpec
import com.idraak.stepskyscraper.core.designsystem.tower.TowerTime
import com.idraak.stepskyscraper.core.domain.cosmetics.CosmeticCatalog
import com.idraak.stepskyscraper.core.domain.cosmetics.NATIVE_PALETTE
import com.idraak.stepskyscraper.core.domain.model.FacadeKit
import com.idraak.stepskyscraper.core.domain.model.Tower
import com.idraak.stepskyscraper.core.domain.model.TowerLook
import com.idraak.stepskyscraper.core.domain.usecase.SkyTime
import com.idraak.stepskyscraper.core.ui.R

/** Maps domain cosmetics and times onto the tower renderer. */
object CosmeticVisuals {
    /** Palette swatches (light, mid, trim). Sandstone/Slate/Sage/Rose Clay are the design's exact swatches. */
    val palettes: Map<String, TowerPalette> = mapOf(
        "palette_sandstone" to TowerPalette(0xE9DCC3, 0xCBB793, 0xB8873A),
        "palette_slate" to TowerPalette(0xC9CED3, 0x8F98A2, 0x4D5A66),
        "palette_sage" to TowerPalette(0xDCE3D2, 0xA9B898, 0x5F7552),
        "palette_rose_clay" to TowerPalette(0xE8C9BC, 0xC7907C, 0x8E5444),
        "palette_harbour" to TowerPalette(0xA8BFCB, 0x7D98A8, 0x4D6A7D),
        "palette_dawn" to TowerPalette(0xF2D4C4, 0xD9A890, 0xB06F55),
        "palette_ochre" to TowerPalette(0xE8D3A0, 0xC9A85E, 0x8A6A24),
        "palette_midnight" to TowerPalette(0x4A5478, 0x343C5E, 0xC9A85E),
        "palette_coral" to TowerPalette(0xF2B8A8, 0xD98872, 0x9E4E3A),
        "palette_graphite" to TowerPalette(0x9A9A9A, 0x6E6E6E, 0x3A3A3A),
        "palette_mint" to TowerPalette(0xD2EDE0, 0x9FCDB8, 0x4F8A72),
        "palette_pearl" to TowerPalette(0xF4F1EA, 0xDAD5CB, 0xA89F90),
        "palette_ivy" to TowerPalette(0xB9CBA5, 0x869D6E, 0x4A5E38),
        "palette_copper" to TowerPalette(0xD9A27A, 0xB37A52, 0x7A4A2A),
        "palette_soot" to TowerPalette(0x9A7A6A, 0x75594D, 0x2E2622),
        "palette_twilight" to TowerPalette(0x6A6F9A, 0x4B5079, 0xC9A85E),
        "palette_terrace" to TowerPalette(0xE8E6D8, 0xC6C8B4, 0x6F8A5A),
        "palette_marble" to TowerPalette(0xEEEAE4, 0xD3CEC6, 0x9A948A),
        "palette_bamboo" to TowerPalette(0xE2DCC0, 0xC2B88E, 0x7E8A4A),
    )

    /** The kit's own colours, shown for the "Original" palette swatch. */
    val nativeSwatch: Map<FacadeKit, TowerPalette> = mapOf(
        FacadeKit.Deco to TowerPalette(0xE9DCC3, 0xCBB793, 0xB8873A),
        FacadeKit.Glass to TowerPalette(0xA8BFCB, 0x7D98A8, 0x4D6A7D),
        FacadeKit.Brick to TowerPalette(0xC57B5D, 0xA26048, 0xF1E7D9),
        FacadeKit.Brut to TowerPalette(0xC6C1B8, 0xA5A097, 0x47433E),
        FacadeKit.Eco to TowerPalette(0xEEEEE4, 0xCED1C4, 0x7EA26A),
    )

    fun facade(kit: FacadeKit): Facade = when (kit) {
        FacadeKit.Deco -> Facade.Deco
        FacadeKit.Glass -> Facade.Glass
        FacadeKit.Brick -> Facade.Brick
        FacadeKit.Brut -> Facade.Brut
        FacadeKit.Eco -> Facade.Eco
    }

    fun crown(id: String): CrownStyle = when (id) {
        "crown_ziggurat" -> CrownStyle.Ziggurat
        "crown_mast" -> CrownStyle.Mast
        "crown_tank" -> CrownStyle.Tank
        "crown_cantilever" -> CrownStyle.Cantilever
        "crown_garden" -> CrownStyle.Garden
        "crown_terrace" -> CrownStyle.Terrace
        "crown_gilded" -> CrownStyle.Gilded
        "crown_beacon" -> CrownStyle.Beacon
        else -> CrownStyle.Native
    }

    fun lights(id: String): LightsStyle = when (id) {
        "lights_office" -> LightsStyle.Office
        "lights_moonlit" -> LightsStyle.Moonlit
        "lights_festival" -> LightsStyle.Festival
        "lights_twinkle" -> LightsStyle.Twinkle
        else -> LightsStyle.Warm
    }

    fun ground(id: String): GroundStyle = when (id) {
        "ground_park" -> GroundStyle.Park
        "ground_plaque" -> GroundStyle.Plaque
        "ground_canal" -> GroundStyle.Canal
        "ground_tram" -> GroundStyle.Tram
        "ground_market" -> GroundStyle.Market
        else -> GroundStyle.Plaza
    }

    /** Facade item palette (e.g. Copper Deco) wins unless the user picked a palette explicitly. */
    fun palette(look: TowerLook): TowerPalette? {
        if (look.paletteId != NATIVE_PALETTE) return palettes[look.paletteId]
        return CosmeticCatalog.item(look.facadeId)?.paletteId?.let { palettes[it] }
    }

    fun time(sky: SkyTime): TowerTime = when (sky) {
        SkyTime.Dawn -> TowerTime.Dawn
        SkyTime.Day -> TowerTime.Day
        SkyTime.Dusk -> TowerTime.Dusk
        SkyTime.Night -> TowerTime.Night
    }

    fun tone(sky: SkyTime): SkyTone = when (sky) {
        SkyTime.Dawn -> SkyTone.Dawn
        SkyTime.Day -> SkyTone.Day
        SkyTime.Dusk -> SkyTone.Dusk
        SkyTime.Night -> SkyTone.Night
    }

    @StringRes
    fun kitName(kit: FacadeKit): Int = when (kit) {
        FacadeKit.Deco -> R.string.kit_deco
        FacadeKit.Glass -> R.string.kit_glass
        FacadeKit.Brick -> R.string.kit_brick
        FacadeKit.Brut -> R.string.kit_brut
        FacadeKit.Eco -> R.string.kit_eco
    }

    @StringRes
    fun kitShortName(kit: FacadeKit): Int = when (kit) {
        FacadeKit.Deco -> R.string.kit_deco
        FacadeKit.Glass -> R.string.kit_glass_short
        FacadeKit.Brick -> R.string.kit_brick_short
        FacadeKit.Brut -> R.string.kit_brut
        FacadeKit.Eco -> R.string.kit_eco_short
    }
}

/** Builds the renderer spec for a tower in the city. */
fun Tower.toSpec(
    sky: SkyTime,
    crane: Boolean = !isComplete,
    progress: Float = 0f,
    crownMode: CrownMode = CrownMode.Auto,
    showGround: Boolean = true,
): TowerSpec = record.look.toSpec(
    floors = floorsBuilt,
    target = target,
    sky = sky,
    crane = crane,
    progress = if (isComplete) 0f else progress,
    crownMode = crownMode,
    seed = index,
    showGround = showGround,
)

@Suppress("LongParameterList")
fun TowerLook.toSpec(
    floors: Int,
    target: Int,
    sky: SkyTime,
    crane: Boolean = floors < target,
    progress: Float = 0f,
    crownMode: CrownMode = CrownMode.Auto,
    seed: Int = 1,
    showGround: Boolean = true,
    mono: Int? = null,
): TowerSpec = TowerSpec(
    facade = CosmeticVisuals.facade(CosmeticCatalog.kitOf(facadeId)),
    floors = floors,
    target = target,
    progress = progress,
    crane = crane,
    crownMode = crownMode,
    crownStyle = CosmeticVisuals.crown(crownId),
    palette = CosmeticVisuals.palette(this),
    lights = CosmeticVisuals.lights(lightsId),
    ground = if (showGround) CosmeticVisuals.ground(groundId) else GroundStyle.None,
    time = CosmeticVisuals.time(sky),
    seed = seed,
    mono = mono,
)

fun Tower.toSkylineTower(): SkylineTower = SkylineTower(
    facade = CosmeticVisuals.facade(CosmeticCatalog.kitOf(record.look.facadeId)),
    floors = floorsBuilt,
    target = target,
    crownStyle = CosmeticVisuals.crown(record.look.crownId),
    palette = CosmeticVisuals.palette(record.look),
    lights = CosmeticVisuals.lights(record.look.lightsId),
)
