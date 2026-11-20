package com.idraak.stepskyscraper.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface StepsDao {
    @Query("SELECT * FROM day_steps ORDER BY date ASC")
    fun observeAll(): Flow<List<DayStepsEntity>>

    @Query("SELECT * FROM day_steps WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<DayStepsEntity>>

    @Query("SELECT * FROM day_steps WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    suspend fun range(from: LocalDate, to: LocalDate): List<DayStepsEntity>

    @Query("SELECT date FROM day_steps WHERE date IN (:dates)")
    suspend fun existingDates(dates: List<LocalDate>): List<LocalDate>

    @Upsert
    suspend fun upsert(days: List<DayStepsEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(days: List<DayStepsEntity>)

    /**
     * Applies a sync batch: recent days are upserted, frozen days (before [frozenBefore]) are only inserted
     * when missing. Returns how many rows changed value.
     */
    @Transaction
    suspend fun applySync(days: List<DayStepsEntity>, frozenBefore: LocalDate): Int {
        val (frozen, recent) = days.partition { it.date.isBefore(frozenBefore) }
        val before = range(days.minOfOrNull { it.date } ?: frozenBefore, days.maxOfOrNull { it.date } ?: frozenBefore)
            .associate { it.date to it.steps }
        insertIfMissing(frozen)
        upsert(recent)
        return days.count { day ->
            val old = before[day.date]
            old == null || (!day.date.isBefore(frozenBefore) && old != day.steps)
        }
    }

    @Query("SELECT * FROM floor_events ORDER BY detected_at ASC")
    fun observeFloorEvents(): Flow<List<FloorEventEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFloorEvents(events: List<FloorEventEntity>)
}

@Dao
interface TowerDao {
    @Query("SELECT * FROM towers ORDER BY tower_index ASC")
    fun observeAll(): Flow<List<TowerEntity>>

    @Query("SELECT * FROM towers ORDER BY tower_index ASC")
    suspend fun all(): List<TowerEntity>

    @Upsert
    suspend fun upsert(tower: TowerEntity)

    @Query("UPDATE towers SET name = :name WHERE tower_index = :index")
    suspend fun rename(index: Int, name: String): Int

    @Query(
        "UPDATE towers SET facade_id = :facade, palette_id = :palette, crown_id = :crown, lights_id = :lights, " +
            "ground_id = :ground WHERE tower_index = :index",
    )
    suspend fun updateLook(index: Int, facade: String, palette: String, crown: String, lights: String, ground: String): Int

    @Query("UPDATE towers SET ceremony_seen = 1 WHERE tower_index = :index")
    suspend fun markCeremonySeen(index: Int): Int

    @Query("UPDATE towers SET rewarded_ad_claimed = 1 WHERE tower_index = :index")
    suspend fun markRewardedAdClaimed(index: Int): Int
}

@Dao
interface WalletDao {
    @Query("SELECT COALESCE(SUM(amount), 0) FROM wallet")
    fun observeBalance(): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM wallet WHERE `key` = :key)")
    suspend fun exists(key: String): Boolean

    /** Returns the row id, or -1 when the key already existed (idempotent award). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: WalletEntryEntity): Long

    @Query("SELECT * FROM wallet ORDER BY created_at ASC")
    suspend fun all(): List<WalletEntryEntity>
}

@Dao
interface UnlockDao {
    @Query("SELECT item_id FROM unlocks")
    fun observeIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(unlock: UnlockEntity)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    fun observeEarned(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<AchievementEntity>)

    @Query("SELECT * FROM counters")
    fun observeCounters(): Flow<List<CounterEntity>>

    @Query("INSERT OR IGNORE INTO counters(name, value) VALUES(:name, 0)")
    suspend fun ensureCounter(name: String)

    @Query("UPDATE counters SET value = value + 1 WHERE name = :name")
    suspend fun bump(name: String)

    @Transaction
    suspend fun increment(name: String) {
        ensureCounter(name)
        bump(name)
    }
}
