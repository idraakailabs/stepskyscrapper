package com.idraak.stepskyscraper.feature.skyline

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
import com.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import com.idraak.stepskyscraper.core.domain.usecase.SkyTime
import com.idraak.stepskyscraper.core.testing.TestGraph
import com.idraak.stepskyscraper.core.ui.mvi.Lce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class SkylineTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun districtShowsFinishedTowersAndPinsTheNewest() = runTest {
        val g = TestGraph()
        g.onboarded(daysAgo = 2, target = 10)
        g.steps.set(g.today.minusDays(1) to 7_000, g.today to 5_000)
        g.ensureTowers()
        val snapshot = g.progress().first()
        val (district, towers) = SkylineReducer.districtTowers(snapshot)
        assertThat(district).isEqualTo(1)
        assertThat(towers.map { it.index }).containsExactly(1)
        assertThat(SkylineReducer.newest(towers, snapshot.today)).isEqualTo(1)
        assertThat(SkylineReducer.newest(towers, snapshot.today.plusDays(5))).isNull()
    }

    @Test
    fun stepThroughTowersIsClamped() {
        val towers = previewSkyline(false, SkyTime.Day).towers
        assertThat(SkylineReducer.step(3, towers, 1)).isEqualTo(4)
        assertThat(SkylineReducer.step(5, towers, 1)).isEqualTo(5)
        assertThat(SkylineReducer.step(1, towers, -1)).isEqualTo(1)
    }

    @Test
    fun newTowerScreenShowsRewardCard() {
        compose.setContent {
            SsTheme(
                reduceMotion = true,
            ) { SkylineScreen(SkylineUiState(Lce.Content(previewSkyline(true, SkyTime.Day)))) {} }
        }
        compose.onNodeWithText("Marlow Spire joined the skyline").assertIsDisplayed()
        compose.onNodeWithText("Optional · about 30 seconds · once per tower").assertIsDisplayed()
        compose.onNode(isRoot()).captureRoboImage("src/test/screenshots/skyline_4a_new_tower.png")
    }

    @Test
    fun nightScreenShowsNextDistrict() {
        compose.setContent {
            SsTheme(
                darkTheme = true,
                reduceMotion = true,
            ) { SkylineScreen(SkylineUiState(Lce.Content(previewSkyline(false, SkyTime.Night)))) {} }
        }
        compose.onNodeWithText("5 of 8 towers").assertIsDisplayed()
        compose.onNode(isRoot()).captureRoboImage("src/test/screenshots/skyline_4c_night_dark.png")
    }
}
