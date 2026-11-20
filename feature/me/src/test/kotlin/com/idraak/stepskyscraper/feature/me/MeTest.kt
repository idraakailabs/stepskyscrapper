package com.idraak.stepskyscraper.feature.me

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
import com.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import com.idraak.stepskyscraper.core.domain.model.GoalPause
import com.idraak.stepskyscraper.core.domain.model.StepSourceStatus
import com.idraak.stepskyscraper.core.domain.usecase.PauseOption
import com.idraak.stepskyscraper.core.testing.MainDispatcherRule
import com.idraak.stepskyscraper.core.testing.TestGraph
import com.idraak.stepskyscraper.core.ui.mvi.Lce
import java.time.Instant
import java.time.ZoneId
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
class MeTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val main = MainDispatcherRule()

    @Test
    fun sourceMapping() {
        assertThat(MeReducer.source(StepSourceStatus.HealthConnectReady)).isEqualTo(SourceKind.HealthConnect)
        assertThat(MeReducer.source(StepSourceStatus.Sensor(true))).isEqualTo(SourceKind.Sensor)
        assertThat(MeReducer.source(StepSourceStatus.HealthConnectPermissionMissing)).isEqualTo(SourceKind.Off)
    }

    @Test
    fun pauseAndResumeGoals() = runTest {
        val g = TestGraph()
        g.onboarded()
        val vm = MeViewModel(g.progress, g.syncStatus, g.entitlements, g.source, g.week, g.pause, g.analytics, g.clock)
        vm.onIntent(MeIntent.TogglePause)
        assertThat(vm.state.value.pauseSheet).isTrue()
        vm.onIntent(MeIntent.Pause(PauseOption.ThreeDays))
        assertThat(g.settings.state.value.goals.pause).isEqualTo(GoalPause.Until(g.today.plusDays(2)))
        vm.onIntent(MeIntent.TogglePause)
        assertThat(g.settings.state.value.goals.pause).isEqualTo(GoalPause.None)
        assertThat(g.achievements.counterState.value["pause_returns"]).isEqualTo(1)
    }

    @Test
    fun meScreenshotAndContent() {
        compose.setContent {
            SsTheme(reduceMotion = true) { MeScreen(MeUiState(Lce.Content(previewMe())), MeNavigation({}, {}, {}, {}, {}, {})) {} }
        }
        compose.onNodeWithText("Site Architect").assertIsDisplayed()
        compose.onNode(isRoot()).captureRoboImage("src/test/screenshots/me_6a.png")
    }

    @Test
    fun stepHelpScreenshot() {
        compose.setContent {
            SsTheme(reduceMotion = true) {
                StepHelpScreen(StepHelpUiState(lastData = Instant.parse("2026-11-18T21:40:00Z"), zone = ZoneId.of("UTC")), {}, {}, {})
            }
        }
        compose.onNodeWithText("Check Health Connect").assertIsDisplayed()
        compose.onNode(isRoot()).captureRoboImage("src/test/screenshots/help_10a.png")
    }
}
