# 0027 — Second facteur obligatoire pour le gérant

## Contexte

SEC-01 prévoit pour le staff « mot de passe + second facteur optionnel », sans distinguer les rôles. Le compte gérant est celui qui voit et modifie tout : caisse, soldes des familles, remises, dérogations, données de santé des enfants. Un mot de passe de gérant qui fuit expose l'ensemble.

## Décision

Le second facteur est un code temporaire par application (TOTP, standard ouvert). Il est obligatoire pour le rôle gérant et optionnel pour les autres utilisateurs du staff. Un gérant sans second facteur actif doit l'activer à sa première connexion, avant tout accès. Un gérant peut réinitialiser le second facteur d'un utilisateur qui a perdu son téléphone ; l'opération est auditée.

## Raison

Le compte gérant concentre les droits sur l'argent et les données sensibles ; c'est celui qu'il faut protéger en premier (10.I : détournement d'espèces, non-conformité CNDP). Le TOTP ne dépend d'aucun prestataire, ne coûte rien, fonctionne sans réseau sur le téléphone, et n'attend pas le connecteur de messagerie de R3.

## Alternatives écartées

- Optionnel pour tous, gérant compris : fidèle au texte du cahier, mais laisse le compte le plus exposé sans protection.
- Code par SMS ou WhatsApp : exige le connecteur de messagerie (R3) et un fournisseur payant ; moins sûr que le TOTP.
- Clé physique ou passkey (WebAuthn) : matériel à gérer, disproportionné pour un club de 150 à 300 inscrits ; à reconsidérer plus tard pour le seul gérant.
- Obligatoire pour tout le staff : contraignant pour un accueil qui se connecte plusieurs fois par jour, sans gain proportionné.

## Source dans le cahier des charges

SEC-01, SEC-02 (permissions fines sur finances, remises, santé), SEC-04, 10.I ; décision 0024 (JWT auto-émis) ; fiche F01, règle 5 et critères C6, C6b, C6c.

## Écarts ou points ouverts

- Écart avec SEC-01 : « optionnel » devient « obligatoire pour le gérant ». Tranché par Omar le 2026-09-20.
- Le second facteur des parents n'est pas concerné : leur accès passe par un code à usage unique (APP-01, R8).

## Date

2026-09-20

## Statut

Acceptée.
