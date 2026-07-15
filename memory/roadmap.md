# Roadmap

## Phase 0 — POC de dérisquage (en cours, démarrée le 15/07/2026)
Objectif : prouver que la détection par accessibilité est fiable et que Google Play accepte la déclaration.
- [x] Squelette du projet (5 modules), moteur de règles testé, service d'accessibilité, overlay, signatures embarquées (Instagram Reels/Explorer, YouTube Shorts, TikTok Pour toi)
- [ ] Vérifier les signatures sur appareil réel avec les versions courantes des 3 apps (les identifiants dans `signatures.json` sont indicatifs et doivent être confirmés au layout inspector / uiautomator)
- [ ] Mesurer précision / faux positifs / latence de blocage
- [ ] Soumission interne Google Play + formulaire de déclaration AccessibilityService
- [ ] Déposer la demande d'entitlement Family Controls (Apple) — long délai, à faire tôt
- Critère GO/NO-GO : ≥ 95 % de détection correcte, déclaration Play acceptée

## Phase 1 — MVP Android (10 semaines)
Onboarding permissions (S1), tableau de bord (S2), liste apps/fonctionnalités (S3), éditeur de règles (S4), bandeau d'avertissement (Warn), stats de base (S6), protection niveau 0–1 (délai 24 h, PIN), watchdog du service, agrégateur quotidien.

## Phase 2 — Android complet (6 semaines)
Facebook, X, Snapchat ; fenêtres horaires dans l'UI ; 4 modes (Travail, Études, Concentration profonde, Temps libre) ; remote config des signatures + télémétrie de fiabilité ; mode strict (niveau 2) et partenaire (niveau 3) ; monétisation.

## Phase 3 — iOS (8 semaines)
Screen Time API (shields, seuils, créneaux), navigateur encadré avec masquage CSS/JS, stats iOS, positionnement transparent sur les limites de la plateforme.

## Référence
Cahier des charges complet (matrice de faisabilité, écrans, flux, risques stores) : artifact Claude « FocusGuard — Cahier des charges » du 15/07/2026.
