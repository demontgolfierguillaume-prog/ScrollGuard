# Décisions actées

## 2026-09-22 — Masquage préventif du bouton Reels
Quand la règle `instagram.reels` produit une décision de blocage, ScrollGuard localise le bouton Reels dans l'arborescence d'accessibilité, le recouvre avec un petit overlay opaque et intercepte le clic. Un message bref confirme le blocage sans fermer Instagram ni déclencher d'action retour. La détection d'un Reel actif ne déclenche jamais l'overlay plein écran : l'état `isSelected` exposé par Instagram peut produire un faux positif dès l'ouverture de l'application et bloquer toute son interface.

## 2026-09-22 — Aucun bouton de retour vers la fonctionnalité bloquée
L'overlay de blocage ne propose plus « Continuer sur l'appli » : le seul bouton affiché ouvre ScrollGuard. Cela évite de présenter une sortie pouvant être comprise comme un accès aux Reels. L'utilisateur peut toujours quitter l'application hôte avec la navigation système.

## 2026-07-15 — Blocage = overlay persistant, jamais d'éjection automatique
Retour de test de Guillaume : l'action « retour » automatique fermait Instagram dès l'ouverture (vécu comme un bug, expérience brutale). Décision : en cas de blocage, on pose un écran de recouvrement qui rend la fonctionnalité inutilisable mais laisse l'app ouverte. Corollaire : les signatures de boutons d'onglets doivent exiger l'état `isSelected` (champ `selectedOnly`), sinon l'onglet Reels est « détecté » dès l'écran d'accueil.

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
