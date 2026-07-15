package com.scrollguard.app.ui

import com.scrollguard.data.db.AppSessionEntity
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Ventile des sessions dans des tranches temporelles. Une session à cheval
 * sur plusieurs tranches (ex. 17h50 → 18h20) est découpée entre elles.
 */

/** Secondes par heure de la journée (index 0 = 0h–1h … 23 = 23h–0h). */
fun hourlyTotals(sessions: List<AppSessionEntity>): LongArray {
    val zone = ZoneId.systemDefault()
    val buckets = LongArray(24)
    for (session in sessions) {
        var cursor = session.startedAtEpochMs
        val end = session.endedAtEpochMs
            ?: (cursor + (session.durationSeconds ?: 0) * 1000)
        while (cursor < end) {
            val zoned = Instant.ofEpochMilli(cursor).atZone(zone)
            val nextBoundary = zoned.truncatedTo(ChronoUnit.HOURS).plusHours(1)
                .toInstant().toEpochMilli()
            val chunkEnd = minOf(end, nextBoundary)
            if (chunkEnd <= cursor) break
            buckets[zoned.hour] += (chunkEnd - cursor) / 1000
            cursor = chunkEnd
        }
    }
    return buckets
}

/** Secondes par jour de semaine (index 0 = lundi … 6 = dimanche). */
fun dailyTotals(sessions: List<AppSessionEntity>): LongArray {
    val zone = ZoneId.systemDefault()
    val buckets = LongArray(7)
    for (session in sessions) {
        var cursor = session.startedAtEpochMs
        val end = session.endedAtEpochMs
            ?: (cursor + (session.durationSeconds ?: 0) * 1000)
        while (cursor < end) {
            val zoned = Instant.ofEpochMilli(cursor).atZone(zone)
            val nextBoundary = zoned.toLocalDate().plusDays(1).atStartOfDay(zone)
                .toInstant().toEpochMilli()
            val chunkEnd = minOf(end, nextBoundary)
            if (chunkEnd <= cursor) break
            buckets[zoned.dayOfWeek.value - 1] += (chunkEnd - cursor) / 1000
            cursor = chunkEnd
        }
    }
    return buckets
}
