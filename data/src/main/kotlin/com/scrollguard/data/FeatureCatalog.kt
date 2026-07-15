package com.scrollguard.data

/**
 * Catalogue produit : les réseaux sociaux pris en charge et leurs
 * fonctionnalités restreignables (cahier des charges §3).
 *
 * `detectable = true` signifie qu'une signature de détection vérifiée existe
 * dans signatures.json ; les autres fonctionnalités sont affichées comme
 * « à venir » dans l'interface — jamais de promesse non tenue à l'écran.
 */
object FeatureCatalog {

    data class App(
        val id: String,
        val name: String,
        val packageName: String,
        val features: List<Feature>,
    )

    data class Feature(
        val id: String,          // ex. "instagram.reels" — clé partagée avec signatures.json
        val name: String,
        val detectable: Boolean,
    )

    val apps: List<App> = listOf(
        App(
            id = "instagram", name = "Instagram", packageName = "com.instagram.android",
            features = listOf(
                Feature("instagram.reels", "Reels", detectable = true),
                Feature("instagram.explore", "Explorer / Découvrir", detectable = true),
                Feature("instagram.stories", "Stories", detectable = false),
                Feature("instagram.feed", "Fil d'actualité", detectable = false),
                Feature("instagram.dm", "Messages privés", detectable = false),
                Feature("instagram.shopping", "Shopping", detectable = false),
            ),
        ),
        App(
            id = "tiktok", name = "TikTok", packageName = "com.zhiliaoapp.musically",
            features = listOf(
                Feature("tiktok.foryou", "Flux « Pour toi »", detectable = true),
                Feature("tiktok.live", "Live", detectable = false),
                Feature("tiktok.search", "Recherche", detectable = false),
                Feature("tiktok.dm", "Messages", detectable = false),
            ),
        ),
        App(
            id = "youtube", name = "YouTube", packageName = "com.google.android.youtube",
            features = listOf(
                Feature("youtube.shorts", "Shorts", detectable = true),
                Feature("youtube.home", "Page d'accueil", detectable = false),
                Feature("youtube.comments", "Commentaires", detectable = false),
                Feature("youtube.search", "Recherche", detectable = false),
            ),
        ),
        App(
            id = "facebook", name = "Facebook", packageName = "com.facebook.katana",
            features = listOf(
                Feature("facebook.reels", "Reels", detectable = false),
                Feature("facebook.watch", "Watch", detectable = false),
                Feature("facebook.feed", "Fil d'actualité", detectable = false),
                Feature("facebook.stories", "Stories", detectable = false),
                Feature("facebook.marketplace", "Marketplace", detectable = false),
            ),
        ),
        App(
            id = "x", name = "X (Twitter)", packageName = "com.twitter.android",
            features = listOf(
                Feature("x.foryou", "Flux « Pour vous »", detectable = false),
                Feature("x.videos", "Vidéos", detectable = false),
                Feature("x.trends", "Tendances", detectable = false),
                Feature("x.search", "Recherche", detectable = false),
            ),
        ),
        App(
            id = "snapchat", name = "Snapchat", packageName = "com.snapchat.android",
            features = listOf(
                Feature("snapchat.spotlight", "Spotlight", detectable = false),
                Feature("snapchat.discover", "Discover", detectable = false),
                Feature("snapchat.publicstories", "Stories publiques", detectable = false),
            ),
        ),
    )

    private val byId: Map<String, Feature> =
        apps.flatMap { it.features }.associateBy { it.id }

    fun featureName(featureId: String): String = byId[featureId]?.name ?: featureId
}
