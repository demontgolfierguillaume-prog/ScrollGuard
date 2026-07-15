package com.scrollguard.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.scrollguard.core.detection.FeatureDetector
import com.scrollguard.core.rules.BlockReason
import com.scrollguard.core.rules.Decision
import com.scrollguard.core.rules.EvaluationContext
import com.scrollguard.core.rules.FeatureId
import com.scrollguard.core.rules.RuleEngine
import com.scrollguard.data.ScrollGuardGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

/**
 * Cœur du produit sur Android : reçoit les événements d'interface des apps
 * cibles (filtrées par packageNames dans accessibility_config.xml), identifie
 * la fonctionnalité active, applique la règle et bloque si nécessaire.
 *
 * Chemin chaud : le matching des signatures s'exécute sur le thread du service
 * (les AccessibilityNodeInfo ne doivent pas changer de thread) ; les accès
 * base de données et le moteur de règles partent en coroutine.
 */
class ScrollGuardAccessibilityService : AccessibilityService() {

    private lateinit var detector: FeatureDetector
    private lateinit var tracker: SessionTracker
    private lateinit var overlay: BlockOverlay
    private val engine = RuleEngine()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onServiceConnected() {
        super.onServiceConnected()
        ScrollGuardGraph.init(applicationContext)
        detector = FeatureDetector(ScrollGuardGraph.signatureRepository.load())
        tracker = SessionTracker(
            usageDao = ScrollGuardGraph.database.usageDao(),
            blockEventDao = ScrollGuardGraph.database.blockEventDao(),
        )
        overlay = BlockOverlay(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) return

        val packageName = event.packageName?.toString() ?: return
        val root = rootInActiveWindow ?: return

        // Matching synchrone sur le thread du service (voir doc de classe).
        val feature = detector.match(packageName, root)

        scope.launch {
            if (feature == null) {
                tracker.onFeatureExited()
                hideOverlay()
                return@launch
            }
            tracker.onFeatureActive(feature)
            when (val decision = evaluate(feature)) {
                is Decision.Allow -> hideOverlay()
                is Decision.Warn -> hideOverlay() // TODO(phase 1) : bandeau du temps restant
                is Decision.Block -> block(feature, decision)
            }
        }
    }

    private suspend fun evaluate(feature: FeatureId): Decision {
        val rules = ScrollGuardGraph.ruleRepository.rulesFor(feature)
        val now = LocalTime.now()
        val context = EvaluationContext(
            dayOfWeek = LocalDate.now().dayOfWeek.value,
            minuteOfDay = now.hour * 60 + now.minute,
            usage = tracker.snapshot(feature),
        )
        return engine.evaluate(rules, context)
    }

    private suspend fun block(feature: FeatureId, decision: Decision.Block) {
        tracker.onBlocked(feature, decision.rule.id, decision.reason.name)
        withContext(Dispatchers.Main) {
            if (!overlay.isShowing) {
                overlay.show(
                    featureLabel = feature.value,
                    reasonLabel = getString(decision.reason.labelRes()),
                ) {
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    overlay.hide()
                }
            }
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    private suspend fun hideOverlay() = withContext(Dispatchers.Main) { overlay.hide() }

    private fun BlockReason.labelRes(): Int = when (this) {
        BlockReason.ALWAYS_BLOCKED -> R.string.block_reason_always
        BlockReason.OUTSIDE_ALLOWED_WINDOW -> R.string.block_reason_window
        BlockReason.DAILY_LIMIT_REACHED -> R.string.block_reason_daily
        BlockReason.WEEKLY_LIMIT_REACHED -> R.string.block_reason_weekly
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
