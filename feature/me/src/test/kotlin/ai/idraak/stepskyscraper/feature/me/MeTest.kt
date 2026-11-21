package ai.idraak.stepskyscraper.feature.me

import ai.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import ai.idraak.stepskyscraper.core.domain.model.GoalPause
import ai.idraak.stepskyscraper.core.domain.model.StepSourceStatus
import ai.idraak.stepskyscraper.core.domain.model.UiHost
import ai.idraak.stepskyscraper.core.domain.usecase.PauseOption
import ai.idraak.stepskyscraper.core.testing.FakeAdPrivacyChoices
import ai.idraak.stepskyscraper.core.testing.MainDispatcherRule
import ai.idraak.stepskyscraper.core.testing.TestGraph
import ai.idraak.stepskyscraper.core.ui.mvi.Lce
import ai.idraak.stepskyscraper.core.ui.platform.LocalPlatformActions
import ai.idraak.stepskyscraper.core.ui.platform.PlatformActions
import ai.idraak.stepskyscraper.core.ui.platform.PreviewPlatformActions
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
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
    fun adPrivacyChoicesFollowTheUmpRequirement() = runTest {
        val host = object : UiHost {}
        val hidden = PrivacyViewModel(FakeAdPrivacyChoices(required = false))
        hidden.onIntent(PrivacyIntent.Check(host))
        assertThat(hidden.state.value.adPrivacyChoices).isFalse()

        val ads = FakeAdPrivacyChoices(required = true)
        val vm = PrivacyViewModel(ads)
        vm.onIntent(PrivacyIntent.Check(host))
        assertThat(vm.state.value.adPrivacyChoices).isTrue()
        vm.onIntent(PrivacyIntent.OpenAdPrivacyChoices(host))
        assertThat(ads.shows).isEqualTo(1)
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
    fun notificationsRowReadsOffWhenTheOsBlocksThem() {
        // BUG-001: the stored preference is "on", but without POST_NOTIFICATIONS the row must say "Off".
        val blocked = object : PlatformActions by PreviewPlatformActions {
            override fun notificationsAllowed() = false
        }
        compose.setContent {
            CompositionLocalProvider(LocalPlatformActions provides blocked) {
                SsTheme(reduceMotion = true) { MeScreen(MeUiState(Lce.Content(previewMe())), MeNavigation({}, {}, {}, {}, {}, {})) {} }
            }
        }
        compose.onNodeWithText("Topping out and weekly summary only").assertDoesNotExist()
        compose.onNodeWithText("Notifications").performScrollTo().assertIsDisplayed()
        assertThat(compose.onAllNodesWithText("Off").fetchSemanticsNodes()).isNotEmpty()
    }

    @Test
    fun notificationsRowReadsOnWhenAllowed() {
        compose.setContent {
            SsTheme(reduceMotion = true) { MeScreen(MeUiState(Lce.Content(previewMe())), MeNavigation({}, {}, {}, {}, {}, {})) {} }
        }
        compose.onNodeWithText("Topping out and weekly summary only").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun helpAndPrivacyLinksTheHostedTerms() {
        var opened = false
        val platform = object : PlatformActions by PreviewPlatformActions {
            override fun openTermsOfService() {
                opened = true
            }
        }
        compose.setContent {
            CompositionLocalProvider(LocalPlatformActions provides platform) {
                SsTheme(reduceMotion = true) { PrivacyScreen(PrivacyUiState(), {}, null) {} }
            }
        }
        compose.onNodeWithText("Read the full privacy policy").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Terms of service").performScrollTo().assertIsDisplayed().performClick()
        assertThat(opened).isTrue()
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
