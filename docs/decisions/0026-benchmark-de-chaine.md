# 0026 — Benchmark de chaîne avant la première feature métier

## Contexte

Le processus prévoit un benchmark à l'étape 2 de chaque feature, explicitement borné au sujet de cette feature. Il sert à repérer un oubli ou une meilleure manière de faire sur un point précis, et il tombe avant le plan et avant le code : sur une feature donnée, il n'y a donc rien à défaire.

Il reste un angle mort : ce benchmark ne regarde jamais la chaîne complète — famille, inscription, facture, paiement, caisse — alors que c'est là que se jouent les choix de modèle irréversibles. Un écart structurant découvert au moment de l'encaissement (F09) obligerait à reprendre la facture et le solde (F08), déjà livrés.

## Décision

Un benchmark unique porte sur la chaîne métier complète, une seule fois, avant la première feature métier (F02). Il examine les structures, pas les écrans ni les fonctionnalités : comment les applications comparables relient une famille à une inscription, à une facture et à un paiement, comment elles ventilent un règlement, comment elles suivent un reste à payer.

Chaque écart suit les trois issues de l'étape 2 : intégrer au modèle, mettre au backlog avec un identifiant et une version cible, écarter avec la raison. Toute conséquence est reportée dans `docs/modele-donnees.md`, et dans `docs/decisions/` si elle s'écarte du cahier des charges.

Le socle (F01) ne porte aucune entité métier : il peut être cadré et livré avant ce benchmark.

Livrable : une note de benchmark de chaîne. Point d'arrêt : Omar tranche chaque écart.

## Raison

Les benchmarks par feature protègent chaque feature prise isolément, jamais leurs relations. La section 9.6 du cahier des charges et le critère d'impact majeur de l'étape 3 sont des parades, pas des préventions : ils traitent la découverte tardive par migration et tests de non-régression, à un coût élevé.

Un passage unique et borné, fait avant que la première entité métier ne soit écrite, coûte un point d'arrêt et supprime le cas le plus cher.

## Alternatives écartées

- Ne rien ajouter : la section 9.6 et le critère d'impact majeur suffisent en théorie ; en pratique, ils constatent la refonte au lieu de l'éviter.
- Cadrer d'un bloc les six features de la colonne vertébrale (F02, F04, F05, F07, F08, F09) avant d'en implémenter une : vision complète du modèle, mais six fiches à valider avant le premier code, donc un délai long avant de voir quelque chose tourner.
- Élargir le benchmark de chaque feature au-delà de son sujet : contredit le processus, qui le borne volontairement, et rallonge chaque étape 2.

## Source dans le cahier des charges

Section 4 (benchmark global déjà réalisé), 4.2, 6.1 (chaîne métier principale), 9.5, 9.6 (à concevoir dès le départ pour éviter une refonte) ; `docs/processus-feature.md`, étapes 2 et 3.

## Écarts ou points ouverts

- `docs/processus-feature.md` est complété d'une section décrivant ce benchmark avant l'étape 1 ; le reste du processus est inchangé.
- Le périmètre exact des applications comparables reprend celles du cahier : Jackrabbit Class et iClassPro d'abord, puis Gymdesk, Glofox et GymMaster.
- Ce benchmark n'est pas une feature : il n'apparaît pas dans `docs/decoupage-features.md` ni dans le classeur de suivi.

## Date

2026-09-20

## Statut

Acceptée.
