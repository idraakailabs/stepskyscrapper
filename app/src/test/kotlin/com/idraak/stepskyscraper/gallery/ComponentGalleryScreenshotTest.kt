package com.idraak.stepskyscraper.gallery

import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.github.takahirom.roborazzi.captureRoboImage
import com.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Screenshot of the whole component gallery, light and dark, for side-by-side review with the design board. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = android.app.Application::class, qualifiers = "w400dp-h2700dp-xhdpi")
class ComponentGalleryScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun galleryLight() {
        compose.setContent { SsTheme(darkTheme = false, reduceMotion = true) { ComponentGallery {} } }
        compose.onNodeWithText("Buttons").assertExists()
        compose.onNode(isRoot()).captureRoboImage("src/test/screenshots/gallery_light.png")
    }

    @Test
    fun galleryDark() {
        compose.setContent { SsTheme(darkTheme = true, reduceMotion = true) { ComponentGallery {} } }
        compose.onNode(isRoot()).captureRoboImage("src/test/screenshots/gallery_dark.png")
    }
}
