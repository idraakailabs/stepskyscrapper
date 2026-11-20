package com.idraak.stepskyscraper.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.idraak.stepskyscraper.core.designsystem.theme.SkyTokens
import com.idraak.stepskyscraper.core.designsystem.theme.SsRadius
import com.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import com.idraak.stepskyscraper.core.designsystem.tower.Facade
import com.idraak.stepskyscraper.core.designsystem.tower.GroundStyle
import com.idraak.stepskyscraper.core.designsystem.tower.SkylineCanvas
import com.idraak.stepskyscraper.core.designsystem.tower.SkylineSpec
import com.idraak.stepskyscraper.core.designsystem.tower.SkylineTower
import com.idraak.stepskyscraper.core.designsystem.tower.TowerCanvas
import com.idraak.stepskyscraper.core.designsystem.tower.TowerSpec
import com.idraak.stepskyscraper.core.designsystem.tower.TowerTime
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Visual parity of the tower.js port: the "Facade kits", "Sky follows the clock" and skyline boards. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1000dp-h900dp-xhdpi")
class TowerRendererScreenshotTest {
    @Test
    fun facadeKits() = captureRoboImage("src/test/screenshots/tower_facade_kits.png") {
        SsTheme(darkTheme = false) {
            Row(Modifier.background(SsTheme.colors.surface).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Facade.entries.forEach { f ->
                    Box(
                        Modifier.size(
                            180.dp,
                            300.dp,
                        ).background(SsTheme.colors.surfaceVariant, SsRadius.shapeCard).padding(12.dp, 16.dp, 12.dp, 8.dp),
                    ) {
                        TowerCanvas(
                            TowerSpec(
                                facade = f,
                                floors = 14,
                                target = 14,
                                ground = if (f ==
                                    Facade.Eco
                                ) {
                                    GroundStyle.Park
                                } else {
                                    GroundStyle.Plaza
                                },
                            ),
                            contentDescription = f.name,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun skies() = captureRoboImage("src/test/screenshots/tower_skies.png") {
        SsTheme(darkTheme = false) {
            Row(Modifier.background(SsTheme.colors.surface).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf(
                    TowerTime.Dawn to SkyTokens.Dawn,
                    TowerTime.Day to SkyTokens.Day,
                    TowerTime.Dusk to SkyTokens.Dusk,
                    TowerTime.Night to SkyTokens.Night,
                ).forEach { (t, sky) ->
                    Box(Modifier.size(220.dp, 260.dp).background(sky.brush(), SsRadius.shapeCard).padding(16.dp, 16.dp, 16.dp, 0.dp)) {
                        TowerCanvas(
                            TowerSpec(floors = 12, target = 12, time = t),
                            contentDescription = t.name,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun constructionAndSkyline() = captureRoboImage("src/test/screenshots/tower_construction_skyline.png") {
        SsTheme(darkTheme = false) {
            Column(Modifier.background(SkyTokens.Day.brush()).padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TowerCanvas(
                        TowerSpec(floors = 14, target = 24, progress = 0.66f, crane = true),
                        contentDescription = "building",
                        modifier = Modifier.size(312.dp, 296.dp),
                    )
                    TowerCanvas(
                        TowerSpec(floors = 16, target = 24, progress = 0.42f, crane = true, time = TowerTime.Dusk),
                        contentDescription = "dusk",
                        modifier = Modifier.size(312.dp, 296.dp),
                    )
                }
                SkylineCanvas(
                    SkylineSpec(
                        towers = listOf(
                            SkylineTower(Facade.Brut, 18),
                            SkylineTower(Facade.Eco, 20),
                            SkylineTower(Facade.Glass, 22),
                            SkylineTower(Facade.Brick, 16),
                            SkylineTower(Facade.Deco, 24),
                        ),
                        selected = 4,
                    ),
                    contentDescription = "skyline",
                    modifier = Modifier.size(340.dp, 250.dp),
                )
            }
        }
    }
}
