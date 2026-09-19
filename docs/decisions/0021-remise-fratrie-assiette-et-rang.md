# 0021 — Remise fratrie : assiette et rang

## Contexte

La fiche 0006 fixe le barème (5 % sur le 2e enfant, 10 % sur le 3e) mais laissait ouverts l'assiette du pourcentage et la règle de rang.

## Décision

- Assiette : le prix des inscriptions de l'enfant, hors frais annuels. Les frais annuels restent pleins.
- Rang : l'ordre d'inscription des enfants de la famille sur la saison. Le premier inscrit est plein tarif, le deuxième a 5 %, le troisième 10 %.
- Les deux sont des valeurs par défaut, paramétrables par club.

## Raison

Les frais annuels couvrent l'assurance et l'inscription : ils ne se remisent pas. L'ordre d'inscription est simple à expliquer au parent et à vérifier par l'accueil.

## Alternatives écartées

- Assiette frais annuels compris : remise sur une assurance, incohérent.
- Rang du plus cher au moins cher : résultat plus stable, mais moins lisible au comptoir.
- Rang du moins cher au plus cher : le plus coûteux pour le club.

## Source dans le cahier des charges

TAR-04 et section 11 (version 1.1), 9.8.

## Écarts ou points ouverts

- Le résultat dépend de l'ordre de saisie : si un enfant remisé est résilié, le rang des autres est-il recalculé ? À cadrer dans la feature de tarification.
- Cumul avec une remise manuelle : plafond à fixer (TAR-04).

## Date

2026-09-19

## Statut

Acceptée. Complète 0006.
