package com.scrollguard.data

import com.scrollguard.core.rules.FeatureId
import com.scrollguard.core.rules.Rule
import com.scrollguard.core.rules.RuleType
import com.scrollguard.core.rules.TimeWindow
import com.scrollguard.data.db.RuleDao
import com.scrollguard.data.db.RuleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Convertit les entités persistées en règles du moteur (:core:rules). */
class RuleRepository(private val ruleDao: RuleDao) {

    suspend fun rulesFor(feature: FeatureId): List<Rule> =
        ruleDao.rulesForFeature(feature.value).mapNotNull { it.toDomain() }

    fun allRules(): Flow<List<RuleEntity>> = ruleDao.allRules()

    suspend fun add(rule: RuleEntity): Long = ruleDao.insert(rule)

    /** Remplace la règle d'une fonctionnalité ; null = plus aucune règle. */
    suspend fun setRule(featureId: String, rule: RuleEntity?) {
        ruleDao.deleteForFeature(featureId)
        rule?.let { ruleDao.insert(it) }
    }

    /** Sérialise des créneaux pour RuleEntity.windowsJson. */
    fun windowsJson(days: List<Int>, startMinuteOfDay: Int, endMinuteOfDay: Int): String =
        json.encodeToString(
            ListSerializer(WindowDto.serializer()),
            listOf(WindowDto(days, startMinuteOfDay, endMinuteOfDay)),
        )

    private fun RuleEntity.toDomain(): Rule? {
        val ruleType = runCatching { RuleType.valueOf(type) }.getOrNull() ?: return null
        return Rule(
            id = id,
            featureId = FeatureId(featureId),
            type = ruleType,
            dailyLimitMinutes = dailyLimitMinutes,
            weeklyLimitMinutes = weeklyLimitMinutes,
            windows = windowsJson?.let(::parseWindows).orEmpty(),
            enabled = enabled,
        )
    }

    private fun parseWindows(raw: String): List<TimeWindow> =
        runCatching {
            json.decodeFromString(ListSerializer(WindowDto.serializer()), raw)
                .map { TimeWindow(it.days.toSet(), it.start, it.end) }
        }.getOrDefault(emptyList())

    @Serializable
    private data class WindowDto(val days: List<Int>, val start: Int, val end: Int)

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
