# Recette fonctionnelle — scénarios

Ce fichier décrit **ce qu'on déroule depuis les écrans**, assez précisément pour
que n'importe qui le refasse à l'identique. Il est la source du classeur
`docs/suivi-tests.xlsx`, régénéré par `tools/generer-suivi-tests.py` ; le
classeur ne se saisit pas, il serait écrasé.

Chaque scénario porte un identifiant. Le test Playwright qui le joue le reprend
dans son titre : c'est ce qui relie la description ci-dessous à un résultat
d'exécution réel. Un scénario `Manuel` se déroule à la main, et son passage se
note dans la table « Passages manuels », en bas.

**Les tests techniques ne figurent pas ici** — unitaires, intégration de l'API,
architecture. Ils sont le filet du développement, pas la recette ; le classeur
n'en donne que le compte.

## Lancer la recette automatisée

Depuis `frontend/`, avec le backend et un club amorcé en marche :

```
npm run e2e
```

Les accès sont lus dans `.env.dev` (voir `docs/acces.md`). Chaque scénario
fabrique les comptes dont il a besoin : aucun ne dépend de ce qu'un autre a
laissé derrière lui, et deux passages de suite donnent le même résultat.

## Rattachement des suites aux features

| Motif de chemin | Feature |
| --- | --- |
| `frontend/e2e/f01-` | F01 |

---

# F01 — Socle plateforme

## Connexion et second facteur

### S01 — Première connexion
- **Mode** : Automatisé
- **Critères** : C5
- **Préalable** : un compte d'administration dont le second facteur n'est pas activé
- **Étapes** :
  1. Ouvrir l'application
  2. Saisir l'adresse électronique et le mot de passe
  3. Valider
- **Attendu** : la session s'ouvre sur le premier écran que le rôle permet. L'en-tête nomme la personne connectée. La navigation ne montre que ce à quoi elle a droit.

### S02 — Mot de passe erroné
- **Mode** : Automatisé
- **Critères** : C5, C36
- **Préalable** : un compte existant
- **Étapes** :
  1. Saisir la bonne adresse et un mauvais mot de passe
  2. Valider
- **Attendu** : « Adresse électronique ou mot de passe incorrect. » On reste sur l'écran de connexion, et le mot de passe saisi n'apparaît nulle part.

### S03 — Cinq échecs verrouillent le compte
- **Mode** : Automatisé
- **Critères** : C10b
- **Préalable** : un compte d'accueil jetable
- **Étapes** :
  1. Saisir cinq fois de suite un mauvais mot de passe
  2. Saisir le bon mot de passe
- **Attendu** : au sixième essai, même avec le bon mot de passe, l'accès est refusé avec « Compte bloqué après plusieurs échecs ». Le verrou tient quinze minutes.

### S04 — Connexion d'un compte déjà inscrit au second facteur
- **Mode** : Automatisé
- **Critères** : C6
- **Préalable** : un compte de gérant dont le second facteur est activé
- **Étapes** :
  1. Se connecter avec l'adresse et le mot de passe
  2. Saisir le code à six chiffres
- **Attendu** : l'écran demande un code, sans afficher ni QR ni nouveaux codes de secours. La session s'ouvre, et aucun bandeau de rappel ne s'affiche : ce compte est protégé.

### S05 — Activer le second facteur depuis le bandeau
- **Mode** : Automatisé
- **Critères** : C6b, C6g
- **Préalable** : un compte de gérant sans second facteur
- **Étapes** :
  1. Se connecter
  2. Cliquer « Activer maintenant » dans le bandeau
  3. Saisir le code produit par l'application d'authentification
- **Attendu** : l'écran montre un QR et huit codes de secours. Après saisie du code, la session se poursuit sans ressaisir le mot de passe, et le bandeau disparaît.

### S06 — Code de second facteur erroné
- **Mode** : Automatisé
- **Critères** : C6, C36
- **Préalable** : un compte dont le second facteur est activé
- **Étapes** :
  1. Se connecter
  2. Saisir un code faux
- **Attendu** : « Code incorrect. Vérifiez l'heure de votre téléphone, puis réessayez. » Le champ se vide, on reste sur l'écran.

### S07 — Se déconnecter
- **Mode** : Automatisé
- **Critères** : C8
- **Préalable** : une session ouverte
- **Étapes** :
  1. Cliquer « Se déconnecter »
  2. Revenir en arrière dans le navigateur
- **Attendu** : retour à l'écran de connexion, et le retour en arrière ne rouvre pas la session.

### S08 — Recharger la page
- **Mode** : Automatisé
- **Critères** : C8c
- **Préalable** : une session ouverte
- **Étapes** :
  1. Recharger la page
- **Attendu** : on reste dans l'application sans ressaisir le mot de passe. Le jeton d'accès n'a pas survécu au rechargement ; le cookie de renouvellement, si.

### S09 — L'accueil n'est jamais invitée à activer un second facteur
- **Mode** : Automatisé
- **Critères** : C6k
- **Préalable** : un compte d'accueil
- **Étapes** :
  1. Se connecter
- **Attendu** : aucun bandeau de rappel. Il ne s'adresse qu'aux rôles qui ouvrent l'argent et les données sensibles.

## Paramètres du club

### S10 — Modifier l'identité du club
- **Mode** : Automatisé
- **Critères** : C28, C37, C16
- **Préalable** : être connecté avec un droit de modification
- **Étapes** :
  1. Ouvrir « Paramètres du club »
  2. Changer le nom
  3. Saisir le téléphone au format local, avec des espaces : 06 12 34 56 78
  4. Enregistrer
  5. Ouvrir le journal d'audit
- **Attendu** : « Paramètres enregistrés. » Le téléphone s'affiche normalisé en +212612345678. Le journal porte une entrée club.updated, avec son auteur.

### S11 — Nom du club laissé vide
- **Mode** : Automatisé
- **Critères** : C36
- **Préalable** : être connecté avec un droit de modification
- **Étapes** :
  1. Vider le champ « Nom du club »
  2. Enregistrer
  3. Recharger la page
- **Attendu** : « Le nom du club est obligatoire. » Rien n'est enregistré : le nom d'origine revient au rechargement.

### S12 — ICE mal formé
- **Mode** : Automatisé
- **Critères** : C28
- **Préalable** : être connecté avec un droit de modification
- **Étapes** :
  1. Saisir 12345 dans le champ ICE
  2. Enregistrer
- **Attendu** : « L'ICE compte quinze chiffres. » Rien n'est enregistré.

### S13 — Changer la couleur du club
- **Mode** : Automatisé
- **Critères** : C28b
- **Préalable** : être connecté avec un droit de modification
- **Étapes** :
  1. Choisir une couleur principale
  2. Enregistrer
  3. Recharger la page
- **Attendu** : l'accent de l'application prend la couleur choisie sans recharger, et elle est toujours là après rechargement.

### S14 — Lire les règles configurables
- **Mode** : Automatisé
- **Critères** : C31
- **Préalable** : être connecté
- **Étapes** :
  1. Ouvrir « Paramètres du club »
  2. Descendre au tableau des règles
- **Attendu** : chaque règle porte un libellé en français, une explication et sa valeur. L'origine dit « Défaut Clubify » ou « Choix du club ». Celles que le club ne peut pas changer portent « Fixée par Clubify » et n'ont pas de champ de saisie.

### S15 — Modifier une règle configurable
- **Mode** : Automatisé
- **Critères** : C31b
- **Préalable** : être connecté avec un droit de modification
- **Étapes** :
  1. Porter « Durée d'un lien de fichier » à 25
  2. Enregistrer
  3. Recharger la page
- **Attendu** : la valeur est retenue, et l'origine passe à « Choix du club ».

### S16 — Le nombre de lignes par page suit la règle du club
- **Mode** : Automatisé
- **Critères** : C40c
- **Préalable** : être connecté avec un droit de modification, et plus de trente comptes dans le club
- **Étapes** :
  1. Porter « Lignes par page » à 30 dans les paramètres
  2. Enregistrer
  3. Se déconnecter, puis se reconnecter
  4. Ouvrir « Utilisateurs »
- **Attendu** : la liste s'ouvre à trente lignes par page, sans qu'on ait rien choisi à l'écran.

## Utilisateurs

### S20 — Consulter la liste
- **Mode** : Automatisé
- **Critères** : C29
- **Préalable** : être connecté avec le droit de consulter
- **Étapes** :
  1. Ouvrir « Utilisateurs »
- **Attendu** : chaque ligne porte nom, adresse, rôle, état du second facteur, statut et dernière connexion. Les dates sont au fuseau du club. Un compte jamais connecté affiche « Jamais ».

### S21 — Parcourir la liste page par page
- **Mode** : Automatisé
- **Critères** : C40
- **Préalable** : plus de vingt comptes dans le club
- **Étapes** :
  1. Ouvrir « Utilisateurs »
  2. Lire la plage affichée
  3. Cliquer « Suivante »
  4. Cliquer « Précédente »
- **Attendu** : la première page montre « 1–20 sur N ». « Suivante » montre « 21–40 sur N » avec des lignes différentes. « Précédente » ramène exactement à la première page.

### S22 — Changer le nombre de lignes par page
- **Mode** : Automatisé
- **Critères** : C40
- **Préalable** : plus de trente comptes dans le club
- **Étapes** :
  1. Aller à la deuxième page
  2. Choisir 50 lignes par page
- **Attendu** : la liste revient à la première page et montre « 1–50 sur N ». Changer la taille ne laisse jamais sur une page qui n'existe plus.

### S23 — Chercher après avoir paginé
- **Mode** : Automatisé
- **Critères** : C40, C41
- **Préalable** : plus de trente comptes, dont un compte connu créé pour l'occasion
- **Étapes** :
  1. Choisir 30 lignes par page
  2. Aller à la deuxième page
  3. Saisir l'adresse du compte connu dans la recherche
- **Attendu** : la liste revient à la première page et ne montre qu'une seule ligne, celle du compte cherché. La plage affiche « 1–1 sur 1 » et les boutons de page sont inactifs.

### S24 — Filtrer par rôle
- **Mode** : Automatisé
- **Critères** : C41
- **Préalable** : des comptes de plusieurs rôles
- **Étapes** :
  1. Choisir « Coach » dans le filtre de rôle
- **Attendu** : toutes les lignes affichées portent le rôle Coach, et le total change en conséquence.

### S25 — Filtrer par statut, puis réinitialiser
- **Mode** : Automatisé
- **Critères** : C41
- **Préalable** : au moins un compte désactivé
- **Étapes** :
  1. Choisir « Désactivé » dans le filtre de statut
  2. Cliquer « Réinitialiser »
- **Attendu** : seules les lignes désactivées s'affichent, puis la liste entière revient et les filtres sont vides.

### S26 — Créer un compte
- **Mode** : Automatisé
- **Critères** : C12
- **Préalable** : être connecté avec le droit de créer
- **Étapes** :
  1. Cliquer « Ajouter un utilisateur »
  2. Renseigner prénom, nom, adresse, rôle et mot de passe
  3. Créer le compte
  4. Chercher la nouvelle adresse dans la liste
  5. Se déconnecter et se connecter avec ce compte
- **Attendu** : « Compte créé. » Le compte se retrouve par la recherche, et la personne peut réellement se connecter.

### S27 — Adresse déjà utilisée
- **Mode** : Automatisé
- **Critères** : C36
- **Préalable** : un compte existant
- **Étapes** :
  1. Créer un compte avec une adresse déjà prise
- **Attendu** : « Cette adresse est déjà utilisée. » Le formulaire reste ouvert avec la saisie, rien n'est créé.

### S28 — Mot de passe trop court
- **Mode** : Automatisé
- **Critères** : C5b
- **Préalable** : être connecté avec le droit de créer
- **Étapes** :
  1. Créer un compte avec un mot de passe de onze caractères
- **Attendu** : le compte n'est pas créé, et le message dit que le mot de passe est trop court.

### S29 — Les rôles proposés sont ceux que l'application sait attribuer
- **Mode** : Automatisé
- **Critères** : C12c
- **Préalable** : être connecté avec le droit de créer
- **Étapes** :
  1. Ouvrir le formulaire de création
  2. Dérouler la liste des rôles
- **Attendu** : Administrateur, Gérant, Accueil et Coach. Ni Parent ni Comptable : leur application n'existe pas encore, les proposer serait un piège.

### S30 — Exporter la liste du personnel
- **Mode** : Automatisé
- **Critères** : C33, C34
- **Préalable** : être connecté avec le droit d'exporter
- **Étapes** :
  1. Cliquer « Exporter »
- **Attendu** : un fichier utilisateurs.csv se télécharge, en-têtes en français, contenant les comptes. Aucune colonne sensible n'y figure.

### S31 — Un rôle sans écran le sait
- **Mode** : Automatisé
- **Critères** : C12
- **Préalable** : un compte de coach
- **Étapes** :
  1. Se connecter avec ce compte
- **Attendu** : « Votre espace n'est pas encore ouvert. » Pas de page blanche, pas de renvoi d'un écran à l'autre.

## Journal d'audit

### S40 — Consulter le journal
- **Mode** : Automatisé
- **Critères** : C19
- **Préalable** : être connecté avec le droit de consulter le journal
- **Étapes** :
  1. Ouvrir « Journal d'audit »
- **Attendu** : les entrées les plus récentes d'abord, avec horodatage au fuseau du club, auteur, type, action et cible. Une connexion réussie porte l'adresse de la personne, jamais « Système ».

### S41 — Filtrer le journal par action
- **Mode** : Automatisé
- **Critères** : C19b
- **Préalable** : des entrées de plusieurs actions
- **Étapes** :
  1. Saisir auth.login.succeeded dans le filtre d'action
  2. Filtrer
  3. Réinitialiser
- **Attendu** : toutes les lignes affichées portent cette action, sans exception. Réinitialiser vide le filtre et rend la liste entière.

### S42 — Filtrer le journal par période
- **Mode** : Automatisé
- **Critères** : C19b
- **Préalable** : être connecté avec le droit de consulter le journal
- **Étapes** :
  1. Choisir comme date de début le lendemain
  2. Filtrer
- **Attendu** : aucune entrée, et le tableau le dit : « Aucune entrée pour ces critères. » Pas de page vide sans explication.

### S43 — Parcourir le journal page par page
- **Mode** : Automatisé
- **Critères** : C19b, C40
- **Préalable** : plus d'une page d'entrées
- **Étapes** :
  1. Ouvrir le journal
  2. Cliquer « Suivante »
- **Attendu** : la page change et les entrées sont différentes. Le journal est borné comme les autres listes.

## Droits et accès

### S50 — L'accueil consulte les paramètres sans pouvoir les modifier
- **Mode** : Automatisé
- **Critères** : C11, C32
- **Préalable** : un compte d'accueil
- **Étapes** :
  1. Se connecter
  2. Ouvrir « Paramètres du club »
- **Attendu** : les champs se lisent mais ne se saisissent pas, aucun bouton « Enregistrer », et les règles configurables n'ont pas de champ.

### S51 — L'accueil n'atteint pas le journal, même par l'adresse
- **Mode** : Automatisé
- **Critères** : C19
- **Préalable** : un compte d'accueil
- **Étapes** :
  1. Se connecter
  2. Constater que « Journal d'audit » est absent du menu
  3. Saisir l'adresse du journal à la main dans le navigateur
- **Attendu** : le journal ne s'ouvre pas, et l'accueil est renvoyée vers un écran qui lui est permis.

### S52 — L'accueil ne voit pas la liste du personnel
- **Mode** : Automatisé
- **Critères** : C11
- **Préalable** : un compte d'accueil
- **Étapes** :
  1. Se connecter
  2. Regarder le menu
- **Attendu** : « Utilisateurs » est absent. Le menu ne promet que ce que le droit permet.

## Recette manuelle

### S90 — Scanner le QR avec un vrai téléphone
- **Mode** : Manuel
- **Critères** : C6
- **Préalable** : un compte de gérant sans second facteur, un téléphone avec une application d'authentification
- **Étapes** :
  1. Se connecter, cliquer « Activer maintenant »
  2. Scanner le code avec le téléphone
  3. Noter les codes de secours
  4. Saisir le code à six chiffres
- **Attendu** : le téléphone reconnaît le compte sous le nom du club, et le code produit est accepté. Aucune machine ne peut jouer ce scénario.

### S91 — Lire les écrans sur un téléphone, en portrait
- **Mode** : Manuel
- **Critères** : —
- **Préalable** : un téléphone
- **Étapes** :
  1. Ouvrir l'application sur le téléphone
  2. Parcourir connexion, paramètres du club, utilisateurs et journal
- **Attendu** : rien n'est coupé, les tableaux se font défiler, les boutons restent atteignables au pouce.

### S92 — Valider les libellés avec l'accueil et le gérant
- **Mode** : Manuel
- **Critères** : —
- **Préalable** : l'accueil et le gérant devant l'écran
- **Étapes** :
  1. Parcourir chaque écran avec eux
  2. Relever chaque mot qui ne leur parle pas
- **Attendu** : le vocabulaire est celui du club, pas celui du logiciel. Tout écart est noté et corrigé.

### S93 — Imprimer la liste des utilisateurs
- **Mode** : Manuel
- **Critères** : —
- **Préalable** : une liste à l'écran
- **Étapes** :
  1. Imprimer depuis le navigateur
- **Attendu** : la feuille est lisible, tient dans la largeur, et ne coupe pas les colonnes.

---

## Passages manuels

Saisi à la main après chaque passage d'un scénario « Manuel ».

| ID | Qui | Date | Résultat | Remarque |
| --- | --- | --- | --- | --- |

## Anomalies trouvées

Une ligne par défaut trouvé après l'écriture du code, avec le test qui l'empêche
de revenir. Une anomalie sans test de non-régression n'est pas close.

| Feature | Anomalie | Trouvée par | Conséquence si elle revenait | Test qui la ferme | État |
| --- | --- | --- | --- | --- | --- |
| F01 | L'écran de second facteur déduisait l'étape de l'existence d'un jeton, au lieu de suivre l'issue du backend | Parcours en navigateur | Le secret et les codes de secours d'un compte déjà inscrit régénérés en silence ; son téléphone cesse de fonctionner | S04 | Close |
| F01 | Le journal d'audit n'attribuait aucun auteur aux connexions | Parcours en navigateur | La colonne que le gérant regarde en premier reste vide (SEC-04) | S40 | Close |
| F01 | Tolérance d'une seconde sur la borne de révocation des jetons | Suite technique devenue instable | Un jeton survit à la fermeture des sessions demandée par le gérant | Test technique `UsersApiTest.c8b` | Close |
| F01 | L'amorçage écrivait hors transaction : un club créé sans trace d'audit | Vérification en base | Une action sensible sans trace, contre la règle 17 | Test technique `ClubSeederTest.c42b` | Close |
| F01 | Les contrôleurs manipulaient des entités JPA | Test d'architecture | Relations paresseuses et cycle de vie transactionnel dans la couche web | Test technique `ArchitectureTest` | Close |
| F01 | Quinze codes d'erreur du backend n'avaient aucune traduction : l'écran affichait « error.auth.mfa.codeInvalid » | Recette S01 | L'accueil lit un code technique au lieu d'une phrase, et ne sait pas quoi faire | Test technique `i18n.spec.ts`, qui compare les deux listes | Close |
| F01 | Préparer une activation écrasait le second facteur d'un compte déjà inscrit, avant toute confirmation | Recette S01 | Le téléphone du gérant cesse de fonctionner sans qu'il ait rien fait | Test technique `AuthApiTest.c6f` | Close |
| F01 | Un renouvellement de jeton se comportait comme une connexion : date de dernière connexion réécrite, entrée au journal | Recette S01 | Le gérant lit « connecté il y a une minute » d'un onglet resté ouvert, et le journal se noie | Test technique `AuthApiTest.c8d` | Close |
| F01 | Deux sessions simultanées d'une même personne se heurtaient sur un verrou optimiste : erreur serveur à la connexion | Recette S01 et S10 | Quelqu'un connecté au comptoir et sur son téléphone voit « L'action n'a pas pu aboutir » | S01 et S10, joués en parallèle | Close |
| F01 | Les champs que l'accueil ne peut pas enregistrer avaient l'air modifiables | Recette S50 | On saisit dans une case qui n'enregistrera rien | S50 | Close |
| F01 | Un rôle sans aucun droit — le coach — tournait en boucle de redirections à la connexion | Recette S26 | La personne croit s'être trompée et appelle le club ; rien ne lui dit ce qui se passe | S31 | Close |
| F01 | L'accueil détenait users.exporter sans users.consulter : un droit inopérant, qui s'activait dès qu'on lui accordait la simple lecture | Question soulevée par la recette S50, démontrée par l'API | Le gérant accorde « consulter la liste » et ouvre sans le savoir l'export de la liste du personnel, contre le critère C33b | Tests techniques `ExportsApiTest.c33c` et `ArchitectureTest.exporterSupposeConsulter` | Close |
| F01 | La pagination était de façade : toute la liste remontait, découpée en mémoire | Revue de code sur demande d'Omar | Une liste de mille comptes traverse l'application pour en afficher vingt | Tests techniques `UsersApiTest.c40` et `PaginationTest` | Close |

## Questions ouvertes

| Feature | Question | Trouvée par | Pourquoi elle compte |
| --- | --- | --- | --- |
| F01 | L'accueil doit-elle pouvoir ouvrir la liste du personnel ? Aujourd'hui non — ni la lire, ni l'exporter | Recette S50 | Si elle a besoin des coordonnées des coachs au comptoir, c'est `users.consulter` qu'il faut lui donner par défaut |
| F01 | Que se passe-t-il si l'administrateur du compte perd son téléphone et ses codes de secours ? Personne au-dessus de lui ne peut le débloquer | Écriture du guide des accès | À trancher avant la mise en service : second administrateur imposé, ou procédure de notre côté |
