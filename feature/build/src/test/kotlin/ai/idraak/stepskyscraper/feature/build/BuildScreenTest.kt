package ai.idraak.stepskyscraper.feature.build

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
import ai.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import ai.idraak.stepskyscraper.core.domain.usecase.SkyTime
import ai.idraak.stepskyscraper.core.ui.mvi.Lce
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class BuildScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun show(content: BuildContent, dark: Boolean = false, syncing: Boolean = false, intents: MutableList<BuildIntent> = mutableListOf()) {
        compose.setContent {
            SsTheme(darkTheme = dark, reduceMotion = true) {
                BuildScreen(BuildUiState(Lce.Content(content), syncing = syncing), { intents += it })
            }
        }
    }

    @Test
    fun showsNextFloorHeroAndWeekCard() {
        show(previewBuildContent())
        compose.onNodeWithText("Next floor in").assertIsDisplayed()
        compose.onNodeWithText("35 of 50 floors this week").assertIsDisplayed()
        compose.onNodeWithText("15 to go · 3 days left").assertIsDisplayed()
        compose.onNodeWithContentDescription("Floor 15: 660 of 1,000 steps").assertIsDisplayed()
    }

    @Test
    fun accessibleTowerDescriptionAndActions() {
        val intents = mutableListOf<BuildIntent>()
        show(previewBuildContent(), intents = intents)
        compose.onNodeWithContentDescription("Customise tower").performClick()
        compose.onNodeWithContentDescription("Share tower").performClick()
        compose.onNodeWithText("35 of 50 floors this week").performClick()
        assertThat(intents).containsAtLeast(BuildIntent.OpenCustomise, BuildIntent.OpenShare, BuildIntent.OpenWeek)
        compose.onNode(androidx.compose.ui.test.hasContentDescription("Marlow Spire, 14 of 24 floors, next floor in 340 steps.", substring = true)).assertExists()
    }

    @Test
    fun zeroStepsShowsCalmCopy() {
        show(previewBuildContent(0, 11, weekFloors = 22, message = WeekMessage.Rest))
        compose.onNodeWithText("A fresh day. Your tower is right where you left it.").assertIsDisplayed()
        compose.onNodeWithText("Resting? You can pause goals").assertIsDisplayed()
    }

    @Test
    fun notConnectedShowsConnectCard() {
        val intents = mutableListOf<BuildIntent>()
        show(previewBuildContent(0, 0, access = StepAccessUi.NotConnected), intents = intents)
        compose.onNodeWithText("Connect steps to start building.").assertIsDisplayed()
        compose.onNodeWithText("Connect steps").performClick()
        assertThat(intents).contains(BuildIntent.ConnectSteps)
    }

    @Test
    fun syncingShowsChip() {
        show(previewBuildContent(stale = true), syncing = true)
        compose.onNodeWithText("Syncing…").assertIsDisplayed()
    }

    @Test
    fun screenshotDay() {
        show(previewBuildContent())
        compose.onRoot().captureRoboImage("src/test/screenshots/build_2a_day.png")
    }

    @Test
    fun screenshotDuskTargetMet() {
        show(previewBuildContent(8_420, 16, SkyTime.Dusk, weekFloors = 43, message = WeekMessage.Bonus))
        compose.onRoot().captureRoboImage("src/test/screenshots/build_2c_dusk.png")
    }

    @Test
    fun screenshotNightDark() {
        show(previewBuildContent(sky = SkyTime.Night), dark = true)
        compose.onRoot().captureRoboImage("src/test/screenshots/build_2f_night_dark.png")
    }
}

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onRoot() = onNode(androidx.compose.ui.test.isRoot())
