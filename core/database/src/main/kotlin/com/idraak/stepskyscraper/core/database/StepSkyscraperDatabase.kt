package com.idraak.stepskyscraper.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import javax.inject.Singleton

class Converters {
    @TypeConverter
    fun fromDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toDate(value: String?): LocalDate? = value?.let(LocalDate::parse)
}

@Database(
    entities = [
        DayStepsEntity::class,
        FloorEventEntity::class,
        TowerEntity::class,
        WalletEntryEntity::class,
        UnlockEntity::class,
        AchievementEntity::class,
        CounterEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class StepSkyscraperDatabase : RoomDatabase() {
    abstract fun stepsDao(): StepsDao

    abstract fun towerDao(): TowerDao

    abstract fun walletDao(): WalletDao

    abstract fun unlockDao(): UnlockDao

    abstract fun achievementDao(): AchievementDao

    companion object {
        /** Included in Auto Backup (see app/src/main/res/xml/backup_rules.xml). */
        const val NAME = "step_skyscraper.db"
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): StepSkyscraperDatabase =
        Room.databaseBuilder(context, StepSkyscraperDatabase::class.java, StepSkyscraperDatabase.NAME).build()

    @Provides fun stepsDao(db: StepSkyscraperDatabase) = db.stepsDao()

    @Provides fun towerDao(db: StepSkyscraperDatabase) = db.towerDao()

    @Provides fun walletDao(db: StepSkyscraperDatabase) = db.walletDao()

    @Provides fun unlockDao(db: StepSkyscraperDatabase) = db.unlockDao()

    @Provides fun achievementDao(db: StepSkyscraperDatabase) = db.achievementDao()
}
