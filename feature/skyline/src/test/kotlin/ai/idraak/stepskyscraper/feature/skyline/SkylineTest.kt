package ai.idraak.stepskyscraper.feature.skyline

import ai.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import ai.idraak.stepskyscraper.core.domain.usecase.SkyTime
import ai.idraak.stepskyscraper.core.testing.MainDispatcherRule
import ai.idraak.stepskyscraper.core.testing.TestGraph
import ai.idraak.stepskyscraper.core.ui.mvi.Lce
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.SavedStateHandle
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
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

    @get:Rule val main = MainDispatcherRule()

    private fun viewModel(g: TestGraph, highlight: Int?) = SkylineViewModel(
        SavedStateHandle(if (highlight != null) mapOf("highlight" to highlight) else emptyMap()),
        g.progress,
        g.towers,
        g.ads,
        g.claimBonus,
        g.wallet,
        g.analytics,
        g.clock,
    )

    @Test
    fun lateCeremonyHighlightsTheRequestedTower() = runTest {
        // Tower 1 topped out 9 days ago: past the 24h "newest" window, so only the route argument can highlight it.
        val g = TestGraph()
        g.onboarded(daysAgo = 10, target = 10)
        g.steps.set(g.today.minusDays(9) to 12_000)
        g.ensureTowers()

        val plain = (viewModel(g, highlight = null).state.value.content as Lce.Content).value
        assertThat(plain.newestIndex).isNull()
        assertThat(plain.reward).isNull()

        val highlighted = (viewModel(g, highlight = 1).state.value.content as Lce.Content).value
        assertThat(highlighted.newestIndex).isEqualTo(1)
        assertThat(highlighted.pinnedIndex).isEqualTo(highlighted.towers.indexOfFirst { it.index == 1 })
        assertThat(highlighted.reward?.towerIndex).isEqualTo(1)
    }

    @Test
    fun highlightIgnoresTowersOutsideTheDistrict() {
        val towers = previewSkyline(false, SkyTime.Day).towers
        assertThat(SkylineReducer.highlighted(towers, LocalDate.of(2030, 1, 1), 3)).isEqualTo(3)
        assertThat(SkylineReducer.highlighted(towers, LocalDate.of(2030, 1, 1), 99)).isNull()
    }

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
