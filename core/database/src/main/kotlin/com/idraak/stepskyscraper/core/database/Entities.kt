package com.idraak.stepskyscraper.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/** One local day's step total. Totals (not deltas) make every sync idempotent. */
@Entity(tableName = "day_steps")
data class DayStepsEntity(
    @PrimaryKey val date: LocalDate,
    val steps: Int,
    val source: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/** A floor completion observed during a sync, with the time it was seen. */
@Entity(tableName = "floor_events", indices = [Index(value = ["date", "floor_number"], unique = true)])
data class FloorEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    @ColumnInfo(name = "floor_number") val floorNumber: Int,
    @ColumnInfo(name = "detected_at") val detectedAt: Long,
)

@Entity(tableName = "towers")
data class TowerEntity(
    @PrimaryKey @ColumnInfo(name = "tower_index") val index: Int,
    val name: String,
    @ColumnInfo(name = "facade_id") val facadeId: String,
    @ColumnInfo(name = "palette_id") val paletteId: String,
    @ColumnInfo(name = "crown_id") val crownId: String,
    @ColumnInfo(name = "lights_id") val lightsId: String,
    @ColumnInfo(name = "ground_id") val groundId: String,
    @ColumnInfo(name = "target_floors") val targetFloors: Int,
    @ColumnInfo(name = "founded_on") val foundedOn: LocalDate,
    @ColumnInfo(name = "ceremony_seen") val ceremonySeen: Boolean,
    @ColumnInfo(name = "rewarded_ad_claimed") val rewardedAdClaimed: Boolean,
)

/** Blueprint ledger entry; the key makes awards idempotent ("tower:5", "ach:century"). */
@Entity(tableName = "wallet")
data class WalletEntryEntity(
    @PrimaryKey val key: String,
    val amount: Int,
    val reason: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(tableName = "unlocks")
data class UnlockEntity(
    @PrimaryKey @ColumnInfo(name = "item_id") val itemId: String,
    @ColumnInfo(name = "purchased_at") val purchasedAt: Long,
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "earned_on") val earnedOn: LocalDate,
)

@Entity(tableName = "counters")
data class CounterEntity(
    @PrimaryKey val name: String,
    val value: Int,
)
