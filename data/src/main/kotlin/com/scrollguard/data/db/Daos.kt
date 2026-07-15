package com.scrollguard.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM rule WHERE featureId = :featureId AND enabled = 1")
    suspend fun rulesForFeature(featureId: String): List<RuleEntity>

    @Query("SELECT * FROM rule ORDER BY featureId")
    fun allRules(): Flow<List<RuleEntity>>

    @Insert
    suspend fun insert(rule: RuleEntity): Long

    @Query("DELETE FROM rule WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface UsageDao {
    @Insert
    suspend fun insert(session: UsageSessionEntity): Long

    @Query("UPDATE usage_session SET endedAtEpochMs = :endedAt, durationSeconds = :durationSeconds, endedBy = :endedBy WHERE id = :id")
    suspend fun close(id: Long, endedAt: Long, durationSeconds: Long, endedBy: String)

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM usage_session WHERE featureId = :featureId AND startedAtEpochMs >= :sinceEpochMs")
    suspend fun usedSecondsSince(featureId: String, sinceEpochMs: Long): Long

    @Query("DELETE FROM usage_session WHERE startedAtEpochMs < :beforeEpochMs")
    suspend fun purgeOlderThan(beforeEpochMs: Long)
}

@Dao
interface BlockEventDao {
    @Insert
    suspend fun insert(event: BlockEventEntity): Long

    @Query("SELECT COUNT(*) FROM block_event WHERE occurredAtEpochMs >= :sinceEpochMs")
    suspend fun countSince(sinceEpochMs: Long): Int
}

@Dao
interface DailyStatDao {
    @Insert
    suspend fun insert(stat: DailyStatEntity)

    @Query("SELECT * FROM daily_stat WHERE day BETWEEN :fromDay AND :toDay ORDER BY day")
    suspend fun between(fromDay: String, toDay: String): List<DailyStatEntity>
}
