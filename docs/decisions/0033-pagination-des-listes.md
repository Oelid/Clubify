# 0033 — Pagination des listes, bornée à cent lignes

## Contexte

La liste des utilisateurs rendait toutes les lignes du club. L'API acceptait
bien `page` et `size`, mais le service chargeait l'intégralité des comptes,
les triait en mémoire et annonçait ce total comme s'il s'agissait d'une page :
une pagination de façade.

Le problème n'est pas propre aux utilisateurs. Les familles, les adhérents, les
factures et le journal d'audit seront tous plus longs encore.

## Décision

- Toute liste de l'API est paginée **par la base**, qui ne rend que la page
  demandée et compte le total à part.
- Le nombre de lignes par page est une règle configurable du club :
  `ui.page_size`, par défaut **20**.
- **Plafond absolu de 100 lignes**, qu'aucun réglage de club ne dépasse. Une
  taille demandée au-delà est refusée par le contrat, avec un message lisible ;
  un réglage de club au-delà est ramené à 100.
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
- **Ramener silencieusement une taille démesurée à cent** plutôt que la refuser.
  Le contrat annonce la borne ; la respecter en silence masquerait une erreur
  d'appel au lieu de la signaler. Le réglage de club, lui, est borné sans bruit :
  il n'y a personne à prévenir au moment où il s'applique.
- **Un défilement infini.** Illisible pour une liste qu'on imprime ou qu'on
  parcourt à la recherche d'un nom.

## Source dans le cahier des charges

Section 9.8 (règles configurables), section 6 de `backend/CLAUDE.md`
(pagination par `page`, `size`, `sort`).

## Écarts ou points ouverts

- Le tri est aujourd'hui fixe — nom, puis prénom. Le paramètre `sort` du contrat
  reste à câbler quand un écran en aura besoin.
- Les filtres `role` et `active` de `listUsers` sont déclarés au contrat mais
  pas encore appliqués : à faire avec l'écran qui les proposera.

## Date

2026-09-20

## Statut

Acceptée
