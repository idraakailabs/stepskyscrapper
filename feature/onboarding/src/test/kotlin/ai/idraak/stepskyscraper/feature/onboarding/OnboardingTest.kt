package ai.idraak.stepskyscraper.feature.onboarding

import ai.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import ai.idraak.stepskyscraper.core.domain.model.FacadeKit
import ai.idraak.stepskyscraper.core.domain.model.StepSourceStatus
import ai.idraak.stepskyscraper.core.domain.model.TargetPreset
import ai.idraak.stepskyscraper.core.domain.usecase.GenerateTowerNameUseCase
import ai.idraak.stepskyscraper.core.domain.usecase.SuggestDailyTargetUseCase
import ai.idraak.stepskyscraper.core.testing.MainDispatcherRule
import ai.idraak.stepskyscraper.core.testing.TestGraph
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
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
class OnboardingTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val main = MainDispatcherRule()

    private fun viewModel(g: TestGraph) = OnboardingViewModel(
        g.source, g.sync, g.steps, g.settings, g.startBuilding, SuggestDailyTargetUseCase(), GenerateTowerNameUseCase(), g.scheduler, g.analytics, g.clock,
    )

    @Test
    fun reducerBackAndKitSelection() {
        val s = OnboardingUiState(step = OnboardingStep.Target)
        assertThat(OnboardingReducer.back(s).step).isEqualTo(OnboardingStep.Style)
        val picked = OnboardingReducer.selectKit(s, FacadeKit.Brick)
        assertThat(picked.kit).isEqualTo(FacadeKit.Brick)
        assertThat(picked.restyleKey).isEqualTo(1)
        assertThat(OnboardingReducer.selectKit(picked, FacadeKit.Brick).restyleKey).isEqualTo(1)
        assertThat(OnboardingReducer.accessFrom(StepSourceStatus.HealthConnectNotInstalled)).isEqualTo(AccessState.NotInstalled)
    }

    @Test
    fun payoffPartialFloorFollowsStepsIntoTheFloor() {
        // BUG-006: no partial floor at 0 steps into the floor; 4,200 steps → 20%.
        assertThat(OnboardingUiState(todaySteps = 0).floorProgress).isEqualTo(0f)
        assertThat(OnboardingUiState(todaySteps = 3_000).floorProgress).isEqualTo(0f)
        assertThat(OnboardingUiState(todaySteps = 4_200).floorProgress).isWithin(1e-6f).of(0.2f)
        assertThat(OnboardingUiState(todaySteps = 4_250).floorProgress).isWithin(1e-6f).of(0.25f)
    }

    @Test
    fun fullFlowBuildsFirstTowerFromTodaysSteps() = runTest {
        val g = TestGraph()
        g.source.status = StepSourceStatus.HealthConnectPermissionMissing
        g.source.totals[g.today] = 4_200
        (1L..7L).forEach { g.source.totals[g.today.minusDays(it)] = 6_400 }
        val vm = viewModel(g)
        vm.onIntent(OnboardingIntent.Start)
        assertThat(vm.state.value.step).isEqualTo(OnboardingStep.Permission)
        g.source.status = StepSourceStatus.HealthConnectReady
        vm.onIntent(OnboardingIntent.AccessResult(granted = true))
        assertThat(vm.state.value.step).isEqualTo(OnboardingStep.Payoff)
        assertThat(vm.state.value.todaySteps).isEqualTo(4_200)
        assertThat(vm.state.value.floorsToday).isEqualTo(4)
        assertThat(vm.state.value.stepsToNext).isEqualTo(800)
        vm.onIntent(OnboardingIntent.ContinueFromPayoff)
        vm.onIntent(OnboardingIntent.SelectKit(FacadeKit.Glass))
        vm.onIntent(OnboardingIntent.NameChanged("Calder Glass"))
        vm.onIntent(OnboardingIntent.ConfirmStyle)
        assertThat(vm.state.value.sevenDayAverage).isEqualTo(6_400)
        assertThat(vm.state.value.suggested).isEqualTo(TargetPreset.Steady)
        assertThat(vm.state.value.weeklyFloors).isEqualTo(49)
        vm.onIntent(OnboardingIntent.ConfirmTarget)
        assertThat(vm.state.value.step).isEqualTo(OnboardingStep.Widget)
        val settings = g.settings.state.value
        assertThat(settings.onboardingComplete).isTrue()
        assertThat(settings.goals.dailyTargetSteps).isEqualTo(7_000)
        val city = g.progress().first().city
        assertThat(city.activeTower.name).isEqualTo("Calder Glass")
        assertThat(city.totalFloors).isEqualTo(4)
        assertThat(g.scheduler.periodic).isEqualTo(1)
    }

    @Test
    fun notNowSkipsToStyleAndRemembers() = runTest {
        val g = TestGraph()
        val vm = viewModel(g)
        vm.onIntent(OnboardingIntent.Start)
        vm.onIntent(OnboardingIntent.NotNow)
        assertThat(vm.state.value.step).isEqualTo(OnboardingStep.Style)
        assertThat(g.settings.state.value.stepsConnectSkipped).isTrue()
    }

    @Test
    fun promiseScreenStartsTheFlow() {
        val intents = mutableListOf<OnboardingIntent>()
        compose.setContent { SsTheme(reduceMotion = true) { OnboardingScreen(OnboardingUiState()) { intents += it } } }
        compose.onNodeWithText("Every 1,000 steps builds a floor").assertIsDisplayed()
        compose.onNodeWithText("Start building").performClick()
        assertThat(intents).containsExactly(OnboardingIntent.Start)
        compose.onNode(isRoot()).captureRoboImage("src/test/screenshots/onboarding_1a_promise.png")
    }

    @Test
    fun permissionScreenOffersAllowAndNotNow() {
        val intents = mutableListOf<OnboardingIntent>()
        compose.setContent {
            SsTheme(reduceMotion = true) {
                OnboardingScreen(OnboardingUiState(step = OnboardingStep.Permission)) {
                    intents +=
                        it
                }
            }
        }
        compose.onNodeWithText("Your steps are the bricks").assertIsDisplayed()
        compose.onNodeWithText("Allow step access").performClick()
        compose.onNodeWithText("Not now").performClick()
        assertThat(intents).containsExactly(OnboardingIntent.AllowAccess, OnboardingIntent.NotNow).inOrder()
        compose.onNode(isRoot()).captureRoboImage("src/test/screenshots/onboarding_1b_permission.png")
    }

    @Test
    fun targetScreenCtaFollowsSelection() {
        compose.setContent {
            SsTheme(reduceMotion = true) {
                OnboardingScreen(
                    OnboardingUiState(step = OnboardingStep.Target, sevenDayAverage = 6_400, selected = TargetPreset.Ambitious),
                ) {}
            }
        }
        compose.onNodeWithText("Set 70 floors a week").assertIsDisplayed()
        compose.onNodeWithText("Suggested").assertIsDisplayed()
        compose.onNode(isRoot()).captureRoboImage("src/test/screenshots/onboarding_1e_target.png")
    }

    @Test
    fun payoffScreenshot() {
        compose.setContent {
            SsTheme(reduceMotion = true) { OnboardingScreen(OnboardingUiState(step = OnboardingStep.Payoff, todaySteps = 4_200)) {} }
        }
        compose.onNodeWithText("That's 4 floors. You're already building.").assertIsDisplayed()
        compose.onNode(isRoot()).captureRoboImage("src/test/screenshots/onboarding_1c_payoff.png")
    }
}
