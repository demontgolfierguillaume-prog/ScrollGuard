package com.scrollguard.core.rules

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RuleEngineTest {

    private val engine = RuleEngine()
    private val reels = FeatureId("instagram.reels")

    private fun context(
        usedToday: Long = 0,
        usedWeek: Long = 0,
        dayOfWeek: Int = 1,
        minuteOfDay: Int = 12 * 60,
    ) = EvaluationContext(dayOfWeek, minuteOfDay, UsageSnapshot(usedToday, usedWeek))

    @Test
    fun `aucune regle - autorise`() {
        assertEquals(Decision.Allow, engine.evaluate(emptyList(), context()))
    }

    @Test
    fun `blocage complet`() {
        val rule = Rule(1, reels, RuleType.BLOCK)
        val decision = engine.evaluate(listOf(rule), context())
        assertIs<Decision.Block>(decision)
        assertEquals(BlockReason.ALWAYS_BLOCKED, decision.reason)
    }

    @Test
    fun `limite quotidienne epuisee - bloque`() {
        val rule = Rule(1, reels, RuleType.DAILY_LIMIT, dailyLimitMinutes = 15)
        val decision = engine.evaluate(listOf(rule), context(usedToday = 15 * 60L))
        assertIs<Decision.Block>(decision)
        assertEquals(BlockReason.DAILY_LIMIT_REACHED, decision.reason)
    }

    @Test
    fun `limite quotidienne presque atteinte - avertit`() {
        val rule = Rule(1, reels, RuleType.DAILY_LIMIT, dailyLimitMinutes = 15)
        val decision = engine.evaluate(listOf(rule), context(usedToday = 14 * 60L + 30))
        assertIs<Decision.Warn>(decision)
    }

    @Test
    fun `hors fenetre horaire - bloque`() {
        val rule = Rule(
            1, reels, RuleType.TIME_WINDOWS,
            windows = listOf(TimeWindow(days = setOf(1), startMinuteOfDay = 18 * 60, endMinuteOfDay = 19 * 60)),
        )
        val decision = engine.evaluate(listOf(rule), context(minuteOfDay = 12 * 60))
        assertIs<Decision.Block>(decision)
        assertEquals(BlockReason.OUTSIDE_ALLOWED_WINDOW, decision.reason)
    }

    @Test
    fun `dans la fenetre horaire - autorise`() {
        val rule = Rule(
            1, reels, RuleType.TIME_WINDOWS,
            windows = listOf(TimeWindow(days = setOf(1), startMinuteOfDay = 18 * 60, endMinuteOfDay = 19 * 60)),
        )
        assertEquals(Decision.Allow, engine.evaluate(listOf(rule), context(minuteOfDay = 18 * 60 + 30)))
    }

    @Test
    fun `la regle la plus restrictive gagne - blocage avant limite`() {
        val rules = listOf(
            Rule(1, reels, RuleType.DAILY_LIMIT, dailyLimitMinutes = 15),
            Rule(2, reels, RuleType.BLOCK),
        )
        val decision = engine.evaluate(rules, context())
        assertIs<Decision.Block>(decision)
        assertEquals(BlockReason.ALWAYS_BLOCKED, decision.reason)
    }

    @Test
    fun `limite hebdomadaire epuisee - bloque meme si le quotidien reste`() {
        val rules = listOf(
            Rule(1, reels, RuleType.DAILY_LIMIT, dailyLimitMinutes = 30),
            Rule(2, reels, RuleType.WEEKLY_LIMIT, weeklyLimitMinutes = 60),
        )
        val decision = engine.evaluate(rules, context(usedToday = 5 * 60L, usedWeek = 60 * 60L))
        assertIs<Decision.Block>(decision)
        assertEquals(BlockReason.WEEKLY_LIMIT_REACHED, decision.reason)
    }
}
