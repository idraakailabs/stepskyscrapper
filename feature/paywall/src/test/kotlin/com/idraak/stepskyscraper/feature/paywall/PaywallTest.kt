package com.idraak.stepskyscraper.feature.paywall

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
import com.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import com.idraak.stepskyscraper.core.domain.model.Entitlement
import com.idraak.stepskyscraper.core.domain.model.PassPlan
import com.idraak.stepskyscraper.core.domain.model.PlanOffer
import com.idraak.stepskyscraper.core.testing.FakeAnalyticsTracker
import com.idraak.stepskyscraper.core.testing.FakeEntitlementRepository
import com.idraak.stepskyscraper.core.testing.MainDispatcherRule
import com.idraak.stepskyscraper.core.testing.TestClock
import com.idraak.stepskyscraper.core.testing.TestHost
import java.time.LocalDate
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
class PaywallTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val main = MainDispatcherRule()

    private val offers = listOf(
        PlanOffer(PassPlan.Annual, "$14.99", 14_990_000, "USD", 7),
        PlanOffer(PassPlan.Monthly, "$2.99", 2_990_000, "USD", 0),
        PlanOffer(PassPlan.Lifetime, "$29.99", 29_990_000, "USD", 0),
    )

    @Test
    fun savingIsAnnualVersusTwelveMonths() {
        assertThat(PaywallReducer.savingPercent(offers[1], offers[0])).isEqualTo(58)
        assertThat(PaywallReducer.savingPercent(null, offers[0])).isNull()
    }

    @Test
    fun purchaseUnlocksAndTracksAnonymously() = runTest {
        val repo = FakeEntitlementRepository()
        val analytics = FakeAnalyticsTracker()
        val vm = PaywallViewModel(repo, analytics, TestClock())
        vm.onIntent(PaywallIntent.Select(PassPlan.Lifetime))
        vm.onIntent(PaywallIntent.Purchase(TestHost))
        assertThat(repo.state.value).isEqualTo(Entitlement(true, PassPlan.Lifetime, null))
        assertThat(analytics.events.map { it.name }).containsAtLeast("paywall_shown", "purchase_started", "purchase_completed")
        assertThat(analytics.events.flatMap { it.params.values }.none { it.any(Char::isDigit) }).isTrue()
    }

    @Test
    fun trialCopyAndScreenshot() {
        compose.setContent {
            SsTheme(reduceMotion = true) { PaywallScreen(PaywallUiState(offers, false, today = LocalDate.of(2026, 11, 20)), {}, {}) }
        }
        compose.onNodeWithText("Start 7-day free trial").assertIsDisplayed()
        compose.onNodeWithText("Save 58%").assertIsDisplayed()
        compose.onNode(isRoot()).captureRoboImage("src/test/screenshots/paywall_9a.png")
    }
}
