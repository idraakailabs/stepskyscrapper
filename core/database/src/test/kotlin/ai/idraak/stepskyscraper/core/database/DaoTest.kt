package ai.idraak.stepskyscraper.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DaoTest {
    private lateinit var db: StepSkyscraperDatabase
    private val today = LocalDate.of(2026, 11, 20)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), StepSkyscraperDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    private fun day(date: LocalDate, steps: Int) = DayStepsEntity(date, steps, "HealthConnect", 0)

    @Test
    fun syncIsIdempotentAndFreezesOldDays() = runTest {
        val dao = db.stepsDao()
        val frozenBefore = today.minusDays(7)
        val first = dao.applySync(listOf(day(today.minusDays(10), 5_000), day(today, 3_000)), frozenBefore)
        assertThat(first).isEqualTo(2)
        // Same batch again: nothing changes.
        assertThat(dao.applySync(listOf(day(today.minusDays(10), 5_000), day(today, 3_000)), frozenBefore)).isEqualTo(0)
        // Old day is frozen, recent day updates.
        dao.applySync(listOf(day(today.minusDays(10), 9_999), day(today, 4_100)), frozenBefore)
        val all = dao.observeAll().first().associate { it.date to it.steps }
        assertThat(all[today.minusDays(10)]).isEqualTo(5_000)
        assertThat(all[today]).isEqualTo(4_100)
    }

    @Test
    fun walletEntriesAreKeyedAndSummed() = runTest {
        val dao = db.walletDao()
        assertThat(dao.insert(WalletEntryEntity("tower:1", 120, "ToppingOut", 1))).isGreaterThan(0L)
        assertThat(dao.insert(WalletEntryEntity("tower:1", 120, "ToppingOut", 2))).isEqualTo(-1L)
        dao.insert(WalletEntryEntity("spend:x", -40, "Purchase", 3))
        assertThat(dao.observeBalance().first()).isEqualTo(80)
        assertThat(dao.exists("tower:1")).isTrue()
    }

    @Test
    fun towersAndCounters() = runTest {
        val towers = db.towerDao()
        towers.upsert(
            TowerEntity(1, "Marlow Spire", "facade_deco", "palette_native", "crown_native", "lights_warm", "ground_plaza", 10, today, false, false),
        )
        towers.rename(1, "Calder Glass")
        towers.markCeremonySeen(1)
        val t = towers.all().single()
        assertThat(t.name).isEqualTo("Calder Glass")
        assertThat(t.ceremonySeen).isTrue()
        val ach = db.achievementDao()
        ach.increment("shares")
        ach.increment("shares")
        assertThat(ach.observeCounters().first().single().value).isEqualTo(2)
    }

    @Test
    fun floorEventsAreUniquePerDayAndFloor() = runTest {
        val dao = db.stepsDao()
        dao.insertFloorEvents(listOf(FloorEventEntity(date = today, floorNumber = 1, detectedAt = 1)))
        dao.insertFloorEvents(listOf(FloorEventEntity(date = today, floorNumber = 1, detectedAt = 2)))
        assertThat(dao.observeFloorEvents().first()).hasSize(1)
    }
}
