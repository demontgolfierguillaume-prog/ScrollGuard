package com.scrollguard.core.detection

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Signature de détection d'une fonctionnalité dans l'interface d'une app cible.
 *
 * Les signatures ne sont JAMAIS codées en dur : elles sont chargées depuis
 * `assets/signatures.json` (repli hors ligne) puis remplacées par le remote
 * config quand une version plus récente est disponible (phase 2).
 */
@Serializable
data class Signature(
    val feature: String,                     // ex. "instagram.reels"
    val matcher: MatcherType,
    val pattern: String,                     // ex. "com.instagram.android:id/clips_tab"
    val locales: List<String> = emptyList(), // pour les matchers dépendant de la langue
    /**
     * Si true, le nœud ne compte que s'il est `isSelected` (onglet actif).
     * Indispensable pour les boutons d'onglets : le bouton « Reels » est présent
     * dès l'écran d'accueil d'Instagram, mais n'est sélectionné que quand
     * l'utilisateur est réellement dans les Reels.
     */
    val selectedOnly: Boolean = false,
)

@Serializable
enum class MatcherType {
    @SerialName("view_id") VIEW_ID,
    @SerialName("content_desc") CONTENT_DESC,
    @SerialName("text") TEXT,
}

/** Catalogue versionné, groupé par package Android cible. */
@Serializable
data class SignatureCatalog(
    val version: Int,
    val packages: Map<String, List<Signature>>,
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(raw: String): SignatureCatalog = json.decodeFromString(serializer(), raw)

        val EMPTY = SignatureCatalog(version = 0, packages = emptyMap())
    }
}
