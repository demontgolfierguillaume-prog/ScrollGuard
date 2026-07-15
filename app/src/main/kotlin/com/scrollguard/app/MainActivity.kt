package com.scrollguard.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.scrollguard.data.ScrollGuardGraph
import com.scrollguard.data.db.RuleEntity
import kotlinx.coroutines.launch

/**
 * Écran unique de la phase 0 : état du service, accès aux réglages système et
 * insertion de règles de démonstration pour tester le blocage sur appareil.
 * L'onboarding et le tableau de bord complets arrivent en phase 1 (S1/S2 du
 * cahier des charges).
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ScrollGuardGraph.init(applicationContext)

        setContent {
            MaterialTheme {
                Phase0Screen(
                    isServiceEnabled = ::isAccessibilityServiceEnabled,
                    onOpenAccessibilitySettings = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onInsertDemoRules = ::insertDemoRules,
                )
            }
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.contains("$packageName/", ignoreCase = true) ||
            enabled.contains("com.scrollguard.service.ScrollGuardAccessibilityService", ignoreCase = true)
    }

    private fun insertDemoRules(onDone: () -> Unit) {
        lifecycleScope.launch {
            val dao = ScrollGuardGraph.database.ruleDao()
            dao.insert(RuleEntity(featureId = "instagram.reels", type = "BLOCK"))
            dao.insert(RuleEntity(featureId = "youtube.shorts", type = "DAILY_LIMIT", dailyLimitMinutes = 15))
            onDone()
        }
    }
}

@androidx.compose.runtime.Composable
private fun Phase0Screen(
    isServiceEnabled: () -> Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onInsertDemoRules: (onDone: () -> Unit) -> Unit,
) {
    var serviceEnabled by remember { mutableStateOf(isServiceEnabled()) }
    var demoRulesInserted by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("ScrollGuard — Phase 0", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Prototype de validation : détection des fonctionnalités (Reels, Shorts…) " +
                    "et blocage par superposition.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        if (serviceEnabled) "✅ Service d'accessibilité actif"
                        else "❌ Service d'accessibilité inactif",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Button(onClick = {
                        onOpenAccessibilitySettings()
                        serviceEnabled = isServiceEnabled()
                    }) {
                        Text("Ouvrir les réglages d'accessibilité")
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Règles de test", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Insère deux règles de démonstration : blocage complet des Reels " +
                            "Instagram et limite de 15 min/jour sur les Shorts YouTube.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(
                        enabled = !demoRulesInserted,
                        onClick = { onInsertDemoRules { demoRulesInserted = true } },
                    ) {
                        Text(if (demoRulesInserted) "Règles insérées" else "Insérer les règles de démo")
                    }
                }
            }
        }
    }
}
