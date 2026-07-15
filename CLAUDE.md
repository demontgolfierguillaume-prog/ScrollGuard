# ScrollGuard — Contexte pour Claude

## À lire en priorité au début de chaque session
- `memory/architecture.md` — structure technique, choix de stack
- `memory/roadmap.md` — objectifs et priorités en cours
- `memory/decisions.md` — décisions actées (et pourquoi)

## Règles de travail
- Le dépôt GitHub (`demontgolfierguillaume-prog/ScrollGuard`) est la source de vérité.
- Réutiliser le code existant ; respecter l'architecture en modules et les conventions (packages `com.scrollguard.*`, français pour la doc et les textes UI).
- Le moteur de règles (`:core:rules`) reste pur Kotlin, sans dépendance Android, et toute modification s'accompagne de tests unitaires.
- Les signatures de détection ne sont jamais codées en dur : uniquement `data/src/main/assets/signatures.json` (puis remote config).
- Avant une action risquée (suppression de fichier, secrets, `git push --force`, migration de base, publication store), s'arrêter et demander confirmation.
- Mettre à jour le fichier `memory/` concerné quand une décision ou une info structurante change.
