package com.scrollguard.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Rect
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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

    /** Fonctionnalité actuellement recouverte par l'overlay (anti-spam d'événements). */
    @Volatile
    private var currentlyBlocked: FeatureId? = null

    /**
     * Les événements des apps hors cibles (lanceur inclus) sont filtrés par
     * packageNames : si l'utilisateur quitte l'app par le geste accueil ou
     * éteint l'écran, aucun événement ne nous préviendra. Ce garde-fou tourne
     * tant qu'une session d'app est ouverte : il clôt les sessions (app et
     * fonctionnalité) et retire l'overlay dès que l'app cible n'est plus au
     * premier plan.
     */
    private var foregroundWatchdog: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        ScrollGuardGraph.init(applicationContext)
        detector = FeatureDetector(ScrollGuardGraph.signatureRepository.load())
        tracker = SessionTracker(
            usageDao = ScrollGuardGraph.database.usageDao(),
            blockEventDao = ScrollGuardGraph.database.blockEventDao(),
            appSessionDao = ScrollGuardGraph.database.appSessionDao(),
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
        val reelsShortcutBounds = if (packageName == INSTAGRAM_PACKAGE) {
            detector.findBounds(
                packageName = packageName,
                feature = INSTAGRAM_REELS,
                root = root,
                ignoreSelected = true,
            )
        } else {
            null
        }

        scope.launch {
            tracker.onAppForeground(packageName)
            ensureForegroundWatchdog()
            updateReelsShortcut(packageName, reelsShortcutBounds)

            if (feature == null) {
                tracker.onFeatureExited()
                currentlyBlocked = null
                hideOverlay()
                return@launch
            }
            // Déjà bloquée et recouverte : ne pas re-journaliser ni re-chronométrer.
            if (feature == currentlyBlocked) return@launch

            tracker.onFeatureActive(feature)
            when (val decision = evaluate(feature)) {
                is Decision.Allow -> {
                    currentlyBlocked = null
                    hideOverlay()
                }
                is Decision.Warn -> {
                    currentlyBlocked = null
                    hideOverlay() // TODO(phase 1) : bandeau du temps restant
                }
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

    /**
     * Ne ferme PAS l'application : pose un écran de blocage persistant
     * par-dessus la fonctionnalité. L'app hôte reste ouverte ; l'utilisateur
     * peut seulement ouvrir ScrollGuard depuis l'overlay. Il n'existe aucun
     * bouton permettant de revenir vers la fonctionnalité bloquée.
     */
    private suspend fun block(feature: FeatureId, decision: Decision.Block) {
        tracker.onBlocked(feature, decision.rule.id, decision.reason.name)
        currentlyBlocked = feature
        withContext(Dispatchers.Main) {
            if (!overlay.isShowing) {
                overlay.show(
                    featureLabel = feature.value,
                    reasonLabel = getString(decision.reason.labelRes()),
                    onOpenScrollGuard = {
                        currentlyBlocked = null
                        overlay.hideAll()
                        packageManager.getLaunchIntentForPackage(packageName)
                            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            ?.let(::startActivity)
                    },
                )
            }
        }
    }

    private fun ensureForegroundWatchdog() {
        if (foregroundWatchdog?.isActive == true) return
        foregroundWatchdog = scope.launch {
            while (isActive) {
                delay(800)
                val foreground = withContext(Dispatchers.Main) {
                    rootInActiveWindow?.packageName?.toString()
                }
                if (foreground == null || foreground !in detector.watchedPackages()) {
                    currentlyBlocked = null
                    tracker.onFeatureExited()
                    tracker.onAppExited()
                    hideAllOverlays()
                    break
                }
            }
        }
    }

    private suspend fun hideOverlay() = withContext(Dispatchers.Main) { overlay.hide() }

    /**
     * Quand les Reels sont interdits, recouvre leur bouton dans la barre
     * Instagram. Le clic est consommé par l'overlay et affiche seulement un
     * message : aucune action retour et aucune fermeture d'Instagram.
     */
    private suspend fun updateReelsShortcut(packageName: String, bounds: Rect?) {
        if (packageName != INSTAGRAM_PACKAGE || bounds == null) {
            withContext(Dispatchers.Main) { overlay.hideBlockedShortcut() }
            return
        }

        val decision = evaluate(INSTAGRAM_REELS)
        withContext(Dispatchers.Main) {
            val foregroundPackage = rootInActiveWindow?.packageName?.toString()
            if (
                foregroundPackage == INSTAGRAM_PACKAGE &&
                decision is Decision.Block &&
                !overlay.isShowing
            ) {
                overlay.showBlockedShortcut(bounds)
            } else {
                overlay.hideBlockedShortcut()
            }
        }
    }

    private suspend fun hideAllOverlays() =
        withContext(Dispatchers.Main) { overlay.hideAll() }

    private fun BlockReason.labelRes(): Int = when (this) {
        BlockReason.ALWAYS_BLOCKED -> R.string.block_reason_always
        BlockReason.OUTSIDE_ALLOWED_WINDOW -> R.string.block_reason_window
        BlockReason.DAILY_LIMIT_REACHED -> R.string.block_reason_daily
        BlockReason.WEEKLY_LIMIT_REACHED -> R.string.block_reason_weekly
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        if (::overlay.isInitialized) overlay.hideAll()
        super.onDestroy()
    }

    private companion object {
        const val INSTAGRAM_PACKAGE = "com.instagram.android"
        val INSTAGRAM_REELS = FeatureId("instagram.reels")
    }
}
