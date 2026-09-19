# 0008 — Capacité saisie par groupe, jamais codée en dur

## Contexte

Aujourd'hui la capacité d'un groupe est implicite : le nombre de lignes de la grille Excel. En piscine, le club confirme 6 enfants.

## Décision

La capacité est obligatoire et saisie groupe par groupe, modifiable en cours de saison. Une valeur par défaut peut être héritée de l'activité ou du lieu. Le 6 de la piscine est une valeur de configuration du club pilote, jamais une constante du code.

## Raison

La capacité varie selon l'activité, le lieu et le coach. C'est une règle configurable par club (9.8).

## Alternatives écartées

- Capacité par activité seulement : deux groupes de la même activité peuvent différer.
- Constante 6 dans le code : contraire à l'invariant.

## Source dans le cahier des charges

GRP-01, GRP-03, 9.8, section 11 (capacité réelle par groupe).

## Écarts ou points ouverts

Aucun.

## Date

2026-09-19

## Statut

Acceptée.
