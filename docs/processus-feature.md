# Processus d'implémentation d'une feature

Six étapes, une feature à la fois. Chaque étape produit un livrable et se termine par un point d'arrêt : Omar valide avant de passer à la suivante. Aucun code avant l'étape 4.

## Étape 1 — Cadrage

Lire le cahier des charges, les fiches de `docs/decisions/` et les fiches de features déjà livrées qui touchent le sujet.
Rédiger la fiche de feature depuis `docs/features/_gabarit.md` : identifiants du backlog, règles métier sourcées, critères d'acceptation en cas concrets.
Lister les « À confirmer » et poser les questions.

Livrable : fiche à l'état « à cadrer ». Point d'arrêt : réponses aux questions.

## Étape 2 — Complétude et benchmark

Complétude : vérifier que la fiche couvre tous les identifiants du backlog et tous les cas d'utilisation de la section 8 rattachés au sujet. La complétude s'apprécie par rapport au cahier des charges, pas par rapport au marché.

Benchmark : regarder ce que proposent trois à cinq applications comparables sur ce sujet précis (Jackrabbit Class, iClassPro, Gymdesk, Glofox, GymMaster, ou d'autres selon le sujet). Le benchmark est borné au sujet de la feature ; il sert à repérer un oubli ou une meilleure manière de faire, pas à définir le périmètre.

Pour chaque écart relevé, trois informations : ce que l'application fait en plus ou autrement ; l'intérêt pour le club pilote ; une recommandation parmi trois issues :
- intégrer dans la feature ;
- mettre au backlog, avec un identifiant et une version cible ;
- écarter, avec la raison.

Un complément n'entre dans la feature que s'il sert le club pilote et le lot en cours. Sinon il va au backlog, pas dans le code.

Livrable : rubrique « Benchmark » de la fiche. Point d'arrêt : Omar tranche chaque écart.

## Étape 3 — Plan d'implémentation et analyse d'impact

Plan : étapes ordonnées ; contrat d'API touché ; entités du modèle créées ou modifiées ; paramètres configurables par club avec valeur par défaut ; écrans.

Analyse d'impact : un niveau par critère, puis un niveau global.

Impact **majeur** si au moins un des cas suivants :
- migration de données existantes ;
- modification d'un contrat d'API déjà consommé ;
- changement d'un calcul d'argent : prix, solde, ventilation, remise, taxe ;
- changement d'un invariant du `CLAUDE.md` ;
- effet sur une feature déjà livrée ;
- nouvelle dépendance.

Impact **maîtrisé** sinon : ajout sans modification de l'existant ; nouvelle entité isolée ; nouveau paramètre dont la valeur par défaut reproduit le comportement actuel.

Régressions : pour chaque impact, lister les régressions possibles. Une régression se formule ainsi : quelle feature livrée pourrait casser, sur quel cas concret, avec quel résultat attendu. Chaque régression listée devient un test à l'étape 4.

Livrable : rubriques « Plan d'implémentation » et « Impacts et régressions » de la fiche. Point d'arrêt : Omar valide le plan ; si l'impact est majeur, il valide explicitement chaque point majeur.

## Étape 4 — Tests d'abord

Avant le code, écrire :
- les tests des règles métier de la fiche, côté backend et côté frontend selon la règle ;
- un test de non-régression pour chaque régression identifiée à l'étape 3 ;
- un test d'isolation par club et un test d'audit quand la feature touche à l'argent, aux remises, aux dérogations ou aux données sensibles.

Ces tests échouent au départ. Aucun nom réel, aucune donnée issue des captures.

Livrable : tests en place, tableau critère d'acceptation → test.

## Étape 5 — Implémentation

Dans cet ordre : contrat d'API, backend, frontend.
Une feature à la fois, pas de refonte hors périmètre.
Toute découverte en cours de route qui change le plan ou le niveau d'impact remonte à Omar avant d'être codée.

Livrable, dans la même livraison : code, tests verts, contrat d'API, `docs/modele-donnees.md`, fiche de feature.

## Étape 6 — Livraison

Récapitulatif remis à Omar :
- ce qui est fait ;
- ce qui reste « À définir » ou « À confirmer » ;
- les écarts avec le cahier des charges, consignés dans `docs/decisions/` ;
- les régressions couvertes et par quel test ;
- la ligne de `docs/roadmap.md` passée à « terminée » avec la date.

La feature n'est terminée que sur validation d'Omar (définition de « terminé », `CLAUDE.md` section 6).
