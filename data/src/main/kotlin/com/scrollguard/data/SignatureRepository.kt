package com.scrollguard.data

import android.content.Context
import com.scrollguard.core.detection.SignatureCatalog

/**
 * Fournit le catalogue de signatures de détection.
 *
 * Aujourd'hui : uniquement le catalogue embarqué dans `assets/signatures.json`.
 * Phase 2 : téléchargement d'un catalogue plus récent (remote config), en
 * gardant l'embarqué comme repli hors ligne — voir memory/roadmap.md.
 */
class SignatureRepository(private val context: Context) {

    fun load(): SignatureCatalog =
        runCatching {
            context.assets.open(BUNDLED_ASSET).bufferedReader().use { reader ->
                SignatureCatalog.parse(reader.readText())
            }
        }.getOrDefault(SignatureCatalog.EMPTY)

    private companion object {
        const val BUNDLED_ASSET = "signatures.json"
    }
}
