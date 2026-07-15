package com.scrollguard.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Règle de restriction persistée. `windowsJson` contient les créneaux au format
 * `[{"days":[1,2,3,4,5],"start":1080,"end":1140}]` (minutes depuis minuit).
 */
@Entity(tableName = "rule")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val featureId: String,
    val type: String,                 // BLOCK | DAILY_LIMIT | WEEKLY_LIMIT | TIME_WINDOWS
    val dailyLimitMinutes: Int? = null,
    val weeklyLimitMinutes: Int? = null,
    val windowsJson: String? = null,
    val enabled: Boolean = true,
)

/** Session d'utilisation d'une fonctionnalité (entrée → sortie). */
@Entity(tableName = "usage_session")
data class UsageSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val featureId: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long? = null,
    val durationSeconds: Long? = null, // mesurée à l'horloge monotone, pas à l'heure murale
    val endedBy: String? = null,       // user | block | screen_off
)

@Entity(tableName = "block_event")
data class BlockEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val featureId: String,
    val ruleId: Long? = null,
    val occurredAtEpochMs: Long,
    val reason: String,
    val reaction: String? = null,      // left | waited | bypassed_5min | circumvented
)

/** Agrégat quotidien précalculé ; les événements bruts sont purgés à 90 jours. */
@Entity(tableName = "daily_stat", primaryKeys = ["day", "featureId"])
data class DailyStatEntity(
    val day: String,                   // "2026-07-15"
    val featureId: String,
    val totalSeconds: Long,
    val opens: Int,
    val blocks: Int,
    val bypasses: Int,
    val savedEstimateSeconds: Long,
)
