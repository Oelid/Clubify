# 0003 — Tarification par durée d'offre, jamais par mois d'entrée

## Contexte

Le club tient six grilles selon le mois d'entrée dans la saison (annuel, décembre, janvier, février, mars), avec un prorata non linéaire calculé à la main et une erreur visible en mars.

## Décision

Le prix dépend de la durée d'offre souscrite : 1, 2, 3, 6, 9 ou 10 mois. Il ne dépend jamais du mois d'entrée.

## Raison

Une grille par durée est stable toute la saison, se vérifie, et supprime le prorata manuel. Décision prise avec le club.

## Alternatives écartées

- Grilles par mois d'entrée (fonctionnement actuel) : un onglet à refaire chaque mois, erreurs.
- Prorata linéaire calculé : ne reproduit pas les prix du club.

## Source dans le cahier des charges

Lecture rapide (point 3), P5, TAR-02, INS-03, OFR-01, UC3, section 11 (grille de mars).

## Écarts ou points ouverts

Aucun. Les captures 07 à 12 décrivent l'ancien fonctionnement : ne pas s'y référer pour le modèle (voir sources/README.md).

## Date

2026-09-19

## Statut

Acceptée.
