# ScrollGuard

Application Android (puis iOS) de gestion de l'attention : elle bloque ou limite des **fonctionnalités précises** à l'intérieur des réseaux sociaux (Reels Instagram, Shorts YouTube, flux « Pour toi » TikTok…), plutôt que les applications entières.

**État actuel : Phase 0 — prototype de validation.** Objectif : prouver la fiabilité de la détection par service d'accessibilité et le blocage par superposition, avant d'investir dans le produit complet.

## Comment ça marche (Android)

1. Un **service d'accessibilité** (`:service`) reçoit les événements d'interface des seules apps cibles (filtre `packageNames`).
2. Le **détecteur** (`:core:detection`) compare l'écran courant à un catalogue de **signatures** (identifiants de vues, descriptions) chargé depuis `assets/signatures.json` — jamais codées en dur, remplaçables par remote config en phase 2.
3. Le **moteur de règles** (`:core:rules`, Kotlin pur, testé unitairement) décide : autoriser, avertir, ou bloquer (blocage complet, limite quotidienne/hebdomadaire, fenêtres horaires — la règle la plus restrictive gagne).
4. En cas de blocage : **overlay plein écran** (`TYPE_ACCESSIBILITY_OVERLAY`) + action « retour », et journalisation pour les statistiques.

## Modules

| Module | Rôle | Dépendances |
|---|---|---|
| `:core:rules` | Moteur de règles pur Kotlin (aucune dépendance Android), tests unitaires | — |
| `:core:detection` | Modèle de signatures + `FeatureDetector` (matching sur l'arborescence d'accessibilité) | `:core:rules` |
| `:data` | Room (règles, sessions, événements de blocage, agrégats), référentiels, catalogue embarqué | `:core:rules`, `:core:detection` |
| `:service` | `ScrollGuardAccessibilityService`, `SessionTracker` (horloge monotone), `BlockOverlay` | tous |
| `:app` | UI Jetpack Compose (phase 0 : statut du service + règles de démo) | tous |

## Construire et tester

Prérequis : Android Studio (Ladybug ou plus récent) avec JDK 17.

```bash
# Ouvrir le projet dans Android Studio (le wrapper Gradle est généré au premier import),
# ou en ligne de commande si Gradle est installé :
gradle wrapper && ./gradlew :core:rules:test   # tests du moteur de règles
./gradlew :app:assembleDebug                   # APK de debug
```

Test sur appareil (phase 0) :
1. Installer l'APK, ouvrir ScrollGuard → « Insérer les règles de démo ».
2. Activer le service dans **Réglages → Accessibilité → ScrollGuard**.
3. Ouvrir Instagram → onglet Reels : l'écran de blocage doit apparaître. Ouvrir YouTube → Shorts : autorisé 15 min/jour, puis bloqué.

## Points d'attention permanents

- **Signatures fragiles** : chaque mise à jour d'Instagram/YouTube/TikTok peut casser la détection. Mettre à jour `data/src/main/assets/signatures.json` (puis remote config en phase 2) et vérifier sur appareil.
- **Google Play** : l'usage du service d'accessibilité exige le formulaire de déclaration + divulgation dans l'app (texte dans `service/src/main/res/values/strings.xml`).
- **Vie privée** : traitement 100 % sur l'appareil ; on ne stocke que des identifiants de fonctionnalités et des durées — jamais de contenu d'écran.

## Feuille de route

Voir `memory/roadmap.md`. Résumé : Phase 0 (POC détection) → Phase 1 (MVP Android : onboarding, éditeur de règles, stats) → Phase 2 (6 apps, modes, remote config, anti-contournement) → Phase 3 (iOS via API Screen Time + navigateur encadré).
