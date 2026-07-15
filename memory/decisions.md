# Décisions actées

## 2026-07-15 — Blocage = overlay persistant, jamais d'éjection automatique
Retour de test de Guillaume : l'action « retour » automatique fermait Instagram dès l'ouverture (vécu comme un bug, expérience brutale). Décision : en cas de blocage, on pose un écran de recouvrement qui rend la fonctionnalité inutilisable mais laisse l'app ouverte ; seul le bouton « Revenir » (choix de l'utilisateur) déclenche l'action retour. Corollaire : les signatures de boutons d'onglets doivent exiger l'état `isSelected` (champ `selectedOnly`), sinon l'onglet Reels est « détecté » dès l'écran d'accueil.

## 2026-07-15 — Android d'abord, natif
Le blocage intra-application n'est possible que sur Android (AccessibilityService). iOS n'a aucune API équivalente : on y proposera blocage d'app entière (Screen Time) + navigateur web encadré. On lance donc Android en premier ; iOS en phase 3 avec un positionnement honnête. Kotlin/Swift natifs — pas de Flutter/React Native : le cœur (accessibilité, overlays, extensions Screen Time) est natif de toute façon, et les extensions iOS sont limitées à ~6 Mo de mémoire.

## 2026-07-15 — Signatures de détection versionnées, jamais en dur
Les réseaux sociaux changent/obfusquent leurs identifiants de vues à chaque mise à jour. Les signatures vivent dans `assets/signatures.json` (repli hors ligne) et seront remplacées par un remote config versionné (phase 2). Télémétrie de « signatures muettes » prévue pour détecter les casses en prod.

## 2026-07-15 — Durées mesurées à l'horloge monotone
`SystemClock.elapsedRealtime()` et non l'heure murale : changer la date du téléphone ne réinitialise pas les compteurs (anti-contournement §5 du cahier des charges).

## 2026-07-15 — Pas de framework d'injection pour l'instant
`ScrollGuardGraph` (object singleton) suffit pour 4 dépendances. À réévaluer (Hilt/Koin) si le graphe grossit en phase 1–2.

## 2026-07-15 — Overlay d'accessibilité plutôt que SYSTEM_ALERT_WINDOW
`TYPE_ACCESSIBILITY_OVERLAY` ne demande aucune permission supplémentaire tant que la fenêtre est posée par le service : une permission sensible de moins à justifier auprès de Google Play et de l'utilisateur.

## 2026-07-15 — Vie privée comme argument produit
Traitement 100 % sur l'appareil ; on ne persiste que des identifiants de fonctionnalités et des durées. Jamais de contenu d'écran, de texte de messages ni de captures. Événements bruts purgés à 90 jours.
