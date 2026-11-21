package ai.idraak.stepskyscraper.feature.skyline

import ai.idraak.stepskyscraper.core.domain.model.AdAvailability
import ai.idraak.stepskyscraper.core.testing.MainDispatcherRule
import ai.idraak.stepskyscraper.core.testing.TestGraph
import ai.idraak.stepskyscraper.core.ui.mvi.Lce
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The rewarded-ad offer follows the ad gateway: hidden while ads are disabled, never a dead button. */
@RunWith(RobolectricTestRunner::class) // toRoute() in the ViewModel (BUG-005) needs Android Bundles
class SkylineRewardGatingTest {
    @get:Rule val main = MainDispatcherRule()

    private suspend fun graphWithNewTower(ads: AdAvailability): TestGraph = TestGraph().apply {
        onboarded(daysAgo = 2, target = 10)
        steps.set(today.minusDays(1) to 7_000, today to 5_000)
        ensureTowers()
        this.ads.state.value = ads
    }

    private fun reward(g: TestGraph): RewardCard? {
        val vm = SkylineViewModel(SavedStateHandle(), g.progress, g.towers, g.ads, g.claimBonus, g.wallet, g.analytics, g.clock)
        val content = (vm.state.value.content as Lce.Content).value
        vm.viewModelScope.cancel()
        return content.reward
    }

    @Test
    fun `ads disabled hides the reward offer entirely`() = runTest {
        assertThat(reward(graphWithNewTower(AdAvailability.Disabled))).isNull()
    }

    @Test
    fun `ads enabled offers the reward with a tappable button`() = runTest {
        val ready = reward(graphWithNewTower(AdAvailability.Ready))
        assertThat(ready).isNotNull()
        assertThat(ready!!.adAvailable).isTrue()
    }

    @Test
    fun `before preload the button stays tappable so the tap can gather consent`() = runTest {
        assertThat(reward(graphWithNewTower(AdAvailability.Unavailable))!!.adAvailable).isTrue()
        assertThat(reward(graphWithNewTower(AdAvailability.ConsentRequired))!!.adAvailable).isTrue()
        assertThat(reward(graphWithNewTower(AdAvailability.Loading))!!.adAvailable).isFalse()
    }
}
