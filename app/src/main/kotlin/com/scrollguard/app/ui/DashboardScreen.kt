package com.scrollguard.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scrollguard.data.FeatureCatalog
import com.scrollguard.data.ScrollGuardGraph
import com.scrollguard.data.db.FeatureTotal

@Composable
fun DashboardScreen(
    versionName: String,
    isServiceEnabled: () -> Boolean,
    onOpenAccessibilitySettings: () -> Unit,
) {
    var serviceEnabled by remember { mutableStateOf(false) }
    var todayTotalSeconds by remember { mutableLongStateOf(0L) }
    var todayBlocks by remember { mutableIntStateOf(0) }
    var topFeatures by remember { mutableStateOf(emptyList<FeatureTotal>()) }

    LaunchedEffect(Unit) {
        serviceEnabled = isServiceEnabled()
        val startOfDay = startOfTodayEpochMs()
        topFeatures = ScrollGuardGraph.database.usageDao().totalsSince(startOfDay)
        todayTotalSeconds = topFeatures.sumOf { it.totalSeconds }
        todayBlocks = ScrollGuardGraph.database.blockEventDao().countSince(startOfDay)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("ScrollGuard v$versionName", style = MaterialTheme.typography.headlineMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    if (serviceEnabled) "✅ Protection active"
                    else "⚠️ Protection inactive",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (!serviceEnabled) {
                    Text(
                        "Active le service d'accessibilité pour que les règles s'appliquent.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(onClick = onOpenAccessibilitySettings) {
                        Text("Activer la protection")
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                value = formatDuration(todayTotalSeconds),
                label = "de distraction aujourd'hui",
            )
            StatCard(
                modifier = Modifier.weight(1f),
                value = "$todayBlocks",
                label = if (todayBlocks > 1) "blocages aujourd'hui" else "blocage aujourd'hui",
            )
        }

        if (topFeatures.isNotEmpty()) {
            Text("Où passe ton temps", style = MaterialTheme.typography.titleMedium)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    topFeatures.take(5).forEach { total ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(FeatureCatalog.featureName(total.featureId))
                            Text(formatDuration(total.totalSeconds), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        } else {
            Text(
                "Aucune utilisation mesurée aujourd'hui. Configure des règles dans l'onglet Blocages.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier = Modifier, value: String, label: String) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}
