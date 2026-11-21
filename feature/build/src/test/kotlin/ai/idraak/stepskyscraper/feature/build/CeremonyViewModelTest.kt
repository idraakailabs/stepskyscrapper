package ai.idraak.stepskyscraper.feature.build

import ai.idraak.stepskyscraper.core.testing.MainDispatcherRule
import ai.idraak.stepskyscraper.core.testing.TestGraph
import ai.idraak.stepskyscraper.feature.build.ceremony.CeremonyEffect
import ai.idraak.stepskyscraper.feature.build.ceremony.CeremonyIntent
import ai.idraak.stepskyscraper.feature.build.ceremony.CeremonyViewModel
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** BUG-001: the notification permission is asked once, when the first ceremony is dismissed. */
@RunWith(RobolectricTestRunner::class)
class CeremonyViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private suspend fun finishedTower(): TestGraph {
        val g = TestGraph()
        g.onboarded(daysAgo = 2, target = 10)
        g.steps.set(g.today.minusDays(1) to 7_000, g.today to 5_000)
        g.ensureTowers()
        return g
    }

    private fun ceremony(g: TestGraph) =
        CeremonyViewModel(SavedStateHandle(mapOf("towerIndex" to 1)), g.progress, g.completeTower, g.towers, g.settings, g.analytics)

    @Test
    fun firstDismissalAsksForNotificationsOnce() = runTest {
        val g = finishedTower()
        val first = ceremony(g)
        first.onIntent(CeremonyIntent.Close)
        val effect = first.effects.first() as CeremonyEffect.Close
        assertThat(effect.askNotifications).isTrue()
        assertThat(effect.showPaywall).isTrue()
        assertThat(g.settings.state.value.notificationPromptHandled).isTrue()

        val second = ceremony(g)
        second.onIntent(CeremonyIntent.StartNext)
        assertThat((second.effects.first() as CeremonyEffect.StartNext).askNotifications).isFalse()
    }

    @Test
    fun noPromptWhenTheUserTurnedNotificationsOff() = runTest {
        val g = finishedTower()
        g.settings.setNotifications(toppingOut = false, weeklySummary = false)
        val vm = ceremony(g)
        vm.onIntent(CeremonyIntent.Close)
        assertThat((vm.effects.first() as CeremonyEffect.Close).askNotifications).isFalse()
    }
}
