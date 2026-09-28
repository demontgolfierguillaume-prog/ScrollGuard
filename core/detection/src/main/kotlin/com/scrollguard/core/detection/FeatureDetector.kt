package com.scrollguard.core.detection

import android.graphics.Rect
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
            if (findNode(root, signature, ignoreSelected = false) != null) {
                return FeatureId(signature.feature)
            }
        }
        return null
    }

    /**
     * Localise à l'écran le contrôle qui ouvre [feature]. [ignoreSelected]
     * permet de trouver un bouton d'onglet même quand il n'est pas actif afin
     * qu'un overlay puisse le masquer avant le clic.
     *
     * Seules les coordonnées sont renvoyées : aucun AccessibilityNodeInfo ne
     * quitte le thread du service.
     */
    fun findBounds(
        packageName: String,
        feature: FeatureId,
        root: AccessibilityNodeInfo,
        ignoreSelected: Boolean = false,
    ): Rect? {
        val signatures = catalog.packages[packageName] ?: return null
        for (signature in signatures) {
            if (signature.feature != feature.value) continue
            val node = findNode(root, signature, ignoreSelected) ?: continue
            val targetBounds = Rect().also(node::getBoundsInScreen)
            if (targetBounds.isEmpty) continue

            // Une signature Instagram peut viser l'ImageView du logo au lieu
            // de la cellule tactile complète. Remonter jusqu'au parent
            // cliquable afin que l'overlay couvre bien toute la cible du tap.
            var ancestor = node
            for (depth in 0 until MAX_INTERACTIVE_ANCESTORS) {
                ancestor = ancestor.parent ?: break
                val ancestorBounds = Rect().also(ancestor::getBoundsInScreen)
                if (ancestorBounds.contains(targetBounds) && ancestor.isClickable) {
                    return ancestorBounds
                }
            }
            return targetBounds
        }
        return null
    }

    private fun findNode(
        root: AccessibilityNodeInfo,
        signature: Signature,
        ignoreSelected: Boolean,
    ): AccessibilityNodeInfo? {
        val accepts: (AccessibilityNodeInfo) -> Boolean =
            { node -> ignoreSelected || !signature.selectedOnly || node.isSelected }
        return when (signature.matcher) {
            MatcherType.VIEW_ID ->
                root.findAccessibilityNodeInfosByViewId(signature.pattern).firstOrNull(accepts)
            MatcherType.CONTENT_DESC ->
                findDescendant(root) {
                    it.contentDescription?.toString() == signature.pattern && accepts(it)
                }
            MatcherType.TEXT ->
                findDescendant(root) { it.text?.toString() == signature.pattern && accepts(it) }
        }
    }

    private fun findDescendant(
        node: AccessibilityNodeInfo,
        depth: Int = 0,
        predicate: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo? {
        if (predicate(node)) return node
        if (depth >= MAX_DEPTH) return null
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findDescendant(child, depth + 1, predicate)?.let { return it }
        }
        return null
    }

    private companion object {
        // Borne la traversée : les arborescences des réseaux sociaux sont profondes
        // et le service reçoit des dizaines d'événements par seconde.
        const val MAX_DEPTH = 12
        const val MAX_INTERACTIVE_ANCESTORS = 6
    }
}
