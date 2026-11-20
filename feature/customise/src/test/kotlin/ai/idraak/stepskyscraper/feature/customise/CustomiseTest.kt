package ai.idraak.stepskyscraper.feature.customise

import ai.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import ai.idraak.stepskyscraper.core.domain.cosmetics.CosmeticCatalog
import ai.idraak.stepskyscraper.core.domain.cosmetics.CosmeticCategory
import ai.idraak.stepskyscraper.core.domain.model.FacadeKit
import ai.idraak.stepskyscraper.core.domain.usecase.CityCalculator
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class CustomiseTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun applyAndSelectionPerCategory() {
        val look = CityCalculator.defaultLook(FacadeKit.Deco)
        val slate = CosmeticCatalog.item("palette_slate")!!
        val applied = CustomiseReducer.apply(look, slate)
        assertThat(applied.paletteId).isEqualTo("palette_slate")
        assertThat(CustomiseReducer.isSelected(applied, slate)).isTrue()
        assertThat(CustomiseReducer.isSelected(look, slate)).isFalse()
        val glass = CosmeticCatalog.item("facade_glass")!!
        assertThat(CustomiseReducer.apply(look, glass).facadeId).isEqualTo("facade_glass")
    }

    @Test
    fun previewShowsLockedItemOverDraft() {
        val state = previewCustomise(CosmeticCategory.Facade)
        val twilight = CosmeticCatalog.item("facade_twilight_glass")!!
        assertThat(state.copy(previewing = twilight).shownLook?.facadeId).isEqualTo("facade_twilight_glass")
        assertThat(state.shownLook?.facadeId).isEqualTo("facade_deco")
    }

    @Test
    fun facadeTabShowsThreeLockTypes() {
        compose.setContent { SsTheme(reduceMotion = true) { CustomiseScreen(previewCustomise(CosmeticCategory.Facade), {}, {}) } }
        compose.onNodeWithText("Finish 10 towers").assertExists()
        compose.onNodeWithText("400 Blueprints").assertExists()
        compose.onAllNodesWithText("Architect Pass").onFirst().assertExists()
        compose.onNodeWithText("Done").assertIsDisplayed()
        compose.onNode(isRoot()).captureRoboImage("src/test/screenshots/customise_5a_facade.png")
    }

    @Test
    fun paletteTabScreenshot() {
        compose.setContent { SsTheme(reduceMotion = true) { CustomiseScreen(previewCustomise(CosmeticCategory.Palette), {}, {}) } }
        compose.onNodeWithText("Colour palette").assertIsDisplayed()
        compose.onNode(isRoot()).captureRoboImage("src/test/screenshots/customise_5b_palette.png")
    }
}
