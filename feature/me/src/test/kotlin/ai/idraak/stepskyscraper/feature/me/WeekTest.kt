package ai.idraak.stepskyscraper.feature.me

import ai.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import ai.idraak.stepskyscraper.core.domain.model.GoalPause
import ai.idraak.stepskyscraper.core.domain.model.Goals
import ai.idraak.stepskyscraper.core.domain.model.TargetPreset
import ai.idraak.stepskyscraper.core.domain.model.WeekProgress
import ai.idraak.stepskyscraper.core.domain.usecase.PauseOption
import ai.idraak.stepskyscraper.core.testing.MainDispatcherRule
import ai.idraak.stepskyscraper.core.testing.TestGraph
import ai.idraak.stepskyscraper.core.ui.mvi.Lce
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

/** BUG-004: the Build week card opens a weekly detail with this week's bars and "Pause goals". */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class WeekTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val main = MainDispatcherRule()

    private fun content(pause: GoalPause = GoalPause.None): WeekContent {
        val me = previewMe()
        val week = WeekProgress(LocalDate.of(2026, 11, 16), me.weekFloors, me.weekGoal, 3, pause != GoalPause.None, me.week)
        return WeekContent(week, Goals(7_000, TargetPreset.Steady, pause))
    }

    @Test
    fun pausingFromTheWeeklyDetailPausesTheWeekShownOnBuild() = runTest {
        val g = TestGraph()
        g.onboarded()
        val vm = WeekViewModel(g.progress, g.pause, g.analytics)
        assertThat((vm.state.value.content as Lce.Content).value.week.paused).isFalse()

        vm.onIntent(WeekIntent.TogglePause)
        assertThat(vm.state.value.pauseSheet).isTrue()
        vm.onIntent(WeekIntent.Pause(PauseOption.OneWeek))

        assertThat(vm.state.value.pauseSheet).isFalse()
        assertThat(g.settings.state.value.goals.pause).isEqualTo(GoalPause.Until(g.today.plusDays(6)))
        // The Build week card reads the same WeekProgress; paused → "Goals paused. Floors still count."
        assertThat(g.progress().first().week.paused).isTrue()
        assertThat((vm.state.value.content as Lce.Content).value.week.paused).isTrue()

        vm.onIntent(WeekIntent.TogglePause) // switch on → resume
        assertThat(g.settings.state.value.goals.pause).isEqualTo(GoalPause.None)
    }

    @Test
    fun weeklyDetailShowsBarsAndPauseControl() {
        var intents = emptyList<WeekIntent>()
        compose.setContent {
            SsTheme(reduceMotion = true) { WeekScreen(WeekUiState(Lce.Content(content())), {}, {}) { intents = intents + it } }
        }
        compose.onNodeWithText("This week").assertIsDisplayed()
        compose.onNodeWithText("of 50 floors").assertIsDisplayed()
        // this week's seven bars (each bar exposes "<day>: <floors>")
        assertThat(compose.onAllNodes(hasContentDescription(": ", substring = true)).fetchSemanticsNodes()).hasSize(7)
        compose.onNodeWithText("Pause goals").assertIsDisplayed().performClick()
        assertThat(intents).containsExactly(WeekIntent.TogglePause)
        compose.onNode(isRoot()).captureRoboImage("src/test/screenshots/week_detail.png")
    }

    @Test
    fun pausedWeekSaysFloorsStillCount() {
        compose.setContent {
            SsTheme(reduceMotion = true) {
                WeekScreen(WeekUiState(Lce.Content(content(GoalPause.Indefinite(LocalDate.of(2026, 11, 18))))), {}, {}) {}
            }
        }
        compose.onNodeWithText("Goals paused. Floors still count.").assertIsDisplayed()
    }
}
