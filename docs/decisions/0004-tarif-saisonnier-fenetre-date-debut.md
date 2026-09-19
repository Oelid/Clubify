# 0004 — Un tarif saisonnier s'applique selon la date de début

## Contexte

Le gérant veut pouvoir fixer un prix propre à une période de l'année (par exemple la natation de mars à juin). Il fallait définir quand ce tarif s'applique.

## Décision

Un tarif saisonnier s'applique seulement si la date de début de l'inscription tombe dans sa fenêtre. Il prime alors sur le tarif courant. Un simple chevauchement de l'inscription avec la fenêtre ne suffit pas.

## Raison

Règle simple, prévisible pour l'accueil et testable. Évite les recalculs au milieu d'une inscription.

## Alternatives écartées

- Application au prorata des jours dans la fenêtre : illisible pour le parent, incontrôlable.
- Application dès qu'il y a chevauchement : un tarif de mars s'appliquerait à une inscription de septembre.

## Source dans le cahier des charges

TAR-02, UC3.

## Écarts ou points ouverts

Aucun.

## Date

2026-09-19

## Statut

Acceptée.
