package com.scrollguard.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scrollguard.data.FeatureCatalog
import com.scrollguard.data.ScrollGuardGraph
import com.scrollguard.data.db.RuleEntity

private enum class RuleChoice(val label: String) {
    NONE("Aucune règle"),
    BLOCK("Blocage complet"),
    DAILY("Limite quotidienne"),
    WEEKLY("Limite hebdomadaire"),
    WINDOWS("Créneaux autorisés"),
}

@Composable
fun RuleEditorDialog(
    feature: FeatureCatalog.Feature,
    existing: RuleEntity?,
    onDismiss: () -> Unit,
    onSave: (RuleEntity?) -> Unit,
) {
    var choice by remember {
        mutableStateOf(
            when (existing?.type) {
                "BLOCK" -> RuleChoice.BLOCK
                "DAILY_LIMIT" -> RuleChoice.DAILY
                "WEEKLY_LIMIT" -> RuleChoice.WEEKLY
                "TIME_WINDOWS" -> RuleChoice.WINDOWS
                else -> RuleChoice.NONE
            },
        )
    }
    var dailyMinutes by remember {
        mutableFloatStateOf((existing?.dailyLimitMinutes ?: 15).toFloat())
    }
    var weeklyMinutes by remember {
        mutableFloatStateOf((existing?.weeklyLimitMinutes ?: 120).toFloat())
    }
    var days by remember { mutableStateOf(setOf(1, 2, 3, 4, 5, 6, 7)) }
    var startHalfHour by remember { mutableFloatStateOf(36f) } // 18h00
    var endHalfHour by remember { mutableFloatStateOf(38f) }   // 19h00

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(feature.name) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RuleChoice.entries.forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = choice == option, onClick = { choice = option })
                        Text(option.label)
                    }
                }

                when (choice) {
                    RuleChoice.DAILY -> {
                        Text("${dailyMinutes.toInt()} min par jour", style = MaterialTheme.typography.titleMedium)
                        Slider(
                            value = dailyMinutes,
                            onValueChange = { dailyMinutes = (it / 5).toInt() * 5f },
                            valueRange = 5f..240f,
                        )
                    }
                    RuleChoice.WEEKLY -> {
                        val minutes = weeklyMinutes.toInt()
                        val label = if (minutes % 60 == 0) "${minutes / 60} h par semaine" else "$minutes min par semaine"
                        Text(label, style = MaterialTheme.typography.titleMedium)
                        Slider(
                            value = weeklyMinutes,
                            onValueChange = { weeklyMinutes = (it / 15).toInt() * 15f },
                            valueRange = 15f..1200f,
                        )
                    }
                    RuleChoice.WINDOWS -> {
                        Text("Jours autorisés", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val labels = listOf("L", "M", "M", "J", "V", "S", "D")
                            (1..7).forEach { day ->
                                FilterChip(
                                    selected = day in days,
                                    onClick = {
                                        days = if (day in days) days - day else days + day
                                    },
                                    label = { Text(labels[day - 1]) },
                                )
                            }
                        }
                        Text(
                            "De ${minuteOfDayLabel(startHalfHour.toInt() * 30)} à ${minuteOfDayLabel(endHalfHour.toInt() * 30)}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text("Début", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = startHalfHour,
                            onValueChange = {
                                startHalfHour = it.toInt().toFloat()
                                if (endHalfHour <= startHalfHour) endHalfHour = startHalfHour + 1
                            },
                            valueRange = 0f..46f,
                        )
                        Text("Fin", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = endHalfHour,
                            onValueChange = {
                                endHalfHour = maxOf(it.toInt().toFloat(), startHalfHour + 1)
                            },
                            valueRange = 1f..47f,
                        )
                    }
                    else -> Unit
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(buildRule(feature.id, choice, dailyMinutes, weeklyMinutes, days, startHalfHour, endHalfHour)) }) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}

private fun buildRule(
    featureId: String,
    choice: RuleChoice,
    dailyMinutes: Float,
    weeklyMinutes: Float,
    days: Set<Int>,
    startHalfHour: Float,
    endHalfHour: Float,
): RuleEntity? = when (choice) {
    RuleChoice.NONE -> null
    RuleChoice.BLOCK -> RuleEntity(featureId = featureId, type = "BLOCK")
    RuleChoice.DAILY -> RuleEntity(
        featureId = featureId, type = "DAILY_LIMIT",
        dailyLimitMinutes = dailyMinutes.toInt(),
    )
    RuleChoice.WEEKLY -> RuleEntity(
        featureId = featureId, type = "WEEKLY_LIMIT",
        weeklyLimitMinutes = weeklyMinutes.toInt(),
    )
    RuleChoice.WINDOWS -> RuleEntity(
        featureId = featureId, type = "TIME_WINDOWS",
        windowsJson = ScrollGuardGraph.ruleRepository.windowsJson(
            days = days.sorted(),
            startMinuteOfDay = startHalfHour.toInt() * 30,
            endMinuteOfDay = endHalfHour.toInt() * 30,
        ),
    )
}
