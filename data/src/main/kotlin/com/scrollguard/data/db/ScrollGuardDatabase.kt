package com.scrollguard.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        RuleEntity::class,
        UsageSessionEntity::class,
        BlockEventEntity::class,
        DailyStatEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class ScrollGuardDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao
    abstract fun usageDao(): UsageDao
    abstract fun blockEventDao(): BlockEventDao
    abstract fun dailyStatDao(): DailyStatDao

    companion object {
        @Volatile
        private var instance: ScrollGuardDatabase? = null

        fun get(context: Context): ScrollGuardDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ScrollGuardDatabase::class.java,
                    "scrollguard.db",
                ).build().also { instance = it }
            }
    }
}
