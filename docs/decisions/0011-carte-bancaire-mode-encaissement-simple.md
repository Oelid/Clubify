# 0011 — Carte bancaire : simple mode d'encaissement au MVP

## Contexte

Le club n'a pas de TPE et n'a choisi aucun acquéreur. Le marché marocain de l'acquisition s'ouvre (fin du monopole du CMI), mais aucun prestataire n'est retenu.

## Décision

Au MVP, la carte est un mode d'encaissement saisi au comptoir (« carte TPE »), sans intégration technique. La couche prestataire de paiement reste abstraite ; le lien de paiement en ligne vient en V2, quand le club aura choisi son acquéreur.

## Raison

Aucun TPE aujourd'hui ; intégrer un prestataire non choisi serait du travail perdu. L'abstraction préserve la suite.

## Alternatives écartées

- Intégration d'un acquéreur dès le MVP : aucun acquéreur choisi, coût sans usage.
- Ignorer la carte : le club l'envisage plus tard, le mode doit exister dans les encaissements.

## Source dans le cahier des charges

FAC-03, FAC-10, INT-02, section 5 (Paiement par carte et en ligne), section 11 (TPE).

## Écarts ou points ouverts

Aucun.

## Date

2026-09-19

## Statut

Acceptée.
