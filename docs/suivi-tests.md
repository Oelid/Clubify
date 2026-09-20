# Suivi des tests — source

Ce fichier est la **source manuelle** du classeur `docs/suivi-tests.xlsx`, qui est
régénéré par `tools/generer-suivi-tests.py`. Le classeur n'est jamais modifié à
la main : il serait écrasé.

Le reste du classeur est extrait du code : chaque scénario y vient d'un
`@DisplayName` (backend) ou d'un `it(...)` (frontend), et chaque résultat d'une
exécution réelle (rapports JUnit). Ce fichier ne porte donc que ce que le code ne
peut pas dire : ce qui reste à écrire, ce qui se vérifie à la main, et ce qui a
été trouvé en défaut.

## Rattachement des suites aux features

Un chemin, une feature. Le premier motif qui correspond gagne ; ce qui ne
correspond à rien est signalé au lieu d'être rattaché au hasard.

| Motif de chemin | Feature |
| --- | --- |
| `backend/src/test/java/ma/clubify/architecture/` | F01 |
| `backend/src/test/java/ma/clubify/platform/` | F01 |
| `frontend/projects/ui/` | F01 |
| `frontend/projects/backoffice/src/app/core/` | F01 |
| `frontend/projects/backoffice/src/app/auth/` | F01 |
| `frontend/projects/backoffice/src/app/app.spec.ts` | F01 |
| `frontend/e2e/` | F01 |

## Tests à compléter

Ce qui manque, et pourquoi. Une ligne par scénario attendu. « Quand » dit à quelle
occasion il sera écrit : maintenant si c'est dans F01, ou la feature qui l'amènera.

| Feature | Scénario attendu | Niveau | Pourquoi il manque | Quand |
| --- | --- | --- | --- | --- |
| F01 | Première connexion de l'administrateur, de bout en bout dans un navigateur | Bout en bout | Le squelette Playwright existe mais n'est pas branché sur un backend de test | F01, avant la mise en service |
| F01 | Chaque composant de `ui` rendu en FR et en droite-à-gauche | Story Storybook | Les composants de `ui` se limitent aux jetons et au thème ; les enveloppes PrimeNG arrivent avec le premier écran métier | F02 |
| F01 | Création d'un utilisateur depuis l'écran | Composant | Le bouton est posé et protégé par droit ; l'action reste à écrire | F01, avant la mise en service |
| F01 | Export d'une liste depuis l'écran | Composant | Même raison que la création d'utilisateur | F01, avant la mise en service |
| F01 | Reprise de session après rechargement de la page | Composant | Vérifié à la main dans le navigateur, pas encore automatisé | F01, avant la mise en service |
| F01 | Amorçage d'un club : transaction et trace d'audit | Intégration | Vérifié à la main en base après le défaut trouvé ; la commande n'existe qu'en `dev` et `demo` | F01, avant la mise en service |
| F01 | Un club sans couleur de marque garde les jetons Clubify, de bout en bout | Bout en bout | Couvert côté magasin de session, pas encore à l'écran | F02 |

## Recette manuelle

Ce qui ne s'automatise pas, ou pas encore, et que quelqu'un déroule avant une
mise en service. « Dernier passage » est saisi ici, à la main.

| Feature | Scénario | Qui | Dernier passage | Résultat | Remarque |
| --- | --- | --- | --- | --- | --- |
| F01 | Scanner le QR avec une vraie application d'authentification et se connecter | Omar | | | Aucun test automatisé ne peut scanner un QR |
| F01 | Lire les écrans sur un téléphone, en portrait | Omar | | | Le backoffice vise l'écran du comptoir ; il doit rester utilisable ailleurs |
| F01 | Imprimer la liste des utilisateurs | Accueil | | | Feuille de secours du comptoir |
| F01 | Vérifier les libellés FR avec l'accueil et le gérant | Accueil, gérant | | | Validation des maquettes (`frontend/CLAUDE.md`) |

## Anomalies trouvées

Une ligne par défaut trouvé après l'écriture du code, avec le test qui l'empêche
de revenir. Une anomalie sans test de non-régression n'est pas close.

| Feature | Anomalie | Trouvée par | Conséquence si elle revenait | Test qui la ferme | État |
| --- | --- | --- | --- | --- | --- |
| F01 | L'écran de second facteur déduisait l'étape de l'existence d'un jeton, au lieu de suivre l'issue du backend | Parcours en navigateur | Le secret et les codes de secours d'un compte déjà inscrit régénérés en silence ; son téléphone cesse de fonctionner | `mfa.page.spec.ts` | Close |
| F01 | Le journal d'audit n'attribuait aucun auteur aux connexions | Parcours en navigateur | La colonne que le gérant regarde en premier reste vide (SEC-04) | `AuditApiTest.c16c` | Close |
| F01 | Tolérance d'une seconde sur la borne de révocation des jetons | Test C8b devenu instable | Un jeton survit à la fermeture des sessions demandée par le gérant | `UsersApiTest.c8b` | Close |
| F01 | L'amorçage écrivait hors transaction : un club créé sans trace d'audit | Vérification en base | Une action sensible sans trace, contre la règle 17 | À écrire (voir « Tests à compléter ») | Ouverte |
| F01 | Les contrôleurs manipulaient des entités JPA | `ArchitectureTest` | Relations paresseuses et cycle de vie transactionnel dans la couche web | `ArchitectureTest.controleurSansEntite` | Close |
