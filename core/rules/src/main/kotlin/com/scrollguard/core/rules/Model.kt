package com.scrollguard.core.rules

/** Identifiant stable d'une fonctionnalité, ex. "instagram.reels", "youtube.shorts". */
@JvmInline
value class FeatureId(val value: String)

enum class RuleType { BLOCK, DAILY_LIMIT, WEEKLY_LIMIT, TIME_WINDOWS }

/**
 * Créneau horaire autorisé.
 * [days] : jours ISO (1 = lundi … 7 = dimanche).
 * [startMinuteOfDay] / [endMinuteOfDay] : minutes depuis minuit, fin exclusive.
 */
data class TimeWindow(
    val days: Set<Int>,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
) {
    fun contains(dayOfWeek: Int, minuteOfDay: Int): Boolean =
        dayOfWeek in days && minuteOfDay >= startMinuteOfDay && minuteOfDay < endMinuteOfDay
}

data class Rule(
    val id: Long,
    val featureId: FeatureId,
    val type: RuleType,
    val dailyLimitMinutes: Int? = null,
    val weeklyLimitMinutes: Int? = null,
    val windows: List<TimeWindow> = emptyList(),
    val enabled: Boolean = true,
)

/** Temps déjà consommé sur la fonctionnalité évaluée. */
data class UsageSnapshot(
    val usedTodaySeconds: Long,
    val usedThisWeekSeconds: Long,
)

data class EvaluationContext(
    val dayOfWeek: Int,      // ISO : 1 = lundi … 7 = dimanche
    val minuteOfDay: Int,    // minutes depuis minuit
    val usage: UsageSnapshot,
)
