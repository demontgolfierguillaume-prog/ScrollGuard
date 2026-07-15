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

    @Query("DELETE FROM rule WHERE featureId = :featureId")
    suspend fun deleteForFeature(featureId: String)
}

/** Agrégat « temps total par fonctionnalité » pour les écrans de stats. */
data class FeatureTotal(val featureId: String, val totalSeconds: Long)

/** Agrégat « nombre d'événements par fonctionnalité ». */
data class FeatureCount(val featureId: String, val count: Int)

@Dao
interface UsageDao {
    @Insert
    suspend fun insert(session: UsageSessionEntity): Long

    @Query("UPDATE usage_session SET endedAtEpochMs = :endedAt, durationSeconds = :durationSeconds, endedBy = :endedBy WHERE id = :id")
    suspend fun close(id: Long, endedAt: Long, durationSeconds: Long, endedBy: String)

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM usage_session WHERE featureId = :featureId AND startedAtEpochMs >= :sinceEpochMs")
    suspend fun usedSecondsSince(featureId: String, sinceEpochMs: Long): Long

    @Query("SELECT featureId, COALESCE(SUM(durationSeconds), 0) AS totalSeconds FROM usage_session WHERE startedAtEpochMs >= :sinceEpochMs GROUP BY featureId ORDER BY totalSeconds DESC")
    suspend fun totalsSince(sinceEpochMs: Long): List<FeatureTotal>

    @Query("DELETE FROM usage_session WHERE startedAtEpochMs < :beforeEpochMs")
    suspend fun purgeOlderThan(beforeEpochMs: Long)
}

@Dao
interface BlockEventDao {
    @Insert
    suspend fun insert(event: BlockEventEntity): Long

    @Query("SELECT COUNT(*) FROM block_event WHERE occurredAtEpochMs >= :sinceEpochMs")
    suspend fun countSince(sinceEpochMs: Long): Int

    @Query("SELECT featureId, COUNT(*) AS count FROM block_event WHERE occurredAtEpochMs >= :sinceEpochMs GROUP BY featureId ORDER BY count DESC")
    suspend fun countsSince(sinceEpochMs: Long): List<FeatureCount>
}

@Dao
interface DailyStatDao {
    @Insert
    suspend fun insert(stat: DailyStatEntity)

    @Query("SELECT * FROM daily_stat WHERE day BETWEEN :fromDay AND :toDay ORDER BY day")
    suspend fun between(fromDay: String, toDay: String): List<DailyStatEntity>
}
