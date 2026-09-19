# Feature : <nom>

Copier ce fichier sous `docs/features/<identifiant>-<nom-court>.md`. Une fiche par feature. La fiche fait foi une fois validée ; tout écart avec le cahier des charges est consigné dans `docs/decisions/`. Les rubriques se remplissent dans l'ordre des étapes de `docs/processus-feature.md`.

## Identifiant(s) du backlog et source

Identifiants de la section 7 du cahier des charges (ex. FAC-08) et sections ou cas d'utilisation concernés (ex. UC4, UC5).

## Objectif

Deux phrases : ce que la feature permet, et pour qui.

## Acteurs

ADM, GER, COA, PAR, SYS. Pour chacun, ce qu'il fait.

## Règles métier

Une règle par ligne, numérotée. Chaque règle cite sa source. Aucune règle inventée : en cas de doute, « À confirmer » et la question.

## Critères d'acceptation

Cas concrets, un par règle au minimum : situation de départ, action, résultat attendu. Montants et dates fictifs, jamais de nom réel.

## Benchmark

Étape 2 du processus. Trois à cinq applications comparables, sur ce sujet précis. Tableau : écart observé, intérêt pour le club pilote, recommandation (intégrer / backlog / écarter), décision d'Omar.

## Hors périmètre

Ce que la feature ne fait pas, même si le sujet est proche.

## Dépendances

Features ou lots requis avant celle-ci.

## Plan d'implémentation

Étape 3. Étapes ordonnées, écrans, ordre contrat → backend → frontend.

## Impacts et régressions

Étape 3. Un niveau par critère (majeur / maîtrisé) et un niveau global, avec les critères de `docs/processus-feature.md`. Puis une ligne par régression possible : feature livrée concernée, cas concret, résultat attendu, test qui la couvre.

## Impact sur le modèle de données

Entités créées ou modifiées. Reporter dans `docs/modele-donnees.md`.

## Impact sur le contrat d'API

Endpoints ou messages créés ou modifiés. Reporter dans `contracts/`.

## Paramètres configurables par club

Règles de la section 9.8 touchées, avec leur valeur par défaut.

## Points de sécurité et données sensibles

Données de santé, CIN, argent, remises, dérogations : accès par rôle, audit, chiffrement.

## Statut

À cadrer / cadrée / en cours / terminée / reportée, avec la date.
