# Frontend — règles propres

Squelette. Complété quand la stack sera choisie. Les règles générales sont dans `../CLAUDE.md` et s'appliquent ici.

## Stack

À définir.

## Arborescence

À définir.

## Conventions de nommage

À définir. Contrainte : noms techniques en anglais, tirés de `docs/glossaire.md`.

## Stratégie de tests

À définir. Contrainte : aucun nom réel dans les données de test ni dans les captures d'écran.

## Commandes

À définir.

## Système de design

- Pas de template d'interface acheté (décision 0017). Bibliothèque de composants open source, à définir, et système de design maison (couleurs, typographie, espacements, composants).
- Conçu pour l'accueil au comptoir : parcours d'inscription en moins de 5 minutes, écrans proches des grilles Excel actuelles (risque d'adoption, section 10.I).
- Mobile d'abord pour le coach (appel en 30 secondes, réseau faible) et le parent.
- i18n dès le départ : FR au MVP, arabe droite-à-gauche et anglais ensuite. Le système de design est testé en droite-à-gauche avant la première livraison d'écran.
- Aucun libellé en dur.

## Règles propres

- Le frontend consomme le contrat d'API de `contracts/`, jamais le modèle de données directement.
- Les montants reçus en centimes sont formatés avec la devise du club ; les dates UTC sont affichées dans le fuseau du club.
- Aucune règle métier recalculée côté client : le prix, le solde et les statuts viennent du backend.
- Les données sensibles (santé, CIN) ne s'affichent que sur les écrans qui y sont autorisés (fiche enfant réduite du coach : APC-03).
- Les alertes liées à une dette s'adressent au staff, jamais à l'enfant (UC6).
