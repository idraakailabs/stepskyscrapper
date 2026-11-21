package ai.idraak.stepskyscraper.core.data

import ai.idraak.stepskyscraper.core.data.repository.RemoteConfigAdsSwitch
import ai.idraak.stepskyscraper.core.data.repository.RemoteConfigLadderRepository
import ai.idraak.stepskyscraper.core.data.repository.RemoteConfigSource
import ai.idraak.stepskyscraper.core.data.repository.RemoteValues
import ai.idraak.stepskyscraper.core.domain.model.Outcome
import ai.idraak.stepskyscraper.core.domain.usecase.BlueprintLadder
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** No google-services.json in tests, as in the repo: Remote Config is unavailable, so ads must stay off. */
@RunWith(RobolectricTestRunner::class)
class RemoteConfigAdsSwitchTest {
    private val source = RemoteConfigSource(ApplicationProvider.getApplicationContext())

    @Test
    fun `ads_enabled defaults to false`() {
        assertThat(RemoteConfigSource.KEY_ADS_ENABLED).isEqualTo("ads_enabled")
        assertThat(RemoteConfigSource.DEFAULT_ADS_ENABLED).isFalse()
        assertThat(RemoteValues.DEFAULT.adsEnabled).isFalse()
    }

    @Test
    fun `without Firebase the switch stays off and the ladder keeps its default`() = runTest {
        val switch = RemoteConfigAdsSwitch(source)
        val ladder = RemoteConfigLadderRepository(source)
        assertThat(switch.adsEnabled.first()).isFalse()
        assertThat(ladder.refresh()).isEqualTo(Outcome.Success(Unit))
        assertThat(switch.adsEnabled.first()).isFalse()
        assertThat(ladder.ladder.first()).isEqualTo(BlueprintLadder.DEFAULT)
    }
}
