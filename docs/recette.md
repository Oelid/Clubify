# Lancer Clubify

Deux façons de faire tourner l'application, pour deux besoins différents. Le
choix tient en une question : **voulez-vous que les corrections apparaissent
pendant que vous testez, ou que rien ne bouge sous vos yeux ?**

## 1. L'environnement de recette — rien ne bouge

```powershell
.\tools\demarrer-recette.ps1
```

L'interface s'ouvre sur **http://localhost:4300**.

Les identifiants s'affichent à la fin du script, et se retrouvent ensuite par
`.	oolsfficher-acces.ps1`. Le détail — première connexion, second facteur,
que faire quand ça coince — est dans **`docs/acces.md`**.

Tout tourne en conteneurs, avec **sa propre base**, séparée de celle du
développement. Les corrections faites pendant que vous testez **n'y apparaissent
pas** : il faut relancer le script pour prendre la dernière version (environ une
minute). C'est délibéré — une application qui redémarre au milieu d'un parcours
ne se teste pas, et un écran qui change pendant qu'on le regarde fait douter de
ce qu'on vient de voir.

| Commande | Effet |
| --- | --- |
| `.\tools\demarrer-recette.ps1` | Construit, démarre, et affiche les identifiants |
| `.\tools\demarrer-recette.ps1 -SansConstruction` | Redémarre sans reconstruire |
| `.\tools\demarrer-recette.ps1 -Arreter` | Arrête, en gardant la base |
| `.\tools\demarrer-recette.ps1 -Effacer` | Arrête et repart d'un club neuf |

Au premier démarrage :

- un fichier `.env` est créé, avec des secrets tirés au hasard et le mot de
  passe de l'administrateur. Il n'est pas versionné (`CLAUDE.md` §5) ;
- le club de recette est amorcé, avec son administrateur ;
- à votre première connexion, l'application impose d'activer le second facteur :
  scannez le code avec votre téléphone et **conservez les codes de secours**.
  Sans eux, un téléphone perdu ferme le club.

Ce qui tourne :

| Service | Adresse | Remarque |
| --- | --- | --- |
| Interface | http://localhost:4300 | nginx ; relaie `/api` vers le backend, donc une seule origine |
| API | http://localhost:8081 | `/swagger-ui` pour explorer le contrat |
| Base | localhost:5433 | Volume propre à la recette |

Aucun de ces ports n'est celui du développement (4200, 8080, 5432) : les deux
environnements cohabitent sans se gêner.

## 2. L'environnement de développement — tout bouge tout de suite

```powershell
cd backend  ; .\mvnw.cmd spring-boot:run     # http://localhost:8080
cd frontend ; npm start                       # http://localhost:4200
```

Les deux serveurs surveillent les fichiers : une correction apparaît en quelques
secondes à l'écran, sans rien relancer. C'est le mode de travail, pas celui de
la recette : le backend redémarre à chaque modification, et une requête tombée
au mauvais moment échoue.

La base de développement est démarrée automatiquement par Spring Boot
(`backend/compose.yaml`).

## Reconstruire les images sans reconstruire les artefacts

Les images embarquent les artefacts construits sur la machine — le jar et les
fichiers de l'interface — plutôt que de les construire elles-mêmes. C'est ce qui
permet de relancer la recette en une minute au lieu de dix : le cache Maven et
`node_modules` sont déjà là. Le jour où une chaîne d'intégration continue
existera, elle construira dans une première étape de l'image ; les `Dockerfile`
changeront, rien d'autre.

## Ce que la recette ne remplace pas

Les scénarios de `docs/suivi-tests.md` sont joués par la machine contre
l'environnement de **développement**. Les quatre scénarios manuels — scanner un
vrai QR, lire sur téléphone, valider les libellés, imprimer — se déroulent ici,
dans l'environnement de recette, et leur passage se note dans
`docs/suivi-tests.md`.
