package com.scrollguard.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.scrollguard.data.FeatureCatalog
import com.scrollguard.data.ScrollGuardGraph
import com.scrollguard.data.db.RuleEntity
import kotlinx.coroutines.launch

/**
 * Catalogue des réseaux et de leurs fonctionnalités. Les fonctionnalités avec
 * signature de détection sont configurables ; les autres sont affichées
 * « À venir » — l'interface ne promet jamais un blocage qu'on ne sait pas
 * encore appliquer.
 */
@Composable
fun FeaturesScreen() {
    val rules by ScrollGuardGraph.ruleRepository.allRules().collectAsState(initial = emptyList())
    val rulesByFeature = rules.filter { it.enabled }.associateBy { it.featureId }
    var editing by remember { mutableStateOf<FeatureCatalog.Feature?>(null) }
    val scope = rememberCoroutineScope()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                "Blocages",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(20.dp),
            )
        }
        FeatureCatalog.apps.forEach { app ->
            item(key = app.id) {
                Text(
                    app.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            items(app.features, key = { it.id }) { feature ->
                FeatureRow(
                    feature = feature,
                    rule = rulesByFeature[feature.id],
                    onClick = { if (feature.detectable) editing = feature },
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
        }
    }

    editing?.let { feature ->
        RuleEditorDialog(
            feature = feature,
            existing = rulesByFeature[feature.id],
            onDismiss = { editing = null },
            onSave = { entity: RuleEntity? ->
                scope.launch { ScrollGuardGraph.ruleRepository.setRule(feature.id, entity) }
                editing = null
            },
        )
    }
}

@Composable
private fun FeatureRow(
    feature: FeatureCatalog.Feature,
    rule: RuleEntity?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = feature.detectable, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .alpha(if (feature.detectable) 1f else 0.45f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(feature.name, style = MaterialTheme.typography.bodyLarge)
            if (!feature.detectable) {
                Text("Détection à venir", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (feature.detectable) {
            SuggestionChip(onClick = onClick, label = { Text(ruleLabel(rule)) })
        }
    }
}
