package com.scrollguard.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scrollguard.data.FeatureCatalog
import com.scrollguard.data.ScrollGuardGraph
import com.scrollguard.data.db.FeatureCount
import com.scrollguard.data.db.FeatureTotal

@Composable
fun StatsScreen() {
    var period by remember { mutableIntStateOf(0) } // 0 = aujourd'hui, 1 = cette semaine
    var totals by remember { mutableStateOf(emptyList<FeatureTotal>()) }
    var blocks by remember { mutableStateOf(emptyList<FeatureCount>()) }

    LaunchedEffect(period) {
        val since = if (period == 0) startOfTodayEpochMs() else startOfWeekEpochMs()
        totals = ScrollGuardGraph.database.usageDao().totalsSince(since)
        blocks = ScrollGuardGraph.database.blockEventDao().countsSince(since)
    }

    val blocksByFeature = blocks.associate { it.featureId to it.count }
    val maxSeconds = totals.maxOfOrNull { it.totalSeconds } ?: 0L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Statistiques", style = MaterialTheme.typography.headlineMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = period == 0, onClick = { period = 0 }, label = { Text("Aujourd'hui") })
            FilterChip(selected = period == 1, onClick = { period = 1 }, label = { Text("Cette semaine") })
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(
                modifier = Modifier.weight(1f),
                value = formatDuration(totals.sumOf { it.totalSeconds }),
                label = "temps mesuré",
            )
            SummaryCard(
                modifier = Modifier.weight(1f),
                value = "${blocks.sumOf { it.count }}",
                label = "blocages",
            )
        }

        if (totals.isEmpty()) {
            Text(
                "Rien à afficher pour cette période. Les temps apparaissent dès que tu ouvres " +
                    "une fonctionnalité suivie (Reels, Shorts…).",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Text("Par fonctionnalité", style = MaterialTheme.typography.titleMedium)
            totals.forEach { total ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(FeatureCatalog.featureName(total.featureId))
                        Text(
                            buildString {
                                append(formatDuration(total.totalSeconds))
                                val count = blocksByFeature[total.featureId] ?: 0
                                if (count > 0) append("  ·  $count 🛡️")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    val fraction = if (maxSeconds > 0) {
                        (total.totalSeconds.toFloat() / maxSeconds).coerceIn(0.02f, 1f)
                    } else 0.02f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(6.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)),
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(modifier: Modifier = Modifier, value: String, label: String) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}
