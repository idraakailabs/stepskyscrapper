package ai.idraak.stepskyscraper.core.data

import ai.idraak.stepskyscraper.core.data.repository.RemoteConfigLadderRepository
import ai.idraak.stepskyscraper.core.domain.model.DomainError
import ai.idraak.stepskyscraper.core.domain.model.GoalPause
import ai.idraak.stepskyscraper.core.domain.model.Outcome
import ai.idraak.stepskyscraper.core.domain.model.StepSourceStatus
import ai.idraak.stepskyscraper.core.domain.usecase.BlueprintLadder
import ai.idraak.stepskyscraper.core.testing.TestGraph
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SyncStepsUseCaseTest {
    @Test
    fun firstSyncImportsThirtyDaysAndLaterSyncsSevenDays() = runTest {
        val g = TestGraph()
        g.settings.state.value = g.settings.state.value.copy(historyImported = false)
        g.source.totals[g.today.minusDays(29)] = 4_000
        g.sync()
        assertThat(g.steps.days.value).hasSize(30)
        assertThat(g.settings.state.value.historyImported).isTrue()
        g.source.totals[g.today.minusDays(29)] = 9_999
        g.sync()
        assertThat(g.steps.days.value[g.today.minusDays(29)]!!.steps).isEqualTo(4_000)
    }

    @Test
    fun syncIsIdempotentAndRecordsNewFloors() = runTest {
        val g = TestGraph()
        g.onboarded()
        g.source.totals[g.today] = 3_400
        val first = g.sync()
        assertThat((first as Outcome.Success).value.floorsAddedToday).isEqualTo(3)
        val second = g.sync()
        assertThat((second as Outcome.Success).value.floorsAddedToday).isEqualTo(0)
        assertThat(g.steps.events.value).hasSize(3)
        assertThat(g.progress().first().city.totalFloors).isEqualTo(3)
    }

    @Test
    fun newEmptyDaysAreNotNewData() = runTest {
        // BUG-012: a new day's 0-step row must not count as data, or the 48h step-help link never shows.
        val g = TestGraph()
        g.onboarded()
        g.source.totals[g.today] = 3_000
        g.sync()
        val firstChange = g.syncStatus.state.value.lastDataChangeAt
        assertThat(firstChange).isNotNull()
        g.clock.current = g.clock.current.plusDays(3)
        g.sync()
        assertThat(g.syncStatus.state.value.lastDataChangeAt).isEqualTo(firstChange)
        g.source.totals[g.clock.today()] = 500
        g.sync()
        assertThat(g.syncStatus.state.value.lastDataChangeAt).isEqualTo(g.clock.now())
    }

    @Test
    fun numbersNeverJumpBackwards() = runTest {
        val g = TestGraph()
        g.onboarded()
        g.source.totals[g.today] = 5_000
        g.sync()
        g.source.totals[g.today] = 4_200
        g.sync()
        assertThat(g.steps.days.value[g.today]!!.steps).isEqualTo(5_000)
    }

    @Test
    fun permissionProblemsAreTypedAndRecorded() = runTest {
        val g = TestGraph()
        g.source.status = StepSourceStatus.HealthConnectPermissionMissing
        val result = g.sync()
        assertThat(result).isEqualTo(Outcome.Failure(DomainError.StepPermissionDenied))
        assertThat(g.syncStatus.state.value.lastError).isEqualTo(DomainError.StepPermissionDenied)
        assertThat(g.source.reads).isEqualTo(0)
    }

    @Test
    fun toppingOutAwardsBlueprintsOnceAndCreatesNextTower() = runTest {
        val g = TestGraph()
        g.onboarded(target = 10)
        g.source.totals[g.today.minusDays(1)] = 7_000
        g.source.totals[g.today] = 5_300
        g.sync()
        g.sync()
        val snapshot = g.progress().first()
        assertThat(snapshot.city.finishedTowers).hasSize(1)
        // 10-floor tower = 50, plus "First Floor" (25) and "Topping Out" (50); awarded once despite two syncs.
        assertThat(snapshot.blueprints).isEqualTo(50 + 25 + 50)
        assertThat(g.towers.records.value.keys).contains(2)
        assertThat(snapshot.city.activeTower.floorsBuilt).isEqualTo(2)
    }

    @Test
    fun expiredPauseIsClearedAndCountsAsReturning() = runTest {
        val g = TestGraph()
        g.onboarded()
        g.settings.setPause(GoalPause.Until(g.today.minusDays(1)))
        g.sync()
        assertThat(g.settings.state.value.goals.pause).isEqualTo(GoalPause.None)
        assertThat(g.achievements.counterState.value["pause_returns"]).isEqualTo(1)
    }

    @Test
    fun remoteLadderParsingFallsBackToDefault() {
        assertThat(RemoteConfigLadderRepository.parse("12, 24,48")).containsExactly(12, 24, 48).inOrder()
        assertThat(RemoteConfigLadderRepository.parse("oops")).isEqualTo(BlueprintLadder.DEFAULT)
        assertThat(RemoteConfigLadderRepository.parse(null)).isEqualTo(BlueprintLadder.DEFAULT)
    }
}
