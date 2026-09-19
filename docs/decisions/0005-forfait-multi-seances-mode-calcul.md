# 0005 — Mode de calcul du forfait multi-séances

## Contexte

Le formulaire propose une « formule annuelle, 2 activités par semaine ». La colonne « 2 séances » des grilles est un prix de forfait réduit, pas un doublement (judo 5 800 au lieu de 7 800). Il fallait une règle pour les combinaisons d'activités différentes.

## Décision

Ordre de calcul du prix d'un forfait :
1. prix explicite de la combinaison s'il existe dans la grille ;
2. sinon prix « 1 séance » de l'activité la plus chère, plus le supplément de séance additionnelle de chaque autre séance ;
3. « cumul diminué d'un pourcentage » reste un mode configurable par club, non retenu par défaut.

Le supplément est défini par activité et par formule (données de configuration, jamais dans le code).

## Raison

Le mode 2 reproduit exactement la grille actuelle (natation + boxe en annuel : 4 500 + 1 900 = 6 400 DH). Un pourcentage unique ne le peut pas : la réduction implicite vaut 25,6 % pour le judo et 23,3 % pour la natation.

## Alternatives écartées

- Addition des prix unitaires : contredit le formulaire et les grilles.
- Remise multi-activités en pourcentage comme seul mode : ne reproduit pas les prix.
- Moteur de tarification générique : trop coûteux pour un MVP (9.1).

## Source dans le cahier des charges

OFR-02, TAR-01, 7.31, section 11 (forfait « 2 activités / semaine »).

## Écarts ou points ouverts

- La section 11 présente le mode 2 comme une règle « proposée, à valider » ; OFR-02 le pose comme mode par défaut. Cette décision le fige.
- L'affichage de l'économie par rapport au cumul brut se fait comme information, jamais comme remise saisie (OFR-02).

## Date

2026-09-19

## Statut

Acceptée.
