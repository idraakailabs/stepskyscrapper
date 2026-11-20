package ai.idraak.stepskyscraper.feature.build

import com.google.common.truth.Truth.assertThat
import ai.idraak.stepskyscraper.core.domain.model.DayProgress
import ai.idraak.stepskyscraper.core.domain.model.DomainError
import ai.idraak.stepskyscraper.core.domain.model.StepSourceStatus
import ai.idraak.stepskyscraper.core.domain.model.WeekProgress
import ai.idraak.stepskyscraper.core.testing.MainDispatcherRule
import ai.idraak.stepskyscraper.core.testing.TestGraph
import ai.idraak.stepskyscraper.core.ui.mvi.Lce
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class BuildViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val day = LocalDate.of(2026, 11, 20)

    private fun week(floors: Int, goal: Int = 49, paused: Boolean = false) = WeekProgress(day.minusDays(4), floors, goal, 3, paused, emptyList())

    private fun today(steps: Int) = DayProgress(day, steps, steps / 1_000, steps % 1_000)

    @Test
    fun `week message prefers pause, then help, then target met, then rest`() {
        assertThat(BuildReducer.weekMessage(week(10, paused = true), today(0), false, 3, true)).isEqualTo(WeekMessage.Paused)
        assertThat(BuildReducer.weekMessage(week(10), today(0), false, 3, true)).isEqualTo(WeekMessage.NeedsHelp)
        assertThat(BuildReducer.weekMessage(week(43), today(8_420), true, 8, false)).isEqualTo(WeekMessage.Bonus)
        assertThat(BuildReducer.weekMessage(week(22), today(0), false, 13, false)).isEqualTo(WeekMessage.Rest)
    }

    @Test
    fun `nearest milestone wins when topping out is closer than the weekly goal`() {
        assertThat(BuildReducer.weekMessage(week(29), today(1_240), false, 4, false)).isEqualTo(WeekMessage.ToppingOut(4))
        assertThat(BuildReducer.weekMessage(week(35), today(6_660), false, 30, false)).isEqualTo(WeekMessage.ToGo(14, 3))
        assertThat(BuildReducer.weekMessage(week(50), today(6_660), false, 30, false)).isEqualTo(WeekMessage.Reached)
    }

    @Test
    fun `stale after ten minutes and help after 48 hours without data`() {
        val now = Instant.parse("2026-11-20T14:00:00Z")
        assertThat(BuildReducer.isStale(now.minusSeconds(9 * 60), now)).isFalse()
        assertThat(BuildReducer.isStale(now.minusSeconds(12 * 60), now)).isTrue()
        assertThat(BuildReducer.needsHelp(now.minusSeconds(49 * 3600), now, 0, null)).isTrue()
        assertThat(BuildReducer.needsHelp(now.minusSeconds(49 * 3600), now, 300, null)).isFalse()
        assertThat(BuildReducer.needsHelp(now, now, 0, DomainError.StepPermissionDenied)).isTrue()
    }

    @Test
    fun `floors drop only when the count grows after the first load`() {
        assertThat(BuildReducer.dropAfterUpdate(null, 14, 0)).isEqualTo(null to 0)
        assertThat(BuildReducer.dropAfterUpdate(12, 14, 0)).isEqualTo(12 to 1)
        assertThat(BuildReducer.dropAfterUpdate(14, 14, 1)).isEqualTo(null to 1)
    }

    @Test
    fun `access maps every source status`() {
        assertThat(BuildReducer.access(StepSourceStatus.HealthConnectReady)).isEqualTo(StepAccessUi.Connected)
        assertThat(BuildReducer.access(StepSourceStatus.HealthConnectNotInstalled)).isEqualTo(StepAccessUi.HcMissing)
        assertThat(BuildReducer.access(StepSourceStatus.HealthConnectNeedsUpdate)).isEqualTo(StepAccessUi.HcNeedsUpdate)
        assertThat(BuildReducer.access(StepSourceStatus.Sensor(false))).isEqualTo(StepAccessUi.NotConnected)
    }

    @Test
    fun `view model syncs on open, shows content and the synced snackbar`() = runTest {
        val g = TestGraph()
        g.onboarded()
        g.source.totals[g.today] = 6_660
        val vm = BuildViewModel(g.progress, g.sync, g.syncStatus, g.source, g.settings, g.clock)
        val content = (vm.state.value.content as Lce.Content).value
        assertThat(content.today.stepsToNextFloor).isEqualTo(340)
        assertThat(content.floorsBuilt).isEqualTo(6)
        assertThat(content.towerName).isEqualTo("Marlow Spire")
        assertThat(vm.state.value.snackbar?.floorsAdded).isEqualTo(6)
        vm.onIntent(BuildIntent.SnackbarDismissed)
        assertThat(vm.state.value.snackbar).isNull()
        vm.onIntent(BuildIntent.TowerTapped)
        assertThat(vm.state.value.zoomed).isTrue()
        // The minute ticker never ends on its own; stop it so virtual time can go idle.
        vm.viewModelScope.cancel()
    }
}

