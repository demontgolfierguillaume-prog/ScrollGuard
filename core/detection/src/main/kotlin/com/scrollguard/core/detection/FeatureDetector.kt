package com.scrollguard.core.detection

import android.view.accessibility.AccessibilityNodeInfo
import com.scrollguard.core.rules.FeatureId

/**
 * Fait correspondre l'arborescence d'accessibilité de l'app au premier plan
 * avec le catalogue de signatures, et renvoie la fonctionnalité active.
 *
 * Appelé sur le thread du service d'accessibilité (les [AccessibilityNodeInfo]
 * ne doivent pas traverser les threads) : le chemin chaud doit rester court.
 */
class FeatureDetector(initialCatalog: SignatureCatalog) {

    @Volatile
    private var catalog: SignatureCatalog = initialCatalog

    /** Remplace le catalogue (mise à jour remote config). */
    fun update(newCatalog: SignatureCatalog) {
        if (newCatalog.version > catalog.version) catalog = newCatalog
    }

    fun watchedPackages(): Set<String> = catalog.packages.keys

    /** Renvoie la fonctionnalité active dans [packageName], ou null si aucune. */
    fun match(packageName: String, root: AccessibilityNodeInfo): FeatureId? {
        val signatures = catalog.packages[packageName] ?: return null
        for (signature in signatures) {
            val selectedOk: (AccessibilityNodeInfo) -> Boolean =
                { node -> !signature.selectedOnly || node.isSelected }
            val hit = when (signature.matcher) {
                MatcherType.VIEW_ID ->
                    root.findAccessibilityNodeInfosByViewId(signature.pattern).any(selectedOk)
                MatcherType.CONTENT_DESC ->
                    hasDescendant(root) { it.contentDescription?.toString() == signature.pattern && selectedOk(it) }
                MatcherType.TEXT ->
                    hasDescendant(root) { it.text?.toString() == signature.pattern && selectedOk(it) }
            }
            if (hit) return FeatureId(signature.feature)
        }
        return null
    }

    private fun hasDescendant(
        node: AccessibilityNodeInfo,
        depth: Int = 0,
        predicate: (AccessibilityNodeInfo) -> Boolean,
    ): Boolean {
        if (predicate(node)) return true
        if (depth >= MAX_DEPTH) return false
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (hasDescendant(child, depth + 1, predicate)) return true
        }
        return false
    }

    private companion object {
        // Borne la traversée : les arborescences des réseaux sociaux sont profondes
        // et le service reçoit des dizaines d'événements par seconde.
        const val MAX_DEPTH = 12
    }
}
