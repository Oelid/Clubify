# Contrats d'API

Ce dossier contient les contrats d'API de Clubify. Il est vide pour l'instant : le format du contrat sera choisi avec la stack.

## Rôle

Le contrat est la source de vérité entre le backend et le frontend.

Ordre de travail pour chaque feature :

1. Le contrat est modifié en premier, dans la même livraison que la fiche de feature.
2. Le backend l'implémente.
3. Le frontend le consomme.

## Règles

- Une feature ne modifie que la partie du contrat qui la concerne. Toute modification hors périmètre est demandée avant d'être faite (voir `CLAUDE.md`).
- Tout changement de contrat est reporté dans la fiche de feature (`docs/features/`) et, s'il touche le modèle, dans `docs/modele-donnees.md`.
- Les libellés ne transitent pas par le contrat : le contrat porte des codes, l'interface traduit (i18n).
- Les montants sont exprimés en centimes avec devise ; les dates en UTC.
- Les identifiants de club et de site ne sont jamais fournis par le client : ils viennent du contexte d'authentification.
