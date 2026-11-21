package ai.idraak.stepskyscraper.feature.paywall

import ai.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import ai.idraak.stepskyscraper.core.domain.model.PassPlan
import ai.idraak.stepskyscraper.core.domain.model.PlanOffer
import ai.idraak.stepskyscraper.core.ui.platform.LocalPlatformActions
import ai.idraak.stepskyscraper.core.ui.platform.PlatformActions
import ai.idraak.stepskyscraper.core.ui.platform.PreviewPlatformActions
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The paywall links the hosted terms and privacy policy, and hides a link whose URL isn't configured. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class PaywallLegalLinksTest {
    @get:Rule val compose = createComposeRule()

    private val offers = listOf(PlanOffer(PassPlan.Monthly, "$2.99", 2_990_000, "USD", 0))

    private class Recorder(override val hasTermsOfService: Boolean, override val hasPrivacyPolicy: Boolean) :
        PlatformActions by PreviewPlatformActions {
        val opened = mutableListOf<String>()

        override fun openTermsOfService() {
            opened += "terms"
        }

        override fun openPrivacyPolicy() {
            opened += "privacy"
        }
    }

    private fun show(platform: Recorder) = compose.setContent {
        CompositionLocalProvider(LocalPlatformActions provides platform) {
            SsTheme(reduceMotion = true) {
                PaywallScreen(PaywallUiState(offers, false, selected = PassPlan.Monthly, today = LocalDate.of(2026, 11, 20)), {}, {})
            }
        }
    }

    @Test
    fun linksOpenTheHostedPages() {
        val platform = Recorder(hasTermsOfService = true, hasPrivacyPolicy = true)
        show(platform)
        compose.onNodeWithText("Terms of service").assertIsDisplayed().performClick()
        compose.onNodeWithText("Privacy policy").assertIsDisplayed().performClick()
        assertThat(platform.opened).containsExactly("terms", "privacy").inOrder()
    }

    @Test
    fun unconfiguredLinksAreHidden() {
        show(Recorder(hasTermsOfService = false, hasPrivacyPolicy = false))
        compose.onNodeWithText("Terms of service").assertDoesNotExist()
        compose.onNodeWithText("Privacy policy").assertDoesNotExist()
    }
}
