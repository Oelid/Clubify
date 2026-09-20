# Suivi des tests fonctionnels — source

Ce fichier décrit, en langage métier, **ce qu'on vérifie depuis les écrans**. Il est
la source du classeur `docs/suivi-tests.xlsx`, régénéré par
`tools/generer-suivi-tests.py`. Le classeur ne se saisit pas : il serait écrasé.

Un scénario porte un identifiant (`S01`, `S02`…). Le test Playwright qui le joue
reprend cet identifiant dans son titre : c'est ce qui relie la description
ci-dessous à un résultat d'exécution réel. Un scénario sans test automatisé se
déroule à la main et porte « Manuel ».

**Ce fichier ne suit pas les tests techniques** — tests unitaires, tests
d'intégration de l'API, tests d'architecture. Ils sont le filet du
développement, pas la recette de la feature ; le classeur n'en donne que le
compte, dans la feuille de synthèse.

## Comment lancer la recette

Les scénarios jouent l'application réelle : il faut donc un backend, une base et
un club amorcé.

1. `cd backend && ./mvnw spring-boot:run "-Dspring-boot.run.arguments=--seed-club --club=Club-de-recette --admin=recette@exemple.test --password=<mot de passe>"`
2. Activer une fois le second facteur de ce compte et **noter son secret** (il
   s'affiche dans l'URI du QR).
3. Renseigner l'environnement, puis lancer :

```
E2E_ADMIN_EMAIL=recette@exemple.test
E2E_ADMIN_PASSWORD=<mot de passe>
E2E_ADMIN_TOTP_SECRET=<secret noté à l'activation>
cd frontend && npm run e2e
```

Sans ces trois variables, la suite s'arrête en le disant, au lieu d'échouer sans
raison lisible. Le secret ne vit que dans l'environnement de recette : aucun
secret n'est versionné (`CLAUDE.md` §5).

## Rattachement des suites aux features

| Motif de chemin | Feature |
| --- | --- |
| `frontend/e2e/f01-` | F01 |

## Scénarios fonctionnels

Une ligne par scénario. « Critères » renvoie aux critères d'acceptation de la
fiche de feature, pour que la recette et la fiche ne divergent pas.

| ID | Feature | Scénario | Ce qu'on vérifie | Critères | Mode |
| --- | --- | --- | --- | --- | --- |
| S01 | F01 | Première connexion de l'administrateur | Mot de passe accepté, second facteur imposé avec QR et codes de secours, arrivée dans l'application | C5, C6b | Automatisé |
| S02 | F01 | Mot de passe erroné | Message lisible en français, aucun accès, aucune trace du mot de passe | C5, C36 | Automatisé |
| S03 | F01 | Cinq échecs de suite verrouillent le compte | Le bon mot de passe est refusé pendant le verrouillage, avec un message qui le dit | C10b | Automatisé |
| S04 | F01 | Connexion d'un compte déjà inscrit au second facteur | Le code est demandé ; ni QR ni nouveaux codes de secours ne sont produits | C6 | Automatisé |
| S05 | F01 | Le gérant modifie l'identité du club | Le téléphone est normalisé en +212, la modification est visible au journal | C28, C37, C16 | Automatisé |
| S06 | F01 | Nom du club laissé vide | Refus avec un message en français, rien n'est enregistré | C36 | Automatisé |
| S07 | F01 | Consulter la liste des utilisateurs | Rôle, second facteur, statut et dernière connexion lisibles, dates au fuseau du club | C29 | Automatisé |
| S08 | F01 | L'accueil consulte sans modifier, et n'atteint pas le journal | Champs en lecture seule, aucune action proposée, journal absent du menu et refusé par l'adresse directe | C11, C19, C32 | Automatisé |
| S09 | F01 | Filtrer le journal d'audit | Le filtre par action réduit la liste ; la remise à zéro la rétablit | C19b | Automatisé |
| S10 | F01 | Se déconnecter | Le retour en arrière ne rouvre pas la session | C8 | Automatisé |
| S11 | F01 | Recharger la page | La session tient sans ressaisir le mot de passe, par le seul cookie | C8c | Automatisé |
| S12 | F01 | Lire les règles configurables du club | Chaque règle affiche sa valeur, son origine et sa source documentée | C31 | Automatisé |
| S13 | F01 | Scanner le QR avec une vraie application d'authentification | Le code produit par le téléphone est accepté | C6 | Manuel |
| S14 | F01 | Lire les écrans sur un téléphone, en portrait | Rien n'est coupé, la navigation reste utilisable | — | Manuel |
| S15 | F01 | Valider les libellés FR avec l'accueil et le gérant | Le vocabulaire est celui du club, pas celui du logiciel | — | Manuel |
| S16 | F01 | Imprimer la liste des utilisateurs | La feuille imprimée est lisible et tient sur la page | — | Manuel |
| S17 | F01 | Le gérant entre sans second facteur et voit un rappel permanent | Rien ne bloque, le rappel est sur chaque écran et ne se ferme pas | C6g | Automatisé |
| S18 | F01 | L'accueil n'est jamais invitée à activer un second facteur | Le rappel ne s'adresse qu'aux rôles sensibles | C6k | Automatisé |
| S19 | F01 | Le gérant change la couleur du club | La couleur s'applique à l'écran dès l'enregistrement et survit au rechargement | C28b | Automatisé |

## Recette manuelle — passages

Saisi ici, à la main, après chaque passage d'un scénario « Manuel ».

| ID | Qui | Date | Résultat | Remarque |
| --- | --- | --- | --- | --- |

## Anomalies trouvées

Une ligne par défaut trouvé après l'écriture du code, avec le test qui l'empêche
de revenir. Une anomalie sans test de non-régression n'est pas close.

| Feature | Anomalie | Trouvée par | Conséquence si elle revenait | Test qui la ferme | État |
| --- | --- | --- | --- | --- | --- |
| F01 | L'écran de second facteur déduisait l'étape de l'existence d'un jeton, au lieu de suivre l'issue du backend | Parcours en navigateur | Le secret et les codes de secours d'un compte déjà inscrit régénérés en silence ; son téléphone cesse de fonctionner | S04 | Close |
| F01 | Le journal d'audit n'attribuait aucun auteur aux connexions | Parcours en navigateur | La colonne que le gérant regarde en premier reste vide (SEC-04) | S05 | Close |
| F01 | Tolérance d'une seconde sur la borne de révocation des jetons | Suite technique devenue instable | Un jeton survit à la fermeture des sessions demandée par le gérant | Test technique `UsersApiTest.c8b` | Close |
| F01 | L'amorçage écrivait hors transaction : un club créé sans trace d'audit | Vérification en base | Une action sensible sans trace, contre la règle 17 | À écrire | Ouverte |
| F01 | Les contrôleurs manipulaient des entités JPA | Test d'architecture | Relations paresseuses et cycle de vie transactionnel dans la couche web | Test technique `ArchitectureTest` | Close |
| F01 | Quinze codes d'erreur du backend n'avaient aucune traduction : l'écran affichait « error.auth.mfa.codeInvalid » | Recette S01 | L'accueil lit un code technique au lieu d'une phrase, et ne sait pas quoi faire | Test technique `i18n.spec.ts`, qui compare les deux listes | Close |
| F01 | Préparer une activation écrasait le second facteur d'un compte déjà inscrit, avant toute confirmation | Recette S01 | Le téléphone du gérant cesse de fonctionner sans qu'il ait rien fait | Test technique `AuthApiTest.c6f` | Close |
| F01 | Un renouvellement de jeton se comportait comme une connexion : date de dernière connexion réécrite, entrée au journal | Recette S01 | Le gérant lit « connecté il y a une minute » d'un onglet resté ouvert, et le journal se noie | Test technique `AuthApiTest.c8d` | Close |
| F01 | Deux sessions simultanées d'une même personne se heurtaient sur un verrou optimiste : erreur serveur à la connexion | Recette S01, S05 | Quelqu'un connecté au comptoir et sur son téléphone voit « L'action n'a pas pu aboutir » | S01 et S05, joués en parallèle | Close |
| F01 | Les champs que l'accueil ne peut pas enregistrer avaient l'air modifiables | Recette S08 | On saisit dans une case qui n'enregistrera rien | S08 | Close |
| F01 | L'accueil détenait `users.exporter` sans `users.consulter` : un droit inopérant, qui s'activait dès qu'on lui accordait la simple lecture | Question soulevée par la recette S08, démontrée par l'API | Le gérant accorde « consulter la liste » et ouvre sans le savoir l'export de la liste du personnel, contre le critère C33b | Tests techniques `ExportsApiTest.c33c` et `ArchitectureTest.exporterSupposeConsulter` | Close |

## Questions ouvertes

| Feature | Question | Trouvée par | Pourquoi elle compte |
| --- | --- | --- | --- |
| F01 | *(refermée le 2026-09-20)* L'accueil détenait `users.exporter` sans `users.consulter`. Vérification faite, il ne pouvait pas exporter — mais accorder la simple lecture ouvrait l'export. Le droit a été retiré du jeu par défaut : le gérant peut l'accorder au cas par cas. À confirmer : l'accueil doit-il pouvoir ouvrir la liste du personnel ? | Recette S08 | Un export quitte l'application et ne se rattrape pas (C33b) |
