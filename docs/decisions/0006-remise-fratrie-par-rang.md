# 0006 — Remise fratrie paramétrable par rang

## Contexte

Le club accorde une remise aux fratries. Le multi-activités d'un même enfant passe par le forfait, pas par une remise.

## Décision

La remise fratrie est un barème paramétrable par club, par rang d'enfant inscrit sur la saison : par défaut 5 % sur le 2e enfant, 10 % sur le 3e, extensible au-delà. Elle s'applique automatiquement.

## Raison

Confirmé par le club. Le barème varie d'un club à l'autre : il est en configuration (9.8).

## Alternatives écartées

- Remise fixe codée en dur : contraire à l'invariant de configuration par club.
- Remise fratrie et remise multi-activités confondues : le multi-activités relève du forfait (0005).

## Source dans le cahier des charges

TAR-04, UC2, 9.8, section 11 (remises confirmées).

## Écarts ou points ouverts

À trancher avant la feature de tarification :
- assiette : le pourcentage porte-t-il sur le total de l'enfant, frais annuels compris ?
- rang : ordre d'inscription, ou classement du plus cher au moins cher ?
- cumul avec une remise manuelle : plafond à fixer (TAR-04).

## Date

2026-09-19

## Statut

Acceptée, points ouverts à trancher.
