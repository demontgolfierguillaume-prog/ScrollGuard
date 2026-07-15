package com.scrollguard.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        RuleEntity::class,
        AppSessionEntity::class,
        UsageSessionEntity::class,
        BlockEventEntity::class,
        DailyStatEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class ScrollGuardDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao
    abstract fun appSessionDao(): AppSessionDao
    abstract fun usageDao(): UsageDao
    abstract fun blockEventDao(): BlockEventDao
    abstract fun dailyStatDao(): DailyStatDao

    companion object {
        /** v2 : suivi du temps par application (table app_session). */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `app_session` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`packageName` TEXT NOT NULL, " +
                        "`startedAtEpochMs` INTEGER NOT NULL, " +
                        "`endedAtEpochMs` INTEGER, " +
                        "`durationSeconds` INTEGER)",
                )
            }
        }

        @Volatile
        private var instance: ScrollGuardDatabase? = null

        fun get(context: Context): ScrollGuardDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ScrollGuardDatabase::class.java,
                    "scrollguard.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
