package com.scrollguard.app.ui

import com.scrollguard.data.db.RuleEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

fun startOfTodayEpochMs(): Long =
    LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun startOfWeekEpochMs(): Long =
    LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun formatDuration(seconds: Long): String = when {
    seconds < 60 -> "$seconds s"
    seconds < 3600 -> "${seconds / 60} min"
    else -> "%d h %02d min".format(seconds / 3600, (seconds % 3600) / 60)
}

/** Libellé court d'une règle pour les listes ("Bloqué", "15 min/j"…). */
fun ruleLabel(rule: RuleEntity?): String = when (rule?.type) {
    null -> "Libre"
    "BLOCK" -> "Bloqué"
    "DAILY_LIMIT" -> "${rule.dailyLimitMinutes ?: 0} min/j"
    "WEEKLY_LIMIT" -> {
        val minutes = rule.weeklyLimitMinutes ?: 0
        if (minutes % 60 == 0) "${minutes / 60} h/sem." else "$minutes min/sem."
    }
    "TIME_WINDOWS" -> "Créneaux"
    else -> rule.type
}

fun minuteOfDayLabel(minuteOfDay: Int): String =
    "%dh%02d".format(minuteOfDay / 60, minuteOfDay % 60)
