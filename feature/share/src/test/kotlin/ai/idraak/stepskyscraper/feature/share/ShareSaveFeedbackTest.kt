package ai.idraak.stepskyscraper.feature.share

import ai.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import ai.idraak.stepskyscraper.core.designsystem.tower.TowerSpec
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** BUG-003: "Save image" reports its result. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class ShareSaveFeedbackTest {
    @get:Rule val compose = createComposeRule()

    private fun show(saveResult: Boolean?) {
        val tower = TowerCardData("Marlow Spire", 24, "Art Deco", "NOV 2026", 24_310, 11, TowerSpec(floors = 24, target = 24))
        compose.setContent {
            SsTheme(reduceMotion = true) {
                ShareScreen(ShareUiState(tower = tower, skyline = SkylineCardData("Juniper Quarter", emptyList(), 0, 0, ""), saveResult = saveResult), {}, {})
            }
        }
    }

    @Test
    fun successShowsSavedToPictures() {
        show(saveResult = true)
        compose.onNodeWithText("Saved to Pictures.").assertIsDisplayed()
    }

    @Test
    fun failureShowsTheFailureCopy() {
        show(saveResult = false)
        compose.onNodeWithText("Couldn't save the image. Please try again.").assertIsDisplayed()
    }

    @Test
    fun noResultShowsNoSnackbar() {
        show(saveResult = null)
        compose.onNodeWithText("Saved to Pictures.").assertDoesNotExist()
    }
}
