# 0032 — La palette du logo devient la valeur par défaut

## Contexte

La décision 0025 pose deux couches de jetons : ceux de Clubify, neutres, et ceux
de la marque du club par-dessus. Elle précise que les valeurs par défaut de la
marque sont les jetons Clubify — autrement dit, qu'un club sans couleur choisie
reste neutre.

À l'usage, cela donne un écran de paramètres où les deux couleurs affichent
« Non définie », et une application sans aucune identité tant que personne n'a
saisi quoi que ce soit.

## Décision

Les deux règles `club.brand.primary` et `club.brand.secondary` prennent pour
valeurs par défaut la palette du logo : **`#307890`** (bleu acier) et
**`#F08840`** (orange).

Un club qui a sa propre identité les remplace depuis l'écran des paramètres.

## Raison

Un défaut vide oblige chacun à saisir quelque chose avant que le logiciel
ressemble à quoi que ce soit. Ces deux couleurs étaient déjà la palette de
référence du projet ; les poser en défaut ne retire rien, puisqu'elles se
changent en deux clics.

## Alternatives écartées

- **Garder un défaut vide** et laisser l'assistant de démarrage (ADM-02, R9)
  demander les couleurs. Correct pour un deuxième club, inutilement austère pour
  le premier, qui est celui qu'on sert aujourd'hui.
- **Poser la palette à l'amorçage** plutôt qu'en défaut du registre. Revenait à
  tenir la même valeur à deux endroits, et laissait sans couleur tout club créé
  autrement que par la commande d'amorçage.

## Source dans le cahier des charges

ADM-01, section 9.8. Décision 0025.

## Écarts ou points ouverts

- Amende la décision 0025 sur ce seul point : les valeurs par défaut de la
  marque ne sont plus les jetons Clubify.
- **Point de vigilance** : ces couleurs sont celles du club pilote. Tout club
  suivant démarrera donc avec l'identité d'un autre. À reprendre avec ADM-02
  (assistant de démarrage, R9), qui devra demander les couleurs plutôt que de
  les supposer.

## Date

2026-09-20

## Statut

Acceptée
