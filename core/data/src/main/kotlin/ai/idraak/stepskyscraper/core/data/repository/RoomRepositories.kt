package ai.idraak.stepskyscraper.core.data.repository

import ai.idraak.stepskyscraper.core.database.AchievementDao
import ai.idraak.stepskyscraper.core.database.AchievementEntity
import ai.idraak.stepskyscraper.core.database.DayStepsEntity
import ai.idraak.stepskyscraper.core.database.FloorEventEntity
import ai.idraak.stepskyscraper.core.database.StepsDao
import ai.idraak.stepskyscraper.core.database.TowerDao
import ai.idraak.stepskyscraper.core.database.TowerEntity
import ai.idraak.stepskyscraper.core.database.UnlockDao
import ai.idraak.stepskyscraper.core.database.UnlockEntity
import ai.idraak.stepskyscraper.core.database.WalletDao
import ai.idraak.stepskyscraper.core.database.WalletEntryEntity
import ai.idraak.stepskyscraper.core.domain.AppClock
import ai.idraak.stepskyscraper.core.domain.model.DaySteps
import ai.idraak.stepskyscraper.core.domain.model.DomainError
import ai.idraak.stepskyscraper.core.domain.model.FloorEvent
import ai.idraak.stepskyscraper.core.domain.model.Outcome
import ai.idraak.stepskyscraper.core.domain.model.StepSourceKind
import ai.idraak.stepskyscraper.core.domain.model.TowerLook
import ai.idraak.stepskyscraper.core.domain.model.TowerRecord
import ai.idraak.stepskyscraper.core.domain.model.WalletEntry
import ai.idraak.stepskyscraper.core.domain.repository.AchievementRepository
import ai.idraak.stepskyscraper.core.domain.repository.StepsRepository
import ai.idraak.stepskyscraper.core.domain.repository.TowerRepository
import ai.idraak.stepskyscraper.core.domain.repository.UnlockRepository
import ai.idraak.stepskyscraper.core.domain.repository.WalletRepository
import android.database.sqlite.SQLiteException
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Runs a database call and converts SQLite failures into a typed [DomainError.Storage]. */
internal suspend fun <T> storage(block: suspend () -> T): Outcome<T> = try {
    Outcome.Success(block())
} catch (e: SQLiteException) {
    Outcome.Failure(DomainError.Storage(e.message ?: "database"))
} catch (e: IllegalStateException) {
    Outcome.Failure(DomainError.Storage(e.message ?: "database"))
}

private fun DayStepsEntity.toDomain() = DaySteps(
    date,
    steps,
    runCatching {
        StepSourceKind.valueOf(source)
    }.getOrDefault(StepSourceKind.HealthConnect),
)

@Singleton
class RoomStepsRepository @Inject constructor(
    private val dao: StepsDao,
    private val clock: AppClock,
) : StepsRepository {
    override fun observeDays(from: LocalDate, to: LocalDate): Flow<List<DaySteps>> =
        dao.observeRange(from, to).map { list -> list.map { it.toDomain() } }

    override fun observeAllDays(): Flow<List<DaySteps>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun days(from: LocalDate, to: LocalDate): Outcome<List<DaySteps>> = storage {
        dao.range(
            from,
            to,
        ).map { it.toDomain() }
    }

    override suspend fun upsertDays(days: List<DaySteps>, frozenBefore: LocalDate): Outcome<Int> = storage {
        val now = clock.now().toEpochMilli()
        dao.applySync(days.map { DayStepsEntity(it.date, it.steps, it.source.name, now) }, frozenBefore)
    }

    override fun observeFloorEvents(): Flow<List<FloorEvent>> = dao.observeFloorEvents().map { list ->
        list.map { FloorEvent(it.date, it.floorNumber, Instant.ofEpochMilli(it.detectedAt)) }
    }

    override suspend fun recordFloorEvents(events: List<FloorEvent>): Outcome<Unit> = storage {
        dao.insertFloorEvents(
            events.map {
                FloorEventEntity(date = it.date, floorNumber = it.floorNumberOfDay, detectedAt = it.detectedAt.toEpochMilli())
            },
        )
    }
}

private fun TowerEntity.toDomain() = TowerRecord(
    index = index,
    name = name,
    look = TowerLook(facadeId, paletteId, crownId, lightsId, groundId),
    targetFloors = targetFloors,
    foundedOn = foundedOn,
    ceremonySeen = ceremonySeen,
    rewardedAdClaimed = rewardedAdClaimed,
)

private fun TowerRecord.toEntity() = TowerEntity(
    index, name, look.facadeId, look.paletteId, look.crownId, look.lightsId, look.groundId, targetFloors, foundedOn, ceremonySeen, rewardedAdClaimed,
)

@Singleton
class RoomTowerRepository @Inject constructor(private val dao: TowerDao) : TowerRepository {
    override fun observeTowerRecords(): Flow<List<TowerRecord>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun towerRecords(): Outcome<List<TowerRecord>> = storage { dao.all().map { it.toDomain() } }

    override suspend fun upsert(record: TowerRecord): Outcome<Unit> = storage { dao.upsert(record.toEntity()) }

    override suspend fun rename(index: Int, name: String): Outcome<Unit> = storage { dao.rename(index, name.trim()) }.map { }

    override suspend fun updateLook(index: Int, look: TowerLook): Outcome<Unit> =
        storage { dao.updateLook(index, look.facadeId, look.paletteId, look.crownId, look.lightsId, look.groundId) }.map { }

    override suspend fun markCeremonySeen(index: Int): Outcome<Unit> = storage { dao.markCeremonySeen(index) }.map { }

    override suspend fun markRewardedAdClaimed(index: Int): Outcome<Unit> = storage { dao.markRewardedAdClaimed(index) }.map { }

    private inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
        is Outcome.Success -> Outcome.Success(transform(value))
        is Outcome.Failure -> this
    }
}

@Singleton
class RoomWalletRepository @Inject constructor(private val dao: WalletDao) : WalletRepository {
    override val balance: Flow<Int> = dao.observeBalance()

    override suspend fun hasEntry(key: String): Boolean = dao.exists(key)

    override suspend fun addEntry(entry: WalletEntry): Outcome<Boolean> = storage {
        dao.insert(WalletEntryEntity(entry.key, entry.amount, entry.reason.name, entry.createdAt.toEpochMilli())) > 0
    }
}

@Singleton
class RoomUnlockRepository @Inject constructor(
    private val dao: UnlockDao,
    private val clock: AppClock,
) : UnlockRepository {
    override val purchasedItemIds: Flow<Set<String>> = dao.observeIds().map { it.toSet() }

    override suspend fun markPurchased(itemId: String): Outcome<Unit> = storage {
        dao.insert(
            UnlockEntity(itemId, clock.now().toEpochMilli()),
        )
    }
}

@Singleton
class RoomAchievementRepository @Inject constructor(private val dao: AchievementDao) : AchievementRepository {
    override val earned: Flow<Map<String, LocalDate>> = dao.observeEarned().map { list -> list.associate { it.id to it.earnedOn } }

    override suspend fun markEarned(ids: Map<String, LocalDate>): Outcome<Unit> =
        storage { dao.insertAll(ids.map { (id, date) -> AchievementEntity(id, date) }) }

    override val counters: Flow<Map<String, Int>> = dao.observeCounters().map { list -> list.associate { it.name to it.value } }

    override suspend fun increment(counter: String): Outcome<Unit> = storage { dao.increment(counter) }
}
