package com.scrollguard.core.rules

sealed interface Decision {
    data object Allow : Decision

    /** La fonctionnalité reste accessible mais la limite approche. */
    data class Warn(val rule: Rule, val remainingSeconds: Long) : Decision

    data class Block(val rule: Rule, val reason: BlockReason) : Decision
}

enum class BlockReason {
    ALWAYS_BLOCKED,
    OUTSIDE_ALLOWED_WINDOW,
    DAILY_LIMIT_REACHED,
    WEEKLY_LIMIT_REACHED,
}

/**
 * Moteur de règles pur (aucune dépendance Android), déterministe et testable.
 *
 * La règle la plus restrictive gagne. Ordre d'évaluation :
 * blocage > fenêtres horaires > limite quotidienne > limite hebdomadaire.
 */
class RuleEngine(private val warnThresholdRatio: Double = 0.10) {

    fun evaluate(rules: List<Rule>, context: EvaluationContext): Decision {
        val active = rules.filter { it.enabled }
        if (active.isEmpty()) return Decision.Allow

        active.firstOrNull { it.type == RuleType.BLOCK }
            ?.let { return Decision.Block(it, BlockReason.ALWAYS_BLOCKED) }

        for (rule in active.filter { it.type == RuleType.TIME_WINDOWS }) {
            val inWindow = rule.windows.any { it.contains(context.dayOfWeek, context.minuteOfDay) }
            if (!inWindow) return Decision.Block(rule, BlockReason.OUTSIDE_ALLOWED_WINDOW)
        }

        var warn: Decision.Warn? = null

        for (rule in active.filter { it.type == RuleType.DAILY_LIMIT }) {
            val limitSeconds = (rule.dailyLimitMinutes ?: continue) * 60L
            val remaining = limitSeconds - context.usage.usedTodaySeconds
            if (remaining <= 0) return Decision.Block(rule, BlockReason.DAILY_LIMIT_REACHED)
            if (remaining <= limitSeconds * warnThresholdRatio) {
                warn = closest(warn, Decision.Warn(rule, remaining))
            }
        }

        for (rule in active.filter { it.type == RuleType.WEEKLY_LIMIT }) {
            val limitSeconds = (rule.weeklyLimitMinutes ?: continue) * 60L
            val remaining = limitSeconds - context.usage.usedThisWeekSeconds
            if (remaining <= 0) return Decision.Block(rule, BlockReason.WEEKLY_LIMIT_REACHED)
            if (remaining <= limitSeconds * warnThresholdRatio) {
                warn = closest(warn, Decision.Warn(rule, remaining))
            }
        }

        return warn ?: Decision.Allow
    }

    private fun closest(a: Decision.Warn?, b: Decision.Warn): Decision.Warn =
        if (a == null || b.remainingSeconds < a.remainingSeconds) b else a
}
