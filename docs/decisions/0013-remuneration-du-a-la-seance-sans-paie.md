# 0013 — Rémunération : calcul du dû à la séance, sans paie

## Contexte

Les coachs sont déclarés mais rémunérés à la séance ; le gérant établit la paie lui-même. La paie complète (CNSS, AMO, IR, bulletins) relève d'une réglementation mouvante et d'une responsabilité élevée.

## Décision

L'application calcule le dû de chaque intervenant à partir des séances tenues (fixe, à la séance, primes, avances) et produit un état et un export. Elle ne calcule aucune cotisation et n'émet aucun bulletin.

## Raison

Le lien séances réalisées → montant dû est le besoin réel. Le déclaratif reste au gérant et au comptable (9.2).

## Alternatives écartées

- Module de paie complet : hors périmètre, réglementation mouvante.
- Aucun calcul : le gérant continuerait à compter à la main.

## Source dans le cahier des charges

REM-01 à REM-05 (V2), P10, 9.2, section 11 (coachs déclarés).

## Écarts ou points ouverts

Aucun. Tout le domaine rémunération est en V2 ; le MVP compte seulement les séances tenues (COA-04).

## Date

2026-09-19

## Statut

Acceptée.
