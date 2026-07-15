# Architecture

## Vue d'ensemble
Android natif (Kotlin, Jetpack Compose), architecture en 5 modules Gradle :

- `:core:rules` — moteur de règles **pur Kotlin** (testable sans Android). Entrée : règles actives + contexte (jour, minute, usage). Sortie : `Allow | Warn | Block`. La règle la plus restrictive gagne (blocage > fenêtres > quotidien > hebdomadaire).
- `:core:detection` — catalogue de signatures (`SignatureCatalog`, versionné, JSON) + `FeatureDetector` qui matche l'arborescence d'accessibilité. Matchers : `view_id`, `content_desc`, `text`.
- `:data` — Room (`rule`, `usage_session`, `block_event`, `daily_stat`), `RuleRepository` (entités → domaine), `SignatureRepository` (asset embarqué, remote config prévu phase 2), `ScrollGuardGraph` (localisateur de services, pas de framework DI).
- `:service` — `ScrollGuardAccessibilityService` (cœur du produit), `SessionTracker` (durées à l'horloge **monotone** : anti-triche sur l'heure système), `BlockOverlay` (`TYPE_ACCESSIBILITY_OVERLAY`, pas besoin de SYSTEM_ALERT_WINDOW).
- `:app` — UI Compose. Phase 0 : un seul écran (statut du service, règles de démo).

## Contraintes structurantes
- Le matching des signatures s'exécute sur le thread du service d'accessibilité (les `AccessibilityNodeInfo` ne traversent pas les threads) ; DB et moteur de règles en coroutines `Dispatchers.Default`.
- Le service ne reçoit que les événements des 6 packages cibles (`packageNames` dans `accessibility_config.xml`) — batterie + vie privée.
- iOS (phase 3) : API Screen Time (blocage d'apps entières) + navigateur encadré WKWebView pour le niveau fonctionnalité. Aucun blocage intra-app natif possible sur iOS.

## Schéma de données
Voir les entités dans `data/src/main/kotlin/com/scrollguard/data/db/Entities.kt`. Les événements bruts seront purgés à 90 jours ; agrégats précalculés dans `daily_stat` (agrégateur WorkManager à écrire en phase 1).
