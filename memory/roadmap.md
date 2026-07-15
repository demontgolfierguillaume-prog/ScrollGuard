# Roadmap

## Phase 0 — POC de dérisquage (en cours, démarrée le 15/07/2026)
Objectif : prouver que la détection par accessibilité est fiable et que Google Play accepte la déclaration.
- [x] Squelette du projet (5 modules), moteur de règles testé, service d'accessibilité, overlay, signatures embarquées (Instagram Reels/Explorer, YouTube Shorts, TikTok Pour toi)
- [ ] Vérifier les signatures sur appareil réel avec les versions courantes des 3 apps (les identifiants dans `signatures.json` sont indicatifs et doivent être confirmés au layout inspector / uiautomator)
- [ ] Mesurer précision / faux positifs / latence de blocage
- [ ] Soumission interne Google Play + formulaire de déclaration AccessibilityService
- [ ] Déposer la demande d'entitlement Family Controls (Apple) — long délai, à faire tôt
- Critère GO/NO-GO : ≥ 95 % de détection correcte, déclaration Play acceptée

## Phase 1 — MVP Android (10 semaines, entamée le 15/07/2026)
- [x] Tableau de bord (S2), liste apps/fonctionnalités (S3, catalogue complet avec badge « Détection à venir »), éditeur de règles 4 types (S4), stats jour/semaine de base (S6) — livrés en v0.2.0
- [x] Overlay partiel : la barre d'onglets de l'app hôte reste utilisable ; watchdog overlay (retrait si sortie de l'app par le geste accueil)
- [x] Signature debug fixe versionnée → mises à jour d'APK sans désinstallation
- [ ] Onboarding permissions (S1) guidé
- [ ] Bandeau d'avertissement (Warn) avec temps restant
- [ ] Signatures vérifiées pour les fonctionnalités « à venir » (stories, feed, DM, Facebook, X, Snapchat)
- [ ] Protection niveau 0–1 (délai 24 h, PIN), agrégateur quotidien + purge 90 j

## Phase 2 — Android complet (6 semaines)
Facebook, X, Snapchat ; fenêtres horaires dans l'UI ; 4 modes (Travail, Études, Concentration profonde, Temps libre) ; remote config des signatures + télémétrie de fiabilité ; mode strict (niveau 2) et partenaire (niveau 3) ; monétisation.

## Phase 3 — iOS (8 semaines)
Screen Time API (shields, seuils, créneaux), navigateur encadré avec masquage CSS/JS, stats iOS, positionnement transparent sur les limites de la plateforme.

## Référence
Cahier des charges complet (matrice de faisabilité, écrans, flux, risques stores) : artifact Claude « FocusGuard — Cahier des charges » du 15/07/2026.
