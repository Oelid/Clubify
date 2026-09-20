# Se connecter — quel compte, et où est son mot de passe

Deux environnements tournent côte à côte, chacun avec **son** club et **son**
administrateur. Ils ne partagent rien : ni base, ni comptes, ni ports.

```powershell
.\tools\afficher-acces.ps1                    # ce qui tourne, et avec quel compte
.\tools\afficher-acces.ps1 -AvecMotsDePasse   # idem, mots de passe affichés
```

C'est la commande à retenir. Le reste de ce guide explique ce qu'elle montre.

## Les deux environnements

| | Recette | Développement |
| --- | --- | --- |
| **Pour qui** | Vous, pour tester | Moi, pour travailler |
| **Interface** | http://localhost:4300 | http://localhost:4200 |
| **API** | http://localhost:8081 | http://localhost:8080 |
| **Base** | localhost:5433 | localhost:5432 |
| **Compte** | `recette@exemple.test` | `admin@ptitclub.test` |
| **Mot de passe** | dans `.env` | dans `.env.dev` |
| **Démarrer** | `.\tools\demarrer-recette.ps1` | `mvnw spring-boot:run` + `npm start` |
| **Mes corrections** | après relance du script | en quelques secondes |

Les deux fichiers d'accès sont à la racine et **ne sont pas versionnés**
(`CLAUDE.md` §5). Ils appartiennent à cette machine.

**Celui que vous voulez, c'est la recette.** Le développement est mon atelier :
je le redémarre, j'y crée des comptes d'essai, j'y renomme le club.

## Première connexion

Vous entrez directement : le second facteur n'est pas imposé (décision 0031).
L'application affiche en revanche un **rappel permanent**, sur chaque écran,
tant qu'il n'est pas activé — et ce rappel ne se ferme pas.

1. `.	ools\demarrer-recette.ps1`, puis http://localhost:4300
2. Connectez-vous avec le compte et le mot de passe affichés par le script
3. Vous êtes dans l'application

### Activer le second facteur

Quand vous aurez votre téléphone sous la main, cliquez sur **« Activer
maintenant »** dans le rappel :

1. L'application montre un code à scanner : ouvrez une application
   d'authentification (Google Authenticator, Microsoft Authenticator, ou celle
   de votre gestionnaire de mots de passe) et scannez-le
2. **Notez les huit codes de secours.** Ils ne s'affichent qu'une fois
3. Saisissez le code à six chiffres affiché par le téléphone

Aux connexions suivantes, seul le code à six chiffres est demandé. Vous pouvez
cocher « ne plus demander sur cet appareil » : l'appareil reste reconnu trente
jours, durée réglable dans les paramètres du club.

Le rappel ne s'adresse qu'à l'administrateur du compte et au gérant : ce sont
les rôles qui ouvrent la caisse, les soldes des familles et les données de
santé. L'accueil et le coach ne le voient jamais.

### Pour l'imposer

Deux règles dans les paramètres du club :

| Règle | Défaut | Effet |
| --- | --- | --- |
| `security.mfa.required` | `false` | À `true`, le second facteur devient bloquant |
| `security.mfa.grace_days` | `7` | Délai laissé avant qu'il le devienne |

## Quand ça coince

### « Adresse électronique ou mot de passe incorrect »

Relisez le mot de passe : `.\tools\afficher-acces.ps1 -AvecMotsDePasse`.

Attention, **cinq échecs de suite bloquent le compte quinze minutes**, et le bon
mot de passe est alors refusé lui aussi. Le message le dit. Attendez, ou
repartez d'une base neuve (`-Effacer`).

### « Compte bloqué après plusieurs échecs »

Quinze minutes, puis c'est fini. C'est une protection, pas une panne.

### Téléphone perdu, ou codes de secours épuisés

Un code de secours remplace le code du téléphone, une fois chacun. S'il n'en
reste plus :

```powershell
.\tools\reinitialiser-second-facteur.ps1 recette@exemple.test -Recette
```

Le second facteur est effacé ; la connexion suivante en redemande l'activation.
Les appareils de confiance sont révoqués au passage : ils ouvraient le compte au
même titre que le téléphone.

> En production, ce script n'existe pas. C'est l'administrateur du compte qui
> réinitialise le second facteur d'un utilisateur depuis l'application, et
> l'action est tracée (critère C6c). Le cas que l'application ne couvre pas —
> l'administrateur lui-même enfermé dehors — reste une question ouverte pour la
> mise en service.

### Mot de passe perdu et fichier d'accès effacé

Il n'y a pas de « mot de passe oublié » dans F01 : rien ne permet de le
retrouver. Sur la recette, on repart d'un club neuf :

```powershell
.\tools\demarrer-recette.ps1 -Effacer
.\tools\demarrer-recette.ps1
```

Un nouveau mot de passe est tiré et affiché. Les données de recette sont
perdues — elles sont faites pour ça.

### « Le serveur est injoignable »

L'interface tourne mais pas le backend. `.\tools\afficher-acces.ps1` le dit en
une ligne ; relancez l'environnement concerné.

## Pour la recette fonctionnelle automatisée

`npm run e2e` lit `.env.dev` tout seul : aucune variable à exporter. Le fichier
porte, en plus du compte, le **secret du second facteur** noté à son activation.

Si vous réinitialisez le second facteur du compte de développement, ce secret ne
vaut plus rien et la recette échouera en le disant. Réactivez le second facteur
et remplacez `E2E_ADMIN_TOTP_SECRET` dans `.env.dev`.

## Ce qui n'est pas là

Aucun de ces comptes n'a de valeur au-delà de cette machine : bases jetables,
données fictives, aucun nom réel (`CLAUDE.md` §7). Les vrais comptes du club
seront créés à la mise en service, avec un chemin d'amorçage qui reste à cadrer
(décision 0030).
