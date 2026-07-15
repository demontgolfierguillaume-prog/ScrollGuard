package com.scrollguard.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrollguard.data.FeatureCatalog
import com.scrollguard.data.ScrollGuardGraph
import com.scrollguard.data.db.AppSessionEntity
import com.scrollguard.data.db.AppTotal
import com.scrollguard.data.db.FeatureCount
import com.scrollguard.data.db.FeatureTotal

@Composable
fun StatsScreen() {
    var period by remember { mutableIntStateOf(0) } // 0 = aujourd'hui, 1 = cette semaine
    var appSessions by remember { mutableStateOf(emptyList<AppSessionEntity>()) }
    var appTotals by remember { mutableStateOf(emptyList<AppTotal>()) }
    var opens by remember { mutableIntStateOf(0) }
    var longestSession by remember { mutableLongStateOf(0L) }
    var featureTotals by remember { mutableStateOf(emptyList<FeatureTotal>()) }
    var blocks by remember { mutableStateOf(emptyList<FeatureCount>()) }

    LaunchedEffect(period) {
        val since = if (period == 0) startOfTodayEpochMs() else startOfWeekEpochMs()
        val appDao = ScrollGuardGraph.database.appSessionDao()
        appSessions = appDao.sessionsSince(since)
        appTotals = appDao.totalsSince(since)
        opens = appDao.opensSince(since)
        longestSession = appDao.longestSessionSince(since)
        featureTotals = ScrollGuardGraph.database.usageDao().totalsSince(since)
        blocks = ScrollGuardGraph.database.blockEventDao().countsSince(since)
    }

    val blocksByFeature = blocks.associate { it.featureId to it.count }
    val totalAppSeconds = appTotals.sumOf { it.totalSeconds }
    val hourly = hourlyTotals(appSessions)
    val daily = dailyTotals(appSessions)
    val peakHour = hourly.indices.maxByOrNull { hourly[it] }?.takeIf { hourly[it] > 0 }

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
            SummaryCard(Modifier.weight(1f), formatDuration(totalAppSeconds), "sur les réseaux")
            SummaryCard(Modifier.weight(1f), "$opens", if (opens > 1) "ouvertures" else "ouverture")
            SummaryCard(Modifier.weight(1f), "${blocks.sumOf { it.count }}", "blocages")
        }

        // ---- Tranches horaires ----
        Text(
            if (period == 0) "Par tranche horaire" else "Par jour",
            style = MaterialTheme.typography.titleMedium,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (period == 0) {
                    BarChart(
                        values = hourly,
                        label = { index -> if (index % 6 == 0) "${index}h" else null },
                    )
                    peakHour?.let {
                        Text(
                            "Pic d'activité : ${it}h–${(it + 1) % 24}h (${formatDuration(hourly[it])})",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                } else {
                    BarChart(
                        values = daily,
                        label = { index -> listOf("L", "M", "M", "J", "V", "S", "D")[index] },
                    )
                }
                if (longestSession > 0) {
                    Text(
                        "Plus longue session : ${formatDuration(longestSession)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        // ---- Par application ----
        if (appTotals.isNotEmpty()) {
            Text("Par application", style = MaterialTheme.typography.titleMedium)
            val maxApp = appTotals.maxOf { it.totalSeconds }.coerceAtLeast(1)
            appTotals.forEach { app ->
                BarRow(
                    label = FeatureCatalog.appName(app.packageName),
                    valueLabel = formatDuration(app.totalSeconds),
                    fraction = app.totalSeconds.toFloat() / maxApp,
                )
            }
        }

        // ---- Par fonctionnalité ----
        if (featureTotals.isNotEmpty()) {
            Text("Par fonctionnalité", style = MaterialTheme.typography.titleMedium)
            val maxFeature = featureTotals.maxOf { it.totalSeconds }.coerceAtLeast(1)
            featureTotals.forEach { total ->
                val count = blocksByFeature[total.featureId] ?: 0
                BarRow(
                    label = FeatureCatalog.featureName(total.featureId),
                    valueLabel = buildString {
                        append(formatDuration(total.totalSeconds))
                        if (count > 0) append("  ·  $count 🛡️")
                    },
                    fraction = total.totalSeconds.toFloat() / maxFeature,
                )
            }
        }

        if (appTotals.isEmpty() && featureTotals.isEmpty()) {
            Text(
                "Rien à afficher pour cette période. Les temps apparaissent dès que tu " +
                    "utilises un des réseaux suivis.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** Histogramme simple : une barre par tranche, étiquettes optionnelles. */
@Composable
private fun BarChart(values: LongArray, label: (Int) -> String?) {
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        values.forEachIndexed { index, value ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val heightDp = if (value > 0) {
                    (value.toFloat() / max * 84f).coerceAtLeast(3f)
                } else 1.5f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heightDp.dp)
                        .background(
                            if (value > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(2.dp),
                        ),
                )
                Text(
                    label(index) ?: " ",
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun BarRow(label: String, valueLabel: String, fraction: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text(valueLabel, style = MaterialTheme.typography.bodyMedium)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                .height(6.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)),
        )
    }
}

@Composable
private fun SummaryCard(modifier: Modifier = Modifier, value: String, label: String) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}
