# 0030 — Ajustements issus de l'implémentation du socle

## Contexte

L'implémentation de F01 a révélé six points que ni la fiche de feature ni la
décision 0029 n'avaient tranchés, et que le code ne pouvait pas laisser ouverts.
Trois viennent de l'écriture du backend, trois du branchement de l'interface et
du parcours réel en navigateur. Cette fiche les consigne pour qu'ils ne soient
pas redécouverts feature par feature.

## Décision

1. **Le jeton de renouvellement voyage en cookie `HttpOnly`**, `SameSite=Strict`,
   borné au chemin `/api/v1/auth`. Le corps de la réponse le porte encore, pour
   les clients sans cookies. `RefreshRequest.refreshToken` devient facultatif.
2. **Confirmer le second facteur ouvre la session** : `POST /profile/mfa/confirm`
   répond par un couple de jetons au lieu d'un 204.
3. **Le jeton d'accès porte son instant d'émission à la milliseconde** (`iat_ms`)
   et l'adresse de son titulaire (`email`).
4. **Les couleurs de marque du club sont deux règles configurables**
   (`club.brand.primary`, `club.brand.secondary`), sans valeur par défaut, et
   voyagent dans `ClubSummary` à la connexion.
5. **Une action menée hors demande authentifiée nomme son auteur** : une portée
   explicite (`UserActor`) attribue la connexion réussie à la personne qui vient
   de se connecter, et non au système.
6. **Les services ne rendent ni entité JPA ni modèle de contrat** : un objet de
   transfert par frontière. L'énumération `Role` quitte le paquet `model.entity`,
   n'étant pas une entité.

## Raison

1. Un jeton lisible par du JavaScript est exfiltrable par la première faille
   d'injection ; `frontend/CLAUDE.md` l'interdisait déjà, le backend ne le
   permettait pas encore. `SameSite=Strict` tient lieu de protection CSRF.
2. Le code que l'utilisateur vient de saisir *est* une preuve de second facteur.
   Sans cette réponse, l'interface devait conserver le mot de passe le temps de
   l'activation pour se reconnecter ensuite — exactement ce qu'on cherche à
   éviter.
3. L'horodatage `iat` d'un JWT est tronqué à la seconde. La borne de révocation
   immédiate se comparait donc à une seconde près, ce qui imposait une tolérance
   pendant laquelle un jeton survivait à la fermeture de ses sessions. La
   milliseconde supprime la tolérance sans rien casser. L'adresse rend le journal
   d'audit lisible seul, et figée à l'instant de l'action : un compte renommé ne
   réécrit pas l'histoire.
4. La marque d'un club est un réglage de club, pas une colonne de son identité :
   elle suit le même chemin que les autres règles de la section 9.8. Sans valeur,
   l'interface garde les jetons Clubify — la couleur du club reste un accent
   (décision 0025).
5. Au moment où une connexion réussit, aucun jeton n'est encore posé dans le
   contexte de sécurité : le journal attribuait donc toutes les connexions au
   « système », avec une colonne « auteur » vide. C'est précisément la ligne que
   le gérant cherche en premier (benchmark B3).
6. `backend/CLAUDE.md` l'exigeait ; le test d'architecture le relevait. Une entité
   JPA porte des relations paresseuses et un cycle de vie transactionnel qui
   n'ont rien à faire dans la couche web.

## Alternatives écartées

- **Jeton de renouvellement en `localStorage`** : plus simple, mais lisible par
  tout script de la page.
- **Reconnexion par mot de passe après activation du second facteur** : obligeait
  l'interface à retenir le mot de passe.
- **Tolérance d'une seconde sur la révocation** : laissait une fenêtre d'accès
  après une fermeture demandée. Se tromper doit aller dans le sens de la
  révocation.
- **Couleurs de marque en colonnes de `club`** : aurait fait de la marque une
  donnée d'identité, et demandé une migration à chaque ajout de jeton de marque.
- **Déduire l'auteur d'une connexion de l'entité touchée** : marche pour
  l'authentification, ne se généralise pas.
- **Assouplir la règle d'architecture** plutôt que le code : la règle est bonne,
  c'était le code qui avait tort.

## Source dans le cahier des charges

SEC-01, SEC-04, ADM-01, PLT-01, section 9.6, section 9.8. Décisions 0024, 0025,
0027, 0028, 0029.

## Écarts ou points ouverts

- Le contrat d'API change sur trois points (`ClubSummary`, `RefreshRequest`,
  réponse de `confirmMfa`), tous dans le périmètre de F01.
- `cookie-secure` vaut `false` en développement et en test, l'interface y tournant
  en clair sur `localhost`. Il reste à `true` partout ailleurs.
- La commande d'amorçage `--seed-club` n'existe qu'en profils `dev` et `demo` ;
  la mise en service du club pilote demandera un chemin équivalent en production,
  à cadrer avec l'hébergement (question ouverte de 0023).

## Date

2026-09-20

## Statut

Proposée
