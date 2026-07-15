package com.scrollguard.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.scrollguard.app.ui.DashboardScreen
import com.scrollguard.app.ui.FeaturesScreen
import com.scrollguard.app.ui.StatsScreen
import com.scrollguard.data.ScrollGuardGraph

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ScrollGuardGraph.init(applicationContext)

        val versionName = packageManager.getPackageInfo(packageName, 0).versionName ?: "?"

        setContent {
            MaterialTheme {
                ScrollGuardApp(
                    versionName = versionName,
                    isServiceEnabled = ::isAccessibilityServiceEnabled,
                    onOpenAccessibilitySettings = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
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
}

private data class Tab(val label: String, val emoji: String)

private val tabs = listOf(
    Tab("Accueil", "🏠"),
    Tab("Blocages", "🛡️"),
    Tab("Stats", "📊"),
)

@Composable
private fun ScrollGuardApp(
    versionName: String,
    isServiceEnabled: () -> Boolean,
    onOpenAccessibilitySettings: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Text(tab.emoji) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                0 -> DashboardScreen(versionName, isServiceEnabled, onOpenAccessibilitySettings)
                1 -> FeaturesScreen()
                else -> StatsScreen()
            }
        }
    }
}
