package com.scrollguard.service

import android.os.SystemClock
import com.scrollguard.core.rules.FeatureId
import com.scrollguard.core.rules.UsageSnapshot
import com.scrollguard.data.db.AppSessionDao
import com.scrollguard.data.db.AppSessionEntity
import com.scrollguard.data.db.BlockEventDao
import com.scrollguard.data.db.BlockEventEntity
import com.scrollguard.data.db.UsageDao
import com.scrollguard.data.db.UsageSessionEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * Chronomètre les sessions par fonctionnalité.
 *
 * Les durées sont mesurées à l'horloge monotone ([SystemClock.elapsedRealtime]) :
 * changer la date du téléphone ne fausse pas les compteurs (anti-contournement,
 * cahier des charges §5).
 */
class SessionTracker(
    private val usageDao: UsageDao,
    private val blockEventDao: BlockEventDao,
    private val appSessionDao: AppSessionDao,
) {

    private data class OpenSession(
        val feature: FeatureId,
        val rowId: Long,
        val startedElapsedMs: Long,
        val startedEpochMs: Long,
    )

    private data class OpenAppSession(
        val packageName: String,
        val rowId: Long,
        val startedElapsedMs: Long,
    )

    private var current: OpenSession? = null
    private var currentApp: OpenAppSession? = null

    /** À appeler à chaque événement d'une app cible : ouvre/continue sa session. */
    suspend fun onAppForeground(packageName: String) {
        if (currentApp?.packageName == packageName) return
        closeAppSession()
        val rowId = appSessionDao.insert(
            AppSessionEntity(packageName = packageName, startedAtEpochMs = System.currentTimeMillis()),
        )
        currentApp = OpenAppSession(packageName, rowId, SystemClock.elapsedRealtime())
    }

    suspend fun onAppExited() {
        closeAppSession()
    }

    private suspend fun closeAppSession() {
        val session = currentApp ?: return
        currentApp = null
        appSessionDao.close(
            id = session.rowId,
            endedAt = System.currentTimeMillis(),
            durationSeconds = (SystemClock.elapsedRealtime() - session.startedElapsedMs) / 1000,
        )
    }

    suspend fun onFeatureActive(feature: FeatureId) {
        if (current?.feature == feature) return
        closeCurrent(endedBy = "user")
        val rowId = usageDao.insert(
            UsageSessionEntity(featureId = feature.value, startedAtEpochMs = System.currentTimeMillis()),
        )
        current = OpenSession(
            feature = feature,
            rowId = rowId,
            startedElapsedMs = SystemClock.elapsedRealtime(),
            startedEpochMs = System.currentTimeMillis(),
        )
    }

    suspend fun onFeatureExited(endedBy: String = "user") {
        closeCurrent(endedBy)
    }

    suspend fun onBlocked(feature: FeatureId, ruleId: Long?, reason: String) {
        blockEventDao.insert(
            BlockEventEntity(
                featureId = feature.value,
                ruleId = ruleId,
                occurredAtEpochMs = System.currentTimeMillis(),
                reason = reason,
            ),
        )
        closeCurrent(endedBy = "block")
    }

    /** Temps consommé aujourd'hui / cette semaine, session en cours incluse. */
    suspend fun snapshot(feature: FeatureId): UsageSnapshot {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val startOfDay = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val startOfWeek = today.with(DayOfWeek.MONDAY).atStartOfDay(zone).toInstant().toEpochMilli()

        val currentSeconds = current
            ?.takeIf { it.feature == feature }
            ?.let { (SystemClock.elapsedRealtime() - it.startedElapsedMs) / 1000 }
            ?: 0L

        return UsageSnapshot(
            usedTodaySeconds = usageDao.usedSecondsSince(feature.value, startOfDay) + currentSeconds,
            usedThisWeekSeconds = usageDao.usedSecondsSince(feature.value, startOfWeek) + currentSeconds,
        )
    }

    private suspend fun closeCurrent(endedBy: String) {
        val session = current ?: return
        current = null
        val durationSeconds = (SystemClock.elapsedRealtime() - session.startedElapsedMs) / 1000
        usageDao.close(
            id = session.rowId,
            endedAt = System.currentTimeMillis(),
            durationSeconds = durationSeconds,
            endedBy = endedBy,
        )
    }
}
