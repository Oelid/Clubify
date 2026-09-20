# 0033 — Pagination des listes, bornée à cent lignes

## Contexte

La liste des utilisateurs rendait toutes les lignes du club. L'API acceptait
bien `page` et `size`, mais le service chargeait l'intégralité des comptes,
les triait en mémoire et annonçait ce total comme s'il s'agissait d'une page :
une pagination de façade.

Le problème n'est pas propre aux utilisateurs. Les familles, les adhérents, les
factures et le journal d'audit seront tous plus longs encore.

## Décision

**Règle générale, valable pour toute liste rendue par l'API, aujourd'hui comme
dans les features à venir.**

- Toute liste de l'API est paginée **par la base**, qui ne rend que la page
  demandée et compte le total à part. Jamais de `findAll()` découpé en
  mémoire : charger mille familles pour en afficher vingt coûte autant que de
  les afficher toutes.
- Le nombre de lignes par page est une règle configurable du club :
  `ui.page_size`, par défaut **20**.
- **Plafond absolu de 100 lignes**, qu'aucun réglage de club ni aucun appelant
  ne dépasse. Le serveur **borne sans refuser** : au-delà de 100, la taille est
  ramenée à 100 ; en dessous de 1, à 1 ; une page négative devient la première ;
  une taille absente prend celle du club.
- La borne est posée à un seul endroit, `PaginationPolicy`, que tout chemin de
  lecture traverse. Un appel écrit à la main ne peut donc pas la contourner :
  il ne peut pas contourner ce chemin.
- L'interface propose **20, 30, 50, 100**. Changer la taille ramène à la
  première page.
- Le club annonce sa taille de page dans `ClubSummary`, pour que l'interface
  ouvre les listes à la bonne taille sans lire les réglages.

## Raison

Une page de mille lignes coûte autant au serveur qu'au navigateur, et personne
ne lit mille lignes. La borne protège les deux, y compris d'un appel direct à
l'API : elle ne dépend pas de la bonne volonté du client.

Laisser le nombre de lignes au club plutôt que le fixer suit la règle générale
de la section 9.8 : c'est un confort d'affichage, qui dépend de l'écran du
comptoir et de l'habitude de chacun.

Cent, parce qu'au-delà l'affichage d'un tableau devient perceptiblement lent sur
un poste modeste, et que la valeur ronde se retient.

## Alternatives écartées

- **Charger toute la liste et paginer à l'écran.** C'est ce que faisait le code ;
  ça tient tant qu'un club a vingt comptes, et pas au-delà.
- **Fixer la taille en dur.** Le comptoir et le bureau n'ont ni le même écran ni
  le même usage.
- **Refuser une taille démesurée** plutôt que la ramener dans les bornes.
  Essayé, puis écarté : un refus oblige chaque appelant à connaître la borne et
  à la respecter, alors que c'est au serveur de se protéger. Borner donne
  toujours une réponse utilisable, et la taille réellement appliquée figure dans
  la réponse — un client attentif la voit.

  Conséquence assumée : un client qui demande 5 000 lignes en reçoit 100 sans
  avertissement. Il lui appartient de lire `page.size`, que la réponse porte.
- **Un défilement infini.** Illisible pour une liste qu'on imprime ou qu'on
  parcourt à la recherche d'un nom.

## Source dans le cahier des charges

Section 9.8 (règles configurables), section 6 de `backend/CLAUDE.md`
(pagination par `page`, `size`, `sort`).

## Comment la règle tient

Une règle que seule la discipline fait respecter finit par être oubliée, et
l'oubli ne se voit qu'en production, le jour où un club a mille familles.
`PaginationTest` la rend vérifiable, à trois niveaux :

| Contrôle | Ce qu'il empêche |
| --- | --- |
| Aucun contrôleur ne dépend de `PageRequest` | Fabriquer une pagination à la main, donc contourner la borne |
| Tout point d'entrée rendant une page appelle `PaginationPolicy` | Rendre une liste sans borne |
| Toute opération du contrat rendant une page déclare `page` et `size` | Livrer une liste que le client ne peut pas parcourir |

Chacun a été éprouvé en le faisant échouer. Le troisième se vérifie sur un
contrat fabriqué pour l'occasion : abîmer le vrai contrat arrêterait la
compilation avant le test, le générateur imposant d'implémenter toute opération
déclarée.

## Écarts ou points ouverts

- Le tri est aujourd'hui fixe — nom, puis prénom. Le paramètre `sort` du contrat
  reste à câbler quand un écran en aura besoin.
- Les filtres `role` et `active` de `listUsers` sont déclarés au contrat mais
  pas encore appliqués : à faire avec l'écran qui les proposera.

## Date

2026-09-20

## Statut

Acceptée
